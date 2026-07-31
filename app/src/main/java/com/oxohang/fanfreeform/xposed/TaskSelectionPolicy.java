package com.oxohang.fanfreeform.xposed;

final class TaskSelectionPolicy {
    private TaskSelectionPolicy() { }

    static int nearestIndex(float displayPosition, int taskCount, boolean selectionActive) {
        if (!selectionActive || taskCount <= 0) return -1;
        return Math.max(0, Math.min(taskCount - 1, Math.round(displayPosition)));
    }

    static float downwardDismissDistance(float baseDistance, boolean extended) {
        float safeDistance = Math.max(1f, baseDistance);
        return extended ? safeDistance * 2f : safeDistance;
    }
}
