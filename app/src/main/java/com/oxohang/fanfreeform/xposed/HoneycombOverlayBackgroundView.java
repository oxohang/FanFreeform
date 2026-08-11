package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class HoneycombOverlayBackgroundView extends View {
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wallpaperPaint = new Paint(Paint.ANTI_ALIAS_FLAG
            | Paint.FILTER_BITMAP_FLAG);
    private final RectF wallpaperBounds = new RectF();
    private boolean released;
    private final BlurredWallpaperCache.Callback wallpaperCallback = bitmap -> post(() -> {
        if (released) return;
        wallpaper = bitmap;
        invalidate();
    });

    private Bitmap wallpaper;
    private boolean appBackgroundEnabled;
    private boolean liveBlurEnabled;
    private int appBackgroundColor;
    private int backgroundDimPercent;
    private int wallpaperBackgroundStyle;
    private int wallpaperDimPercent;
    private int wallpaperBlurDp;
    private int statusBarHeight;
    private float visible = 1f;

    HoneycombOverlayBackgroundView(Context context) {
        super(context);
        backgroundPaint.setColor(Color.BLACK);
        appBackgroundColor = ForegroundAppBackgroundResolver.fallback(context);
        statusBarHeight = 0;
    }

    void configure(GestureConfig config) {
        appBackgroundEnabled = config.honeycombAppBackgroundEnabled;
        liveBlurEnabled = config.honeycombLiveBlurEnabled;
        backgroundDimPercent = config.honeycombBackgroundDimPercent;
        wallpaperBackgroundStyle = config.honeycombBackgroundStyle;
        wallpaperDimPercent = config.honeycombDimPercent;
        wallpaperBlurDp = config.honeycombBlurDp;
        if (!appBackgroundEnabled && !liveBlurEnabled
                && wallpaperBackgroundStyle == com.oxohang.fanfreeform.config.ConfigContract
                .HONEYCOMB_BACKGROUND_BLUR) {
            wallpaper = BlurredWallpaperCache.getOrRequest(getContext(), wallpaperBlurDp,
                    wallpaperCallback);
        }
        invalidate();
    }

    void setAppBackgroundColor(int color) {
        if (released) return;
        appBackgroundColor = color | 0xff000000;
        invalidate();
    }

    void setVisible(float value) {
        float next = Math.max(0f, Math.min(1f, value));
        if (Math.abs(next - visible) < 0.001f) return;
        visible = next;
        invalidate();
    }

    void releaseResources() {
        released = true;
        wallpaper = null;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (liveBlurEnabled) {
            if (appBackgroundEnabled) {
                backgroundPaint.setColor(appBackgroundColor);
                backgroundPaint.setAlpha(Math.round(92f * visible));
                canvas.drawRect(0, statusBarHeight, getWidth(), getHeight(), backgroundPaint);
            }
            backgroundPaint.setColor(Color.BLACK);
            backgroundPaint.setAlpha(Math.round(255f * backgroundDimPercent / 100f * visible));
        } else if (appBackgroundEnabled) {
            backgroundPaint.setColor(appBackgroundColor);
            backgroundPaint.setAlpha(Math.round(255f * visible));
        } else if (wallpaper != null && wallpaperBackgroundStyle
                == com.oxohang.fanfreeform.config.ConfigContract.HONEYCOMB_BACKGROUND_BLUR) {
            wallpaperPaint.setAlpha(Math.round(255f * visible));
            wallpaperBounds.set(0, statusBarHeight, getWidth(), getHeight());
            canvas.drawBitmap(wallpaper, null, wallpaperBounds, wallpaperPaint);
            backgroundPaint.setColor(Color.BLACK);
            backgroundPaint.setAlpha(Math.round(255f * wallpaperDimPercent / 100f * visible));
        } else {
            backgroundPaint.setColor(Color.BLACK);
            backgroundPaint.setAlpha(Math.round(255f * visible));
        }
        canvas.drawRect(0, statusBarHeight, getWidth(), getHeight(), backgroundPaint);
    }
}
