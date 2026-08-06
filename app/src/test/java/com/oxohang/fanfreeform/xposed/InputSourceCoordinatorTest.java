package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class InputSourceCoordinatorTest {
    @Test
    public void cornerFallbackIsTheInitialInputSource() {
        InputSourceCoordinator coordinator = new InputSourceCoordinator();

        assertEquals(InputSourceCoordinator.Mode.CORNER_FALLBACK, coordinator.mode());
        assertTrue(coordinator.shouldDispatchCornerFallback());
        assertFalse(coordinator.usesNativeInput());
    }

    @Test
    public void nativeRegistrationPausesFallbackBeforeTakingOver() {
        InputSourceCoordinator coordinator = new InputSourceCoordinator();

        assertTrue(coordinator.beginNativeRegistration());
        assertEquals(InputSourceCoordinator.Mode.SWITCHING_TO_NATIVE, coordinator.mode());
        assertFalse(coordinator.shouldDispatchCornerFallback());

        coordinator.completeNativeRegistration();

        assertEquals(InputSourceCoordinator.Mode.NATIVE_GLOBAL, coordinator.mode());
        assertFalse(coordinator.shouldDispatchCornerFallback());
        assertTrue(coordinator.usesNativeInput());
    }

    @Test
    public void failedNativeRegistrationRestoresFallback() {
        InputSourceCoordinator coordinator = new InputSourceCoordinator();

        coordinator.beginNativeRegistration();
        coordinator.failNativeRegistration();

        assertEquals(InputSourceCoordinator.Mode.CORNER_FALLBACK, coordinator.mode());
        assertTrue(coordinator.shouldDispatchCornerFallback());
        assertFalse(coordinator.usesNativeInput());
    }

    @Test
    public void repeatedNativeRegistrationDoesNotReenableFallback() {
        InputSourceCoordinator coordinator = new InputSourceCoordinator();
        coordinator.beginNativeRegistration();
        coordinator.completeNativeRegistration();

        assertFalse(coordinator.beginNativeRegistration());
        coordinator.failNativeRegistration();

        assertEquals(InputSourceCoordinator.Mode.NATIVE_GLOBAL, coordinator.mode());
        assertFalse(coordinator.shouldDispatchCornerFallback());
    }
}
