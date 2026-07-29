package com.oxohang.fanfreeform.xposed;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import com.oxohang.fanfreeform.config.ConfigContract;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Executor;

import de.robv.android.xposed.XposedHelpers;

final class HyperOsFreeformBridge {
    static final class ImeState {
        final boolean known;
        final boolean visible;
        final int height;

        ImeState(boolean known, boolean visible, int height) {
            this.known = known;
            this.visible = visible;
            this.height = Math.max(0, height);
        }
    }

    private static final long MATCH_WINDOW_MS = 6000;
    private static final long LAUNCH_INPUT_PROTECTION_MS = 1000;
    private static final ThreadLocal<Integer> FORCE_RIGHT_MINI_TASK = new ThreadLocal<>();

    private final Context context;
    private final ClassLoader classLoader;
    private final Handler mainHandler;
    private final Object taskLock = new Object();
    private final FreeformTaskRegistry tasks = new FreeformTaskRegistry();
    private final LinkedHashSet<Integer> visibleFreeformTaskIds = new LinkedHashSet<>();
    private final LinkedHashSet<Integer> normalFreeformTaskIds = new LinkedHashSet<>();
    private final LinkedHashSet<Integer> edgePinnedTaskIds = new LinkedHashSet<>();

    private volatile Object controller;
    private volatile int trackedTaskId = -1;
    private volatile int externalTrackedTaskId = -1;
    private volatile String pendingPackage;
    private volatile int pendingUserId;
    private volatile long pendingSince;
    private volatile Rect pendingLaunchVisualBounds;
    private volatile long launchInputProtectionUntil;
    private volatile int launchInputProtectionTaskId = -1;
    private volatile boolean pendingLaunchDismissRequested;
    private volatile int pendingLaunchDeferredAction = ConfigContract.ACTION_NONE;
    private volatile Intent pendingLaunchIntent;
    private volatile Bundle pendingLaunchOptions;
    private volatile boolean pendingResizeRetry;
    private volatile boolean pendingShortcutLaunch;
    private volatile String recentShortcutPackage;
    private volatile int recentShortcutUserId;
    private volatile long recentShortcutUntil;
    private volatile Rect recentShortcutLaunchBounds;
    private volatile String recentLandscapeLaunchPackage;
    private volatile int recentLandscapeLaunchUserId;
    private volatile long recentLandscapeLaunchUntil;
    private volatile Rect recentLandscapeLaunchBounds;
    private volatile int lastShortcutAlignedTaskId = -1;
    private volatile long lastShortcutAlignmentAt;
    private volatile int lastShortcutPreparedTaskId = -1;
    private volatile long lastShortcutPreparedAt;
    private volatile GestureConfig latestConfig = GestureConfig.defaults();
    private volatile RuntimeTarget queuedLaunchTarget;
    private volatile GestureConfig queuedLaunchConfig;
    private volatile Runnable queuedLaunchArmedCallback;
    private volatile int queuedAfterSuspendTaskId = -1;
    private volatile long queuedLaunchGeneration;

    HyperOsFreeformBridge(Context context, ClassLoader classLoader, Handler mainHandler) {
        this.context = context;
        this.classLoader = classLoader;
        this.mainHandler = mainHandler;
    }

    void setController(Object controller) {
        if (controller != null) {
            this.controller = controller;
            Log.i("MiuiFreeformModeController connected");
        }
    }

    boolean hasController() {
        return controller != null;
    }

    void updateConfig(GestureConfig config) {
        if (config != null) latestConfig = config;
    }

    boolean launch(RuntimeTarget target, GestureConfig config, Runnable onLaunchArmed) {
        if (queueLaunchAfterSuspendingCurrent(target, config, onLaunchArmed)) return true;
        return launchNow(target, config, onLaunchArmed);
    }

    private boolean queueLaunchAfterSuspendingCurrent(RuntimeTarget target,
                                                      GestureConfig config,
                                                      Runnable onLaunchArmed) {
        int taskId = interactionTaskId();
        Object info = taskInfo(taskId);
        if (taskId < 0 || info == null || controller == null
                || !booleanCall(info, "isNormalState", false)
                || booleanCall(info, "isMiniState", false) || isPinned(info)
                || (target.packageName.equals(packageName(info))
                && target.userId == userId(info))) {
            return false;
        }
        long generation;
        synchronized (taskLock) {
            queuedLaunchTarget = target;
            queuedLaunchConfig = config;
            queuedLaunchArmedCallback = onLaunchArmed;
            queuedAfterSuspendTaskId = taskId;
            generation = ++queuedLaunchGeneration;
        }
        if (!miniTask(taskId, info)) {
            clearQueuedLaunch(generation);
            return false;
        }
        mainHandler.postDelayed(() -> runQueuedLaunchAfterSuspend(taskId, generation,
                "suspend timeout"), 700L);
        Log.i("Queued second freeform until current task is suspended current=" + taskId
                + " target=" + target.packageName);
        return true;
    }

