package com.oxohang.fanfreeform.xposed;

final class TaskSelectionPolicy {
    private TaskSelectionPolicy() { }

    static float advancePosition(float currentPosition, float inwardDelta,
                                 float step, int taskCount) {
        if (taskCount <= 1) return 0f;
        float safeStep = Math.max(1f, step);
        float linearPosition = inverseAcceleratedPosition(Math.max(0f, currentPosition));
        float nextLinearPosition = Math.max(0f, linearPosition + inwardDelta / safeStep);
        return clamp(acceleratedPosition(nextLinearPosition),
                0f, taskCount - 1f);
    }

    static int nearestIndex(float displayPosition, int taskCount, boolean selectionActive) {
        if (!selectionActive || taskCount <= 0) return -1;
        return Math.max(0, Math.min(taskCount - 1, Math.round(displayPosition)));
    }

    static float downwardDismissDistance(float baseDistance, boolean extended) {
        float safeDistance = Math.max(1f, baseDistance);
        return extended ? safeDistance * 2f : safeDistance;
    }

    private static float acceleratedPosition(float linearPosition) {
        if (linearPosition <= 1.5f) return linearPosition;
        return 1.5f + (linearPosition - 1.5f) * 1.80f;
    }

    private static float inverseAcceleratedPosition(float acceleratedPosition) {
        if (acceleratedPosition <= 1.5f) return acceleratedPosition;
        return 1.5f + (acceleratedPosition - 1.5f) / 1.80f;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
