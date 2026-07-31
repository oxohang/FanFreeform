package com.oxohang.fanfreeform.xposed;

import com.oxohang.fanfreeform.config.ConfigContract;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
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
    public void treatsOnlyActualImeRectangleAsPartOfFreeform() {
        assertTrue(GestureGeometry.insideEither(500, 1000,
                200, 500, 900, 1800,
                0, 1900, 1200, 2608, true));
        assertTrue(GestureGeometry.insideEither(500, 2300,
                200, 500, 900, 1800,
                0, 1900, 1200, 2608, true));
        assertFalse(GestureGeometry.insideEither(500, 1850,
                200, 500, 900, 1800,
                0, 1900, 1200, 2608, true));
        assertFalse(GestureGeometry.insideEither(500, 2300,
                200, 500, 900, 1800,
                0, 1900, 1200, 2608, false));
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
    public void crowdedFanUsesUpToThreeSelectableRows() {
        assertEquals(1, GestureGeometry.fanRowCount(8));
        assertEquals(2, GestureGeometry.fanRowCount(9));
        assertEquals(3, GestureGeometry.fanRowCount(24));
        for (int index = 0; index < 24; index++) {
            GestureGeometry.Point point = GestureGeometry.iconCenter(
                    GestureGeometry.Corner.LEFT, index, 24, 1200, 2608, 696);
            assertEquals(index, GestureGeometry.selection(GestureGeometry.Corner.LEFT,
                    point.x, point.y, 1200, 2608, 24, 696, 42, 3));
        }
    }

    @Test
    public void fanAllocatesMoreIconsToTheRoomierOuterRows() {
        assertArrayEquals(new int[] {6, 7}, GestureGeometry.fanRowCounts(13));
        assertArrayEquals(new int[] {7, 7}, GestureGeometry.fanRowCounts(14));
        assertArrayEquals(new int[] {7, 8}, GestureGeometry.fanRowCounts(15));
        assertArrayEquals(new int[] {5, 6, 6}, GestureGeometry.fanRowCounts(17));
        assertArrayEquals(new int[] {8, 8, 8}, GestureGeometry.fanRowCounts(24));
        for (int count = 9; count <= 24; count++) {
            int total = 0;
            int previous = 0;
            for (int rowCount : GestureGeometry.fanRowCounts(count)) {
                assertTrue(rowCount >= previous);
                assertTrue(rowCount - previous <= 1 || previous == 0);
                previous = rowCount;
                total += rowCount;
            }
            assertEquals(count, total);
        }
    }

    @Test
    public void fixedSevenModeFillsEachRowBeforeStartingTheNext() {
        assertArrayEquals(new int[] {7}, GestureGeometry.fanRowCounts(7, true));
        assertArrayEquals(new int[] {7, 1}, GestureGeometry.fanRowCounts(8, true));
        assertArrayEquals(new int[] {7, 6}, GestureGeometry.fanRowCounts(13, true));
        assertArrayEquals(new int[] {7, 7}, GestureGeometry.fanRowCounts(14, true));
        assertArrayEquals(new int[] {7, 7, 1},
                GestureGeometry.fanRowCounts(15, true));
        assertArrayEquals(new int[] {7, 7, 7},
                GestureGeometry.fanRowCounts(21, true));
        assertArrayEquals(new int[] {7, 7, 7, 1},
                GestureGeometry.fanRowCounts(22, true));
        assertArrayEquals(new int[] {7, 7, 7, 3},
                GestureGeometry.fanRowCounts(24, true));
        assertEquals(4, GestureGeometry.fanRowCount(24, true));
    }

    @Test
    public void fixedSevenModeUsesTheSameCentersForDrawingAndHitTesting() {
        float radius = GestureGeometry.effectiveRadius(24, 580f, 28f, 6f, true);
        float diameter = GestureGeometry.effectiveIconDiameter(
                24, radius, 46f, 28f, 6f, true);
        for (int index = 0; index < 24; index++) {
            GestureGeometry.Point point = GestureGeometry.iconCenter(
                    GestureGeometry.Corner.RIGHT, index, 24, 1200, 2608,
                    radius, true);
            assertEquals(index, GestureGeometry.selection(
                    GestureGeometry.Corner.RIGHT, point.x, point.y,
                    1200, 2608, 24, radius, diameter, 3f, true));
        }
    }

    @Test
    public void customModeFillsInnerThenStartsMiddleAtTheLowestSlot() {
        int mode = ConfigContract.FAN_LAYOUT_CUSTOM;
        float radius = GestureGeometry.effectiveRadius(
                5, 580f, 28f, 6f, mode, 6, 5, 4);
        float diameter = GestureGeometry.effectiveIconDiameter(
                5, radius, 46f, 28f, 6f, mode, 6, 5, 4);
        GestureGeometry.Point innerFirst = GestureGeometry.iconCenter(
                GestureGeometry.Corner.LEFT, 0, 5, 1200, 2608, radius,
                mode, 6, 5, 4);
        GestureGeometry.Point middleFirst = GestureGeometry.iconCenter(
                GestureGeometry.Corner.LEFT, 4, 5, 1200, 2608, radius,
                mode, 6, 5, 4);
        float expectedInnerRadius = radius * 0.68f;
        assertEquals(expectedInnerRadius * (float) Math.cos(Math.toRadians(12)),
                innerFirst.x, 0.01f);
        assertEquals(2608f - expectedInnerRadius * (float) Math.sin(Math.toRadians(12)),
                innerFirst.y, 0.01f);
        float expectedMiddleRadius = radius * 0.88f;
        assertEquals(expectedMiddleRadius * (float) Math.cos(Math.toRadians(15.5)),
                middleFirst.x, 0.01f);
        assertEquals(2608f - expectedMiddleRadius * (float) Math.sin(Math.toRadians(15.5)),
                middleFirst.y, 0.01f);
        assertEquals(4, GestureGeometry.selection(GestureGeometry.Corner.LEFT,
                middleFirst.x, middleFirst.y, 1200, 2608, 5, radius, diameter, 3f,
                mode, 6, 5, 4));
    }

    @Test
    public void customModeSupportsZeroMiddleAndOuterRows() {
        int mode = ConfigContract.FAN_LAYOUT_CUSTOM;
        float radius = 580f;
        for (int index = 0; index < 5; index++) {
            GestureGeometry.Point point = GestureGeometry.iconCenter(
                    GestureGeometry.Corner.LEFT, index, 5, 1200, 2608, radius,
                    mode, 0, 0, 5);
            assertEquals(index, GestureGeometry.selection(
                    GestureGeometry.Corner.LEFT, point.x, point.y,
                    1200, 2608, 5, radius, 46f, 3f,
                    mode, 0, 0, 5));
        }
        GestureGeometry.Point firstMiddle = GestureGeometry.iconCenter(
                GestureGeometry.Corner.LEFT, 5, 7, 1200, 2608, radius,
                mode, 0, 2, 5);
        assertEquals(radius * 0.88f * (float) Math.cos(Math.toRadians(15.5)),
                firstMiddle.x, 0.01f);
    }

    @Test
    public void customModeMirrorsEveryVisualCenterAndHitTarget() {
        int mode = ConfigContract.FAN_LAYOUT_CUSTOM;
        float radius = GestureGeometry.effectiveRadius(
                18, 580f, 28f, 6f, mode, 7, 6, 5);
        float diameter = GestureGeometry.effectiveIconDiameter(
                18, radius, 46f, 28f, 6f, mode, 7, 6, 5);
        for (int index = 0; index < 18; index++) {
            GestureGeometry.Point left = GestureGeometry.iconCenter(
                    GestureGeometry.Corner.LEFT, index, 18, 1200, 2608, radius,
                    mode, 7, 6, 5);
            GestureGeometry.Point right = GestureGeometry.iconCenter(
                    GestureGeometry.Corner.RIGHT, index, 18, 1200, 2608, radius,
                    mode, 7, 6, 5);
            assertEquals(1200f, left.x + right.x, 0.01f);
            assertEquals(left.y, right.y, 0.01f);
            assertEquals(index, GestureGeometry.selection(GestureGeometry.Corner.LEFT,
                    left.x, left.y, 1200, 2608, 18, radius, diameter, 3f,
                    mode, 7, 6, 5));
            assertEquals(index, GestureGeometry.selection(GestureGeometry.Corner.RIGHT,
                    right.x, right.y, 1200, 2608, 18, radius, diameter, 3f,
                    mode, 7, 6, 5));
        }
    }

    @Test
    public void honeycombSafetyBoundaryStartsOutsideTheOutermostFanIcon() {
        int mode = ConfigContract.FAN_LAYOUT_CUSTOM;
        float radius = GestureGeometry.effectiveRadius(
                18, 580f, 28f, 6f, mode, 7, 6, 5);
        float diameter = GestureGeometry.effectiveIconDiameter(
                18, radius, 46f, 28f, 6f, mode, 7, 6, 5);
        float safetyMargin = 30f;
        float boundary = GestureGeometry.fanOutermostEdgeDistance(
                GestureGeometry.Corner.LEFT, 18, 1200, 2608, radius,
                diameter, safetyMargin, mode, 7, 6, 5);
        float outermostCenter = 0f;
        for (int index = 0; index < 18; index++) {
            GestureGeometry.Point point = GestureGeometry.iconCenter(
                    GestureGeometry.Corner.LEFT, index, 18, 1200, 2608,
                    radius, mode, 7, 6, 5);
            outermostCenter = Math.max(outermostCenter,
                    GestureGeometry.distance(0f, 2608f, point.x, point.y));
        }
        assertEquals(outermostCenter + diameter / 2f + safetyMargin,
                boundary, 0.01f);
        assertFalse(GestureGeometry.beyondFanEdge(GestureGeometry.Corner.LEFT,
                boundary - 1f, 2608f, 1200, 2608, boundary));
        assertTrue(GestureGeometry.beyondFanEdge(GestureGeometry.Corner.LEFT,
                boundary, 2608f, 1200, 2608, boundary));
        assertFalse(GestureGeometry.beyondFanEdge(GestureGeometry.Corner.RIGHT,
                1200f - boundary + 1f, 2608f, 1200, 2608, boundary));
        assertTrue(GestureGeometry.beyondFanEdge(GestureGeometry.Corner.RIGHT,
                1200f - boundary, 2608f, 1200, 2608, boundary));
    }

    @Test
    public void reservesBothEdgesAtEveryHeight() {
        assertTrue(GestureGeometry.inSideGestureReserve(20, 1200, 60, 70));
        assertTrue(GestureGeometry.inSideGestureReserve(1150, 1200, 60, 70));
        assertFalse(GestureGeometry.inSideGestureReserve(600, 1200, 60, 70));
    }

    @Test
    public void sideTapKeepsNaturalJitterButYieldsToIntentionalBackSwipe() {
        assertFalse(GestureGeometry.shouldYieldSideTap(
                true, 20, 1000, 42, 1024, 36, 54));
        assertFalse(GestureGeometry.shouldYieldSideTap(
                false, 1180, 1000, 1158, 1024, 36, 54));
        assertTrue(GestureGeometry.shouldYieldSideTap(
                true, 20, 1000, 70, 1008, 36, 54));
        assertTrue(GestureGeometry.shouldYieldSideTap(
                false, 1180, 1000, 1130, 992, 36, 54));
    }

    @Test
    public void sideTapYieldsAfterLargeNonBackMovement() {
        assertTrue(GestureGeometry.shouldYieldSideTap(
                true, 20, 1000, 35, 1060, 36, 54));
        assertTrue(GestureGeometry.shouldYieldSideTap(
                false, 1180, 1000, 1190, 1060, 36, 54));
    }

    @Test
    public void recognizesOnlyIntentionalInwardSideSwipe() {
        assertTrue(GestureGeometry.isIntentionalSideSwipe(
                GestureGeometry.Corner.LEFT, 10, 1000, 60, 1010, 36));
        assertTrue(GestureGeometry.isIntentionalSideSwipe(
                GestureGeometry.Corner.RIGHT, 1190, 1000, 1140, 990, 36));
        assertFalse(GestureGeometry.isIntentionalSideSwipe(
                GestureGeometry.Corner.LEFT, 10, 1000, 30, 1002, 36));
        assertFalse(GestureGeometry.isIntentionalSideSwipe(
                GestureGeometry.Corner.RIGHT, 1190, 1000, 1140, 1080, 36));
    }

    @Test
    public void bottomCornersStayFlushWhileSideGestureUsesUpperBand() {
        assertEquals(GestureGeometry.Corner.LEFT,
                GestureGeometry.cornerAt(30, 2500, 1200, 2600, 96, 440));
        assertEquals(GestureGeometry.Corner.RIGHT,
                GestureGeometry.cornerAt(1170, 2500, 1200, 2600, 96, 440));
        assertEquals(GestureGeometry.Corner.LEFT,
                GestureGeometry.sideAt(20, 1200, 1200, 60, 60,
                        520, 2136, 96));
        assertNull(GestureGeometry.sideAt(20, 2500, 1200, 60, 60,
                520, 2136, 96));
        assertNull(GestureGeometry.sideAt(20, 400, 1200, 60, 60,
                520, 2136, 96));
    }

    @Test
    public void sideListMirrorsClampsAndSelectsRows() {
        float top = GestureGeometry.sideListTop(1300, 6, 150, 520, 2136);
        GestureGeometry.Point left = GestureGeometry.sideListIconCenter(
                GestureGeometry.Corner.LEFT, 2, 1200, top, 150, 120, 40);
        GestureGeometry.Point right = GestureGeometry.sideListIconCenter(
                GestureGeometry.Corner.RIGHT, 2, 1200, top, 150, 120, 40);
        assertEquals(1200f, left.x + right.x, 0.01f);
        assertEquals(left.y, right.y, 0.01f);
        assertTrue(top >= 520f);
        assertTrue(top + 6 * 150 <= 2136f);
        assertEquals(2, GestureGeometry.sideListSelection(left.y, 6, top, 150));
        assertEquals(-1, GestureGeometry.sideListSelection(top - 1, 6, top, 150));
    }

    @Test
    public void fingerCenteredListAlignsChosenMiddleItem() {
        float oddTop = GestureGeometry.sideListTopForAnchor(
                1300, 7, 3, 150, 520, 2136);
        GestureGeometry.Point oddMiddle = GestureGeometry.sideListIconCenter(
                3, 460, oddTop, 150);
        assertEquals(1300f, oddMiddle.y, 0.01f);

        float evenTop = GestureGeometry.sideListTopForAnchor(
                1300, 8, 3, 150, 400, 2200);
        GestureGeometry.Point upperMiddle = GestureGeometry.sideListIconCenter(
                3, 460, evenTop, 150);
        assertEquals(1300f, upperMiddle.y, 0.01f);
    }

    @Test
    public void sideFanPlacesMiddleAppAtFingerAndMirrorsSelection() {
        GestureGeometry.Point left = GestureGeometry.sideFanIconCenter(
                GestureGeometry.Corner.LEFT, 2, 6, 1200, 420, 1300, 420);
        GestureGeometry.Point right = GestureGeometry.sideFanIconCenter(
                GestureGeometry.Corner.RIGHT, 2, 6, 1200, 780, 1300, 420);
        assertEquals(420f, left.x, 0.01f);
        assertEquals(1300f, left.y, 0.01f);
        assertEquals(1200f, left.x + right.x, 0.01f);
        assertEquals(2, GestureGeometry.sideFanSelection(
                GestureGeometry.Corner.LEFT, left.x, left.y, 1200,
                6, 420, 1300, 420, 90, 8));
        assertEquals(2, GestureGeometry.sideFanSelection(
                GestureGeometry.Corner.RIGHT, right.x, right.y, 1200,
                6, 780, 1300, 420, 90, 8));
    }

    @Test
    public void sideFanCenterKeepsEveryIconInsideSafeBand() {
        float center = GestureGeometry.sideFanCenterY(
                560, 8, 420, 90, 520, 2136);
        for (int index = 0; index < 8; index++) {
            GestureGeometry.Point point = GestureGeometry.sideFanIconCenter(
                    GestureGeometry.Corner.LEFT, index, 8, 1200, 420, center, 420);
            assertTrue(point.y - 53 >= 520);
            assertTrue(point.y + 53 <= 2136);
        }
    }

    @Test
    public void sideRingMovesInwardAndKeepsEveryIconVisible() {
        float diameter = 120f;
        float radius = GestureGeometry.sideRingRadius(8, diameter, 24f);
        GestureGeometry.Point center = GestureGeometry.sideRingCenter(
                40, 700, radius, diameter, 20, 520, 1180, 2136);
        assertTrue(center.x > 40);
        for (int index = 0; index < 8; index++) {
            GestureGeometry.Point icon = GestureGeometry.sideRingIconCenter(
                    index, 8, center.x, center.y, radius);
            assertTrue(icon.x - diameter / 2f >= 20f);
            assertTrue(icon.x + diameter / 2f <= 1180f);
            assertTrue(icon.y - diameter / 2f >= 520f);
            assertTrue(icon.y + diameter / 2f <= 2136f);
            assertEquals(index, GestureGeometry.sideRingSelection(
                    icon.x, icon.y, 8, center.x, center.y, radius,
                    diameter, 8f));
        }
        assertEquals(-1, GestureGeometry.sideRingSelection(
                center.x, center.y, 8, center.x, center.y, radius,
                diameter, 8f));
    }

    @Test
    public void sideRingSizeScalesRadiusWithoutChangingIconsAndClampsToScreen() {
        float automatic = 180f;
        assertEquals(108f, GestureGeometry.scaledSideRingRadius(
                automatic, 60, 400f), 0.01f);
        assertEquals(180f, GestureGeometry.scaledSideRingRadius(
                automatic, 100, 400f), 0.01f);
        assertEquals(324f, GestureGeometry.scaledSideRingRadius(
                automatic, 180, 400f), 0.01f);
        assertEquals(250f, GestureGeometry.scaledSideRingRadius(
                automatic, 180, 250f), 0.01f);
    }

    @Test
    public void followFingerListAppearsAheadOfSwipeAndListExitIsExplicit() {
        assertEquals(460f, GestureGeometry.sideListCenterX(
                GestureGeometry.Corner.LEFT, 460, 1200, 120, 40, true), 0.01f);
        assertEquals(740f, GestureGeometry.sideListCenterX(
                GestureGeometry.Corner.RIGHT, 740, 1200, 120, 40, true), 0.01f);
        assertEquals(100f, GestureGeometry.sideListCenterX(
                GestureGeometry.Corner.LEFT, 460, 1200, 120, 40, false), 0.01f);
        assertEquals(1100f, GestureGeometry.sideListCenterX(
                GestureGeometry.Corner.RIGHT, 740, 1200, 120, 40, false), 0.01f);
        assertTrue(GestureGeometry.insideSideList(
                470, 900, 460, 180, 7, 600, 100));
        assertFalse(GestureGeometry.insideSideList(
                560, 900, 460, 180, 7, 600, 100));
        assertFalse(GestureGeometry.insideSideList(
                460, 1300, 460, 180, 7, 600, 100));
    }

    @Test
    public void wheelSelectionClampsAndRoundsToCenter() {
        assertEquals(0, GestureGeometry.wheelSelection(-0.4f, 7));
        assertEquals(3, GestureGeometry.wheelSelection(2.6f, 7));
        assertEquals(6, GestureGeometry.wheelSelection(8f, 7));
        assertEquals(-1, GestureGeometry.wheelSelection(2f, 0));
    }

    @Test
    public void sideReverseFadeMirrorsAndClamps() {
        assertEquals(352f, GestureGeometry.sideListOuterBoundary(
                GestureGeometry.Corner.LEFT, 400, 96), 0.01f);
        assertEquals(848f, GestureGeometry.sideListOuterBoundary(
                GestureGeometry.Corner.RIGHT, 800, 96), 0.01f);
        assertEquals(0f, GestureGeometry.sideReverseDistance(
                GestureGeometry.Corner.LEFT, 352, 360), 0.01f);
        assertEquals(40f, GestureGeometry.sideReverseDistance(
                GestureGeometry.Corner.LEFT, 400, 360), 0.01f);
        assertEquals(40f, GestureGeometry.sideReverseDistance(
                GestureGeometry.Corner.RIGHT, 800, 840), 0.01f);
        assertEquals(0f, GestureGeometry.sideReverseDistance(
                GestureGeometry.Corner.LEFT, 400, 500), 0.01f);
        assertEquals(1f, GestureGeometry.sideListOpacity(0, 120), 0.01f);
        assertEquals(0.5f, GestureGeometry.sideListOpacity(60, 120), 0.01f);
        assertEquals(0f, GestureGeometry.sideListOpacity(140, 120), 0.01f);
    }

    @Test
    public void sideListExitDistinguishesEdgeInwardAndVerticalDirections() {
        assertTrue(GestureGeometry.outsideSideListVertically(590, 7, 600, 100));
        assertFalse(GestureGeometry.outsideSideListVertically(900, 7, 600, 100));
        assertTrue(GestureGeometry.beyondSideListInward(
                GestureGeometry.Corner.LEFT, 570, 460, 180));
        assertFalse(GestureGeometry.beyondSideListInward(
                GestureGeometry.Corner.LEFT, 350, 460, 180));
        assertTrue(GestureGeometry.beyondSideListInward(
                GestureGeometry.Corner.RIGHT, 630, 740, 180));
        assertFalse(GestureGeometry.beyondSideListInward(
                GestureGeometry.Corner.RIGHT, 850, 740, 180));
    }
}
