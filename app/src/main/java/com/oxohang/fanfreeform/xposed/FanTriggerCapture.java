package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

final class FanTriggerCapture {
    private static final int TYPE_NAVIGATION_BAR_PANEL = 2024;

    private final Context context;
    private final Handler mainHandler;
    private final WindowManager windowManager;
    private View leftView;
    private View rightView;
    private volatile boolean capturing;
    private int width;
    private int height;

    FanTriggerCapture(Context context, Handler mainHandler) {
        this.context = context;
        this.mainHandler = mainHandler;
        windowManager = context.getSystemService(WindowManager.class);
    }

    void update(boolean enabled, int hotWidthPercent, int hotHeightPercent) {
        runOnMain(() -> updateNow(enabled, hotWidthPercent, hotHeightPercent));
    }

    boolean isCapturing() {
        return capturing;
    }

    private void updateNow(boolean enabled, int hotWidthPercent, int hotHeightPercent) {
        if (!enabled) {
            removeNow();
            return;
        }
        Rect bounds = windowManager.getCurrentWindowMetrics().getBounds();
        int nextWidth = Math.max(1, bounds.width() * hotWidthPercent / 100);
        int nextHeight = Math.max(1, bounds.height() * hotHeightPercent / 100);
        if (capturing && nextWidth == width && nextHeight == height) return;
        removeNow();
        width = nextWidth;
        height = nextHeight;
        if (!attach(TYPE_NAVIGATION_BAR_PANEL) && !attach(2038)) {
            Log.i("Corner trigger capture unavailable; using pilfer fallback");
        }
    }

    private boolean attach(int type) {
        View left = captureView("LEFT");
        View right = captureView("RIGHT");
        try {
            windowManager.addView(left, params(type, Gravity.BOTTOM | Gravity.START,
                    "FanFreeformTriggerLeft"));
            windowManager.addView(right, params(type, Gravity.BOTTOM | Gravity.END,
                    "FanFreeformTriggerRight"));
            leftView = left;
            rightView = right;
            capturing = true;
            Log.i("Corner trigger capture attached size=" + width + "x" + height
                    + " type=" + type);
            return true;
        } catch (Throwable error) {
            removeView(left);
            removeView(right);
            leftView = null;
            rightView = null;
            capturing = false;
            Log.e("Cannot attach corner trigger capture type=" + type, error);
            return false;
        }
    }

    private View captureView(String corner) {
        View view = new View(context);
        view.setClickable(true);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        view.setOnTouchListener((target, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                Log.i("Corner trigger window received down corner=" + corner);
            }
            return true;
        });
        return view;
    }

    private WindowManager.LayoutParams params(int type, int gravity, String title) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = gravity;
        params.setTitle(title);
        params.setFitInsetsTypes(0);
        params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        return params;
    }

    private void removeNow() {
        removeView(leftView);
        removeView(rightView);
        leftView = null;
        rightView = null;
        capturing = false;
    }

    private void removeView(View view) {
        if (view == null || !view.isAttachedToWindow()) return;
        try {
            windowManager.removeViewImmediate(view);
        } catch (Throwable error) {
            Log.e("Cannot remove corner trigger capture", error);
        }
    }

    private void runOnMain(Runnable runnable) {
        if (mainHandler.getLooper().isCurrentThread()) runnable.run();
        else mainHandler.post(runnable);
    }
}
