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
        assertEquals(97, ConfigContract.DEFAULT_SIDE_RING_SIZE_PERCENT);
        assertEquals(180, ConfigContract.MAX_SIDE_RING_SIZE_PERCENT);
        assertEquals(ConfigContract.DEFAULT_SHOW_SELECTED_APP_NAME,
                ConfigContract.DEFAULT_SIDE_SHOW_APP_NAMES);
        assertEquals(false, ConfigContract.DEFAULT_SIDE_FOLLOW_HONEYCOMB);
    }

    @Test
    public void keepsHoneycombDiscSizingWithinTheScreen() {
        assertEquals(50, ConfigContract.MIN_HONEYCOMB_DISC_SIZE_PERCENT);
        assertEquals(60, ConfigContract.DEFAULT_HONEYCOMB_DISC_SIZE_PERCENT);
        assertEquals(100, ConfigContract.MAX_HONEYCOMB_DISC_SIZE_PERCENT);
        assertEquals(true, ConfigContract.DEFAULT_HONEYCOMB_SHOW_SELECTED_NAME);
    }

    @Test
    public void orientationDefaultsMatchTheReferencePhone() {
        assertEquals(true, ConfigContract.DEFAULT_BOTTOM_PORTRAIT_ENABLED);
        assertEquals(true, ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_ENABLED);
        assertEquals(true, ConfigContract.DEFAULT_SIDE_PORTRAIT_ENABLED);
        assertEquals(false, ConfigContract.DEFAULT_SIDE_LANDSCAPE_ENABLED);
        assertEquals(false, ConfigContract.DEFAULT_HONEYCOMB_LANDSCAPE_ENABLED);
        assertEquals(false, ConfigContract.DEFAULT_FAN_FIXED_SEVEN_ROWS);
    }

    @Test
    public void landscapeWindowAndFixedHoneycombDefaultsMatchTheDesign() {
        assertEquals(20, ConfigContract.DEFAULT_LANDSCAPE_WIDTH_PERCENT);
        assertEquals(20, ConfigContract.MIN_LANDSCAPE_WIDTH_PERCENT);
        assertEquals(90, ConfigContract.MAX_LANDSCAPE_WIDTH_PERCENT);
        assertEquals(85, ConfigContract.DEFAULT_LANDSCAPE_HEIGHT_PERCENT);
        assertEquals(0, ConfigContract.DEFAULT_LANDSCAPE_POSITION_X);
        assertEquals(50, ConfigContract.DEFAULT_LANDSCAPE_POSITION_Y);
        assertEquals(50, ConfigContract.DEFAULT_HONEYCOMB_FIXED_X_PERCENT);
        assertEquals(60, ConfigContract.DEFAULT_HONEYCOMB_FIXED_Y_PERCENT);
        assertEquals(true, ConfigContract.DEFAULT_BOTTOM_HONEYCOMB_FREEFORM);
    }

    @Test
    public void interactionDefaultsMatchTheReferencePhone() {
        assertEquals(17, ConfigContract.DEFAULT_TRIGGER_PERCENT);
        assertEquals(75, ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT);
        assertEquals(37, ConfigContract.DEFAULT_ICON_SIZE_DP);
        assertEquals(85, ConfigContract.DEFAULT_WIDTH_PERCENT);
        assertEquals(65, ConfigContract.DEFAULT_HEIGHT_PERCENT);
        assertEquals(180, ConfigContract.DEFAULT_OUTSIDE_TAP_WINDOW_MS);
        assertEquals(ConfigContract.ACTION_EDGE_PIN,
                ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION);
        assertEquals(true, ConfigContract.DEFAULT_HONEYCOMB_ENABLED);
        assertEquals(ConfigContract.HONEYCOMB_MODE_HOLD,
                ConfigContract.DEFAULT_HONEYCOMB_MODE);
    }

    @Test
    public void fanSelectionDistanceCanStartAtOnePercent() {
        assertEquals(1, ConfigContract.MIN_SELECTION_RADIUS_PERCENT);
        assertEquals(75, ConfigContract.MAX_SELECTION_RADIUS_PERCENT);
    }

    @Test
    public void edgePinIsASeparateOutsideAction() {
        assertEquals(4, ConfigContract.ACTION_EDGE_PIN);
    }
}
