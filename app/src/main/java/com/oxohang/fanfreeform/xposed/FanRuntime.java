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
    private enum State { IDLE, ARMED, CLAIMED, ACTIVE, YIELDED }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler configHandler;
    private final FanOverlayController overlay;
    private final HyperOsFreeformBridge freeform;
    private final OutsideGestureRecognizer outsideGestures;
    private final GestureArbitrator gestureArbitrator = new GestureArbitrator();
    private final float density;
    private final float directionDecisionDistance;
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

    FanRuntime(Context context, ClassLoader classLoader) {
        this.context = context;
        HandlerThread configThread = new HandlerThread("FanFreeformConfig");
        configThread.start();
        configHandler = new Handler(configThread.getLooper());
        density = context.getResources().getDisplayMetrics().density;
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        directionDecisionDistance = Math.max(viewConfiguration.getScaledTouchSlop(), 10 * density);
        overlay = new FanOverlayController(context, mainHandler);
        freeform = new HyperOsFreeformBridge(context, classLoader, mainHandler);
        outsideGestures = new OutsideGestureRecognizer(mainHandler,
                viewConfiguration, this::performOutsideAction);
        requestConfigReload();
        context.getContentResolver().registerContentObserver(ConfigContract.URI, false,
                new ContentObserver(mainHandler) {
                    @Override public void onChange(boolean selfChange) { requestConfigReload(); }
                });
        try {
            context.registerReceiver(new BroadcastReceiver() {
                @Override public void onReceive(Context receiverContext, Intent intent) {
                    configHandler.post(() -> {
                        configRetryCount = 0;
                        configRetryScheduled = false;
                        requestConfigReload();
                        if (freeform.hasController()) {
                            reportStatus("HyperOS 3 原生接口已连接");
                        }
                    });
                }
            }, new IntentFilter(Intent.ACTION_USER_UNLOCKED), Context.RECEIVER_NOT_EXPORTED);
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

        if (action == MotionEvent.ACTION_DOWN) {
            requestOnDemandConfigReload();
            resetFan();
            float hotWidth = width * config.hotWidthPercent / 100f;
            float hotHeight = height * config.hotHeightPercent / 100f;
            GestureGeometry.Corner downCorner = GestureGeometry.cornerAt(
                    x, y, width, height, hotWidth, hotHeight);
            Rect tracked = freeform.trackedBounds();
            if (tracked != null) {
                if (tracked.contains((int) x, (int) y)) return;
                if (downCorner != null && canStart()) {
                    outsideGestures.onCancel();
                    pilfer(inputMonitor);
                    arm(downCorner, x, y);
                    Log.i("Fan hot zone claimed on down corner=" + downCorner);
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
                pilfer(inputMonitor);
                arm(downCorner, x, y);
                Log.i("Fan hot zone claimed on down corner=" + downCorner);
            }
            return;
        }

        if (action == MotionEvent.ACTION_POINTER_DOWN || event.getPointerCount() > 1) {
            resetFan();
            outsideGestures.onCancel();
            return;
        }

        if (state == State.ARMED && action == MotionEvent.ACTION_MOVE) {
            float distance = GestureGeometry.distance(downX, downY, x, y);
            GestureArbitrator.Decision decision = gestureArbitrator.update(
                    downX, downY, x, y, directionDecisionDistance);
            if (decision == GestureArbitrator.Decision.PENDING) return;
            if (decision == GestureArbitrator.Decision.SYSTEM) {
                state = State.YIELDED;
                Log.i("Fan input cancelled by direction gate after hot-zone claim");
                return;
            }
            state = State.CLAIMED;
            Log.i("Fan direction accepted after upward arbitration distance="
                    + Math.round(distance));
            activateFanIfReady(distance, x, y, width, height);
            return;
        }

        if (state == State.CLAIMED && action == MotionEvent.ACTION_MOVE) {
            float distance = GestureGeometry.distance(downX, downY, x, y);
            activateFanIfReady(distance, x, y, width, height);
            return;
        }

        if (state == State.YIELDED && action == MotionEvent.ACTION_MOVE) return;

        if (state == State.ACTIVE && action == MotionEvent.ACTION_MOVE) {
            float selectionRadius = selectionRadius(width, height);
            updateSelection(x, y, width, height, selectionRadius, iconDiameter(selectionRadius));
            return;
        }

        if (state == State.IDLE && action == MotionEvent.ACTION_MOVE) {
            outsideGestures.onMove(x, y);
            return;
        }

        if (action == MotionEvent.ACTION_UP) {
            if (state == State.ACTIVE) {
                float selectionRadius = selectionRadius(width, height);
                updateSelection(x, y, width, height, selectionRadius, iconDiameter(selectionRadius));
                if (selected >= 0 && selected < targets.size()) {
                    RuntimeTarget target = targets.get(selected);
                    freeform.launch(target, config);
                    outsideGestures.clearAll();
                } else {
                    Log.i("Fan released without icon hit; launch cancelled");
                }
            } else if (state == State.IDLE) {
                outsideGestures.onUp(x, y, event.getEventTime());
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
            float selectionRadius = selectionRadius(width, height);
            float iconDiameter = iconDiameter(selectionRadius);
            overlay.show(targets, corner, selectionRadius, iconDiameter);
            if (config.haptic) vibrateTick();
            updateSelection(x, y, width, height, selectionRadius, iconDiameter);
        }
    }

    private void arm(GestureGeometry.Corner corner, float x, float y) {
        gestureArbitrator.reset();
        state = State.ARMED;
        this.corner = corner;
        downX = x;
        downY = y;
    }

    private void updateSelection(float x, float y, int width, int height,
                                 float radius, float iconDiameter) {
        selected = GestureGeometry.selection(corner, x, y, width, height, targets.size(),
                radius, iconDiameter, 6 * density);
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
        state = State.IDLE;
        corner = null;
        selected = -1;
    }

    private boolean canStart() {
        if (!config.ready() || targets.size() < 3
                || (context.getDisplay() != null && context.getDisplay().getDisplayId() != 0)) return false;
        PowerManager power = context.getSystemService(PowerManager.class);
        KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
        return (power == null || power.isInteractive()) && (keyguard == null || !keyguard.isKeyguardLocked());
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

    private void pilfer(Object inputMonitor) {
        if (inputMonitor == null) return;
        try {
            XposedHelpers.callMethod(inputMonitor, "pilferPointers");
        } catch (Throwable error) {
            Log.e("Cannot pilfer input pointers", error);
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
                if (!next.enabled || nextTargets.size() < 3) resetFan();
                Log.i("Configuration loaded apps=" + nextTargets.size() + " trigger="
                        + next.triggerPercent + "% selection=" + next.selectionRadiusPercent
                        + "% hot=" + next.hotWidthPercent + "x" + next.hotHeightPercent
                        + "% icon=" + next.iconSizeDp + "dp");
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
