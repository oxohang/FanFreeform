package com.oxohang.fanfreeform.xposed;

import android.content.ComponentName;
import android.os.Bundle;

import com.oxohang.fanfreeform.config.ConfigContract;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class GestureConfig {
    final boolean enabled;
    final boolean haptic;
    final boolean fanShadow;
    final boolean sideGestureEnabled;
    final int sideTriggerPercent;
    final int sideIconSizeDp;
    final int sideTopSafeMarginPercent;
    final boolean sideShowAppNames;
    final boolean sideFollowFinger;
    final boolean sideWheelMode;
    final int sideReverseCancelPercent;
    final int triggerPercent;
    final int selectionRadiusPercent;
    final int hotWidthPercent;
    final int hotHeightPercent;
    final int iconSizeDp;
    final int widthPercent;
    final int heightPercent;
    final int positionX;
    final int positionY;
    final int outsideSingleAction;
    final int outsideDoubleAction;
    final List<ComponentName> components;
    final List<Boolean> shortcutFlags; // true = shortcut (specific activity), false = app (launcher)

    private GestureConfig(boolean enabled, boolean haptic, boolean fanShadow,
                          boolean sideGestureEnabled, int sideTriggerPercent,
                          int sideIconSizeDp, int sideTopSafeMarginPercent,
                          boolean sideShowAppNames, boolean sideFollowFinger,
                          boolean sideWheelMode, int sideReverseCancelPercent,
                          int triggerPercent, int selectionRadiusPercent,
                          int hotWidthPercent, int hotHeightPercent, int iconSizeDp,
                          int widthPercent, int heightPercent, int positionX,
                          int positionY, int outsideSingleAction, int outsideDoubleAction,
                          List<ComponentName> components, List<Boolean> shortcutFlags) {
        this.enabled = enabled;
        this.haptic = haptic;
        this.fanShadow = fanShadow;
        this.sideGestureEnabled = sideGestureEnabled;
        this.sideTriggerPercent = clamp(sideTriggerPercent, 18, 50);
        this.sideIconSizeDp = clamp(sideIconSizeDp, 34, 64);
        this.sideTopSafeMarginPercent = clamp(sideTopSafeMarginPercent, 8, 35);
        this.sideShowAppNames = sideShowAppNames;
        this.sideFollowFinger = sideFollowFinger;
        this.sideWheelMode = false;
        this.sideReverseCancelPercent = clamp(sideReverseCancelPercent,
                ConfigContract.MIN_SIDE_REVERSE_CANCEL_PERCENT,
                ConfigContract.MAX_SIDE_REVERSE_CANCEL_PERCENT);
        this.triggerPercent = clamp(triggerPercent, 6, 24);
        this.selectionRadiusPercent = clamp(selectionRadiusPercent, 35, 75);
        this.hotWidthPercent = clamp(hotWidthPercent, 5, 20);
        this.hotHeightPercent = clamp(hotHeightPercent, 3,
                ConfigContract.MAX_HOT_HEIGHT_PERCENT);
        this.iconSizeDp = clamp(iconSizeDp, 34, 64);
        this.widthPercent = clamp(widthPercent, 40, 90);
        this.heightPercent = clamp(heightPercent, 35, 85);
        this.positionX = clamp(positionX, 0, 100);
        this.positionY = clamp(positionY, 0, 100);
        this.outsideSingleAction = supportedOutsideAction(outsideSingleAction);
        this.outsideDoubleAction = supportedOutsideAction(outsideDoubleAction);
        this.components = Collections.unmodifiableList(components);
        this.shortcutFlags = shortcutFlags != null
                ? Collections.unmodifiableList(new ArrayList<>(shortcutFlags))
                : Collections.emptyList();
    }

    static GestureConfig defaults() {
        return new GestureConfig(true, true, ConfigContract.DEFAULT_FAN_SHADOW,
                ConfigContract.DEFAULT_SIDE_GESTURE_ENABLED,
                ConfigContract.DEFAULT_SIDE_TRIGGER_PERCENT,
                ConfigContract.DEFAULT_SIDE_ICON_SIZE_DP,
                ConfigContract.DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT,
                ConfigContract.DEFAULT_SIDE_SHOW_APP_NAMES,
                ConfigContract.DEFAULT_SIDE_FOLLOW_FINGER,
                ConfigContract.DEFAULT_SIDE_WHEEL_MODE,
                ConfigContract.DEFAULT_SIDE_REVERSE_CANCEL_PERCENT,
                ConfigContract.DEFAULT_TRIGGER_PERCENT,
                ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT,
                ConfigContract.DEFAULT_HOT_WIDTH_PERCENT, ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT,
                ConfigContract.DEFAULT_ICON_SIZE_DP,
                ConfigContract.DEFAULT_WIDTH_PERCENT, ConfigContract.DEFAULT_HEIGHT_PERCENT,
                ConfigContract.DEFAULT_POSITION_X, ConfigContract.DEFAULT_POSITION_Y,
                ConfigContract.DEFAULT_OUTSIDE_SINGLE_ACTION, ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION,
                new ArrayList<>(), new ArrayList<>());
    }

    static GestureConfig from(Bundle bundle) {
        if (bundle == null) return defaults();
        ArrayList<ComponentName> components = new ArrayList<>();
        ArrayList<Boolean> shortcutFlags = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(bundle.getString(ConfigContract.KEY_COMPONENTS, "[]"));
            for (int i = 0; i < Math.min(8, array.length()); i++) {
                Object item = array.opt(i);
                ComponentName component = null;
                boolean shortcut = false;
                if (item instanceof String) {
                    component = ComponentName.unflattenFromString((String) item);
                } else if (item instanceof JSONObject) {
                    JSONObject obj = (JSONObject) item;
                    component = ComponentName.unflattenFromString(obj.optString("c", ""));
                    shortcut = obj.optBoolean("s", false);
                }
                if (component != null && !components.contains(component)) {
                    components.add(component);
                    shortcutFlags.add(shortcut);
                }
            }
        } catch (Exception ignored) {}
        return new GestureConfig(
                bundle.getBoolean(ConfigContract.KEY_ENABLED, ConfigContract.DEFAULT_ENABLED),
                bundle.getBoolean(ConfigContract.KEY_HAPTIC, ConfigContract.DEFAULT_HAPTIC),
                bundle.getBoolean(ConfigContract.KEY_FAN_SHADOW, ConfigContract.DEFAULT_FAN_SHADOW),
                bundle.getBoolean(ConfigContract.KEY_SIDE_GESTURE_ENABLED, ConfigContract.DEFAULT_SIDE_GESTURE_ENABLED),
                bundle.getInt(ConfigContract.KEY_SIDE_TRIGGER_PERCENT, ConfigContract.DEFAULT_SIDE_TRIGGER_PERCENT),
                bundle.getInt(ConfigContract.KEY_SIDE_ICON_SIZE_DP,
                        bundle.getInt(ConfigContract.KEY_ICON_SIZE_DP,
                                ConfigContract.DEFAULT_SIDE_ICON_SIZE_DP)),
                bundle.getInt(ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT),
                bundle.getBoolean(ConfigContract.KEY_SIDE_SHOW_APP_NAMES,
                        ConfigContract.DEFAULT_SIDE_SHOW_APP_NAMES),
                bundle.getBoolean(ConfigContract.KEY_SIDE_FOLLOW_FINGER,
                        ConfigContract.DEFAULT_SIDE_FOLLOW_FINGER),
                bundle.getBoolean(ConfigContract.KEY_SIDE_WHEEL_MODE,
                        ConfigContract.DEFAULT_SIDE_WHEEL_MODE),
                bundle.getInt(ConfigContract.KEY_SIDE_REVERSE_CANCEL_PERCENT,
                        ConfigContract.DEFAULT_SIDE_REVERSE_CANCEL_PERCENT),
                bundle.getInt(ConfigContract.KEY_TRIGGER_PERCENT, ConfigContract.DEFAULT_TRIGGER_PERCENT),
                bundle.getInt(ConfigContract.KEY_SELECTION_RADIUS_PERCENT, ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT),
                bundle.getInt(ConfigContract.KEY_HOT_WIDTH_PERCENT, ConfigContract.DEFAULT_HOT_WIDTH_PERCENT),
                bundle.getInt(ConfigContract.KEY_HOT_HEIGHT_PERCENT, ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT),
                bundle.getInt(ConfigContract.KEY_ICON_SIZE_DP, ConfigContract.DEFAULT_ICON_SIZE_DP),
                bundle.getInt(ConfigContract.KEY_WIDTH_PERCENT, ConfigContract.DEFAULT_WIDTH_PERCENT),
                bundle.getInt(ConfigContract.KEY_HEIGHT_PERCENT, ConfigContract.DEFAULT_HEIGHT_PERCENT),
                bundle.getInt(ConfigContract.KEY_POSITION_X, ConfigContract.DEFAULT_POSITION_X),
                bundle.getInt(ConfigContract.KEY_POSITION_Y, ConfigContract.DEFAULT_POSITION_Y),
                bundle.getInt(ConfigContract.KEY_OUTSIDE_SINGLE_ACTION, ConfigContract.DEFAULT_OUTSIDE_SINGLE_ACTION),
                bundle.getInt(ConfigContract.KEY_OUTSIDE_DOUBLE_ACTION, ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION),
                components, shortcutFlags);
    }

    boolean ready() {
        return enabled && components.size() >= 3;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int supportedOutsideAction(int action) {
        return action == ConfigContract.ACTION_CLOSE
                || action == ConfigContract.ACTION_PIN
                || action == ConfigContract.ACTION_FULLSCREEN
                ? action : ConfigContract.ACTION_NONE;
    }
}
