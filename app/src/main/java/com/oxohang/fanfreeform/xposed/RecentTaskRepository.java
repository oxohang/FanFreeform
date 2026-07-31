package com.oxohang.fanfreeform.xposed;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.drawable.Drawable;
import android.hardware.HardwareBuffer;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

final class RecentTaskRepository {
    private final Context context;
    private final ActivityManager activityManager;
    private final PackageManager packageManager;

    RecentTaskRepository(Context context) {
        this.context = context;
        activityManager = context.getSystemService(ActivityManager.class);
        packageManager = context.getPackageManager();
    }

    List<RecentTaskPreview> loadRunningTasks(int maximum, boolean includeSnapshots) {
        ArrayList<RecentTaskPreview> result = new ArrayList<>();
        if (activityManager == null) return result;
        int limit = Math.max(2, Math.min(10, maximum));
        try {
            List<ActivityManager.RecentTaskInfo> recents = activityManager.getRecentTasks(
                    30, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
            for (ActivityManager.RecentTaskInfo task : recents) {
                if (result.size() >= limit) break;
                if (task == null || task.taskId < 0 || !task.isRunning
                        || task.numActivities <= 0) continue;
                ComponentName component = task.topActivity != null
                        ? task.topActivity : task.baseActivity;
                if (component == null && task.baseIntent != null) {
                    component = task.baseIntent.getComponent();
                }
                if (component == null) continue;
                String packageName = component.getPackageName();
                if (packageName.equals(context.getPackageName())
                        || packageName.equals("com.android.systemui")
                        || packageName.equals("com.miui.home")) continue;
                ApplicationInfo app = packageManager.getApplicationInfo(packageName, 0);
                CharSequence label = packageManager.getApplicationLabel(app);
                Drawable icon = packageManager.getApplicationIcon(app);
                Bitmap snapshot = includeSnapshots ? loadSnapshot(task.taskId) : null;
                result.add(new RecentTaskPreview(task.taskId, packageName, label,
                        snapshot, icon));
            }
        } catch (Throwable error) {
            Log.e("Cannot load running task previews", error);
        }
        Log.i("Running task previews loaded count=" + result.size()
                + " snapshots=" + includeSnapshots);
        return result;
    }

    void release(List<RecentTaskPreview> previews) {
        if (previews == null) return;
        for (RecentTaskPreview preview : previews) {
            if (preview == null || preview.snapshot == null
                    || preview.snapshot.isRecycled()) continue;
            preview.snapshot.recycle();
        }
    }

    @SuppressLint("MissingPermission")
    boolean launch(RecentTaskPreview task) {
        if (task == null) return false;
        try {
            Object service = activityTaskManagerService();
            Bundle centeredOptions = FullscreenAppLauncher.centeredScaleOptions(context)
                    .toBundle();
            Object result = startFromRecents(service, task.taskId, centeredOptions);
            Log.i("Recent task launched with centered animation id="
                    + task.taskId + " result=" + result);
            return true;
        } catch (Throwable primary) {
            try {
                Object service = activityTaskManagerService();
                Object result = startFromRecents(service, task.taskId,
                        ActivityOptions.makeBasic().toBundle());
                Log.i("Recent task launched with basic animation id="
                        + task.taskId + " result=" + result);
                return true;
            } catch (Throwable basicFallback) {
                try {
                    activityManager.moveTaskToFront(task.taskId,
                            ActivityManager.MOVE_TASK_WITH_HOME);
                    Log.i("Recent task moved to front id=" + task.taskId);
                    return true;
                } catch (Throwable finalFallback) {
                    Log.e("Cannot launch recent task id=" + task.taskId, finalFallback);
                    return false;
                }
            }
        }
    }

    private static Object startFromRecents(Object service, int taskId, Bundle options) {
        return XposedHelpers.callMethod(service,
                "startActivityFromRecents", taskId, options);
    }

    private Bitmap loadSnapshot(int taskId) {
        HardwareBuffer buffer = null;
        try {
            Object service = activityTaskManagerService();
            Object snapshot = XposedHelpers.callMethod(service,
                    "getTaskSnapshot", taskId, false);
            if (snapshot == null) {
                snapshot = XposedHelpers.callMethod(service,
                        "takeTaskSnapshot", taskId, false);
            }
            if (snapshot == null) return null;
            buffer = (HardwareBuffer) XposedHelpers.callMethod(snapshot,
                    "getHardwareBuffer");
            ColorSpace colorSpace = (ColorSpace) XposedHelpers.callMethod(snapshot,
                    "getColorSpace");
            if (buffer == null || buffer.isClosed()) return null;
            Bitmap hardware = Bitmap.wrapHardwareBuffer(buffer, colorSpace);
            if (hardware == null) return null;
            hardware.setHasMipMap(true);
            return hardware;
        } catch (Throwable error) {
            Log.e("Cannot load task snapshot id=" + taskId, error);
            return null;
        } finally {
            if (buffer != null && !buffer.isClosed()) buffer.close();
        }
    }

    private static Object activityTaskManagerService() {
        Class<?> manager = XposedHelpers.findClass(
                "android.app.ActivityTaskManager", null);
        return XposedHelpers.callStaticMethod(manager, "getService");
    }
}
