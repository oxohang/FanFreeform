package com.oxohang.fanfreeform.config;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PressureGesturePolicyTest {
    @Test public void circleUsesShortEdgeAndConfiguredCenter() {
        assertTrue(PressureGesturePolicy.isInsideCircle(
                500f, 500f, 1000f, 2000f, 50, 25, 10));
        assertFalse(PressureGesturePolicy.isInsideCircle(
                700f, 500f, 1000f, 2000f, 50, 25, 10));
    }

    @Test public void lowerPressureIsIgnored() {
        assertEquals(0f, PressureGesturePolicy.positiveDelta(1013.2f, 1012.8f), 0f);
        assertEquals(0.7f, PressureGesturePolicy.positiveDelta(1013.2f, 1013.9f), 0.0001f);
    }

    @Test public void calibrationDropsLowerAndInvalidSamples() {
        PressureGesturePolicy.CalibrationResult result = PressureGesturePolicy.calibrate(
                Arrays.asList(0.9f, -0.4f, Float.NaN, 1.8f, 1.2f, 1.5f));
        assertTrue(result.valid);
        assertEquals(4, result.validCount);
        // Sorted valid values: 0.9, 1.2, 1.5, 1.8; median is 1.35.
        assertEquals(1.125f, result.threshold, 0.0001f);
    }

    @Test public void calibrationRequiresThreeRisingSamples() {
        PressureGesturePolicy.CalibrationResult result = PressureGesturePolicy.calibrate(
                Collections.singletonList(0.8f));
        assertFalse(result.valid);
        assertEquals(1, result.validCount);
    }

}
