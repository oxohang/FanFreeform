package com.oxohang.fanfreeform.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class PressurePulseRecognizerTest {
    @Test public void singleModeTriggersOnFirstRisingWave() {
        PressurePulseRecognizer recognizer = new PressurePulseRecognizer();
        recognizer.start();
        assertEquals(PressurePulseRecognizer.Signal.SINGLE_PRESS,
                recognizer.onPressure(1.1f, 1f, 100L));
        assertEquals(PressurePulseRecognizer.Signal.NONE,
                recognizer.onPressure(1.4f, 1f, 120L));
    }

    @Test public void releaseWithoutPressureDoesNotTrigger() {
        PressurePulseRecognizer recognizer = new PressurePulseRecognizer();
        recognizer.start();
        assertEquals(PressurePulseRecognizer.Signal.NONE, recognizer.onUp());
    }
}
