package com.oxohang.fanfreeform.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ConfigContractTest {
    @Test
    public void keepsSideCancellationRangeStable() {
        assertEquals(12, ConfigContract.DEFAULT_SIDE_REVERSE_CANCEL_PERCENT);
        assertEquals(5, ConfigContract.MIN_SIDE_REVERSE_CANCEL_PERCENT);
        assertEquals(25, ConfigContract.MAX_SIDE_REVERSE_CANCEL_PERCENT);
    }

    @Test
    public void doublesFanAnimationAdjustmentCeilings() {
        assertEquals(130, ConfigContract.MAX_FAN_REVEAL_AMOUNT);
        assertEquals(90, ConfigContract.MAX_FAN_ROTATION_DEGREES);
        assertEquals(40, ConfigContract.MAX_FAN_SELECTION_SCALE_PERCENT);
    }

    @Test
    public void keepsRingSizingRangeAndUnifiedNameDefaultStable() {
        assertEquals(60, ConfigContract.MIN_SIDE_RING_SIZE_PERCENT);
        assertEquals(100, ConfigContract.DEFAULT_SIDE_RING_SIZE_PERCENT);
        assertEquals(180, ConfigContract.MAX_SIDE_RING_SIZE_PERCENT);
        assertEquals(ConfigContract.DEFAULT_SHOW_SELECTED_APP_NAME,
                ConfigContract.DEFAULT_SIDE_SHOW_APP_NAMES);
    }
}
