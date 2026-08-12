package com.oxohang.fanfreeform.xposed;

final class InputSourceCoordinator {
    enum Mode {
        CORNER_FALLBACK,
        SWITCHING_TO_NATIVE,
        NATIVE_GLOBAL
    }

    private Mode mode = Mode.CORNER_FALLBACK;

    synchronized boolean beginNativeRegistration() {
        if (mode == Mode.NATIVE_GLOBAL || mode == Mode.SWITCHING_TO_NATIVE) return false;
        mode = Mode.SWITCHING_TO_NATIVE;
        return true;
    }

    synchronized void completeNativeRegistration() {
        mode = Mode.NATIVE_GLOBAL;
    }

    synchronized void failNativeRegistration() {
        if (mode == Mode.SWITCHING_TO_NATIVE) mode = Mode.CORNER_FALLBACK;
    }

    synchronized boolean shouldDispatchCornerFallback() {
        return mode == Mode.CORNER_FALLBACK;
    }

    synchronized boolean usesNativeInput() {
        return mode == Mode.NATIVE_GLOBAL;
    }

    synchronized Mode mode() {
        return mode;
    }
}
