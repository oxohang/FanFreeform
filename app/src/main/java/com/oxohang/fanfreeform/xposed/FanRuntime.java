package com.oxohang.fanfreeform.xposed;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
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
import com.oxohang.fanfreeform.config.ShortcutIconLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XposedHelpers;

final class FanRuntime {
    private static final long BOTTOM_HONEYCOMB_SETTLE_MS = 80L;
    private static final float BOTTOM_HONEYCOMB_SETTLE_DP = 8f;
    private static final float BOTTOM_HONEYCOMB_FAN_GAP_DP = 10f;
    private enum State {
        IDLE, ARMED, SIDE_ARMED, CLAIMED, ACTIVE, HONEYCOMB,
        TASKS_LOADING, TASKS, CANCELLED
    }

    private final Context context;
    private final WindowManager windowManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler configHandler;
    private final FanOverlayController overlay;
    private final HoneycombOverlayController honeycombOverlay;
    private final TaskSwitcherOverlayController taskSwitcherOverlay;
    private final RecentTaskRepository recentTaskRepository;
    private final SystemRecentsLauncher systemRecentsLauncher;
    private final FullscreenAppLauncher fullscreenLauncher;
    private final FanTriggerCapture triggerCapture;
    private final OutsideTouchCapture outsideCapture;
    private final HyperOsFreeformBridge freeform;
    private final OutsideGestureRecognizer outsideGestures;
    private final GestureArbitrator gestureArbitrator = new GestureArbitrator();
    private final SideGestureArbitrator sideGestureArbitrator = new SideGestureArbitrator();
    private final GestureReplayGuard gestureReplayGuard = new GestureReplayGuard();
    private final HoneycombGestureState honeycombGestureState = new HoneycombGestureState();
    private final float density;
    private final float directionDecisionDistance;
    private final float sideDirectionSlop;
    private final float sideVerticalFloor;
    private final Rect cachedDisplayBounds = new Rect();
    private Insets cachedDisplaySafeInsets = Insets.NONE;
    private Insets cachedSideGestureReserves = Insets.NONE;
    private Insets cachedSystemSideGestureInsets = Insets.NONE;
    private int cachedStatusBarGestureHeight;
    private volatile GestureConfig config = GestureConfig.defaults();
    private volatile List<RuntimeTarget> targets = Collections.emptyList();
    private volatile List<RuntimeTarget> honeycombTargets = Collections.emptyList();
    private volatile List<RuntimeTarget> sideTargets = Collections.emptyList();
    private List<RuntimeTarget> activeTargets = Collections.emptyList();
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
    private long ignoredCaptureDownTime = -1L;
    private long systemPanelInputDownTime = -1L;
    private boolean activeSideList;
    private boolean activeSideFanList;
    private boolean activeSideRingList;
    private float sideListCenterX;
    private float sideListTop;
    private float sideRowHeight;
    private float sideIconDiameter;
    private float sideListHitWidth;
    private float sideListActivationX;
    private float sideReverseCancelDistance;
    private float sideFanRadius;
    private float sideFanCenterY;
    private float sideRingRadius;
    private float sideRingCenterX;
    private float sideRingCenterY;
    private boolean sideListEntered;
    private boolean sideHoldScheduled;
    private float sideHoldAnchorX;
    private float sideHoldAnchorY;
    private float sideHoldCurrentX;
    private float sideHoldCurrentY;
    private Object sideHoldInputMonitor;
    private Handler sideHoldHandler;
    private final Runnable sideHoldTrigger = this::triggerHeldSideList;
    private boolean bottomHoneycombSettleScheduled;
    private float bottomHoneycombSettleAnchorX;
    private float bottomHoneycombSettleAnchorY;
    private float bottomHoneycombCurrentX;
    private float bottomHoneycombCurrentY;
    private int bottomHoneycombDisplayWidth;
    private int bottomHoneycombDisplayHeight;
    private Handler bottomHoneycombSettleHandler;
    private final Runnable bottomHoneycombSettleTrigger = this::triggerSettledBottomHoneycomb;
    private volatile boolean wheelSessionActive;
    private volatile boolean imeVisible;
    private volatile int imeHeight;
    private volatile boolean shadeExpanded;
    private volatile boolean controlCenterExpanded;
    private long shadeStateVersion;
    private long controlCenterStateVersion;
    private long authoritativePanelStateVersion;
    private volatile long systemPanelTouchBlockUntil;
    private int lastHapticSelection = -1;
    private float activeHoneycombReturnThreshold;
    private boolean geometryRefreshQueued;
    private long lastGeometryRefreshUptime;
    private boolean outsideCaptureWindowTransitionBlocked;
    private long outsideCaptureWindowTransitionGeneration;
    private long systemRecentsWatchVersion;
    private int taskLoadGeneration;
    private float taskLoadCurrentX;
    private float taskLoadCurrentY;

