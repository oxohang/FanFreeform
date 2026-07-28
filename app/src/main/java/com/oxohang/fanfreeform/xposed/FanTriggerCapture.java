package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import de.robv.android.xposed.XposedHelpers;

final class FanTriggerCapture {
    private static final int TYPE_NAVIGATION_BAR_PANEL = 2024;

    private final Context context;
    private final Handler mainHandler;
    private final WindowManager windowManager;
    private View leftView;
    private View rightView;
    private volatile boolean capturing;
    private volatile long injectedDownTime;
    private volatile long injectedUntil;
    private volatile float injectedX;
    private volatile float injectedY;
    private int width;
    private int height;
    private int leftInset;
    private int rightInset;
    private boolean desiredEnabled;
    private int desiredHotWidthPercent;
    private int desiredHotHeightPercent;
    private int desiredLeftInset;
    private int desiredRightInset;
    private boolean passthroughInProgress;

    FanTriggerCapture(Context context, Handler mainHandler) {
        this.context = context;
        this.mainHandler = mainHandler;
        windowManager = context.getSystemService(WindowManager.class);
    }

    void update(boolean enabled, int hotWidthPercent, int hotHeightPercent,
                int leftInset, int rightInset) {
        runOnMain(() -> updateNow(enabled, hotWidthPercent, hotHeightPercent,
                leftInset, rightInset));
    }

    boolean isCapturing() {
        return capturing;
    }

    boolean isInjectedEvent(MotionEvent event) {
        if (event == null || SystemClock.uptimeMillis() > injectedUntil) return false;
        return event.getDownTime() == injectedDownTime
                || (Math.abs(event.getX() - injectedX) <= 2f
                && Math.abs(event.getY() - injectedY) <= 2f);
    }

    void passthroughTap(float x, float y, int displayId) {
        runOnMain(() -> passthroughTapNow(x, y, displayId));
    }

    void dispatchBack(int displayId) {
        runOnMain(() -> dispatchBackNow(displayId));
    }

    private void updateNow(boolean enabled, int hotWidthPercent, int hotHeightPercent,
                           int leftInset, int rightInset) {
        desiredEnabled = enabled;
        desiredHotWidthPercent = hotWidthPercent;
        desiredHotHeightPercent = hotHeightPercent;
        desiredLeftInset = Math.max(0, leftInset);
        desiredRightInset = Math.max(0, rightInset);
        if (passthroughInProgress) return;
        if (!enabled) {
            removeNow();
            return;
        }
        Rect bounds = windowManager.getCurrentWindowMetrics().getBounds();
        int nextWidth = Math.max(1, bounds.width() * hotWidthPercent / 100);
        int nextHeight = Math.max(1, bounds.height() * hotHeightPercent / 100);
        if (capturing && nextWidth == width && nextHeight == height
                && this.leftInset == desiredLeftInset
                && this.rightInset == desiredRightInset) return;
        removeNow();
        width = nextWidth;
        height = nextHeight;
        this.leftInset = desiredLeftInset;
        this.rightInset = desiredRightInset;
        if (!attach(TYPE_NAVIGATION_BAR_PANEL) && !attach(2038)) {
            Log.i("Corner trigger capture unavailable; using pilfer fallback");
        }
    }

    private void passthroughTapNow(float x, float y, int displayId) {
        if (passthroughInProgress) return;
        passthroughInProgress = true;
        removeNow();
        long downTime = SystemClock.uptimeMillis() + 24L;
        injectedDownTime = downTime;
        injectedUntil = downTime + 140L;
        injectedX = x;
        injectedY = y;
        mainHandler.postDelayed(() -> injectTapEvent(MotionEvent.ACTION_DOWN,
                downTime, downTime, x, y, displayId), 24L);
        mainHandler.postDelayed(() -> injectTapEvent(MotionEvent.ACTION_UP,
                downTime, downTime + 18L, x, y, displayId), 42L);
        mainHandler.postDelayed(() -> {
            passthroughInProgress = false;
            updateNow(desiredEnabled, desiredHotWidthPercent, desiredHotHeightPercent,
                    desiredLeftInset, desiredRightInset);
            Log.i("Corner trigger capture restored after application tap passthrough");
        }, 96L);
        Log.i("Replaying corner tap to application x=" + Math.round(x)
                + " y=" + Math.round(y));
    }

    private void injectTapEvent(int action, long downTime, long eventTime,
                                float x, float y, int displayId) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        try {
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            try {
                XposedHelpers.callMethod(event, "setDisplayId", displayId);
            } catch (Throwable ignored) {}
            Object inputManager = context.getSystemService(Context.INPUT_SERVICE);
            Object result = XposedHelpers.callMethod(inputManager,
                    "injectInputEvent", event, 0);
            if (result instanceof Boolean && !((Boolean) result)) {
                Log.i("Application tap passthrough was rejected action=" + action);
            }
        } catch (Throwable error) {
            Log.e("Cannot replay application tap action=" + action, error);
        } finally {
            event.recycle();
        }
    }

    private void dispatchBackNow(int displayId) {
        long downTime = SystemClock.uptimeMillis();
        injectBackEvent(KeyEvent.ACTION_DOWN, downTime, downTime, displayId);
        injectBackEvent(KeyEvent.ACTION_UP, downTime, downTime + 16L, displayId);
        Log.i("Captured side swipe dispatched as system back");
    }

    private void injectBackEvent(int action, long downTime, long eventTime, int displayId) {
        KeyEvent event = new KeyEvent(downTime, eventTime, action, KeyEvent.KEYCODE_BACK,
                0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                InputDevice.SOURCE_KEYBOARD);
        try {
            try {
                XposedHelpers.callMethod(event, "setDisplayId", displayId);
            } catch (Throwable ignored) {}
            Object inputManager = context.getSystemService(Context.INPUT_SERVICE);
            Object result = XposedHelpers.callMethod(inputManager,
                    "injectInputEvent", event, 0);
            if (result instanceof Boolean && !((Boolean) result)) {
                Log.i("Captured side back was rejected action=" + action);
            }
        } catch (Throwable error) {
            Log.e("Cannot dispatch captured side back action=" + action, error);
        }
    }

    private boolean attach(int type) {
        View left = captureView("LEFT");
        View right = captureView("RIGHT");
        try {
            windowManager.addView(left, params(type, Gravity.BOTTOM | Gravity.START,
                    "FanFreeformTriggerLeft", leftInset));
            windowManager.addView(right, params(type, Gravity.BOTTOM | Gravity.END,
                    "FanFreeformTriggerRight", rightInset));
            leftView = left;
            rightView = right;
            capturing = true;
            Log.i("Corner trigger capture attached size=" + width + "x" + height
                    + " inset=" + leftInset + "/" + rightInset + " type=" + type);
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

    private WindowManager.LayoutParams params(int type, int gravity, String title,
                                              int horizontalInset) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = gravity;
        params.x = horizontalInset;
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
