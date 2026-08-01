package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class TaskSelectionPolicyTest {
    @Test
    public void alwaysSelectsTheNearestVisualTaskOnceActive() {
        assertEquals(0, TaskSelectionPolicy.nearestIndex(0.49f, 4, true));
        assertEquals(1, TaskSelectionPolicy.nearestIndex(0.51f, 4, true));
        assertEquals(2, TaskSelectionPolicy.nearestIndex(2.49f, 4, true));
        assertEquals(3, TaskSelectionPolicy.nearestIndex(3.8f, 4, true));
    }

    @Test
    public void clearsOnlyWhenSelectionHasNotStarted() {
        assertEquals(-1, TaskSelectionPolicy.nearestIndex(1.2f, 4, false));
        assertEquals(-1, TaskSelectionPolicy.nearestIndex(0f, 0, true));
    }

    @Test
    public void edgeOverscrollDoesNotCreateSelectionDebt() {
        float step = 40f;
        float position = TaskSelectionPolicy.advancePosition(0f, step * 3f, step, 4);
        position = TaskSelectionPolicy.advancePosition(position, -step * 3f, step, 4);
        assertEquals(0f, position, 0.001f);

        position = TaskSelectionPolicy.advancePosition(position, -step * 4f, step, 4);
        assertEquals(0f, position, 0.001f);

        position = TaskSelectionPolicy.advancePosition(position, step * 0.51f, step, 4);
        assertEquals(1, TaskSelectionPolicy.nearestIndex(position, 4, true));
    }

    @Test
    public void incrementalMovementKeepsTheExistingFastTraversalCurve() {
        float position = TaskSelectionPolicy.advancePosition(0f, 100f, 40f, 8);
        assertEquals(3.3f, position, 0.001f);
    }

    @Test
    public void extendedToleranceDoublesOnlyTheDownwardDismissDistance() {
        assertEquals(180f, TaskSelectionPolicy.downwardDismissDistance(90f, true), 0.001f);
        assertEquals(90f, TaskSelectionPolicy.downwardDismissDistance(90f, false), 0.001f);
    }
}
