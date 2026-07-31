package com.oxohang.fanfreeform.xposed;

import com.oxohang.fanfreeform.config.ConfigContract;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class GestureOrientationConfigTest {
    @Test
    public void bottomStagesAndLaunchModesAreIndependentByOrientation() {
        GestureConfig config = GestureConfig.defaults();
        config.bottomPortraitEnabled = true;
        config.bottomLandscapeEnabled = false;
        config.bottomPortraitSecondStageEnabled = true;
        config.bottomLandscapeSecondStageEnabled = false;
        config.bottomPortraitFullscreen = false;
        config.bottomLandscapeFullscreen = true;
        config.bottomPortraitHoneycombFreeform = true;
        config.bottomLandscapeHoneycombFreeform = false;

        assertTrue(config.bottomEnabledFor(false));
        assertFalse(config.bottomEnabledFor(true));
        assertTrue(config.bottomSecondStageEnabledFor(false));
        assertFalse(config.bottomSecondStageEnabledFor(true));
        assertFalse(config.bottomFullscreenFor(false));
        assertTrue(config.bottomFullscreenFor(true));
        assertTrue(config.bottomHoneycombFreeformFor(false));
        assertFalse(config.bottomHoneycombFreeformFor(true));
    }

    @Test
    public void sideKeepsOneStageWithIndependentLayoutAndLaunchMode() {
        GestureConfig config = GestureConfig.defaults();
        config.sidePortraitLayoutMode = ConfigContract.SIDE_LAYOUT_RING;
        config.sideLandscapeLayoutMode = ConfigContract.SIDE_LAYOUT_TASKS;
        config.sidePortraitFullscreen = false;
        config.sideLandscapeFullscreen = true;

        assertEquals(ConfigContract.SIDE_LAYOUT_RING, config.sideLayoutModeFor(false));
        assertEquals(ConfigContract.SIDE_LAYOUT_TASKS, config.sideLayoutModeFor(true));
        assertFalse(config.sideFullscreenFor(false));
        assertTrue(config.sideFullscreenFor(true));
    }
}
