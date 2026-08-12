package com.oxohang.fanfreeform.xposed;

final class GestureArbitrator {
    enum Decision { PENDING, FAN, CANCELLED }

    private static final float MIN_UPWARD_TO_INWARD = 0.08f;
    private static final float MIN_INWARD_TO_UPWARD = 0.25f;
    private Decision decision = Decision.PENDING;

    Decision update(GestureGeometry.Corner corner, float downX, float downY,
                    float x, float y, float decisionDistance) {
        if (decision != Decision.PENDING) return decision;
        float inward = corner == GestureGeometry.Corner.LEFT ? x - downX : downX - x;
        float upward = downY - y;
        if (Math.hypot(inward, upward) < decisionDistance) return Decision.PENDING;
        decision = corner != null && upward > 0f && inward > 0f
                && inward >= upward * MIN_INWARD_TO_UPWARD
                && upward >= inward * MIN_UPWARD_TO_INWARD
                ? Decision.FAN : Decision.CANCELLED;
        return decision;
    }

    void reset() {
        decision = Decision.PENDING;
    }
}
