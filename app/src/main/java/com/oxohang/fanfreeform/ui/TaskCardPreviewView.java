package com.oxohang.fanfreeform.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

final class TaskCardPreviewView extends View {
    private final float density;
    private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint card = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detail = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private int widthDp;
    private int heightDp;
    private int cornerDp;

    TaskCardPreviewView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        background.setColor(0xffeef0f7);
        detail.setColor(0xb8ffffff);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    void update(int widthDp, int heightDp, int cornerDp) {
        this.widthDp = widthDp;
        this.heightDp = heightDp;
        this.cornerDp = cornerDp;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float outerCorner = 18f * density;
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(),
                outerCorner, outerCorner, background);
        if (widthDp <= 0 || heightDp <= 0) return;

        float availableWidth = getWidth() - 52f * density;
        float availableHeight = getHeight() - 30f * density;
        float scale = Math.min(availableWidth / widthDp, availableHeight / heightDp);
        float width = widthDp * scale;
        float height = heightDp * scale;
        float left = (getWidth() - width) / 2f;
        float top = (getHeight() - height) / 2f;
        bounds.set(left, top, left + width, top + height);
        float corner = Math.min(Math.min(width, height) / 2f, cornerDp * scale);

        card.setShader(new LinearGradient(left, top, bounds.right, bounds.bottom,
                0xff59627d, 0xff222633, Shader.TileMode.CLAMP));
        card.setShadowLayer(14f * density, 0, 5f * density, 0x55000000);
        canvas.drawRoundRect(bounds, corner, corner, card);
        card.clearShadowLayer();
        card.setShader(null);

        float padding = Math.min(width, height) * 0.10f;
        float headerHeight = Math.max(8f * density, height * 0.13f);
        canvas.drawRoundRect(left + padding, top + padding,
                bounds.right - padding, top + padding + headerHeight,
                headerHeight / 2f, headerHeight / 2f, detail);
        detail.setAlpha(145);
        float lineHeight = Math.max(5f * density, height * 0.055f);
        float lineTop = top + padding * 2f + headerHeight;
        for (int index = 0; index < 3; index++) {
            float rightInset = index == 2 ? width * 0.30f : padding;
            canvas.drawRoundRect(left + padding, lineTop,
                    bounds.right - rightInset, lineTop + lineHeight,
                    lineHeight / 2f, lineHeight / 2f, detail);
            lineTop += lineHeight * 2.1f;
        }
        detail.setAlpha(255);
    }
}
