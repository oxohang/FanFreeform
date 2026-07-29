package com.oxohang.fanfreeform.config;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;

public final class ConfigProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        enforceCaller();
        Context context = requireProviderContext();
        SharedPreferences prefs = context.getSharedPreferences(ConfigContract.PREFS, Context.MODE_PRIVATE);
        if ("get".equals(method)) {
            ConfigStore.seedDefaultsIfNeeded(context, prefs);
            ConfigStore.migrateUnifiedActionsIfNeeded(prefs);
            ConfigStore.migrateSideLayoutModeIfNeeded(prefs);
            ConfigStore.migrateSelectedAppNameIfNeeded(prefs);
            ConfigStore.migrateIndependentSideTargetsIfNeeded(prefs);
            Bundle out = new Bundle();
            out.putBoolean(ConfigContract.KEY_ENABLED, prefs.getBoolean(ConfigContract.KEY_ENABLED, ConfigContract.DEFAULT_ENABLED));
            out.putBoolean(ConfigContract.KEY_HAPTIC, prefs.getBoolean(ConfigContract.KEY_HAPTIC, ConfigContract.DEFAULT_HAPTIC));
            out.putBoolean(ConfigContract.KEY_FAN_SHADOW, prefs.getBoolean(ConfigContract.KEY_FAN_SHADOW, ConfigContract.DEFAULT_FAN_SHADOW));
            out.putBoolean(ConfigContract.KEY_FAN_ANIMATIONS_ENABLED, prefs.getBoolean(
                    ConfigContract.KEY_FAN_ANIMATIONS_ENABLED,
                    ConfigContract.DEFAULT_FAN_ANIMATIONS_ENABLED));
            out.putInt(ConfigContract.KEY_FAN_ANIMATION_SPEED, Math.max(
                    ConfigContract.MIN_FAN_ANIMATION_SPEED, Math.min(
                            ConfigContract.MAX_FAN_ANIMATION_SPEED, prefs.getInt(
                                    ConfigContract.KEY_FAN_ANIMATION_SPEED,
                                    ConfigContract.DEFAULT_FAN_ANIMATION_SPEED))));
            out.putInt(ConfigContract.KEY_FAN_REVEAL_AMOUNT, Math.max(
                    ConfigContract.MIN_FAN_REVEAL_AMOUNT, Math.min(
                            ConfigContract.MAX_FAN_REVEAL_AMOUNT, prefs.getInt(
                                    ConfigContract.KEY_FAN_REVEAL_AMOUNT,
                                    ConfigContract.DEFAULT_FAN_REVEAL_AMOUNT))));
            out.putInt(ConfigContract.KEY_FAN_ROTATION_DEGREES, Math.max(
                    ConfigContract.MIN_FAN_ROTATION_DEGREES, Math.min(
                            ConfigContract.MAX_FAN_ROTATION_DEGREES, prefs.getInt(
                                    ConfigContract.KEY_FAN_ROTATION_DEGREES,
                                    ConfigContract.DEFAULT_FAN_ROTATION_DEGREES))));
            out.putInt(ConfigContract.KEY_FAN_SELECTION_SCALE_PERCENT, Math.max(
                    ConfigContract.MIN_FAN_SELECTION_SCALE_PERCENT, Math.min(
                            ConfigContract.MAX_FAN_SELECTION_SCALE_PERCENT, prefs.getInt(
                                    ConfigContract.KEY_FAN_SELECTION_SCALE_PERCENT,
                                    ConfigContract.DEFAULT_FAN_SELECTION_SCALE_PERCENT))));
            out.putBoolean(ConfigContract.KEY_FAN_SELECTION_RING, prefs.getBoolean(
                    ConfigContract.KEY_FAN_SELECTION_RING,
                    ConfigContract.DEFAULT_FAN_SELECTION_RING));
            out.putBoolean(ConfigContract.KEY_FAN_FIXED_SEVEN_ROWS, prefs.getBoolean(
                    ConfigContract.KEY_FAN_FIXED_SEVEN_ROWS,
                    ConfigContract.DEFAULT_FAN_FIXED_SEVEN_ROWS));
            out.putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_ENABLED, prefs.getBoolean(
                    ConfigContract.KEY_BOTTOM_PORTRAIT_ENABLED,
                    ConfigContract.DEFAULT_BOTTOM_PORTRAIT_ENABLED));
            out.putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_ENABLED, prefs.getBoolean(
                    ConfigContract.KEY_BOTTOM_LANDSCAPE_ENABLED,
                    ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_ENABLED));
            out.putBoolean(ConfigContract.KEY_SIDE_GESTURE_ENABLED, prefs.getBoolean(ConfigContract.KEY_SIDE_GESTURE_ENABLED, ConfigContract.DEFAULT_SIDE_GESTURE_ENABLED));
            out.putBoolean(ConfigContract.KEY_SIDE_PORTRAIT_ENABLED, prefs.getBoolean(
                    ConfigContract.KEY_SIDE_PORTRAIT_ENABLED,
                    ConfigContract.DEFAULT_SIDE_PORTRAIT_ENABLED));
            out.putBoolean(ConfigContract.KEY_SIDE_LANDSCAPE_ENABLED, prefs.getBoolean(
                    ConfigContract.KEY_SIDE_LANDSCAPE_ENABLED,
                    ConfigContract.DEFAULT_SIDE_LANDSCAPE_ENABLED));
            out.putInt(ConfigContract.KEY_SIDE_TRIGGER_PERCENT, prefs.getInt(ConfigContract.KEY_SIDE_TRIGGER_PERCENT, ConfigContract.DEFAULT_SIDE_TRIGGER_PERCENT));
            out.putInt(ConfigContract.KEY_SIDE_ICON_SIZE_DP, prefs.getInt(
                    ConfigContract.KEY_SIDE_ICON_SIZE_DP,
                    prefs.getInt(ConfigContract.KEY_ICON_SIZE_DP,
                            ConfigContract.DEFAULT_SIDE_ICON_SIZE_DP)));
            out.putInt(ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT, prefs.getInt(
                    ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT,
                    ConfigContract.DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT));
            out.putBoolean(ConfigContract.KEY_SHOW_SELECTED_APP_NAME, prefs.getBoolean(
                    ConfigContract.KEY_SHOW_SELECTED_APP_NAME,
                    ConfigContract.DEFAULT_SHOW_SELECTED_APP_NAME));
            out.putBoolean(ConfigContract.KEY_SIDE_FOLLOW_FINGER, prefs.getBoolean(
                    ConfigContract.KEY_SIDE_FOLLOW_FINGER,
                    ConfigContract.DEFAULT_SIDE_FOLLOW_FINGER));
            out.putBoolean(ConfigContract.KEY_SIDE_FAN_LIST, prefs.getBoolean(
                    ConfigContract.KEY_SIDE_FAN_LIST,
                    ConfigContract.DEFAULT_SIDE_FAN_LIST));
            out.putInt(ConfigContract.KEY_SIDE_LAYOUT_MODE, Math.max(
                    ConfigContract.SIDE_LAYOUT_LIST, Math.min(
                            ConfigContract.SIDE_LAYOUT_HONEYCOMB, prefs.getInt(
                                    ConfigContract.KEY_SIDE_LAYOUT_MODE,
                                    ConfigContract.DEFAULT_SIDE_LAYOUT_MODE))));
            out.putInt(ConfigContract.KEY_SIDE_RING_SIZE_PERCENT, Math.max(
                    ConfigContract.MIN_SIDE_RING_SIZE_PERCENT, Math.min(
                            ConfigContract.MAX_SIDE_RING_SIZE_PERCENT, prefs.getInt(
                                    ConfigContract.KEY_SIDE_RING_SIZE_PERCENT,
                                    ConfigContract.DEFAULT_SIDE_RING_SIZE_PERCENT))));
            out.putBoolean(ConfigContract.KEY_SIDE_WHEEL_MODE, prefs.getBoolean(
                    ConfigContract.KEY_SIDE_WHEEL_MODE,
                    ConfigContract.DEFAULT_SIDE_WHEEL_MODE));
            out.putInt(ConfigContract.KEY_SIDE_REVERSE_CANCEL_PERCENT,
                    Math.max(ConfigContract.MIN_SIDE_REVERSE_CANCEL_PERCENT,
                            Math.min(ConfigContract.MAX_SIDE_REVERSE_CANCEL_PERCENT,
                                    prefs.getInt(ConfigContract.KEY_SIDE_REVERSE_CANCEL_PERCENT,
                                            ConfigContract.DEFAULT_SIDE_REVERSE_CANCEL_PERCENT))));
            out.putInt(ConfigContract.KEY_TRIGGER_PERCENT, prefs.getInt(ConfigContract.KEY_TRIGGER_PERCENT, ConfigContract.DEFAULT_TRIGGER_PERCENT));
            out.putInt(ConfigContract.KEY_SELECTION_RADIUS_PERCENT, prefs.getInt(ConfigContract.KEY_SELECTION_RADIUS_PERCENT, ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT));
            out.putInt(ConfigContract.KEY_HOT_WIDTH_PERCENT, prefs.getInt(ConfigContract.KEY_HOT_WIDTH_PERCENT, ConfigContract.DEFAULT_HOT_WIDTH_PERCENT));
            out.putInt(ConfigContract.KEY_HOT_HEIGHT_PERCENT, prefs.getInt(ConfigContract.KEY_HOT_HEIGHT_PERCENT, ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT));
            out.putInt(ConfigContract.KEY_ICON_SIZE_DP, prefs.getInt(ConfigContract.KEY_ICON_SIZE_DP, ConfigContract.DEFAULT_ICON_SIZE_DP));
            out.putInt(ConfigContract.KEY_WIDTH_PERCENT, prefs.getInt(ConfigContract.KEY_WIDTH_PERCENT, ConfigContract.DEFAULT_WIDTH_PERCENT));
            out.putInt(ConfigContract.KEY_HEIGHT_PERCENT, prefs.getInt(ConfigContract.KEY_HEIGHT_PERCENT, ConfigContract.DEFAULT_HEIGHT_PERCENT));
            out.putInt(ConfigContract.KEY_POSITION_X, prefs.getInt(ConfigContract.KEY_POSITION_X, ConfigContract.DEFAULT_POSITION_X));
            out.putInt(ConfigContract.KEY_POSITION_Y, prefs.getInt(ConfigContract.KEY_POSITION_Y, ConfigContract.DEFAULT_POSITION_Y));
            out.putInt(ConfigContract.KEY_LANDSCAPE_WIDTH_PERCENT, prefs.getInt(
                    ConfigContract.KEY_LANDSCAPE_WIDTH_PERCENT,
                    ConfigContract.DEFAULT_LANDSCAPE_WIDTH_PERCENT));
            out.putInt(ConfigContract.KEY_LANDSCAPE_HEIGHT_PERCENT, prefs.getInt(
                    ConfigContract.KEY_LANDSCAPE_HEIGHT_PERCENT,
                    ConfigContract.DEFAULT_LANDSCAPE_HEIGHT_PERCENT));
            out.putInt(ConfigContract.KEY_LANDSCAPE_POSITION_X, prefs.getInt(
                    ConfigContract.KEY_LANDSCAPE_POSITION_X,
                    ConfigContract.DEFAULT_LANDSCAPE_POSITION_X));
            out.putInt(ConfigContract.KEY_LANDSCAPE_POSITION_Y, prefs.getInt(
                    ConfigContract.KEY_LANDSCAPE_POSITION_Y,
                    ConfigContract.DEFAULT_LANDSCAPE_POSITION_Y));
            out.putInt(ConfigContract.KEY_OUTSIDE_SINGLE_ACTION, prefs.getInt(ConfigContract.KEY_OUTSIDE_SINGLE_ACTION, ConfigContract.DEFAULT_OUTSIDE_SINGLE_ACTION));
            out.putInt(ConfigContract.KEY_OUTSIDE_DOUBLE_ACTION, prefs.getInt(ConfigContract.KEY_OUTSIDE_DOUBLE_ACTION, ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION));
            out.putInt(ConfigContract.KEY_OUTSIDE_TAP_WINDOW_MS, clamp(prefs.getInt(
                    ConfigContract.KEY_OUTSIDE_TAP_WINDOW_MS,
                    ConfigContract.DEFAULT_OUTSIDE_TAP_WINDOW_MS),
                    ConfigContract.MIN_OUTSIDE_TAP_WINDOW_MS,
                    ConfigContract.MAX_OUTSIDE_TAP_WINDOW_MS));
            out.putString(ConfigContract.KEY_COMPONENTS, prefs.getString(ConfigContract.KEY_COMPONENTS, "[]"));
            out.putBoolean(ConfigContract.KEY_HONEYCOMB_ENABLED, prefs.getBoolean(
                    ConfigContract.KEY_HONEYCOMB_ENABLED,
                    ConfigContract.DEFAULT_HONEYCOMB_ENABLED));
            out.putString(ConfigContract.KEY_HONEYCOMB_COMPONENTS, prefs.getString(
                    ConfigContract.KEY_HONEYCOMB_COMPONENTS, "[]"));
            out.putInt(ConfigContract.KEY_HONEYCOMB_MODE, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_MODE,
                    ConfigContract.DEFAULT_HONEYCOMB_MODE), 0, 1));
            out.putInt(ConfigContract.KEY_HONEYCOMB_TRIGGER_DP, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_TRIGGER_DP,
                    ConfigContract.DEFAULT_HONEYCOMB_TRIGGER_DP),
                    ConfigContract.MIN_HONEYCOMB_TRIGGER_DP,
                    ConfigContract.MAX_HONEYCOMB_TRIGGER_DP));
            out.putInt(ConfigContract.KEY_HONEYCOMB_ICON_SIZE_DP, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_ICON_SIZE_DP,
                    ConfigContract.DEFAULT_HONEYCOMB_ICON_SIZE_DP),
                    ConfigContract.MIN_HONEYCOMB_ICON_SIZE_DP,
                    ConfigContract.MAX_HONEYCOMB_ICON_SIZE_DP));
            out.putInt(ConfigContract.KEY_HONEYCOMB_SPACING_DP, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_SPACING_DP,
                    ConfigContract.DEFAULT_HONEYCOMB_SPACING_DP),
                    ConfigContract.MIN_HONEYCOMB_SPACING_DP,
                    ConfigContract.MAX_HONEYCOMB_SPACING_DP));
            out.putInt(ConfigContract.KEY_HONEYCOMB_ANIMATION_SPEED, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_ANIMATION_SPEED,
                    ConfigContract.DEFAULT_HONEYCOMB_ANIMATION_SPEED), 0, 4));
            out.putInt(ConfigContract.KEY_HONEYCOMB_INERTIA, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_INERTIA,
                    ConfigContract.DEFAULT_HONEYCOMB_INERTIA), 0, 2));
            out.putInt(ConfigContract.KEY_HONEYCOMB_CENTER_SCALE, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_CENTER_SCALE,
                    ConfigContract.DEFAULT_HONEYCOMB_CENTER_SCALE),
                    ConfigContract.MIN_HONEYCOMB_CENTER_SCALE,
                    ConfigContract.MAX_HONEYCOMB_CENTER_SCALE));
            out.putInt(ConfigContract.KEY_HONEYCOMB_EDGE_SCALE, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_EDGE_SCALE,
                    ConfigContract.DEFAULT_HONEYCOMB_EDGE_SCALE),
                    ConfigContract.MIN_HONEYCOMB_EDGE_SCALE,
                    ConfigContract.MAX_HONEYCOMB_EDGE_SCALE));
            out.putInt(ConfigContract.KEY_HONEYCOMB_SELECTION_SCALE, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_SELECTION_SCALE,
                    ConfigContract.DEFAULT_HONEYCOMB_SELECTION_SCALE),
                    ConfigContract.MIN_HONEYCOMB_SELECTION_SCALE,
                    ConfigContract.MAX_HONEYCOMB_SELECTION_SCALE));
            out.putBoolean(ConfigContract.KEY_HONEYCOMB_SHOW_SELECTED_NAME,
                    prefs.getBoolean(ConfigContract.KEY_HONEYCOMB_SHOW_SELECTED_NAME,
                            ConfigContract.DEFAULT_HONEYCOMB_SHOW_SELECTED_NAME));
            out.putBoolean(ConfigContract.KEY_HONEYCOMB_EMPTY_TAP_CLOSE,
                    prefs.getBoolean(ConfigContract.KEY_HONEYCOMB_EMPTY_TAP_CLOSE,
                            ConfigContract.DEFAULT_HONEYCOMB_EMPTY_TAP_CLOSE));
            out.putInt(ConfigContract.KEY_FAN_MAX_TARGETS, clamp(prefs.getInt(
                    ConfigContract.KEY_FAN_MAX_TARGETS,
                    ConfigContract.DEFAULT_FAN_MAX_TARGETS), 3, 24));
            out.putInt(ConfigContract.KEY_HONEYCOMB_MAX_TARGETS, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_MAX_TARGETS,
                    ConfigContract.DEFAULT_HONEYCOMB_MAX_TARGETS), 1, 60));
            out.putString(ConfigContract.KEY_SIDE_COMPONENTS, prefs.getString(
                    ConfigContract.KEY_SIDE_COMPONENTS, "[]"));
            out.putBoolean(ConfigContract.KEY_SIDE_FOLLOW_HONEYCOMB, prefs.getBoolean(
                    ConfigContract.KEY_SIDE_FOLLOW_HONEYCOMB,
                    ConfigContract.DEFAULT_SIDE_FOLLOW_HONEYCOMB));
            out.putInt(ConfigContract.KEY_SIDE_MAX_TARGETS, clamp(prefs.getInt(
                    ConfigContract.KEY_SIDE_MAX_TARGETS,
                    ConfigContract.DEFAULT_SIDE_MAX_TARGETS), 1, 36));
            out.putBoolean(ConfigContract.KEY_SIDE_DIRECTION_HORIZONTAL, prefs.getBoolean(
                    ConfigContract.KEY_SIDE_DIRECTION_HORIZONTAL, true));
            out.putBoolean(ConfigContract.KEY_SIDE_DIRECTION_UP, prefs.getBoolean(
                    ConfigContract.KEY_SIDE_DIRECTION_UP, true));
            out.putBoolean(ConfigContract.KEY_SIDE_DIRECTION_DOWN, prefs.getBoolean(
                    ConfigContract.KEY_SIDE_DIRECTION_DOWN, true));
            out.putBoolean(ConfigContract.KEY_SIDE_HONEYCOMB_FULLSCREEN, prefs.getBoolean(
                    ConfigContract.KEY_SIDE_HONEYCOMB_FULLSCREEN, false));
            out.putBoolean(ConfigContract.KEY_HONEYCOMB_FOLLOW_FINGER, prefs.getBoolean(
                    ConfigContract.KEY_HONEYCOMB_FOLLOW_FINGER,
                    ConfigContract.DEFAULT_HONEYCOMB_FOLLOW_FINGER));
            out.putBoolean(ConfigContract.KEY_HONEYCOMB_LANDSCAPE_ENABLED,
                    prefs.getBoolean(ConfigContract.KEY_HONEYCOMB_LANDSCAPE_ENABLED,
                            ConfigContract.DEFAULT_HONEYCOMB_LANDSCAPE_ENABLED));
            out.putInt(ConfigContract.KEY_HONEYCOMB_FIXED_X_PERCENT, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_FIXED_X_PERCENT,
                    ConfigContract.DEFAULT_HONEYCOMB_FIXED_X_PERCENT), 0, 100));
            out.putInt(ConfigContract.KEY_HONEYCOMB_FIXED_Y_PERCENT, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_FIXED_Y_PERCENT,
                    ConfigContract.DEFAULT_HONEYCOMB_FIXED_Y_PERCENT), 0, 100));
            out.putInt(ConfigContract.KEY_HONEYCOMB_BACKGROUND_STYLE, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_BACKGROUND_STYLE, 0), 0, 1));
            out.putInt(ConfigContract.KEY_HONEYCOMB_BLUR_DP, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_BLUR_DP, 36), 0, 60));
            out.putInt(ConfigContract.KEY_HONEYCOMB_DIM_PERCENT, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_DIM_PERCENT, 22), 0, 60));
            out.putInt(ConfigContract.KEY_HONEYCOMB_RETREAT_DP, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_RETREAT_DP,
                    ConfigContract.DEFAULT_HONEYCOMB_RETREAT_DP),
                    ConfigContract.MIN_HONEYCOMB_RETREAT_DP,
                    ConfigContract.MAX_HONEYCOMB_RETREAT_DP));
            out.putInt(ConfigContract.KEY_HONEYCOMB_DISC_SIZE_PERCENT, clamp(prefs.getInt(
                    ConfigContract.KEY_HONEYCOMB_DISC_SIZE_PERCENT,
                    ConfigContract.DEFAULT_HONEYCOMB_DISC_SIZE_PERCENT),
                    ConfigContract.MIN_HONEYCOMB_DISC_SIZE_PERCENT,
                    ConfigContract.MAX_HONEYCOMB_DISC_SIZE_PERCENT));
            return out;
        }
        if ("report".equals(method) && extras != null) {
            prefs.edit()
                    .putString(ConfigContract.KEY_INTERFACE_STATUS, extras.getString(ConfigContract.KEY_INTERFACE_STATUS, ""))
                    .putLong(ConfigContract.KEY_INTERFACE_TIME, System.currentTimeMillis())
                    .apply();
            context.getContentResolver().notifyChange(ConfigContract.URI, null);
            return Bundle.EMPTY;
        }
        if ("report_shortcuts".equals(method) && extras != null) {
            String catalog = extras.getString(ConfigContract.KEY_SHORTCUT_CATALOG, "[]");
            if (!catalog.equals(prefs.getString(ConfigContract.KEY_SHORTCUT_CATALOG, "[]"))) {
                prefs.edit().putString(ConfigContract.KEY_SHORTCUT_CATALOG, catalog).apply();
                context.getContentResolver().notifyChange(ConfigContract.URI, null);
            }
            return Bundle.EMPTY;
        }
        if ("report_activities".equals(method) && extras != null) {
            String catalog = extras.getString(ConfigContract.KEY_ACTIVITY_CATALOG, "[]");
            if (!catalog.equals(prefs.getString(ConfigContract.KEY_ACTIVITY_CATALOG, "[]"))) {
                prefs.edit().putString(ConfigContract.KEY_ACTIVITY_CATALOG, catalog).apply();
                context.getContentResolver().notifyChange(ConfigContract.URI, null);
            }
            return Bundle.EMPTY;
        }
        return super.call(method, arg, extras);
    }

    private Context requireProviderContext() {
        Context context = getContext();
        if (context == null) {
            throw new IllegalStateException("Provider context is unavailable");
        }
        return context;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void enforceCaller() {
        Context context = requireProviderContext();
        int uid = Binder.getCallingUid();
        if (uid == Process.myUid() || uid == Process.SYSTEM_UID) {
            return;
        }
        String[] packages = context.getPackageManager().getPackagesForUid(uid);
        if (packages != null) {
            for (String packageName : packages) {
                if ("com.android.systemui".equals(packageName)
                        || "com.miui.home".equals(packageName)) {
                    return;
                }
            }
        }
        throw new SecurityException("Caller cannot read FanFreeform configuration");
    }

    @Override public String getType(Uri uri) { return null; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { throw new UnsupportedOperationException(); }
}
