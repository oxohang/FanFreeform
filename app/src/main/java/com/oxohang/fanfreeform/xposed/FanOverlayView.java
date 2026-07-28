package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.TextPaint;
import android.view.View;

import java.util.Collections;
import java.util.List;

final class FanOverlayView extends View {
    private final Paint backdrop = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint railPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path iconClipPath = new Path();
    private float fanRadius;
    private float iconDiameter;
    private List<RuntimeTarget> targets = Collections.emptyList();
    private GestureGeometry.Corner corner = GestureGeometry.Corner.LEFT;
    private int selected = -1;
    private float pointerX;
    private float pointerY;
    private boolean showBackdrop = true;
    private boolean sideListLayout;
    private float sideListTop;
    private float sideRowHeight;
    private boolean showSideNames;

    FanOverlayView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        iconShadowPaint.setColor(0x01000000);
        iconShadowPaint.setShadowLayer(dp(9), 0, dp(3), 0x5c000000);
        iconStrokePaint.setStyle(Paint.Style.STROKE);
        iconStrokePaint.setStrokeWidth(dp(1));
        iconStrokePaint.setColor(0x88ffffff);
        selectedPaint.setColor(0xff6572f6);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(dp(3));
        selectedPaint.setShadowLayer(dp(10), 0, dp(3), 0x52000000);
        selectedFillPaint.setColor(0x386572f6);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(14));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        pillPaint.setColor(0xdd20263f);
        railPaint.setColor(0xb820263f);
        railPaint.setShadowLayer(dp(14), 0, dp(4), 0x50000000);
    }

    void configure(List<RuntimeTarget> targets, GestureGeometry.Corner corner,
                   float radius, float iconDiameter, boolean showBackdrop) {
        this.targets = targets;
        this.corner = corner;
        this.fanRadius = radius;
        this.iconDiameter = iconDiameter;
        this.showBackdrop = showBackdrop;
        this.sideListLayout = false;
        this.sideListTop = 0f;
        this.sideRowHeight = 0f;
        this.showSideNames = false;
        selected = -1;
        updateBackdropShader();
        invalidate();
    }

    void configureSideList(List<RuntimeTarget> targets, GestureGeometry.Corner side,
                           float listTop, float rowHeight, float iconDiameter,
                           boolean showNames, boolean showBackdrop) {
        this.targets = targets;
        this.corner = side;
        this.fanRadius = 0f;
        this.iconDiameter = iconDiameter;
        this.showBackdrop = showBackdrop;
        this.sideListLayout = true;
        this.sideListTop = listTop;
        this.sideRowHeight = rowHeight;
        this.showSideNames = showNames;
        selected = -1;
        invalidate();
    }

    void updateSelection(int selected, float x, float y) {
        this.selected = selected;
        pointerX = x;
        pointerY = y;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (targets.isEmpty()) return;
        if (sideListLayout) {
            drawSideList(canvas);
            return;
        }
        float originX = corner == GestureGeometry.Corner.LEFT ? 0 : getWidth();
        float drawOriginY = getHeight();
        float radius = fanRadius > 0 ? fanRadius : Math.min(getWidth(), getHeight()) * 0.58f;
        if (showBackdrop) canvas.drawCircle(originX, drawOriginY, radius + dp(100), backdrop);

        for (int i = 0; i < targets.size(); i++) {
            GestureGeometry.Point center = GestureGeometry.iconCenter(corner, i, targets.size(),
                    getWidth(), getHeight(), radius);
            float x = center.x;
            float y = center.y;
            boolean active = i == selected;
            float diameter = iconDiameter * (active ? 1.08f : 1f);
            float iconRadius = diameter / 2f;
            canvas.drawCircle(x, y, iconRadius, iconShadowPaint);
            drawCircularIcon(canvas, targets.get(i).icon, x, y, diameter);
            canvas.drawCircle(x, y, iconRadius - dp(0.5f), iconStrokePaint);
            if (active) canvas.drawCircle(x, y, iconRadius + dp(6), selectedPaint);
        }

        if (selected >= 0 && selected < targets.size()) {
            String label = targets.get(selected).label;
            float padding = dp(14);
            float width = textPaint.measureText(label) + padding * 2;
            float centerX = Math.max(width / 2 + dp(10),
                    Math.min(getWidth() - width / 2 - dp(10), pointerX));
            float centerY = Math.max(dp(42), pointerY - dp(58));
            canvas.drawRoundRect(centerX - width / 2, centerY - dp(22), centerX + width / 2,
                    centerY + dp(12), dp(17), dp(17), pillPaint);
            canvas.drawText(label, centerX, centerY, textPaint);
        }
    }

    private void drawSideList(Canvas canvas) {
        float edgeMargin = dp(14);
        float railPadding = dp(8);
        float railLeft = corner == GestureGeometry.Corner.LEFT
                ? edgeMargin - railPadding
                : getWidth() - edgeMargin - iconDiameter - railPadding;
        float railRight = corner == GestureGeometry.Corner.LEFT
                ? edgeMargin + iconDiameter + railPadding
                : getWidth() - edgeMargin + railPadding;
        float railTop = sideListTop + dp(2);
        float railBottom = sideListTop + targets.size() * sideRowHeight - dp(2);
        if (showBackdrop) {
            canvas.drawRoundRect(railLeft, railTop, railRight, railBottom,
                    dp(22), dp(22), railPaint);
        }

        for (int i = 0; i < targets.size(); i++) {
            GestureGeometry.Point center = GestureGeometry.sideListIconCenter(
                    corner, i, getWidth(), sideListTop, sideRowHeight,
                    iconDiameter, edgeMargin);
            boolean active = i == selected;
            float diameter = iconDiameter * (active ? 1.1f : 1f);
            float radius = diameter / 2f;
            if (active) canvas.drawCircle(center.x, center.y, radius + dp(7), selectedFillPaint);
            canvas.drawCircle(center.x, center.y, radius, iconShadowPaint);
            drawCircularIcon(canvas, targets.get(i).icon, center.x, center.y, diameter);
            canvas.drawCircle(center.x, center.y, radius - dp(0.5f), iconStrokePaint);
            if (active) canvas.drawCircle(center.x, center.y, radius + dp(5), selectedPaint);
            if (showSideNames || active) {
                drawSideLabel(canvas, targets.get(i).label, center.x, center.y,
                        diameter, active);
            }
        }
    }

    private void drawSideLabel(Canvas canvas, String label, float iconX, float iconY,
                               float diameter, boolean active) {
        float maxTextWidth = Math.min(dp(180), getWidth() * 0.46f);
        CharSequence fitted = TextUtils.ellipsize(label, textPaint,
                maxTextWidth, TextUtils.TruncateAt.END);
        float textWidth = textPaint.measureText(fitted, 0, fitted.length());
        float horizontalPadding = dp(13);
        float gap = dp(10);
        float boxWidth = textWidth + horizontalPadding * 2f;
        float boxHeight = dp(36);
        float left;
        float right;
        if (corner == GestureGeometry.Corner.LEFT) {
            left = iconX + diameter / 2f + gap;
            right = Math.min(getWidth() - dp(10), left + boxWidth);
            left = right - boxWidth;
        } else {
            right = iconX - diameter / 2f - gap;
            left = Math.max(dp(10), right - boxWidth);
            right = left + boxWidth;
        }
        pillPaint.setColor(active ? 0xf020263f : 0xc820263f);
        canvas.drawRoundRect(left, iconY - boxHeight / 2f, right,
                iconY + boxHeight / 2f, boxHeight / 2f, boxHeight / 2f, pillPaint);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = iconY - (metrics.ascent + metrics.descent) / 2f;
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(fitted, 0, fitted.length(), (left + right) / 2f,
                baseline, textPaint);
    }

    private void drawCircularIcon(Canvas canvas, Drawable drawable, float centerX, float centerY,
                                  float diameter) {
        if (drawable == null) return;
        float radius = diameter / 2f;
        int intrinsicWidth = Math.max(1, drawable.getIntrinsicWidth());
        int intrinsicHeight = Math.max(1, drawable.getIntrinsicHeight());
        float target = diameter * 1.16f;
        float scale = Math.max(target / intrinsicWidth, target / intrinsicHeight);
        int drawWidth = Math.round(intrinsicWidth * scale);
        int drawHeight = Math.round(intrinsicHeight * scale);
        Rect old = drawable.copyBounds();
        int save = canvas.save();
        iconClipPath.reset();
        iconClipPath.addCircle(centerX, centerY, radius, Path.Direction.CW);
        canvas.clipPath(iconClipPath);
        drawable.setBounds(Math.round(centerX - drawWidth / 2f),
                Math.round(centerY - drawHeight / 2f),
                Math.round(centerX + drawWidth / 2f),
                Math.round(centerY + drawHeight / 2f));
        drawable.draw(canvas);
        canvas.restoreToCount(save);
        drawable.setBounds(old);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        updateBackdropShader();
    }

    private void updateBackdropShader() {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        if (fanRadius <= 0) fanRadius = Math.min(getWidth(), getHeight()) * 0.58f;
        float originX = corner == GestureGeometry.Corner.LEFT ? 0 : getWidth();
        float drawOriginY = getHeight();
        backdrop.setShader(new RadialGradient(originX, drawOriginY, fanRadius + dp(100),
                new int[]{0x99202742, 0x77202742, 0x00202742},
                new float[]{0f, 0.72f, 1f}, Shader.TileMode.CLAMP));
    }
}
