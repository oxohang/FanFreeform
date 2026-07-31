package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class FreeformOrientationPolicyTest {
    @Test
    public void portraitDisplayPreservesForcedLandscapeGameRestore() {
        assertTrue(FreeformOrientationPolicy.shouldPreserveNativeLandscapeRestore(
                1200, 2608, 6, 1038, 515, 1626, 2583, false));
    }

    @Test
    public void portraitDisplayKeepsCustomRestoreForPortraitApp() {
        assertFalse(FreeformOrientationPolicy.shouldPreserveNativeLandscapeRestore(
                1200, 2608, 1, 596, 1200, 1200, 1920, false));
    }

    @Test
    public void physicalLandscapeStillUsesLandscapeCustomSettings() {
        assertFalse(FreeformOrientationPolicy.shouldPreserveNativeLandscapeRestore(
                2608, 1200, 6, 1038, 515, 1038, 515, false));
    }

    @Test
    public void appForcedLandscapeWhileRotationLockedPreservesNativeRestore() {
        assertTrue(FreeformOrientationPolicy.shouldPreserveNativeLandscapeRestore(
                2608, 1200, 6, 1038, 515, 1626, 2583, true));
    }

    @Test
    public void wideNativeRestoreIsEnoughWhenOrientationIsFlexible() {
        assertTrue(FreeformOrientationPolicy.shouldPreserveNativeLandscapeRestore(
                1200, 2608, -1, 1038, 515, 1038, 515, false));
    }

    @Test
    public void lockedPortraitUsesNativeLaunchForFixedLandscapeActivity() {
        assertTrue(FreeformOrientationPolicy.shouldUseNativeLandscapeLaunch(true, 6));
    }

    @Test
    public void autoRotatedLandscapeKeepsConfiguredLandscapeLaunch() {
        assertFalse(FreeformOrientationPolicy.shouldUseNativeLandscapeLaunch(false, 6));
    }

    @Test
    public void lockedPortraitKeepsConfiguredLaunchForPortraitActivity() {
        assertFalse(FreeformOrientationPolicy.shouldUseNativeLandscapeLaunch(true, 1));
    }
}
