package com.oxohang.fanfreeform.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

final class GesturePreviewView extends View {
    private final Paint screenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint zonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF screen = new RectF();
    private final RectF leftZone = new RectF();
    private final RectF rightZone = new RectF();
    private final Path clipPath = new Path();
    private int hotWidthPercent;
    private int hotHeightPercent;

    GesturePreviewView(Context context) {
        super(context);
        screenPaint.setColor(0xffe8eaf2);
        zonePaint.setColor(0x886572f6);
        edgePaint.setColor(0xffc8ccd9);
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(Ui.dp(context, 1));
    }

    void update(int hotWidthPercent, int hotHeightPercent) {
        this.hotWidthPercent = hotWidthPercent;
        this.hotHeightPercent = hotHeightPercent;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float margin = Ui.dp(getContext(), 12);
        screen.set(margin, margin, getWidth() - margin, getHeight() - margin);
        float corner = Ui.dp(getContext(), 18);
        canvas.drawRoundRect(screen, corner, corner, screenPaint);
        float zoneWidth = screen.width() * hotWidthPercent / 100f;
        float zoneHeight = screen.height() * hotHeightPercent / 100f;
        leftZone.set(screen.left, screen.bottom - zoneHeight,
                screen.left + zoneWidth, screen.bottom);
        rightZone.set(screen.right - zoneWidth, screen.bottom - zoneHeight,
                screen.right, screen.bottom);
        int save = canvas.save();
        clipPath.reset();
        clipPath.addRoundRect(screen, corner, corner, Path.Direction.CW);
        canvas.clipPath(clipPath);
        canvas.drawRect(leftZone, zonePaint);
        canvas.drawRect(rightZone, zonePaint);
        canvas.restoreToCount(save);
        canvas.drawRoundRect(screen, corner, corner, edgePaint);
    }
}
