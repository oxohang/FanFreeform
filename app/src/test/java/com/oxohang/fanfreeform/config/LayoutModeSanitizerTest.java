package com.oxohang.fanfreeform.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class LayoutModeSanitizerTest {
    @Test
    public void removedSideIconWallFallsBackToList() {
        assertEquals(ConfigContract.SIDE_LAYOUT_LIST,
                LayoutModeSanitizer.sideLayout(6));
    }

    @Test
    public void removedTaskIconWallFallsBackToIcons() {
        assertEquals(ConfigContract.SIDE_TASK_LAYOUT_ICONS,
                LayoutModeSanitizer.taskLayout(2));
    }

    @Test
    public void invalidValuesAreClamped() {
        assertEquals(ConfigContract.SIDE_LAYOUT_LIST,
                LayoutModeSanitizer.sideLayout(-10));
        assertEquals(ConfigContract.SIDE_TASK_LAYOUT_ICONS,
                LayoutModeSanitizer.taskLayout(50));
    }
}
