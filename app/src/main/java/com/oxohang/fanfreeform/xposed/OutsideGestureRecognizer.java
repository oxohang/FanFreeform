package com.oxohang.fanfreeform.xposed;

import android.graphics.Rect;
import android.os.Handler;
import android.view.ViewConfiguration;

final class OutsideGestureRecognizer {
    private static final long DOUBLE_TAP_WINDOW_MS = 180L;

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
    private boolean sideGestureCandidate;
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
        this.doubleTapTimeout = Math.min(ViewConfiguration.getDoubleTapTimeout(),
                DOUBLE_TAP_WINDOW_MS);
        this.tapTimeout = ViewConfiguration.getLongPressTimeout();
    }

    boolean onDown(float x, float y, Rect bounds, int displayWidth,
                   float leftReserve, float rightReserve, long eventTime) {
        active = false;
        secondTap = false;
        sideGestureCandidate = false;
        if (bounds == null) return false;

        GestureGeometry.OutsideRegion region = GestureGeometry.outsideRegion(
                x, y, bounds.left, bounds.top, bounds.right, bounds.bottom);
        if (region != GestureGeometry.OutsideRegion.OUTSIDE) return false;

        sideGestureCandidate = GestureGeometry.inSideGestureReserve(
                x, displayWidth, leftReserve, rightReserve);

        if (pendingTap) {
            boolean inTime = eventTime - pendingTime <= doubleTapTimeout;
            boolean inRange = squaredDistance(x, y, pendingX, pendingY) <= doubleTapSlopSquared;
            if (inTime && inRange) {
                secondTap = true;
            } else {
                dispatchPendingSingle();
            }
        }

        active = true;
        downX = x;
        downY = y;
        downTime = eventTime;
        if (sideGestureCandidate) {
            Log.i("Outside side-edge tap armed; movement still reserved for system x="
                    + Math.round(x));
        }
        return !sideGestureCandidate;
    }

    void onMove(float x, float y) {
        if (active && squaredDistance(x, y, downX, downY) > touchSlopSquared) {
            active = false;
            secondTap = false;
            if (sideGestureCandidate) {
                Log.i("Outside side-edge movement yielded to system gesture");
            }
            sideGestureCandidate = false;
        }
    }

    void onUp(float x, float y, long eventTime, Runnable claimInput) {
        if (!active) return;
        boolean valid = eventTime - downTime <= tapTimeout
                && squaredDistance(x, y, downX, downY) <= touchSlopSquared;
        active = false;
        if (!valid) {
            secondTap = false;
            sideGestureCandidate = false;
            return;
        }
        if (sideGestureCandidate && claimInput != null) claimInput.run();
        sideGestureCandidate = false;
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
        sideGestureCandidate = false;
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
