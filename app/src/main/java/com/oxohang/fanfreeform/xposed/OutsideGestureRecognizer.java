package com.oxohang.fanfreeform.xposed;

import android.graphics.Rect;
import android.os.Handler;
import android.view.ViewConfiguration;

final class OutsideGestureRecognizer {
    interface Listener {
        void onOutsideAction(boolean doubleTap);
    }

    private final Handler handler;
    private final Listener listener;
    private final float touchSlopSquared;
    private final float doubleTapSlopSquared;
    private final long doubleTapTimeout;
    private final long tapTimeout;

    private boolean active;
    private boolean secondTap;
    private float downX;
    private float downY;
    private long downTime;
    private boolean pendingTap;
    private float pendingX;
    private float pendingY;
    private long pendingTime;
    private final Runnable singleTapRunnable = this::dispatchPendingSingle;

    OutsideGestureRecognizer(Handler handler, ViewConfiguration configuration, Listener listener) {
        this.handler = handler;
        this.listener = listener;
        float touchSlop = configuration.getScaledTouchSlop();
        float doubleTapSlop = configuration.getScaledDoubleTapSlop();
        this.touchSlopSquared = touchSlop * touchSlop;
        this.doubleTapSlopSquared = doubleTapSlop * doubleTapSlop;
        this.doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout();
        this.tapTimeout = ViewConfiguration.getLongPressTimeout();
    }

    boolean onDown(float x, float y, Rect bounds, int displayWidth,
                   float leftReserve, float rightReserve, long eventTime) {
        active = false;
        secondTap = false;
        if (bounds == null) return false;
        if (GestureGeometry.inSideGestureReserve(x, displayWidth, leftReserve, rightReserve)) {
            Log.i("System side gesture reserved x=" + Math.round(x));
            return false;
        }

        GestureGeometry.OutsideRegion region = GestureGeometry.outsideRegion(
                x, y, bounds.left, bounds.top, bounds.right, bounds.bottom);
        if (region != GestureGeometry.OutsideRegion.OUTSIDE) return false;

        if (pendingTap) {
            boolean inTime = eventTime - pendingTime <= doubleTapTimeout;
            boolean inRange = squaredDistance(x, y, pendingX, pendingY) <= doubleTapSlopSquared;
            if (inTime && inRange) {
                secondTap = true;
            } else {
                dispatchPendingSingle();
                return true;
            }
        }

        active = true;
        downX = x;
        downY = y;
        downTime = eventTime;
        return true;
    }

    void onMove(float x, float y) {
        if (active && squaredDistance(x, y, downX, downY) > touchSlopSquared) {
            active = false;
            secondTap = false;
        }
    }

    void onUp(float x, float y, long eventTime) {
        if (!active) return;
        boolean valid = eventTime - downTime <= tapTimeout
                && squaredDistance(x, y, downX, downY) <= touchSlopSquared;
        active = false;
        if (!valid) {
            secondTap = false;
            return;
        }
        if (secondTap) {
            clearPending();
            secondTap = false;
            listener.onOutsideAction(true);
            return;
        }
        pendingTap = true;
        pendingX = x;
        pendingY = y;
        pendingTime = eventTime;
        handler.removeCallbacks(singleTapRunnable);
        handler.postDelayed(singleTapRunnable, doubleTapTimeout);
    }

    void onCancel() {
        active = false;
        secondTap = false;
    }

    void clearAll() {
        onCancel();
        clearPending();
    }

    private void dispatchPendingSingle() {
        if (!pendingTap) return;
        clearPending();
        listener.onOutsideAction(false);
    }

    private void clearPending() {
        handler.removeCallbacks(singleTapRunnable);
        pendingTap = false;
        pendingTime = 0L;
    }

    private static float squaredDistance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return dx * dx + dy * dy;
    }
}
