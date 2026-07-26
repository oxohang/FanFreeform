package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GestureReplayGuardTest {
    @Test
    public void ignoresRepeatedDownForCurrentGesture() {
        GestureReplayGuard guard = new GestureReplayGuard();
        guard.begin(1000, 1000);
        guard.record(1000, 1050);
        assertTrue(guard.isRepeatedDown(1000));
        assertFalse(guard.isRepeatedDown(2000));
    }

    @Test
    public void ignoresOnlyTerminalsThatMoveBackwardsInTime() {
        GestureReplayGuard guard = new GestureReplayGuard();
        guard.begin(1000, 1000);
        guard.record(1000, 1050);
        assertTrue(guard.isStaleTerminal(1000, 1000));
        assertFalse(guard.isStaleTerminal(1000, 1050));
        assertFalse(guard.isStaleTerminal(1000, 1100));
        assertFalse(guard.isStaleTerminal(2000, 1000));
    }

    @Test
    public void recordAdvancesOnlyTheCurrentGestureClock() {
        GestureReplayGuard guard = new GestureReplayGuard();
        guard.begin(1000, 1000);
        guard.record(2000, 1200);
        assertFalse(guard.isStaleTerminal(1000, 1100));
        guard.record(1000, 1200);
        assertTrue(guard.isStaleTerminal(1000, 1100));
    }

    @Test
    public void resetAllowsTheSameTimestampToStartFresh() {
        GestureReplayGuard guard = new GestureReplayGuard();
        guard.begin(1000, 1000);
        guard.reset();
        assertFalse(guard.isRepeatedDown(1000));
        assertFalse(guard.isStaleTerminal(1000, 900));
    }
}
