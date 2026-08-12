package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class BroadcastSenderValidatorTest {
    @Test
    public void acceptsOnlyTheExpectedPackage() {
        assertTrue(BroadcastSenderValidator.containsPackage(
                "com.android.systemui", new String[]{"com.android.systemui"}));
        assertFalse(BroadcastSenderValidator.containsPackage(
                "com.android.systemui", new String[]{"com.example.other"}));
    }

    @Test
    public void rejectsMissingOrNullPackageLists() {
        assertFalse(BroadcastSenderValidator.containsPackage(
                "com.android.systemui", null));
        assertFalse(BroadcastSenderValidator.containsPackage(
                "com.android.systemui", new String[0]));
    }
}
