package com.oxohang.fanfreeform.xposed;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;

import java.util.List;

import de.robv.android.xposed.XposedHelpers;

final class SystemRecentsLauncher {
    private final Context context;
    private final ActivityManager activityManager;

    SystemRecentsLauncher(Context context) {
        this.context = context;
        activityManager = context.getSystemService(ActivityManager.class);
    }

    boolean show() {
        if (toggleThroughCommandQueue()) return true;
        return toggleThroughStatusBarService();
    }

    boolean isVisible() {
        if (activityManager == null) return false;
        try {
            List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
            if (tasks == null || tasks.isEmpty()) return false;
            ComponentName top = tasks.get(0).topActivity;
            if (top == null) return false;
            String className = top.getClassName().toLowerCase(java.util.Locale.ROOT);
            return "com.miui.home".equals(top.getPackageName())
                    && className.contains("recents");
        } catch (Throwable error) {
            Log.e("Cannot inspect HyperOS system recents visibility", error);
            return false;
        }
    }

    private boolean toggleThroughCommandQueue() {
        try {
            ClassLoader loader = context.getClassLoader();
            Class<?> dependencyClass = XposedHelpers.findClass(
                    "com.android.systemui.Dependency", loader);
            Class<?> commandQueueClass = XposedHelpers.findClass(
                    "com.android.systemui.statusbar.CommandQueue", loader);
            Object dependency = XposedHelpers.getStaticObjectField(
                    dependencyClass, "sDependency");
            if (dependency == null) return false;
            Object commandQueue = XposedHelpers.callMethod(
                    dependency, "getDependencyInner", commandQueueClass);
            if (commandQueue == null) return false;
            XposedHelpers.callMethod(commandQueue, "toggleRecentApps");
            Log.i("HyperOS system recents requested through CommandQueue");
            return true;
        } catch (Throwable error) {
            Log.e("CommandQueue recents entry unavailable", error);
            return false;
        }
    }

    private boolean toggleThroughStatusBarService() {
        try {
            Class<?> serviceManager = XposedHelpers.findClass(
                    "android.os.ServiceManager", null);
            Object binder = XposedHelpers.callStaticMethod(
                    serviceManager, "getService", "statusbar");
            if (binder == null) return false;
            Class<?> stub = XposedHelpers.findClass(
                    "com.android.internal.statusbar.IStatusBarService$Stub", null);
            Object service = XposedHelpers.callStaticMethod(stub, "asInterface", binder);
            if (service == null) return false;
            XposedHelpers.callMethod(service, "toggleRecentApps");
            Log.i("HyperOS system recents requested through status bar service");
            return true;
        } catch (Throwable error) {
            Log.e("Cannot open HyperOS system recents", error);
            return false;
        }
    }
}
