package com.oxohang.fanfreeform.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class WindowSizePolicyTest {
    @Test
    public void oneHundredPercentPreservesArbitraryNativeDimensions() {
        int[] pixels = WindowSizePolicy.scaleNativeLogicalPixels(
                913, 1379, 2000, 2000, 0.7f, 100);
        assertEquals(913, pixels[0]);
        assertEquals(1379, pixels[1]);
    }

    @Test
    public void scalesNativeWidthAndHeightByTheSameFactor() {
        int[] pixels = WindowSizePolicy.scaleNativeLogicalPixels(
                750, 1130, 2000, 2000, 0.7f, 120);
        assertEquals(900, pixels[0]);
        assertEquals(1356, pixels[1]);
    }

    @Test
    public void fitsOversizedNativeWindowWithoutChangingItsRatio() {
        int[] pixels = WindowSizePolicy.scaleNativeLogicalPixels(
                1200, 1800, 700, 900, 0.7f, 150);
        assertEquals(857, pixels[0]);
        assertEquals(1286, pixels[1]);
        assertEquals(2f / 3f, pixels[0] / (float) pixels[1], 0.001f);
    }

    @Test
    public void doesNotImposeOneRatioAcrossDifferentNativeApps() {
        int[] square = WindowSizePolicy.scaleNativeLogicalPixels(
                1000, 1000, 2000, 2000, 0.7f, 100);
        int[] wide = WindowSizePolicy.scaleNativeLogicalPixels(
                1400, 800, 2000, 2000, 0.7f, 100);
        assertEquals(1f, square[0] / (float) square[1], 0.001f);
        assertEquals(1.75f, wide[0] / (float) wide[1], 0.001f);
    }
}
