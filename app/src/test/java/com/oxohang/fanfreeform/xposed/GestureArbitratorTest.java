package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GestureArbitratorTest {
    @Test
    public void waitsUntilDecisionDistance() {
        GestureArbitrator arbitrator = new GestureArbitrator();
        assertEquals(GestureArbitrator.Decision.PENDING,
                arbitrator.update(0, 100, 2, 94, 10));
    }

    @Test
    public void claimsOnlyClearlyUpwardMotion() {
        GestureArbitrator arbitrator = new GestureArbitrator();
        assertEquals(GestureArbitrator.Decision.FAN,
                arbitrator.update(0, 100, 6, 80, 10));

        arbitrator.reset();
        assertEquals(GestureArbitrator.Decision.SYSTEM,
                arbitrator.update(0, 100, 20, 94, 10));

        arbitrator.reset();
        assertEquals(GestureArbitrator.Decision.SYSTEM,
                arbitrator.update(0, 100, 10, 90, 10));

        arbitrator.reset();
        assertEquals(GestureArbitrator.Decision.SYSTEM,
                arbitrator.update(0, 100, 0, 112, 10));
    }

    @Test
    public void systemDecisionCannotTurnIntoFanLater() {
        GestureArbitrator arbitrator = new GestureArbitrator();
        assertEquals(GestureArbitrator.Decision.SYSTEM,
                arbitrator.update(0, 100, 20, 96, 10));
        assertEquals(GestureArbitrator.Decision.SYSTEM,
                arbitrator.update(0, 100, 22, 40, 10));
    }

    @Test
    public void resetAllowsNextGestureToBeClaimed() {
        GestureArbitrator arbitrator = new GestureArbitrator();
        assertEquals(GestureArbitrator.Decision.SYSTEM,
                arbitrator.update(0, 100, 20, 96, 10));
        arbitrator.reset();
        assertEquals(GestureArbitrator.Decision.FAN,
                arbitrator.update(0, 100, 4, 80, 10));
    }
}
