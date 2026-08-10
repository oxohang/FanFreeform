package com.oxohang.fanfreeform.xposed;

import android.graphics.Canvas;

import com.oxohang.fanfreeform.ui.IconBadgeRenderer;

final class ShortcutBadgeRenderer {

    private ShortcutBadgeRenderer() { }

    static void draw(Canvas canvas, float iconX, float iconY, float iconDiameter,
                     float alpha, float density) {
        IconBadgeRenderer.drawShortcut(canvas, iconX, iconY, iconDiameter, alpha, density);
    }

    static void drawDual(Canvas canvas, float iconX, float iconY, float iconDiameter,
                         float alpha, float density, boolean topRight) {
        IconBadgeRenderer.drawDual(canvas, iconX, iconY, iconDiameter, alpha, density,
                topRight);
    }
}
