package com.oxohang.fanfreeform.xposed;

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
    final boolean fanAnimationsEnabled;
    final int fanAnimationSpeed;
    final int fanRevealAmount;
    final int fanRotationDegrees;
    final int fanSelectionScalePercent;
    final boolean fanSelectionRing;
    final boolean sideGestureEnabled;
    final int sideTriggerPercent;
    final int sideIconSizeDp;
    final int sideTopSafeMarginPercent;
    final boolean showSelectedAppName;
    final boolean sideFollowFinger;
    final int sideLayoutMode;
    final boolean sideFanList;
    final boolean sideRingList;
    final int sideRingSizePercent;
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
    final List<TargetSpec> targets;

    static final class TargetSpec {
        final String component;
        final String packageName;
        final String shortcutId;
        final String shortcutLabel;
        final int userId;

        private TargetSpec(String component, String packageName, String shortcutId,
                           String shortcutLabel, int userId) {
            this.component = component;
            this.packageName = packageName;
            this.shortcutId = shortcutId;
            this.shortcutLabel = shortcutLabel;
            this.userId = userId;
        }

        static TargetSpec activity(String component, int userId) {
            return new TargetSpec(component, "", "", "", Math.max(0, userId));
        }

        static TargetSpec shortcut(String packageName, String shortcutId, String label) {
            return new TargetSpec("", packageName, shortcutId, label, 0);
        }

        boolean isShortcut() { return !packageName.isEmpty() && !shortcutId.isEmpty(); }
    }

    private GestureConfig(boolean enabled, boolean haptic, boolean fanShadow,
                          boolean fanAnimationsEnabled, int fanAnimationSpeed,
                          int fanRevealAmount, int fanRotationDegrees,
                          int fanSelectionScalePercent, boolean fanSelectionRing,
                          boolean sideGestureEnabled, int sideTriggerPercent,
                          int sideIconSizeDp, int sideTopSafeMarginPercent,
                          boolean showSelectedAppName, boolean sideFollowFinger,
                          int sideLayoutMode, int sideRingSizePercent,
                          boolean sideWheelMode,
                          int sideReverseCancelPercent,
                          int triggerPercent, int selectionRadiusPercent,
                          int hotWidthPercent, int hotHeightPercent, int iconSizeDp,
                          int widthPercent, int heightPercent, int positionX,
                          int positionY, int outsideSingleAction, int outsideDoubleAction,
                          List<TargetSpec> targets) {
        this.enabled = enabled;
        this.haptic = haptic;
        this.fanShadow = fanShadow;
        this.fanAnimationsEnabled = fanAnimationsEnabled;
        this.fanAnimationSpeed = clamp(fanAnimationSpeed,
                ConfigContract.MIN_FAN_ANIMATION_SPEED,
                ConfigContract.MAX_FAN_ANIMATION_SPEED);
        this.fanRevealAmount = clamp(fanRevealAmount,
                ConfigContract.MIN_FAN_REVEAL_AMOUNT,
                ConfigContract.MAX_FAN_REVEAL_AMOUNT);
        this.fanRotationDegrees = clamp(fanRotationDegrees,
                ConfigContract.MIN_FAN_ROTATION_DEGREES,
                ConfigContract.MAX_FAN_ROTATION_DEGREES);
        this.fanSelectionScalePercent = clamp(fanSelectionScalePercent,
                ConfigContract.MIN_FAN_SELECTION_SCALE_PERCENT,
                ConfigContract.MAX_FAN_SELECTION_SCALE_PERCENT);
        this.fanSelectionRing = fanSelectionRing;
        this.sideGestureEnabled = sideGestureEnabled;
        this.sideTriggerPercent = clamp(sideTriggerPercent, 18, 50);
        this.sideIconSizeDp = clamp(sideIconSizeDp, 34, 64);
        this.sideTopSafeMarginPercent = clamp(sideTopSafeMarginPercent, 8, 35);
        this.showSelectedAppName = showSelectedAppName;
        this.sideFollowFinger = sideFollowFinger;
        this.sideLayoutMode = clamp(sideLayoutMode,
                ConfigContract.SIDE_LAYOUT_LIST, ConfigContract.SIDE_LAYOUT_FAN);
        this.sideFanList = this.sideLayoutMode == ConfigContract.SIDE_LAYOUT_FAN;
        this.sideRingList = this.sideLayoutMode == ConfigContract.SIDE_LAYOUT_RING;
        this.sideRingSizePercent = clamp(sideRingSizePercent,
                ConfigContract.MIN_SIDE_RING_SIZE_PERCENT,
                ConfigContract.MAX_SIDE_RING_SIZE_PERCENT);
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
        this.targets = Collections.unmodifiableList(targets);
    }

    static GestureConfig defaults() {
        return new GestureConfig(true, true, ConfigContract.DEFAULT_FAN_SHADOW,
                ConfigContract.DEFAULT_FAN_ANIMATIONS_ENABLED,
                ConfigContract.DEFAULT_FAN_ANIMATION_SPEED,
                ConfigContract.DEFAULT_FAN_REVEAL_AMOUNT,
                ConfigContract.DEFAULT_FAN_ROTATION_DEGREES,
                ConfigContract.DEFAULT_FAN_SELECTION_SCALE_PERCENT,
                ConfigContract.DEFAULT_FAN_SELECTION_RING,
                ConfigContract.DEFAULT_SIDE_GESTURE_ENABLED,
                ConfigContract.DEFAULT_SIDE_TRIGGER_PERCENT,
                ConfigContract.DEFAULT_SIDE_ICON_SIZE_DP,
                ConfigContract.DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT,
                ConfigContract.DEFAULT_SHOW_SELECTED_APP_NAME,
                ConfigContract.DEFAULT_SIDE_FOLLOW_FINGER,
                ConfigContract.DEFAULT_SIDE_LAYOUT_MODE,
                ConfigContract.DEFAULT_SIDE_RING_SIZE_PERCENT,
                ConfigContract.DEFAULT_SIDE_WHEEL_MODE,
                ConfigContract.DEFAULT_SIDE_REVERSE_CANCEL_PERCENT,
                ConfigContract.DEFAULT_TRIGGER_PERCENT,
                ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT,
                ConfigContract.DEFAULT_HOT_WIDTH_PERCENT, ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT,
                ConfigContract.DEFAULT_ICON_SIZE_DP,
                ConfigContract.DEFAULT_WIDTH_PERCENT, ConfigContract.DEFAULT_HEIGHT_PERCENT,
                ConfigContract.DEFAULT_POSITION_X, ConfigContract.DEFAULT_POSITION_Y,
                ConfigContract.DEFAULT_OUTSIDE_SINGLE_ACTION, ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION,
                new ArrayList<>());
    }

    static GestureConfig from(Bundle bundle) {
        if (bundle == null) return defaults();
        ArrayList<TargetSpec> targets = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(bundle.getString(ConfigContract.KEY_COMPONENTS, "[]"));
            for (int i = 0; i < Math.min(8, array.length()); i++) {
                Object raw = array.opt(i);
                TargetSpec target = null;
                if (raw instanceof JSONObject) {
                    JSONObject object = (JSONObject) raw;
                    if ("shortcut".equals(object.optString("type"))) {
                        String packageName = object.optString("package");
                        String shortcutId = object.optString("id");
                        if (!packageName.isEmpty() && !shortcutId.isEmpty()) {
                            target = TargetSpec.shortcut(packageName, shortcutId,
                                    object.optString("label"));
                        }
                    } else {
                        target = validActivity(object.optString("component"),
                                object.optInt("userId", 0));
                    }
                } else {
                    target = validActivity(array.optString(i), 0);
                }
                if (target != null && !containsTarget(targets, target)) targets.add(target);
            }
        } catch (Exception ignored) {}
        return new GestureConfig(
                bundle.getBoolean(ConfigContract.KEY_ENABLED, ConfigContract.DEFAULT_ENABLED),
                bundle.getBoolean(ConfigContract.KEY_HAPTIC, ConfigContract.DEFAULT_HAPTIC),
                bundle.getBoolean(ConfigContract.KEY_FAN_SHADOW, ConfigContract.DEFAULT_FAN_SHADOW),
                bundle.getBoolean(ConfigContract.KEY_FAN_ANIMATIONS_ENABLED,
                        ConfigContract.DEFAULT_FAN_ANIMATIONS_ENABLED),
                bundle.getInt(ConfigContract.KEY_FAN_ANIMATION_SPEED,
                        ConfigContract.DEFAULT_FAN_ANIMATION_SPEED),
                bundle.getInt(ConfigContract.KEY_FAN_REVEAL_AMOUNT,
                        ConfigContract.DEFAULT_FAN_REVEAL_AMOUNT),
                bundle.getInt(ConfigContract.KEY_FAN_ROTATION_DEGREES,
                        ConfigContract.DEFAULT_FAN_ROTATION_DEGREES),
                bundle.getInt(ConfigContract.KEY_FAN_SELECTION_SCALE_PERCENT,
                        ConfigContract.DEFAULT_FAN_SELECTION_SCALE_PERCENT),
                bundle.getBoolean(ConfigContract.KEY_FAN_SELECTION_RING,
                        ConfigContract.DEFAULT_FAN_SELECTION_RING),
                bundle.getBoolean(ConfigContract.KEY_SIDE_GESTURE_ENABLED, ConfigContract.DEFAULT_SIDE_GESTURE_ENABLED),
                bundle.getInt(ConfigContract.KEY_SIDE_TRIGGER_PERCENT, ConfigContract.DEFAULT_SIDE_TRIGGER_PERCENT),
                bundle.getInt(ConfigContract.KEY_SIDE_ICON_SIZE_DP,
                        bundle.getInt(ConfigContract.KEY_ICON_SIZE_DP,
                                ConfigContract.DEFAULT_SIDE_ICON_SIZE_DP)),
                bundle.getInt(ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT),
                bundle.getBoolean(ConfigContract.KEY_SHOW_SELECTED_APP_NAME,
                        bundle.getBoolean(ConfigContract.KEY_SIDE_SHOW_APP_NAMES,
                                ConfigContract.DEFAULT_SHOW_SELECTED_APP_NAME)),
                bundle.getBoolean(ConfigContract.KEY_SIDE_FOLLOW_FINGER,
                        ConfigContract.DEFAULT_SIDE_FOLLOW_FINGER),
                bundle.getInt(ConfigContract.KEY_SIDE_LAYOUT_MODE,
                        bundle.getBoolean(ConfigContract.KEY_SIDE_FAN_LIST,
                                ConfigContract.DEFAULT_SIDE_FAN_LIST)
                                ? ConfigContract.SIDE_LAYOUT_FAN
                                : ConfigContract.DEFAULT_SIDE_LAYOUT_MODE),
                bundle.getInt(ConfigContract.KEY_SIDE_RING_SIZE_PERCENT,
                        ConfigContract.DEFAULT_SIDE_RING_SIZE_PERCENT),
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
                targets);
    }

    boolean ready() {
        return enabled && targets.size() >= 3;
    }

    private static TargetSpec validActivity(String component, int userId) {
        return android.content.ComponentName.unflattenFromString(component) == null
                ? null : TargetSpec.activity(component, userId);
    }

    private static boolean containsTarget(List<TargetSpec> targets, TargetSpec candidate) {
        for (TargetSpec target : targets) {
            if (target.isShortcut() == candidate.isShortcut()
                    && target.component.equals(candidate.component)
                    && target.packageName.equals(candidate.packageName)
                    && target.shortcutId.equals(candidate.shortcutId)
                    && target.userId == candidate.userId) return true;
        }
        return false;
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
