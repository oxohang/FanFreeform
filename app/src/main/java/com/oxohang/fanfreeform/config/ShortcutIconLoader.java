package com.oxohang.fanfreeform.config;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;

import java.util.Collections;
import java.util.List;

public final class ShortcutIconLoader {
    public static final String METHOD_GET = "get_shortcut_icon";
    public static final String METHOD_REPORT = "report_shortcut_icons";
    public static final String EXTRA_KEYS = "shortcut_icon_keys";
    public static final String EXTRA_ICONS = "shortcut_icons";
    public static final String EXTRA_ICON = "shortcut_icon";

    private ShortcutIconLoader() { }

    public static Drawable load(Context context, String packageName, String shortcutId,
                                int userId) throws PackageManager.NameNotFoundException {
        PackageManager packageManager = context.getPackageManager();
        try {
            LauncherApps launcherApps = context.getSystemService(LauncherApps.class);
            if (launcherApps != null && shortcutId != null && !shortcutId.isEmpty()) {
                LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery()
                        .setPackage(packageName)
                        .setShortcutIds(Collections.singletonList(shortcutId))
                        .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                                | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                                | LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                                | LauncherApps.ShortcutQuery.FLAG_MATCH_CACHED);
                List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(
                        query, userId <= 0 ? Process.myUserHandle()
                                : UserHandle.getUserHandleForUid(userId * 100000));
                if (shortcuts != null && !shortcuts.isEmpty()) {
                    Drawable icon = launcherApps.getShortcutIconDrawable(shortcuts.get(0),
                            context.getResources().getDisplayMetrics().densityDpi);
                    if (icon != null) return icon;
                }
            }
        } catch (Throwable ignored) {
            // Most module processes are not registered shortcut hosts; try launcher cache.
        }
        try {
            Bundle response = context.getContentResolver().call(ConfigContract.URI,
                    METHOD_GET, key(packageName, shortcutId, userId), null);
            if (response != null) {
                @SuppressWarnings("deprecation")
                Bitmap bitmap = response.getParcelable(EXTRA_ICON);
                if (bitmap != null) {
                    return new BitmapDrawable(context.getResources(), bitmap);
                }
            }
        } catch (Throwable ignored) {
            // The launcher cache may not have been populated yet.
        }
        ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
        return appInfo.loadIcon(packageManager);
    }

    public static String key(String packageName, String shortcutId, int userId) {
        return (packageName == null ? "" : packageName) + "\n"
                + (shortcutId == null ? "" : shortcutId) + "\n" + Math.max(0, userId);
    }
}
