package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class HoneycombGestureStateTest {
    @Test public void returnsOnlyAtTheOriginalFanActivationPosition() {
        HoneycombGestureState state = new HoneycombGestureState();
        assertTrue(state.shouldEnter(520f, 500f));
        assertFalse(state.shouldExit(171f, 170f));
        assertTrue(state.shouldExit(170f, 170f));
        assertFalse(state.shouldExit(120f, 170f));
        assertTrue(state.shouldEnter(520f, 500f));
    }

    @Test public void resetAllowsFreshEntry() {
        HoneycombGestureState state = new HoneycombGestureState();
        assertTrue(state.shouldEnter(200f, 170f));
        state.reset();
        assertTrue(state.shouldEnter(200f, 170f));
    }

    @Test public void sideHoneycombWithoutFanReturnThresholdNeverSwitchesToFan() {
        HoneycombGestureState state = new HoneycombGestureState();
        assertTrue(state.shouldEnter(520f, 500f));
        assertFalse(state.shouldExit(0f, 0f));
    }
}
