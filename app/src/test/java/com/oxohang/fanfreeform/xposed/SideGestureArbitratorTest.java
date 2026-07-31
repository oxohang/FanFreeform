package com.oxohang.fanfreeform.xposed;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class SideGestureArbitratorTest {
    @Test
    public void leftLongHorizontalSwipeTriggersFanOnce() {
        SideGestureArbitrator arbitrator = new SideGestureArbitrator();
        assertEquals(SideGestureArbitrator.Decision.PENDING,
                arbitrator.update(GestureGeometry.Corner.LEFT,
                        10, 900, 160, 910, 300, 20, 48));
        assertEquals(SideGestureArbitrator.Decision.FAN,
                arbitrator.update(GestureGeometry.Corner.LEFT,
                        10, 900, 320, 915, 300, 20, 48));
        assertEquals(SideGestureArbitrator.Decision.FAN,
                arbitrator.update(GestureGeometry.Corner.LEFT,
                        10, 900, 100, 1200, 300, 20, 48));
    }

    @Test
    public void rightLongHorizontalSwipeTriggersFan() {
        SideGestureArbitrator arbitrator = new SideGestureArbitrator();
        assertEquals(SideGestureArbitrator.Decision.FAN,
                arbitrator.update(GestureGeometry.Corner.RIGHT,
                        1190, 900, 870, 880, 300, 20, 48));
    }

    @Test
    public void shortSwipeRemainsPendingForNativeBack() {
        SideGestureArbitrator arbitrator = new SideGestureArbitrator();
        assertEquals(SideGestureArbitrator.Decision.PENDING,
                arbitrator.update(GestureGeometry.Corner.LEFT,
                        10, 900, 180, 920, 300, 20, 48));
    }

    @Test
    public void verticalOrOutwardSwipeCancels() {
        SideGestureArbitrator vertical = new SideGestureArbitrator();
        assertEquals(SideGestureArbitrator.Decision.CANCELLED,
                vertical.update(GestureGeometry.Corner.LEFT,
                        10, 900, 90, 1020, 300, 20, 48));
        SideGestureArbitrator outward = new SideGestureArbitrator();
        assertEquals(SideGestureArbitrator.Decision.CANCELLED,
                outward.update(GestureGeometry.Corner.RIGHT,
                        1190, 900, 1215, 900, 300, 20, 48));
    }

    @Test
    public void directionSwitchesIndependentlyAllowHorizontalAndDiagonals() {
        SideGestureArbitrator up = new SideGestureArbitrator();
        assertEquals(SideGestureArbitrator.Decision.FAN,
                up.update(GestureGeometry.Corner.LEFT, 10, 1000, 270, 790,
                        300, 20, 48, false, true, false));
        SideGestureArbitrator blockedUp = new SideGestureArbitrator();
        assertEquals(SideGestureArbitrator.Decision.CANCELLED,
                blockedUp.update(GestureGeometry.Corner.LEFT, 10, 1000, 270, 790,
                        300, 20, 48, true, false, true));
        SideGestureArbitrator down = new SideGestureArbitrator();
        assertEquals(SideGestureArbitrator.Decision.FAN,
                down.update(GestureGeometry.Corner.RIGHT, 1190, 1000, 930, 1210,
                        300, 20, 48, false, false, true));
    }
}
