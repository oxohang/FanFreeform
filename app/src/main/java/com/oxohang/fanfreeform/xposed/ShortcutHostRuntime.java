package com.oxohang.fanfreeform.xposed;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ShortcutIconLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ShortcutHostRuntime {
    static final String MIUI_HOME = "com.miui.home";
    static final String ACTION_START_SHORTCUT = "com.oxohang.fanfreeform.START_SHORTCUT";
    static final String ACTION_START_ACTIVITY = "com.oxohang.fanfreeform.START_ACTIVITY";
    static final String ACTION_START_LAUNCHER_SHORTCUT =
            "com.oxohang.fanfreeform.START_LAUNCHER_SHORTCUT";
    static final String EXTRA_PACKAGE = "package";
    static final String EXTRA_SHORTCUT_ID = "shortcut_id";
    static final String EXTRA_COMPONENT = "component";
    static final String EXTRA_USER_ID = "user_id";
    static final String EXTRA_OPTIONS = "options";
    static final String EXTRA_INTENT_URI = "intent_uri";
    private static volatile boolean installed;
    private static final Map<String, Long> publishedIconVersions = new HashMap<>();
    private static final Object CATALOG_LOCK = new Object();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ExecutorService CATALOG_EXECUTOR = Executors.newSingleThreadExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "hypergesture-shortcut-catalog");
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            });
    private static final long CATALOG_DEBOUNCE_MS = 350L;
    private static Context catalogContext;
    private static boolean catalogScheduled;
    private static boolean catalogRunning;
    private static boolean catalogPending;
    private static final Runnable START_CATALOG_REFRESH =
            ShortcutHostRuntime::startCatalogRefresh;

    private ShortcutHostRuntime() { }

    static synchronized void install(Context context) {
        if (installed) return;
        Context appContext = context.getApplicationContext();
        if (appContext == null) {
            Log.i("Shortcut host waiting for MiuiHome application context");
            return;
        }
        Log.attachReporter(appContext);
        RecentsClearButtonRuntime.install(appContext);
        IntentFilter filter = new IntentFilter(ACTION_START_SHORTCUT);
        filter.addAction(ACTION_START_ACTIVITY);
        filter.addAction(ACTION_START_LAUNCHER_SHORTCUT);
        appContext.registerReceiver(new BroadcastReceiver() {
            @Override public void onReceive(Context receiverContext, Intent intent) {
                if (ACTION_START_ACTIVITY.equals(intent.getAction())) {
                    startActivity(receiverContext, intent);
                } else if (ACTION_START_LAUNCHER_SHORTCUT.equals(intent.getAction())) {
                    startLauncherShortcut(receiverContext, intent);
                } else {
                    startShortcut(receiverContext, intent);
                }
            }
        }, filter, Context.RECEIVER_EXPORTED);
        appContext.getContentResolver().registerContentObserver(ConfigContract.URI, false,
                new android.database.ContentObserver(MAIN_HANDLER) {
                    @Override public void onChange(boolean selfChange) {
                        requestCatalogRefresh(appContext, CATALOG_DEBOUNCE_MS);
                    }
                });
        installed = true;
        requestCatalogRefresh(appContext, 0L);
        Log.i("Shortcut host active in MiuiHome");
    }

    private static void requestCatalogRefresh(Context context, long delayMs) {
        Context appContext = context.getApplicationContext();
        if (appContext == null) appContext = context;
        synchronized (CATALOG_LOCK) {
            catalogContext = appContext;
            if (catalogRunning) {
                catalogPending = true;
                return;
            }
            if (catalogScheduled) MAIN_HANDLER.removeCallbacks(START_CATALOG_REFRESH);
            catalogScheduled = true;
            MAIN_HANDLER.postDelayed(START_CATALOG_REFRESH, Math.max(0L, delayMs));
        }
    }

    private static void startCatalogRefresh() {
        Context context;
        synchronized (CATALOG_LOCK) {
            catalogScheduled = false;
            if (catalogRunning) {
                catalogPending = true;
                return;
            }
            context = catalogContext;
            if (context == null) return;
            catalogRunning = true;
        }
        Context refreshContext = context;
        CATALOG_EXECUTOR.execute(() -> {
            long started = SystemClock.elapsedRealtime();
            try {
                publishCatalog(refreshContext);
                long elapsed = SystemClock.elapsedRealtime() - started;
                Log.i("MiuiHome catalog refresh completed in " + elapsed + "ms");
            } catch (Throwable error) {
                Log.e("MiuiHome catalog refresh failed", error);
            } finally {
                MAIN_HANDLER.post(() -> finishCatalogRefresh(refreshContext));
            }
        });
    }

    private static void finishCatalogRefresh(Context context) {
        boolean refreshAgain;
        synchronized (CATALOG_LOCK) {
            catalogRunning = false;
            refreshAgain = catalogPending;
            catalogPending = false;
        }
        if (refreshAgain) requestCatalogRefresh(context, CATALOG_DEBOUNCE_MS);
    }

    private static void startLauncherShortcut(Context context, Intent request) {
        String expectedPackage = request.getStringExtra(EXTRA_PACKAGE);
        String intentUri = request.getStringExtra(EXTRA_INTENT_URI);
        Bundle options = request.getBundleExtra(EXTRA_OPTIONS);
        if (expectedPackage == null || expectedPackage.isEmpty()
                || intentUri == null || intentUri.isEmpty()) return;
        try {
            Intent launch = Intent.parseUri(intentUri, 0);
            String actualPackage = packageName(context, launch);
            if (!expectedPackage.equals(actualPackage)) {
                throw new SecurityException("Desktop shortcut package changed");
            }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launch, options == null ? Bundle.EMPTY : options);
            Log.i("MiuiHome started desktop shortcut package=" + expectedPackage);
        } catch (Throwable error) {
            Log.e("Cannot start desktop shortcut in MiuiHome", error);
        }
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
            Set<String> seen = new HashSet<>();
            ArrayList<String> iconKeys = new ArrayList<>();
            ArrayList<Bitmap> icons = new ArrayList<>();
            for (ShortcutInfo shortcut : shortcuts) {
                if (!shortcut.isEnabled()) continue;
                CharSequence label = shortcut.getShortLabel();
                if (label == null) label = shortcut.getLongLabel();
                if (label == null) continue;
                JSONObject item = new JSONObject();
                item.put("package", shortcut.getPackage());
                item.put("id", shortcut.getId());
                item.put("label", displayShortcutLabel(context, shortcut.getPackage(),
                        label.toString()));
                item.put("kind", "standard");
                out.put(item);
                seen.add(shortcut.getPackage() + "\n" + shortcut.getId());
                Drawable icon = launcherApps.getShortcutIconDrawable(shortcut,
                        context.getResources().getDisplayMetrics().densityDpi);
                queueShortcutIcon(context, iconKeys, icons,
                        ShortcutIconLoader.key(shortcut.getPackage(), shortcut.getId(), 0),
                        shortcut.getLastChangedTimestamp(), icon);
            }
            appendLauncherShortcuts(context, out, seen, iconKeys, icons);
            flushShortcutIcons(context, iconKeys, icons);
            Bundle extras = new Bundle();
            extras.putString(ConfigContract.KEY_SHORTCUT_CATALOG, out.toString());
            context.getContentResolver().call(ConfigContract.URI, "report_shortcuts", null, extras);
            publishActivityCatalog(context, launcherApps);
        } catch (Throwable error) {
            Log.e("Cannot publish shortcut catalog from MiuiHome", error);
        }
    }

    private static void appendLauncherShortcuts(Context context, JSONArray out,
                                                Set<String> seen,
                                                ArrayList<String> iconKeys,
                                                ArrayList<Bitmap> icons) {
        Uri favorites = Uri.parse(
                "content://com.miui.home.launcher.settings/favorites");
        int added = 0;
        try (Cursor cursor = context.getContentResolver().query(
                favorites, null, null, null, null)) {
            if (cursor == null) return;
            int idColumn = cursor.getColumnIndex("_id");
            int titleColumn = cursor.getColumnIndex("title");
            int intentColumn = cursor.getColumnIndex("intent");
            int typeColumn = cursor.getColumnIndex("itemType");
            int shortcutColumn = cursor.getColumnIndex("isShortcut");
            int iconColumn = cursor.getColumnIndex("icon");
            while (cursor.moveToNext() && out.length() < 240) {
                int itemType = typeColumn >= 0 && !cursor.isNull(typeColumn)
                        ? cursor.getInt(typeColumn) : -1;
                int isShortcut = shortcutColumn >= 0 && !cursor.isNull(shortcutColumn)
                        ? cursor.getInt(shortcutColumn) : 0;
                if (itemType != 1 && itemType != 14 && isShortcut == 0) continue;
                if (intentColumn < 0 || cursor.isNull(intentColumn)) continue;
                String rawIntent = cursor.getString(intentColumn);
                if (rawIntent == null || rawIntent.isEmpty()) continue;
                Intent launch;
                try {
                    launch = Intent.parseUri(rawIntent, 0);
                } catch (Throwable ignored) {
                    continue;
                }
                String packageName = packageName(context, launch);
                if (packageName == null || packageName.isEmpty()) continue;
                String label = titleColumn >= 0 && !cursor.isNull(titleColumn)
                        ? cursor.getString(titleColumn) : packageName;
                if (label == null || label.isEmpty()) label = packageName;
                String shortcutId = launch.getStringExtra("shortcut_id");
                if (shortcutId == null || shortcutId.isEmpty()) {
                    long rowId = idColumn >= 0 && !cursor.isNull(idColumn)
                            ? cursor.getLong(idColumn) : rawIntent.hashCode();
                    shortcutId = "launcher:" + rowId;
                }
                String key = packageName + "\n" + shortcutId;
                if (!seen.add(key)) continue;
                try {
                    context.getPackageManager().getApplicationInfo(packageName, 0);
                    JSONObject item = new JSONObject();
                    item.put("package", packageName);
                    item.put("id", shortcutId);
                    item.put("label", displayShortcutLabel(context, packageName, label));
                    item.put("kind", "launcher");
                    item.put("intent", rawIntent);
                    item.put("userId", 0);
                    out.put(item);
                    if (iconColumn >= 0 && !cursor.isNull(iconColumn)) {
                        byte[] encoded = cursor.getBlob(iconColumn);
                        Bitmap icon = encoded == null ? null : BitmapFactory.decodeByteArray(
                                encoded, 0, encoded.length);
                        queueShortcutIcon(context, iconKeys, icons,
                                ShortcutIconLoader.key(packageName, shortcutId, 0),
                                encoded == null ? 0L : java.util.Arrays.hashCode(encoded), icon);
                    }
                    added++;
                } catch (Throwable ignored) { }
            }
            Log.i("MiuiHome published desktop shortcut catalog added=" + added);
        } catch (Throwable error) {
            Log.e("Cannot read MiuiHome desktop shortcut catalog", error);
        }
    }

    private static void queueShortcutIcon(Context context, ArrayList<String> keys,
                                          ArrayList<Bitmap> icons, String key, long version,
                                          Drawable drawable) {
        if (drawable == null) return;
        Long previous = publishedIconVersions.get(key);
        if (previous != null && previous == version) return;
        int intrinsic = Math.max(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        int size = Math.max(96, Math.min(144, intrinsic));
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        queueShortcutIcon(context, keys, icons, key, version, bitmap);
    }

    private static void queueShortcutIcon(Context context, ArrayList<String> keys,
                                          ArrayList<Bitmap> icons, String key, long version,
                                          Bitmap bitmap) {
        if (bitmap == null || key == null || key.isEmpty()) return;
        Long previous = publishedIconVersions.get(key);
        if (previous != null && previous == version) return;
        publishedIconVersions.put(key, version);
        keys.add(key);
        icons.add(bitmap);
        if (keys.size() >= 6) flushShortcutIcons(context, keys, icons);
    }

    private static void flushShortcutIcons(Context context, ArrayList<String> keys,
                                           ArrayList<Bitmap> icons) {
        if (keys.isEmpty()) return;
        Bundle extras = new Bundle();
        extras.putStringArrayList(ShortcutIconLoader.EXTRA_KEYS,
                new ArrayList<>(keys));
        extras.putParcelableArrayList(ShortcutIconLoader.EXTRA_ICONS,
                new ArrayList<>(icons));
        context.getContentResolver().call(ConfigContract.URI,
                ShortcutIconLoader.METHOD_REPORT, null, extras);
        keys.clear();
        icons.clear();
    }

    private static String packageName(Context context, Intent intent) {
        if (intent.getPackage() != null && !intent.getPackage().isEmpty()) {
            return intent.getPackage();
        }
        if (intent.getComponent() != null) return intent.getComponent().getPackageName();
        try {
            android.content.pm.ResolveInfo resolved = context.getPackageManager()
                    .resolveActivity(intent, 0);
            return resolved == null || resolved.activityInfo == null
                    ? null : resolved.activityInfo.packageName;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String displayShortcutLabel(Context context, String packageName,
                                               String shortcutLabel) {
        String shortcut = shortcutLabel == null ? "" : shortcutLabel.trim();
        String app = "";
        try {
            android.content.pm.ApplicationInfo info = context.getPackageManager()
                    .getApplicationInfo(packageName, 0);
            CharSequence label = context.getPackageManager().getApplicationLabel(info);
            if (label != null) app = label.toString().trim();
        } catch (Throwable ignored) { }
        if (app.isEmpty()) app = packageName == null ? "" : packageName;
        if (shortcut.isEmpty()) return app;
        if (app.isEmpty() || shortcut.startsWith(app)) return shortcut;
        return app + " · " + shortcut;
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
