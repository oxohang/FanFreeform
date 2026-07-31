package com.oxohang.fanfreeform.debug;

import android.app.Activity;
import android.os.Bundle;

public final class DebugHookReloadActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        new Thread(() -> {
            try {
                runRoot("killall com.android.systemui");
                Thread.sleep(650L);
                runRoot("killall com.miui.home");
            } catch (Throwable ignored) {
                // This activity is only an ADB recovery path for a touch-blocked debug build.
            } finally {
                runOnUiThread(this::finish);
            }
        }, "debug-hook-reload").start();
    }

    private static void runRoot(String command) throws Exception {
        Process process = new ProcessBuilder("su", "-c", command).start();
        int code = process.waitFor();
        if (code != 0) throw new IllegalStateException(command + " exited " + code);
    }
}
