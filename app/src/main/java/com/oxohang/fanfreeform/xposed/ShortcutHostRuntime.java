package com.oxohang.fanfreeform.xposed;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;

import com.oxohang.fanfreeform.config.ConfigContract;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

final class ShortcutHostRuntime {
    static final String MIUI_HOME = "com.miui.home";
    static final String ACTION_START_SHORTCUT = "com.oxohang.fanfreeform.START_SHORTCUT";
    static final String ACTION_START_ACTIVITY = "com.oxohang.fanfreeform.START_ACTIVITY";
    static final String EXTRA_PACKAGE = "package";
    static final String EXTRA_SHORTCUT_ID = "shortcut_id";
    static final String EXTRA_COMPONENT = "component";
    static final String EXTRA_USER_ID = "user_id";
    static final String EXTRA_OPTIONS = "options";
    private static volatile boolean installed;

    private ShortcutHostRuntime() { }

    static synchronized void install(Context context) {
        if (installed) return;
        Context appContext = context.getApplicationContext();
        if (appContext == null) {
            Log.i("Shortcut host waiting for MiuiHome application context");
            return;
        }
        IntentFilter filter = new IntentFilter(ACTION_START_SHORTCUT);
        filter.addAction(ACTION_START_ACTIVITY);
        appContext.registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context receiverContext, Intent intent) {
                if (ACTION_START_ACTIVITY.equals(intent.getAction())) {
                    startActivity(receiverContext, intent);
                } else {
                    startShortcut(receiverContext, intent);
                }
            }
        }, filter, Context.RECEIVER_EXPORTED);
        appContext.getContentResolver().registerContentObserver(ConfigContract.URI, false,
                new android.database.ContentObserver(new android.os.Handler(
                        android.os.Looper.getMainLooper())) {
                    @Override public void onChange(boolean selfChange) { publishCatalog(appContext); }
                });
        installed = true;
        publishCatalog(appContext);
        Log.i("Shortcut host active in MiuiHome");
    }

    private static void startShortcut(Context context, Intent intent) {
        if (!ACTION_START_SHORTCUT.equals(intent.getAction())) return;
        String packageName = intent.getStringExtra(EXTRA_PACKAGE);
        String shortcutId = intent.getStringExtra(EXTRA_SHORTCUT_ID);
        Bundle options = intent.getBundleExtra(EXTRA_OPTIONS);
        if (packageName == null || packageName.isEmpty() || shortcutId == null || shortcutId.isEmpty()) return;
        try {
            LauncherApps launcherApps = context.getSystemService(LauncherApps.class);
            if (launcherApps == null) throw new IllegalStateException("LauncherApps unavailable");
            launcherApps.startShortcut(packageName, shortcutId, null,
                    options == null ? Bundle.EMPTY : options, Process.myUserHandle());
            Log.i("MiuiHome started shortcut " + packageName + "/" + shortcutId);
        } catch (Throwable error) {
            Log.e("Cannot start shortcut in MiuiHome", error);
        }
    }

    private static void startActivity(Context context, Intent intent) {
        if (!ACTION_START_ACTIVITY.equals(intent.getAction())) return;
        ComponentName component = ComponentName.unflattenFromString(
                intent.getStringExtra(EXTRA_COMPONENT));
        int userId = intent.getIntExtra(EXTRA_USER_ID, 0);
        Bundle options = intent.getBundleExtra(EXTRA_OPTIONS);
        if (component == null || userId == 0) return;
        try {
            LauncherApps launcherApps = context.getSystemService(LauncherApps.class);
            if (launcherApps == null) throw new IllegalStateException("LauncherApps unavailable");
            launcherApps.startMainActivity(component, userHandle(userId), null,
                    options == null ? Bundle.EMPTY : options);
            Log.i("MiuiHome started activity " + component.flattenToShortString()
                    + " user=" + userId);
        } catch (Throwable error) {
            Log.e("Cannot start cross-user activity in MiuiHome", error);
        }
    }

    private static void publishCatalog(Context context) {
        try {
            LauncherApps launcherApps = context.getSystemService(LauncherApps.class);
            if (launcherApps == null) return;
            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery().setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                            | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                            | LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                            | LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED);
            List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle());
            if (shortcuts == null) shortcuts = Collections.emptyList();
            JSONArray out = new JSONArray();
            for (ShortcutInfo shortcut : shortcuts) {
                if (!shortcut.isEnabled() || out.length() >= 160) continue;
                CharSequence label = shortcut.getShortLabel();
                if (label == null) label = shortcut.getLongLabel();
                if (label == null) continue;
                JSONObject item = new JSONObject();
                item.put("package", shortcut.getPackage());
                item.put("id", shortcut.getId());
                item.put("label", label.toString());
                out.put(item);
            }
            Bundle extras = new Bundle();
            extras.putString(ConfigContract.KEY_SHORTCUT_CATALOG, out.toString());
            context.getContentResolver().call(ConfigContract.URI, "report_shortcuts", null, extras);
            publishActivityCatalog(context, launcherApps);
        } catch (Throwable error) {
            Log.e("Cannot publish shortcut catalog from MiuiHome", error);
        }
    }

    private static void publishActivityCatalog(Context context, LauncherApps launcherApps) {
        JSONArray out = new JSONArray();
        try {
            List<LauncherActivityInfo> activities = launcherApps.getActivityList(
                    null, userHandle(999));
            if (activities == null) activities = Collections.emptyList();
            for (LauncherActivityInfo activity : activities) {
                ComponentName component = activity.getComponentName();
                CharSequence label = activity.getLabel();
                if (component == null || label == null || out.length() >= 160) continue;
                JSONObject item = new JSONObject();
                item.put("component", component.flattenToString());
                item.put("label", label.toString());
                item.put("userId", 999);
                out.put(item);
            }
            Bundle extras = new Bundle();
            extras.putString(ConfigContract.KEY_ACTIVITY_CATALOG, out.toString());
            context.getContentResolver().call(ConfigContract.URI, "report_activities",
                    null, extras);
            Log.i("MiuiHome published dual-app activity catalog count=" + out.length());
        } catch (Throwable error) {
            Log.e("Cannot publish dual-app activity catalog from MiuiHome", error);
        }
    }

    private static UserHandle userHandle(int userId) {
        return UserHandle.getUserHandleForUid(userId * 100000);
    }
}
