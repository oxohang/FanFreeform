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
    public void sideTriggerCanUseTheWholeScreenWidth() {
        assertEquals(20, ConfigContract.MIN_SIDE_TRIGGER_PERCENT);
        assertEquals(100, ConfigContract.MAX_SIDE_TRIGGER_PERCENT);
        assertEquals(true, ConfigContract.DEFAULT_SIDE_HOLD_ENABLED);
        assertEquals(150, ConfigContract.MIN_SIDE_HOLD_DELAY_MS);
        assertEquals(360, ConfigContract.DEFAULT_SIDE_HOLD_DELAY_MS);
        assertEquals(1200, ConfigContract.MAX_SIDE_HOLD_DELAY_MS);
    }

    @Test
    public void taskSwitcherIsAnIndependentSideLayout() {
        assertEquals(4, ConfigContract.SIDE_LAYOUT_TASKS);
        assertEquals(5, ConfigContract.SIDE_LAYOUT_SYSTEM_RECENTS);
        assertEquals(2, ConfigContract.MIN_SIDE_TASK_MAX_COUNT);
        assertEquals(6, ConfigContract.DEFAULT_SIDE_TASK_MAX_COUNT);
        assertEquals(10, ConfigContract.MAX_SIDE_TASK_MAX_COUNT);
        assertEquals(100, ConfigContract.MIN_SIDE_TASK_SWIPE_SPEED_PERCENT);
        assertEquals(180, ConfigContract.DEFAULT_SIDE_TASK_SWIPE_SPEED_PERCENT);
        assertEquals(300, ConfigContract.MAX_SIDE_TASK_SWIPE_SPEED_PERCENT);
        assertEquals(true, ConfigContract.DEFAULT_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE);
        assertEquals(0, ConfigContract.SIDE_TASK_LAYOUT_FLAT);
        assertEquals(1, ConfigContract.SIDE_TASK_LAYOUT_ICONS);
        assertEquals(32, ConfigContract.MIN_SIDE_TASK_ICON_SIZE_DP);
        assertEquals(64, ConfigContract.DEFAULT_SIDE_TASK_ICON_SIZE_DP);
        assertEquals(120, ConfigContract.MAX_SIDE_TASK_ICON_SIZE_DP);
        assertEquals(0, ConfigContract.MIN_SIDE_TASK_ICON_GAP_DP);
        assertEquals(16, ConfigContract.DEFAULT_SIDE_TASK_ICON_GAP_DP);
        assertEquals(80, ConfigContract.MAX_SIDE_TASK_ICON_GAP_DP);
        assertEquals(false, ConfigContract.DEFAULT_HIDE_SYSTEM_RECENTS_CLEAR);
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
        assertEquals(false, ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_ENABLED);
        assertEquals(true, ConfigContract.DEFAULT_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED);
        assertEquals(false, ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED);
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
        assertEquals(false, ConfigContract.DEFAULT_BOTTOM_FULLSCREEN);
        assertEquals(false, ConfigContract.DEFAULT_SIDE_FULLSCREEN);
        assertEquals(false, ConfigContract.DEFAULT_SIDE_PORTRAIT_FULLSCREEN);
        assertEquals(false, ConfigContract.DEFAULT_SIDE_LANDSCAPE_FULLSCREEN);
        assertEquals(true, ConfigContract.DEFAULT_SIDE_ANIMATIONS_ENABLED);
    }

    @Test
    public void interactionDefaultsMatchTheReferencePhone() {
        assertEquals(17, ConfigContract.DEFAULT_TRIGGER_PERCENT);
        assertEquals(75, ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT);
        assertEquals(37, ConfigContract.DEFAULT_ICON_SIZE_DP);
        assertEquals(85, ConfigContract.DEFAULT_WIDTH_PERCENT);
        assertEquals(65, ConfigContract.DEFAULT_HEIGHT_PERCENT);
        assertEquals(180, ConfigContract.DEFAULT_OUTSIDE_TAP_WINDOW_MS);
        assertEquals(true, ConfigContract.DEFAULT_OUTSIDE_ENABLED);
        assertEquals(true, ConfigContract.DEFAULT_CUSTOM_WINDOW_PORTRAIT_ENABLED);
        assertEquals(false, ConfigContract.DEFAULT_CUSTOM_WINDOW_LANDSCAPE_ENABLED);
        assertEquals(true, ConfigContract.DEFAULT_PORTRAIT_PROPORTIONAL_SIZE_ENABLED);
        assertEquals(true, ConfigContract.DEFAULT_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED);
        assertEquals(50, ConfigContract.MIN_NATIVE_WINDOW_SCALE_PERCENT);
        assertEquals(100, ConfigContract.DEFAULT_NATIVE_WINDOW_SCALE_PERCENT);
        assertEquals(150, ConfigContract.MAX_NATIVE_WINDOW_SCALE_PERCENT);
        assertEquals(true, ConfigContract.DEFAULT_OUTSIDE_PORTRAIT_ENABLED);
        assertEquals(false, ConfigContract.DEFAULT_OUTSIDE_LANDSCAPE_ENABLED);
        assertEquals(ConfigContract.ACTION_EDGE_PIN,
                ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION);
        assertEquals(true, ConfigContract.DEFAULT_HONEYCOMB_ENABLED);
        assertEquals(ConfigContract.HONEYCOMB_MODE_HOLD,
                ConfigContract.DEFAULT_HONEYCOMB_MODE);
    }

    @Test
    public void fanSelectionDistanceCanStartAtOnePercent() {
        assertEquals(1, ConfigContract.MIN_SELECTION_RADIUS_PERCENT);
        assertEquals(150, ConfigContract.MAX_SELECTION_RADIUS_PERCENT);
    }

    @Test
    public void edgePinIsASeparateOutsideAction() {
        assertEquals(4, ConfigContract.ACTION_EDGE_PIN);
    }

    @Test
    public void circularIconsRemainTheCompatibilityDefault() {
        assertEquals(true, ConfigContract.DEFAULT_FORCE_CIRCULAR_ICONS);
    }
}
