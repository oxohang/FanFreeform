package com.oxohang.fanfreeform.config;

public final class FanRowAllocation {
    private FanRowAllocation() { }

    public static int[] normalize(int total, int inner, int middle) {
        int safeTotal = Math.max(0, total);
        int resolvedInner = clamp(inner, minimumInner(safeTotal), maximumInner(safeTotal));
        int remaining = safeTotal - resolvedInner;
        int resolvedMiddle = clamp(middle, minimumMiddle(remaining),
                maximumMiddle(remaining));
        return new int[] {resolvedInner, resolvedMiddle, remaining - resolvedMiddle};
    }

    public static int[] afterInnerChanged(int total, int inner) {
        int safeTotal = Math.max(0, total);
        int resolvedInner = clamp(inner, minimumInner(safeTotal), maximumInner(safeTotal));
        int remaining = safeTotal - resolvedInner;
        int resolvedMiddle = maximumMiddle(remaining);
        return new int[] {resolvedInner, resolvedMiddle, remaining - resolvedMiddle};
    }

    public static int[] afterMiddleChanged(int total, int inner, int middle) {
        return normalize(total, inner, middle);
    }

    public static int[] afterOuterChanged(int total, int inner, int outer) {
        int safeTotal = Math.max(0, total);
        int resolvedInner = clamp(inner, minimumInner(safeTotal), maximumInner(safeTotal));
        int remaining = safeTotal - resolvedInner;
        int resolvedOuter = clamp(outer, minimumOuter(remaining), maximumOuter(remaining));
        return new int[] {resolvedInner, remaining - resolvedOuter, resolvedOuter};
    }

    public static int minimumInner(int total) {
        return Math.max(0, Math.max(0, total)
                - ConfigContract.MAX_FAN_CUSTOM_MIDDLE_COUNT
                - ConfigContract.MAX_FAN_CUSTOM_OUTER_COUNT);
    }

    public static int maximumInner(int total) {
        return Math.min(Math.max(0, total), ConfigContract.MAX_FAN_CUSTOM_INNER_COUNT);
    }

    public static int minimumMiddle(int remaining) {
        return Math.max(0, Math.max(0, remaining)
                - ConfigContract.MAX_FAN_CUSTOM_OUTER_COUNT);
    }

    public static int maximumMiddle(int remaining) {
        return Math.min(Math.max(0, remaining),
                ConfigContract.MAX_FAN_CUSTOM_MIDDLE_COUNT);
    }

    public static int minimumOuter(int remaining) {
        return Math.max(0, Math.max(0, remaining)
                - ConfigContract.MAX_FAN_CUSTOM_MIDDLE_COUNT);
    }

    public static int maximumOuter(int remaining) {
        return Math.min(Math.max(0, remaining), ConfigContract.MAX_FAN_CUSTOM_OUTER_COUNT);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
