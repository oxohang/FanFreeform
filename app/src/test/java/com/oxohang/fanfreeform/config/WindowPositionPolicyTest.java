package com.oxohang.fanfreeform.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class WindowPositionPolicyTest {
    @Test
    public void everyValidPercentageRemainsExact() {
        for (int value = 0; value <= 100; value++) {
            assertEquals(value, WindowPositionPolicy.clampPercent(value));
            assertEquals(value + "%", WindowPositionPolicy.percentageLabel(value));
        }
    }

    @Test
    public void abnormalValuesClampToSafeEndpoints() {
        assertEquals(0, WindowPositionPolicy.clampPercent(-25));
        assertEquals(100, WindowPositionPolicy.clampPercent(135));
        assertEquals("0%", WindowPositionPolicy.percentageLabel(-1));
        assertEquals("100%", WindowPositionPolicy.percentageLabel(101));
    }
}
