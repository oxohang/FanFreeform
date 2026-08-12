package com.oxohang.fanfreeform.xposed;

import android.os.Bundle;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.FanRowAllocation;
import com.oxohang.fanfreeform.config.PressureTrigger;

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
    int bottomAnimationSpeed = ConfigContract.DEFAULT_BOTTOM_ANIMATION_SPEED;
    boolean bottomTriggerHaptic = ConfigContract.DEFAULT_BOTTOM_TRIGGER_HAPTIC;
    int selectionTransformLevel = ConfigContract.DEFAULT_SELECTION_TRANSFORM_LEVEL;
    boolean forceCircularIcons = ConfigContract.DEFAULT_FORCE_CIRCULAR_ICONS;
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
    boolean customWindowPortraitEnabled =
            ConfigContract.DEFAULT_CUSTOM_WINDOW_PORTRAIT_ENABLED;
    boolean customWindowLandscapeEnabled =
            ConfigContract.DEFAULT_CUSTOM_WINDOW_LANDSCAPE_ENABLED;
    boolean portraitProportionalSizeEnabled =
            ConfigContract.DEFAULT_PORTRAIT_PROPORTIONAL_SIZE_ENABLED;
    boolean landscapeProportionalSizeEnabled =
            ConfigContract.DEFAULT_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED;
    int portraitNativeScalePercent =
            ConfigContract.DEFAULT_NATIVE_WINDOW_SCALE_PERCENT;
    int landscapeNativeScalePercent =
            ConfigContract.DEFAULT_NATIVE_WINDOW_SCALE_PERCENT;
    final int widthPercent;
    final int heightPercent;
    final int positionX;
    final int positionY;
    final int outsideSingleAction;
    final int outsideDoubleAction;
    final List<TargetSpec> targets;
    final boolean honeycombEnabled;
    final int honeycombMode;
    final int honeycombIconSizeDp;
    final int honeycombSpacingDp;
    final int honeycombAnimationSpeed;
    final int honeycombInertia;
    final int honeycombCenterScale;
    final int honeycombEdgeScale;
    final int honeycombSelectionScale;
    final boolean honeycombEmptyTapClose;
    final List<TargetSpec> honeycombTargets;
    int fanMaxTargets = ConfigContract.DEFAULT_FAN_MAX_TARGETS;
    boolean fanFixedSevenRows = ConfigContract.DEFAULT_FAN_FIXED_SEVEN_ROWS;
    int fanLayoutMode = ConfigContract.DEFAULT_FAN_LAYOUT_MODE;
    int fanCustomOuterCount = ConfigContract.DEFAULT_FAN_CUSTOM_OUTER_COUNT;
    int fanCustomMiddleCount = ConfigContract.DEFAULT_FAN_CUSTOM_MIDDLE_COUNT;
    int fanCustomInnerCount = ConfigContract.DEFAULT_FAN_CUSTOM_INNER_COUNT;
    boolean bottomPortraitEnabled = ConfigContract.DEFAULT_BOTTOM_PORTRAIT_ENABLED;
    boolean bottomLandscapeEnabled = ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_ENABLED;
    boolean bottomPortraitSecondStageEnabled =
            ConfigContract.DEFAULT_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED;
    boolean bottomPortraitFirstPressureLaunch =
            ConfigContract.DEFAULT_BOTTOM_PORTRAIT_FIRST_PRESSURE_LAUNCH;
    boolean bottomPortraitSecondPressureLaunch =
            ConfigContract.DEFAULT_BOTTOM_PORTRAIT_SECOND_PRESSURE_LAUNCH;
    boolean bottomLandscapeSecondStageEnabled =
            ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED;
    boolean sidePortraitEnabled = ConfigContract.DEFAULT_SIDE_PORTRAIT_ENABLED;
    boolean sideLandscapeEnabled = ConfigContract.DEFAULT_SIDE_LANDSCAPE_ENABLED;
    int sidePortraitLayoutMode = ConfigContract.DEFAULT_SIDE_LAYOUT_MODE;
    int sideLandscapeLayoutMode = ConfigContract.DEFAULT_SIDE_LAYOUT_MODE;
    int landscapeWidthPercent = ConfigContract.DEFAULT_LANDSCAPE_WIDTH_PERCENT;
    int landscapeHeightPercent = ConfigContract.DEFAULT_LANDSCAPE_HEIGHT_PERCENT;
    int landscapePositionX = ConfigContract.DEFAULT_LANDSCAPE_POSITION_X;
    int landscapePositionY = ConfigContract.DEFAULT_LANDSCAPE_POSITION_Y;
    int honeycombMaxTargets = ConfigContract.DEFAULT_HONEYCOMB_MAX_TARGETS;
    int sideMaxTargets = ConfigContract.DEFAULT_SIDE_MAX_TARGETS;
    boolean sideFollowHoneycomb = ConfigContract.DEFAULT_SIDE_FOLLOW_HONEYCOMB;
    boolean sideDirectionHorizontal = true;
    boolean sideDirectionUp = true;
    boolean sideDirectionDown = true;
    boolean sideHoldEnabled = ConfigContract.DEFAULT_SIDE_HOLD_ENABLED;
    int sideHoldDelayMs = ConfigContract.DEFAULT_SIDE_HOLD_DELAY_MS;
    int sideTaskMaxCount = ConfigContract.DEFAULT_SIDE_TASK_MAX_COUNT;
    int sideTaskCardWidthDp = ConfigContract.DEFAULT_SIDE_TASK_CARD_WIDTH_DP;
    int sideTaskCardHeightDp = ConfigContract.DEFAULT_SIDE_TASK_CARD_HEIGHT_DP;
    int sideTaskCardCornerDp = ConfigContract.DEFAULT_SIDE_TASK_CARD_CORNER_DP;
    int sideTaskIconSizeDp = ConfigContract.DEFAULT_SIDE_TASK_ICON_SIZE_DP;
    int sideTaskIconGapDp = ConfigContract.DEFAULT_SIDE_TASK_ICON_GAP_DP;
    int sideTaskFingerOffsetDp = ConfigContract.DEFAULT_SIDE_TASK_FINGER_OFFSET_DP;
    int sideTaskLayoutMode = ConfigContract.DEFAULT_SIDE_TASK_LAYOUT_MODE;
    boolean sideTaskReverseOrder = ConfigContract.DEFAULT_SIDE_TASK_REVERSE_ORDER;
    boolean sideTaskShowName = ConfigContract.DEFAULT_SIDE_TASK_SHOW_NAME;
    int sideTaskMotionMode = ConfigContract.DEFAULT_SIDE_TASK_MOTION_MODE;
    int sideTaskSwipeSpeedPercent =
            ConfigContract.DEFAULT_SIDE_TASK_SWIPE_SPEED_PERCENT;
    int sideTaskAnimationSpeed = ConfigContract.DEFAULT_SIDE_TASK_ANIMATION_SPEED;
    boolean honeycombCenteredSystemAnimation =
            ConfigContract.DEFAULT_HONEYCOMB_CENTERED_SYSTEM_ANIMATION;
    boolean sideTaskExtendedDownwardTolerance =
            ConfigContract.DEFAULT_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE;
    boolean sideFullscreen = ConfigContract.DEFAULT_SIDE_FULLSCREEN;
    boolean bottomFullscreen = ConfigContract.DEFAULT_BOTTOM_FULLSCREEN;
    boolean bottomHoneycombFreeform = ConfigContract.DEFAULT_BOTTOM_HONEYCOMB_FREEFORM;
    boolean sidePortraitFullscreen = ConfigContract.DEFAULT_SIDE_PORTRAIT_FULLSCREEN;
    boolean sideLandscapeFullscreen = ConfigContract.DEFAULT_SIDE_LANDSCAPE_FULLSCREEN;
    boolean bottomPortraitFullscreen = ConfigContract.DEFAULT_BOTTOM_PORTRAIT_FULLSCREEN;
    boolean bottomLandscapeFullscreen = ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_FULLSCREEN;
    boolean bottomPortraitHoneycombFreeform =
            ConfigContract.DEFAULT_BOTTOM_PORTRAIT_HONEYCOMB_FREEFORM;
    boolean bottomLandscapeHoneycombFreeform =
            ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_HONEYCOMB_FREEFORM;
    int bottomHoneycombSettleMs = ConfigContract.DEFAULT_BOTTOM_HONEYCOMB_SETTLE_MS;
    boolean sideAnimationsEnabled = ConfigContract.DEFAULT_SIDE_ANIMATIONS_ENABLED;
    int sideAnimationSpeed = ConfigContract.DEFAULT_SIDE_ANIMATION_SPEED;
    int sideRevealAmount = ConfigContract.DEFAULT_SIDE_REVEAL_AMOUNT;
    int sideRotationDegrees = ConfigContract.DEFAULT_SIDE_ROTATION_DEGREES;
    int sideSelectionScalePercent = ConfigContract.DEFAULT_SIDE_SELECTION_SCALE_PERCENT;
    boolean sideSelectionRing = ConfigContract.DEFAULT_SIDE_SELECTION_RING;
    boolean honeycombFollowFinger = true;
    boolean honeycombLandscapeEnabled = ConfigContract.DEFAULT_HONEYCOMB_LANDSCAPE_ENABLED;
    int honeycombFixedXPercent = ConfigContract.DEFAULT_HONEYCOMB_FIXED_X_PERCENT;
    int honeycombFixedYPercent = ConfigContract.DEFAULT_HONEYCOMB_FIXED_Y_PERCENT;
    int honeycombBackgroundStyle;
    int honeycombBlurDp = 36;
    int honeycombDimPercent = 22;
    boolean honeycombAppBackgroundEnabled =
            ConfigContract.DEFAULT_HONEYCOMB_APP_BACKGROUND_ENABLED;
    boolean honeycombLiveBlurEnabled = ConfigContract.DEFAULT_HONEYCOMB_LIVE_BLUR_ENABLED;
    int honeycombLiveBlurDp = ConfigContract.DEFAULT_HONEYCOMB_LIVE_BLUR_DP;
    int honeycombBackgroundDimPercent =
            ConfigContract.DEFAULT_HONEYCOMB_BACKGROUND_DIM_PERCENT;
    int honeycombDiscSizePercent = ConfigContract.DEFAULT_HONEYCOMB_DISC_SIZE_PERCENT;
    boolean honeycombShowSelectedName = ConfigContract.DEFAULT_HONEYCOMB_SHOW_SELECTED_NAME;
    int outsideTapWindowMs = ConfigContract.DEFAULT_OUTSIDE_TAP_WINDOW_MS;
    boolean outsidePortraitEnabled = ConfigContract.DEFAULT_OUTSIDE_PORTRAIT_ENABLED;
    boolean outsideLandscapeEnabled = ConfigContract.DEFAULT_OUTSIDE_LANDSCAPE_ENABLED;
    List<TargetSpec> sideTargets = Collections.emptyList();
    List<TargetSpec> pressureTargets = Collections.emptyList();
    List<PressureTrigger> pressureTriggers = Collections.emptyList();
    int pressureAction = ConfigContract.DEFAULT_PRESSURE_ACTION;
    boolean pressureOpenAsFreeform = ConfigContract.DEFAULT_PRESSURE_OPEN_AS_FREEFORM;
    boolean pressureHeavyLaunchEnabled =
            ConfigContract.DEFAULT_PRESSURE_HEAVY_LAUNCH_ENABLED;
    int pressureHapticMode = ConfigContract.DEFAULT_PRESSURE_HAPTIC_MODE;
    int pressureSensorRateMode = ConfigContract.DEFAULT_PRESSURE_SENSOR_RATE_MODE;
    boolean pressureGestureEnabled = ConfigContract.DEFAULT_PRESSURE_GESTURE_ENABLED;
    int pressureCenterXPercent = ConfigContract.DEFAULT_PRESSURE_CENTER_X_PERCENT;
    int pressureCenterYPercent = ConfigContract.DEFAULT_PRESSURE_CENTER_Y_PERCENT;
    int pressureRadiusPercent = ConfigContract.DEFAULT_PRESSURE_RADIUS_PERCENT;
    float pressureThreshold = ConfigContract.DEFAULT_PRESSURE_THRESHOLD;
    int pressureCalibrationValidCount =
            ConfigContract.DEFAULT_PRESSURE_CALIBRATION_VALID_COUNT;
    boolean pressureCalibrated = ConfigContract.DEFAULT_PRESSURE_CALIBRATED;
    boolean pressureShowPosition = ConfigContract.DEFAULT_PRESSURE_SHOW_POSITION;
    int pressureOrbTheme = ConfigContract.DEFAULT_PRESSURE_ORB_THEME;
    boolean pressureThemeEnabled = ConfigContract.DEFAULT_PRESSURE_THEME_ENABLED;
    int pressureOrbSizePercent = ConfigContract.DEFAULT_PRESSURE_ORB_SIZE_PERCENT;
    boolean pressureFirstHapticEnabled = ConfigContract.DEFAULT_PRESSURE_FIRST_HAPTIC_ENABLED;
    int pressureFirstHapticDurationMs = ConfigContract.DEFAULT_PRESSURE_FIRST_HAPTIC_DURATION_MS;
    int pressureFirstHapticAmplitude = ConfigContract.DEFAULT_PRESSURE_FIRST_HAPTIC_AMPLITUDE;
    boolean pressureSecondHapticEnabled = ConfigContract.DEFAULT_PRESSURE_SECOND_HAPTIC_ENABLED;
    int pressureSecondHapticDurationMs = ConfigContract.DEFAULT_PRESSURE_SECOND_HAPTIC_DURATION_MS;
    int pressureSecondHapticAmplitude = ConfigContract.DEFAULT_PRESSURE_SECOND_HAPTIC_AMPLITUDE;
    boolean pressureCalibrationActive = ConfigContract.DEFAULT_PRESSURE_CALIBRATION_ACTIVE;

    static final class TargetSpec {
        final String component;
        final String packageName;
        final String shortcutId;
        final String shortcutLabel;
        final String shortcutIntentUri;
        final int userId;

        private TargetSpec(String component, String packageName, String shortcutId,
                           String shortcutLabel, String shortcutIntentUri, int userId) {
            this.component = component;
            this.packageName = packageName;
            this.shortcutId = shortcutId;
            this.shortcutLabel = shortcutLabel;
            this.shortcutIntentUri = shortcutIntentUri;
            this.userId = userId;
        }

        static TargetSpec activity(String component, int userId) {
            return new TargetSpec(component, "", "", "", "", Math.max(0, userId));
        }

        static TargetSpec shortcut(String packageName, String shortcutId, String label) {
            return new TargetSpec("", packageName, shortcutId, label, "", 0);
        }

        static TargetSpec launcherShortcut(String packageName, String shortcutId,
                                           String label, String intentUri, int userId) {
            return new TargetSpec("", packageName, shortcutId, label, intentUri,
                    Math.max(0, userId));
        }

        boolean isShortcut() { return !packageName.isEmpty() && !shortcutId.isEmpty(); }
        boolean isLauncherShortcut() {
            return isShortcut() && !shortcutIntentUri.isEmpty();
        }
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
                          List<TargetSpec> targets, boolean honeycombEnabled,
                          int honeycombMode,
                          int honeycombIconSizeDp, int honeycombSpacingDp,
                          int honeycombAnimationSpeed, int honeycombInertia,
                          int honeycombCenterScale, int honeycombEdgeScale,
                          int honeycombSelectionScale, boolean honeycombEmptyTapClose,
                          List<TargetSpec> honeycombTargets) {
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
        this.sideTriggerPercent = clamp(sideTriggerPercent,
                ConfigContract.MIN_SIDE_TRIGGER_PERCENT,
                ConfigContract.MAX_SIDE_TRIGGER_PERCENT);
        this.sideIconSizeDp = clamp(sideIconSizeDp, 34, 64);
        this.sideTopSafeMarginPercent = clamp(sideTopSafeMarginPercent, 8, 35);
        this.showSelectedAppName = showSelectedAppName;
        this.sideFollowFinger = sideFollowFinger;
        this.sideLayoutMode = clamp(sideLayoutMode,
                ConfigContract.SIDE_LAYOUT_LIST,
                ConfigContract.SIDE_LAYOUT_SYSTEM_RECENTS);
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
        this.selectionRadiusPercent = clamp(selectionRadiusPercent,
                ConfigContract.MIN_SELECTION_RADIUS_PERCENT,
                ConfigContract.MAX_SELECTION_RADIUS_PERCENT);
        this.hotWidthPercent = clamp(hotWidthPercent, 5, 20);
        this.hotHeightPercent = clamp(hotHeightPercent, 3,
                ConfigContract.MAX_HOT_HEIGHT_PERCENT);
        this.iconSizeDp = clamp(iconSizeDp, ConfigContract.MIN_ICON_SIZE_DP,
                ConfigContract.MAX_ICON_SIZE_DP);
        this.widthPercent = clamp(widthPercent, 40, 90);
        this.heightPercent = clamp(heightPercent, 35, 85);
        this.positionX = clamp(positionX, 0, 100);
        this.positionY = clamp(positionY, 0, 100);
        this.outsideSingleAction = supportedOutsideAction(outsideSingleAction);
        this.outsideDoubleAction = supportedOutsideAction(outsideDoubleAction);
        this.targets = Collections.unmodifiableList(targets);
        this.honeycombEnabled = honeycombEnabled;
        this.honeycombMode = clamp(honeycombMode,
                ConfigContract.HONEYCOMB_MODE_BROWSE,
                ConfigContract.HONEYCOMB_MODE_HOLD);
        this.honeycombIconSizeDp = clamp(honeycombIconSizeDp,
                ConfigContract.MIN_HONEYCOMB_ICON_SIZE_DP,
                ConfigContract.MAX_HONEYCOMB_ICON_SIZE_DP);
        this.honeycombSpacingDp = clamp(honeycombSpacingDp,
                ConfigContract.MIN_HONEYCOMB_SPACING_DP,
                ConfigContract.MAX_HONEYCOMB_SPACING_DP);
        this.honeycombAnimationSpeed = clamp(honeycombAnimationSpeed, 0, 4);
        this.honeycombInertia = clamp(honeycombInertia, 0, 2);
        this.honeycombCenterScale = clamp(honeycombCenterScale,
                ConfigContract.MIN_HONEYCOMB_CENTER_SCALE,
                ConfigContract.MAX_HONEYCOMB_CENTER_SCALE);
        this.honeycombEdgeScale = clamp(honeycombEdgeScale,
                ConfigContract.MIN_HONEYCOMB_EDGE_SCALE,
                ConfigContract.MAX_HONEYCOMB_EDGE_SCALE);
        this.honeycombSelectionScale = clamp(honeycombSelectionScale,
                ConfigContract.MIN_HONEYCOMB_SELECTION_SCALE,
                ConfigContract.MAX_HONEYCOMB_SELECTION_SCALE);
        this.honeycombEmptyTapClose = honeycombEmptyTapClose;
        this.honeycombTargets = Collections.unmodifiableList(honeycombTargets);
    }

    static GestureConfig defaults() {
        GestureConfig result = new GestureConfig(true, true, ConfigContract.DEFAULT_FAN_SHADOW,
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
                new ArrayList<>(), ConfigContract.DEFAULT_HONEYCOMB_ENABLED,
                ConfigContract.DEFAULT_HONEYCOMB_MODE,
                ConfigContract.DEFAULT_HONEYCOMB_ICON_SIZE_DP,
                ConfigContract.DEFAULT_HONEYCOMB_SPACING_DP,
                ConfigContract.DEFAULT_HONEYCOMB_ANIMATION_SPEED,
                ConfigContract.DEFAULT_HONEYCOMB_INERTIA,
                ConfigContract.DEFAULT_HONEYCOMB_CENTER_SCALE,
                ConfigContract.DEFAULT_HONEYCOMB_EDGE_SCALE,
                ConfigContract.DEFAULT_HONEYCOMB_SELECTION_SCALE,
                ConfigContract.DEFAULT_HONEYCOMB_EMPTY_TAP_CLOSE,
                new ArrayList<>());
        result.pressureTriggers = Collections.singletonList(PressureTrigger.defaultTrigger());
        return result;
    }

    static GestureConfig from(Bundle bundle) {
        if (bundle == null) return defaults();
        ArrayList<TargetSpec> targets = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(bundle.getString(ConfigContract.KEY_COMPONENTS, "[]"));
            for (int i = 0; i < Math.min(ConfigContract.MAX_FAN_MAX_TARGETS,
                    array.length()); i++) {
                Object raw = array.opt(i);
                TargetSpec target = null;
                if (raw instanceof JSONObject) {
                    JSONObject object = (JSONObject) raw;
                    String type = object.optString("type");
                    if ("shortcut".equals(type) || "launcher_shortcut".equals(type)) {
                        String packageName = object.optString("package");
                        String shortcutId = object.optString("id");
                        if (!packageName.isEmpty() && !shortcutId.isEmpty()) {
                            target = "launcher_shortcut".equals(type)
                                    ? TargetSpec.launcherShortcut(packageName, shortcutId,
                                    object.optString("label"), object.optString("intent"),
                                    object.optInt("userId", 0))
                                    : TargetSpec.shortcut(packageName, shortcutId,
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
        ArrayList<TargetSpec> honeycombTargets = parseTargets(bundle.getString(
                ConfigContract.KEY_HONEYCOMB_COMPONENTS, "[]"), 60);
        GestureConfig result = new GestureConfig(
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
                targets,
                bundle.getBoolean(ConfigContract.KEY_HONEYCOMB_ENABLED,
                        ConfigContract.DEFAULT_HONEYCOMB_ENABLED),
                bundle.getInt(ConfigContract.KEY_HONEYCOMB_MODE,
                        ConfigContract.DEFAULT_HONEYCOMB_MODE),
                bundle.getInt(ConfigContract.KEY_HONEYCOMB_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_ICON_SIZE_DP),
                bundle.getInt(ConfigContract.KEY_HONEYCOMB_SPACING_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_SPACING_DP),
                bundle.getInt(ConfigContract.KEY_HONEYCOMB_ANIMATION_SPEED,
                        ConfigContract.DEFAULT_HONEYCOMB_ANIMATION_SPEED),
                bundle.getInt(ConfigContract.KEY_HONEYCOMB_INERTIA,
                        ConfigContract.DEFAULT_HONEYCOMB_INERTIA),
                bundle.getInt(ConfigContract.KEY_HONEYCOMB_CENTER_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_CENTER_SCALE),
                bundle.getInt(ConfigContract.KEY_HONEYCOMB_EDGE_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_EDGE_SCALE),
                bundle.getInt(ConfigContract.KEY_HONEYCOMB_SELECTION_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_SELECTION_SCALE),
                bundle.getBoolean(ConfigContract.KEY_HONEYCOMB_EMPTY_TAP_CLOSE,
                        ConfigContract.DEFAULT_HONEYCOMB_EMPTY_TAP_CLOSE),
                honeycombTargets);
        result.fanMaxTargets = clamp(bundle.getInt(ConfigContract.KEY_FAN_MAX_TARGETS,
                ConfigContract.DEFAULT_FAN_MAX_TARGETS), 3, 24);
        result.forceCircularIcons = bundle.getBoolean(
                ConfigContract.KEY_FORCE_CIRCULAR_ICONS,
                ConfigContract.DEFAULT_FORCE_CIRCULAR_ICONS);
        boolean legacyFixedRows = bundle.getBoolean(
                ConfigContract.KEY_FAN_FIXED_SEVEN_ROWS,
                ConfigContract.DEFAULT_FAN_FIXED_SEVEN_ROWS);
        result.fanLayoutMode = clamp(bundle.getInt(ConfigContract.KEY_FAN_LAYOUT_MODE,
                        legacyFixedRows ? ConfigContract.FAN_LAYOUT_SMART
                                : ConfigContract.DEFAULT_FAN_LAYOUT_MODE),
                ConfigContract.FAN_LAYOUT_SMART, ConfigContract.FAN_LAYOUT_CUSTOM);
        if (result.fanLayoutMode == ConfigContract.FAN_LAYOUT_FIXED_SEVEN) {
            result.fanLayoutMode = ConfigContract.FAN_LAYOUT_SMART;
        }
        result.fanFixedSevenRows = false;
        int configuredInner = clamp(bundle.getInt(
                        ConfigContract.KEY_FAN_CUSTOM_INNER_COUNT,
                        ConfigContract.DEFAULT_FAN_CUSTOM_INNER_COUNT),
                ConfigContract.MIN_FAN_CUSTOM_ROW_COUNT,
                ConfigContract.MAX_FAN_CUSTOM_INNER_COUNT);
        int configuredMiddle = clamp(bundle.getInt(
                        ConfigContract.KEY_FAN_CUSTOM_MIDDLE_COUNT,
                        ConfigContract.DEFAULT_FAN_CUSTOM_MIDDLE_COUNT),
                ConfigContract.MIN_FAN_CUSTOM_ROW_COUNT,
                ConfigContract.MAX_FAN_CUSTOM_MIDDLE_COUNT);
        int[] rowAllocation = FanRowAllocation.normalize(result.targets.size(),
                configuredInner, configuredMiddle);
        result.fanCustomInnerCount = rowAllocation[0];
        result.fanCustomMiddleCount = rowAllocation[1];
        result.fanCustomOuterCount = rowAllocation[2];
        result.bottomPortraitEnabled = bundle.getBoolean(
                ConfigContract.KEY_BOTTOM_PORTRAIT_ENABLED,
                ConfigContract.DEFAULT_BOTTOM_PORTRAIT_ENABLED);
        result.bottomLandscapeEnabled = bundle.getBoolean(
                ConfigContract.KEY_BOTTOM_LANDSCAPE_ENABLED,
                ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_ENABLED);
        result.bottomPortraitSecondStageEnabled = bundle.getBoolean(
                ConfigContract.KEY_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED,
                ConfigContract.DEFAULT_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED);
        result.bottomPortraitFirstPressureLaunch = bundle.getBoolean(
                ConfigContract.KEY_BOTTOM_PORTRAIT_FIRST_PRESSURE_LAUNCH,
                ConfigContract.DEFAULT_BOTTOM_PORTRAIT_FIRST_PRESSURE_LAUNCH);
        result.bottomPortraitSecondPressureLaunch = bundle.getBoolean(
                ConfigContract.KEY_BOTTOM_PORTRAIT_SECOND_PRESSURE_LAUNCH,
                ConfigContract.DEFAULT_BOTTOM_PORTRAIT_SECOND_PRESSURE_LAUNCH);
        result.bottomLandscapeSecondStageEnabled = bundle.getBoolean(
                ConfigContract.KEY_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED,
                ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED);
        result.bottomHoneycombSettleMs = clamp(bundle.getInt(
                        ConfigContract.KEY_BOTTOM_HONEYCOMB_SETTLE_MS,
                        ConfigContract.DEFAULT_BOTTOM_HONEYCOMB_SETTLE_MS),
                ConfigContract.MIN_BOTTOM_HONEYCOMB_SETTLE_MS,
                ConfigContract.MAX_BOTTOM_HONEYCOMB_SETTLE_MS);
        result.customWindowPortraitEnabled = bundle.getBoolean(
                ConfigContract.KEY_CUSTOM_WINDOW_PORTRAIT_ENABLED,
                bundle.getBoolean(ConfigContract.KEY_CUSTOM_WINDOW_BOUNDS_ENABLED,
                        ConfigContract.DEFAULT_CUSTOM_WINDOW_PORTRAIT_ENABLED));
        result.customWindowLandscapeEnabled = bundle.getBoolean(
                ConfigContract.KEY_CUSTOM_WINDOW_LANDSCAPE_ENABLED,
                ConfigContract.DEFAULT_CUSTOM_WINDOW_LANDSCAPE_ENABLED);
        result.portraitProportionalSizeEnabled = bundle.getBoolean(
                ConfigContract.KEY_PORTRAIT_PROPORTIONAL_SIZE_ENABLED,
                ConfigContract.DEFAULT_PORTRAIT_PROPORTIONAL_SIZE_ENABLED);
        result.landscapeProportionalSizeEnabled = bundle.getBoolean(
                ConfigContract.KEY_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED,
                ConfigContract.DEFAULT_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED);
        result.portraitNativeScalePercent = clamp(bundle.getInt(
                        ConfigContract.KEY_PORTRAIT_NATIVE_SCALE_PERCENT,
                        ConfigContract.DEFAULT_NATIVE_WINDOW_SCALE_PERCENT),
                ConfigContract.MIN_NATIVE_WINDOW_SCALE_PERCENT,
                ConfigContract.MAX_NATIVE_WINDOW_SCALE_PERCENT);
        result.sidePortraitEnabled = bundle.getBoolean(
                ConfigContract.KEY_SIDE_PORTRAIT_ENABLED,
                ConfigContract.DEFAULT_SIDE_PORTRAIT_ENABLED);
        result.sideLandscapeEnabled = bundle.getBoolean(
                ConfigContract.KEY_SIDE_LANDSCAPE_ENABLED,
                ConfigContract.DEFAULT_SIDE_LANDSCAPE_ENABLED);
        result.sidePortraitLayoutMode = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_PORTRAIT_LAYOUT_MODE,
                        result.sideLayoutMode), ConfigContract.SIDE_LAYOUT_LIST,
                ConfigContract.SIDE_LAYOUT_SYSTEM_RECENTS);
        result.sideLandscapeLayoutMode = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_LANDSCAPE_LAYOUT_MODE,
                        result.sideLayoutMode), ConfigContract.SIDE_LAYOUT_LIST,
                ConfigContract.SIDE_LAYOUT_SYSTEM_RECENTS);
        result.landscapeWidthPercent = clamp(bundle.getInt(
                ConfigContract.KEY_LANDSCAPE_WIDTH_PERCENT,
                ConfigContract.DEFAULT_LANDSCAPE_WIDTH_PERCENT),
                ConfigContract.MIN_LANDSCAPE_WIDTH_PERCENT,
                ConfigContract.MAX_LANDSCAPE_WIDTH_PERCENT);
        result.landscapeHeightPercent = clamp(bundle.getInt(
                ConfigContract.KEY_LANDSCAPE_HEIGHT_PERCENT,
                ConfigContract.DEFAULT_LANDSCAPE_HEIGHT_PERCENT), 35, 85);
        result.landscapeNativeScalePercent = clamp(bundle.getInt(
                        ConfigContract.KEY_LANDSCAPE_NATIVE_SCALE_PERCENT,
                        ConfigContract.DEFAULT_NATIVE_WINDOW_SCALE_PERCENT),
                ConfigContract.MIN_NATIVE_WINDOW_SCALE_PERCENT,
                ConfigContract.MAX_NATIVE_WINDOW_SCALE_PERCENT);
        result.landscapePositionX = clamp(bundle.getInt(
                ConfigContract.KEY_LANDSCAPE_POSITION_X,
                ConfigContract.DEFAULT_LANDSCAPE_POSITION_X), 0, 100);
        result.landscapePositionY = clamp(bundle.getInt(
                ConfigContract.KEY_LANDSCAPE_POSITION_Y,
                ConfigContract.DEFAULT_LANDSCAPE_POSITION_Y), 0, 100);
        result.honeycombMaxTargets = clamp(bundle.getInt(
                ConfigContract.KEY_HONEYCOMB_MAX_TARGETS,
                ConfigContract.DEFAULT_HONEYCOMB_MAX_TARGETS), 1, 60);
        result.sideMaxTargets = clamp(bundle.getInt(ConfigContract.KEY_SIDE_MAX_TARGETS,
                ConfigContract.DEFAULT_SIDE_MAX_TARGETS), 1, 36);
        result.sideFollowHoneycomb = bundle.getBoolean(
                ConfigContract.KEY_SIDE_FOLLOW_HONEYCOMB,
                ConfigContract.DEFAULT_SIDE_FOLLOW_HONEYCOMB);
        result.sideDirectionHorizontal = bundle.getBoolean(
                ConfigContract.KEY_SIDE_DIRECTION_HORIZONTAL, true);
        result.sideDirectionUp = bundle.getBoolean(ConfigContract.KEY_SIDE_DIRECTION_UP, true);
        result.sideDirectionDown = bundle.getBoolean(ConfigContract.KEY_SIDE_DIRECTION_DOWN, true);
        result.sideHoldEnabled = bundle.getBoolean(ConfigContract.KEY_SIDE_HOLD_ENABLED,
                ConfigContract.DEFAULT_SIDE_HOLD_ENABLED);
        result.sideHoldDelayMs = clamp(bundle.getInt(ConfigContract.KEY_SIDE_HOLD_DELAY_MS,
                        ConfigContract.DEFAULT_SIDE_HOLD_DELAY_MS),
                ConfigContract.MIN_SIDE_HOLD_DELAY_MS,
                ConfigContract.MAX_SIDE_HOLD_DELAY_MS);
        result.sideTaskMaxCount = clamp(bundle.getInt(ConfigContract.KEY_SIDE_TASK_MAX_COUNT,
                        ConfigContract.DEFAULT_SIDE_TASK_MAX_COUNT),
                ConfigContract.MIN_SIDE_TASK_MAX_COUNT,
                ConfigContract.MAX_SIDE_TASK_MAX_COUNT);
        result.sideTaskCardWidthDp = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_TASK_CARD_WIDTH_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_WIDTH_DP),
                ConfigContract.MIN_SIDE_TASK_CARD_WIDTH_DP,
                ConfigContract.MAX_SIDE_TASK_CARD_WIDTH_DP);
        result.sideTaskCardHeightDp = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_TASK_CARD_HEIGHT_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_HEIGHT_DP),
                ConfigContract.MIN_SIDE_TASK_CARD_HEIGHT_DP,
                ConfigContract.MAX_SIDE_TASK_CARD_HEIGHT_DP);
        result.sideTaskCardCornerDp = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_TASK_CARD_CORNER_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_CORNER_DP),
                ConfigContract.MIN_SIDE_TASK_CARD_CORNER_DP,
                ConfigContract.MAX_SIDE_TASK_CARD_CORNER_DP);
        result.sideTaskIconSizeDp = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_TASK_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_ICON_SIZE_DP),
                ConfigContract.MIN_SIDE_TASK_ICON_SIZE_DP,
                ConfigContract.MAX_SIDE_TASK_ICON_SIZE_DP);
        result.sideTaskIconGapDp = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_TASK_ICON_GAP_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_ICON_GAP_DP),
                ConfigContract.MIN_SIDE_TASK_ICON_GAP_DP,
                ConfigContract.MAX_SIDE_TASK_ICON_GAP_DP);
        result.sideTaskFingerOffsetDp = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_TASK_FINGER_OFFSET_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_FINGER_OFFSET_DP),
                ConfigContract.MIN_SIDE_TASK_FINGER_OFFSET_DP,
                ConfigContract.MAX_SIDE_TASK_FINGER_OFFSET_DP);
        result.sideTaskLayoutMode = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_TASK_LAYOUT_MODE,
                ConfigContract.DEFAULT_SIDE_TASK_LAYOUT_MODE),
                ConfigContract.SIDE_TASK_LAYOUT_FLAT,
                ConfigContract.SIDE_TASK_LAYOUT_ICONS);
        result.sideTaskReverseOrder = bundle.getBoolean(
                ConfigContract.KEY_SIDE_TASK_REVERSE_ORDER,
                ConfigContract.DEFAULT_SIDE_TASK_REVERSE_ORDER);
        result.sideTaskShowName = bundle.getBoolean(
                ConfigContract.KEY_SIDE_TASK_SHOW_NAME,
                ConfigContract.DEFAULT_SIDE_TASK_SHOW_NAME);
        result.sideTaskMotionMode = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_TASK_MOTION_MODE,
                        ConfigContract.DEFAULT_SIDE_TASK_MOTION_MODE),
                ConfigContract.SIDE_TASK_MOTION_APPLE,
                ConfigContract.SIDE_TASK_MOTION_MARBLE);
        result.sideTaskSwipeSpeedPercent = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_TASK_SWIPE_SPEED_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TASK_SWIPE_SPEED_PERCENT),
                ConfigContract.MIN_SIDE_TASK_SWIPE_SPEED_PERCENT,
                ConfigContract.MAX_SIDE_TASK_SWIPE_SPEED_PERCENT);
        result.sideTaskAnimationSpeed = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_TASK_ANIMATION_SPEED,
                        ConfigContract.DEFAULT_SIDE_TASK_ANIMATION_SPEED), 0, 4);
        result.sideTaskExtendedDownwardTolerance = bundle.getBoolean(
                ConfigContract.KEY_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE,
                ConfigContract.DEFAULT_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE);
        result.bottomAnimationSpeed = clamp(bundle.getInt(
                        ConfigContract.KEY_BOTTOM_ANIMATION_SPEED,
                        result.fanAnimationSpeed),
                ConfigContract.MIN_FAN_ANIMATION_SPEED,
                ConfigContract.MAX_FAN_ANIMATION_SPEED);
        result.bottomTriggerHaptic = bundle.getBoolean(
                ConfigContract.KEY_BOTTOM_TRIGGER_HAPTIC, result.haptic);
        result.selectionTransformLevel = clamp(bundle.getInt(
                        ConfigContract.KEY_SELECTION_TRANSFORM_LEVEL,
                        ConfigContract.DEFAULT_SELECTION_TRANSFORM_LEVEL),
                ConfigContract.SELECTION_TRANSFORM_OFF,
                ConfigContract.SELECTION_TRANSFORM_STRONG);
        result.honeycombCenteredSystemAnimation = bundle.getBoolean(
                ConfigContract.KEY_HONEYCOMB_CENTERED_SYSTEM_ANIMATION,
                ConfigContract.DEFAULT_HONEYCOMB_CENTERED_SYSTEM_ANIMATION);
        result.sideFullscreen = bundle.getBoolean(
                ConfigContract.KEY_SIDE_FULLSCREEN,
                ConfigContract.DEFAULT_SIDE_FULLSCREEN);
        result.bottomFullscreen = bundle.getBoolean(
                ConfigContract.KEY_BOTTOM_FULLSCREEN,
                ConfigContract.DEFAULT_BOTTOM_FULLSCREEN);
        result.bottomHoneycombFreeform = bundle.getBoolean(
                ConfigContract.KEY_BOTTOM_HONEYCOMB_FREEFORM,
                ConfigContract.DEFAULT_BOTTOM_HONEYCOMB_FREEFORM);
        result.sidePortraitFullscreen = bundle.getBoolean(
                ConfigContract.KEY_SIDE_PORTRAIT_FULLSCREEN, result.sideFullscreen);
        result.sideLandscapeFullscreen = bundle.getBoolean(
                ConfigContract.KEY_SIDE_LANDSCAPE_FULLSCREEN, result.sideFullscreen);
        result.bottomPortraitFullscreen = bundle.getBoolean(
                ConfigContract.KEY_BOTTOM_PORTRAIT_FULLSCREEN, result.bottomFullscreen);
        result.bottomLandscapeFullscreen = bundle.getBoolean(
                ConfigContract.KEY_BOTTOM_LANDSCAPE_FULLSCREEN, result.bottomFullscreen);
        result.bottomPortraitHoneycombFreeform = bundle.getBoolean(
                ConfigContract.KEY_BOTTOM_PORTRAIT_HONEYCOMB_FREEFORM,
                result.bottomHoneycombFreeform);
        result.bottomLandscapeHoneycombFreeform = bundle.getBoolean(
                ConfigContract.KEY_BOTTOM_LANDSCAPE_HONEYCOMB_FREEFORM,
                result.bottomHoneycombFreeform);
        result.sideAnimationsEnabled = bundle.getBoolean(
                ConfigContract.KEY_SIDE_ANIMATIONS_ENABLED,
                ConfigContract.DEFAULT_SIDE_ANIMATIONS_ENABLED);
        result.sideAnimationSpeed = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_ANIMATION_SPEED,
                        ConfigContract.DEFAULT_SIDE_ANIMATION_SPEED),
                ConfigContract.MIN_FAN_ANIMATION_SPEED,
                ConfigContract.MAX_FAN_ANIMATION_SPEED);
        result.sideRevealAmount = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_REVEAL_AMOUNT,
                        ConfigContract.DEFAULT_SIDE_REVEAL_AMOUNT),
                ConfigContract.MIN_FAN_REVEAL_AMOUNT,
                ConfigContract.MAX_FAN_REVEAL_AMOUNT);
        result.sideRotationDegrees = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_ROTATION_DEGREES,
                        ConfigContract.DEFAULT_SIDE_ROTATION_DEGREES),
                ConfigContract.MIN_FAN_ROTATION_DEGREES,
                ConfigContract.MAX_FAN_ROTATION_DEGREES);
        result.sideSelectionScalePercent = clamp(bundle.getInt(
                        ConfigContract.KEY_SIDE_SELECTION_SCALE_PERCENT,
                        ConfigContract.DEFAULT_SIDE_SELECTION_SCALE_PERCENT),
                ConfigContract.MIN_FAN_SELECTION_SCALE_PERCENT,
                ConfigContract.MAX_FAN_SELECTION_SCALE_PERCENT);
        result.sideSelectionRing = bundle.getBoolean(
                ConfigContract.KEY_SIDE_SELECTION_RING,
                ConfigContract.DEFAULT_SIDE_SELECTION_RING);
        result.honeycombFollowFinger = bundle.getBoolean(
                ConfigContract.KEY_HONEYCOMB_FOLLOW_FINGER,
                ConfigContract.DEFAULT_HONEYCOMB_FOLLOW_FINGER);
        result.honeycombLandscapeEnabled = bundle.getBoolean(
                ConfigContract.KEY_HONEYCOMB_LANDSCAPE_ENABLED,
                ConfigContract.DEFAULT_HONEYCOMB_LANDSCAPE_ENABLED);
        result.honeycombFixedXPercent = clamp(bundle.getInt(
                ConfigContract.KEY_HONEYCOMB_FIXED_X_PERCENT,
                ConfigContract.DEFAULT_HONEYCOMB_FIXED_X_PERCENT), 0, 100);
        result.honeycombFixedYPercent = clamp(bundle.getInt(
                ConfigContract.KEY_HONEYCOMB_FIXED_Y_PERCENT,
                ConfigContract.DEFAULT_HONEYCOMB_FIXED_Y_PERCENT), 0, 100);
        result.honeycombBackgroundStyle = clamp(bundle.getInt(
                ConfigContract.KEY_HONEYCOMB_BACKGROUND_STYLE, 0), 0, 1);
        result.honeycombBlurDp = clamp(bundle.getInt(
                ConfigContract.KEY_HONEYCOMB_BLUR_DP, 36), 0, 60);
        result.honeycombDimPercent = clamp(bundle.getInt(
                ConfigContract.KEY_HONEYCOMB_DIM_PERCENT, 22), 0, 60);
        result.honeycombAppBackgroundEnabled = bundle.getBoolean(
                ConfigContract.KEY_HONEYCOMB_APP_BACKGROUND_ENABLED,
                ConfigContract.DEFAULT_HONEYCOMB_APP_BACKGROUND_ENABLED);
        result.honeycombLiveBlurEnabled = bundle.getBoolean(
                ConfigContract.KEY_HONEYCOMB_LIVE_BLUR_ENABLED,
                ConfigContract.DEFAULT_HONEYCOMB_LIVE_BLUR_ENABLED);
        result.honeycombLiveBlurDp = clamp(bundle.getInt(
                ConfigContract.KEY_HONEYCOMB_LIVE_BLUR_DP,
                ConfigContract.DEFAULT_HONEYCOMB_LIVE_BLUR_DP), 0, 60);
        result.honeycombBackgroundDimPercent = clamp(bundle.getInt(
                ConfigContract.KEY_HONEYCOMB_BACKGROUND_DIM_PERCENT,
                ConfigContract.DEFAULT_HONEYCOMB_BACKGROUND_DIM_PERCENT), 0, 60);
        result.honeycombDiscSizePercent = clamp(bundle.getInt(
                ConfigContract.KEY_HONEYCOMB_DISC_SIZE_PERCENT,
                ConfigContract.DEFAULT_HONEYCOMB_DISC_SIZE_PERCENT),
                ConfigContract.MIN_HONEYCOMB_DISC_SIZE_PERCENT,
                ConfigContract.MAX_HONEYCOMB_DISC_SIZE_PERCENT);
        result.honeycombShowSelectedName = bundle.getBoolean(
                ConfigContract.KEY_HONEYCOMB_SHOW_SELECTED_NAME,
                ConfigContract.DEFAULT_HONEYCOMB_SHOW_SELECTED_NAME);
        result.outsideTapWindowMs = clamp(bundle.getInt(
                ConfigContract.KEY_OUTSIDE_TAP_WINDOW_MS,
                ConfigContract.DEFAULT_OUTSIDE_TAP_WINDOW_MS),
                ConfigContract.MIN_OUTSIDE_TAP_WINDOW_MS,
                ConfigContract.MAX_OUTSIDE_TAP_WINDOW_MS);
        result.outsidePortraitEnabled = bundle.getBoolean(
                ConfigContract.KEY_OUTSIDE_PORTRAIT_ENABLED,
                bundle.getBoolean(ConfigContract.KEY_OUTSIDE_ENABLED,
                        ConfigContract.DEFAULT_OUTSIDE_PORTRAIT_ENABLED));
        result.outsideLandscapeEnabled = bundle.getBoolean(
                ConfigContract.KEY_OUTSIDE_LANDSCAPE_ENABLED,
                ConfigContract.DEFAULT_OUTSIDE_LANDSCAPE_ENABLED);
        result.sideTargets = Collections.unmodifiableList(parseTargets(bundle.getString(
                ConfigContract.KEY_SIDE_COMPONENTS, "[]"), 36));
        result.pressureTargets = Collections.unmodifiableList(parseTargets(bundle.getString(
                ConfigContract.KEY_PRESSURE_COMPONENTS, "[]"),
                ConfigContract.MAX_FAN_MAX_TARGETS));
        result.pressureAction = clamp(bundle.getInt(ConfigContract.KEY_PRESSURE_ACTION,
                        ConfigContract.DEFAULT_PRESSURE_ACTION),
                ConfigContract.PRESSURE_ACTION_HONEYCOMB,
                ConfigContract.PRESSURE_ACTION_SINGLE_TARGET);
        result.pressureTriggers = Collections.unmodifiableList(parsePressureTriggers(bundle,
                result.pressureAction));
        result.pressureOpenAsFreeform = bundle.getBoolean(
                ConfigContract.KEY_PRESSURE_OPEN_AS_FREEFORM,
                ConfigContract.DEFAULT_PRESSURE_OPEN_AS_FREEFORM);
        result.pressureHeavyLaunchEnabled = bundle.getBoolean(
                ConfigContract.KEY_PRESSURE_HEAVY_LAUNCH_ENABLED,
                ConfigContract.DEFAULT_PRESSURE_HEAVY_LAUNCH_ENABLED);
        result.pressureHapticMode = clamp(bundle.getInt(
                        ConfigContract.KEY_PRESSURE_HAPTIC_MODE,
                        ConfigContract.DEFAULT_PRESSURE_HAPTIC_MODE),
                ConfigContract.PRESSURE_HAPTIC_SYSTEM,
                ConfigContract.PRESSURE_HAPTIC_CUSTOM);
        int sensorRateMode = bundle.getInt(ConfigContract.KEY_PRESSURE_SENSOR_RATE_MODE,
                ConfigContract.DEFAULT_PRESSURE_SENSOR_RATE_MODE);
        if (sensorRateMode != ConfigContract.PRESSURE_SENSOR_RATE_BALANCED
                && sensorRateMode != ConfigContract.PRESSURE_SENSOR_RATE_ECO) {
            sensorRateMode = ConfigContract.PRESSURE_SENSOR_RATE_BALANCED;
        }
        result.pressureSensorRateMode = sensorRateMode;
        result.pressureGestureEnabled = bundle.getBoolean(
                ConfigContract.KEY_PRESSURE_GESTURE_ENABLED,
                ConfigContract.DEFAULT_PRESSURE_GESTURE_ENABLED);
        result.pressureCenterXPercent = clamp(bundle.getInt(
                ConfigContract.KEY_PRESSURE_CENTER_X_PERCENT,
                ConfigContract.DEFAULT_PRESSURE_CENTER_X_PERCENT), 0, 100);
        result.pressureCenterYPercent = clamp(bundle.getInt(
                ConfigContract.KEY_PRESSURE_CENTER_Y_PERCENT,
                ConfigContract.DEFAULT_PRESSURE_CENTER_Y_PERCENT), 0, 100);
        result.pressureRadiusPercent = clamp(bundle.getInt(
                        ConfigContract.KEY_PRESSURE_RADIUS_PERCENT,
                        ConfigContract.DEFAULT_PRESSURE_RADIUS_PERCENT),
                ConfigContract.MIN_PRESSURE_RADIUS_PERCENT,
                ConfigContract.MAX_PRESSURE_RADIUS_PERCENT);
        result.pressureThreshold = clamp(bundle.getFloat(
                        ConfigContract.KEY_PRESSURE_THRESHOLD,
                        ConfigContract.DEFAULT_PRESSURE_THRESHOLD),
                0f, ConfigContract.MAX_PRESSURE_THRESHOLD);
        result.pressureCalibrationValidCount = clamp(bundle.getInt(
                ConfigContract.KEY_PRESSURE_CALIBRATION_VALID_COUNT,
                ConfigContract.DEFAULT_PRESSURE_CALIBRATION_VALID_COUNT), 0, 5);
        result.pressureCalibrated = bundle.getBoolean(
                ConfigContract.KEY_PRESSURE_CALIBRATED,
                ConfigContract.DEFAULT_PRESSURE_CALIBRATED)
                && result.pressureThreshold > 0f
                && result.pressureCalibrationValidCount >= 3;
        result.pressureShowPosition = bundle.getBoolean(
                ConfigContract.KEY_PRESSURE_SHOW_POSITION,
                ConfigContract.DEFAULT_PRESSURE_SHOW_POSITION);
        result.pressureOrbTheme = clamp(bundle.getInt(
                        ConfigContract.KEY_PRESSURE_ORB_THEME,
                        ConfigContract.DEFAULT_PRESSURE_ORB_THEME),
                ConfigContract.PRESSURE_ORB_ORBITS,
                ConfigContract.PRESSURE_ORB_MORPH);
        result.pressureThemeEnabled = bundle.getBoolean(
                ConfigContract.KEY_PRESSURE_THEME_ENABLED,
                ConfigContract.DEFAULT_PRESSURE_THEME_ENABLED);
        result.pressureOrbSizePercent = clamp(bundle.getInt(
                        ConfigContract.KEY_PRESSURE_ORB_SIZE_PERCENT,
                        ConfigContract.DEFAULT_PRESSURE_ORB_SIZE_PERCENT),
                ConfigContract.MIN_PRESSURE_ORB_SIZE_PERCENT,
                ConfigContract.MAX_PRESSURE_ORB_SIZE_PERCENT);
        result.pressureFirstHapticEnabled = bundle.getBoolean(
                ConfigContract.KEY_PRESSURE_FIRST_HAPTIC_ENABLED,
                ConfigContract.DEFAULT_PRESSURE_FIRST_HAPTIC_ENABLED);
        result.pressureFirstHapticDurationMs = clamp(bundle.getInt(
                ConfigContract.KEY_PRESSURE_FIRST_HAPTIC_DURATION_MS,
                ConfigContract.DEFAULT_PRESSURE_FIRST_HAPTIC_DURATION_MS),
                ConfigContract.MIN_PRESSURE_FIRST_HAPTIC_DURATION_MS,
                ConfigContract.MAX_PRESSURE_FIRST_HAPTIC_DURATION_MS);
        result.pressureFirstHapticAmplitude = clamp(bundle.getInt(
                ConfigContract.KEY_PRESSURE_FIRST_HAPTIC_AMPLITUDE,
                ConfigContract.DEFAULT_PRESSURE_FIRST_HAPTIC_AMPLITUDE),
                ConfigContract.MIN_PRESSURE_FIRST_HAPTIC_AMPLITUDE,
                ConfigContract.MAX_PRESSURE_FIRST_HAPTIC_AMPLITUDE);
        result.pressureSecondHapticEnabled = bundle.getBoolean(
                ConfigContract.KEY_PRESSURE_SECOND_HAPTIC_ENABLED,
                ConfigContract.DEFAULT_PRESSURE_SECOND_HAPTIC_ENABLED);
        result.pressureSecondHapticDurationMs = clamp(bundle.getInt(
                ConfigContract.KEY_PRESSURE_SECOND_HAPTIC_DURATION_MS,
                ConfigContract.DEFAULT_PRESSURE_SECOND_HAPTIC_DURATION_MS),
                ConfigContract.MIN_PRESSURE_SECOND_HAPTIC_DURATION_MS,
                ConfigContract.MAX_PRESSURE_SECOND_HAPTIC_DURATION_MS);
        result.pressureSecondHapticAmplitude = clamp(bundle.getInt(
                ConfigContract.KEY_PRESSURE_SECOND_HAPTIC_AMPLITUDE,
                ConfigContract.DEFAULT_PRESSURE_SECOND_HAPTIC_AMPLITUDE),
                ConfigContract.MIN_PRESSURE_SECOND_HAPTIC_AMPLITUDE,
                ConfigContract.MAX_PRESSURE_SECOND_HAPTIC_AMPLITUDE);
        result.pressureCalibrationActive = bundle.getBoolean(
                ConfigContract.KEY_PRESSURE_CALIBRATION_ACTIVE,
                ConfigContract.DEFAULT_PRESSURE_CALIBRATION_ACTIVE);
        return result;
    }

    boolean ready() {
        return enabled && targets.size() >= 3;
    }

    boolean bottomEnabledFor(boolean landscape) {
        return landscape ? bottomLandscapeEnabled : bottomPortraitEnabled;
    }

    boolean sideEnabledFor(boolean landscape) {
        return sideGestureEnabled
                && (landscape ? sideLandscapeEnabled : sidePortraitEnabled);
    }

    boolean bottomSecondStageEnabledFor(boolean landscape) {
        return landscape ? bottomLandscapeSecondStageEnabled
                : bottomPortraitSecondStageEnabled;
    }

    boolean bottomFullscreenFor(boolean landscape) {
        return landscape ? bottomLandscapeFullscreen : bottomPortraitFullscreen;
    }

    boolean bottomHoneycombFreeformFor(boolean landscape) {
        return landscape ? bottomLandscapeHoneycombFreeform
                : bottomPortraitHoneycombFreeform;
    }

    boolean bottomFirstPressureLaunchFor(boolean landscape) {
        return !landscape && bottomPortraitFirstPressureLaunch;
    }

    boolean bottomSecondPressureLaunchFor(boolean landscape) {
        return !landscape && bottomPortraitSecondPressureLaunch;
    }

    int sideLayoutModeFor(boolean landscape) {
        return landscape ? sideLandscapeLayoutMode : sidePortraitLayoutMode;
    }

    boolean sideFullscreenFor(boolean landscape) {
        return landscape ? sideLandscapeFullscreen : sidePortraitFullscreen;
    }

    boolean honeycombEnabledFor(boolean landscape) {
        return honeycombEnabled;
    }

    boolean customWindowBoundsEnabledFor(boolean landscape) {
        return landscape ? customWindowLandscapeEnabled : customWindowPortraitEnabled;
    }

    boolean outsideEnabledFor(boolean landscape) {
        return landscape ? outsideLandscapeEnabled : outsidePortraitEnabled;
    }

    int windowWidthPercent(boolean landscape) {
        return landscape ? landscapeWidthPercent : widthPercent;
    }

    int windowHeightPercent(boolean landscape) {
        return landscape ? landscapeHeightPercent : heightPercent;
    }

    boolean nativeWindowScalingEnabled(boolean landscape) {
        return landscape ? landscapeProportionalSizeEnabled
                : portraitProportionalSizeEnabled;
    }

    int nativeWindowScalePercent(boolean landscape) {
        return landscape ? landscapeNativeScalePercent : portraitNativeScalePercent;
    }

    int windowPositionX(boolean landscape) {
        return landscape ? landscapePositionX : positionX;
    }

    int windowPositionY(boolean landscape) {
        return landscape ? landscapePositionY : positionY;
    }

    private static TargetSpec validActivity(String component, int userId) {
        return android.content.ComponentName.unflattenFromString(component) == null
                ? null : TargetSpec.activity(component, userId);
    }

    private static ArrayList<TargetSpec> parseTargets(String raw, int maximum) {
        ArrayList<TargetSpec> targets = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw == null ? "[]" : raw);
            for (int i = 0; i < Math.min(maximum, array.length()); i++) {
                Object rawTarget = array.opt(i);
                TargetSpec target = null;
                if (rawTarget instanceof JSONObject) {
                    JSONObject object = (JSONObject) rawTarget;
                    String type = object.optString("type");
                    if ("shortcut".equals(type) || "launcher_shortcut".equals(type)) {
                        String packageName = object.optString("package");
                        String shortcutId = object.optString("id");
                        if (!packageName.isEmpty() && !shortcutId.isEmpty()) {
                            target = "launcher_shortcut".equals(type)
                                    ? TargetSpec.launcherShortcut(packageName, shortcutId,
                                    object.optString("label"), object.optString("intent"),
                                    object.optInt("userId", 0))
                                    : TargetSpec.shortcut(packageName, shortcutId,
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
        } catch (Exception ignored) { }
        return targets;
    }

    private static ArrayList<PressureTrigger> parsePressureTriggers(Bundle bundle,
                                                                      int legacyAction) {
        ArrayList<PressureTrigger> triggers = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(bundle.getString(
                    ConfigContract.KEY_PRESSURE_TRIGGERS, "[]"));
            for (int i = 0; i < Math.min(ConfigContract.MAX_PRESSURE_TRIGGERS,
                    array.length()); i++) {
                PressureTrigger trigger = PressureTrigger.fromJson(array.optJSONObject(i));
                if (trigger != null && !containsPressureTrigger(triggers, trigger.id)) {
                    triggers.add(trigger);
                }
            }
        } catch (Exception ignored) { }
        if (triggers.isEmpty()) {
            triggers.add(new PressureTrigger(1, true,
                    bundle.getInt(ConfigContract.KEY_PRESSURE_CENTER_X_PERCENT,
                            ConfigContract.DEFAULT_PRESSURE_CENTER_X_PERCENT),
                    bundle.getInt(ConfigContract.KEY_PRESSURE_CENTER_Y_PERCENT,
                            ConfigContract.DEFAULT_PRESSURE_CENTER_Y_PERCENT),
                    bundle.getInt(ConfigContract.KEY_PRESSURE_RADIUS_PERCENT,
                            ConfigContract.DEFAULT_PRESSURE_RADIUS_PERCENT),
                    legacyAction, null));
        }
        return triggers;
    }

    private static boolean containsPressureTrigger(List<PressureTrigger> triggers, int id) {
        for (PressureTrigger trigger : triggers) if (trigger.id == id) return true;
        return false;
    }

    private static boolean containsTarget(List<TargetSpec> targets, TargetSpec candidate) {
        for (TargetSpec target : targets) {
            if (target.isShortcut() == candidate.isShortcut()
                    && target.component.equals(candidate.component)
                    && target.packageName.equals(candidate.packageName)
                    && target.shortcutId.equals(candidate.shortcutId)
                    && target.shortcutIntentUri.equals(candidate.shortcutIntentUri)
                    && target.userId == candidate.userId) return true;
        }
        return false;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static int supportedOutsideAction(int action) {
        return action == ConfigContract.ACTION_CLOSE
                || action == ConfigContract.ACTION_PIN
                || action == ConfigContract.ACTION_FULLSCREEN
                || action == ConfigContract.ACTION_EDGE_PIN
                ? action : ConfigContract.ACTION_NONE;
    }
}
