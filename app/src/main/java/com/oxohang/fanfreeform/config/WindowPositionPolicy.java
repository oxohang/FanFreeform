package com.oxohang.fanfreeform.config;

public final class WindowPositionPolicy {
    private WindowPositionPolicy() { }

    public static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public static String percentageLabel(int value) {
        return clampPercent(value) + "%";
    }
}
