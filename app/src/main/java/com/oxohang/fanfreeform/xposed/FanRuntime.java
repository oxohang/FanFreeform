package com.oxohang.fanfreeform.xposed;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import com.oxohang.fanfreeform.config.ConfigContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

final class FanRuntime {
    private enum State { IDLE, ARMED, SIDE_ARMED, CLAIMED, ACTIVE, CANCELLED }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler configHandler;
    private final FanOverlayController overlay;
    private final FanTriggerCapture triggerCapture;
    private final HyperOsFreeformBridge freeform;
    private final OutsideGestureRecognizer outsideGestures;
    private final GestureArbitrator gestureArbitrator = new GestureArbitrator();
    private final SideGestureArbitrator sideGestureArbitrator = new SideGestureArbitrator();
    private final GestureReplayGuard gestureReplayGuard = new GestureReplayGuard();
    private final float density;
    private final float directionDecisionDistance;
    private final float sideDirectionSlop;
    private final float sideVerticalFloor;
    private volatile GestureConfig config = GestureConfig.defaults();
    private volatile List<RuntimeTarget> targets = Collections.emptyList();
    private State state = State.IDLE;
    private volatile int configRetryCount;
    private volatile boolean configRetryScheduled;
    private volatile boolean configLoadQueued;
    private long lastOnDemandConfigReload;
    private GestureGeometry.Corner corner;
    private float downX;
    private float downY;
    private int selected = -1;
    private boolean startedWithTriggerCapture;
    private boolean cornerTapIsOutsideWindow;
    private boolean sideTapIsOutsideWindow;
    private boolean activeSideList;
    private float sideListTop;
    private float sideRowHeight;
    private float sideActivationX;
    private float sideActivationY;
    private boolean sideSelectionReady;
    private int lastHapticSelection = -1;

    FanRuntime(Context context, ClassLoader classLoader) {
        this.context = context;
        HandlerThread configThread = new HandlerThread("FanFreeformConfig");
        configThread.start();
        configHandler = new Handler(configThread.getLooper());
        density = context.getResources().getDisplayMetrics().density;
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        directionDecisionDistance = Math.max(viewConfiguration.getScaledTouchSlop(), 10 * density);
        sideDirectionSlop = viewConfiguration.getScaledTouchSlop();
        sideVerticalFloor = 24 * density;
        overlay = new FanOverlayController(context, mainHandler);
        triggerCapture = new FanTriggerCapture(context, mainHandler);
        freeform = new HyperOsFreeformBridge(context, classLoader, mainHandler);
        outsideGestures = new OutsideGestureRecognizer(mainHandler,
                viewConfiguration, this::performOutsideAction);
        requestConfigReload();
        context.getContentResolver().registerContentObserver(ConfigContract.URI, false,
                new ContentObserver(mainHandler) {
                    @Override public void onChange(boolean selfChange) { requestConfigReload(); }
                });
        try {
            IntentFilter lifecycleFilter = new IntentFilter(Intent.ACTION_USER_UNLOCKED);
            lifecycleFilter.addAction(Intent.ACTION_USER_PRESENT);
            lifecycleFilter.addAction(Intent.ACTION_SCREEN_ON);
            lifecycleFilter.addAction(Intent.ACTION_SCREEN_OFF);
            context.registerReceiver(new BroadcastReceiver() {
                @Override public void onReceive(Context receiverContext, Intent intent) {
                    mainHandler.post(FanRuntime.this::refreshTriggerCapture);
                    String action = intent.getAction();
                    if (Intent.ACTION_SCREEN_ON.equals(action)) {
                        mainHandler.postDelayed(FanRuntime.this::refreshTriggerCapture, 750L);
                        mainHandler.postDelayed(FanRuntime.this::refreshTriggerCapture, 1500L);
                    }
                    if (!Intent.ACTION_USER_UNLOCKED.equals(action)
                            && !Intent.ACTION_USER_PRESENT.equals(action)) return;
                    configHandler.post(() -> {
                        configRetryCount = 0;
                        configRetryScheduled = false;
                        requestConfigReload();
                        if (freeform.hasController()) {
                            reportStatus("HyperOS 3 原生接口已连接");
                        }
                    });
                }
            }, lifecycleFilter, Context.RECEIVER_NOT_EXPORTED);
        } catch (Throwable error) {
            Log.e("Cannot register user-unlocked configuration reload", error);
        }
    }

