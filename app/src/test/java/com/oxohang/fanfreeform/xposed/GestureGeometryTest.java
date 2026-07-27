package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GestureGeometryTest {
    @Test
    public void detectsOnlyBottomCorners() {
        assertEquals(GestureGeometry.Corner.LEFT,
                GestureGeometry.cornerAt(20, 2580, 1200, 2608, 144, 183));
        assertEquals(GestureGeometry.Corner.RIGHT,
                GestureGeometry.cornerAt(1180, 2580, 1200, 2608, 144, 183));
        assertNull(GestureGeometry.cornerAt(600, 2580, 1200, 2608, 144, 183));
        assertNull(GestureGeometry.cornerAt(20, 2100, 1200, 2608, 144, 183));
    }

    @Test
    public void twentyPercentHeightIncludesBottomHandleAndStopsAtConfiguredTop() {
        float hotHeight = 2608 * 0.20f;
        assertEquals(GestureGeometry.Corner.LEFT,
                GestureGeometry.cornerAt(20, 2607, 1200, 2608, 144, hotHeight));
        assertEquals(GestureGeometry.Corner.LEFT,
                GestureGeometry.cornerAt(20, 2608 - hotHeight, 1200, 2608, 144, hotHeight));
        assertNull(GestureGeometry.cornerAt(
                20, 2608 - hotHeight - 1, 1200, 2608, 144, hotHeight));
    }

    @Test
    public void measuresDistanceFromOriginalDownPoint() {
        assertEquals(5f, GestureGeometry.distance(10, 10, 13, 14), 0.001f);
    }

    @Test
    public void mirrorsSelectionAcrossCorners() {
        GestureGeometry.Point point = GestureGeometry.iconCenter(
                GestureGeometry.Corner.LEFT, 2, 6, 1200, 2608, 696);
        int left = GestureGeometry.selection(GestureGeometry.Corner.LEFT,
                point.x, point.y, 1200, 2608, 6, 696, 46, 6);
        int right = GestureGeometry.selection(GestureGeometry.Corner.RIGHT,
                1200 - point.x, point.y, 1200, 2608, 6, 696, 46, 6);
        assertEquals(left, right);
        assertEquals(2, left);
    }

    @Test
    public void opensOnlyWhenPointerActuallyHitsIcon() {
        GestureGeometry.Point point = GestureGeometry.iconCenter(
                GestureGeometry.Corner.LEFT, 3, 6, 1200, 2608, 696);
        assertEquals(3, GestureGeometry.selection(GestureGeometry.Corner.LEFT,
                point.x + 20, point.y, 1200, 2608, 6, 696, 46, 6));
        assertEquals(-1, GestureGeometry.selection(GestureGeometry.Corner.LEFT,
                point.x + 80, point.y, 1200, 2608, 6, 696, 46, 6));
        assertEquals(-1, GestureGeometry.selection(GestureGeometry.Corner.LEFT,
                300, 2450, 1200, 2608, 6, 696, 46, 6));
    }

    @Test
    public void classifiesOutsideUsingActualWindowBounds() {
        assertEquals(GestureGeometry.OutsideRegion.INSIDE,
                GestureGeometry.outsideRegion(500, 1000, 200, 500, 900, 1800));
        assertEquals(GestureGeometry.OutsideRegion.OUTSIDE,
                GestureGeometry.outsideRegion(500, 300, 200, 500, 900, 1800));
        assertEquals(GestureGeometry.OutsideRegion.OUTSIDE,
                GestureGeometry.outsideRegion(500, 2000, 200, 500, 900, 1800));
        assertEquals(GestureGeometry.OutsideRegion.OUTSIDE,
                GestureGeometry.outsideRegion(100, 1000, 200, 500, 900, 1800));
    }

    @Test
    public void growsRadiusBeforeShrinkingBelowMinimumIconSize() {
        float radius = GestureGeometry.effectiveRadius(8, 140, 28, 6);
        float diameter = GestureGeometry.effectiveIconDiameter(8, radius, 46, 28, 6);
        assertTrue(radius > 140);
        assertTrue(diameter >= 28);
        assertTrue(diameter <= 46);
    }

    @Test
    public void reservesBothEdgesAtEveryHeight() {
        assertTrue(GestureGeometry.inSideGestureReserve(20, 1200, 60, 70));
        assertTrue(GestureGeometry.inSideGestureReserve(1150, 1200, 60, 70));
        assertFalse(GestureGeometry.inSideGestureReserve(600, 1200, 60, 70));
    }

    @Test
    public void insetBottomCornersDoNotOverlapSideGestureStrip() {
        assertEquals(GestureGeometry.Corner.LEFT,
                GestureGeometry.cornerAt(100, 2500, 1200, 2600,
                        96, 440, 62, 62));
        assertNull(GestureGeometry.cornerAt(30, 2500, 1200, 2600,
                96, 440, 62, 62));
        assertEquals(GestureGeometry.Corner.RIGHT,
                GestureGeometry.cornerAt(1100, 2500, 1200, 2600,
                        96, 440, 62, 62));
    }

    @Test
    public void sideFanMirrorsAndClampsVertically() {
        GestureGeometry.Point left = GestureGeometry.sideIconCenter(
                GestureGeometry.Corner.LEFT, 1, 3, 1200, 1300, 500);
        GestureGeometry.Point right = GestureGeometry.sideIconCenter(
                GestureGeometry.Corner.RIGHT, 1, 3, 1200, 1300, 500);
        assertEquals(1200f, left.x + right.x, 0.01f);
        assertEquals(left.y, right.y, 0.01f);
        float adjusted = GestureGeometry.adjustedSideOriginY(
                10, 2600, 500, 46, 144, 74);
        assertTrue(adjusted > 500f);
        assertEquals(1, GestureGeometry.sideSelection(GestureGeometry.Corner.LEFT,
                left.x, left.y, 1200, 3, 1300, 500, 46, 1));
    }
}
