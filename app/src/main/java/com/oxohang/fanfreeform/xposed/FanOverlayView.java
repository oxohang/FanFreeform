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
import android.view.View;

import java.util.Collections;
import java.util.List;

final class FanOverlayView extends View {
    private final Paint backdrop = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path iconClipPath = new Path();
    private float fanRadius;
    private float iconDiameter;
    private List<RuntimeTarget> targets = Collections.emptyList();
    private GestureGeometry.Corner corner = GestureGeometry.Corner.LEFT;
    private int selected = -1;
    private float pointerX;
    private float pointerY;

    FanOverlayView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        ring.setColor(0x66ffffff);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(dp(1.5f));
        iconShadowPaint.setColor(0x01000000);
        iconShadowPaint.setShadowLayer(dp(9), 0, dp(3), 0x66000000);
        iconStrokePaint.setStyle(Paint.Style.STROKE);
        iconStrokePaint.setStrokeWidth(dp(1));
        iconStrokePaint.setColor(0x99ffffff);
        selectedPaint.setColor(0xff6572f6);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(dp(4));
        selectedPaint.setShadowLayer(dp(12), 0, dp(4), 0x66000000);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(14));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        pillPaint.setColor(0xdd20263f);
    }

    void configure(List<RuntimeTarget> targets, GestureGeometry.Corner corner,
                   float radius, float iconDiameter) {
        this.targets = targets;
        this.corner = corner;
        this.fanRadius = radius;
        this.iconDiameter = iconDiameter;
        selected = -1;
        updateBackdropShader();
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
        float originX = corner == GestureGeometry.Corner.LEFT ? 0 : getWidth();
        float originY = getHeight();
        float radius = fanRadius > 0 ? fanRadius : Math.min(getWidth(), getHeight()) * 0.58f;
        canvas.drawCircle(originX, originY, radius + dp(100), backdrop);

        canvas.drawCircle(originX, originY, radius, ring);
        canvas.drawCircle(originX, originY, radius - dp(64), ring);

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
            float centerX = Math.max(width / 2 + dp(10), Math.min(getWidth() - width / 2 - dp(10), pointerX));
            float centerY = Math.max(dp(42), pointerY - dp(58));
            canvas.drawRoundRect(centerX - width / 2, centerY - dp(22), centerX + width / 2,
                    centerY + dp(12), dp(17), dp(17), pillPaint);
            canvas.drawText(label, centerX, centerY, textPaint);
        }
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
        drawable.setBounds(Math.round(centerX - drawWidth / 2f), Math.round(centerY - drawHeight / 2f),
                Math.round(centerX + drawWidth / 2f), Math.round(centerY + drawHeight / 2f));
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
        backdrop.setShader(new RadialGradient(originX, getHeight(), fanRadius + dp(100),
                new int[]{0x99202742, 0x77202742, 0x00202742},
                new float[]{0f, 0.72f, 1f}, Shader.TileMode.CLAMP));
    }
}
