package com.oxohang.fanfreeform.xposed;

final class GestureArbitrator {
    enum Decision { PENDING, FAN, SYSTEM }

    private static final float UPWARD_DOMINANCE = 1.15f;
    private Decision decision = Decision.PENDING;

    Decision update(float downX, float downY, float x, float y, float decisionDistance) {
        if (decision != Decision.PENDING) return decision;
        float horizontal = Math.abs(x - downX);
        float upward = downY - y;
        if (Math.hypot(horizontal, upward) < decisionDistance) return Decision.PENDING;
        decision = upward > 0f && upward >= horizontal * UPWARD_DOMINANCE
                ? Decision.FAN : Decision.SYSTEM;
        return decision;
    }

    void reset() {
        decision = Decision.PENDING;
    }
}
