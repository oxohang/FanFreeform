package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GestureGeometryTest {
    @Test
    public void detectsOnlyBottomCorners() {
        assertEquals(GestureGeometry.Corner.LEFT, GestureGeometry.cornerAt(20, 2580, 1200, 2608, 180));
        assertEquals(GestureGeometry.Corner.RIGHT, GestureGeometry.cornerAt(1180, 2580, 1200, 2608, 180));
        assertNull(GestureGeometry.cornerAt(600, 2580, 1200, 2608, 180));
        assertNull(GestureGeometry.cornerAt(20, 2100, 1200, 2608, 180));
    }

    @Test
    public void requiresUpwardAndInwardMotion() {
        assertTrue(GestureGeometry.movesInward(GestureGeometry.Corner.LEFT, 10, 2590, 220, 2300));
        assertTrue(GestureGeometry.movesInward(GestureGeometry.Corner.RIGHT, 1190, 2590, 980, 2300));
    }

    @Test
    public void mirrorsSelectionAcrossCorners() {
        int left = GestureGeometry.selection(GestureGeometry.Corner.LEFT, 480, 2050, 1200, 2608, 6, 100);
        int right = GestureGeometry.selection(GestureGeometry.Corner.RIGHT, 720, 2050, 1200, 2608, 6, 100);
        assertEquals(left, right);
    }

    @Test
    public void rejectsPointsBeforeSelectionRadius() {
        assertEquals(-1, GestureGeometry.selection(GestureGeometry.Corner.LEFT, 30, 2580, 1200, 2608, 6, 100));
    }
}
