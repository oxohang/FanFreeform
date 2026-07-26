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
            Bundle out = new Bundle();
            out.putBoolean(ConfigContract.KEY_ENABLED, prefs.getBoolean(ConfigContract.KEY_ENABLED, ConfigContract.DEFAULT_ENABLED));
            out.putBoolean(ConfigContract.KEY_HAPTIC, prefs.getBoolean(ConfigContract.KEY_HAPTIC, ConfigContract.DEFAULT_HAPTIC));
            out.putInt(ConfigContract.KEY_TRIGGER_PERCENT, prefs.getInt(ConfigContract.KEY_TRIGGER_PERCENT, ConfigContract.DEFAULT_TRIGGER_PERCENT));
            out.putInt(ConfigContract.KEY_SELECTION_RADIUS_PERCENT, prefs.getInt(ConfigContract.KEY_SELECTION_RADIUS_PERCENT, ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT));
            out.putInt(ConfigContract.KEY_HOT_WIDTH_PERCENT, prefs.getInt(ConfigContract.KEY_HOT_WIDTH_PERCENT, ConfigContract.DEFAULT_HOT_WIDTH_PERCENT));
            out.putInt(ConfigContract.KEY_HOT_HEIGHT_PERCENT, prefs.getInt(ConfigContract.KEY_HOT_HEIGHT_PERCENT, ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT));
            out.putInt(ConfigContract.KEY_ICON_SIZE_DP, prefs.getInt(ConfigContract.KEY_ICON_SIZE_DP, ConfigContract.DEFAULT_ICON_SIZE_DP));
            out.putInt(ConfigContract.KEY_WIDTH_PERCENT, prefs.getInt(ConfigContract.KEY_WIDTH_PERCENT, ConfigContract.DEFAULT_WIDTH_PERCENT));
            out.putInt(ConfigContract.KEY_HEIGHT_PERCENT, prefs.getInt(ConfigContract.KEY_HEIGHT_PERCENT, ConfigContract.DEFAULT_HEIGHT_PERCENT));
            out.putInt(ConfigContract.KEY_POSITION_X, prefs.getInt(ConfigContract.KEY_POSITION_X, ConfigContract.DEFAULT_POSITION_X));
            out.putInt(ConfigContract.KEY_POSITION_Y, prefs.getInt(ConfigContract.KEY_POSITION_Y, ConfigContract.DEFAULT_POSITION_Y));
            out.putInt(ConfigContract.KEY_UPPER_SINGLE_ACTION, prefs.getInt(ConfigContract.KEY_UPPER_SINGLE_ACTION, ConfigContract.DEFAULT_UPPER_SINGLE_ACTION));
            out.putInt(ConfigContract.KEY_UPPER_DOUBLE_ACTION, prefs.getInt(ConfigContract.KEY_UPPER_DOUBLE_ACTION, ConfigContract.DEFAULT_UPPER_DOUBLE_ACTION));
            out.putInt(ConfigContract.KEY_LOWER_SINGLE_ACTION, prefs.getInt(ConfigContract.KEY_LOWER_SINGLE_ACTION, ConfigContract.DEFAULT_LOWER_SINGLE_ACTION));
            out.putInt(ConfigContract.KEY_LOWER_DOUBLE_ACTION, prefs.getInt(ConfigContract.KEY_LOWER_DOUBLE_ACTION, ConfigContract.DEFAULT_LOWER_DOUBLE_ACTION));
            out.putInt(ConfigContract.KEY_OUTSIDE_SINGLE_ACTION, prefs.getInt(ConfigContract.KEY_OUTSIDE_SINGLE_ACTION, ConfigContract.DEFAULT_OUTSIDE_SINGLE_ACTION));
            out.putInt(ConfigContract.KEY_OUTSIDE_DOUBLE_ACTION, prefs.getInt(ConfigContract.KEY_OUTSIDE_DOUBLE_ACTION, ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION));
            out.putString(ConfigContract.KEY_COMPONENTS, prefs.getString(ConfigContract.KEY_COMPONENTS, "[]"));
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
        return super.call(method, arg, extras);
    }

    private Context requireProviderContext() {
        Context context = getContext();
        if (context == null) {
            throw new IllegalStateException("Provider context is unavailable");
        }
        return context;
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
                if ("com.android.systemui".equals(packageName)) {
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
