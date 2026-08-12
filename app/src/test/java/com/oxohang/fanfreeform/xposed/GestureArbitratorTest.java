package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GestureArbitratorTest {
    @Test
    public void waitsUntilDecisionDistance() {
        GestureArbitrator arbitrator = new GestureArbitrator();
        assertEquals(GestureArbitrator.Decision.PENDING,
                arbitrator.update(GestureGeometry.Corner.LEFT, 0, 100, 2, 94, 10));
    }

    @Test
    public void acceptsInwardDiagonalAcrossTheFanSweep() {
        GestureArbitrator arbitrator = new GestureArbitrator();
        assertEquals(GestureArbitrator.Decision.FAN,
                arbitrator.update(GestureGeometry.Corner.LEFT, 0, 100, 40, 96, 10));

        arbitrator.reset();
        assertEquals(GestureArbitrator.Decision.FAN,
                arbitrator.update(GestureGeometry.Corner.RIGHT, 100, 100, 60, 96, 10));

        arbitrator.reset();
        assertEquals(GestureArbitrator.Decision.FAN,
                arbitrator.update(GestureGeometry.Corner.LEFT, 0, 100, 5, 80, 10));

    }

    @Test
    public void rejectsHorizontalOutwardAndDownwardMotion() {
        GestureArbitrator arbitrator = new GestureArbitrator();
        assertEquals(GestureArbitrator.Decision.CANCELLED,
                arbitrator.update(GestureGeometry.Corner.LEFT, 0, 100, 20, 100, 10));

        arbitrator.reset();
        assertEquals(GestureArbitrator.Decision.CANCELLED,
                arbitrator.update(GestureGeometry.Corner.LEFT, 0, 100, -12, 92, 10));

        arbitrator.reset();
        assertEquals(GestureArbitrator.Decision.CANCELLED,
                arbitrator.update(GestureGeometry.Corner.RIGHT, 100, 100, 112, 92, 10));

        arbitrator.reset();
        assertEquals(GestureArbitrator.Decision.CANCELLED,
                arbitrator.update(GestureGeometry.Corner.LEFT, 0, 100, 8, 112, 10));

        arbitrator.reset();
        // Pure vertical swipes from the corner should stay with the system home gesture.
        assertEquals(GestureArbitrator.Decision.CANCELLED,
                arbitrator.update(GestureGeometry.Corner.RIGHT, 100, 100, 100, 80, 10));
    }

    @Test
    public void cancelledDecisionCannotTurnIntoFanLater() {
        GestureArbitrator arbitrator = new GestureArbitrator();
        assertEquals(GestureArbitrator.Decision.CANCELLED,
                arbitrator.update(GestureGeometry.Corner.LEFT, 0, 100, -12, 92, 10));
        assertEquals(GestureArbitrator.Decision.CANCELLED,
                arbitrator.update(GestureGeometry.Corner.LEFT, 0, 100, 22, 40, 10));
    }

    @Test
    public void resetAllowsNextGestureToBeClaimed() {
        GestureArbitrator arbitrator = new GestureArbitrator();
        assertEquals(GestureArbitrator.Decision.CANCELLED,
                arbitrator.update(GestureGeometry.Corner.LEFT, 0, 100, -12, 92, 10));
        arbitrator.reset();
        assertEquals(GestureArbitrator.Decision.FAN,
                arbitrator.update(GestureGeometry.Corner.LEFT, 0, 100, 20, 94, 10));
    }
}