    void setFreeformController(Object controller) {
        freeform.setController(controller);
        reportStatusAsync("HyperOS 3 原生接口已连接");
    }

    void reportInputReady() {
        reportStatusAsync(freeform.hasController()
                ? "HyperOS 3 原生接口已连接" : "手势接口已连接，等待小窗控制器");
    }

    void onTaskAppeared(int taskId) {
        freeform.onTaskAppeared(taskId);
    }

    void onTaskInfo(Object info) {
        freeform.onTaskInfo(info);
        if (freeform.trackedTaskId() < 0) outsideGestures.clearAll();
    }

    void onTaskVanished(int taskId) {
        freeform.onTaskVanished(taskId);
        if (freeform.trackedTaskId() < 0) outsideGestures.clearAll();
    }

    void adjustMiniTargetIfNeeded(int animationType, Object info, Object target) {
        freeform.adjustMiniTargetIfNeeded(animationType, info, target);
    }

    void onMotion(MotionEvent event, Object inputMonitor) {
        try {
            handleMotion(event, inputMonitor);
        } catch (Throwable error) {
            resetFan();
            outsideGestures.onCancel();
            Log.e("Gesture event failed safely", error);
        }
    }

    private void handleMotion(MotionEvent event, Object inputMonitor) {
        int action = event.getActionMasked();
        int width = displayBounds().width();
        int height = displayBounds().height();
        float x = event.getX();
        float y = event.getY();

        if (triggerCapture.isInjectedEvent(event)) return;

        if (state != State.IDLE) {
            boolean replayedDown = action == MotionEvent.ACTION_DOWN
                    && gestureReplayGuard.isRepeatedDown(event.getDownTime());
            boolean staleTerminal = (action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_CANCEL)
                    && gestureReplayGuard.isStaleTerminal(
                    event.getDownTime(), event.getEventTime());
            if (replayedDown || staleTerminal) {
                Log.i("Ignored HyperOS replayed gesture boundary action=" + action
                        + " downTime=" + event.getDownTime()
                        + " eventTime=" + event.getEventTime());
                return;
            }
            gestureReplayGuard.record(event.getDownTime(), event.getEventTime());
        }

        if (action == MotionEvent.ACTION_DOWN) {
            requestOnDemandConfigReload();
            resetFan();
            float hotWidth = width * config.hotWidthPercent / 100f;
            float hotHeight = height * config.hotHeightPercent / 100f;
            Insets systemSides = systemSideGestureInsets();
            GestureGeometry.Corner downCorner = GestureGeometry.cornerAt(
                    x, y, width, height, hotWidth, hotHeight);
            float sideSafeTop = sideSafeTop(height);
            float sideSafeBottom = sideSafeBottom(height, hotHeight);
            GestureGeometry.Corner downSide = config.sideGestureEnabled && downCorner == null
                    ? GestureGeometry.sideAt(x, y, width, systemSides.left,
                    systemSides.right, sideSafeTop, sideSafeBottom, 96 * density)
                    : null;
            if (!triggerCapture.isCapturing() && canStart()) {
                triggerCapture.update(true, config.hotWidthPercent, config.hotHeightPercent,
                        0, 0);
            }
            Rect tracked = freeform.trackedBounds();
            if (downSide != null && canStart()) {
                if (tracked != null && !tracked.contains((int) x, (int) y)) {
                    Insets reserves = sideGestureReserves();
                    outsideGestures.onDown(x, y, tracked, width,
                            reserves.left, reserves.right, event.getEventTime());
                    sideTapIsOutsideWindow = true;
                } else {
                    outsideGestures.onCancel();
                }
                armSide(downSide, x, y, event.getDownTime(), event.getEventTime());
                Log.i("Side-distance gesture armed side=" + downSide
                        + " threshold=" + config.sideTriggerPercent + "% range="
                        + Math.round(sideSafeTop) + ".." + Math.round(sideSafeBottom));
                return;
            }
            if (tracked != null) {
                if (tracked.contains((int) x, (int) y)) return;
                if (downCorner != null && canStart()) {
                    Insets reserves = sideGestureReserves();
                    outsideGestures.onDown(x, y, tracked, width,
                            reserves.left, reserves.right, event.getEventTime());
                    if (triggerCapture.isCapturing()) {
                        pilfer(inputMonitor);
                        Log.i("Fan input claimed on down through corner capture window");
                    }
                    arm(downCorner, x, y, event.getDownTime(), event.getEventTime());
                    cornerTapIsOutsideWindow = true;
                    Log.i("Fan hot zone armed on down corner=" + downCorner);
                    return;
                }
                Insets reserves = sideGestureReserves();
                if (outsideGestures.onDown(x, y, tracked, width,
                        reserves.left, reserves.right, event.getEventTime())) {
                    pilfer(inputMonitor);
                }
                return;
            }
            if (downCorner != null && canStart()) {
                if (triggerCapture.isCapturing()) {
                    pilfer(inputMonitor);
                    Log.i("Fan input claimed on down through corner capture window");
                }
                arm(downCorner, x, y, event.getDownTime(), event.getEventTime());
                Log.i("Fan hot zone armed on down corner=" + downCorner);
            }
            return;
        }

        if (action == MotionEvent.ACTION_POINTER_DOWN || event.getPointerCount() > 1) {
            resetFan();
            outsideGestures.onCancel();
            return;
        }

        if (state == State.SIDE_ARMED && action == MotionEvent.ACTION_MOVE) {
            if (sideTapIsOutsideWindow) outsideGestures.onMove(x, y);
            float threshold = width * config.sideTriggerPercent / 100f;
            SideGestureArbitrator.Decision decision = sideGestureArbitrator.update(
                    corner, downX, downY, x, y, threshold,
                    sideDirectionSlop, sideVerticalFloor);
            if (decision == SideGestureArbitrator.Decision.PENDING) return;
            if (decision == SideGestureArbitrator.Decision.CANCELLED) {
                state = State.CANCELLED;
                Log.i("Side-distance candidate yielded to native input");
                return;
            }
            if (!pilfer(inputMonitor)) {
                state = State.CANCELLED;
                Log.i("Side-distance takeover unavailable; native back preserved");
                return;
            }
            outsideGestures.onCancel();
            activateSideList(x, y, width, height);
            Log.i("Side-distance gesture took over native back distance="
                    + Math.round(GestureGeometry.distance(downX, downY, x, y)));
            return;
        }

        if (state == State.ARMED && action == MotionEvent.ACTION_MOVE) {
            if (cornerTapIsOutsideWindow) outsideGestures.onMove(x, y);
            float distance = GestureGeometry.distance(downX, downY, x, y);
            GestureArbitrator.Decision decision = gestureArbitrator.update(
                    corner, downX, downY, x, y, directionDecisionDistance);
            if (decision == GestureArbitrator.Decision.PENDING) return;
            if (decision == GestureArbitrator.Decision.CANCELLED) {
                state = State.CANCELLED;
                Log.i("Fan input cancelled outside inward-upward fan direction");
                return;
            }
            if (!triggerCapture.isCapturing()) pilfer(inputMonitor);
            outsideGestures.onCancel();
            state = State.CLAIMED;
            Log.i("Fan input claimed after inward-upward direction distance="
                    + Math.round(distance) + " capture="
                    + (triggerCapture.isCapturing() ? "window" : "pilfer"));
            activateFanIfReady(distance, x, y, width, height);
            return;
        }

        if (state == State.CLAIMED && action == MotionEvent.ACTION_MOVE) {
            float distance = GestureGeometry.distance(downX, downY, x, y);
            activateFanIfReady(distance, x, y, width, height);
            return;
        }

        if (state == State.CANCELLED && action == MotionEvent.ACTION_MOVE) return;

        if (state == State.ACTIVE && action == MotionEvent.ACTION_MOVE) {
            if (activeSideList) {
                updateSideListSelection(x, y);
            } else {
                float selectionRadius = selectionRadius(width, height);
                updateSelection(x, y, width, height, selectionRadius,
                        iconDiameter(selectionRadius));
            }
            return;
        }

        if (state == State.IDLE && action == MotionEvent.ACTION_MOVE) {
            outsideGestures.onMove(x, y);
            return;
        }

        if (action == MotionEvent.ACTION_UP) {
            if (state == State.ACTIVE) {
                if (activeSideList) {
                    updateSideListSelection(x, y);
                } else {
                    float selectionRadius = selectionRadius(width, height);
                    updateSelection(x, y, width, height, selectionRadius,
                            iconDiameter(selectionRadius));
                }
                if (selected >= 0 && selected < targets.size()) {
                    RuntimeTarget target = targets.get(selected);
                    freeform.launch(target, config);
                    outsideGestures.clearAll();
                } else {
                    Log.i("Fan released without icon hit; launch cancelled");
                }
            } else if (state == State.IDLE) {
                outsideGestures.onUp(x, y, event.getEventTime(),
                        () -> pilfer(inputMonitor));
            } else if (state == State.ARMED && cornerTapIsOutsideWindow) {
                outsideGestures.onUp(x, y, event.getEventTime(),
                        () -> pilfer(inputMonitor));
            } else if (state == State.SIDE_ARMED && sideTapIsOutsideWindow) {
                outsideGestures.onUp(x, y, event.getEventTime(),
                        () -> pilfer(inputMonitor));
            } else if (state == State.ARMED && startedWithTriggerCapture
                    && GestureGeometry.distance(downX, downY, x, y)
                    <= directionDecisionDistance) {
                int displayId = context.getDisplay() == null
                        ? 0 : context.getDisplay().getDisplayId();
                triggerCapture.passthroughTap(downX, downY, displayId);
            }
            resetFan();
        } else if (action == MotionEvent.ACTION_CANCEL) {
            resetFan();
            outsideGestures.onCancel();
        }
    }

