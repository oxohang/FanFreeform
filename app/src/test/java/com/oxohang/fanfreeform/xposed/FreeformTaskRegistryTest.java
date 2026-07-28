package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class FreeformTaskRegistryTest {
    @Test
    public void keepsMultipleSuspendedTasksOwned() {
        FreeformTaskRegistry registry = new FreeformTaskRegistry();
        registry.put(10, "app.a", FreeformTaskRegistry.State.SUSPENDED);
        registry.put(11, "app.b", FreeformTaskRegistry.State.SUSPENDED);

        assertEquals(2, registry.size());
        assertTrue(registry.contains(10));
        assertTrue(registry.contains(11));
    }

    @Test
    public void removingTaskClearsOwnership() {
        FreeformTaskRegistry registry = new FreeformTaskRegistry();
        registry.put(10, "app.a", FreeformTaskRegistry.State.NORMAL);
        registry.remove(10);
        assertFalse(registry.contains(10));
    }
}
