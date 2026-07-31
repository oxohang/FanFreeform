package com.oxohang.fanfreeform.xposed;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

final class RecentTaskPreview {
    final int taskId;
    final String packageName;
    final CharSequence label;
    final Bitmap snapshot;
    final Drawable icon;

    RecentTaskPreview(int taskId, String packageName, CharSequence label,
                      Bitmap snapshot, Drawable icon) {
        this.taskId = taskId;
        this.packageName = packageName;
        this.label = label;
        this.snapshot = snapshot;
        this.icon = icon;
    }
}
