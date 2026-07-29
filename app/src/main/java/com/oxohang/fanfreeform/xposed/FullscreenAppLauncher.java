package com.oxohang.fanfreeform.xposed;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import de.robv.android.xposed.XposedHelpers;

final class FullscreenAppLauncher {
    private final Context context;

    FullscreenAppLauncher(Context context) {
        this.context = context;
    }

    boolean launch(RuntimeTarget target) {
        if (target == null || target.component == null || target.isShortcut()) return false;
        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            try {
                XposedHelpers.callMethod(options, "setLaunchWindowingMode", 1);
            } catch (Throwable ignored) { }
            try {
                Object display = context.getDisplay();
                if (display != null) options.setLaunchDisplayId(context.getDisplay().getDisplayId());
            } catch (Throwable ignored) { }
            Bundle bundle = options.toBundle();
            if (target.userId != 0) {
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
            Log.i("Honeycomb launched fullscreen " + target.component.flattenToShortString()
                    + (target.userId == 0 ? "" : " user=" + target.userId));
            return true;
        } catch (Throwable error) {
            Log.e("Cannot launch honeycomb target fullscreen", error);
            Toast.makeText(context, "应用不可用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
