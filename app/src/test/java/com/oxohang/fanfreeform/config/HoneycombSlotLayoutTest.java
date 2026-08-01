package com.oxohang.fanfreeform.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class HoneycombSlotLayoutTest {
    @Test public void compactPointsReturnsStableDistinctSlots() {
        List<HoneycombSlotLayout.Point> first = HoneycombSlotLayout.compactPoints(43, 82f);
        List<HoneycombSlotLayout.Point> second = HoneycombSlotLayout.compactPoints(43, 82f);
        assertEquals(43, first.size());
        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).x, second.get(i).x, 0.001f);
            assertEquals(first.get(i).y, second.get(i).y, 0.001f);
            for (int j = i + 1; j < first.size(); j++) {
                assertNotEquals(0f, (float) Math.hypot(first.get(i).x - first.get(j).x,
                        first.get(i).y - first.get(j).y), 0.001f);
            }
        }
    }

    @Test public void nearestAndSwapUseVisibleGridOrder() {
        List<HoneycombSlotLayout.Point> points = HoneycombSlotLayout.compactPoints(5, 80f);
        HoneycombSlotLayout.Point target = points.get(3);
        assertEquals(3, HoneycombSlotLayout.nearest(points, target.x + 2f,
                target.y - 2f, 20f));
        ArrayList<String> values = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        HoneycombSlotLayout.swap(values, 0, 2);
        assertEquals(Arrays.asList("c", "b", "a", "d"), values);
    }
}
