package com.oxohang.fanfreeform.xposed;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import java.util.List;
import java.util.concurrent.Executor;

import de.robv.android.xposed.XposedHelpers;

final class HyperOsFreeformBridge {
    private static final long MATCH_WINDOW_MS = 6000;
    private final Context context;
    private final ClassLoader classLoader;
    private final Handler mainHandler;
    private volatile Object controller;
    private volatile int trackedTaskId = -1;
    private volatile String pendingPackage;
    private volatile long pendingSince;

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

    boolean launch(RuntimeTarget target, GestureConfig config) {
        String packageName = target.component.getPackageName();
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

            Intent intent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(target.component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            beginPendingMatch(packageName);
            context.startActivity(intent, options.toBundle());
            Log.i("Launching or reusing " + target.component.flattenToShortString()
                    + " in freeform without home reorder bounds=" + launchBounds
                    + " scale=" + scale);
            scheduleScans();
            return true;
        } catch (Throwable error) {
            pendingPackage = null;
            Log.e("Native freeform launch failed", error);
            return false;
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
        boolean pinned = booleanCall(info, "isInPinMode", false)
                || booleanCall(info, "isNormalPinedState", false)
                || booleanCall(info, "isMiniPinedState", false);

        if (taskId == trackedTaskId && (!normal || mini || pinned)) {
            Log.i("Tracked task changed mode; auto-dismiss tracking stopped task=" + taskId);
            trackedTaskId = -1;
        }

        String expected = pendingPackage;
        if (expected == null || SystemClock.elapsedRealtime() - pendingSince > MATCH_WINDOW_MS) return;
        String actual = packageName(info);
        if (normal && !mini && !pinned && expected.equals(actual)) {
            trackedTaskId = taskId;
            pendingPackage = null;
            Log.i("Tracking fan-launched task=" + taskId + " package=" + actual);
        }
    }

    void onTaskVanished(int taskId) {
        if (taskId == trackedTaskId) {
            trackedTaskId = -1;
            Log.i("Tracked task vanished task=" + taskId);
        }
    }

    Rect trackedBounds() {
        int taskId = trackedTaskId;
        if (taskId < 0) return null;
        Object info = taskInfo(taskId);
        if (info == null || !booleanCall(info, "isNormalState", false)
                || booleanCall(info, "isMiniState", false)
                || booleanCall(info, "isInPinMode", false)
                || booleanCall(info, "isNormalPinedState", false)) {
            trackedTaskId = -1;
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

    boolean dismissTracked() {
        int taskId = trackedTaskId;
        if (taskId < 0 || controller == null) return false;
        try {
            Object executor = XposedHelpers.getObjectField(controller, "mMainExecutor");
            if (!(executor instanceof Executor)) {
                throw new IllegalStateException("HyperOS Shell main executor is unavailable");
            }
            trackedTaskId = -1;
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
            trackedTaskId = -1;
            Log.i("Immediately dismissed fan-launched task=" + taskId);
            return true;
        } catch (Throwable error) {
            Log.e("Cannot dismiss tracked task=" + taskId, error);
            return false;
        }
    }

    boolean fullscreenTracked() {
        int taskId = trackedTaskId;
        trackedTaskId = -1;
        if (taskId < 0 || controller == null) return false;
        try {
            XposedHelpers.callMethod(controller, "fullscreenFreeformWithoutAnim", taskId, true);
            Log.i("Fullscreen fan-launched task=" + taskId);
            return true;
        } catch (Throwable error) {
            Log.e("Cannot fullscreen tracked task=" + taskId, error);
            return false;
        }
    }

    boolean miniTracked() {
        int taskId = trackedTaskId;
        Object info = taskInfo(taskId);
        if (taskId < 0 || controller == null || info == null) return false;
        try {
            Object running = XposedHelpers.callMethod(info, "getTaskInfo");
            if (!(running instanceof ActivityManager.RunningTaskInfo)) {
                throw new IllegalStateException("Tracked RunningTaskInfo is unavailable");
            }
            Object result = XposedHelpers.callMethod(controller,
                    "lunchSmallFreeformFromRecent", running, 2);
            if (result == null) {
                throw new IllegalStateException("Right-top mini-freeform request was rejected");
            }
            trackedTaskId = -1;
            Log.i("Mini-freeform requested at right-top task=" + taskId);
            return true;
        } catch (Throwable error) {
            Log.e("Right-top mini-freeform unavailable; trying native mini fallback task="
                    + taskId, error);
            try {
                Object executor = XposedHelpers.getObjectField(controller, "mMainExecutor");
                if (!(executor instanceof Executor)) {
                    throw new IllegalStateException("HyperOS Shell main executor is unavailable");
                }
                trackedTaskId = -1;
                ((Executor) executor).execute(() -> {
                    try {
                        XposedHelpers.callMethod(controller, "fromFreeformToMini", taskId);
                        Log.i("Native mini-freeform fallback started task=" + taskId);
                    } catch (Throwable fallbackError) {
                        Log.e("Cannot minimize tracked task=" + taskId, fallbackError);
                    }
                });
                return true;
            } catch (Throwable fallbackError) {
                Log.e("Cannot schedule mini-freeform fallback task=" + taskId, fallbackError);
                return false;
            }
        }
    }

    int trackedTaskId() {
        return trackedTaskId;
    }

    private void beginPendingMatch(String packageName) {
        pendingPackage = packageName;
        pendingSince = SystemClock.elapsedRealtime();
    }

    private void scheduleScans() {
        mainHandler.postDelayed(this::scanPendingTasks, 250);
        mainHandler.postDelayed(this::scanPendingTasks, 900);
        mainHandler.postDelayed(this::scanPendingTasks, 2200);
        mainHandler.postDelayed(() -> {
            if (pendingPackage != null && SystemClock.elapsedRealtime() - pendingSince >= MATCH_WINDOW_MS) {
                Log.i("No matching freeform task appeared for " + pendingPackage);
                pendingPackage = null;
            }
        }, MATCH_WINDOW_MS + 100);
    }

    @SuppressWarnings("unchecked")
    private void scanPendingTasks() {
        if (pendingPackage == null || controller == null) return;
        try {
            Object repository = XposedHelpers.getObjectField(controller, "mMultiTaskingTaskRepository");
            Object value = XposedHelpers.callMethod(repository, "getFreeformTasksInZOrder");
            if (!(value instanceof List)) return;
            for (Object item : (List<Object>) value) {
                if (item instanceof Number) {
                    Object info = taskInfo(((Number) item).intValue());
                    if (info != null) onTaskInfo(info);
                    if (pendingPackage == null) return;
                }
            }
        } catch (Throwable error) {
            Log.e("Pending task scan failed", error);
        }
    }

    private Object taskInfo(int taskId) {
        Object owner = controller;
        if (owner == null) return null;
        try {
            Object repository = XposedHelpers.getObjectField(owner, "mMultiTaskingTaskRepository");
            return XposedHelpers.callMethod(repository, "getMiuiFreeformTaskInfo", taskId);
        } catch (Throwable ignored) {
            return null;
        }
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
                    if (component instanceof ComponentName) return ((ComponentName) component).getPackageName();
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return "";
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
        Rect safe = new Rect(display.left + insets.left + edge, display.top + insets.top + edge,
                display.right - insets.right - edge, display.bottom - insets.bottom - edge);
        if (safe.width() <= 0 || safe.height() <= 0) safe.set(display);

        int visualWidth = Math.max(1, Math.round(safe.width() * config.widthPercent / 100f));
        int visualHeight = Math.max(1, Math.round(safe.height() * config.heightPercent / 100f));
        int visualLeft = safe.left + Math.round((safe.width() - visualWidth) * config.positionX / 100f);
        int visualTop = safe.top + Math.round((safe.height() - visualHeight) * config.positionY / 100f);
        int logicalWidth = Math.round(visualWidth / scale);
        int logicalHeight = Math.round(visualHeight / scale);
        return new Rect(visualLeft, visualTop, visualLeft + logicalWidth, visualTop + logicalHeight);
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
}
