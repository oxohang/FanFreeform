package com.oxohang.fanfreeform.config;

final class LayoutModeSanitizer {
    private static final int LEGACY_SIDE_LAYOUT_ICON_WALL = 6;
    private static final int LEGACY_SIDE_TASK_LAYOUT_ICON_WALL = 2;

    private LayoutModeSanitizer() { }

    static int sideLayout(int value) {
        if (value == LEGACY_SIDE_LAYOUT_ICON_WALL) return ConfigContract.SIDE_LAYOUT_LIST;
        return clamp(value, ConfigContract.SIDE_LAYOUT_LIST,
                ConfigContract.SIDE_LAYOUT_SYSTEM_RECENTS);
    }

    static int taskLayout(int value) {
        if (value == LEGACY_SIDE_TASK_LAYOUT_ICON_WALL) {
            return ConfigContract.SIDE_TASK_LAYOUT_ICONS;
        }
        return clamp(value, ConfigContract.SIDE_TASK_LAYOUT_FLAT,
                ConfigContract.SIDE_TASK_LAYOUT_ICONS);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
