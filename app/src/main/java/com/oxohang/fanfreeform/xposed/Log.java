package com.oxohang.fanfreeform.xposed;

import de.robv.android.xposed.XposedBridge;

final class Log {
    private static final String PREFIX = "[FanFreeform] ";
    private Log() {}

    static void i(String message) {
        XposedBridge.log(PREFIX + message);
    }

    static void e(String message, Throwable throwable) {
        XposedBridge.log(PREFIX + message + ": " + throwable);
    }
}

