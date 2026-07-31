package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class HoneycombGeometryTest {
    @Test public void compactLayoutStartsAtCenterAndCreatesRoundSixNeighbors() {
        List<HoneycombGeometry.Point> points = HoneycombGeometry.compactPoints(7, 60f);
        assertEquals(7, points.size());
        assertEquals(0f, points.get(0).x, 0.001f);
        assertEquals(0f, points.get(0).y, 0.001f);
        for (int index = 1; index < points.size(); index++) {
            float distance = (float) Math.hypot(points.get(index).x, points.get(index).y);
            assertTrue(distance >= 50f && distance <= 61f);
        }
    }

    @Test public void centerScaleAndEdgeScaleUseSmoothBounds() {
        assertEquals(1.28f, HoneycombGeometry.smoothScale(0f, 100f, 1.28f, 0.62f), 0.001f);
        assertEquals(0.62f, HoneycombGeometry.smoothScale(100f, 100f, 1.28f, 0.62f), 0.001f);
        float middle = HoneycombGeometry.smoothScale(50f, 100f, 1.28f, 0.62f);
        assertTrue(middle < 1.28f && middle > 0.62f);
    }

    @Test public void hitChoosesNearestIconWithinRadius() {
        List<HoneycombGeometry.Point> points = HoneycombGeometry.compactPoints(7, 60f);
        assertEquals(0, HoneycombGeometry.hit(points, 2f, 1f, 25f));
        assertEquals(-1, HoneycombGeometry.hit(points, 400f, 400f, 25f));
    }

    @Test public void dragResistanceOnlyAffectsOverflow() {
        assertEquals(50f, HoneycombGeometry.resisted(50f, 0f, 100f, 0.3f), 0.001f);
        assertEquals(106f, HoneycombGeometry.resisted(120f, 0f, 100f, 0.3f), 0.001f);
        assertEquals(-6f, HoneycombGeometry.resisted(-20f, 0f, 100f, 0.3f), 0.001f);
    }

    @Test public void partialOuterRingStaysCircularAndBalanced() {
        List<HoneycombGeometry.Point> points = HoneycombGeometry.compactPoints(36, 60f);
        float maxX = 0f;
        float maxY = 0f;
        float sumX = 0f;
        float sumY = 0f;
        for (HoneycombGeometry.Point point : points) {
            maxX = Math.max(maxX, Math.abs(point.x));
            maxY = Math.max(maxY, Math.abs(point.y));
            sumX += point.x;
            sumY += point.y;
        }
        assertTrue(maxX / maxY > 0.78f && maxX / maxY < 1.22f);
        assertTrue(Math.abs(sumX / points.size()) < 8f);
        assertTrue(Math.abs(sumY / points.size()) < 8f);
    }

    @Test public void scaledHitUsesTheVisibleIconUnderThePointer() {
        List<HoneycombGeometry.Point> points = HoneycombGeometry.compactPoints(7, 60f);
        HoneycombGeometry.Point target = points.get(4);
        assertEquals(4, HoneycombGeometry.hitScaled(points, target.x, target.y,
                52f, 0f, 0f, 200f, 1.28f, 0.62f));
    }

    @Test public void commonAppCountsKeepACircularCentroid() {
        for (int count : new int[] {7, 8, 12, 19, 36, 60}) {
            List<HoneycombGeometry.Point> points = HoneycombGeometry.compactPoints(count, 60f);
            float maxX = 0f;
            float maxY = 0f;
            float sumX = 0f;
            float sumY = 0f;
            for (HoneycombGeometry.Point point : points) {
                maxX = Math.max(maxX, Math.abs(point.x));
                maxY = Math.max(maxY, Math.abs(point.y));
                sumX += point.x;
                sumY += point.y;
            }
            float outerAspect = (maxX + 30f) / (maxY + 30f);
            assertTrue("layout is too wide at count=" + count + " aspect=" + outerAspect,
                    outerAspect < 1.25f);
            assertTrue("layout is too tall at count=" + count + " aspect=" + outerAspect,
                    outerAspect > 0.80f);
            assertTrue("x centroid drift at count=" + count,
                    Math.abs(sumX / count) < 10f);
            assertTrue("y centroid drift at count=" + count,
                    Math.abs(sumY / count) < 10f);
        }
    }

    @Test public void circularRowsPreserveStrictHoneycombNeighborSpacing() {
        List<HoneycombGeometry.Point> points = HoneycombGeometry.compactPoints(36, 60f);
        for (HoneycombGeometry.Point point : points) {
            float nearest = Float.MAX_VALUE;
            for (HoneycombGeometry.Point other : points) {
                if (point == other) continue;
                nearest = Math.min(nearest, (float) Math.hypot(
                        point.x - other.x, point.y - other.y));
            }
            assertEquals(60f, nearest, 0.02f);
        }
    }

    @Test public void edgeTransformShrinksContinuouslyAndHidesOutside() {
        float inside = HoneycombGeometry.edgeVisibility(70f, 100f, 20f);
        float crossing = HoneycombGeometry.edgeVisibility(100f, 100f, 20f);
        float outside = HoneycombGeometry.edgeVisibility(125f, 100f, 20f);
        assertEquals(1f, inside, 0.001f);
        assertTrue(crossing > 0f && crossing < inside);
        assertEquals(0f, outside, 0.001f);
        assertTrue(HoneycombGeometry.edgeScale(100f, 100f, 20f)
                < HoneycombGeometry.edgeScale(70f, 100f, 20f));
        assertTrue(HoneycombGeometry.edgeInset(100f, 100f, 20f) > 0f);
    }

    @Test public void visibleHitUsesRenderedCenterAndSkipsHiddenIcon() {
        float[] centersX = {30f, 70f, 100f};
        float[] centersY = {40f, 40f, 40f};
        float[] radii = {20f, 0f, 18f};
        assertEquals(0, HoneycombGeometry.hitVisible(
                centersX, centersY, radii, 3, 32f, 41f));
        assertEquals(-1, HoneycombGeometry.hitVisible(
                centersX, centersY, radii, 3, 70f, 40f));
        assertEquals(2, HoneycombGeometry.hitVisible(
                centersX, centersY, radii, 3, 99f, 39f));
    }

    @Test public void pressureFieldFallsOffSmoothlyAndStopsAtItsEdge() {
        assertEquals(1f, HoneycombGeometry.pressureInfluence(0f, 100f), 0.001f);
        float middle = HoneycombGeometry.pressureInfluence(50f, 100f);
        assertTrue(middle > 0f && middle < 1f);
        assertEquals(0f, HoneycombGeometry.pressureInfluence(100f, 100f), 0.001f);
        assertEquals(0f, HoneycombGeometry.pressureInfluence(140f, 100f), 0.001f);
    }

    @Test public void fisheyeMagnifiesFocusAndShrinksTheWholeOuterDisc() {
        assertEquals(1.24f, HoneycombGeometry.fisheyeScale(
                0f, 100f, 0.66f, 1.24f), 0.001f);
        float middle = HoneycombGeometry.fisheyeScale(50f, 100f, 0.66f, 1.24f);
        assertTrue(middle > 0.66f && middle < 1.24f);
        assertEquals(0.66f, HoneycombGeometry.fisheyeScale(
                100f, 100f, 0.66f, 1.24f), 0.001f);
    }

    @Test public void surfaceBulgeMovesTheWholeMeshButReturnsAtItsBoundary() {
        assertEquals(0f, HoneycombGeometry.surfaceBulgeOffset(0f, 120f, 42f), 0.001f);
        float inner = HoneycombGeometry.surfaceBulgeOffset(20f, 120f, 42f);
        float middle = HoneycombGeometry.surfaceBulgeOffset(40f, 120f, 42f);
        float outer = HoneycombGeometry.surfaceBulgeOffset(90f, 120f, 42f);
        assertTrue(inner > 0f);
        assertTrue(middle > inner);
        assertTrue(outer > 0f && outer < middle);
        assertEquals(0f, HoneycombGeometry.surfaceBulgeOffset(120f, 120f, 42f),
                0.001f);
    }

    @Test public void edgePanStartsOnlyInTheOuterBandAndAcceleratesSmoothly() {
        assertEquals(0f, HoneycombGeometry.edgePanStrength(70f, 100f, 0.78f),
                0.001f);
        assertEquals(0f, HoneycombGeometry.edgePanStrength(78f, 100f, 0.78f),
                0.001f);
        float middle = HoneycombGeometry.edgePanStrength(89f, 100f, 0.78f);
        assertTrue(middle > 0f && middle < 1f);
        assertEquals(1f, HoneycombGeometry.edgePanStrength(100f, 100f, 0.78f),
                0.001f);
        assertEquals(1f, HoneycombGeometry.edgePanStrength(120f, 100f, 0.78f),
                0.001f);
    }

    @Test public void holdAccelerationMovesTheWholeGridAgainstTheFinger() {
        assertEquals(-3.5f, HoneycombGeometry.counterPanDelta(10f, 0.35f),
                0.001f);
        assertEquals(7f, HoneycombGeometry.counterPanDelta(-20f, 0.35f),
                0.001f);
        assertEquals(-10f, HoneycombGeometry.counterPanDelta(10f, 2f),
                0.001f);
    }
}
