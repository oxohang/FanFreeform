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
}
