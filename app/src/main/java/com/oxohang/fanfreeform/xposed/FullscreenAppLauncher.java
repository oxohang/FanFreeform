package com.oxohang.fanfreeform.xposed;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import de.robv.android.xposed.XposedHelpers;

final class FullscreenAppLauncher {
    private final Context context;

    FullscreenAppLauncher(Context context) {
        this.context = context;
    }

    boolean launch(RuntimeTarget target) {
        return launch(target, false);
    }

    boolean launch(RuntimeTarget target, boolean centeredSystemAnimation) {
        if (target == null) return false;
        try {
            ActivityOptions options = centeredSystemAnimation
                    ? centeredScaleOptions(context) : ActivityOptions.makeBasic();
            try {
                XposedHelpers.callMethod(options, "setLaunchWindowingMode", 1);
            } catch (Throwable ignored) { }
            try {
                Object display = context.getDisplay();
                if (display != null) options.setLaunchDisplayId(context.getDisplay().getDisplayId());
            } catch (Throwable ignored) { }
            Bundle bundle = options.toBundle();
            if (target.isShortcut()) {
                Intent request = new Intent(target.isLauncherShortcut()
                        ? ShortcutHostRuntime.ACTION_START_LAUNCHER_SHORTCUT
                        : ShortcutHostRuntime.ACTION_START_SHORTCUT)
                        .setPackage(ShortcutHostRuntime.MIUI_HOME)
                        .putExtra(ShortcutHostRuntime.EXTRA_PACKAGE, target.packageName)
                        .putExtra(ShortcutHostRuntime.EXTRA_SHORTCUT_ID, target.shortcutId)
                        .putExtra(ShortcutHostRuntime.EXTRA_OPTIONS, bundle);
                if (target.isLauncherShortcut()) {
                    request.putExtra(ShortcutHostRuntime.EXTRA_INTENT_URI,
                            target.shortcutIntentUri);
                }
                context.sendBroadcast(request);
            } else if (target.component == null) {
                return false;
            } else if (target.userId != 0) {
                Intent request = new Intent(ShortcutHostRuntime.ACTION_START_ACTIVITY)
                        .setPackage(ShortcutHostRuntime.MIUI_HOME)
                        .putExtra(ShortcutHostRuntime.EXTRA_COMPONENT,
                                target.component.flattenToString())
                        .putExtra(ShortcutHostRuntime.EXTRA_USER_ID, target.userId)
                        .putExtra(ShortcutHostRuntime.EXTRA_OPTIONS, bundle);
                context.sendBroadcast(request);
            } else {
                Intent intent = Intent.makeMainActivity(target.component)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(intent, bundle);
            }
            Log.i("Launched fullscreen " + (target.isShortcut()
                    ? target.packageName + "/" + target.shortcutId
                    : target.component.flattenToShortString()
                    + (target.userId == 0 ? "" : " user=" + target.userId))
                    + " centeredAnimation=" + centeredSystemAnimation);
            return true;
        } catch (Throwable error) {
            Log.e("Cannot launch target fullscreen", error);
            Toast.makeText(context, "应用不可用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    static ActivityOptions centeredScaleOptions(Context context) {
        WindowManager windowManager = context.getSystemService(WindowManager.class);
        Rect display = windowManager == null
                ? new Rect(0, 0, context.getResources().getDisplayMetrics().widthPixels,
                context.getResources().getDisplayMetrics().heightPixels)
                : new Rect(windowManager.getCurrentWindowMetrics().getBounds());
        View source = new View(context);
        source.layout(0, 0, display.width(), display.height());
        int startSize = Math.max(1, Math.round(56f
                * context.getResources().getDisplayMetrics().density));
        int startX = display.centerX() - startSize / 2;
        int startY = display.centerY() - startSize / 2;
        ActivityOptions options = ActivityOptions.makeScaleUpAnimation(
                source, startX, startY, startSize, startSize);
        // HyperOS normally replaces an app-supplied scale animation when an existing
        // task is brought to the front. This framework flag is also used by MiuiHome
        // when it needs its launch animation to win over the task transition.
        try {
            XposedHelpers.callMethod(options, "setOverrideTaskTransition", true);
            Log.i("Using centered fullscreen task animation at "
                    + display.centerX() + "," + display.centerY());
        } catch (Throwable error) {
            // Older HyperOS builds may only expose the backing field.
            try {
                XposedHelpers.setBooleanField(options, "mOverrideTaskTransition", true);
                Log.i("Using centered fullscreen task animation (field fallback)");
            } catch (Throwable ignored) {
                Log.i("Cannot force centered task transition; using scale fallback");
            }
        }
        return options;
    }
}
