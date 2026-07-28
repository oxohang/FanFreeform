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
    private final OutsideTouchCapture outsideCapture;
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
    private boolean sideBackRecognized;
    private boolean sideSequenceCaptured;
    private boolean sideListAllowed;
    private boolean imeDismissTap;
    private boolean capturedImeDismissTap;
    private long capturedNativeEdgeDownTime = -1L;
    private long ignoredCaptureDownTime = -1L;
    private boolean activeSideList;
    private float sideListCenterX;
    private float sideListTop;
    private float sideRowHeight;
    private float sideIconDiameter;
    private float sideListHitWidth;
    private float sideListActivationX;
    private float sideReverseCancelDistance;
    private boolean sideListEntered;
    private volatile boolean wheelSessionActive;
    private volatile boolean imeVisible;
    private volatile int imeHeight;
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
        outsideCapture = new OutsideTouchCapture(context, mainHandler,
                this::onCapturedOutsideTouch);
        freeform = new HyperOsFreeformBridge(context, classLoader, mainHandler);
        outsideGestures = new OutsideGestureRecognizer(mainHandler,
                viewConfiguration, density, this::performOutsideAction);
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
                    if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                        wheelSessionActive = false;
                        mainHandler.post(overlay::removeNow);
                        outsideCapture.remove();
                    }
                    if (Intent.ACTION_SCREEN_ON.equals(action)) {
                        mainHandler.postDelayed(FanRuntime.this::refreshTriggerCapture, 750L);
                        mainHandler.postDelayed(FanRuntime.this::refreshTriggerCapture, 1500L);
                        mainHandler.postDelayed(FanRuntime.this::refreshOutsideCapture, 750L);
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
        mainHandler.postDelayed(this::refreshOutsideCapture, 120L);
        reportStatusAsync("HyperOS 3 原生接口已连接");
    }

    void reportInputReady() {
        reportStatusAsync(freeform.hasController()
                ? "HyperOS 3 原生接口已连接" : "手势接口已连接，等待小窗控制器");
    }

    void onTaskAppeared(int taskId) {
        freeform.onTaskAppeared(taskId);
        mainHandler.postDelayed(this::refreshOutsideCapture, 60L);
    }

    void onTaskInfo(Object info) {
        freeform.onTaskInfo(info);
        refreshOutsideCapture();
        // HyperOS reports the normal state before the mini-to-freeform animation has
        // committed its final bounds. Re-read the live bounds while that animation settles.
        mainHandler.postDelayed(this::refreshOutsideCapture, 100L);
        mainHandler.postDelayed(this::refreshOutsideCapture, 320L);
        mainHandler.postDelayed(this::refreshOutsideCapture, 760L);
        if (freeform.trackedTaskId() < 0 && freeform.pendingLaunchBounds() == null) {
            outsideGestures.clearAll();
        }
    }

    void onTaskVanished(int taskId) {
        freeform.onTaskVanished(taskId);
        refreshOutsideCapture();
        if (freeform.trackedTaskId() < 0 && freeform.pendingLaunchBounds() == null) {
            outsideGestures.clearAll();
        }
    }

    void onImeVisibilityChanged(boolean visible, int height) {
        imeVisible = visible;
        imeHeight = visible ? Math.max(0, height) : 0;
        if (visible && (activeSideList || state == State.SIDE_ARMED)) {
            resetFan();
            Log.i("Side application gesture cancelled because input method became visible");
        }
        Log.i("IME visibility changed visible=" + visible + " height=" + imeHeight);
        refreshOutsideCapture();
        mainHandler.postDelayed(this::refreshOutsideCapture, 60L);
        mainHandler.postDelayed(this::refreshOutsideCapture, 180L);
        mainHandler.postDelayed(this::refreshOutsideCapture, 420L);
        mainHandler.postDelayed(this::refreshOutsideCapture, 800L);
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
        if (wheelSessionActive || overlay.isWheelVisible()) return;
        boolean nativeSideEdge = action == MotionEvent.ACTION_DOWN
                && GestureGeometry.sideAt(x, width, systemSideGestureInsets().left,
                systemSideGestureInsets().right) != null;
        if (action == MotionEvent.ACTION_DOWN && outsideCapture.captures(x, y)
                && !nativeSideEdge) {
            ignoredCaptureDownTime = event.getDownTime();
            return;
        }
        if (event.getDownTime() == ignoredCaptureDownTime) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                ignoredCaptureDownTime = -1L;
            }
            return;
        }

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
            GestureGeometry.Corner systemEdgeSide = downCorner == null
                    ? GestureGeometry.sideAt(x, width, systemSides.left, systemSides.right)
                    : null;
            GestureGeometry.Corner listSide = config.sideGestureEnabled && downCorner == null
                    ? GestureGeometry.sideAt(x, y, width, systemSides.left,
                    systemSides.right, sideSafeTop, sideSafeBottom, 96 * density)
                    : null;
            GestureGeometry.Corner downSide = listSide;
            if (!triggerCapture.isCapturing() && canStart()) {
                triggerCapture.update(true, config.hotWidthPercent, config.hotHeightPercent,
                        0, 0);
            }
            Rect pendingLaunch = freeform.pendingLaunchBounds();
            Rect tracked = freeform.trackedBounds();
            Rect ime = tracked == null ? null : visibleImeBounds();
            boolean freeformActive = pendingLaunch != null || tracked != null
                    || freeform.hasInteractiveFreeform();
            if (pendingLaunch != null) {
                if (GestureGeometry.outsideRegion(x, y, pendingLaunch.left,
                        pendingLaunch.top, pendingLaunch.right, pendingLaunch.bottom)
                        == GestureGeometry.OutsideRegion.OUTSIDE) {
                    Insets reserves = sideGestureReserves();
                    if (outsideGestures.onDown(x, y, pendingLaunch, width,
                            reserves.left, reserves.right, event.getEventTime())) {
                        pilfer(inputMonitor);
                    }
                    Log.i("Startup outside tap armed bounds=" + pendingLaunch);
                }
                return;
            }
            boolean outsideTracked = tracked != null
                    && !insideTrackedOrIme(x, y, tracked, ime);
            boolean imeActive = imeVisible || ime != null;
            if (imeActive || freeformActive) downSide = null;
            if (outsideTracked && imeActive) {
                imeDismissTap = true;
                if (systemEdgeSide == null) pilfer(inputMonitor);
            }
            if (outsideTracked && systemEdgeSide != null && canStart()) {
                Insets reserves = sideGestureReserves();
                outsideGestures.onDown(x, y, tracked, width,
                        reserves.left, reserves.right, event.getEventTime());
                sideTapIsOutsideWindow = true;
                sideSequenceCaptured = false;
                sideListAllowed = !imeActive && !freeformActive && listSide != null;
                armSide(systemEdgeSide, x, y, event.getDownTime(), event.getEventTime());
                Log.i("Captured outside side sequence side=" + systemEdgeSide
                        + " list=" + sideListAllowed);
                return;
            }
            if (downSide != null && canStart()) {
                if (outsideTracked) {
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
                if (!outsideTracked) return;
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
            if (sideTapIsOutsideWindow && GestureGeometry.isIntentionalSideSwipe(
                    corner, downX, downY, x, y, directionDecisionDistance)) {
                sideBackRecognized = true;
            }
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
            if (!sideListAllowed && sideTapIsOutsideWindow) {
                sideBackRecognized = true;
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
                    if (state == State.CANCELLED) {
                        resetFan();
                        return;
                    }
                } else {
                    float selectionRadius = selectionRadius(width, height);
                    updateSelection(x, y, width, height, selectionRadius,
                            iconDiameter(selectionRadius));
                }
                if (selected >= 0 && selected < targets.size()) {
                    if (activeSideList && config.sideWheelMode) {
                        showPersistentWheel();
                        resetFan(false);
                        return;
                    } else if (!activeSideList) {
                        // 底部斜滑：先播弹出+旋转动画，再启动应用
                        RuntimeTarget target = targets.get(selected);
                        int launchIdx = selected;
                        overlay.animateLaunch(launchIdx, () -> {
                            freeform.launch(target, config, FanRuntime.this::refreshOutsideCapture);
                            refreshOutsideCaptureAfterLaunch();
                            outsideGestures.clearAll();
                            resetFan();
                        });
                        return; // 跳过底部 resetFan，由动画回调处理
                    } else {
                        RuntimeTarget target = targets.get(selected);
                        freeform.launch(target, config, this::refreshOutsideCapture);
                        refreshOutsideCaptureAfterLaunch();
                        outsideGestures.clearAll();
                    }
                } else {
                    Log.i("Fan released without icon hit; launch cancelled");
                }
            } else if (state == State.IDLE) {
                finishOutsideTap(x, y, event.getEventTime(), inputMonitor);
            } else if (state == State.ARMED && cornerTapIsOutsideWindow) {
                finishOutsideTap(x, y, event.getEventTime(), inputMonitor);
            } else if (state == State.SIDE_ARMED && sideTapIsOutsideWindow) {
                if (sideBackRecognized) {
                    outsideGestures.onCancel();
                    if (sideSequenceCaptured) {
                        int displayId = context.getDisplay() == null
                                ? 0 : context.getDisplay().getDisplayId();
                        triggerCapture.dispatchBack(displayId);
                    }
                } else {
                    if (!sideSequenceCaptured) pilfer(inputMonitor);
                    finishOutsideTap(x, y, event.getEventTime(), inputMonitor);
                }
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
        sideIconDiameter = Math.min(config.sideIconSizeDp * density,
                Math.max(28 * density, availablePerItem - 4 * density));
        sideRowHeight = Math.min(sideIconDiameter + 10 * density, availablePerItem);
        int anchor = (targets.size() - 1) / 2;
        sideListTop = GestureGeometry.sideListTopForAnchor(y, targets.size(), anchor,
                sideRowHeight, safeTop, safeBottom);
        sideListCenterX = GestureGeometry.sideListCenterX(corner, x, width,
                sideIconDiameter, 14 * density, config.sideFollowFinger);
        sideListHitWidth = Math.max(sideIconDiameter + 32 * density, 72 * density);
        sideListActivationX = x;
        sideReverseCancelDistance = width * config.sideReverseCancelPercent / 100f;
        sideListEntered = false;
        selected = -1;
        lastHapticSelection = -1;
        overlay.showSideList(targets, corner, sideListCenterX, sideListTop,
                sideRowHeight, sideIconDiameter, config.sideShowAppNames, config.fanShadow);
        if (config.haptic) vibrateTick();
        overlay.update(selected, x, y);
        Log.i("Side list shown center=" + Math.round(sideListCenterX) + ","
                + Math.round(sideListTop + (anchor + 0.5f) * sideRowHeight)
                + " selected=" + selected + " follow=" + config.sideFollowFinger
                + " wheel=" + config.sideWheelMode);
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
        int next = GestureGeometry.selection(corner, x, y, width, height, targets.size(),
                radius, iconDiameter, 6 * density);
        if (config.haptic && next >= 0 && next != lastHapticSelection) {
            vibrateTick();
        }
        lastHapticSelection = next;
        selected = next;
        overlay.update(selected, x, y);
    }

    private void updateSideListSelection(float x, float y) {
        float reverseDistance = GestureGeometry.sideReverseDistance(
                corner, sideListActivationX, x);
        if (reverseDistance > 0f) {
            selected = -1;
            float opacity = GestureGeometry.sideListOpacity(
                    reverseDistance, sideReverseCancelDistance);
            overlay.update(-1, x, y);
            overlay.setOpacity(opacity);
            if (reverseDistance >= sideReverseCancelDistance) {
                state = State.CANCELLED;
                overlay.hide();
                Log.i("Side list cancelled at reverse distance="
                        + Math.round(reverseDistance));
            }
            return;
        }
        overlay.setOpacity(1f);
        boolean inside = GestureGeometry.insideSideList(x, y, sideListCenterX,
                sideListHitWidth, targets.size(), sideListTop, sideRowHeight);
        boolean terminalExit = GestureGeometry.outsideSideListVertically(
                y, targets.size(), sideListTop, sideRowHeight)
                || GestureGeometry.beyondSideListInward(
                corner, x, sideListCenterX, sideListHitWidth);
        if (!inside && sideListEntered && terminalExit) {
            selected = -1;
            state = State.CANCELLED;
            overlay.hide();
            Log.i("Side list cancelled after pointer left its bounds");
            return;
        }
        if (!inside) {
            selected = -1;
            overlay.update(-1, x, y);
            return;
        }
        sideListEntered = true;
        int next = GestureGeometry.sideListSelection(
                y, targets.size(), sideListTop, sideRowHeight);
        selected = next;
        if (config.haptic && next >= 0 && next != lastHapticSelection) {
            vibrateTick();
            lastHapticSelection = next;
        }
        overlay.update(selected, x, y);
    }

    private void showPersistentWheel() {
        int initialSelection = selected;
        float wheelCenterY = sideListTop + (initialSelection + 0.5f) * sideRowHeight;
        List<RuntimeTarget> wheelTargets = targets;
        GestureGeometry.Corner wheelSide = corner;
        wheelSessionActive = true;
        overlay.showWheel(wheelTargets, wheelSide, sideListCenterX, wheelCenterY,
                sideRowHeight, sideIconDiameter, initialSelection,
                config.sideShowAppNames, config.fanShadow,
                new FanOverlayController.WheelListener() {
                    @Override public void onLaunch(int index) {
                        wheelSessionActive = false;
                        if (index < 0 || index >= wheelTargets.size()) return;
                        freeform.launch(wheelTargets.get(index), config,
                                FanRuntime.this::refreshOutsideCapture);
                        refreshOutsideCaptureAfterLaunch();
                        outsideGestures.clearAll();
                        Log.i("Side wheel launched index=" + index);
                    }

                    @Override public void onDismiss() {
                        wheelSessionActive = false;
                        Log.i("Side wheel dismissed");
                    }

                    @Override public void onSelectionChanged(int index) {
                        if (config.haptic) vibrateTick();
                    }
                });
        Log.i("Side wheel retained selected=" + initialSelection);
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
        resetFan(true);
    }

    private void resetFan(boolean hideOverlay) {
        if (hideOverlay && state == State.ACTIVE) overlay.hide();
        gestureArbitrator.reset();
        sideGestureArbitrator.reset();
        gestureReplayGuard.reset();
        state = State.IDLE;
        corner = null;
        selected = -1;
        startedWithTriggerCapture = false;
        cornerTapIsOutsideWindow = false;
        sideTapIsOutsideWindow = false;
        sideBackRecognized = false;
        sideSequenceCaptured = false;
        sideListAllowed = false;
        imeDismissTap = false;
        activeSideList = false;
        sideListCenterX = 0f;
        sideListTop = 0f;
        sideRowHeight = 0f;
        sideIconDiameter = 0f;
        sideListHitWidth = 0f;
        sideListActivationX = 0f;
        sideReverseCancelDistance = 0f;
        sideListEntered = false;
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

    private void refreshOutsideCaptureAfterLaunch() {
        refreshOutsideCapture();
        mainHandler.postDelayed(this::refreshOutsideCapture, 80L);
        mainHandler.postDelayed(this::refreshOutsideCapture, 240L);
    }

    private void refreshOutsideCapture() {
        Rect pending = freeform.pendingLaunchBounds();
        Rect tracked = freeform.trackedBounds();
        List<Rect> visibleWindows = freeform.visibleFreeformBounds();
        if (pending != null && !visibleWindows.contains(pending)) {
            visibleWindows.add(pending);
        }
        boolean hasInteractiveWindow = freeform.hasInteractiveFreeform();
        if (pending == null && !hasInteractiveWindow) visibleWindows.clear();
        Rect ime = hasInteractiveWindow ? visibleImeBounds() : null;
        if (ime == null && !visibleWindows.isEmpty() && triggerCapture.isCapturing()) {
            Rect display = displayBounds();
            int hotWidth = Math.round(display.width() * config.hotWidthPercent / 100f);
            int hotHeight = Math.round(display.height() * config.hotHeightPercent / 100f);
            visibleWindows.add(new Rect(display.left, display.bottom - hotHeight,
                    display.left + hotWidth, display.bottom));
            visibleWindows.add(new Rect(display.right - hotWidth,
                    display.bottom - hotHeight, display.right, display.bottom));
        }
        // Edge DOWN events must be captured too; waiting until UP to pilfer lets the
        // application underneath receive a real click. Side swipes are handled below.
        outsideCapture.update(visibleWindows, ime, 0, 0);
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

    private Rect visibleImeBounds() {
        try {
            WindowMetrics metrics = context.getSystemService(WindowManager.class)
                    .getCurrentWindowMetrics();
            WindowInsets windowInsets = metrics.getWindowInsets();
            HyperOsFreeformBridge.ImeState shellIme = freeform.inputMethodState();
            boolean insetsVisible = windowInsets.isVisible(WindowInsets.Type.ime());
            boolean visible = shellIme.known ? shellIme.visible
                    : (insetsVisible || imeVisible);
            if (!visible) return null;
            Insets ime = windowInsets.getInsets(WindowInsets.Type.ime());
            Rect display = new Rect(metrics.getBounds());
            int visibleHeight = Math.max(ime.bottom,
                    Math.max(shellIme.height, imeHeight));
            if (visibleHeight <= 0 || visibleHeight >= display.height()) return null;
            return new Rect(display.left, display.bottom - visibleHeight,
                    display.right, display.bottom);
        } catch (Throwable error) {
            HyperOsFreeformBridge.ImeState shellIme = freeform.inputMethodState();
            boolean visible = shellIme.known ? shellIme.visible : imeVisible;
            int height = Math.max(shellIme.height, imeHeight);
            if (!visible || height <= 0) return null;
            Rect display = displayBounds();
            if (height >= display.height()) return null;
            return new Rect(display.left, display.bottom - height,
                    display.right, display.bottom);
        }
    }

    private static boolean insideTrackedOrIme(float x, float y, Rect tracked, Rect ime) {
        return GestureGeometry.insideEither(x, y,
                tracked.left, tracked.top, tracked.right, tracked.bottom,
                ime == null ? 0 : ime.left,
                ime == null ? 0 : ime.top,
                ime == null ? 0 : ime.right,
                ime == null ? 0 : ime.bottom,
                ime != null);
    }

    private void finishOutsideTap(float x, float y, long eventTime, Object inputMonitor) {
        Runnable claimInput = () -> pilfer(inputMonitor);
        if (imeDismissTap) {
            outsideGestures.onUpImmediate(x, y, eventTime, claimInput, () -> {
                outsideGestures.clearAll();
                freeform.hideInputMethodIfNeeded();
                Log.i("Outside tap consumed to hide current input method");
            });
            return;
        }
        outsideGestures.onUp(x, y, eventTime, claimInput);
    }

    private void onCapturedOutsideTouch(int action, float x, float y,
                                        long downTime, long eventTime) {
        int width = displayBounds().width();
        if (action == MotionEvent.ACTION_DOWN) {
            Insets systemSides = systemSideGestureInsets();
            if (GestureGeometry.sideAt(x, width,
                    systemSides.left, systemSides.right) != null) {
                capturedNativeEdgeDownTime = downTime;
                Log.i("Outside guard consumed redirected native-edge stream");
                return;
            }
            ignoredCaptureDownTime = downTime;
            capturedImeDismissTap = false;
            Rect ime = visibleImeBounds();
            boolean imeActive = imeVisible || ime != null;
            Rect pending = freeform.pendingLaunchBounds();
            if (pending != null) {
                outsideGestures.onDown(x, y, pending, width, 0, 0, eventTime);
                return;
            }
            Rect tracked = freeform.trackedBounds();
            if (tracked == null) {
                outsideGestures.onCancel();
                return;
            }
            if (insideTrackedOrIme(x, y, tracked, ime)) {
                outsideGestures.onCancel();
                return;
            }
            capturedImeDismissTap = imeActive;
            outsideGestures.onDown(x, y, tracked, width, 0, 0, eventTime);
            return;
        }
        if (downTime == capturedNativeEdgeDownTime) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                capturedNativeEdgeDownTime = -1L;
            }
            return;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            outsideGestures.onMove(x, y);
            return;
        }
        if (action == MotionEvent.ACTION_UP) {
            if (capturedImeDismissTap) {
                outsideGestures.onUpImmediate(x, y, eventTime, () -> {}, () -> {
                    outsideGestures.clearAll();
                    freeform.hideInputMethodIfNeeded();
                    mainHandler.postDelayed(this::refreshOutsideCapture, 80L);
                    mainHandler.postDelayed(this::refreshOutsideCapture, 260L);
                    mainHandler.postDelayed(this::refreshOutsideCapture, 700L);
                    Log.i("Captured outside tap consumed to hide current input method");
                });
            } else {
                outsideGestures.onUp(x, y, eventTime, () -> {});
            }
            clearCapturedOutsideState();
            return;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            clearCapturedOutsideState();
            outsideGestures.onCancel();
        }
    }

    private void clearCapturedOutsideState() {
        capturedImeDismissTap = false;
    }

    private void performOutsideAction(boolean doubleTap) {
        int action = doubleTap ? config.outsideDoubleAction : config.outsideSingleAction;
        boolean handled;
        Log.i("Outside tap=" + (doubleTap ? "double" : "single") + " action=" + action);
        switch (action) {
            case ConfigContract.ACTION_CLOSE:
                handled = freeform.dismissTrackedOrPendingLaunch();
                break;
            case ConfigContract.ACTION_PIN:
                handled = freeform.miniTrackedOrPendingLaunch();
                break;
            case ConfigContract.ACTION_FULLSCREEN:
                handled = freeform.fullscreenTrackedOrPendingLaunch();
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
            int componentCount = Math.min(next.components.size(), next.shortcutFlags.size());
            for (int idx = 0; idx < next.components.size(); idx++) {
                ComponentName component = next.components.get(idx);
                boolean isShortcut = idx < next.shortcutFlags.size() && next.shortcutFlags.get(idx);
                try {
                    ActivityInfo info = packageManager.getActivityInfo(component, 0);
                    CharSequence label = info.loadLabel(packageManager);
                    resolved.add(new RuntimeTarget(component,
                            label == null ? component.getPackageName() : label.toString(),
                            info.loadIcon(packageManager), isShortcut));
                } catch (Throwable error) {
                    Log.e("Configured activity is unavailable: " + component.flattenToShortString(), error);
                }
            }
            List<RuntimeTarget> nextTargets = Collections.unmodifiableList(resolved);
            configRetryCount = 0;
            configRetryScheduled = false;
            mainHandler.post(() -> {
                if (wheelSessionActive || overlay.isWheelVisible()) {
                    wheelSessionActive = false;
                    overlay.removeNow();
                }
                config = next;
                targets = nextTargets;
                freeform.updateConfig(next);
                if (!next.enabled || nextTargets.size() < 3) resetFan();
                refreshTriggerCapture();
                refreshOutsideCapture();
                Log.i("Configuration loaded apps=" + nextTargets.size() + " trigger="
                        + next.triggerPercent + "% selection=" + next.selectionRadiusPercent
                        + "% hot=" + next.hotWidthPercent + "x" + next.hotHeightPercent
                        + "% icon=" + next.iconSizeDp + "dp side="
                        + next.sideGestureEnabled + "@" + next.sideTriggerPercent
                        + "% sideIcon=" + next.sideIconSizeDp + "dp safeTop="
                        + next.sideTopSafeMarginPercent + "% names="
                        + next.sideShowAppNames + " follow=" + next.sideFollowFinger
                        + " wheel=" + next.sideWheelMode + " reverseCancel="
                        + next.sideReverseCancelPercent + "%");
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
