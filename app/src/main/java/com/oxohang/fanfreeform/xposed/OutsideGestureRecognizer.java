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
    private final float sideTapSlopSquared;
    private final float sideSwipeDecisionDistance;
    private final float sideTapRetentionDistance;
    private final float doubleTapSlopSquared;
    private long doubleTapTimeout;
    private final long tapTimeout;

    private boolean active;
    private boolean secondTap;
    private boolean sideGestureCandidate;
    private boolean sideFromLeft;
    private float downX;
    private float downY;
    private long downTime;
    private long lastAcceptedDownEventTime = Long.MIN_VALUE;
    private boolean pendingTap;
    private float pendingX;
    private float pendingY;
    private long pendingTime;
    private final Runnable singleTapRunnable = this::dispatchPendingSingle;

    OutsideGestureRecognizer(Handler handler, ViewConfiguration configuration,
                             float density, Listener listener) {
        this.handler = handler;
        this.listener = listener;
        float touchSlop = configuration.getScaledTouchSlop();
        float doubleTapSlop = configuration.getScaledDoubleTapSlop();
        this.touchSlopSquared = touchSlop * touchSlop;
        this.sideSwipeDecisionDistance = Math.max(touchSlop * 1.5f, 12f * density);
        this.sideTapRetentionDistance = Math.max(touchSlop * 2f, 18f * density);
        this.sideTapSlopSquared = sideTapRetentionDistance * sideTapRetentionDistance;
        this.doubleTapSlopSquared = doubleTapSlop * doubleTapSlop;
        this.doubleTapTimeout = com.oxohang.fanfreeform.config.ConfigContract
                .DEFAULT_OUTSIDE_TAP_WINDOW_MS;
        this.tapTimeout = ViewConfiguration.getLongPressTimeout();
    }

    void setDoubleTapTimeout(long timeoutMs) {
        long resolved = Math.max(com.oxohang.fanfreeform.config.ConfigContract
                        .MIN_OUTSIDE_TAP_WINDOW_MS,
                Math.min(com.oxohang.fanfreeform.config.ConfigContract
                                .MAX_OUTSIDE_TAP_WINDOW_MS, timeoutMs));
        if (doubleTapTimeout == resolved) return;
        if (pendingTap) dispatchPendingSingle();
        doubleTapTimeout = resolved;
    }

    boolean onDown(float x, float y, Rect bounds, int displayWidth,
                   float leftReserve, float rightReserve, long eventTime) {
        // Side-edge touches are visible both to the global input monitor and to the
        // transparent outside window. They carry the same event time. If the second
        // callback is delivered after the first UP, treating it as a new DOWN turns one
        // physical tap into a double tap. Accept each physical stream only once.
        if (eventTime == lastAcceptedDownEventTime) {
            Log.i("Ignored duplicate outside DOWN eventTime=" + eventTime);
            return false;
        }
        lastAcceptedDownEventTime = eventTime;
        active = false;
        secondTap = false;
        sideGestureCandidate = false;
        sideFromLeft = false;
        if (bounds == null) return false;

        GestureGeometry.OutsideRegion region = GestureGeometry.outsideRegion(
                x, y, bounds.left, bounds.top, bounds.right, bounds.bottom);
        if (region != GestureGeometry.OutsideRegion.OUTSIDE) return false;

        sideGestureCandidate = GestureGeometry.inSideGestureReserve(
                x, displayWidth, leftReserve, rightReserve);
        sideFromLeft = sideGestureCandidate && x <= leftReserve;

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
        if (!active) return;
        boolean movedOutsideTap = sideGestureCandidate
                ? GestureGeometry.shouldYieldSideTap(sideFromLeft, downX, downY, x, y,
                sideSwipeDecisionDistance, sideTapRetentionDistance)
                : squaredDistance(x, y, downX, downY) > touchSlopSquared;
        if (movedOutsideTap) {
            active = false;
            secondTap = false;
            if (sideGestureCandidate) {
                Log.i("Outside side-edge movement yielded to system gesture dx="
                        + Math.round(x - downX) + " dy=" + Math.round(y - downY));
            }
            sideGestureCandidate = false;
        }
    }

    void onUp(float x, float y, long eventTime, Runnable claimInput) {
        if (!finishTap(x, y, eventTime, claimInput)) return;
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

    void onUpImmediate(float x, float y, long eventTime,
                       Runnable claimInput, Runnable action) {
        if (!finishTap(x, y, eventTime, claimInput)) return;
        clearPending();
        secondTap = false;
        if (action != null) action.run();
    }

    void onCancel() {
        active = false;
        secondTap = false;
        sideGestureCandidate = false;
        sideFromLeft = false;
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

    private boolean finishTap(float x, float y, long eventTime, Runnable claimInput) {
        if (!active) return false;
        float allowedMovementSquared = sideGestureCandidate
                ? sideTapSlopSquared : touchSlopSquared;
        boolean valid = eventTime - downTime <= tapTimeout
                && squaredDistance(x, y, downX, downY) <= allowedMovementSquared;
        active = false;
        if (!valid) {
            secondTap = false;
            sideGestureCandidate = false;
            sideFromLeft = false;
            return false;
        }
        if (sideGestureCandidate && claimInput != null) claimInput.run();
        sideGestureCandidate = false;
        sideFromLeft = false;
        return true;
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
