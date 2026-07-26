package com.oxohang.fanfreeform.xposed;

final class GestureReplayGuard {
    private long activeDownTime = -1L;
    private long latestEventTime = -1L;

    void begin(long downTime, long eventTime) {
        activeDownTime = downTime;
        latestEventTime = eventTime;
    }

    boolean isRepeatedDown(long downTime) {
        return activeDownTime >= 0L && downTime == activeDownTime;
    }

    boolean isStaleTerminal(long downTime, long eventTime) {
        return activeDownTime >= 0L && downTime == activeDownTime
                && eventTime < latestEventTime;
    }

    void record(long downTime, long eventTime) {
        if (downTime == activeDownTime && eventTime > latestEventTime) {
            latestEventTime = eventTime;
        }
    }

    void reset() {
        activeDownTime = -1L;
        latestEventTime = -1L;
    }
}