    private void activateFanIfReady(float distance, float x, float y, int width, int height) {
        float threshold = Math.min(width, height) * config.triggerPercent / 100f;
        if (distance >= threshold) {
            state = State.ACTIVE;
            activeSideList = false;
            float selectionRadius = selectionRadius(width, height);
            float iconDiameter = iconDiameter(selectionRadius);
            overlay.show(targets, corner, selectionRadius, iconDiameter, config.fanShadow);
            if (config.haptic) vibrateTick();
            updateSelection(x, y, width, height, selectionRadius, iconDiameter);
        }
    }

    private void activateSideList(float x, float y, int width, int height) {
        state = State.ACTIVE;
        activeSideList = true;
        float hotHeight = height * config.hotHeightPercent / 100f;
        float safeTop = sideSafeTop(height);
        float safeBottom = sideSafeBottom(height, hotHeight);
        float availablePerItem = (safeBottom - safeTop) / Math.max(1, targets.size());
        float iconDiameter = Math.min(config.sideIconSizeDp * density,
                Math.max(28 * density, availablePerItem - 4 * density));
        sideRowHeight = Math.min(iconDiameter + 10 * density, availablePerItem);
        sideListTop = GestureGeometry.sideListTop(downY, targets.size(),
                sideRowHeight, safeTop, safeBottom);
        sideActivationX = x;
        sideActivationY = y;
        sideSelectionReady = false;
        selected = -1;
        lastHapticSelection = -1;
        overlay.showSideList(targets, corner, sideListTop, sideRowHeight,
                iconDiameter, config.sideShowAppNames, config.fanShadow);
        if (config.haptic) vibrateTick();
        overlay.update(-1, x, y);
    }

