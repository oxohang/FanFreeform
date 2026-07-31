package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;

import com.oxohang.fanfreeform.config.ConfigContract;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XposedBridge;

final class Log {
    private static final String PREFIX = "[FanFreeform] ";
    private static final Object LOCK = new Object();
    private static final ArrayDeque<String> RECENT = new ArrayDeque<>();
    private static final int MAX_RECENT = 100;
    private static final long INFO_PUBLISH_DELAY_MS = 5000L;
    private static final Runnable PUBLISH_TASK = Log::publish;
    private static final ThreadLocal<SimpleDateFormat> FORMATTER =
            new ThreadLocal<SimpleDateFormat>() {
                @Override protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
                }
            };
    private static Context reporterContext;
    private static Handler reporterHandler;
    private static boolean publishScheduled;
    private static long publishAtUptime;
    private Log() {}

    static void i(String message) {
        XposedBridge.log(PREFIX + message);
        remember("I", message, null, false);
    }

    static void e(String message, Throwable throwable) {
        XposedBridge.log(PREFIX + message + ": " + throwable);
        remember("E", message, throwable, true);
    }

    static void attachReporter(Context context) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        if (appContext == null) appContext = context;
        synchronized (LOCK) {
            reporterContext = appContext;
            reporterHandler = new Handler(appContext.getMainLooper());
            schedulePublishLocked(0L);
        }
    }

    private static void remember(String level, String message, Throwable throwable,
                                 boolean urgent) {
        StringBuilder line = new StringBuilder();
        line.append(FORMATTER.get().format(new Date())).append(' ').append(level).append(' ')
                .append(message == null ? "" : message);
        if (throwable != null) {
            line.append(" | ").append(throwable.getClass().getName());
            if (throwable.getMessage() != null && !throwable.getMessage().isEmpty()) {
                line.append(": ").append(throwable.getMessage());
            }
            StackTraceElement[] trace = throwable.getStackTrace();
            for (int i = 0; i < Math.min(4, trace.length); i++) {
                line.append("\n    at ").append(trace[i]);
            }
        }
        synchronized (LOCK) {
            RECENT.addLast(line.toString());
            while (RECENT.size() > MAX_RECENT) RECENT.removeFirst();
            schedulePublishLocked(urgent ? 0L : INFO_PUBLISH_DELAY_MS);
        }
    }

    private static void schedulePublishLocked(long delayMs) {
        if (reporterContext == null || reporterHandler == null) return;
        long target = SystemClock.uptimeMillis() + Math.max(0L, delayMs);
        if (publishScheduled && target >= publishAtUptime) return;
        if (publishScheduled) reporterHandler.removeCallbacks(PUBLISH_TASK);
        publishScheduled = true;
        publishAtUptime = target;
        reporterHandler.postDelayed(PUBLISH_TASK, Math.max(0L, delayMs));
    }

    private static void publish() {
        Context context;
        String text;
        synchronized (LOCK) {
            publishScheduled = false;
            publishAtUptime = 0L;
            context = reporterContext;
            StringBuilder snapshot = new StringBuilder();
            for (String line : RECENT) {
                if (snapshot.length() > 0) snapshot.append('\n');
                snapshot.append(line);
            }
            text = snapshot.toString();
        }
        if (context == null) return;
        try {
            Bundle extras = new Bundle();
            extras.putString(ConfigContract.EXTRA_DIAGNOSTIC_PROCESS,
                    context.getPackageName());
            extras.putString(ConfigContract.EXTRA_DIAGNOSTIC_TEXT, text);
            context.getContentResolver().call(ConfigContract.URI,
                    "report_diagnostics", null, extras);
        } catch (Throwable error) {
            XposedBridge.log(PREFIX + "Cannot publish compatibility diagnostics: " + error);
        }
    }
}
