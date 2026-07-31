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

    Decision update(GestureGeometry.Corner side, float downX, float downY,
                    float x, float y, float triggerDistance,
                    float directionSlop, float verticalFloor,
                    boolean horizontalEnabled, boolean upEnabled, boolean downEnabled) {
        if (decision != Decision.PENDING) return decision;
        if (side == null) return decision = Decision.CANCELLED;
        float inward = side == GestureGeometry.Corner.LEFT ? x - downX : downX - x;
        float signedVertical = y - downY;
        float vertical = Math.abs(signedVertical);
        if (inward < -directionSlop) return decision = Decision.CANCELLED;
        float distance = (float) Math.hypot(Math.max(0f, inward), vertical);
        if (distance >= directionSlop) {
            float angle = (float) Math.toDegrees(Math.atan2(vertical,
                    Math.max(1f, inward)));
            if (angle > 65f) return decision = Decision.CANCELLED;
            boolean allowed = angle <= 25f ? horizontalEnabled
                    : signedVertical < 0f ? upEnabled : downEnabled;
            if (!allowed) return decision = Decision.CANCELLED;
        }
        if (vertical > Math.max(verticalFloor * 3f, Math.max(1f, inward) * 2.15f)) {
            return decision = Decision.CANCELLED;
        }
        if (distance >= triggerDistance && inward > directionSlop) return decision = Decision.FAN;
        return decision;
    }

    void reset() {
        decision = Decision.PENDING;
    }
}
