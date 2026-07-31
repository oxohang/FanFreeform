package com.oxohang.fanfreeform.config;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public final class FanRowAllocationTest {
    @Test public void fiveSelectedCanAllStayInInnerRow() {
        assertArrayEquals(new int[] {5, 0, 0},
                FanRowAllocation.normalize(5, 5, 6));
    }

    @Test public void sevenSelectedFillMiddleAfterFiveInner() {
        assertArrayEquals(new int[] {5, 2, 0},
                FanRowAllocation.afterInnerChanged(7, 5));
    }

    @Test public void middleAndOuterEditsAlwaysPreserveSelectedTotal() {
        assertArrayEquals(new int[] {5, 1, 1},
                FanRowAllocation.afterMiddleChanged(7, 5, 1));
        assertArrayEquals(new int[] {5, 1, 1},
                FanRowAllocation.afterOuterChanged(7, 5, 1));
    }

    @Test public void everyRowRespectsItsIndependentMaximum() {
        assertArrayEquals(new int[] {8, 10, 6},
                FanRowAllocation.afterInnerChanged(24, 99));
        assertArrayEquals(new int[] {0, 10, 14},
                FanRowAllocation.afterInnerChanged(24, 0));
        assertArrayEquals(new int[] {8, 2, 14},
                FanRowAllocation.afterOuterChanged(24, 8, 99));
    }
}
