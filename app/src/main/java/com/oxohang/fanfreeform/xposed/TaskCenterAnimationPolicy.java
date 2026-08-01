package com.oxohang.fanfreeform.xposed;

final class TaskCenterAnimationPolicy {
    private static final float[] MULTIPLIERS = {0.68f, 0.84f, 1f, 1.25f, 1.55f};

    private TaskCenterAnimationPolicy() { }

    static long durationMs(long baseDurationMs, int speedIndex) {
        int clamped = Math.max(0, Math.min(MULTIPLIERS.length - 1, speedIndex));
        return Math.max(1L, Math.round(baseDurationMs * MULTIPLIERS[clamped]));
    }
}
