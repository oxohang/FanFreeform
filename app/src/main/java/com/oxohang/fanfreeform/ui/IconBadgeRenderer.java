package com.oxohang.fanfreeform.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** Shared shortcut and dual-app badge drawing for settings previews and SystemUI overlays. */
public final class IconBadgeRenderer {
    private static final Paint BORDER = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint BACKGROUND = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint GLYPH = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path LIGHTNING = new Path();

    static {
        BORDER.setColor(0xffffffff);
        GLYPH.setColor(0xffffffff);
        GLYPH.setStyle(Paint.Style.FILL);
    }

    private IconBadgeRenderer() { }

    public static void drawShortcut(Canvas canvas, float iconX, float iconY,
                                    float iconDiameter, float alpha, float density) {
        drawAtIconCorner(canvas, iconX, iconY, iconDiameter, alpha, density,
                false, false);
    }

    public static void drawDual(Canvas canvas, float iconX, float iconY,
                                float iconDiameter, float alpha, float density,
                                boolean topRight) {
        drawAtIconCorner(canvas, iconX, iconY, iconDiameter, alpha, density,
                true, topRight);
    }

    public static void drawBadge(Canvas canvas, float centerX, float centerY,
                                 float badgeDiameter, float alpha, boolean dual) {
        if (canvas == null || badgeDiameter <= 1f || alpha <= 0f) return;
        float radius = badgeDiameter / 2f;
        int resolvedAlpha = Math.max(0, Math.min(255, Math.round(255f * alpha)));
        BORDER.setAlpha(resolvedAlpha);
        BACKGROUND.setColor(dual ? 0xff249b74 : 0xff4976f2);
        BACKGROUND.setAlpha(resolvedAlpha);
        GLYPH.setAlpha(resolvedAlpha);
        canvas.drawCircle(centerX, centerY, radius + badgeDiameter * 0.085f, BORDER);
        canvas.drawCircle(centerX, centerY, radius, BACKGROUND);
        if (dual) drawDualGlyph(canvas, centerX, centerY, radius);
        else drawLightningGlyph(canvas, centerX, centerY, radius);
    }

    private static void drawAtIconCorner(Canvas canvas, float iconX, float iconY,
                                         float iconDiameter, float alpha, float density,
                                         boolean dual, boolean topRight) {
        if (canvas == null || alpha <= 0f || iconDiameter <= 1f) return;
        float badgeDiameter = Math.max(9f * density, iconDiameter * 0.27f);
        float centerX = iconX + iconDiameter * 0.34f;
        float centerY = iconY + (topRight ? -0.34f : 0.34f) * iconDiameter;
        drawBadge(canvas, centerX, centerY, badgeDiameter, alpha, dual);
    }

    private static void drawDualGlyph(Canvas canvas, float centerX, float centerY,
                                      float radius) {
        Paint.Style oldStyle = GLYPH.getStyle();
        float oldStrokeWidth = GLYPH.getStrokeWidth();
        GLYPH.setStyle(Paint.Style.STROKE);
        GLYPH.setStrokeWidth(Math.max(1f, radius * 0.16f));
        float glyphRadius = radius * 0.24f;
        canvas.drawCircle(centerX - radius * 0.19f, centerY - radius * 0.10f,
                glyphRadius, GLYPH);
        canvas.drawCircle(centerX + radius * 0.19f, centerY + radius * 0.10f,
                glyphRadius, GLYPH);
        GLYPH.setStrokeWidth(oldStrokeWidth);
        GLYPH.setStyle(oldStyle);
    }

    private static void drawLightningGlyph(Canvas canvas, float centerX, float centerY,
                                           float radius) {
        LIGHTNING.reset();
        LIGHTNING.moveTo(centerX + radius * 0.02f, centerY - radius * 0.62f);
        LIGHTNING.lineTo(centerX - radius * 0.45f, centerY + radius * 0.05f);
        LIGHTNING.lineTo(centerX - radius * 0.10f, centerY + radius * 0.05f);
        LIGHTNING.lineTo(centerX - radius * 0.27f, centerY + radius * 0.62f);
        LIGHTNING.lineTo(centerX + radius * 0.47f, centerY - radius * 0.16f);
        LIGHTNING.lineTo(centerX + radius * 0.12f, centerY - radius * 0.16f);
        LIGHTNING.close();
        canvas.drawPath(LIGHTNING, GLYPH);
    }
}
