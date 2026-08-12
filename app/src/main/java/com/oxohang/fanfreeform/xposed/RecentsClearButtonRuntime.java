package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.oxohang.fanfreeform.config.ConfigContract;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import de.robv.android.xposed.XposedHelpers;

/** Keeps HyperOS' native recents clear-all button in sync with the module setting. */
final class RecentsClearButtonRuntime {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Runnable RELOAD_TASK = RecentsClearButtonRuntime::reload;
    private static final Set<View> CLEAR_BUTTONS = Collections.newSetFromMap(
            new WeakHashMap<>());
    private static volatile Context appContext;
    private static volatile boolean installed;
    private static volatile boolean hidden;

    private RecentsClearButtonRuntime() { }

    static synchronized void install(Context context) {
        Context application = context.getApplicationContext();
        if (application == null || installed) return;
        appContext = application;
        application.getContentResolver().registerContentObserver(ConfigContract.RUNTIME_URI, false,
                new ContentObserver(MAIN) {
                    @Override public void onChange(boolean selfChange) {
                        MAIN.removeCallbacks(RELOAD_TASK);
                        MAIN.postDelayed(RELOAD_TASK, 150L);
                    }
                });
        installed = true;
        reload();
    }

    static void attach(Object recentsContainer) {
        if (!(recentsContainer instanceof View)) return;
        View button = findClearButton((View) recentsContainer, recentsContainer);
        if (button == null) {
            Log.i("HyperOS recents clear button was not found in this launcher build");
            return;
        }
        synchronized (CLEAR_BUTTONS) {
            CLEAR_BUTTONS.add(button);
        }
        apply(button);
    }

    private static View findClearButton(View containerView, Object container) {
        String[] fields = {"mClearAnimView", "mClearAllButton", "mClearButton"};
        for (String field : fields) {
            try {
                Object candidate = XposedHelpers.getObjectField(container, field);
                if (candidate instanceof View) return (View) candidate;
            } catch (Throwable ignored) { }
        }
        try {
            int id = containerView.getResources().getIdentifier(
                    "clearAnimView", "id", containerView.getContext().getPackageName());
            return id == 0 ? null : containerView.findViewById(id);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void reload() {
        Context context = appContext;
        if (context == null) return;
        try {
            Bundle state = context.getContentResolver().call(
                    ConfigContract.URI, "get", null, null);
            hidden = state != null && state.getBoolean(
                    ConfigContract.KEY_HIDE_SYSTEM_RECENTS_CLEAR,
                    ConfigContract.DEFAULT_HIDE_SYSTEM_RECENTS_CLEAR);
            MAIN.post(RecentsClearButtonRuntime::applyAll);
            Log.i("HyperOS recents clear button hidden=" + hidden);
        } catch (Throwable error) {
            Log.e("Cannot read recents clear-button setting", error);
        }
    }

    private static void applyAll() {
        synchronized (CLEAR_BUTTONS) {
            for (View button : CLEAR_BUTTONS) apply(button);
        }
    }

    private static void apply(View button) {
        if (button == null) return;
        button.animate().cancel();
        button.setVisibility(hidden ? View.GONE : View.VISIBLE);
        button.setClickable(!hidden);
        button.setImportantForAccessibility(hidden
                ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }
}
