package com.oxohang.fanfreeform.config;

/** Detects the first rising pressure wave during one touch. */
public final class PressurePulseRecognizer {
    public enum Signal {
        NONE,
        SINGLE_PRESS
    }

    private boolean active;
    private boolean aboveThreshold;
    private boolean resolved;

    public void start() {
        active = true;
        aboveThreshold = false;
        resolved = false;
    }

    public Signal onPressure(float delta, float threshold, long ignoredEventTime) {
        if (!active || resolved || !Float.isFinite(delta) || !(threshold > 0f)) {
            return Signal.NONE;
        }
        boolean nextAbove = delta >= threshold;
        if (!nextAbove) {
            aboveThreshold = false;
            return Signal.NONE;
        }
        if (aboveThreshold) return Signal.NONE;
        aboveThreshold = true;
        resolved = true;
        return Signal.SINGLE_PRESS;
    }

    public Signal onUp() {
        active = false;
        return Signal.NONE;
    }

    public void cancel() {
        active = false;
        aboveThreshold = false;
        resolved = false;
    }
}
