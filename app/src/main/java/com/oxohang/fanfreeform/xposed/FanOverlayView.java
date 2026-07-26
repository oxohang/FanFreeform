package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float fanRadius;
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
        selectedPaint.setColor(0xff6572f6);
        selectedPaint.setShadowLayer(dp(12), 0, dp(4), 0x66000000);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(14));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        pillPaint.setColor(0xdd20263f);
    }

    void configure(List<RuntimeTarget> targets, GestureGeometry.Corner corner) {
        this.targets = targets;
        this.corner = corner;
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
        float radius = fanRadius > 0 ? fanRadius : Math.min(getWidth() * 0.72f, dp(292));
        canvas.drawCircle(originX, originY, radius + dp(100), backdrop);

        canvas.drawCircle(originX, originY, radius, ring);
        canvas.drawCircle(originX, originY, radius - dp(64), ring);

        for (int i = 0; i < targets.size(); i++) {
            double degrees = targets.size() == 1 ? 47 : 12 + (70.0 * i / (targets.size() - 1));
            double radians = Math.toRadians(degrees);
            float x = corner == GestureGeometry.Corner.LEFT
                    ? (float) (radius * Math.cos(radians))
                    : getWidth() - (float) (radius * Math.cos(radians));
            float y = getHeight() - (float) (radius * Math.sin(radians));
            boolean active = i == selected;
            float circleRadius = dp(active ? 35 : 29);
            Paint circle = active ? selectedPaint : ring;
            if (!active) {
                ring.setStyle(Paint.Style.FILL);
                ring.setColor(0xddffffff);
            }
            canvas.drawCircle(x, y, circleRadius, circle);
            if (!active) {
                ring.setStyle(Paint.Style.STROKE);
                ring.setColor(0x66ffffff);
            }
            drawIcon(canvas, targets.get(i).icon, x, y, dp(active ? 52 : 44));
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

    private void drawIcon(Canvas canvas, Drawable drawable, float centerX, float centerY, float size) {
        if (drawable == null) return;
        int half = Math.round(size / 2);
        Rect old = drawable.copyBounds();
        drawable.setBounds(Math.round(centerX) - half, Math.round(centerY) - half,
                Math.round(centerX) + half, Math.round(centerY) + half);
        drawable.draw(canvas);
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
        fanRadius = Math.min(getWidth() * 0.72f, dp(292));
        float originX = corner == GestureGeometry.Corner.LEFT ? 0 : getWidth();
        backdrop.setShader(new RadialGradient(originX, getHeight(), fanRadius + dp(100),
                new int[]{0x99202742, 0x77202742, 0x00202742},
                new float[]{0f, 0.72f, 1f}, Shader.TileMode.CLAMP));
    }
}
