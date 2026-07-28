package com.oxohang.fanfreeform.config;

import android.content.Context;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class ConfigStore {
    private static final String KEY_DOUBLE_PIN_MIGRATED = "double_pin_migrated_v1";
    private final Context context;
    private final SharedPreferences preferences;

    public ConfigStore(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(ConfigContract.PREFS, Context.MODE_PRIVATE);
        seedDefaultsIfNeeded(this.context, preferences);
        migrateUnifiedActionsIfNeeded(preferences);
        migrateDoubleTapPinIfNeeded(preferences);
    }

    public SharedPreferences preferences() {
        return preferences;
    }

    public List<AppTarget> getTargets() {
        ArrayList<AppTarget> result = new ArrayList<>();
        String raw = preferences.getString(ConfigContract.KEY_COMPONENTS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                Object item = array.get(i);
                String flattened;
                boolean shortcut = false;
                if (item instanceof String) {
                    flattened = (String) item;
                } else if (item instanceof org.json.JSONObject) {
                    org.json.JSONObject obj = (org.json.JSONObject) item;
                    flattened = obj.optString("c", "");
                    shortcut = obj.optBoolean("s", false);
                } else {
                    continue;
                }
                AppTarget target = new AppTarget(flattened, shortcut);
                if (target.componentName() != null && !result.contains(target)) {
                    result.add(target);
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public void setTargets(List<AppTarget> targets) {
        JSONArray array = new JSONArray();
        int count = Math.min(8, targets.size());
        for (int i = 0; i < count; i++) {
            AppTarget t = targets.get(i);
            if (t.isShortcut) {
                org.json.JSONObject obj = new org.json.JSONObject();
                try {
                    obj.put("c", t.component);
                    obj.put("s", true);
                } catch (Exception ignored) {}
                array.put(obj);
            } else {
                array.put(t.component);
            }
        }
        preferences.edit().putString(ConfigContract.KEY_COMPONENTS, array.toString()).apply();
        notifyChanged();
    }

    public void putBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
        notifyChanged();
    }

    public void putInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
        notifyChanged();
    }

    public void resetTuning() {
        preferences.edit()
                .putBoolean(ConfigContract.KEY_ENABLED, ConfigContract.DEFAULT_ENABLED)
                .putBoolean(ConfigContract.KEY_HAPTIC, ConfigContract.DEFAULT_HAPTIC)
                .putBoolean(ConfigContract.KEY_FAN_SHADOW, ConfigContract.DEFAULT_FAN_SHADOW)
                .putBoolean(ConfigContract.KEY_SIDE_GESTURE_ENABLED, ConfigContract.DEFAULT_SIDE_GESTURE_ENABLED)
                .putInt(ConfigContract.KEY_SIDE_TRIGGER_PERCENT, ConfigContract.DEFAULT_SIDE_TRIGGER_PERCENT)
                .putInt(ConfigContract.KEY_SIDE_ICON_SIZE_DP, ConfigContract.DEFAULT_SIDE_ICON_SIZE_DP)
                .putInt(ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT)
                .putBoolean(ConfigContract.KEY_SIDE_SHOW_APP_NAMES,
                        ConfigContract.DEFAULT_SIDE_SHOW_APP_NAMES)
                .putBoolean(ConfigContract.KEY_SIDE_FOLLOW_FINGER,
                        ConfigContract.DEFAULT_SIDE_FOLLOW_FINGER)
                .putBoolean(ConfigContract.KEY_SIDE_WHEEL_MODE,
                        ConfigContract.DEFAULT_SIDE_WHEEL_MODE)
                .putInt(ConfigContract.KEY_SIDE_REVERSE_CANCEL_PERCENT,
                        ConfigContract.DEFAULT_SIDE_REVERSE_CANCEL_PERCENT)
                .putInt(ConfigContract.KEY_TRIGGER_PERCENT, ConfigContract.DEFAULT_TRIGGER_PERCENT)
                .putInt(ConfigContract.KEY_SELECTION_RADIUS_PERCENT, ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT)
                .putInt(ConfigContract.KEY_HOT_WIDTH_PERCENT, ConfigContract.DEFAULT_HOT_WIDTH_PERCENT)
                .putInt(ConfigContract.KEY_HOT_HEIGHT_PERCENT, ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT)
                .putInt(ConfigContract.KEY_ICON_SIZE_DP, ConfigContract.DEFAULT_ICON_SIZE_DP)
                .putInt(ConfigContract.KEY_WIDTH_PERCENT, ConfigContract.DEFAULT_WIDTH_PERCENT)
                .putInt(ConfigContract.KEY_HEIGHT_PERCENT, ConfigContract.DEFAULT_HEIGHT_PERCENT)
                .putInt(ConfigContract.KEY_POSITION_X, ConfigContract.DEFAULT_POSITION_X)
                .putInt(ConfigContract.KEY_POSITION_Y, ConfigContract.DEFAULT_POSITION_Y)
                .putInt(ConfigContract.KEY_OUTSIDE_SINGLE_ACTION, ConfigContract.DEFAULT_OUTSIDE_SINGLE_ACTION)
                .putInt(ConfigContract.KEY_OUTSIDE_DOUBLE_ACTION, ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION)
                .apply();
        notifyChanged();
    }

    public void notifyChanged() {
        context.getContentResolver().notifyChange(ConfigContract.URI, null);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("ApplySharedPref")
    static void seedDefaultsIfNeeded(Context context, SharedPreferences preferences) {
        if (preferences.contains(ConfigContract.KEY_COMPONENTS)) return;
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent, 0);
        Map<String, ComponentName> byPackage = new HashMap<>();
        for (ResolveInfo item : activities) {
            if (item.activityInfo != null) {
                byPackage.putIfAbsent(item.activityInfo.packageName,
                        new ComponentName(item.activityInfo.packageName, item.activityInfo.name));
            }
        }
        String[] preferred = {
                "com.miui.calculator", "com.miui.notes", "com.android.calendar",
                "com.android.browser", "com.android.fileexplorer", "com.miui.weather2"
        };
        JSONArray array = new JSONArray();
        for (String packageName : preferred) {
            ComponentName component = byPackage.get(packageName);
            if (component != null) array.put(component.flattenToString());
        }
        if (array.length() < 3) {
            for (ComponentName component : byPackage.values()) {
                if (context.getPackageName().equals(component.getPackageName())) continue;
                array.put(component.flattenToString());
                if (array.length() >= 6) break;
            }
        }
        preferences.edit().putString(ConfigContract.KEY_COMPONENTS, array.toString()).commit();
    }

    @SuppressLint("ApplySharedPref")
    static void migrateUnifiedActionsIfNeeded(SharedPreferences preferences) {
        boolean needsSingle = !preferences.contains(ConfigContract.KEY_OUTSIDE_SINGLE_ACTION);
        boolean needsDouble = !preferences.contains(ConfigContract.KEY_OUTSIDE_DOUBLE_ACTION);
        if (!needsSingle && !needsDouble) return;
        SharedPreferences.Editor editor = preferences.edit();
        if (needsSingle) {
            editor.putInt(ConfigContract.KEY_OUTSIDE_SINGLE_ACTION,
                    preferences.getInt("lower_single_action",
                            ConfigContract.DEFAULT_OUTSIDE_SINGLE_ACTION));
        }
        if (needsDouble) {
            editor.putInt(ConfigContract.KEY_OUTSIDE_DOUBLE_ACTION,
                    preferences.getInt("lower_double_action",
                            ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION));
        }
        editor.commit();
    }

    @SuppressLint("ApplySharedPref")
    static void migrateDoubleTapPinIfNeeded(SharedPreferences preferences) {
        if (preferences.getBoolean(KEY_DOUBLE_PIN_MIGRATED, false)) return;
        preferences.edit()
                .putInt(ConfigContract.KEY_OUTSIDE_DOUBLE_ACTION,
                        ConfigContract.ACTION_PIN)
                .putBoolean(KEY_DOUBLE_PIN_MIGRATED, true)
                .commit();
    }
}
