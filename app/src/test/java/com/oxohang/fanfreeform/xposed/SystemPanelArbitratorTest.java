package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SystemPanelArbitratorTest {
    @Test
    public void ignoresNotificationDrivenExpansionWithoutTopTouch() {
        assertFalse(SystemPanelArbitrator.acceptShadeExpansion(false, 2000L, 0L));
    }

    @Test
    public void acceptsExpansionAfterRealTopTouchAndDuringActivePanel() {
        assertTrue(SystemPanelArbitrator.acceptShadeExpansion(false, 2000L, 2500L));
        assertTrue(SystemPanelArbitrator.acceptShadeExpansion(true, 3000L, 0L));
    }
}
