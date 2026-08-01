package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class BottomTriggerHapticPolicyTest {
    @Test
    public void bothSwitchesMustBeEnabled() {
        assertTrue(BottomTriggerHapticPolicy.shouldVibrate(true, true));
        assertFalse(BottomTriggerHapticPolicy.shouldVibrate(true, false));
        assertFalse(BottomTriggerHapticPolicy.shouldVibrate(false, true));
        assertFalse(BottomTriggerHapticPolicy.shouldVibrate(false, false));
    }
}