    private void arm(GestureGeometry.Corner corner, float x, float y,
                     long downTime, long eventTime) {
        gestureArbitrator.reset();
        gestureReplayGuard.begin(downTime, eventTime);
        state = State.ARMED;
        this.corner = corner;
        downX = x;
        downY = y;
        startedWithTriggerCapture = triggerCapture.isCapturing();
    }

    private void armSide(GestureGeometry.Corner side, float x, float y,
                         long downTime, long eventTime) {
        sideGestureArbitrator.reset();
        gestureReplayGuard.begin(downTime, eventTime);
        state = State.SIDE_ARMED;
        corner = side;
        downX = x;
        downY = y;
    }

    private void updateSelection(float x, float y, int width, int height,
                                 float radius, float iconDiameter) {
        selected = GestureGeometry.selection(corner, x, y, width, height, targets.size(),
                radius, iconDiameter, 6 * density);
        overlay.update(selected, x, y);
    }

    private void updateSideListSelection(float x, float y) {
        if (!sideSelectionReady) {
            if (GestureGeometry.distance(sideActivationX, sideActivationY, x, y)
                    < sideDirectionSlop) {
                overlay.update(-1, x, y);
                return;
            }
            sideSelectionReady = true;
        }
        int next = GestureGeometry.sideListSelection(
                y, targets.size(), sideListTop, sideRowHeight);
        selected = next;
        if (config.haptic && next >= 0 && next != lastHapticSelection) {
            vibrateTick();
            lastHapticSelection = next;
        }
        overlay.update(selected, x, y);
    }