    FanRuntime(Context context, ClassLoader classLoader) {
        this.context = context;
        Log.attachReporter(context);
        HandlerThread configThread = new HandlerThread("FanFreeformConfig");
        configThread.start();
        configHandler = new Handler(configThread.getLooper());
        density = context.getResources().getDisplayMetrics().density;
        windowManager = context.getSystemService(WindowManager.class);
        refreshDisplayGeometry();
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        directionDecisionDistance = Math.max(viewConfiguration.getScaledTouchSlop(), 10 * density);
        sideDirectionSlop = viewConfiguration.getScaledTouchSlop();
        sideVerticalFloor = 24 * density;
        overlay = new FanOverlayController(context, mainHandler);
        honeycombOverlay = new HoneycombOverlayController(context, mainHandler);
        taskSwitcherOverlay = new TaskSwitcherOverlayController(context, mainHandler);
        recentTaskRepository = new RecentTaskRepository(context);
        systemRecentsLauncher = new SystemRecentsLauncher(context);
        fullscreenLauncher = new FullscreenAppLauncher(context);
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
            lifecycleFilter.addAction(Intent.ACTION_WALLPAPER_CHANGED);
            context.registerReceiver(new BroadcastReceiver() {
                @Override public void onReceive(Context receiverContext, Intent intent) {
                    mainHandler.post(FanRuntime.this::refreshTriggerCapture);
                    String action = intent.getAction();
                    if (Intent.ACTION_WALLPAPER_CHANGED.equals(action)) {
                        BlurredWallpaperCache.clear();
                    }
                    if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                        wheelSessionActive = false;
                        mainHandler.post(overlay::removeNow);
                        mainHandler.post(honeycombOverlay::removeNow);
                        mainHandler.post(taskSwitcherOverlay::removeNow);
                        resetFan(false);
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
        context.registerComponentCallbacks(new ComponentCallbacks() {
            @Override public void onConfigurationChanged(Configuration newConfig) {
                mainHandler.post(() -> {
                    BlurredWallpaperCache.clear();
                    overlay.removeNow();
                    honeycombOverlay.removeNow();
                    taskSwitcherOverlay.removeNow();
                    resetFan(false);
                    refreshGeometryAndCaptures("configuration immediate");
                    mainHandler.post(() -> refreshGeometryAndCaptures(
                            "configuration next frame"));
                    mainHandler.postDelayed(() -> refreshGeometryAndCaptures(
                            "configuration settled"), 180L);
                });
            }

            @Override public void onLowMemory() { BlurredWallpaperCache.clear(); }
        });
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
        releaseOutsideCaptureTransitionIfSettled();
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
        releaseOutsideCaptureTransitionIfSettled();
        refreshOutsideCapture();
        if (freeform.trackedTaskId() < 0 && freeform.pendingLaunchBounds() == null) {
            outsideGestures.clearAll();
        }
    }

    void onNativeFreeformMotion(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_MOVE) {
            if (geometryRefreshQueued) return;
            geometryRefreshQueued = true;
            long elapsed = android.os.SystemClock.uptimeMillis() - lastGeometryRefreshUptime;
            mainHandler.postDelayed(() -> {
                geometryRefreshQueued = false;
                lastGeometryRefreshUptime = android.os.SystemClock.uptimeMillis();
                refreshOutsideCapture();
            }, Math.max(0L, 16L - elapsed));
            return;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            refreshOutsideCapture();
            mainHandler.postDelayed(this::refreshOutsideCapture, 32L);
            mainHandler.postDelayed(this::refreshOutsideCapture, 100L);
            mainHandler.postDelayed(this::refreshOutsideCapture, 240L);
            mainHandler.postDelayed(this::refreshOutsideCapture, 500L);
        }
    }

    void onNativeFreeformLeavingInteractiveState(String reason) {
        suspendOutsideCaptureForWindowTransition("native " + reason);
    }

    void onImeVisibilityChanged(boolean visible, int height) {
        imeVisible = visible;
        imeHeight = visible ? Math.max(0, height) : 0;
        if (visible && (state == State.ARMED || state == State.CLAIMED
                || (state == State.ACTIVE && !activeSideList)
                || state == State.HONEYCOMB)) {
            resetFan();
            Log.i("Bottom application gesture cancelled because input method became visible");
        } else if (visible && (activeSideList || state == State.SIDE_ARMED)) {
            resetFan();
            Log.i("Side application gesture cancelled because input method became visible");
        }
        Log.i("IME visibility changed visible=" + visible + " height=" + imeHeight);
        refreshTriggerCapture();
        refreshOutsideCapture();
        mainHandler.postDelayed(this::refreshTriggerCapture, 60L);
        mainHandler.postDelayed(this::refreshTriggerCapture, 180L);
        mainHandler.postDelayed(this::refreshOutsideCapture, 60L);
        mainHandler.postDelayed(this::refreshOutsideCapture, 180L);
        mainHandler.postDelayed(this::refreshOutsideCapture, 420L);
        mainHandler.postDelayed(this::refreshOutsideCapture, 800L);
    }

    void onShadeExpansionChanged(float fraction, boolean expanded) {
        boolean next = expanded || fraction > 0.01f;
        long now = android.os.SystemClock.uptimeMillis();
        if (next && !SystemPanelArbitrator.acceptShadeExpansion(
                shadeExpanded, now, systemPanelTouchBlockUntil)) {
            // Heads-up notifications and Dynamic Island also animate the shade's
            // expansion fraction. Without a real top-edge touch they must not remove
            // the freeform guard or pause an active honeycomb session.
            Log.i("Ignored notification-driven shade expansion fraction=" + fraction);
            return;
        }
        long version = ++shadeStateVersion;
        if (next) {
            if (!shadeExpanded) {
                shadeExpanded = true;
                applySystemPanelCaptureState("notification shade");
            } else {
                outsideCapture.remove();
            }
            return;
        }
        mainHandler.postDelayed(() -> {
            if (shadeStateVersion != version || !shadeExpanded) return;
            shadeExpanded = false;
            applySystemPanelCaptureState("notification shade");
        }, 480L);
    }

    void onControlCenterExpansionChanged(boolean expanded) {
        long version = ++controlCenterStateVersion;
        if (expanded) {
            if (!controlCenterExpanded) {
                controlCenterExpanded = true;
                applySystemPanelCaptureState("control center");
            } else {
                outsideCapture.remove();
            }
            return;
        }
        mainHandler.postDelayed(() -> {
            if (controlCenterStateVersion != version || !controlCenterExpanded) return;
            controlCenterExpanded = false;
            applySystemPanelCaptureState("control center");
        }, 620L);
    }

    void onAuthoritativeSystemPanelState(boolean notificationExpanded,
                                         boolean controlExpanded) {
        if (!mainHandler.getLooper().isCurrentThread()) {
            mainHandler.post(() -> onAuthoritativeSystemPanelState(
                    notificationExpanded, controlExpanded));
            return;
        }
        long version = ++authoritativePanelStateVersion;
        ++shadeStateVersion;
        ++controlCenterStateVersion;
        shadeExpanded = notificationExpanded;
        controlCenterExpanded = controlExpanded;
        Log.i("Authoritative system panel state notification=" + notificationExpanded
                + " control=" + controlExpanded);
        if (notificationExpanded || controlExpanded) {
            applySystemPanelCaptureState("authoritative system panel state");
            return;
        }
        mainHandler.postDelayed(() -> {
            // Expansion progress callbacks are noisy and can arrive after HyperOS has already
            // committed the final collapsed state. Only a newer authoritative state may cancel
            // this restore; stale progress=0 callbacks must not leave the outside layer detached.
            if (authoritativePanelStateVersion != version) return;
            shadeExpanded = false;
            controlCenterExpanded = false;
            systemPanelTouchBlockUntil = 0L;
            applySystemPanelCaptureState("authoritative system panel state");
        }, 220L);
    }

    void onSystemPanelTouchStarted(String source) {
        long until = android.os.SystemClock.uptimeMillis() + 1800L;
        systemPanelTouchBlockUntil = until;
        outsideCapture.remove();
        honeycombOverlay.setPaused(true);
        outsideGestures.clearAll();
        mainHandler.postDelayed(() -> {
            if (systemPanelTouchBlockUntil != until) return;
            systemPanelTouchBlockUntil = 0L;
            if (!shadeExpanded && !controlCenterExpanded) {
                honeycombOverlay.setPaused(false);
                refreshOutsideCapture();
                mainHandler.postDelayed(this::refreshOutsideCapture, 160L);
            }
        }, 1850L);
        Log.i("Outside capture paused at " + source + " touch down");
    }

    private void applySystemPanelCaptureState(String source) {
        if (shadeExpanded || controlCenterExpanded) {
            outsideCapture.remove();
            honeycombOverlay.setPaused(true);
            outsideGestures.clearAll();
            Log.i("Outside capture paused while " + source + " is expanded");
        } else {
            honeycombOverlay.setPaused(false);
            refreshOutsideCapture();
            mainHandler.postDelayed(this::refreshOutsideCapture, 90L);
            mainHandler.postDelayed(this::refreshOutsideCapture, 260L);
            Log.i("Outside capture restored after " + source + " collapsed");
        }
    }

    void adjustMiniTargetIfNeeded(int animationType, Object info, Object target) {
        freeform.adjustMiniTargetIfNeeded(animationType, info, target);
    }

    void adjustEdgePinRestoreTarget(Object info, Object target, Object transaction) {
        freeform.adjustEdgePinRestoreTarget(info, target, transaction);
    }

    boolean stabilizeLandscapeWindowShape(Object info) {
        return freeform.stabilizeLandscapeWindowShape(info);
    }

    boolean suppressLandscapeShapeChange(int taskId, int orientation) {
        return freeform.suppressLandscapeShapeChange(taskId, orientation);
    }

    void prepareShortcutEnterAnimation(Object change, Object taskInfo) {
        freeform.prepareShortcutEnterAnimation(change, taskInfo);
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
        if (action == MotionEvent.ACTION_DOWN) ensureDisplayGeometryCurrent();
        int width = displayBounds().width();
        int height = displayBounds().height();
        float x = event.getX();
        float y = event.getY();

        if (triggerCapture.isInjectedEvent(event)) return;
        if (state == State.TASKS_LOADING) {
            taskLoadCurrentX = x;
            taskLoadCurrentY = y;
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                taskLoadGeneration++;
                resetFan(false);
                refreshOutsideCapture();
            }
            return;
        }
        if (state == State.TASKS) {
            if (action == MotionEvent.ACTION_MOVE) taskSwitcherOverlay.externalMove(x, y);
            if (action == MotionEvent.ACTION_CANCEL) {
                taskSwitcherOverlay.externalCancel();
                resetFan(false);
            }
            if (action == MotionEvent.ACTION_UP) {
                taskSwitcherOverlay.externalUp(x, y, false);
                resetFan(false);
            }
            return;
        }
        if (state == State.HONEYCOMB) {
            if (action == MotionEvent.ACTION_MOVE) {
                float distance = GestureGeometry.distance(downX, downY, x, y);
                if (honeycombGestureState.shouldExit(distance,
                        activeHoneycombReturnThreshold)) {
                    honeycombOverlay.removeNow();
                    state = State.ACTIVE;
                    float selectionRadius = selectionRadius(width, height);
                    float iconDiameter = iconDiameter(selectionRadius);
                    overlay.show(targets, corner, selectionRadius, iconDiameter,
                            config.showSelectedAppName, config.fanShadow,
                            config.fanAnimationsEnabled, config.fanAnimationSpeed,
                            config.fanRevealAmount, config.fanRotationDegrees,
                            config.fanSelectionScalePercent, config.fanSelectionRing,
                            config.fanLayoutMode, config.fanCustomOuterCount,
                            config.fanCustomMiddleCount, config.fanCustomInnerCount,
                            config.forceCircularIcons);
                    updateSelection(x, y, width, height, selectionRadius, iconDiameter);
                    Log.i("Honeycomb retreated through hysteresis; fan restored");
                    return;
                }
                honeycombOverlay.externalMove(x, y);
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                // A heads-up notification can cancel the monitor's current stream even
                // though the user never opened a system panel. Keep the honeycomb alive;
                // its own full-screen view will accept the next touch immediately.
                honeycombOverlay.externalCancel();
                resetFan(false);
                Log.i("Honeycomb input stream cancelled externally; overlay retained");
                return;
            }
            if (action == MotionEvent.ACTION_UP) {
                honeycombOverlay.externalUp(x, y, false);
                resetFan(false);
            }
            return;
        }
        if (honeycombOverlay.isVisible() || taskSwitcherOverlay.isVisible()) return;
        if (wheelSessionActive || overlay.isWheelVisible()) return;
        if (action == MotionEvent.ACTION_DOWN && y <= statusBarGestureHeight()) {
            systemPanelInputDownTime = event.getDownTime();
            onSystemPanelTouchStarted("top system panel input");
            Log.i("Top system panel gesture excluded from global input takeover");
            return;
        }
        if (event.getDownTime() == systemPanelInputDownTime) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                systemPanelInputDownTime = -1L;
            }
            return;
        }
        if (systemPanelInputBlocked()) {
            if (state != State.IDLE) resetFan();
            outsideGestures.clearAll();
            return;
        }
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
            boolean landscape = width > height;
            boolean outsideEnabled = config.outsideEnabledFor(landscape);
            float hotWidth = width * config.hotWidthPercent / 100f;
            float hotHeight = height * config.hotHeightPercent / 100f;
            Insets systemSides = systemSideGestureInsets();
            GestureGeometry.Corner downCorner = config.bottomEnabledFor(landscape)
                    ? GestureGeometry.cornerAt(x, y, width, height, hotWidth, hotHeight)
                    : null;
            float sideSafeTop = sideSafeTop(height);
            float sideSafeBottom = sideSafeBottom(height, hotHeight);
            GestureGeometry.Corner systemEdgeSide = downCorner == null
                    ? GestureGeometry.sideAt(x, width, systemSides.left, systemSides.right)
                    : null;
            int sideLayoutMode = config.sideLayoutModeFor(landscape);
            boolean sideSourceAvailable = sideLayoutMode
                    == ConfigContract.SIDE_LAYOUT_TASKS
                    || sideLayoutMode == ConfigContract.SIDE_LAYOUT_SYSTEM_RECENTS
                    || (sideLayoutMode == ConfigContract.SIDE_LAYOUT_HONEYCOMB
                    ? config.honeycombEnabledFor(landscape) && !honeycombTargets.isEmpty()
                    : !sideTargets.isEmpty());
            GestureGeometry.Corner listSide = config.sideEnabledFor(landscape)
                    && sideSourceAvailable && downCorner == null
                    ? GestureGeometry.sideAt(x, y, width, systemSides.left,
                    systemSides.right, sideSafeTop, sideSafeBottom, 96 * density)
                    : null;
            GestureGeometry.Corner downSide = listSide;
            if (!triggerCapture.isCapturing() && canStartBottom()) {
                triggerCapture.update(true, config.hotWidthPercent, config.hotHeightPercent,
                        0, 0);
            }
            Rect pendingLaunch = freeform.pendingLaunchBounds();
            Rect tracked = freeform.trackedBounds();
            Rect ime = tracked == null ? null : visibleImeBounds();
            boolean freeformActive = pendingLaunch != null || tracked != null
                    || freeform.hasInteractiveFreeform();
            if (pendingLaunch != null) {
                if (outsideEnabled && GestureGeometry.outsideRegion(x, y,
                        pendingLaunch.left,
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
            if (outsideEnabled && outsideTracked && imeActive) {
                imeDismissTap = true;
                if (systemEdgeSide == null) pilfer(inputMonitor);
            }
            if (outsideEnabled && outsideTracked && systemEdgeSide != null
                    && canStartSide()) {
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
            if (downSide != null && canStartSide()) {
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
                if (downCorner != null && canStartBottom()) {
                    if (outsideEnabled) {
                        Insets reserves = sideGestureReserves();
                        outsideGestures.onDown(x, y, tracked, width,
                                reserves.left, reserves.right, event.getEventTime());
                    }
                    if (triggerCapture.isCapturing()) {
                        pilfer(inputMonitor);
                        Log.i("Fan input claimed on down through corner capture window");
                    }
                    arm(downCorner, x, y, event.getDownTime(), event.getEventTime());
                    cornerTapIsOutsideWindow = outsideEnabled;
                    Log.i("Fan hot zone armed on down corner=" + downCorner);
                    return;
                }
                if (!outsideEnabled) return;
                Insets reserves = sideGestureReserves();
                if (outsideGestures.onDown(x, y, tracked, width,
                        reserves.left, reserves.right, event.getEventTime())) {
                    pilfer(inputMonitor);
                }
                return;
            }
            if (downCorner != null && canStartBottom()) {
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
            cancelBottomHoneycombSettle();
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
                    sideDirectionSlop, sideVerticalFloor,
                    config.sideDirectionHorizontal, config.sideDirectionUp,
                    config.sideDirectionDown);
            if (decision == SideGestureArbitrator.Decision.PENDING) {
                cancelSideHold();
                return;
            }
            if (decision == SideGestureArbitrator.Decision.CANCELLED) {
                cancelSideHold();
                state = State.CANCELLED;
                Log.i("Side-distance candidate yielded to native input");
                return;
            }
            if (waitForSideHold(x, y, width, inputMonitor)) return;
            cancelSideHold();
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
                float activeIconDiameter = iconDiameter(selectionRadius);
                int next = updateSelection(x, y, width, height, selectionRadius,
                        activeIconDiameter);
                updateBottomHoneycombSettleCandidate(x, y, width, height, next,
                        selectionRadius, activeIconDiameter);
            }
            return;
        }

        if (state == State.IDLE && action == MotionEvent.ACTION_MOVE) {
            outsideGestures.onMove(x, y);
            return;
        }

        if (action == MotionEvent.ACTION_UP) {
            cancelBottomHoneycombSettle();
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
                if (selected >= 0 && selected < activeTargets.size()) {
                    if (activeSideList && config.sideWheelMode) {
                        showPersistentWheel();
                        resetFan(false);
                        return;
                    } else {
                        RuntimeTarget target = activeTargets.get(selected);
                        overlay.confirmAndHide(selected);
                        boolean landscape = isLandscape();
                        boolean fullscreen = activeSideList
                                ? config.sideFullscreenFor(landscape)
                                : config.bottomFullscreenFor(landscape);
                        if (fullscreen) {
                            launchFullscreenTarget(target, false);
                        } else {
                            freeform.launch(target, config, this::refreshOutsideCapture);
                            refreshOutsideCaptureAfterLaunch();
                        }
                        outsideGestures.clearAll();
                        resetFan(false);
                        return;
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
            cancelBottomHoneycombSettle();
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
            overlay.show(targets, corner, selectionRadius, iconDiameter,
                    config.showSelectedAppName, config.fanShadow,
                    config.fanAnimationsEnabled, config.fanAnimationSpeed,
                    config.fanRevealAmount, config.fanRotationDegrees,
                    config.fanSelectionScalePercent,
                    config.fanSelectionRing, config.fanLayoutMode,
                    config.fanCustomOuterCount, config.fanCustomMiddleCount,
                    config.fanCustomInnerCount,
                    config.forceCircularIcons);
            int next = updateSelection(x, y, width, height, selectionRadius, iconDiameter);
            updateBottomHoneycombSettleCandidate(x, y, width, height, next,
                    selectionRadius, iconDiameter);
        }
    }

    private boolean activateBottomHoneycomb(float x, float y, int width, int height) {
        boolean landscape = isLandscape();
        if (!config.bottomSecondStageEnabledFor(landscape)
                || !config.honeycombEnabledFor(landscape)
                || honeycombTargets.isEmpty() || activeSideList) return false;
        honeycombGestureState.enter();
        GestureGeometry.Corner launchCorner = corner;
        overlay.hide();
        selected = -1;
        outsideCapture.remove();
        outsideGestures.clearAll();
        boolean shown = honeycombOverlay.show(honeycombTargets, launchCorner, x, y, config,
                new HoneycombOverlayController.Listener() {
                    @Override public void onLaunch(RuntimeTarget target) {
                        if (config.bottomHoneycombFreeformFor(isLandscape())) {
                            freeform.launch(target, config,
                                    FanRuntime.this::refreshOutsideCapture);
                            refreshOutsideCaptureAfterLaunch();
                        } else {
                            launchFullscreenTarget(target, true);
                        }
                    }

                    @Override public void onClosed() {
                        refreshOutsideCapture();
                    }
                });
        if (!shown) {
            honeycombGestureState.reset();
            return false;
        }
        activeHoneycombReturnThreshold = Math.min(width, height)
                * config.triggerPercent / 100f;
        state = State.HONEYCOMB;
        honeycombOverlay.externalMove(x, y);
        if (config.haptic) vibrateTick();
        Log.i("Honeycomb activated targets=" + honeycombTargets.size()
                + " mode=" + config.honeycombMode + " after blank-area settle");
        return true;
    }

    private void launchFullscreenTarget(RuntimeTarget target, boolean centeredSystemAnimation) {
        outsideCapture.remove();
        outsideGestures.clearAll();
        boolean closingFreeform = freeform.dismissTracked();
        Runnable launch = () -> {
            boolean launched = fullscreenLauncher.launch(target, centeredSystemAnimation);
            if (!launched) Log.i("Fullscreen target was unavailable");
        };
        if (closingFreeform) mainHandler.postDelayed(launch, 220L);
        else launch.run();
    }

    private void activateSideList(float x, float y, int width, int height) {
        int sideLayoutMode = currentSideLayoutMode();
        if (sideLayoutMode == ConfigContract.SIDE_LAYOUT_SYSTEM_RECENTS) {
            activateSystemRecents();
            return;
        }
        if (sideLayoutMode == ConfigContract.SIDE_LAYOUT_TASKS) {
            activateSideTasks(x, y);
            return;
        }
        if (sideLayoutMode == ConfigContract.SIDE_LAYOUT_HONEYCOMB) {
            activateSideHoneycomb(x, y);
            return;
        }
        state = State.ACTIVE;
        activeSideList = true;
        activeSideFanList = sideLayoutMode == ConfigContract.SIDE_LAYOUT_FAN;
        activeSideRingList = sideLayoutMode == ConfigContract.SIDE_LAYOUT_RING;
        float hotHeight = height * config.hotHeightPercent / 100f;
        float safeTop = sideSafeTop(height);
        float safeBottom = sideSafeBottom(height, hotHeight);
        float availablePerItem = (safeBottom - safeTop) / Math.max(1, activeTargets.size());
        sideIconDiameter = Math.min(config.sideIconSizeDp * density,
                Math.max(28 * density, availablePerItem - 4 * density));
        sideRowHeight = Math.min(sideIconDiameter + 10 * density, availablePerItem);
        int anchor = (activeTargets.size() - 1) / 2;
        sideListTop = GestureGeometry.sideListTopForAnchor(y, activeTargets.size(), anchor,
                sideRowHeight, safeTop, safeBottom);
        sideListCenterX = GestureGeometry.sideListCenterX(corner, x, width,
                sideIconDiameter, 14 * density, config.sideFollowFinger);
        sideListHitWidth = Math.max(sideIconDiameter + 56 * density, 96 * density);
        sideListActivationX = GestureGeometry.sideListOuterBoundary(
                corner, sideListCenterX, sideListHitWidth);
        // The list stays available while the finger explores the screen. Only returning
        // to the physical edge dismisses it, with the fade spanning that whole distance.
        sideReverseCancelDistance = Math.max(1f, corner == GestureGeometry.Corner.LEFT
                ? sideListActivationX : width - sideListActivationX);
        sideListEntered = false;
        selected = -1;
        lastHapticSelection = -1;
        activeHoneycombReturnThreshold = 0f;
        if (activeSideRingList) {
            float safeLeft = 12 * density;
            float safeRight = width - 12 * density;
            float gap = 8 * density;
            float minimumIcon = 28 * density;
            float maxOuter = Math.max(1f, Math.min(
                    (safeRight - safeLeft) / 2f,
                    (safeBottom - safeTop) / 2f));
            sideIconDiameter = Math.min(config.sideIconSizeDp * density,
                    Math.max(minimumIcon, maxOuter / 2f));
            float automaticRadius = GestureGeometry.sideRingRadius(
                    activeTargets.size(), sideIconDiameter, gap);
            sideRingRadius = GestureGeometry.scaledSideRingRadius(
                    automaticRadius, config.sideRingSizePercent,
                    maxOuter - sideIconDiameter / 2f);
            GestureGeometry.Point center = GestureGeometry.sideRingCenter(
                    x, y, sideRingRadius, sideIconDiameter,
                    safeLeft, safeTop, safeRight, safeBottom);
            sideRingCenterX = center.x;
            sideRingCenterY = center.y;
            sideListCenterX = center.x;
            sideListTop = center.y;
            overlay.showSideRingList(activeTargets, corner,
                    sideRingCenterX, sideRingCenterY, sideRingRadius,
                    sideIconDiameter, config.showSelectedAppName,
                    false, config.sideAnimationsEnabled,
                    config.sideAnimationSpeed, config.sideRevealAmount,
                    config.sideRotationDegrees, config.sideSelectionScalePercent,
                    config.sideSelectionRing, config.forceCircularIcons);
        } else if (activeSideFanList) {
            sideListCenterX = x;
            sideFanRadius = Math.max(sideIconDiameter * 1.35f,
                    corner == GestureGeometry.Corner.LEFT ? x : width - x);
            sideFanCenterY = GestureGeometry.sideFanCenterY(y, activeTargets.size(),
                    sideFanRadius, sideIconDiameter, safeTop, safeBottom);
            overlay.showSideFanList(activeTargets, corner, sideListCenterX, sideFanCenterY,
                    sideFanRadius, sideIconDiameter, config.showSelectedAppName,
                    false, config.sideAnimationsEnabled,
                    config.sideAnimationSpeed, config.sideRevealAmount,
                    config.sideRotationDegrees, config.sideSelectionScalePercent,
                    config.sideSelectionRing, config.forceCircularIcons);
        } else {
            overlay.showSideList(activeTargets, corner, sideListCenterX, sideListTop,
                    sideRowHeight, sideIconDiameter, config.showSelectedAppName,
                    false, config.sideAnimationsEnabled,
                    config.sideAnimationSpeed, config.sideRevealAmount,
                    config.sideRotationDegrees, config.sideSelectionScalePercent,
                    config.sideSelectionRing, config.forceCircularIcons);
        }
        if (config.haptic) vibrateTick();
        updateSideListSelection(x, y);
        Log.i("Side list shown center=" + Math.round(sideListCenterX) + ","
                + Math.round(activeSideRingList ? sideRingCenterY
                : activeSideFanList ? sideFanCenterY
                : sideListTop + (anchor + 0.5f) * sideRowHeight)
                + " selected=" + selected + " follow=" + config.sideFollowFinger
                + " layout=" + sideLayoutMode + " wheel=" + config.sideWheelMode);
    }

    private void activateSideHoneycomb(float x, float y) {
        state = State.ACTIVE;
        activeSideList = true;
        activeTargets = honeycombTargets;
        outsideCapture.remove();
        outsideGestures.clearAll();
        activeHoneycombReturnThreshold = 0f;
        boolean shown = honeycombOverlay.show(activeTargets, corner, x, y, config,
                new HoneycombOverlayController.Listener() {
                    @Override public void onLaunch(RuntimeTarget target) {
                        if (config.sideFullscreenFor(isLandscape())) {
                            launchFullscreenTarget(target, true);
                        }
                        else {
                            freeform.launch(target, config,
                                    FanRuntime.this::refreshOutsideCapture);
                            refreshOutsideCaptureAfterLaunch();
                        }
                    }
                    @Override public void onClosed() { refreshOutsideCapture(); }
                });
        if (shown) {
            state = State.HONEYCOMB;
            honeycombOverlay.externalMove(x, y);
            if (config.haptic) vibrateTick();
        } else state = State.CANCELLED;
    }

    private void activateSideTasks(float x, float y) {
        state = State.TASKS_LOADING;
        activeSideList = true;
        outsideCapture.remove();
        outsideGestures.clearAll();
        taskLoadCurrentX = x;
        taskLoadCurrentY = y;
        int generation = ++taskLoadGeneration;
        GestureGeometry.Corner taskCorner = corner;
        GestureConfig taskConfig = config;
        configHandler.post(() -> {
            List<RecentTaskPreview> previews = recentTaskRepository.loadRunningTasks(
                    taskConfig.sideTaskMaxCount,
                    taskConfig.sideTaskLayoutMode != ConfigContract.SIDE_TASK_LAYOUT_ICONS);
            mainHandler.post(() -> showLoadedSideTasks(previews, taskCorner, x, y,
                    taskConfig, generation));
        });
    }

    private void showLoadedSideTasks(List<RecentTaskPreview> previews,
                                     GestureGeometry.Corner taskCorner,
                                     float anchorX, float anchorY,
                                     GestureConfig taskConfig, int generation) {
        if (state != State.TASKS_LOADING || generation != taskLoadGeneration) {
            recentTaskRepository.release(previews);
            return;
        }
        if (previews.isEmpty()) {
            state = State.CANCELLED;
            Log.i("Task switcher skipped because no running tasks were available");
            refreshOutsideCapture();
            return;
        }
        boolean shown = taskSwitcherOverlay.show(previews, taskCorner, anchorX, anchorY,
                taskConfig,
                new TaskSwitcherOverlayController.Listener() {
                    @Override public void onLaunch(RecentTaskPreview task) {
                        recentTaskRepository.launch(task);
                        mainHandler.postDelayed(FanRuntime.this::refreshOutsideCapture, 500L);
                    }

                    @Override public void onClosed() {
                        refreshOutsideCapture();
                    }
                });
        if (shown) {
            state = State.TASKS;
            taskSwitcherOverlay.externalMove(taskLoadCurrentX, taskLoadCurrentY);
            if (taskConfig.haptic) vibrateTick();
        } else {
            recentTaskRepository.release(previews);
            state = State.CANCELLED;
            refreshOutsideCapture();
        }
    }

    private void activateSystemRecents() {
        state = State.CANCELLED;
        activeSideList = false;
        outsideCapture.remove();
        outsideGestures.clearAll();
        boolean shown = systemRecentsLauncher.show();
        if (!shown) {
            Log.i("HyperOS system recents request failed safely");
            refreshOutsideCapture();
            return;
        }
        if (config.haptic) vibrateTick();
        long version = ++systemRecentsWatchVersion;
        mainHandler.postDelayed(() -> watchSystemRecents(version, 0, false), 100L);
    }

    private void watchSystemRecents(long version, int attempts, boolean wasVisible) {
        if (version != systemRecentsWatchVersion) return;
        boolean visible = systemRecentsLauncher.isVisible();
        if (visible) {
            mainHandler.postDelayed(
                    () -> watchSystemRecents(version, attempts + 1, true), 450L);
            return;
        }
        if (!wasVisible && attempts < 12) {
            mainHandler.postDelayed(
                    () -> watchSystemRecents(version, attempts + 1, false), 100L);
            return;
        }
        refreshOutsideCapture();
        Log.i("Outside capture refreshed after HyperOS system recents closed");
    }

    private void arm(GestureGeometry.Corner corner, float x, float y,
                     long downTime, long eventTime) {
        gestureArbitrator.reset();
        gestureReplayGuard.begin(downTime, eventTime);
        state = State.ARMED;
        activeTargets = targets;
        this.corner = corner;
        downX = x;
        downY = y;
        startedWithTriggerCapture = triggerCapture.isCapturing();
    }

    private void armSide(GestureGeometry.Corner side, float x, float y,
                         long downTime, long eventTime) {
        cancelSideHold();
        sideGestureArbitrator.reset();
        gestureReplayGuard.begin(downTime, eventTime);
        state = State.SIDE_ARMED;
        activeTargets = currentSideLayoutMode() == ConfigContract.SIDE_LAYOUT_HONEYCOMB
                ? honeycombTargets : sideTargets;
        corner = side;
        downX = x;
        downY = y;
    }

    private void updateSideHoldCandidate(float x, float y, int width, Object inputMonitor) {
        if (!config.sideHoldEnabled
                || currentSideLayoutMode() != ConfigContract.SIDE_LAYOUT_HONEYCOMB
                || state != State.SIDE_ARMED || corner == null) {
            cancelSideHold();
            return;
        }
        if (sideTapIsOutsideWindow && !sideListAllowed) {
            cancelSideHold();
            return;
        }
        if (!config.honeycombEnabledFor(isLandscape()) || honeycombTargets.isEmpty()) {
            cancelSideHold();
            return;
        }
        float inward = corner == GestureGeometry.Corner.LEFT ? x - downX : downX - x;
        float distance = GestureGeometry.distance(downX, downY, x, y);
        float minimum = width * config.sideTriggerPercent / 100f;
        if (inward <= sideDirectionSlop || distance < minimum) {
            cancelSideHold();
            return;
        }
        sideHoldCurrentX = x;
        sideHoldCurrentY = y;
        sideHoldInputMonitor = inputMonitor;
        boolean moved = !sideHoldScheduled || Math.hypot(
                x - sideHoldAnchorX, y - sideHoldAnchorY) > sideDirectionSlop;
        if (!moved) return;
        Handler currentHandler = sideHoldHandler;
        Looper currentLooper = Looper.myLooper();
        if (currentLooper == null) currentHandler = mainHandler;
        else if (currentHandler == null || currentHandler.getLooper() != currentLooper) {
            if (currentHandler != null) currentHandler.removeCallbacks(sideHoldTrigger);
            currentHandler = new Handler(currentLooper);
        }
        sideHoldHandler = currentHandler;
        currentHandler.removeCallbacks(sideHoldTrigger);
        sideHoldAnchorX = x;
        sideHoldAnchorY = y;
        sideHoldScheduled = true;
        currentHandler.postDelayed(sideHoldTrigger, config.sideHoldDelayMs);
    }

    private boolean waitForSideHold(float x, float y, int width,
                                    Object inputMonitor) {
        if (!config.sideHoldEnabled
                || currentSideLayoutMode() != ConfigContract.SIDE_LAYOUT_HONEYCOMB) return false;
        if (!config.honeycombEnabledFor(isLandscape()) || honeycombTargets.isEmpty()) {
            return false;
        }
        updateSideHoldCandidate(x, y, width, inputMonitor);
        // Hold mode replaces immediate distance activation. Once the shared threshold
        // is reached, continued movement only restarts the timer; it never opens the
        // honeycomb until the finger actually settles.
        return true;
    }

    private void triggerHeldSideList() {
        sideHoldScheduled = false;
        sideHoldHandler = null;
        if (!config.sideHoldEnabled
                || currentSideLayoutMode() != ConfigContract.SIDE_LAYOUT_HONEYCOMB
                || state != State.SIDE_ARMED || corner == null) return;
        if (Math.hypot(sideHoldCurrentX - sideHoldAnchorX,
                sideHoldCurrentY - sideHoldAnchorY) > sideDirectionSlop * 1.5f) return;
        if (sideTapIsOutsideWindow && !sideListAllowed) return;
        Object inputMonitor = sideHoldInputMonitor;
        sideHoldInputMonitor = null;
        if (!pilfer(inputMonitor)) {
            Log.i("Side hold takeover unavailable; native back preserved");
            return;
        }
        sideSequenceCaptured = true;
        outsideGestures.onCancel();
        activateSideHoneycomb(sideHoldCurrentX, sideHoldCurrentY);
        Log.i("Side honeycomb opened after hold distance=" + Math.round(
                GestureGeometry.distance(downX, downY,
                        sideHoldCurrentX, sideHoldCurrentY)));
    }

    private void cancelSideHold() {
        Handler currentHandler = sideHoldHandler;
        if (currentHandler != null) currentHandler.removeCallbacks(sideHoldTrigger);
        mainHandler.removeCallbacks(sideHoldTrigger);
        sideHoldScheduled = false;
        sideHoldHandler = null;
        sideHoldInputMonitor = null;
    }

    private void updateBottomHoneycombSettleCandidate(float x, float y, int width, int height,
                                                       int selection, float fanRadius,
                                                       float fanIconDiameter) {
        if (selection >= 0 || state != State.ACTIVE || activeSideList
                || !config.bottomSecondStageEnabledFor(isLandscape())
                || !config.honeycombEnabledFor(isLandscape())
                || honeycombTargets.isEmpty()) {
            cancelBottomHoneycombSettle();
            return;
        }
        float minimumDistance = GestureGeometry.fanOutermostEdgeDistance(
                corner, activeTargets.size(), width, height, fanRadius,
                fanIconDiameter, BOTTOM_HONEYCOMB_FAN_GAP_DP * density,
                config.fanLayoutMode, config.fanCustomOuterCount,
                config.fanCustomMiddleCount, config.fanCustomInnerCount);
        if (!GestureGeometry.beyondFanEdge(
                corner, x, y, width, height, minimumDistance)) {
            cancelBottomHoneycombSettle();
            return;
        }
        bottomHoneycombCurrentX = x;
        bottomHoneycombCurrentY = y;
        bottomHoneycombDisplayWidth = width;
        bottomHoneycombDisplayHeight = height;
        float settleSlop = BOTTOM_HONEYCOMB_SETTLE_DP * density;
        boolean moved = !bottomHoneycombSettleScheduled || Math.hypot(
                x - bottomHoneycombSettleAnchorX,
                y - bottomHoneycombSettleAnchorY) > settleSlop;
        if (!moved) return;
        Handler currentHandler = bottomHoneycombSettleHandler;
        Looper currentLooper = Looper.myLooper();
        if (currentLooper == null) currentHandler = mainHandler;
        else if (currentHandler == null || currentHandler.getLooper() != currentLooper) {
            if (currentHandler != null) {
                currentHandler.removeCallbacks(bottomHoneycombSettleTrigger);
            }
            currentHandler = new Handler(currentLooper);
        }
        bottomHoneycombSettleHandler = currentHandler;
        currentHandler.removeCallbacks(bottomHoneycombSettleTrigger);
        bottomHoneycombSettleAnchorX = x;
        bottomHoneycombSettleAnchorY = y;
        bottomHoneycombSettleScheduled = true;
        currentHandler.postDelayed(bottomHoneycombSettleTrigger,
                BOTTOM_HONEYCOMB_SETTLE_MS);
    }

    private void triggerSettledBottomHoneycomb() {
        bottomHoneycombSettleScheduled = false;
        bottomHoneycombSettleHandler = null;
        float settleSlop = BOTTOM_HONEYCOMB_SETTLE_DP * density;
        if (state != State.ACTIVE || activeSideList || selected >= 0
                || Math.hypot(bottomHoneycombCurrentX - bottomHoneycombSettleAnchorX,
                bottomHoneycombCurrentY - bottomHoneycombSettleAnchorY) > settleSlop) {
            return;
        }
        activateBottomHoneycomb(bottomHoneycombCurrentX, bottomHoneycombCurrentY,
                bottomHoneycombDisplayWidth, bottomHoneycombDisplayHeight);
    }

    private void cancelBottomHoneycombSettle() {
        Handler currentHandler = bottomHoneycombSettleHandler;
        if (currentHandler != null) {
            currentHandler.removeCallbacks(bottomHoneycombSettleTrigger);
        }
        mainHandler.removeCallbacks(bottomHoneycombSettleTrigger);
        bottomHoneycombSettleScheduled = false;
        bottomHoneycombSettleHandler = null;
    }

    private int updateSelection(float x, float y, int width, int height,
                                float radius, float iconDiameter) {
        int next = GestureGeometry.selection(corner, x, y, width, height, activeTargets.size(),
                radius, iconDiameter, 6 * density, config.fanLayoutMode,
                config.fanCustomOuterCount, config.fanCustomMiddleCount,
                config.fanCustomInnerCount);
        if (config.haptic && next >= 0 && next != lastHapticSelection) {
            vibrateTick();
        }
        lastHapticSelection = next;
        selected = next;
        overlay.update(selected, x, y);
        return next;
    }

    private void updateSideListSelection(float x, float y) {
        if (activeSideRingList) {
            overlay.setOpacity(1f);
            int next = GestureGeometry.sideRingSelection(x, y, activeTargets.size(),
                    sideRingCenterX, sideRingCenterY, sideRingRadius,
                    sideIconDiameter, 10 * density);
            selected = next;
            if (config.haptic && next >= 0 && next != lastHapticSelection) {
                vibrateTick();
                lastHapticSelection = next;
            }
            overlay.update(selected, x, y);
            return;
        }
        if (activeSideFanList) {
            overlay.setOpacity(1f);
            int width = displayBounds().width();
            int next = GestureGeometry.sideFanSelection(corner, x, y, width,
                    activeTargets.size(), sideListCenterX, sideFanCenterY, sideFanRadius,
                    sideIconDiameter, 10 * density);
            selected = next;
            if (config.haptic && next >= 0 && next != lastHapticSelection) {
                vibrateTick();
                lastHapticSelection = next;
            }
            overlay.update(selected, x, y);
            return;
        }
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
                sideListHitWidth, activeTargets.size(), sideListTop, sideRowHeight);
        if (!inside) {
            selected = -1;
            overlay.update(-1, x, y);
            return;
        }
        sideListEntered = true;
        int next = GestureGeometry.sideListSelection(
                y, activeTargets.size(), sideListTop, sideRowHeight);
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
        List<RuntimeTarget> wheelTargets = activeTargets;
        GestureGeometry.Corner wheelSide = corner;
        wheelSessionActive = true;
        overlay.showWheel(wheelTargets, wheelSide, sideListCenterX, wheelCenterY,
                sideRowHeight, sideIconDiameter, initialSelection,
                config.showSelectedAppName, false,
                config.forceCircularIcons,
                new FanOverlayController.WheelListener() {
                    @Override public void onLaunch(int index) {
                        wheelSessionActive = false;
                        if (index < 0 || index >= wheelTargets.size()) return;
                        RuntimeTarget target = wheelTargets.get(index);
                        if (config.sideFullscreenFor(isLandscape())) {
                            launchFullscreenTarget(target, false);
                        } else {
                            freeform.launch(target, config,
                                    FanRuntime.this::refreshOutsideCapture);
                            refreshOutsideCaptureAfterLaunch();
                        }
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
        return GestureGeometry.effectiveRadius(activeTargets.size(), configured,
                28 * density, 6 * density, config.fanLayoutMode,
                config.fanCustomOuterCount, config.fanCustomMiddleCount,
                config.fanCustomInnerCount);
    }

    private float iconDiameter(float radius) {
        return GestureGeometry.effectiveIconDiameter(activeTargets.size(), radius,
                config.iconSizeDp * density, 28 * density, 6 * density,
                config.fanLayoutMode, config.fanCustomOuterCount,
                config.fanCustomMiddleCount, config.fanCustomInnerCount);
    }

    private void resetFan() {
        resetFan(true);
    }

    private void resetFan(boolean hideOverlay) {
        cancelSideHold();
        cancelBottomHoneycombSettle();
        if (hideOverlay && state == State.ACTIVE) overlay.hide();
        if (hideOverlay && state == State.HONEYCOMB) honeycombOverlay.removeNow();
        if (hideOverlay && state == State.TASKS) taskSwitcherOverlay.removeNow();
        if (state == State.TASKS_LOADING) taskLoadGeneration++;
        gestureArbitrator.reset();
        sideGestureArbitrator.reset();
        gestureReplayGuard.reset();
        honeycombGestureState.reset();
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
        activeSideFanList = false;
        activeSideRingList = false;
        sideListCenterX = 0f;
        sideListTop = 0f;
        sideRowHeight = 0f;
        sideIconDiameter = 0f;
        sideListHitWidth = 0f;
        sideListActivationX = 0f;
        sideReverseCancelDistance = 0f;
        sideFanRadius = 0f;
        sideFanCenterY = 0f;
        sideRingRadius = 0f;
        sideRingCenterX = 0f;
        sideRingCenterY = 0f;
        sideListEntered = false;
        lastHapticSelection = -1;
    }

    private boolean runtimeReady() {
        if (!config.enabled
                || (context.getDisplay() != null && context.getDisplay().getDisplayId() != 0)) return false;
        PowerManager power = context.getSystemService(PowerManager.class);
        KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
        return (power == null || power.isInteractive()) && (keyguard == null || !keyguard.isKeyguardLocked());
    }

    private boolean canStartBottom() {
        return runtimeReady() && !imeVisible && targets.size() >= 3
                && config.bottomEnabledFor(isLandscape());
    }

    private boolean canStartSide() {
        if (!runtimeReady() || !config.sideEnabledFor(isLandscape())) return false;
        int sideLayoutMode = currentSideLayoutMode();
        if (sideLayoutMode == ConfigContract.SIDE_LAYOUT_TASKS
                || sideLayoutMode == ConfigContract.SIDE_LAYOUT_SYSTEM_RECENTS) {
            return true;
        }
        return sideLayoutMode == ConfigContract.SIDE_LAYOUT_HONEYCOMB
                ? config.honeycombEnabledFor(isLandscape()) && !honeycombTargets.isEmpty()
                : !sideTargets.isEmpty();
    }

    private int currentSideLayoutMode() {
        return config.sideLayoutModeFor(isLandscape());
    }

    private boolean isLandscape() {
        Rect display = displayBounds();
        return display.width() > display.height();
    }

    private void refreshTriggerCapture() {
        triggerCapture.update(canStartBottom(), config.hotWidthPercent, config.hotHeightPercent,
                0, 0);
    }

    private void refreshGeometryAndCaptures(String reason) {
        refreshDisplayGeometry();
        refreshTriggerCapture();
        refreshOutsideCapture();
        Log.i("Display geometry refreshed reason=" + reason + " bounds="
                + cachedDisplayBounds + " landscape=" + isLandscape());
    }

    private void ensureDisplayGeometryCurrent() {
        if (windowManager == null) return;
        try {
            Rect actual = windowManager.getCurrentWindowMetrics().getBounds();
            if (actual.width() == cachedDisplayBounds.width()
                    && actual.height() == cachedDisplayBounds.height()) return;
            refreshDisplayGeometry();
            refreshTriggerCapture();
            Log.i("Display geometry corrected on input bounds=" + cachedDisplayBounds);
        } catch (Throwable ignored) { }
    }

    private void refreshOutsideCaptureAfterLaunch() {
        refreshOutsideCapture();
        mainHandler.postDelayed(this::refreshOutsideCapture, 80L);
        mainHandler.postDelayed(this::refreshOutsideCapture, 240L);
        mainHandler.postDelayed(this::clearFailedLaunchCapture, 500L);
    }

    private void clearFailedLaunchCapture() {
        if (!freeform.abandonStalePendingLaunch(500L)) return;
        outsideCapture.remove();
        outsideGestures.clearAll();
        refreshOutsideCapture();
        Log.i("Outside capture removed because selected app did not enter freeform");
    }

    private void refreshOutsideCapture() {
        if (!outsideEnabledForCurrentDisplay()) {
            outsideCapture.remove();
            outsideGestures.clearAll();
            return;
        }
        if (outsideCaptureWindowTransitionBlocked) {
            outsideCapture.remove();
            return;
        }
        if (honeycombOverlay.isVisible() || shadeExpanded || controlCenterExpanded
                || android.os.SystemClock.uptimeMillis() < systemPanelTouchBlockUntil) {
            outsideCapture.remove();
            return;
        }
        Rect pending = freeform.pendingLaunchBounds();
        List<Rect> visibleWindows = freeform.visibleFreeformBounds();
        if (pending != null && !visibleWindows.contains(pending)) {
            visibleWindows.add(pending);
        }
        boolean hasInteractiveWindow = freeform.hasInteractiveFreeform();
        if (pending == null && !hasInteractiveWindow) visibleWindows.clear();
        boolean inputMethodActive = hasInteractiveWindow && inputMethodActive();
        Rect ime = hasInteractiveWindow ? visibleImeBounds() : null;
        if (inputMethodActive && ime == null) {
            // A floating IME often reports visibility without a stable inset rectangle.
            // A guessed full-width bottom cutout would cover the real keyboard position,
            // so pause capture until HyperOS reports a reliable frame or hides the IME.
            outsideCapture.remove();
            return;
        }
        if (!visibleWindows.isEmpty()) {
            Rect display = displayBounds();
            int statusBarHeight = statusBarGestureHeight();
            visibleWindows.add(new Rect(display.left, display.top,
                    display.right, Math.min(display.bottom,
                    display.top + statusBarHeight)));
        }
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

    private int statusBarGestureHeight() {
        return cachedStatusBarGestureHeight;
    }

    private void refreshDisplayGeometry() {
        int fallbackSide = Math.round(32 * density);
        int sideExtra = Math.round(8 * density);
        try {
            WindowMetrics metrics = windowManager == null ? null
                    : windowManager.getCurrentWindowMetrics();
            if (metrics == null) throw new IllegalStateException("Window metrics unavailable");
            cachedDisplayBounds.set(metrics.getBounds());
            WindowInsets insets = metrics.getWindowInsets();
            cachedDisplaySafeInsets = insets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            Insets gestures = insets.getInsets(WindowInsets.Type.systemGestures());
            int systemLeft = gestures.left > 0 ? gestures.left : fallbackSide;
            int systemRight = gestures.right > 0 ? gestures.right : fallbackSide;
            cachedSystemSideGestureInsets = Insets.of(systemLeft, 0, systemRight, 0);
            cachedSideGestureReserves = Insets.of(
                    gestures.left > 0 ? gestures.left + sideExtra : fallbackSide, 0,
                    gestures.right > 0 ? gestures.right + sideExtra : fallbackSide, 0);
        } catch (Throwable error) {
            cachedDisplayBounds.set(0, 0,
                    context.getResources().getDisplayMetrics().widthPixels,
                    context.getResources().getDisplayMetrics().heightPixels);
            cachedDisplaySafeInsets = Insets.of(0, Math.round(48 * density), 0,
                    Math.round(32 * density));
            cachedSystemSideGestureInsets = Insets.of(fallbackSide, 0, fallbackSide, 0);
            cachedSideGestureReserves = Insets.of(fallbackSide, 0, fallbackSide, 0);
        }
        int resourceHeight = 0;
        try {
            int resourceId = context.getResources().getIdentifier(
                    "status_bar_height", "dimen", "android");
            if (resourceId != 0) {
                resourceHeight = context.getResources().getDimensionPixelSize(resourceId);
            }
        } catch (Throwable ignored) {}
        cachedStatusBarGestureHeight = Math.max(cachedDisplaySafeInsets.top,
                Math.max(resourceHeight, Math.round(48 * density)));
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
        return cachedDisplayBounds;
    }

    private boolean outsideEnabledForCurrentDisplay() {
        Rect display = displayBounds();
        return config.outsideEnabledFor(display.width() > display.height());
    }

    private Insets sideGestureReserves() {
        return cachedSideGestureReserves;
    }

    private Insets systemSideGestureInsets() {
        return cachedSystemSideGestureInsets;
    }

    private Insets displaySafeInsets() {
        return cachedDisplaySafeInsets;
    }

    private Rect visibleImeBounds() {
        try {
            WindowMetrics metrics = context.getSystemService(WindowManager.class)
                    .getCurrentWindowMetrics();
            WindowInsets windowInsets = metrics.getWindowInsets();
            HyperOsFreeformBridge.ImeState shellIme = freeform.inputMethodState();
            boolean insetsVisible = windowInsets.isVisible(WindowInsets.Type.ime());
            boolean visible = shellIme.visible || insetsVisible || imeVisible;
            if (!visible) return null;
            Insets ime = windowInsets.getInsets(WindowInsets.Type.ime());
            Rect display = new Rect(metrics.getBounds());
            // A docked keyboard contributes a bottom inset. Floating keyboards usually
            // only report visibility/height, not their screen position; do not invent a
            // bottom-aligned rectangle for them.
            int visibleHeight = Math.max(ime.bottom, shellIme.bottomInset);
            if (visibleHeight <= 0 || visibleHeight >= display.height()) return null;
            return new Rect(display.left, display.bottom - visibleHeight,
                    display.right, display.bottom);
        } catch (Throwable error) {
            return null;
        }
    }

    private boolean inputMethodActive() {
        HyperOsFreeformBridge.ImeState shellIme = freeform.inputMethodState();
        if (shellIme.visible || imeVisible) return true;
        try {
            WindowMetrics metrics = context.getSystemService(WindowManager.class)
                    .getCurrentWindowMetrics();
            return metrics.getWindowInsets().isVisible(WindowInsets.Type.ime());
        } catch (Throwable ignored) {
            return false;
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
        if (!outsideEnabledForCurrentDisplay()) {
            outsideCapture.remove();
            outsideGestures.clearAll();
            clearCapturedOutsideState();
            return;
        }
        if (systemPanelInputBlocked()) {
            outsideCapture.remove();
            outsideGestures.clearAll();
            clearCapturedOutsideState();
            return;
        }
        int width = displayBounds().width();
        if (action == MotionEvent.ACTION_DOWN) {
            Insets systemSides = systemSideGestureInsets();
            boolean nativeSideEdge = GestureGeometry.sideAt(x, width,
                    systemSides.left, systemSides.right) != null;
            // The capture window owns this touch stream. A side-edge tap therefore
            // still has to enter the outside-tap recognizer; consuming it here without
            // arming the recognizer leaves an apparently dead strip on both sides.
            // Keep the wider side tolerance so a real back swipe yields instead of
            // being mistaken for a tap. The global input monitor continues handling
            // that yielded swipe and preserves the system back gesture.
            if (!nativeSideEdge) ignoredCaptureDownTime = downTime;
            capturedImeDismissTap = false;
            Rect ime = visibleImeBounds();
            boolean imeActive = imeVisible || ime != null;
            float leftReserve = nativeSideEdge ? systemSides.left : 0f;
            float rightReserve = nativeSideEdge ? systemSides.right : 0f;
            Rect pending = freeform.pendingLaunchBounds();
            if (pending != null) {
                outsideGestures.onDown(x, y, pending, width,
                        leftReserve, rightReserve, eventTime);
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
            outsideGestures.onDown(x, y, tracked, width,
                    leftReserve, rightReserve, eventTime);
            if (nativeSideEdge) {
                Log.i("Outside side-edge tap armed through capture window");
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

    private boolean systemPanelInputBlocked() {
        return shadeExpanded || controlCenterExpanded
                || android.os.SystemClock.uptimeMillis() < systemPanelTouchBlockUntil;
    }

    private void performOutsideAction(boolean doubleTap) {
        if (!outsideEnabledForCurrentDisplay()) {
            outsideGestures.clearAll();
            return;
        }
        if (systemPanelInputBlocked()) {
            outsideGestures.clearAll();
            Log.i("Ignored pending outside action while a system panel is active");
            return;
        }
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
            case ConfigContract.ACTION_EDGE_PIN:
                handled = freeform.edgePinTrackedOrPendingLaunch();
                break;
            default:
                Log.i("Outside " + (doubleTap ? "double" : "single") + " tap: no action");
                return;
        }
        if (handled) {
            outsideGestures.clearAll();
            suspendOutsideCaptureForWindowTransition("outside action=" + action);
        }
    }

    private void suspendOutsideCaptureForWindowTransition(String reason) {
        outsideCaptureWindowTransitionBlocked = true;
        long generation = ++outsideCaptureWindowTransitionGeneration;
        outsideCapture.remove();
        outsideGestures.clearAll();
        mainHandler.postDelayed(() -> settleOutsideCaptureWindowTransition(generation), 180L);
        mainHandler.postDelayed(() -> settleOutsideCaptureWindowTransition(generation), 500L);
        mainHandler.postDelayed(() -> settleOutsideCaptureWindowTransition(generation), 900L);
        mainHandler.postDelayed(() -> finishOutsideCaptureWindowTransition(generation), 1400L);
        Log.i("Outside capture paused for window transition reason=" + reason);
    }

    private void settleOutsideCaptureWindowTransition(long generation) {
        if (generation != outsideCaptureWindowTransitionGeneration
                || !outsideCaptureWindowTransitionBlocked) return;
        if (!freeform.hasInteractiveFreeform()) {
            outsideCaptureWindowTransitionBlocked = false;
            refreshOutsideCapture();
            Log.i("Outside capture transition settled in non-interactive state");
        }
    }

    private void finishOutsideCaptureWindowTransition(long generation) {
        if (generation != outsideCaptureWindowTransitionGeneration
                || !outsideCaptureWindowTransitionBlocked) return;
        outsideCaptureWindowTransitionBlocked = false;
        refreshOutsideCapture();
        Log.i("Outside capture transition timeout restored current state");
    }

    private void releaseOutsideCaptureTransitionIfSettled() {
        if (!outsideCaptureWindowTransitionBlocked || freeform.hasInteractiveFreeform()) return;
        outsideCaptureWindowTransitionBlocked = false;
        outsideCaptureWindowTransitionGeneration++;
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
            ArrayList<RuntimeTarget> resolvedHoneycomb = new ArrayList<>();
            ArrayList<RuntimeTarget> resolvedSide = new ArrayList<>();
            PackageManager packageManager = context.getPackageManager();
            Map<String, RuntimeTarget> targetCache = new HashMap<>();
            for (GestureConfig.TargetSpec target : next.targets) {
                try {
                    RuntimeTarget runtimeTarget = resolveTarget(packageManager, target,
                            targetCache);
                    if (runtimeTarget != null) resolved.add(runtimeTarget);
                } catch (Throwable error) {
                    Log.e("Configured target is unavailable", error);
                }
            }
            for (GestureConfig.TargetSpec target : next.honeycombTargets) {
                try {
                    RuntimeTarget runtimeTarget = resolveTarget(packageManager, target,
                            targetCache);
                    if (runtimeTarget != null) resolvedHoneycomb.add(runtimeTarget);
                } catch (Throwable error) {
                    Log.e("Configured honeycomb target is unavailable", error);
                }
            }
            for (GestureConfig.TargetSpec target : next.sideTargets) {
                try {
                    RuntimeTarget runtimeTarget = resolveTarget(packageManager, target,
                            targetCache);
                    if (runtimeTarget != null) resolvedSide.add(runtimeTarget);
                } catch (Throwable error) {
                    Log.e("Configured side target is unavailable", error);
                }
            }
            if (resolved.size() > next.fanMaxTargets) {
                resolved.subList(next.fanMaxTargets, resolved.size()).clear();
            }
            if (resolvedHoneycomb.size() > next.honeycombMaxTargets) {
                resolvedHoneycomb.subList(next.honeycombMaxTargets,
                        resolvedHoneycomb.size()).clear();
            }
            if (resolvedSide.size() > next.sideMaxTargets) {
                resolvedSide.subList(next.sideMaxTargets, resolvedSide.size()).clear();
            }
            List<RuntimeTarget> nextTargets = Collections.unmodifiableList(resolved);
            List<RuntimeTarget> nextHoneycombTargets = Collections.unmodifiableList(
                    resolvedHoneycomb);
            List<RuntimeTarget> nextSideTargets = Collections.unmodifiableList(resolvedSide);
            configRetryCount = 0;
            configRetryScheduled = false;
            mainHandler.post(() -> {
                if (wheelSessionActive || overlay.isWheelVisible()) {
                    wheelSessionActive = false;
                    overlay.removeNow();
                }
                config = next;
                outsideGestures.setDoubleTapTimeout(next.outsideTapWindowMs);
                targets = nextTargets;
                honeycombTargets = nextHoneycombTargets;
                sideTargets = nextSideTargets;
                if (activeTargets.isEmpty()) activeTargets = nextTargets;
                freeform.updateConfig(next);
                if (!next.honeycombEnabled && honeycombOverlay.isVisible()) {
                    honeycombOverlay.dismiss();
                }
                if (!next.enabled || nextTargets.size() < 3) resetFan();
                refreshTriggerCapture();
                refreshOutsideCapture();
                Log.i("Configuration loaded targets=" + nextTargets.size() + " trigger="
                        + next.triggerPercent + "% selection=" + next.selectionRadiusPercent
                        + "% hot=" + next.hotWidthPercent + "x" + next.hotHeightPercent
                        + "% icon=" + next.iconSizeDp + "dp side="
                        + next.sideGestureEnabled + "@" + next.sideTriggerPercent
                        + "% sideHold=" + next.sideHoldEnabled + "@"
                        + next.sideTriggerPercent + "%/"
                        + next.sideHoldDelayMs + "ms"
                        + " sideIcon=" + next.sideIconSizeDp + "dp safeTop="
                        + next.sideTopSafeMarginPercent + "% names="
                        + next.showSelectedAppName + " follow=" + next.sideFollowFinger
                        + " layout=" + next.sidePortraitLayoutMode + "/"
                        + next.sideLandscapeLayoutMode + " ringSize="
                        + next.sideRingSizePercent + "%"
                        + " wheel=" + next.sideWheelMode + " reverseCancel="
                        + next.sideReverseCancelPercent + "% animation="
                        + next.fanAnimationsEnabled + "@" + next.fanAnimationSpeed
                        + "% reveal=" + next.fanRevealAmount + "% rotation="
                        + next.fanRotationDegrees + "deg ring=" + next.fanSelectionRing
                        + " circularIcons=" + next.forceCircularIcons
                        + " honeycomb=" + next.honeycombEnabled + "/"
                        + nextHoneycombTargets.size() + " mode=" + next.honeycombMode
                        + " bottomFullscreen=" + next.bottomPortraitFullscreen + "/"
                        + next.bottomLandscapeFullscreen
                        + " sideFullscreen=" + next.sidePortraitFullscreen + "/"
                        + next.sideLandscapeFullscreen
                        + " bottomFreeform=" + next.bottomPortraitHoneycombFreeform + "/"
                        + next.bottomLandscapeHoneycombFreeform
                        + " blankSettle=" + BOTTOM_HONEYCOMB_SETTLE_MS
                        + "ms returnToFan=" + next.triggerPercent + "% outsideTap="
                        + next.outsideTapWindowMs + "ms outside="
                        + next.outsidePortraitEnabled + "/"
                        + next.outsideLandscapeEnabled + " honeycombFollow="
                        + next.honeycombFollowFinger);
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

    private RuntimeTarget resolveTarget(PackageManager packageManager,
                                        GestureConfig.TargetSpec target) throws Exception {
        if (target.isShortcut()) {
            return new RuntimeTarget(target.packageName, target.shortcutId,
                    target.shortcutIntentUri,
                    target.shortcutLabel.isEmpty() ? target.shortcutId
                            : target.shortcutLabel,
                    ShortcutIconLoader.load(context, target.packageName,
                            target.shortcutId, target.userId), target.userId);
        }
        ComponentName component = ComponentName.unflattenFromString(target.component);
        if (component == null) return null;
        ActivityInfo info = packageManager.getActivityInfo(component, 0);
        CharSequence label = info.loadLabel(packageManager);
        return new RuntimeTarget(component,
                label == null ? component.getPackageName() : label.toString(),
                info.loadIcon(packageManager), target.userId);
    }

    private RuntimeTarget resolveTarget(PackageManager packageManager,
                                        GestureConfig.TargetSpec target,
                                        Map<String, RuntimeTarget> cache) throws Exception {
        String key = target.component + '\u0000' + target.packageName + '\u0000'
                + target.shortcutId + '\u0000' + target.shortcutIntentUri + '\u0000'
                + target.userId;
        RuntimeTarget cached = cache.get(key);
        if (cached != null) return cached;
        RuntimeTarget resolved = resolveTarget(packageManager, target);
        if (resolved != null) cache.put(key, resolved);
        return resolved;
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
