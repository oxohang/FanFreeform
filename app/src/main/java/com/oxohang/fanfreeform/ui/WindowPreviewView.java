package com.oxohang.fanfreeform.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public final class WindowPreviewView extends View {
    private final Paint screenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint windowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF screenRect = new RectF();
    private final RectF windowRect = new RectF();
    private int widthPercent = 62;
    private int heightPercent = 58;
    private int positionX = 50;
    private int positionY = 50;
    private boolean landscape;

    public WindowPreviewView(Context context) {
        super(context);
        screenPaint.setColor(0xffe7e9f2);
        windowPaint.setColor(0xff737ffc);
        strokePaint.setColor(0x335a67f2);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(Ui.dp(context, 2));
        setMinimumHeight(Ui.dp(context, 230));
    }

    public void update(int widthPercent, int heightPercent, int positionX, int positionY) {
        this.widthPercent = widthPercent;
        this.heightPercent = heightPercent;
        this.positionX = positionX;
        this.positionY = positionY;
        invalidate();
    }

    public void setLandscape(boolean landscape) {
        this.landscape = landscape;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float pad = Ui.dp(getContext(), 18);
        float availableW = getWidth() - pad * 2;
        float availableH = getHeight() - pad * 2;
        float screenRatio = landscape ? 2608f / 1200f : 1200f / 2608f;
        float screenH = availableH;
        float screenW = screenH * screenRatio;
        if (screenW > availableW) {
            screenW = availableW;
            screenH = screenW / screenRatio;
        }
        float left = (getWidth() - screenW) / 2f;
        float top = (getHeight() - screenH) / 2f;
        screenRect.set(left, top, left + screenW, top + screenH);
        canvas.drawRoundRect(screenRect, Ui.dp(getContext(), 18), Ui.dp(getContext(), 18), screenPaint);

        float winW = screenW * widthPercent / 100f;
        float winH = screenH * heightPercent / 100f;
        float travelX = screenW - winW;
        float travelY = screenH - winH;
        float winLeft = left + travelX * positionX / 100f;
        float winTop = top + travelY * positionY / 100f;
        windowRect.set(winLeft, winTop, winLeft + winW, winTop + winH);
        canvas.drawRoundRect(windowRect, Ui.dp(getContext(), 12), Ui.dp(getContext(), 12), windowPaint);
        canvas.drawRoundRect(windowRect, Ui.dp(getContext(), 12), Ui.dp(getContext(), 12), strokePaint);
    }
}