    private boolean launchNow(RuntimeTarget target, GestureConfig config,
                              Runnable onLaunchArmed) {
        String packageName = target.packageName;
        try {
            Class<?> manager = XposedHelpers.findClass("miui.app.MiuiFreeFormManager", classLoader);
            Object result = XposedHelpers.callStaticMethod(manager, "getActivityOptions",
                    context, packageName, true, false);
            ActivityOptions options;
            if (result instanceof ActivityOptions) {
                options = (ActivityOptions) result;
            } else {
                options = ActivityOptions.makeBasic();
                XposedHelpers.callMethod(options, "setLaunchWindowingMode", 5);
            }

            float scale = readFreeformScale(options);
            Rect launchBounds = customLaunchBounds(config, scale);
            options.setLaunchBounds(launchBounds);
            if (target.isShortcut()) {
                try {
                    XposedHelpers.callMethod(options, "setForceLaunchNewTask");
                    Log.i("Shortcut dispatcher isolated in a new task package="
                            + packageName);
                } catch (Throwable error) {
                    Log.e("Cannot isolate shortcut dispatcher task; continuing safely",
                            error);
                }
            }

            Intent intent = target.isShortcut()
                    ? new Intent(Intent.ACTION_MAIN).setPackage(packageName)
                    : new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(target.component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ActivityManager.RunningTaskInfo existing = target.isShortcut() ? null
                    : findRunningTask(packageName, target.userId);
            if (existing != null) {
                forceTaskResizable(existing.taskId);
            }
            Bundle launchOptions = options.toBundle();
            beginPendingMatch(packageName, target.userId,
                    visualBounds(launchBounds, scale), launchBounds,
                    intent, launchOptions, target.isShortcut());
            notifyLaunchArmed(onLaunchArmed);
            if (target.isShortcut()) {
                dispatchShortcut(target, launchOptions);
            } else if (target.userId != 0) {
                Intent request = new Intent(ShortcutHostRuntime.ACTION_START_ACTIVITY)
                        .setPackage(ShortcutHostRuntime.MIUI_HOME)
                        .putExtra(ShortcutHostRuntime.EXTRA_COMPONENT,
                                target.component.flattenToString())
                        .putExtra(ShortcutHostRuntime.EXTRA_USER_ID, target.userId)
                        .putExtra(ShortcutHostRuntime.EXTRA_OPTIONS, launchOptions);
                context.sendBroadcast(request);
            } else {
                context.startActivity(intent, launchOptions);
            }
            Log.i("Preparing or launching " + (target.isShortcut()
                    ? packageName + "/" + target.shortcutId
                    : target.component.flattenToShortString()
                    + (target.userId == 0 ? "" : " user=" + target.userId))
                    + " in freeform without home reorder bounds=" + launchBounds
                    + " scale=" + scale);
            scheduleScans();
            return true;
        } catch (Throwable error) {
            clearPendingMatch();
            Log.e("Native freeform launch failed", error);
            return false;
        }
    }

    private void dispatchShortcut(RuntimeTarget target, Bundle launchOptions) {
        refreshPendingLaunchTimestamp();
        Intent request = new Intent(ShortcutHostRuntime.ACTION_START_SHORTCUT)
                .setPackage(ShortcutHostRuntime.MIUI_HOME)
                .putExtra(ShortcutHostRuntime.EXTRA_PACKAGE, target.packageName)
                .putExtra(ShortcutHostRuntime.EXTRA_SHORTCUT_ID, target.shortcutId)
                .putExtra(ShortcutHostRuntime.EXTRA_OPTIONS, launchOptions);
        context.sendBroadcast(request);
        Log.i("Shortcut dispatched in isolated task " + target.packageName
                + "/" + target.shortcutId);
    }

    void prepareShortcutEnterAnimation(Object change, Object taskInfo) {
        if (change == null || taskInfo == null) return;
        ActivityManager.RunningTaskInfo running;
        try {
            Object value = XposedHelpers.callMethod(taskInfo, "getTaskInfo");
            if (!(value instanceof ActivityManager.RunningTaskInfo)) return;
            running = (ActivityManager.RunningTaskInfo) value;
        } catch (Throwable ignored) {
            return;
        }
        Rect configuredBounds;
        boolean landscapeLaunch;
        long now = SystemClock.elapsedRealtime();
        synchronized (taskLock) {
            String actualPackage = packageName(running);
            int actualUserId = taskUserId(running);
            boolean shortcutLaunch = recentShortcutPackage != null
                    && now <= recentShortcutUntil
                    && recentShortcutPackage.equals(actualPackage)
                    && recentShortcutUserId == actualUserId;
            landscapeLaunch = recentLandscapeLaunchPackage != null
                    && now <= recentLandscapeLaunchUntil
                    && recentLandscapeLaunchPackage.equals(actualPackage)
                    && recentLandscapeLaunchUserId == actualUserId;
            if (!shortcutLaunch && !landscapeLaunch) {
                return;
            }
            if (lastShortcutPreparedTaskId == running.taskId
                    && now - lastShortcutPreparedAt < 700L) {
                return;
            }
            Rect savedBounds = landscapeLaunch
                    ? recentLandscapeLaunchBounds : recentShortcutLaunchBounds;
            configuredBounds = savedBounds == null ? null : new Rect(savedBounds);
            if (configuredBounds == null || configuredBounds.isEmpty()) return;
            lastShortcutPreparedTaskId = running.taskId;
            lastShortcutPreparedAt = now;
        }
        try {
            setRectResult(change, "getStartAbsBounds", configuredBounds);
            setRectResult(change, "getEndAbsBounds", configuredBounds);

            Object configuration = XposedHelpers.getObjectField(running,
                    "configuration");
            Object windowConfiguration = XposedHelpers.getObjectField(
                    configuration, "windowConfiguration");
            XposedHelpers.callMethod(windowConfiguration, "setBounds",
                    new Rect(configuredBounds));

            Object internalTaskBounds = XposedHelpers.getObjectField(taskInfo,
                    "mTaskBounds");
            if (internalTaskBounds instanceof Rect) {
                ((Rect) internalTaskBounds).set(configuredBounds);
            }
            float scale = numberCall(taskInfo, "getFreeformScale", 0.7f);
            Object baseTarget = XposedHelpers.callMethod(taskInfo,
                    "getBaseAnimTarget");
            XposedHelpers.callMethod(baseTarget, "setBaseAnimTargetParam",
                    new Rect(configuredBounds), scale, scale, 0f);

            Object owner = controller;
            if (owner != null) {
                Object organizer = XposedHelpers.getObjectField(owner,
                        "mRootTaskDisplayAreaOrganizer");
                Object token = XposedHelpers.getObjectField(running, "token");
                Class<?> transactionClass = XposedHelpers.findClass(
                        "android.window.WindowContainerTransaction", null);
                Object transaction = transactionClass.getDeclaredConstructor().newInstance();
                XposedHelpers.callMethod(transaction, "setBounds", token,
                        new Rect(configuredBounds));
                XposedHelpers.callMethod(organizer, "applyTransaction", transaction);
            }
            Log.i((landscapeLaunch ? "Landscape" : "Shortcut")
                    + " enter animation prepared at configured bounds task="
                    + running.taskId + " bounds=" + configuredBounds
                    + " top=" + running.topActivity);
        } catch (Throwable error) {
            Log.e("Cannot prepare configured enter animation task=" + running.taskId,
                    error);
        }
    }

    private static void setRectResult(Object target, String method, Rect bounds) {
        try {
            Object result = XposedHelpers.callMethod(target, method);
            if (result instanceof Rect) ((Rect) result).set(bounds);
        } catch (Throwable ignored) { }
    }

    private void refreshPendingLaunchTimestamp() {
        synchronized (taskLock) {
            if (pendingPackage == null) return;
            pendingSince = SystemClock.elapsedRealtime();
            launchInputProtectionUntil = pendingSince + LAUNCH_INPUT_PROTECTION_MS;
        }
    }

    void onTaskAppeared(int taskId) {
        Object info = taskInfo(taskId);
        if (info != null) onTaskInfo(info);
    }

    void onTaskInfo(Object info) {
        if (info == null) return;
        int taskId = intCall(info, "getTaskId", -1);
        if (taskId < 0) return;
        boolean normal = booleanCall(info, "isNormalState", false);
        boolean mini = booleanCall(info, "isMiniState", false);
        boolean pinned = isPinned(info);
        String actual = packageName(info);
        boolean retainTransition = false;
        boolean alignShortcutTask = false;
        int dismissMatchedTaskId = -1;
        int deferredMatchedAction = ConfigContract.ACTION_NONE;

        synchronized (taskLock) {
            updateVisibleStateLocked(taskId, normal, mini, pinned);
            String expected = pendingPackage;
            boolean inMatchWindow = expected != null
                    && SystemClock.elapsedRealtime() - pendingSince <= MATCH_WINDOW_MS;
            boolean matchesPending = inMatchWindow && expected.equals(actual)
                    && pendingUserId == userId(info) && !pinned;
            boolean matchesRecentShortcut = normal && recentShortcutPackage != null
                    && SystemClock.elapsedRealtime() <= recentShortcutUntil
                    && recentShortcutPackage.equals(actual)
                    && recentShortcutUserId == userId(info);
            if (matchesPending && pendingLaunchDismissRequested && (normal || mini)) {
                tasks.put(taskId, actual, normal
                        ? FreeformTaskRegistry.State.NORMAL
                        : FreeformTaskRegistry.State.SUSPENDED);
                removeTaskLocked(taskId, "startup outside tap requested close");
                clearPendingMatchLocked();
                dismissMatchedTaskId = taskId;
            } else {
                FreeformTaskRegistry.Record record = tasks.get(taskId);
                if (record != null) {
                    if (pinned) {
                        removeTaskLocked(taskId, "entered pin mode");
                        return;
                    }
                    if (mini) {
                        tasks.markSuspended(taskId);
                        if (trackedTaskId == taskId) trackedTaskId = -1;
                    } else if (normal) {
                        tasks.markNormal(taskId);
                        trackedTaskId = taskId;
                        externalTrackedTaskId = -1;
                        Log.i("Owned task interactive task=" + taskId
                                + " tracked=" + trackedTaskId + " ownedCount=" + tasks.size());
                    } else {
                        retainTransition = true;
                    }
                }

                if (matchesPending) {
                    if (mini) {
                        tasks.put(taskId, actual, FreeformTaskRegistry.State.SUSPENDED);
                        trackedTaskId = -1;
                        finishPendingMatchLocked();
                        Log.i("Fan-selected task remains in native mini state task=" + taskId);
                    } else if (normal) {
                        alignShortcutTask = pendingShortcutLaunch || matchesRecentShortcut;
                        tasks.put(taskId, actual, FreeformTaskRegistry.State.NORMAL);
                        trackedTaskId = taskId;
                        externalTrackedTaskId = -1;
                        launchInputProtectionTaskId = taskId;
                        deferredMatchedAction = pendingLaunchDeferredAction;
                        finishPendingMatchLocked();
                        Log.i("Tracking fan-launched task=" + taskId + " package=" + actual
                                + " tracked=" + trackedTaskId + " ownedCount=" + tasks.size());
                    }
                }
                if (matchesRecentShortcut) alignShortcutTask = true;
                if (record == null && !matchesPending) {
                    if (normal && !mini && !pinned) {
                        if (externalTrackedTaskId != taskId) {
                            Log.i("Tracking native external freeform task=" + taskId
                                    + " package=" + actual);
                        }
                        externalTrackedTaskId = taskId;
                    } else if (externalTrackedTaskId == taskId) {
                        externalTrackedTaskId = -1;
                        Log.i("Native external freeform no longer interactive task=" + taskId);
                    }
                }
            }
        }

        if (dismissMatchedTaskId >= 0) {
            scheduleDismiss(dismissMatchedTaskId);
            Log.i("Startup outside tap closed matched task=" + dismissMatchedTaskId);
            return;
        }
        if (alignShortcutTask) alignShortcutTaskToConfiguredBounds(taskId);
        if (deferredMatchedAction != ConfigContract.ACTION_NONE) {
            performDeferredLaunchAction(deferredMatchedAction);
            return;
        }
        if (mini) {
            runQueuedLaunchAfterSuspend(taskId, queuedLaunchGeneration,
                    "current task entered mini state");
        }
        if (retainTransition) retainOwnershipDuringModeTransition(taskId);
    }

    void onTaskVanished(int taskId) {
        synchronized (taskLock) {
            if (externalTrackedTaskId == taskId) externalTrackedTaskId = -1;
            visibleFreeformTaskIds.remove(taskId);
            normalFreeformTaskIds.remove(taskId);
            edgePinnedTaskIds.remove(taskId);
            removeTaskLocked(taskId, "task vanished");
        }
    }

    Rect trackedBounds() {
        int taskId = interactionTaskId();
        if (taskId < 0) return null;
        boolean owned;
        synchronized (taskLock) {
            owned = tasks.contains(taskId);
        }
        Object info = taskInfo(taskId);
        if (info == null) {
            synchronized (taskLock) {
                if (taskId == launchInputProtectionTaskId
                        && SystemClock.elapsedRealtime() <= launchInputProtectionUntil) {
                    return null;
                }
                if (owned) removeTaskLocked(taskId, "task info unavailable");
                else if (externalTrackedTaskId == taskId) externalTrackedTaskId = -1;
            }
            return null;
        }
        boolean normal = booleanCall(info, "isNormalState", false);
        boolean mini = booleanCall(info, "isMiniState", false);
        boolean pinned = isPinned(info);
        if (!normal || mini || pinned) {
            if (!owned) {
                if (externalTrackedTaskId == taskId) externalTrackedTaskId = -1;
            } else if (mini && !pinned) {
                synchronized (taskLock) {
                    tasks.markSuspended(taskId);
                    if (trackedTaskId == taskId) trackedTaskId = -1;
                }
            } else if (!pinned) {
                retainOwnershipDuringModeTransition(taskId);
            } else {
                synchronized (taskLock) {
                    removeTaskLocked(taskId, "tracked bounds no longer normal freeform");
                }
            }
            return null;
        }
        try {
            Object bounds = XposedHelpers.callMethod(info, "getScaledBounds");
            return bounds instanceof Rect ? new Rect((Rect) bounds) : null;
        } catch (Throwable error) {
            Log.e("Cannot read tracked task bounds", error);
            return null;
        }
    }

    List<Rect> visibleFreeformBounds() {
        List<Integer> taskIds;
        synchronized (taskLock) {
            taskIds = new ArrayList<>(visibleFreeformTaskIds);
        }
        List<Rect> bounds = new ArrayList<>();
        Rect display = new Rect(context.getSystemService(WindowManager.class)
                .getCurrentWindowMetrics().getBounds());
        ImeState imeState = inputMethodState();
        for (int taskId : taskIds) {
            Object info = taskInfo(taskId);
            if (info == null) {
                synchronized (taskLock) {
                    visibleFreeformTaskIds.remove(taskId);
                    normalFreeformTaskIds.remove(taskId);
                }
                continue;
            }
            boolean visible = booleanCall(info, "isNormalState", false)
                    || booleanCall(info, "isMiniState", false) || isPinned(info);
            if (!visible) {
                synchronized (taskLock) {
                    visibleFreeformTaskIds.remove(taskId);
                    normalFreeformTaskIds.remove(taskId);
                }
                continue;
            }
            try {
                boolean animating = isTaskAnimating(taskId);
                addUniqueVisibleBounds(bounds,
                        XposedHelpers.callMethod(info, "getScaledBounds"), display);
                if (animating) {
                    addUniqueVisibleBounds(bounds,
                            XposedHelpers.callMethod(info, "getScaledDestinationBounds"),
                            display);
                }
                if (animating || imeState.visible) {
                    Object restoreValue = XposedHelpers.callMethod(info,
                            "getRestoreNormalBounds");
                    if (restoreValue instanceof Rect && !((Rect) restoreValue).isEmpty()) {
                        float scale = numberCall(info, "getFreeformScale", 1f);
                        addUniqueVisibleBounds(bounds,
                                visualBounds((Rect) restoreValue, scale), display);
                    }
                }
            } catch (Throwable error) {
                Log.e("Cannot read visible freeform bounds task=" + taskId, error);
            }
        }
        return bounds;
    }

    private static void addUniqueVisibleBounds(List<Rect> bounds, Object value,
                                               Rect display) {
        if (!(value instanceof Rect) || ((Rect) value).isEmpty()) return;
        Rect copy = new Rect((Rect) value);
        if (!copy.intersect(display) || copy.isEmpty()) return;
        long displayArea = (long) display.width() * display.height();
        long area = (long) copy.width() * copy.height();
        boolean coversDisplay = copy.width() >= display.width() * 0.96f
                && copy.height() >= display.height() * 0.96f;
        if (coversDisplay || area >= displayArea * 0.92f) {
            Log.i("Ignoring transient fullscreen bounds for outside capture=" + copy);
            return;
        }
        if (!bounds.contains(copy)) bounds.add(copy);
    }

    private boolean isTaskAnimating(int taskId) {
        Object owner = controller;
        if (owner == null) return false;
        try {
            Object animation = XposedHelpers.getObjectField(owner,
                    "mMiuiFreeformModeAnimation");
            Object result = XposedHelpers.callMethod(animation, "isAnimating", taskId);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    boolean hasInteractiveFreeform() {
        synchronized (taskLock) {
            return !normalFreeformTaskIds.isEmpty();
        }
    }

    boolean dismissTracked() {
        int taskId = interactionTaskId();
        if (taskId < 0 || controller == null) return false;
        synchronized (taskLock) {
            clearInteractiveTaskLocked(taskId, "native close requested");
        }
        return scheduleDismiss(taskId);
    }

    Rect pendingLaunchBounds() {
        Rect bounds = pendingLaunchVisualBounds;
        long now = SystemClock.elapsedRealtime();
        boolean awaitingMatch = pendingPackage != null && now - pendingSince <= MATCH_WINDOW_MS;
        if (bounds == null || (!awaitingMatch && now > launchInputProtectionUntil)) return null;
        return new Rect(bounds);
    }

    boolean abandonStalePendingLaunch(long minimumAgeMs) {
        String failedPackage;
        long age;
        synchronized (taskLock) {
            if (pendingPackage == null) return false;
            age = SystemClock.elapsedRealtime() - pendingSince;
            if (age < Math.max(0L, minimumAgeMs)) return false;
            failedPackage = pendingPackage;
            clearPendingMatchLocked();
        }
        Log.i("Freeform launch did not materialize; clearing input protection package="
                + failedPackage + " age=" + age + "ms");
        return true;
    }

    boolean dismissTrackedOrPendingLaunch() {
        int taskId = -1;
        synchronized (taskLock) {
            if (launchInputProtectionTaskId >= 0
                    && SystemClock.elapsedRealtime() <= launchInputProtectionUntil
                    && tasks.contains(launchInputProtectionTaskId)) {
                taskId = launchInputProtectionTaskId;
                removeTaskLocked(taskId, "protected startup outside tap requested close");
            } else if (pendingPackage != null
                    && SystemClock.elapsedRealtime() - pendingSince <= MATCH_WINDOW_MS) {
                pendingLaunchDismissRequested = true;
                pendingLaunchDeferredAction = ConfigContract.ACTION_NONE;
                Log.i("Startup outside tap queued close package=" + pendingPackage);
                return true;
            } else {
                taskId = interactionTaskId();
                if (taskId >= 0) {
                    clearInteractiveTaskLocked(taskId, "outside tap requested close");
                }
            }
        }
        return taskId >= 0 && controller != null && scheduleDismiss(taskId);
    }

    boolean fullscreenTrackedOrPendingLaunch() {
        if (queuePendingLaunchAction(ConfigContract.ACTION_FULLSCREEN)) return true;
        return fullscreenTracked();
    }

    boolean miniTrackedOrPendingLaunch() {
        if (queuePendingLaunchAction(ConfigContract.ACTION_PIN)) return true;
        return miniTracked();
    }

    boolean edgePinTrackedOrPendingLaunch() {
        if (queuePendingLaunchAction(ConfigContract.ACTION_EDGE_PIN)) return true;
        return edgePinTracked();
    }

    private boolean queuePendingLaunchAction(int action) {
        synchronized (taskLock) {
            if (pendingPackage == null
                    || SystemClock.elapsedRealtime() - pendingSince > MATCH_WINDOW_MS) {
                return false;
            }
            pendingLaunchDismissRequested = false;
            pendingLaunchDeferredAction = action;
            Log.i("Startup outside action queued action=" + action
                    + " package=" + pendingPackage);
            return true;
        }
    }

    private void performDeferredLaunchAction(int action) {
        boolean handled;
        if (action == ConfigContract.ACTION_PIN) {
            handled = miniTracked();
        } else if (action == ConfigContract.ACTION_FULLSCREEN) {
            handled = fullscreenTracked();
        } else if (action == ConfigContract.ACTION_EDGE_PIN) {
            handled = edgePinTracked();
        } else {
            return;
        }
        Log.i("Startup deferred outside action=" + action + " handled=" + handled);
    }

    boolean miniTracked() {
        int taskId = interactionTaskId();
        Object info = taskInfo(taskId);
        if (taskId < 0 || controller == null || info == null
                || !booleanCall(info, "isNormalState", false)
                || booleanCall(info, "isMiniState", false) || isPinned(info)) return false;
        return miniTask(taskId, info);
    }

    boolean edgePinTracked() {
        int taskId = interactionTaskId();
        Object info = taskInfo(taskId);
        if (taskId < 0 || controller == null || info == null
                || !booleanCall(info, "isNormalState", false)
                || booleanCall(info, "isMiniState", false) || isPinned(info)) return false;
        try {
            Object executor = XposedHelpers.getObjectField(controller, "mMainExecutor");
            if (!(executor instanceof Executor)) {
                throw new IllegalStateException("HyperOS Shell main executor is unavailable");
            }
            synchronized (taskLock) {
                edgePinnedTaskIds.add(taskId);
            }
            ((Executor) executor).execute(() -> edgePinOnShellThread(taskId));
            Log.i("Native freeform edge-pin scheduled task=" + taskId);
            return true;
        } catch (Throwable error) {
            synchronized (taskLock) {
                edgePinnedTaskIds.remove(taskId);
            }
            Log.e("Cannot schedule native freeform edge-pin task=" + taskId, error);
            return false;
        }
    }

    private void edgePinOnShellThread(int taskId) {
        try {
            Object info = taskInfo(taskId);
            if (info == null || !booleanCall(info, "isNormalState", false)
                    || booleanCall(info, "isMiniState", false) || isPinned(info)) {
                synchronized (taskLock) {
                    edgePinnedTaskIds.remove(taskId);
                }
                return;
            }
            Rect bounds = null;
            Object value = XposedHelpers.callMethod(info, "getScaledBounds");
            if (value instanceof Rect && !((Rect) value).isEmpty()) {
                bounds = new Rect((Rect) value);
            }
            Rect display = context.getSystemService(WindowManager.class)
                    .getCurrentWindowMetrics().getBounds();
            float centerX = bounds == null ? display.exactCenterX() : bounds.exactCenterX();
            float centerY = bounds == null ? display.exactCenterY() : bounds.exactCenterY();
            boolean right = centerX >= display.exactCenterX();
            float edgeX = right ? display.right : display.left;
            float outwardVelocity = (right ? 1f : -1f) * Math.max(1800f,
                    display.width() * 1.8f);
            XposedHelpers.callMethod(controller, "startPinAnimation", info,
                    edgeX, centerY, outwardVelocity, 0f);
            Log.i("Native freeform edge-pin started task=" + taskId
                    + " side=" + (right ? "right" : "left"));
        } catch (Throwable error) {
            synchronized (taskLock) {
                edgePinnedTaskIds.remove(taskId);
            }
            Log.e("Cannot start native freeform edge-pin task=" + taskId, error);
        }
    }

    private boolean miniTask(int taskId, Object info) {
        try {
            Object executor = XposedHelpers.getObjectField(controller, "mMainExecutor");
            if (!(executor instanceof Executor)) {
                throw new IllegalStateException("HyperOS Shell main executor is unavailable");
            }
            synchronized (taskLock) {
                if (!tasks.contains(taskId)) {
                    tasks.put(taskId, packageName(info), FreeformTaskRegistry.State.NORMAL);
                }
                normalFreeformTaskIds.remove(taskId);
                if (trackedTaskId == taskId) trackedTaskId = -1;
                if (externalTrackedTaskId == taskId) externalTrackedTaskId = -1;
            }
            ((Executor) executor).execute(() -> minimizeOnShellThread(taskId));
            Log.i("Native current-freeform-to-mini scheduled task=" + taskId);
            return true;
        } catch (Throwable error) {
            Log.e("Cannot schedule native current-freeform-to-mini task=" + taskId, error);
            onTaskInfo(info);
            return false;
        }
    }

    private void runQueuedLaunchAfterSuspend(int taskId, long generation, String reason) {
        RuntimeTarget target;
        GestureConfig config;
        Runnable callback;
        synchronized (taskLock) {
            if (generation != queuedLaunchGeneration
                    || taskId != queuedAfterSuspendTaskId
                    || queuedLaunchTarget == null) return;
            target = queuedLaunchTarget;
            config = queuedLaunchConfig;
            callback = queuedLaunchArmedCallback;
            queuedLaunchTarget = null;
            queuedLaunchConfig = null;
            queuedLaunchArmedCallback = null;
            queuedAfterSuspendTaskId = -1;
        }
        Log.i("Starting queued second freeform reason=" + reason
                + " target=" + target.packageName);
        launchNow(target, config, callback);
    }

    private void clearQueuedLaunch(long generation) {
        synchronized (taskLock) {
            if (generation != queuedLaunchGeneration) return;
            queuedLaunchTarget = null;
            queuedLaunchConfig = null;
            queuedLaunchArmedCallback = null;
            queuedAfterSuspendTaskId = -1;
        }
    }

    void adjustMiniTargetIfNeeded(int animationType, Object info, Object target) {
        int taskId = info == null ? -1 : intCall(info, "getTaskId", -1);
        boolean owned;
        synchronized (taskLock) {
            owned = tasks.contains(taskId);
        }
        if (animationType == 4 && owned && target != null) {
            try {
                float scale = numberCall(info, "getMiniRestoreScaleX",
                        numberCall(info, "getFreeformScale", 0.7f));
                if (scale < 0.35f || scale > 1.2f) scale = 0.7f;
                Rect configuredBounds = customLaunchBounds(latestConfig, scale);
                XposedHelpers.callMethod(target, "setAnimParam",
                        configuredBounds, scale, scale, 0f);
                Log.i("Redirected mini restore to configured bounds task=" + taskId
                        + " bounds=" + configuredBounds);
            } catch (Throwable error) {
                Log.e("Cannot redirect mini restore task=" + taskId, error);
            }
            return;
        }
        Integer forcedTaskId = FORCE_RIGHT_MINI_TASK.get();
        if (animationType != 11 || forcedTaskId == null || target == null
                || taskId != forcedTaskId) return;
        try {
            float centerX = numberCall(target, "getCenterX", 0f);
            int displayWidth = context.getSystemService(WindowManager.class)
                    .getCurrentWindowMetrics().getBounds().width();
            if (displayWidth <= 0 || centerX <= 0 || centerX >= displayWidth / 2f) return;
            XposedHelpers.callMethod(target, "setCenterX", displayWidth - centerX);
            XposedHelpers.callMethod(info, "setPreRightMini", true);
            Log.i("Mirrored native mini target to right task=" + taskId);
        } catch (Throwable error) {
            Log.e("Cannot force native mini target to right task=" + taskId, error);
        }
    }

    void adjustEdgePinRestoreTarget(Object info, Object target, Object transaction) {
        int taskId = info == null ? -1 : intCall(info, "getTaskId", -1);
        synchronized (taskLock) {
            if (taskId < 0 || !edgePinnedTaskIds.contains(taskId)) return;
        }
        try {
            float scale = numberCall(info, "getFreeformScale", 0.7f);
            if (scale < 0.35f || scale > 1.2f) scale = 0.7f;
            Rect configuredBounds = customLaunchBounds(latestConfig, scale);
            Object resolvedTarget = target;
            if (resolvedTarget == null) {
                resolvedTarget = XposedHelpers.callMethod(info, "getAnimInfo");
            }
            if (resolvedTarget != null) {
                XposedHelpers.callMethod(resolvedTarget, "setAnimParam",
                        new Rect(configuredBounds), scale, scale, 0f);
            }
            if (transaction != null) {
                Object running = XposedHelpers.callMethod(info, "getTaskInfo");
                Object token = XposedHelpers.getObjectField(running, "token");
                XposedHelpers.callMethod(transaction, "setBounds", token,
                        new Rect(configuredBounds));
            }
            Log.i("Redirected edge-pin restore to configured bounds task=" + taskId
                    + " bounds=" + configuredBounds + " scale=" + scale
                    + " transaction=" + (transaction != null));
        } catch (Throwable error) {
            Log.e("Cannot redirect edge-pin restore task=" + taskId, error);
        }
    }

    boolean stabilizeLandscapeWindowShape(Object info) {
        int taskId = info == null ? -1 : intCall(info, "getTaskId", -1);
        if (!shouldHoldLandscapeShape(taskId)) return false;
        try {
            Rect bounds = (Rect) XposedHelpers.callMethod(info, "getBounds");
            if (bounds == null || bounds.isEmpty()) return false;
            boolean landscapeShape = bounds.width() > bounds.height();
            XposedHelpers.callMethod(info, "setIsLandscapeFreeform", landscapeShape);
            Log.i("Suppressed automatic freeform orientation reshape task=" + taskId
                    + " bounds=" + bounds + " landscapeShape=" + landscapeShape);
            return true;
        } catch (Throwable error) {
            Log.e("Cannot stabilize landscape freeform shape task=" + taskId, error);
            return false;
        }
    }

    boolean suppressLandscapeShapeChange(int taskId, int orientation) {
        if (!shouldHoldLandscapeShape(taskId) || isFlexibleOrientation(orientation)) {
            return false;
        }
        Log.i("Suppressed app-requested freeform reshape in landscape task=" + taskId
                + " orientation=" + orientation);
        return true;
    }

    private boolean shouldHoldLandscapeShape(int taskId) {
        synchronized (taskLock) {
            if (taskId < 0 || !tasks.contains(taskId)) return false;
        }
        Rect display = context.getSystemService(WindowManager.class)
                .getCurrentWindowMetrics().getBounds();
        if (display.width() <= display.height()) return false;
        Object info = taskInfo(taskId);
        return info != null && booleanCall(info, "isNormalState", false)
                && !booleanCall(info, "isMiniState", false) && !isPinned(info);
    }

    private static boolean isFlexibleOrientation(int orientation) {
        return orientation == -2 || orientation == -1 || orientation == 2
                || orientation == 3 || orientation == 4 || orientation == 10
                || orientation == 13 || orientation == 14;
    }

    private void minimizeOnShellThread(int taskId) {
        try {
            FORCE_RIGHT_MINI_TASK.set(taskId);
            XposedHelpers.callMethod(controller, "fromFreeformToMini", taskId);
            Log.i("Native current-freeform-to-mini started task=" + taskId);
        } catch (Throwable error) {
            Object info = taskInfo(taskId);
            if (info != null) onTaskInfo(info);
            Log.e("Cannot minimize task=" + taskId, error);
        } finally {
            FORCE_RIGHT_MINI_TASK.remove();
        }
    }

    boolean hideInputMethodIfNeeded() {
        try {
            Object executor = XposedHelpers.getObjectField(controller, "mMainExecutor");
            if (!(executor instanceof Executor)) {
                throw new IllegalStateException("HyperOS Shell main executor is unavailable");
            }
            ((Executor) executor).execute(this::hideInputMethodOnShellThread);
            return true;
        } catch (Throwable error) {
            Log.e("Cannot schedule current input-method hide", error);
            return false;
        }
    }

    ImeState inputMethodState() {
        Object owner = controller;
        if (owner == null) return new ImeState(false, false, 0);
        try {
            Object displayInfo = XposedHelpers.getObjectField(owner,
                    "mMiuiFreeformModeDisplayInfo");
            boolean visible = (Boolean) XposedHelpers.callMethod(displayInfo,
                    "isImeShowing");
            int height = ((Number) XposedHelpers.callMethod(displayInfo,
                    "getImeHeight")).intValue();
            return new ImeState(true, visible, height);
        } catch (Throwable error) {
            return new ImeState(false, false, 0);
        }
    }

    private void hideInputMethodOnShellThread() {
        try {
            Class<?> utils = XposedHelpers.findClass(
                    "com.android.wm.shell.multitasking.miuifreeform.MiuiFreeformModeUtils",
                    classLoader);
            XposedHelpers.callStaticMethod(utils, "hideInputMethodIfNeed");
            Log.i("HyperOS current input method hide requested");
        } catch (Throwable error) {
            Log.e("Cannot hide current input method through HyperOS", error);
        }
    }

    boolean fullscreenTracked() {
        int taskId = interactionTaskId();
        if (taskId < 0 || controller == null) return false;
        try {
            synchronized (taskLock) {
                clearInteractiveTaskLocked(taskId, "fullscreen requested");
            }
            XposedHelpers.callMethod(controller, "fullscreenFreeformWithoutAnim", taskId, true);
            Log.i("Fullscreen fan-launched task=" + taskId);
            return true;
        } catch (Throwable error) {
            Log.e("Cannot fullscreen tracked task=" + taskId, error);
            return false;
        }
    }

    int trackedTaskId() {
        return interactionTaskId();
    }

    private boolean scheduleDismiss(int taskId) {
        try {
            Object executor = XposedHelpers.getObjectField(controller, "mMainExecutor");
            if (!(executor instanceof Executor)) {
                throw new IllegalStateException("HyperOS Shell main executor is unavailable");
            }
            ((Executor) executor).execute(() -> dismissOnShellThread(taskId));
            Log.i("Native caption-close scheduled on Shell thread task=" + taskId);
            return true;
        } catch (Throwable error) {
            Log.e("Cannot schedule native caption-close task=" + taskId, error);
            return dismissImmediately(taskId);
        }
    }

    private void dismissOnShellThread(int taskId) {
        Object info = taskInfo(taskId);
        if (info != null) {
            try {
                Object running = XposedHelpers.callMethod(info, "getTaskInfo");
                if (!(running instanceof ActivityManager.RunningTaskInfo)) {
                    throw new IllegalStateException("Tracked RunningTaskInfo is unavailable");
                }
                Object starter = XposedHelpers.getObjectField(controller,
                        "mMulWinSwitchAnimStarter");
                XposedHelpers.callMethod(starter, "closeFullOrFreeform", running);
                Log.i("Native caption-close transition started on Shell thread task=" + taskId);
                return;
            } catch (Throwable error) {
                Log.e("Native caption-close unavailable; falling back to immediate exit task="
                        + taskId, error);
            }
        }
        dismissImmediately(taskId);
    }

    private boolean dismissImmediately(int taskId) {
        try {
            XposedHelpers.callMethod(controller, "exitFreeformTask", taskId, true);
            synchronized (taskLock) {
                removeTaskLocked(taskId, "immediate close requested");
            }
            Log.i("Immediately dismissed fan-launched task=" + taskId);
            return true;
        } catch (Throwable error) {
            Log.e("Cannot dismiss tracked task=" + taskId, error);
            return false;
        }
    }

    private void retainOwnershipDuringModeTransition(int taskId) {
        long deadline;
        synchronized (taskLock) {
            FreeformTaskRegistry.Record record = tasks.get(taskId);
            if (record == null) return;
            long now = SystemClock.elapsedRealtime();
            if (record.graceUntil > 0 && now >= record.graceUntil) {
                removeTaskLocked(taskId, "unsupported mode after transition grace");
                return;
            }
            if (record.graceUntil > now) return;
            deadline = now + 2000;
            record.graceUntil = deadline;
            if (trackedTaskId == taskId) trackedTaskId = -1;
        }
        mainHandler.postDelayed(() -> {
            synchronized (taskLock) {
                FreeformTaskRegistry.Record record = tasks.get(taskId);
                if (record == null || record.graceUntil != deadline) return;
            }
            Object current = taskInfo(taskId);
            if (current == null) {
                synchronized (taskLock) {
                    removeTaskLocked(taskId, "task missing after transition grace");
                }
            } else {
                onTaskInfo(current);
            }
        }, 2050);
    }

    private void removeTaskLocked(int taskId, String reason) {
        FreeformTaskRegistry.Record removed = tasks.remove(taskId);
        if (trackedTaskId == taskId) trackedTaskId = -1;
        if (launchInputProtectionTaskId == taskId) {
            launchInputProtectionTaskId = -1;
            pendingLaunchVisualBounds = null;
            launchInputProtectionUntil = 0L;
        }
        if (removed != null) {
            Log.i("Fan task ownership cleared task=" + taskId + " reason=" + reason
                    + " tracked=" + trackedTaskId + " ownedCount=" + tasks.size());
        }
    }

    private int interactionTaskId() {
        synchronized (taskLock) {
            int active = -1;
            for (int taskId : normalFreeformTaskIds) active = taskId;
            if (active >= 0) return active;
            int external = externalTrackedTaskId;
            return external >= 0 ? external : trackedTaskId;
        }
    }

    private void clearInteractiveTaskLocked(int taskId, String reason) {
        normalFreeformTaskIds.remove(taskId);
        if (tasks.contains(taskId)) {
            removeTaskLocked(taskId, reason);
            return;
        }
        if (externalTrackedTaskId == taskId) {
            externalTrackedTaskId = -1;
            Log.i("Native external freeform cleared task=" + taskId + " reason=" + reason);
        }
    }

    private void updateVisibleStateLocked(int taskId, boolean normal,
                                          boolean mini, boolean pinned) {
        if (normal || mini || pinned) {
            visibleFreeformTaskIds.remove(taskId);
            visibleFreeformTaskIds.add(taskId);
        } else {
            visibleFreeformTaskIds.remove(taskId);
        }
        if (normal && !mini && !pinned) {
            normalFreeformTaskIds.remove(taskId);
            normalFreeformTaskIds.add(taskId);
        } else {
            normalFreeformTaskIds.remove(taskId);
        }
    }

    private void beginPendingMatch(String packageName, int userId, Rect visualBounds,
                                   Rect launchBounds, Intent intent, Bundle options,
                                   boolean shortcutLaunch) {
        synchronized (taskLock) {
            pendingPackage = packageName;
            pendingUserId = userId;
            pendingSince = SystemClock.elapsedRealtime();
            pendingLaunchVisualBounds = visualBounds == null ? null : new Rect(visualBounds);
            launchInputProtectionUntil = pendingSince + LAUNCH_INPUT_PROTECTION_MS;
            launchInputProtectionTaskId = -1;
            pendingLaunchDismissRequested = false;
            pendingLaunchDeferredAction = ConfigContract.ACTION_NONE;
            pendingLaunchIntent = new Intent(intent);
            pendingLaunchOptions = new Bundle(options);
            pendingResizeRetry = false;
            pendingShortcutLaunch = shortcutLaunch;
            WindowMetrics metrics = context.getSystemService(WindowManager.class)
                    .getCurrentWindowMetrics();
            Rect display = metrics.getBounds();
            if (display.width() > display.height()) {
                recentLandscapeLaunchPackage = packageName;
                recentLandscapeLaunchUserId = userId;
                recentLandscapeLaunchUntil = pendingSince + 2200L;
                recentLandscapeLaunchBounds = launchBounds == null
                        ? null : new Rect(launchBounds);
            } else {
                recentLandscapeLaunchPackage = null;
                recentLandscapeLaunchUserId = 0;
                recentLandscapeLaunchUntil = 0L;
                recentLandscapeLaunchBounds = null;
            }
            if (shortcutLaunch) {
                recentShortcutPackage = packageName;
                recentShortcutUserId = userId;
                recentShortcutUntil = pendingSince + 2200L;
                recentShortcutLaunchBounds = launchBounds == null
                        ? null : new Rect(launchBounds);
            }
        }
    }

    private void notifyLaunchArmed(Runnable onLaunchArmed) {
        if (onLaunchArmed == null) return;
        try {
            onLaunchArmed.run();
        } catch (Throwable error) {
            Log.e("Cannot arm startup outside input capture", error);
        }
    }

    private void clearPendingMatch() {
        synchronized (taskLock) {
            clearPendingMatchLocked();
        }
    }

    private void clearPendingMatchLocked() {
        finishPendingMatchLocked();
        pendingLaunchVisualBounds = null;
        launchInputProtectionUntil = 0L;
        launchInputProtectionTaskId = -1;
    }

    private void finishPendingMatchLocked() {
        pendingPackage = null;
        pendingUserId = 0;
        pendingSince = 0L;
        pendingLaunchDismissRequested = false;
        pendingLaunchDeferredAction = ConfigContract.ACTION_NONE;
        pendingLaunchIntent = null;
        pendingLaunchOptions = null;
        pendingResizeRetry = false;
        pendingShortcutLaunch = false;
    }

    private void scheduleScans() {
        mainHandler.postDelayed(this::scanPendingTasks, 40);
        mainHandler.postDelayed(this::scanPendingTasks, 120);
        mainHandler.postDelayed(this::scanPendingTasks, 250);
        mainHandler.postDelayed(this::scanPendingTasks, 900);
        mainHandler.postDelayed(this::scanPendingTasks, 2200);
        mainHandler.postDelayed(() -> {
            if (pendingPackage != null
                    && SystemClock.elapsedRealtime() - pendingSince >= MATCH_WINDOW_MS) {
                Log.i("No matching freeform task appeared for " + pendingPackage);
                clearPendingMatch();
            }
        }, MATCH_WINDOW_MS + 100);
    }

    @SuppressWarnings("unchecked")
    private void scanPendingTasks() {
        if (pendingPackage == null || controller == null) return;
        try {
            Object repository = XposedHelpers.getObjectField(controller,
                    "mMultiTaskingTaskRepository");
            Object value = XposedHelpers.callMethod(repository, "getFreeformTasksInZOrder");
            if (!(value instanceof List)) return;
            for (Object item : (List<Object>) value) {
                if (item instanceof Number) {
                    Object info = taskInfo(((Number) item).intValue());
                    if (info != null) onTaskInfo(info);
                    if (pendingPackage == null) return;
                }
            }
            retryNonResizableTaskIfNeeded();
        } catch (Throwable error) {
            Log.e("Pending task scan failed", error);
        }
    }

    private void retryNonResizableTaskIfNeeded() {
        String expected = pendingPackage;
        if (expected == null || pendingResizeRetry || pendingShortcutLaunch) return;
        int expectedUserId = pendingUserId;
        ActivityManager.RunningTaskInfo running = findRunningTask(expected, expectedUserId);
        if (running == null || intCall(running, "getWindowingMode", 1) == 5) return;
        if (expectedUserId != 0) return;
        Intent intent = pendingLaunchIntent;
        Bundle options = pendingLaunchOptions;
        if (intent == null || options == null || !forceTaskResizable(running.taskId)) return;
        pendingResizeRetry = true;
        try {
            context.startActivity(new Intent(intent), new Bundle(options));
            Log.i("Retried non-resizable task as freeform task=" + running.taskId
                    + " package=" + expected);
        } catch (Throwable error) {
            Log.e("Cannot retry non-resizable task=" + running.taskId, error);
        }
    }

    private void alignShortcutTaskToConfiguredBounds(int taskId) {
        long now = SystemClock.elapsedRealtime();
        if (lastShortcutAlignedTaskId == taskId && now - lastShortcutAlignmentAt < 700L) {
            return;
        }
        lastShortcutAlignedTaskId = taskId;
        lastShortcutAlignmentAt = now;
        resizeShortcutTask(taskId);
        mainHandler.postDelayed(() -> resizeShortcutTask(taskId), 120L);
        mainHandler.postDelayed(() -> resizeShortcutTask(taskId), 320L);
    }

    private void resizeShortcutTask(int taskId) {
        Object info = taskInfo(taskId);
        if (info == null || !booleanCall(info, "isNormalState", false)) return;
        try {
            float transientScale = numberCall(info, "getFreeformScale", 1f);
            Rect savedBounds = recentShortcutLaunchBounds;
            // A reused shortcut task reports scale=1 during its transition even though the
            // ActivityOptions were created with HyperOS' actual freeform scale (normally 0.7).
            // Reuse the exact pre-launch logical bounds instead of recalculating from that
            // transient task value. This is identical to the bounds used by normal app launches.
            Rect configuredBounds = savedBounds == null
                    ? customLaunchBounds(latestConfig, 0.7f) : new Rect(savedBounds);
            Object running = XposedHelpers.callMethod(info, "getTaskInfo");
            if (running instanceof ActivityManager.RunningTaskInfo
                    && taskMatchesBounds((ActivityManager.RunningTaskInfo) running,
                    configuredBounds)) {
                Log.i("Shortcut freeform bounds verified task=" + taskId
                        + " bounds=" + taskBounds((ActivityManager.RunningTaskInfo) running));
                return;
            }
            Class<?> activityTaskManager = XposedHelpers.findClass(
                    "android.app.ActivityTaskManager", null);
            Object service = XposedHelpers.callStaticMethod(activityTaskManager, "getService");
            XposedHelpers.callMethod(service, "resizeTask", taskId, configuredBounds, 0);
            Log.i("Shortcut freeform aligned to configured bounds task=" + taskId
                    + " bounds=" + configuredBounds
                    + " transientScale=" + transientScale);
        } catch (Throwable error) {
            Log.e("Cannot align shortcut freeform task=" + taskId, error);
        }
    }

    @SuppressWarnings("deprecation")
    private ActivityManager.RunningTaskInfo findRunningTask(String packageName, int userId) {
        try {
            ActivityManager manager = context.getSystemService(ActivityManager.class);
            if (manager == null) return null;
            for (ActivityManager.RunningTaskInfo running : manager.getRunningTasks(100)) {
                if (packageName.equals(packageName(running))
                        && taskUserId(running) == userId) return running;
            }
        } catch (Throwable error) {
            Log.e("Cannot inspect running tasks for " + packageName, error);
        }
        return null;
    }

    private static boolean taskMatchesBounds(ActivityManager.RunningTaskInfo task,
                                             Rect expected) {
        Rect actual = taskBounds(task);
        return actual != null && expected != null
                && Math.abs(actual.left - expected.left) <= 2
                && Math.abs(actual.top - expected.top) <= 2
                && Math.abs(actual.right - expected.right) <= 2
                && Math.abs(actual.bottom - expected.bottom) <= 2;
    }

    private static Rect taskBounds(ActivityManager.RunningTaskInfo task) {
        try {
            Object configuration = XposedHelpers.getObjectField(task, "configuration");
            Object windowConfiguration = XposedHelpers.getObjectField(
                    configuration, "windowConfiguration");
            Object bounds = XposedHelpers.callMethod(windowConfiguration, "getBounds");
            return bounds instanceof Rect ? new Rect((Rect) bounds) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean forceTaskResizable(int taskId) {
        try {
            Class<?> activityTaskManager = XposedHelpers.findClass(
                    "android.app.ActivityTaskManager", null);
            Object service = XposedHelpers.callStaticMethod(activityTaskManager, "getService");
            XposedHelpers.callMethod(service, "setTaskResizeable", taskId, 2);
            Log.i("Task marked resizable for freeform task=" + taskId);
            return true;
        } catch (Throwable error) {
            Log.e("Cannot mark task resizable task=" + taskId, error);
            return false;
        }
    }

    private Object taskInfo(int taskId) {
        Object owner = controller;
        if (owner == null || taskId < 0) return null;
        try {
            Object repository = XposedHelpers.getObjectField(owner,
                    "mMultiTaskingTaskRepository");
            return XposedHelpers.callMethod(repository, "getMiuiFreeformTaskInfo", taskId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isPinned(Object info) {
        return booleanCall(info, "isInPinMode", false)
                || booleanCall(info, "isNormalPinedState", false)
                || booleanCall(info, "isMiniPinedState", false);
    }

    private String packageName(Object info) {
        try {
            Object task = XposedHelpers.callMethod(info, "getTaskInfo");
            if (task instanceof ActivityManager.RunningTaskInfo) {
                return packageName((ActivityManager.RunningTaskInfo) task);
            }
            for (String field : new String[]{"topActivity", "realActivity", "baseActivity"}) {
                try {
                    Object component = XposedHelpers.getObjectField(task, field);
                    if (component instanceof ComponentName) {
                        return ((ComponentName) component).getPackageName();
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return "";
    }

    private int userId(Object info) {
        try {
            Object task = XposedHelpers.callMethod(info, "getTaskInfo");
            if (task instanceof ActivityManager.RunningTaskInfo) {
                return taskUserId(task);
            }
            Object value = XposedHelpers.getObjectField(task, "userId");
            return value instanceof Number ? ((Number) value).intValue() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int taskUserId(Object task) {
        try {
            return XposedHelpers.getIntField(task, "userId");
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static String packageName(ActivityManager.RunningTaskInfo running) {
        ComponentName component = running.topActivity != null ? running.topActivity
                : componentField(running, "realActivity");
        if (component == null) component = running.baseActivity;
        return component == null ? "" : component.getPackageName();
    }

    private static ComponentName componentField(Object target, String field) {
        try {
            Object value = XposedHelpers.getObjectField(target, field);
            return value instanceof ComponentName ? (ComponentName) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private float readFreeformScale(ActivityOptions options) {
        try {
            Object injector = XposedHelpers.callMethod(options, "getActivityOptionsInjector");
            float scale;
            try {
                scale = XposedHelpers.getFloatField(injector, "mFreeformScale");
            } catch (Throwable ignored) {
                Object value = XposedHelpers.callMethod(injector, "getFreeformScale");
                scale = value instanceof Number ? ((Number) value).floatValue() : 0.7f;
            }
            if (scale >= 0.35f && scale <= 1.2f) return scale;
        } catch (Throwable ignored) {}
        return 0.7f;
    }

    private Rect customLaunchBounds(GestureConfig config, float scale) {
        WindowManager windowManager = context.getSystemService(WindowManager.class);
        WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
        Rect display = new Rect(metrics.getBounds());
        Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        int edge = Math.round(12 * context.getResources().getDisplayMetrics().density);
        Rect safe = new Rect(display.left + insets.left + edge,
                display.top + insets.top + edge,
                display.right - insets.right - edge,
                display.bottom - insets.bottom - edge);
        if (safe.width() <= 0 || safe.height() <= 0) safe.set(display);
        boolean landscape = display.width() > display.height();

        // In landscape the system-bar insets take a much larger share of the short edge.
        // Using the inset-safe rectangle as the percentage reference made the configured
        // window look wider and shorter than the settings preview. Keep the safe rectangle
        // for placement, but interpret landscape width/height as percentages of the physical
        // display, exactly as the preview presents them. Portrait behavior stays unchanged.
        Rect sizeReference = landscape ? display : safe;
        Rect placement = landscape
                ? new Rect(display.left + edge, display.top + edge,
                display.right - edge, display.bottom - edge)
                : safe;
        int visualWidth = Math.max(1,
                Math.round(sizeReference.width()
                        * config.windowWidthPercent(landscape) / 100f));
        int visualHeight = Math.max(1,
                Math.round(sizeReference.height()
                        * config.windowHeightPercent(landscape) / 100f));
        int visualLeft = placement.left
                + Math.round((placement.width() - visualWidth)
                * config.windowPositionX(landscape) / 100f);
        int visualTop = placement.top
                + Math.round((placement.height() - visualHeight)
                * config.windowPositionY(landscape) / 100f);
        int logicalWidth = Math.round(visualWidth / scale);
        int logicalHeight = Math.round(visualHeight / scale);
        return new Rect(visualLeft, visualTop,
                visualLeft + logicalWidth, visualTop + logicalHeight);
    }

    private static Rect visualBounds(Rect logicalBounds, float scale) {
        return new Rect(logicalBounds.left, logicalBounds.top,
                logicalBounds.left + Math.round(logicalBounds.width() * scale),
                logicalBounds.top + Math.round(logicalBounds.height() * scale));
    }

    private static boolean booleanCall(Object target, String method, boolean fallback) {
        try {
            Object value = XposedHelpers.callMethod(target, method);
            return value instanceof Boolean ? (Boolean) value : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int intCall(Object target, String method, int fallback) {
        try {
            Object value = XposedHelpers.callMethod(target, method);
            return value instanceof Number ? ((Number) value).intValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static float numberCall(Object target, String method, float fallback) {
        try {
            Object value = XposedHelpers.callMethod(target, method);
            return value instanceof Number ? ((Number) value).floatValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
