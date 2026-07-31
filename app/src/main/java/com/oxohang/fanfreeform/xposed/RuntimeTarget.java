package com.oxohang.fanfreeform.xposed;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;

final class RuntimeTarget {
    final ComponentName component;
    final String packageName;
    final String shortcutId;
    final String shortcutIntentUri;
    final String label;
    final Drawable icon;
    final int userId;

    RuntimeTarget(ComponentName component, String label, Drawable icon) {
        this(component, label, icon, 0);
    }

    RuntimeTarget(ComponentName component, String label, Drawable icon, int userId) {
        this.component = component;
        this.packageName = component.getPackageName();
        this.shortcutId = "";
        this.shortcutIntentUri = "";
        this.label = label;
        this.icon = icon;
        this.userId = Math.max(0, userId);
    }

    RuntimeTarget(String packageName, String shortcutId, String label, Drawable icon) {
        this(packageName, shortcutId, "", label, icon, 0);
    }

    RuntimeTarget(String packageName, String shortcutId, String shortcutIntentUri,
                  String label, Drawable icon, int userId) {
        this.component = null;
        this.packageName = packageName;
        this.shortcutId = shortcutId;
        this.shortcutIntentUri = shortcutIntentUri == null ? "" : shortcutIntentUri;
        this.label = label;
        this.icon = icon;
        this.userId = Math.max(0, userId);
    }

    boolean isShortcut() { return !shortcutId.isEmpty(); }
    boolean isLauncherShortcut() { return isShortcut() && !shortcutIntentUri.isEmpty(); }
}