    private float selectionRadius(int width, int height) {
        float configured = Math.min(width, height) * config.selectionRadiusPercent / 100f;
        return GestureGeometry.effectiveRadius(targets.size(), configured,
                28 * density, 6 * density);
    }

    private float iconDiameter(float radius) {
        return GestureGeometry.effectiveIconDiameter(targets.size(), radius,
                config.iconSizeDp * density, 28 * density, 6 * density);
    }

    private void resetFan() {
        if (state == State.ACTIVE) overlay.hide();
        gestureArbitrator.reset();
        sideGestureArbitrator.reset();
        gestureReplayGuard.reset();
        state = State.IDLE;
        corner = null;
        selected = -1;
        startedWithTriggerCapture = false;
        cornerTapIsOutsideWindow = false;
        sideTapIsOutsideWindow = false;
        activeSideList = false;
        sideListTop = 0f;
        sideRowHeight = 0f;
        sideActivationX = 0f;
        sideActivationY = 0f;
        sideSelectionReady = false;
        lastHapticSelection = -1;
    }

    private boolean canStart() {
        if (!config.ready() || targets.size() < 3
                || (context.getDisplay() != null && context.getDisplay().getDisplayId() != 0)) return false;
        PowerManager power = context.getSystemService(PowerManager.class);
        KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
        return (power == null || power.isInteractive()) && (keyguard == null || !keyguard.isKeyguardLocked());
    }

    private void refreshTriggerCapture() {
        triggerCapture.update(canStart(), config.hotWidthPercent, config.hotHeightPercent,
                0, 0);
    }

    private float sideSafeTop(int height) {
        Insets safe = displaySafeInsets();
        return Math.max(safe.top, height * config.sideTopSafeMarginPercent / 100f);
    }

    private float sideSafeBottom(int height, float hotHeight) {
        Insets safe = displaySafeInsets();
        return Math.min(height - safe.bottom,
                height - hotHeight - 24 * density);
    }

    private Rect displayBounds() {
        WindowManager windowManager = context.getSystemService(WindowManager.class);
        return new Rect(windowManager.getCurrentWindowMetrics().getBounds());
    }

    private Insets sideGestureReserves() {
        int fallback = Math.round(32 * density);
        int extra = Math.round(8 * density);
        try {
            WindowMetrics metrics = context.getSystemService(WindowManager.class).getCurrentWindowMetrics();
            Insets gestures = metrics.getWindowInsets().getInsets(WindowInsets.Type.systemGestures());
            int left = gestures.left > 0 ? gestures.left + extra : fallback;
            int right = gestures.right > 0 ? gestures.right + extra : fallback;
            return Insets.of(left, 0, right, 0);
        } catch (Throwable error) {
            return Insets.of(fallback, 0, fallback, 0);
        }
    }

    private Insets systemSideGestureInsets() {
        int fallback = Math.round(32 * density);
        try {
            WindowMetrics metrics = context.getSystemService(WindowManager.class)
                    .getCurrentWindowMetrics();
            Insets gestures = metrics.getWindowInsets()
                    .getInsets(WindowInsets.Type.systemGestures());
            return Insets.of(gestures.left > 0 ? gestures.left : fallback, 0,
                    gestures.right > 0 ? gestures.right : fallback, 0);
        } catch (Throwable error) {
            return Insets.of(fallback, 0, fallback, 0);
        }
    }

    private Insets displaySafeInsets() {
        try {
            WindowMetrics metrics = context.getSystemService(WindowManager.class)
                    .getCurrentWindowMetrics();
            return metrics.getWindowInsets().getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        } catch (Throwable error) {
            return Insets.of(0, Math.round(48 * density), 0, Math.round(32 * density));
        }
    }

    private void performOutsideAction(boolean doubleTap) {
        int action = doubleTap ? config.outsideDoubleAction : config.outsideSingleAction;
        boolean handled;
        Log.i("Outside tap=" + (doubleTap ? "double" : "single") + " action=" + action);
        switch (action) {
            case ConfigContract.ACTION_CLOSE:
                handled = freeform.dismissTracked();
                break;
            case ConfigContract.ACTION_PIN:
                handled = freeform.miniTracked();
                break;
            case ConfigContract.ACTION_FULLSCREEN:
                handled = freeform.fullscreenTracked();
                break;
            default:
                Log.i("Outside " + (doubleTap ? "double" : "single") + " tap: no action");
                return;
        }
        if (handled) outsideGestures.clearAll();
    }

