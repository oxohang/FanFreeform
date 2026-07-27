package com.oxohang.fanfreeform.xposed;

final class SideGestureArbitrator {
    enum Decision { PENDING, FAN, CANCELLED }

    private static final float MAX_VERTICAL_TO_INWARD = 0.55f;
    private Decision decision = Decision.PENDING;

    Decision update(GestureGeometry.Corner side, float downX, float downY,
                    float x, float y, float triggerDistance,
                    float directionSlop, float verticalFloor) {
        if (decision != Decision.PENDING) return decision;
        if (side == null) return decision = Decision.CANCELLED;
        float inward = side == GestureGeometry.Corner.LEFT ? x - downX : downX - x;
        float vertical = Math.abs(y - downY);
        if (inward < -directionSlop) return decision = Decision.CANCELLED;
        if (Math.hypot(inward, vertical) >= directionSlop
                && vertical > Math.max(verticalFloor, inward * MAX_VERTICAL_TO_INWARD)) {
            return decision = Decision.CANCELLED;
        }
        if (inward >= triggerDistance) return decision = Decision.FAN;
        return decision;
    }

    void reset() {
        decision = Decision.PENDING;
    }
}
