package com.oxohang.fanfreeform.xposed;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;

final class RuntimeTarget {
    final ComponentName component;
    final String label;
    final Drawable icon;

    RuntimeTarget(ComponentName component, String label, Drawable icon) {
        this.component = component;
        this.label = label;
        this.icon = icon;
    }
}