    private boolean pilfer(Object inputMonitor) {
        if (inputMonitor == null) return false;
        try {
            XposedHelpers.callMethod(inputMonitor, "pilferPointers");
            return true;
        } catch (Throwable error) {
            Log.e("Cannot pilfer input pointers", error);
            return false;
        }
    }

    private void vibrateTick() {
        try {
            VibrationEffect effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK);
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                VibratorManager manager = context.getSystemService(VibratorManager.class);
                if (manager != null) manager.getDefaultVibrator().vibrate(effect);
            } else {
                Vibrator vibrator = context.getSystemService(Vibrator.class);
                if (vibrator != null) vibrator.vibrate(effect);
            }
        } catch (Throwable error) {
            Log.e("Haptic feedback failed", error);
        }
    }

    private void reloadConfig() {
        try {
            Bundle bundle = context.getContentResolver().call(ConfigContract.URI, "get", null, null);
            GestureConfig next = GestureConfig.from(bundle);
            ArrayList<RuntimeTarget> resolved = new ArrayList<>();
            PackageManager packageManager = context.getPackageManager();
            for (ComponentName component : next.components) {
                try {
                    ActivityInfo info = packageManager.getActivityInfo(component, 0);
                    CharSequence label = info.loadLabel(packageManager);
                    resolved.add(new RuntimeTarget(component,
                            label == null ? component.getPackageName() : label.toString(),
                            info.loadIcon(packageManager)));
                } catch (Throwable error) {
                    Log.e("Configured activity is unavailable: " + component.flattenToShortString(), error);
                }
            }
            List<RuntimeTarget> nextTargets = Collections.unmodifiableList(resolved);
            configRetryCount = 0;
            configRetryScheduled = false;
            mainHandler.post(() -> {
                config = next;
                targets = nextTargets;
                freeform.updateConfig(next);
                if (!next.enabled || nextTargets.size() < 3) resetFan();
                refreshTriggerCapture();
                Log.i("Configuration loaded apps=" + nextTargets.size() + " trigger="
                        + next.triggerPercent + "% selection=" + next.selectionRadiusPercent
                        + "% hot=" + next.hotWidthPercent + "x" + next.hotHeightPercent
                        + "% icon=" + next.iconSizeDp + "dp side="
                        + next.sideGestureEnabled + "@" + next.sideTriggerPercent
                        + "% sideIcon=" + next.sideIconSizeDp + "dp safeTop="
                        + next.sideTopSafeMarginPercent + "% names="
                        + next.sideShowAppNames);
            });
        } catch (Throwable error) {
            Log.e("Cannot read module configuration", error);
            scheduleConfigRetry();
        } finally {
            configLoadQueued = false;
        }
    }

    private synchronized void requestConfigReload() {
        if (configLoadQueued) return;
        configLoadQueued = true;
        configHandler.post(this::reloadConfig);
    }

    private void scheduleConfigRetry() {
        if (configRetryScheduled || configRetryCount >= 5) return;
        long delay = Math.min(8000, 1000L << configRetryCount);
        configRetryCount++;
        configRetryScheduled = true;
        configHandler.postDelayed(() -> {
            configRetryScheduled = false;
            requestConfigReload();
        }, delay);
        Log.i("Configuration reload retry scheduled attempt=" + configRetryCount
                + " delayMs=" + delay);
    }

    private void requestOnDemandConfigReload() {
        if (!targets.isEmpty() || configRetryScheduled) return;
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastOnDemandConfigReload < 2000) return;
        lastOnDemandConfigReload = now;
        requestConfigReload();
    }

    private void reportStatusAsync(String value) {
        configHandler.post(() -> reportStatus(value));
    }

    private void reportStatus(String value) {
        try {
            Bundle extras = new Bundle();
            extras.putString(ConfigContract.KEY_INTERFACE_STATUS, value);
            context.getContentResolver().call(ConfigContract.URI, "report", null, extras);
        } catch (Throwable error) {
            Log.e("Cannot report interface status", error);
        }
    }
}
