package com.oxohang.fanfreeform.xposed;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class TaskCenterAnimationPolicyTest {
    @Test
    public void standardKeepsBaseDurationAndAllFiveSpeedsAreOrdered() {
        assertEquals(245L, TaskCenterAnimationPolicy.durationMs(245L, 2));
        long previous = 0L;
        for (int speed = 0; speed <= 4; speed++) {
            long duration = TaskCenterAnimationPolicy.durationMs(245L, speed);
            assertTrue(duration > previous);
            previous = duration;
        }
    }

    @Test
    public void speedIndexIsClamped() {
        assertEquals(TaskCenterAnimationPolicy.durationMs(200L, 0),
                TaskCenterAnimationPolicy.durationMs(200L, -1));
        assertEquals(TaskCenterAnimationPolicy.durationMs(200L, 4),
                TaskCenterAnimationPolicy.durationMs(200L, 8));
    }
}
