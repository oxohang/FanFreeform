package com.oxohang.fanfreeform.config;

import android.content.Context;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class ConfigStore {
    private static final String KEY_DOUBLE_PIN_MIGRATED = "double_pin_migrated_v1";
    private static final String KEY_SIDE_TARGETS_INDEPENDENT_MIGRATED =
            "side_targets_independent_migrated_v1";
    private static final String KEY_ORIENTATION_BEHAVIOR_MIGRATED =
            "orientation_behavior_migrated_v1";
    private static final String[] DEFAULT_BOTTOM_PACKAGES = {
            "com.ss.android.article.news", "tv.danmaku.bili", "com.twitter.android",
            "nu.gpu.nagram", "com.tencent.mm", "com.ss.android.yumme.video",
            "com.xingin.xhs", "com.xunmeng.pinduoduo", "com.miui.calculator",
            "com.android.purebilibili", "com.tencent.androidqqmail",
            "com.zhiliaoapp.musically", "com.trim.app"
    };
    private static final String[] DEFAULT_HONEYCOMB_PACKAGES = {
            "com.miui.calculator", "tv.danmaku.bili", "com.trim.app",
            "com.ss.android.ugc.aweme", "com.autonavi.minimap",
            "com.ss.android.yumme.video", "com.ss.android.article.news",
            "com.jingdong.app.mall", "com.coolapk.market", "com.luna.music",
            "com.aliyun.tongyi", "com.bytedance.dreamina", "com.quark.clouddrive",
            "com.xiaomi.smarthome", "com.xunmeng.pinduoduo", "com.taobao.taobao",
            "com.tencent.tmgp.sgame", "com.netease.cloudmusic", "com.tencent.mm",
            "com.tencent.weread", "com.taobao.idlefish", "com.miui.gallery",
            "com.xingin.xhs", "com.max.xiaoheihe", "com.unionpay",
            "com.cmbchina.ccd.pluto.cmbActivity", "com.eg.android.AlipayGphone",
            "com.android.purebilibili", "com.openai.chatgpt", "com.android.chrome",
            "com.anthropic.claude", "com.deepseek.chat", "com.alibaba.android.rimet",
            "ai.x.grok", "nu.gpu.nagram", "com.tencent.mobileqq",
            "com.tencent.androidqqmail", "com.android.vending",
            "com.zhiliaoapp.musically", "com.netease.uuremote", "com.twitter.android",
            "com.google.android.youtube", "com.larus.nova"
    };
    private static final String[] DEFAULT_SIDE_PACKAGES = {
            "com.didjdk.adbhelper", "com.lemon.lv", "com.ccb.longjiLife",
            "com.xhey.xcamera", "com.ss.android.article.news", "com.jingdong.app.mall",
            "com.coolapk.market", "com.quark.clouddrive", "com.android.contacts"
    };
    private final Context context;
    private final SharedPreferences preferences;

    public ConfigStore(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(ConfigContract.PREFS, Context.MODE_PRIVATE);
        seedDefaultsIfNeeded(this.context, preferences);
        migrateUnifiedActionsIfNeeded(preferences);
        migrateDoubleTapPinIfNeeded(preferences);
        migrateSideLayoutModeIfNeeded(preferences);
        migrateSelectedAppNameIfNeeded(preferences);
        migrateIndependentSideTargetsIfNeeded(preferences);
        migrateOrientationBehaviorIfNeeded(preferences);
        ensureNativeWindowScaleDefaults(preferences);
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
                Object value = array.opt(i);
                AppTarget target = value instanceof JSONObject
                        ? AppTarget.fromJson((JSONObject) value)
                        : new AppTarget(array.optString(i, ""));
                if (target != null && (target.isShortcut() || target.componentName() != null)
                        && !result.contains(target)) {
                    result.add(target);
                }
            }
        } catch (Exception ignored) {
            // A malformed list is treated as empty; the settings UI can repair it.
        }
        return result;
    }

    public void setTargets(List<AppTarget> targets) {
        JSONArray array = new JSONArray();
        int count = Math.min(ConfigContract.MAX_FAN_MAX_TARGETS, targets.size());
        for (int i = 0; i < count; i++) {
            array.put(targets.get(i).toJson());
        }
        preferences.edit().putString(ConfigContract.KEY_COMPONENTS, array.toString()).apply();
        notifyChanged();
    }

    public List<AppTarget> getHoneycombTargets() {
        ArrayList<AppTarget> result = new ArrayList<>();
        String raw = preferences.getString(ConfigContract.KEY_HONEYCOMB_COMPONENTS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < Math.min(ConfigContract.MAX_HONEYCOMB_MAX_TARGETS,
                    array.length()); i++) {
                JSONObject value = array.optJSONObject(i);
                AppTarget target = AppTarget.fromJson(value);
                if (target != null && (target.isShortcut() || target.componentName() != null)
                        && !result.contains(target)) result.add(target);
            }
        } catch (Exception ignored) { }
        return result;
    }

    public void setHoneycombTargets(List<AppTarget> targets) {
        JSONArray array = new JSONArray();
        for (AppTarget target : targets) {
            if (array.length() >= ConfigContract.MAX_HONEYCOMB_MAX_TARGETS) break;
            if (target != null && (target.isShortcut() || target.componentName() != null)) {
                array.put(target.toJson());
            }
        }
        preferences.edit().putString(ConfigContract.KEY_HONEYCOMB_COMPONENTS,
                array.toString()).apply();
        notifyChanged();
    }

    public List<AppTarget> getSideTargets() {
        ArrayList<AppTarget> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(preferences.getString(
                    ConfigContract.KEY_SIDE_COMPONENTS, "[]"));
            for (int i = 0; i < Math.min(ConfigContract.MAX_SIDE_MAX_TARGETS,
                    array.length()); i++) {
                AppTarget target = AppTarget.fromJson(array.optJSONObject(i));
                if (target != null && (target.isShortcut() || target.componentName() != null)
                        && !result.contains(target)) {
                    result.add(target);
                }
            }
        } catch (Exception ignored) { }
        return result;
    }

    public void setSideTargets(List<AppTarget> targets) {
        JSONArray array = new JSONArray();
        for (AppTarget target : targets) {
            if (array.length() >= ConfigContract.MAX_SIDE_MAX_TARGETS) break;
            if (target != null && (target.isShortcut() || target.componentName() != null)) {
                array.put(target.toJson());
            }
        }
        preferences.edit().putString(ConfigContract.KEY_SIDE_COMPONENTS,
                array.toString()).apply();
        notifyChanged();
    }

    public void copyHoneycombTargetsToSide() {
        setSideTargets(getHoneycombTargets());
    }

    public void resetHoneycombTuning() {
        preferences.edit()
                .putBoolean(ConfigContract.KEY_HONEYCOMB_ENABLED,
                        ConfigContract.DEFAULT_HONEYCOMB_ENABLED)
                .putInt(ConfigContract.KEY_HONEYCOMB_MODE,
                        ConfigContract.DEFAULT_HONEYCOMB_MODE)
                .putInt(ConfigContract.KEY_HONEYCOMB_TRIGGER_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_TRIGGER_DP)
                .putInt(ConfigContract.KEY_HONEYCOMB_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_ICON_SIZE_DP)
                .putInt(ConfigContract.KEY_HONEYCOMB_SPACING_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_SPACING_DP)
                .putInt(ConfigContract.KEY_HONEYCOMB_ANIMATION_SPEED,
                        ConfigContract.DEFAULT_HONEYCOMB_ANIMATION_SPEED)
                .putInt(ConfigContract.KEY_HONEYCOMB_INERTIA,
                        ConfigContract.DEFAULT_HONEYCOMB_INERTIA)
                .putInt(ConfigContract.KEY_HONEYCOMB_CENTER_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_CENTER_SCALE)
                .putInt(ConfigContract.KEY_HONEYCOMB_EDGE_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_EDGE_SCALE)
                .putInt(ConfigContract.KEY_HONEYCOMB_SELECTION_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_SELECTION_SCALE)
                .putBoolean(ConfigContract.KEY_HONEYCOMB_SHOW_SELECTED_NAME,
                        ConfigContract.DEFAULT_HONEYCOMB_SHOW_SELECTED_NAME)
                .putBoolean(ConfigContract.KEY_HONEYCOMB_EMPTY_TAP_CLOSE,
                        ConfigContract.DEFAULT_HONEYCOMB_EMPTY_TAP_CLOSE)
                .putInt(ConfigContract.KEY_HONEYCOMB_MAX_TARGETS,
                        ConfigContract.DEFAULT_HONEYCOMB_MAX_TARGETS)
                .putBoolean(ConfigContract.KEY_HONEYCOMB_FOLLOW_FINGER,
                        ConfigContract.DEFAULT_HONEYCOMB_FOLLOW_FINGER)
                .putBoolean(ConfigContract.KEY_HONEYCOMB_LANDSCAPE_ENABLED,
                        ConfigContract.DEFAULT_HONEYCOMB_LANDSCAPE_ENABLED)
                .putInt(ConfigContract.KEY_HONEYCOMB_FIXED_X_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_FIXED_X_PERCENT)
                .putInt(ConfigContract.KEY_HONEYCOMB_FIXED_Y_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_FIXED_Y_PERCENT)
                .putInt(ConfigContract.KEY_HONEYCOMB_BACKGROUND_STYLE,
                        ConfigContract.DEFAULT_HONEYCOMB_BACKGROUND_STYLE)
                .putInt(ConfigContract.KEY_HONEYCOMB_BLUR_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_BLUR_DP)
                .putInt(ConfigContract.KEY_HONEYCOMB_DIM_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_DIM_PERCENT)
                .putInt(ConfigContract.KEY_HONEYCOMB_RETREAT_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_RETREAT_DP)
                .putInt(ConfigContract.KEY_HONEYCOMB_DISC_SIZE_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_DISC_SIZE_PERCENT)
                .apply();
        notifyChanged();
    }

    public void resetHoneycombLayoutSettings() {
        preferences.edit()
                .putBoolean(ConfigContract.KEY_HONEYCOMB_ENABLED,
                        ConfigContract.DEFAULT_HONEYCOMB_ENABLED)
                .putInt(ConfigContract.KEY_HONEYCOMB_MODE,
                        ConfigContract.DEFAULT_HONEYCOMB_MODE)
                .putBoolean(ConfigContract.KEY_HONEYCOMB_EMPTY_TAP_CLOSE,
                        ConfigContract.DEFAULT_HONEYCOMB_EMPTY_TAP_CLOSE)
                .putInt(ConfigContract.KEY_HONEYCOMB_MAX_TARGETS,
                        ConfigContract.DEFAULT_HONEYCOMB_MAX_TARGETS)
                .putBoolean(ConfigContract.KEY_HONEYCOMB_FOLLOW_FINGER,
                        ConfigContract.DEFAULT_HONEYCOMB_FOLLOW_FINGER)
                .putBoolean(ConfigContract.KEY_HONEYCOMB_LANDSCAPE_ENABLED,
                        ConfigContract.DEFAULT_HONEYCOMB_LANDSCAPE_ENABLED)
                .putInt(ConfigContract.KEY_HONEYCOMB_FIXED_X_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_FIXED_X_PERCENT)
                .putInt(ConfigContract.KEY_HONEYCOMB_FIXED_Y_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_FIXED_Y_PERCENT)
                .putInt(ConfigContract.KEY_HONEYCOMB_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_ICON_SIZE_DP)
                .putInt(ConfigContract.KEY_HONEYCOMB_SPACING_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_SPACING_DP)
                .putInt(ConfigContract.KEY_HONEYCOMB_DISC_SIZE_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_DISC_SIZE_PERCENT)
                .putBoolean(ConfigContract.KEY_HONEYCOMB_SHOW_SELECTED_NAME,
                        ConfigContract.DEFAULT_HONEYCOMB_SHOW_SELECTED_NAME)
                .putInt(ConfigContract.KEY_HONEYCOMB_BACKGROUND_STYLE,
                        ConfigContract.DEFAULT_HONEYCOMB_BACKGROUND_STYLE)
                .putInt(ConfigContract.KEY_HONEYCOMB_BLUR_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_BLUR_DP)
                .putInt(ConfigContract.KEY_HONEYCOMB_DIM_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_DIM_PERCENT)
                .apply();
        notifyChanged();
    }

    public void resetHoneycombAnimationSettings() {
        preferences.edit()
                .putInt(ConfigContract.KEY_HONEYCOMB_ANIMATION_SPEED,
                        ConfigContract.DEFAULT_HONEYCOMB_ANIMATION_SPEED)
                .putInt(ConfigContract.KEY_HONEYCOMB_INERTIA,
                        ConfigContract.DEFAULT_HONEYCOMB_INERTIA)
                .putInt(ConfigContract.KEY_HONEYCOMB_CENTER_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_CENTER_SCALE)
                .putInt(ConfigContract.KEY_HONEYCOMB_EDGE_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_EDGE_SCALE)
                .putInt(ConfigContract.KEY_HONEYCOMB_SELECTION_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_SELECTION_SCALE)
                .apply();
        notifyChanged();
    }

    public void resetGeneralSettings() {
        preferences.edit()
                .putBoolean(ConfigContract.KEY_ENABLED, ConfigContract.DEFAULT_ENABLED)
                .putBoolean(ConfigContract.KEY_HAPTIC, ConfigContract.DEFAULT_HAPTIC)
                .putBoolean(ConfigContract.KEY_SHOW_SELECTED_APP_NAME,
                        ConfigContract.DEFAULT_SHOW_SELECTED_APP_NAME)
                .putBoolean(ConfigContract.KEY_FORCE_CIRCULAR_ICONS,
                        ConfigContract.DEFAULT_FORCE_CIRCULAR_ICONS)
                .apply();
        notifyChanged();
    }

    public void resetAnimationSettings() {
        preferences.edit()
                .putBoolean(ConfigContract.KEY_FAN_SHADOW, ConfigContract.DEFAULT_FAN_SHADOW)
                .putBoolean(ConfigContract.KEY_FAN_ANIMATIONS_ENABLED,
                        ConfigContract.DEFAULT_FAN_ANIMATIONS_ENABLED)
                .putInt(ConfigContract.KEY_FAN_ANIMATION_SPEED,
                        ConfigContract.DEFAULT_FAN_ANIMATION_SPEED)
                .putInt(ConfigContract.KEY_FAN_REVEAL_AMOUNT,
                        ConfigContract.DEFAULT_FAN_REVEAL_AMOUNT)
                .putInt(ConfigContract.KEY_FAN_ROTATION_DEGREES,
                        ConfigContract.DEFAULT_FAN_ROTATION_DEGREES)
                .putInt(ConfigContract.KEY_FAN_SELECTION_SCALE_PERCENT,
                        ConfigContract.DEFAULT_FAN_SELECTION_SCALE_PERCENT)
                .putBoolean(ConfigContract.KEY_FAN_SELECTION_RING,
                        ConfigContract.DEFAULT_FAN_SELECTION_RING)
                .apply();
        notifyChanged();
    }

    public void resetSideAnimationSettings() {
        preferences.edit()
                .putBoolean(ConfigContract.KEY_SIDE_ANIMATIONS_ENABLED,
                        ConfigContract.DEFAULT_SIDE_ANIMATIONS_ENABLED)
                .putInt(ConfigContract.KEY_SIDE_ANIMATION_SPEED,
                        ConfigContract.DEFAULT_SIDE_ANIMATION_SPEED)
                .putInt(ConfigContract.KEY_SIDE_REVEAL_AMOUNT,
                        ConfigContract.DEFAULT_SIDE_REVEAL_AMOUNT)
                .putInt(ConfigContract.KEY_SIDE_ROTATION_DEGREES,
                        ConfigContract.DEFAULT_SIDE_ROTATION_DEGREES)
                .putInt(ConfigContract.KEY_SIDE_SELECTION_SCALE_PERCENT,
                        ConfigContract.DEFAULT_SIDE_SELECTION_SCALE_PERCENT)
                .putBoolean(ConfigContract.KEY_SIDE_SELECTION_RING,
                        ConfigContract.DEFAULT_SIDE_SELECTION_RING)
                .apply();
        notifyChanged();
    }

    public void resetBottomGestureSettings() {
        preferences.edit()
                .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_ENABLED,
                        ConfigContract.DEFAULT_BOTTOM_PORTRAIT_ENABLED)
                .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_ENABLED,
                        ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_ENABLED)
                .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED,
                        ConfigContract.DEFAULT_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED)
                .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED,
                        ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED)
                .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_FULLSCREEN,
                        ConfigContract.DEFAULT_BOTTOM_PORTRAIT_FULLSCREEN)
                .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_FULLSCREEN,
                        ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_FULLSCREEN)
                .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_HONEYCOMB_FREEFORM,
                        ConfigContract.DEFAULT_BOTTOM_PORTRAIT_HONEYCOMB_FREEFORM)
                .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_HONEYCOMB_FREEFORM,
                        ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_HONEYCOMB_FREEFORM)
                .putBoolean(ConfigContract.KEY_BOTTOM_FULLSCREEN,
                        ConfigContract.DEFAULT_BOTTOM_FULLSCREEN)
                .putBoolean(ConfigContract.KEY_BOTTOM_HONEYCOMB_FREEFORM,
                        ConfigContract.DEFAULT_BOTTOM_HONEYCOMB_FREEFORM)
                .putBoolean(ConfigContract.KEY_FAN_FIXED_SEVEN_ROWS,
                        ConfigContract.DEFAULT_FAN_FIXED_SEVEN_ROWS)
                .putInt(ConfigContract.KEY_FAN_LAYOUT_MODE,
                        ConfigContract.DEFAULT_FAN_LAYOUT_MODE)
                .putInt(ConfigContract.KEY_FAN_CUSTOM_OUTER_COUNT,
                        ConfigContract.DEFAULT_FAN_CUSTOM_OUTER_COUNT)
                .putInt(ConfigContract.KEY_FAN_CUSTOM_MIDDLE_COUNT,
                        ConfigContract.DEFAULT_FAN_CUSTOM_MIDDLE_COUNT)
                .putInt(ConfigContract.KEY_FAN_CUSTOM_INNER_COUNT,
                        ConfigContract.DEFAULT_FAN_CUSTOM_INNER_COUNT)
                .putInt(ConfigContract.KEY_HOT_WIDTH_PERCENT,
                        ConfigContract.DEFAULT_HOT_WIDTH_PERCENT)
                .putInt(ConfigContract.KEY_HOT_HEIGHT_PERCENT,
                        ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT)
                .putInt(ConfigContract.KEY_TRIGGER_PERCENT,
                        ConfigContract.DEFAULT_TRIGGER_PERCENT)
                .putInt(ConfigContract.KEY_SELECTION_RADIUS_PERCENT,
                        ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT)
                .putInt(ConfigContract.KEY_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_ICON_SIZE_DP)
                .putInt(ConfigContract.KEY_FAN_MAX_TARGETS,
                        ConfigContract.DEFAULT_FAN_MAX_TARGETS)
                .apply();
        notifyChanged();
    }

    public void resetBottomOrientationSettings(boolean landscape) {
        SharedPreferences.Editor editor = preferences.edit();
        if (landscape) {
            editor.putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_ENABLED,
                            ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_ENABLED)
                    .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED,
                            ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED)
                    .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_FULLSCREEN,
                            ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_FULLSCREEN)
                    .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_HONEYCOMB_FREEFORM,
                            ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_HONEYCOMB_FREEFORM);
        } else {
            editor.putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_ENABLED,
                            ConfigContract.DEFAULT_BOTTOM_PORTRAIT_ENABLED)
                    .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED,
                            ConfigContract.DEFAULT_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED)
                    .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_FULLSCREEN,
                            ConfigContract.DEFAULT_BOTTOM_PORTRAIT_FULLSCREEN)
                    .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_HONEYCOMB_FREEFORM,
                            ConfigContract.DEFAULT_BOTTOM_PORTRAIT_HONEYCOMB_FREEFORM);
        }
        editor.apply();
        notifyChanged();
    }

    public void resetBottomTriggerSettings() {
        preferences.edit()
                .putInt(ConfigContract.KEY_HOT_WIDTH_PERCENT,
                        ConfigContract.DEFAULT_HOT_WIDTH_PERCENT)
                .putInt(ConfigContract.KEY_HOT_HEIGHT_PERCENT,
                        ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT)
                .putInt(ConfigContract.KEY_TRIGGER_PERCENT,
                        ConfigContract.DEFAULT_TRIGGER_PERCENT)
                .putInt(ConfigContract.KEY_SELECTION_RADIUS_PERCENT,
                        ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT)
                .putInt(ConfigContract.KEY_HONEYCOMB_TRIGGER_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_TRIGGER_DP)
                .putInt(ConfigContract.KEY_HONEYCOMB_RETREAT_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_RETREAT_DP)
                .apply();
        notifyChanged();
    }

    public void resetBottomLayoutSettings() {
        resetFanLayoutSettings();
        preferences.edit()
                .putInt(ConfigContract.KEY_ICON_SIZE_DP, ConfigContract.DEFAULT_ICON_SIZE_DP)
                .putInt(ConfigContract.KEY_FAN_MAX_TARGETS,
                        ConfigContract.DEFAULT_FAN_MAX_TARGETS)
                .apply();
        notifyChanged();
    }

    public void resetFanLayoutSettings() {
        preferences.edit()
                .putBoolean(ConfigContract.KEY_FAN_FIXED_SEVEN_ROWS,
                        ConfigContract.DEFAULT_FAN_FIXED_SEVEN_ROWS)
                .putInt(ConfigContract.KEY_FAN_LAYOUT_MODE,
                        ConfigContract.DEFAULT_FAN_LAYOUT_MODE)
                .putInt(ConfigContract.KEY_FAN_CUSTOM_OUTER_COUNT,
                        ConfigContract.DEFAULT_FAN_CUSTOM_OUTER_COUNT)
                .putInt(ConfigContract.KEY_FAN_CUSTOM_MIDDLE_COUNT,
                        ConfigContract.DEFAULT_FAN_CUSTOM_MIDDLE_COUNT)
                .putInt(ConfigContract.KEY_FAN_CUSTOM_INNER_COUNT,
                        ConfigContract.DEFAULT_FAN_CUSTOM_INNER_COUNT)
                .apply();
        notifyChanged();
    }

    public void resetSideGestureSettings() {
        preferences.edit()
                .putBoolean(ConfigContract.KEY_SIDE_GESTURE_ENABLED,
                        ConfigContract.DEFAULT_SIDE_GESTURE_ENABLED)
                .putBoolean(ConfigContract.KEY_SIDE_PORTRAIT_ENABLED,
                        ConfigContract.DEFAULT_SIDE_PORTRAIT_ENABLED)
                .putBoolean(ConfigContract.KEY_SIDE_LANDSCAPE_ENABLED,
                        ConfigContract.DEFAULT_SIDE_LANDSCAPE_ENABLED)
                .putInt(ConfigContract.KEY_SIDE_PORTRAIT_LAYOUT_MODE,
                        ConfigContract.DEFAULT_SIDE_LAYOUT_MODE)
                .putInt(ConfigContract.KEY_SIDE_LANDSCAPE_LAYOUT_MODE,
                        ConfigContract.DEFAULT_SIDE_LAYOUT_MODE)
                .putBoolean(ConfigContract.KEY_SIDE_PORTRAIT_FULLSCREEN,
                        ConfigContract.DEFAULT_SIDE_PORTRAIT_FULLSCREEN)
                .putBoolean(ConfigContract.KEY_SIDE_LANDSCAPE_FULLSCREEN,
                        ConfigContract.DEFAULT_SIDE_LANDSCAPE_FULLSCREEN)
                .putBoolean(ConfigContract.KEY_SIDE_DIRECTION_HORIZONTAL,
                        ConfigContract.DEFAULT_SIDE_DIRECTION_HORIZONTAL)
                .putBoolean(ConfigContract.KEY_SIDE_DIRECTION_UP,
                        ConfigContract.DEFAULT_SIDE_DIRECTION_UP)
                .putBoolean(ConfigContract.KEY_SIDE_DIRECTION_DOWN,
                        ConfigContract.DEFAULT_SIDE_DIRECTION_DOWN)
                .putInt(ConfigContract.KEY_SIDE_MAX_TARGETS,
                        ConfigContract.DEFAULT_SIDE_MAX_TARGETS)
                .putInt(ConfigContract.KEY_SIDE_LAYOUT_MODE,
                        ConfigContract.DEFAULT_SIDE_LAYOUT_MODE)
                .putInt(ConfigContract.KEY_SIDE_TRIGGER_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TRIGGER_PERCENT)
                .putInt(ConfigContract.KEY_SIDE_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_SIDE_ICON_SIZE_DP)
                .putInt(ConfigContract.KEY_SIDE_RING_SIZE_PERCENT,
                        ConfigContract.DEFAULT_SIDE_RING_SIZE_PERCENT)
                .putInt(ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT)
                .putInt(ConfigContract.KEY_SIDE_REVERSE_CANCEL_PERCENT,
                        ConfigContract.DEFAULT_SIDE_REVERSE_CANCEL_PERCENT)
                .putBoolean(ConfigContract.KEY_SIDE_HOLD_ENABLED,
                        ConfigContract.DEFAULT_SIDE_HOLD_ENABLED)
                .putInt(ConfigContract.KEY_SIDE_HOLD_DELAY_MS,
                        ConfigContract.DEFAULT_SIDE_HOLD_DELAY_MS)
                .putBoolean(ConfigContract.KEY_SIDE_FOLLOW_FINGER,
                        ConfigContract.DEFAULT_SIDE_FOLLOW_FINGER)
                .putBoolean(ConfigContract.KEY_SIDE_FULLSCREEN,
                        ConfigContract.DEFAULT_SIDE_FULLSCREEN)
                .apply();
        notifyChanged();
    }

    public void resetSideOrientationSettings(boolean landscape) {
        SharedPreferences.Editor editor = preferences.edit();
        if (landscape) {
            editor.putBoolean(ConfigContract.KEY_SIDE_LANDSCAPE_ENABLED,
                            ConfigContract.DEFAULT_SIDE_LANDSCAPE_ENABLED)
                    .putBoolean(ConfigContract.KEY_SIDE_LANDSCAPE_FULLSCREEN,
                            ConfigContract.DEFAULT_SIDE_LANDSCAPE_FULLSCREEN)
                    .putInt(ConfigContract.KEY_SIDE_LANDSCAPE_LAYOUT_MODE,
                            ConfigContract.DEFAULT_SIDE_LAYOUT_MODE);
        } else {
            editor.putBoolean(ConfigContract.KEY_SIDE_PORTRAIT_ENABLED,
                            ConfigContract.DEFAULT_SIDE_PORTRAIT_ENABLED)
                    .putBoolean(ConfigContract.KEY_SIDE_PORTRAIT_FULLSCREEN,
                            ConfigContract.DEFAULT_SIDE_PORTRAIT_FULLSCREEN)
                    .putInt(ConfigContract.KEY_SIDE_PORTRAIT_LAYOUT_MODE,
                            ConfigContract.DEFAULT_SIDE_LAYOUT_MODE);
        }
        editor.apply();
        notifyChanged();
    }

    public void resetSideTriggerSettings() {
        preferences.edit()
                .putBoolean(ConfigContract.KEY_SIDE_DIRECTION_HORIZONTAL,
                        ConfigContract.DEFAULT_SIDE_DIRECTION_HORIZONTAL)
                .putBoolean(ConfigContract.KEY_SIDE_DIRECTION_UP,
                        ConfigContract.DEFAULT_SIDE_DIRECTION_UP)
                .putBoolean(ConfigContract.KEY_SIDE_DIRECTION_DOWN,
                        ConfigContract.DEFAULT_SIDE_DIRECTION_DOWN)
                .putInt(ConfigContract.KEY_SIDE_TRIGGER_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TRIGGER_PERCENT)
                .putInt(ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT)
                .putInt(ConfigContract.KEY_SIDE_REVERSE_CANCEL_PERCENT,
                        ConfigContract.DEFAULT_SIDE_REVERSE_CANCEL_PERCENT)
                .putBoolean(ConfigContract.KEY_SIDE_HOLD_ENABLED,
                        ConfigContract.DEFAULT_SIDE_HOLD_ENABLED)
                .putInt(ConfigContract.KEY_SIDE_HOLD_DELAY_MS,
                        ConfigContract.DEFAULT_SIDE_HOLD_DELAY_MS)
                .apply();
        notifyChanged();
    }

    public void resetSideLayoutSettings() {
        preferences.edit()
                .putInt(ConfigContract.KEY_SIDE_MAX_TARGETS,
                        ConfigContract.DEFAULT_SIDE_MAX_TARGETS)
                .putInt(ConfigContract.KEY_SIDE_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_SIDE_ICON_SIZE_DP)
                .putInt(ConfigContract.KEY_SIDE_RING_SIZE_PERCENT,
                        ConfigContract.DEFAULT_SIDE_RING_SIZE_PERCENT)
                .putBoolean(ConfigContract.KEY_SIDE_FOLLOW_FINGER,
                        ConfigContract.DEFAULT_SIDE_FOLLOW_FINGER)
                .apply();
        notifyChanged();
    }

    public void resetTaskCenterSettings() {
        preferences.edit()
                .putInt(ConfigContract.KEY_SIDE_TASK_MAX_COUNT,
                        ConfigContract.DEFAULT_SIDE_TASK_MAX_COUNT)
                .putInt(ConfigContract.KEY_SIDE_TASK_CARD_SIZE_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_SIZE_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_CARD_WIDTH_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_WIDTH_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_CARD_HEIGHT_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_HEIGHT_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_CARD_CORNER_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_CORNER_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_ICON_SIZE_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_ICON_GAP_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_ICON_GAP_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_FINGER_OFFSET_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_FINGER_OFFSET_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_LAYOUT_MODE,
                        ConfigContract.DEFAULT_SIDE_TASK_LAYOUT_MODE)
                .putBoolean(ConfigContract.KEY_SIDE_TASK_SHOW_NAME,
                        ConfigContract.DEFAULT_SIDE_TASK_SHOW_NAME)
                .putInt(ConfigContract.KEY_SIDE_TASK_MOTION_MODE,
                        ConfigContract.DEFAULT_SIDE_TASK_MOTION_MODE)
                .putInt(ConfigContract.KEY_SIDE_TASK_SWIPE_SPEED_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TASK_SWIPE_SPEED_PERCENT)
                .putBoolean(ConfigContract.KEY_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE,
                        ConfigContract.DEFAULT_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE)
                .apply();
        notifyChanged();
    }

    public void resetTaskCenterLayoutSettings() {
        preferences.edit()
                .putInt(ConfigContract.KEY_SIDE_TASK_MAX_COUNT,
                        ConfigContract.DEFAULT_SIDE_TASK_MAX_COUNT)
                .putInt(ConfigContract.KEY_SIDE_TASK_CARD_SIZE_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_SIZE_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_CARD_WIDTH_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_WIDTH_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_CARD_HEIGHT_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_HEIGHT_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_CARD_CORNER_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_CORNER_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_ICON_SIZE_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_ICON_GAP_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_ICON_GAP_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_FINGER_OFFSET_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_FINGER_OFFSET_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_LAYOUT_MODE,
                        ConfigContract.DEFAULT_SIDE_TASK_LAYOUT_MODE)
                .putBoolean(ConfigContract.KEY_SIDE_TASK_SHOW_NAME,
                        ConfigContract.DEFAULT_SIDE_TASK_SHOW_NAME)
                .putBoolean(ConfigContract.KEY_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE,
                        ConfigContract.DEFAULT_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE)
                .apply();
        notifyChanged();
    }

    public void resetTaskCenterAnimationSettings() {
        preferences.edit()
                .putInt(ConfigContract.KEY_SIDE_TASK_MOTION_MODE,
                        ConfigContract.DEFAULT_SIDE_TASK_MOTION_MODE)
                .putInt(ConfigContract.KEY_SIDE_TASK_SWIPE_SPEED_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TASK_SWIPE_SPEED_PERCENT)
                .apply();
        notifyChanged();
    }

    public void resetWindowSettings() {
        preferences.edit()
                .putBoolean(ConfigContract.KEY_CUSTOM_WINDOW_BOUNDS_ENABLED,
                        ConfigContract.DEFAULT_CUSTOM_WINDOW_BOUNDS_ENABLED)
                .putBoolean(ConfigContract.KEY_CUSTOM_WINDOW_PORTRAIT_ENABLED,
                        ConfigContract.DEFAULT_CUSTOM_WINDOW_PORTRAIT_ENABLED)
                .putBoolean(ConfigContract.KEY_CUSTOM_WINDOW_LANDSCAPE_ENABLED,
                        ConfigContract.DEFAULT_CUSTOM_WINDOW_LANDSCAPE_ENABLED)
                .putBoolean(ConfigContract.KEY_PORTRAIT_PROPORTIONAL_SIZE_ENABLED,
                        ConfigContract.DEFAULT_PORTRAIT_PROPORTIONAL_SIZE_ENABLED)
                .putBoolean(ConfigContract.KEY_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED,
                        ConfigContract.DEFAULT_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED)
                .putInt(ConfigContract.KEY_PORTRAIT_NATIVE_SCALE_PERCENT,
                        ConfigContract.DEFAULT_NATIVE_WINDOW_SCALE_PERCENT)
                .putInt(ConfigContract.KEY_LANDSCAPE_NATIVE_SCALE_PERCENT,
                        ConfigContract.DEFAULT_NATIVE_WINDOW_SCALE_PERCENT)
                .putInt(ConfigContract.KEY_WIDTH_PERCENT,
                        ConfigContract.DEFAULT_WIDTH_PERCENT)
                .putInt(ConfigContract.KEY_HEIGHT_PERCENT,
                        ConfigContract.DEFAULT_HEIGHT_PERCENT)
                .putInt(ConfigContract.KEY_POSITION_X, ConfigContract.DEFAULT_POSITION_X)
                .putInt(ConfigContract.KEY_POSITION_Y, ConfigContract.DEFAULT_POSITION_Y)
                .putInt(ConfigContract.KEY_LANDSCAPE_WIDTH_PERCENT,
                        ConfigContract.DEFAULT_LANDSCAPE_WIDTH_PERCENT)
                .putInt(ConfigContract.KEY_LANDSCAPE_HEIGHT_PERCENT,
                        ConfigContract.DEFAULT_LANDSCAPE_HEIGHT_PERCENT)
                .putInt(ConfigContract.KEY_LANDSCAPE_POSITION_X,
                        ConfigContract.DEFAULT_LANDSCAPE_POSITION_X)
                .putInt(ConfigContract.KEY_LANDSCAPE_POSITION_Y,
                        ConfigContract.DEFAULT_LANDSCAPE_POSITION_Y)
                .apply();
        notifyChanged();
    }

    public void resetOutsideSettings() {
        preferences.edit()
                .putBoolean(ConfigContract.KEY_OUTSIDE_ENABLED,
                        ConfigContract.DEFAULT_OUTSIDE_ENABLED)
                .putBoolean(ConfigContract.KEY_OUTSIDE_PORTRAIT_ENABLED,
                        ConfigContract.DEFAULT_OUTSIDE_PORTRAIT_ENABLED)
                .putBoolean(ConfigContract.KEY_OUTSIDE_LANDSCAPE_ENABLED,
                        ConfigContract.DEFAULT_OUTSIDE_LANDSCAPE_ENABLED)
                .putInt(ConfigContract.KEY_OUTSIDE_SINGLE_ACTION,
                        ConfigContract.DEFAULT_OUTSIDE_SINGLE_ACTION)
                .putInt(ConfigContract.KEY_OUTSIDE_DOUBLE_ACTION,
                        ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION)
                .putInt(ConfigContract.KEY_OUTSIDE_TAP_WINDOW_MS,
                        ConfigContract.DEFAULT_OUTSIDE_TAP_WINDOW_MS)
                .apply();
        notifyChanged();
    }

    public void resetBottomTargets() {
        resetTargets(ConfigContract.KEY_COMPONENTS, DEFAULT_BOTTOM_PACKAGES, true);
    }

    public void resetSideTargets() {
        resetTargets(ConfigContract.KEY_SIDE_COMPONENTS, DEFAULT_SIDE_PACKAGES, false);
    }

    public void resetHoneycombTargets() {
        resetTargets(ConfigContract.KEY_HONEYCOMB_COMPONENTS,
                DEFAULT_HONEYCOMB_PACKAGES, false);
    }

    private void resetTargets(String key, String[] packages, boolean ensureMinimum) {
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent, 0);
        Map<String, ComponentName> byPackage = new HashMap<>();
        for (ResolveInfo item : activities) {
            if (item.activityInfo != null) {
                byPackage.putIfAbsent(item.activityInfo.packageName,
                        new ComponentName(item.activityInfo.packageName, item.activityInfo.name));
            }
        }
        JSONArray defaults = installedTargets(byPackage, packages);
        if (ensureMinimum && defaults.length() < 3) {
            for (ComponentName component : byPackage.values()) {
                if (context.getPackageName().equals(component.getPackageName())) continue;
                defaults.put(new AppTarget(component.flattenToString()).toJson());
                if (defaults.length() >= 6) break;
            }
        }
        preferences.edit().putString(key, defaults.toString()).apply();
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

    public void putFanRowAllocation(int inner, int middle, int outer) {
        preferences.edit()
                .putInt(ConfigContract.KEY_FAN_CUSTOM_INNER_COUNT, Math.max(0, inner))
                .putInt(ConfigContract.KEY_FAN_CUSTOM_MIDDLE_COUNT, Math.max(0, middle))
                .putInt(ConfigContract.KEY_FAN_CUSTOM_OUTER_COUNT, Math.max(0, outer))
                .apply();
        notifyChanged();
    }

    public List<ShortcutCatalogEntry> getShortcutCatalog() {
        ArrayList<ShortcutCatalogEntry> result = new ArrayList<>();
        try {
            JSONArray items = new JSONArray(preferences.getString(
                    ConfigContract.KEY_SHORTCUT_CATALOG, "[]"));
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                String packageName = item.optString("package");
                String shortcutId = item.optString("id");
                String label = item.optString("label");
                String intentUri = item.optString("intent");
                int userId = item.optInt("userId", 0);
                boolean launcherShortcut = "launcher".equals(item.optString("kind"))
                        && !intentUri.isEmpty();
                if (!packageName.isEmpty() && !shortcutId.isEmpty() && !label.isEmpty()) {
                    result.add(new ShortcutCatalogEntry(packageName, shortcutId, label,
                            intentUri, userId, launcherShortcut));
                }
            }
        } catch (Exception ignored) { }
        return result;
    }

    public void requestShortcutCatalog() {
        preferences.edit().putLong(ConfigContract.KEY_SHORTCUT_CATALOG_REQUEST,
                System.currentTimeMillis()).apply();
        notifyChanged();
    }

    public List<ActivityCatalogEntry> getActivityCatalog() {
        ArrayList<ActivityCatalogEntry> result = new ArrayList<>();
        try {
            JSONArray items = new JSONArray(preferences.getString(
                    ConfigContract.KEY_ACTIVITY_CATALOG, "[]"));
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                String component = item.optString("component");
                String label = item.optString("label");
                int userId = item.optInt("userId", 0);
                if (ComponentName.unflattenFromString(component) != null
                        && !label.isEmpty() && userId != 0) {
                    result.add(new ActivityCatalogEntry(component, label, userId));
                }
            }
        } catch (Exception ignored) { }
        return result;
    }

    public void requestActivityCatalog() {
        preferences.edit().putLong(ConfigContract.KEY_ACTIVITY_CATALOG_REQUEST,
                System.currentTimeMillis()).apply();
        notifyChanged();
    }

    public static final class ShortcutCatalogEntry {
        public final String packageName;
        public final String shortcutId;
        public final String label;
        public final String intentUri;
        public final int userId;
        public final boolean launcherShortcut;

        ShortcutCatalogEntry(String packageName, String shortcutId, String label,
                             String intentUri, int userId, boolean launcherShortcut) {
            this.packageName = packageName;
            this.shortcutId = shortcutId;
            this.label = label;
            this.intentUri = intentUri;
            this.userId = Math.max(0, userId);
            this.launcherShortcut = launcherShortcut;
        }
    }

    public static final class ActivityCatalogEntry {
        public final String component;
        public final String label;
        public final int userId;

        ActivityCatalogEntry(String component, String label, int userId) {
            this.component = component;
            this.label = label;
            this.userId = userId;
        }
    }

    public void resetTuning() {
        preferences.edit()
                .putBoolean(ConfigContract.KEY_ENABLED, ConfigContract.DEFAULT_ENABLED)
                .putBoolean(ConfigContract.KEY_HAPTIC, ConfigContract.DEFAULT_HAPTIC)
                .putBoolean(ConfigContract.KEY_FAN_SHADOW, ConfigContract.DEFAULT_FAN_SHADOW)
                .putBoolean(ConfigContract.KEY_FAN_ANIMATIONS_ENABLED,
                        ConfigContract.DEFAULT_FAN_ANIMATIONS_ENABLED)
                .putInt(ConfigContract.KEY_FAN_ANIMATION_SPEED,
                        ConfigContract.DEFAULT_FAN_ANIMATION_SPEED)
                .putInt(ConfigContract.KEY_FAN_REVEAL_AMOUNT,
                        ConfigContract.DEFAULT_FAN_REVEAL_AMOUNT)
                .putInt(ConfigContract.KEY_FAN_ROTATION_DEGREES,
                        ConfigContract.DEFAULT_FAN_ROTATION_DEGREES)
                .putInt(ConfigContract.KEY_FAN_SELECTION_SCALE_PERCENT,
                        ConfigContract.DEFAULT_FAN_SELECTION_SCALE_PERCENT)
                .putBoolean(ConfigContract.KEY_FAN_SELECTION_RING,
                        ConfigContract.DEFAULT_FAN_SELECTION_RING)
                .putBoolean(ConfigContract.KEY_FORCE_CIRCULAR_ICONS,
                        ConfigContract.DEFAULT_FORCE_CIRCULAR_ICONS)
                .putBoolean(ConfigContract.KEY_FAN_FIXED_SEVEN_ROWS,
                        ConfigContract.DEFAULT_FAN_FIXED_SEVEN_ROWS)
                .putInt(ConfigContract.KEY_FAN_LAYOUT_MODE,
                        ConfigContract.DEFAULT_FAN_LAYOUT_MODE)
                .putInt(ConfigContract.KEY_FAN_CUSTOM_OUTER_COUNT,
                        ConfigContract.DEFAULT_FAN_CUSTOM_OUTER_COUNT)
                .putInt(ConfigContract.KEY_FAN_CUSTOM_MIDDLE_COUNT,
                        ConfigContract.DEFAULT_FAN_CUSTOM_MIDDLE_COUNT)
                .putInt(ConfigContract.KEY_FAN_CUSTOM_INNER_COUNT,
                        ConfigContract.DEFAULT_FAN_CUSTOM_INNER_COUNT)
                .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_ENABLED,
                        ConfigContract.DEFAULT_BOTTOM_PORTRAIT_ENABLED)
                .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_ENABLED,
                        ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_ENABLED)
                .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED,
                        ConfigContract.DEFAULT_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED)
                .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED,
                        ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED)
                .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_FULLSCREEN,
                        ConfigContract.DEFAULT_BOTTOM_PORTRAIT_FULLSCREEN)
                .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_FULLSCREEN,
                        ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_FULLSCREEN)
                .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_HONEYCOMB_FREEFORM,
                        ConfigContract.DEFAULT_BOTTOM_PORTRAIT_HONEYCOMB_FREEFORM)
                .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_HONEYCOMB_FREEFORM,
                        ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_HONEYCOMB_FREEFORM)
                .putBoolean(ConfigContract.KEY_SIDE_GESTURE_ENABLED, ConfigContract.DEFAULT_SIDE_GESTURE_ENABLED)
                .putBoolean(ConfigContract.KEY_SIDE_PORTRAIT_ENABLED,
                        ConfigContract.DEFAULT_SIDE_PORTRAIT_ENABLED)
                .putBoolean(ConfigContract.KEY_SIDE_LANDSCAPE_ENABLED,
                        ConfigContract.DEFAULT_SIDE_LANDSCAPE_ENABLED)
                .putInt(ConfigContract.KEY_SIDE_PORTRAIT_LAYOUT_MODE,
                        ConfigContract.DEFAULT_SIDE_LAYOUT_MODE)
                .putInt(ConfigContract.KEY_SIDE_LANDSCAPE_LAYOUT_MODE,
                        ConfigContract.DEFAULT_SIDE_LAYOUT_MODE)
                .putBoolean(ConfigContract.KEY_SIDE_PORTRAIT_FULLSCREEN,
                        ConfigContract.DEFAULT_SIDE_PORTRAIT_FULLSCREEN)
                .putBoolean(ConfigContract.KEY_SIDE_LANDSCAPE_FULLSCREEN,
                        ConfigContract.DEFAULT_SIDE_LANDSCAPE_FULLSCREEN)
                .putBoolean(ConfigContract.KEY_SIDE_ANIMATIONS_ENABLED,
                        ConfigContract.DEFAULT_SIDE_ANIMATIONS_ENABLED)
                .putInt(ConfigContract.KEY_SIDE_ANIMATION_SPEED,
                        ConfigContract.DEFAULT_SIDE_ANIMATION_SPEED)
                .putInt(ConfigContract.KEY_SIDE_REVEAL_AMOUNT,
                        ConfigContract.DEFAULT_SIDE_REVEAL_AMOUNT)
                .putInt(ConfigContract.KEY_SIDE_ROTATION_DEGREES,
                        ConfigContract.DEFAULT_SIDE_ROTATION_DEGREES)
                .putInt(ConfigContract.KEY_SIDE_SELECTION_SCALE_PERCENT,
                        ConfigContract.DEFAULT_SIDE_SELECTION_SCALE_PERCENT)
                .putBoolean(ConfigContract.KEY_SIDE_SELECTION_RING,
                        ConfigContract.DEFAULT_SIDE_SELECTION_RING)
                .putInt(ConfigContract.KEY_SIDE_TRIGGER_PERCENT, ConfigContract.DEFAULT_SIDE_TRIGGER_PERCENT)
                .putInt(ConfigContract.KEY_SIDE_ICON_SIZE_DP, ConfigContract.DEFAULT_SIDE_ICON_SIZE_DP)
                .putInt(ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT)
                .putBoolean(ConfigContract.KEY_SHOW_SELECTED_APP_NAME,
                        ConfigContract.DEFAULT_SHOW_SELECTED_APP_NAME)
                .putBoolean(ConfigContract.KEY_SIDE_FOLLOW_FINGER,
                        ConfigContract.DEFAULT_SIDE_FOLLOW_FINGER)
                .putBoolean(ConfigContract.KEY_SIDE_FAN_LIST,
                        ConfigContract.DEFAULT_SIDE_FAN_LIST)
                .putInt(ConfigContract.KEY_SIDE_LAYOUT_MODE,
                        ConfigContract.DEFAULT_SIDE_LAYOUT_MODE)
                .putInt(ConfigContract.KEY_SIDE_RING_SIZE_PERCENT,
                        ConfigContract.DEFAULT_SIDE_RING_SIZE_PERCENT)
                .putBoolean(ConfigContract.KEY_SIDE_WHEEL_MODE,
                        ConfigContract.DEFAULT_SIDE_WHEEL_MODE)
                .putInt(ConfigContract.KEY_SIDE_REVERSE_CANCEL_PERCENT,
                        ConfigContract.DEFAULT_SIDE_REVERSE_CANCEL_PERCENT)
                .putBoolean(ConfigContract.KEY_SIDE_HOLD_ENABLED,
                        ConfigContract.DEFAULT_SIDE_HOLD_ENABLED)
                .putInt(ConfigContract.KEY_SIDE_HOLD_DELAY_MS,
                        ConfigContract.DEFAULT_SIDE_HOLD_DELAY_MS)
                .putInt(ConfigContract.KEY_SIDE_TASK_MAX_COUNT,
                        ConfigContract.DEFAULT_SIDE_TASK_MAX_COUNT)
                .putInt(ConfigContract.KEY_SIDE_TASK_CARD_SIZE_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_SIZE_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_CARD_WIDTH_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_WIDTH_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_CARD_HEIGHT_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_HEIGHT_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_CARD_CORNER_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_CARD_CORNER_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_ICON_SIZE_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_ICON_GAP_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_ICON_GAP_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_FINGER_OFFSET_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_FINGER_OFFSET_DP)
                .putInt(ConfigContract.KEY_SIDE_TASK_LAYOUT_MODE,
                        ConfigContract.DEFAULT_SIDE_TASK_LAYOUT_MODE)
                .putBoolean(ConfigContract.KEY_SIDE_TASK_SHOW_NAME,
                        ConfigContract.DEFAULT_SIDE_TASK_SHOW_NAME)
                .putInt(ConfigContract.KEY_SIDE_TASK_MOTION_MODE,
                        ConfigContract.DEFAULT_SIDE_TASK_MOTION_MODE)
                .putInt(ConfigContract.KEY_SIDE_TASK_SWIPE_SPEED_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TASK_SWIPE_SPEED_PERCENT)
                .putBoolean(ConfigContract.KEY_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE,
                        ConfigContract.DEFAULT_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE)
                .putBoolean(ConfigContract.KEY_HIDE_SYSTEM_RECENTS_CLEAR,
                        ConfigContract.DEFAULT_HIDE_SYSTEM_RECENTS_CLEAR)
                .putInt(ConfigContract.KEY_TRIGGER_PERCENT, ConfigContract.DEFAULT_TRIGGER_PERCENT)
                .putInt(ConfigContract.KEY_SELECTION_RADIUS_PERCENT, ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT)
                .putInt(ConfigContract.KEY_HOT_WIDTH_PERCENT, ConfigContract.DEFAULT_HOT_WIDTH_PERCENT)
                .putInt(ConfigContract.KEY_HOT_HEIGHT_PERCENT, ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT)
                .putInt(ConfigContract.KEY_ICON_SIZE_DP, ConfigContract.DEFAULT_ICON_SIZE_DP)
                .putBoolean(ConfigContract.KEY_CUSTOM_WINDOW_BOUNDS_ENABLED,
                        ConfigContract.DEFAULT_CUSTOM_WINDOW_BOUNDS_ENABLED)
                .putBoolean(ConfigContract.KEY_CUSTOM_WINDOW_PORTRAIT_ENABLED,
                        ConfigContract.DEFAULT_CUSTOM_WINDOW_PORTRAIT_ENABLED)
                .putBoolean(ConfigContract.KEY_CUSTOM_WINDOW_LANDSCAPE_ENABLED,
                        ConfigContract.DEFAULT_CUSTOM_WINDOW_LANDSCAPE_ENABLED)
                .putBoolean(ConfigContract.KEY_PORTRAIT_PROPORTIONAL_SIZE_ENABLED,
                        ConfigContract.DEFAULT_PORTRAIT_PROPORTIONAL_SIZE_ENABLED)
                .putBoolean(ConfigContract.KEY_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED,
                        ConfigContract.DEFAULT_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED)
                .putInt(ConfigContract.KEY_PORTRAIT_NATIVE_SCALE_PERCENT,
                        ConfigContract.DEFAULT_NATIVE_WINDOW_SCALE_PERCENT)
                .putInt(ConfigContract.KEY_LANDSCAPE_NATIVE_SCALE_PERCENT,
                        ConfigContract.DEFAULT_NATIVE_WINDOW_SCALE_PERCENT)
                .putInt(ConfigContract.KEY_WIDTH_PERCENT, ConfigContract.DEFAULT_WIDTH_PERCENT)
                .putInt(ConfigContract.KEY_HEIGHT_PERCENT, ConfigContract.DEFAULT_HEIGHT_PERCENT)
                .putInt(ConfigContract.KEY_POSITION_X, ConfigContract.DEFAULT_POSITION_X)
                .putInt(ConfigContract.KEY_POSITION_Y, ConfigContract.DEFAULT_POSITION_Y)
                .putInt(ConfigContract.KEY_LANDSCAPE_WIDTH_PERCENT,
                        ConfigContract.DEFAULT_LANDSCAPE_WIDTH_PERCENT)
                .putInt(ConfigContract.KEY_LANDSCAPE_HEIGHT_PERCENT,
                        ConfigContract.DEFAULT_LANDSCAPE_HEIGHT_PERCENT)
                .putInt(ConfigContract.KEY_LANDSCAPE_POSITION_X,
                        ConfigContract.DEFAULT_LANDSCAPE_POSITION_X)
                .putInt(ConfigContract.KEY_LANDSCAPE_POSITION_Y,
                        ConfigContract.DEFAULT_LANDSCAPE_POSITION_Y)
                .putInt(ConfigContract.KEY_OUTSIDE_SINGLE_ACTION, ConfigContract.DEFAULT_OUTSIDE_SINGLE_ACTION)
                .putInt(ConfigContract.KEY_OUTSIDE_DOUBLE_ACTION, ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION)
                .putInt(ConfigContract.KEY_OUTSIDE_TAP_WINDOW_MS,
                        ConfigContract.DEFAULT_OUTSIDE_TAP_WINDOW_MS)
                .putBoolean(ConfigContract.KEY_OUTSIDE_ENABLED,
                        ConfigContract.DEFAULT_OUTSIDE_ENABLED)
                .putBoolean(ConfigContract.KEY_OUTSIDE_PORTRAIT_ENABLED,
                        ConfigContract.DEFAULT_OUTSIDE_PORTRAIT_ENABLED)
                .putBoolean(ConfigContract.KEY_OUTSIDE_LANDSCAPE_ENABLED,
                        ConfigContract.DEFAULT_OUTSIDE_LANDSCAPE_ENABLED)
                .putBoolean(ConfigContract.KEY_HONEYCOMB_ENABLED,
                        ConfigContract.DEFAULT_HONEYCOMB_ENABLED)
                .putInt(ConfigContract.KEY_HONEYCOMB_MODE,
                        ConfigContract.DEFAULT_HONEYCOMB_MODE)
                .putInt(ConfigContract.KEY_HONEYCOMB_TRIGGER_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_TRIGGER_DP)
                .putInt(ConfigContract.KEY_HONEYCOMB_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_ICON_SIZE_DP)
                .putInt(ConfigContract.KEY_HONEYCOMB_SPACING_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_SPACING_DP)
                .putInt(ConfigContract.KEY_HONEYCOMB_ANIMATION_SPEED,
                        ConfigContract.DEFAULT_HONEYCOMB_ANIMATION_SPEED)
                .putInt(ConfigContract.KEY_HONEYCOMB_INERTIA,
                        ConfigContract.DEFAULT_HONEYCOMB_INERTIA)
                .putInt(ConfigContract.KEY_HONEYCOMB_CENTER_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_CENTER_SCALE)
                .putInt(ConfigContract.KEY_HONEYCOMB_EDGE_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_EDGE_SCALE)
                .putInt(ConfigContract.KEY_HONEYCOMB_SELECTION_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_SELECTION_SCALE)
                .putBoolean(ConfigContract.KEY_HONEYCOMB_SHOW_SELECTED_NAME,
                        ConfigContract.DEFAULT_HONEYCOMB_SHOW_SELECTED_NAME)
                .putInt(ConfigContract.KEY_HONEYCOMB_DISC_SIZE_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_DISC_SIZE_PERCENT)
                .putBoolean(ConfigContract.KEY_HONEYCOMB_EMPTY_TAP_CLOSE,
                        ConfigContract.DEFAULT_HONEYCOMB_EMPTY_TAP_CLOSE)
                .putInt(ConfigContract.KEY_FAN_MAX_TARGETS,
                        ConfigContract.DEFAULT_FAN_MAX_TARGETS)
                .putInt(ConfigContract.KEY_HONEYCOMB_MAX_TARGETS,
                        ConfigContract.DEFAULT_HONEYCOMB_MAX_TARGETS)
                .putBoolean(ConfigContract.KEY_SIDE_FOLLOW_HONEYCOMB,
                        ConfigContract.DEFAULT_SIDE_FOLLOW_HONEYCOMB)
                .putInt(ConfigContract.KEY_SIDE_MAX_TARGETS,
                        ConfigContract.DEFAULT_SIDE_MAX_TARGETS)
                .putBoolean(ConfigContract.KEY_SIDE_DIRECTION_HORIZONTAL,
                        ConfigContract.DEFAULT_SIDE_DIRECTION_HORIZONTAL)
                .putBoolean(ConfigContract.KEY_SIDE_DIRECTION_UP,
                        ConfigContract.DEFAULT_SIDE_DIRECTION_UP)
                .putBoolean(ConfigContract.KEY_SIDE_DIRECTION_DOWN,
                        ConfigContract.DEFAULT_SIDE_DIRECTION_DOWN)
                .putBoolean(ConfigContract.KEY_SIDE_FULLSCREEN,
                        ConfigContract.DEFAULT_SIDE_FULLSCREEN)
                .putBoolean(ConfigContract.KEY_BOTTOM_FULLSCREEN,
                        ConfigContract.DEFAULT_BOTTOM_FULLSCREEN)
                .putBoolean(ConfigContract.KEY_BOTTOM_HONEYCOMB_FREEFORM,
                        ConfigContract.DEFAULT_BOTTOM_HONEYCOMB_FREEFORM)
                .putBoolean(ConfigContract.KEY_HONEYCOMB_FOLLOW_FINGER,
                        ConfigContract.DEFAULT_HONEYCOMB_FOLLOW_FINGER)
                .putBoolean(ConfigContract.KEY_HONEYCOMB_LANDSCAPE_ENABLED,
                        ConfigContract.DEFAULT_HONEYCOMB_LANDSCAPE_ENABLED)
                .putInt(ConfigContract.KEY_HONEYCOMB_FIXED_X_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_FIXED_X_PERCENT)
                .putInt(ConfigContract.KEY_HONEYCOMB_FIXED_Y_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_FIXED_Y_PERCENT)
                .putInt(ConfigContract.KEY_HONEYCOMB_BACKGROUND_STYLE,
                        ConfigContract.DEFAULT_HONEYCOMB_BACKGROUND_STYLE)
                .putInt(ConfigContract.KEY_HONEYCOMB_BLUR_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_BLUR_DP)
                .putInt(ConfigContract.KEY_HONEYCOMB_DIM_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_DIM_PERCENT)
                .putInt(ConfigContract.KEY_HONEYCOMB_RETREAT_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_RETREAT_DP)
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
        JSONArray bottom = installedTargets(byPackage, DEFAULT_BOTTOM_PACKAGES);
        if (bottom.length() < 3) {
            for (ComponentName component : byPackage.values()) {
                if (context.getPackageName().equals(component.getPackageName())) continue;
                bottom.put(new AppTarget(component.flattenToString()).toJson());
                if (bottom.length() >= 6) break;
            }
        }
        preferences.edit()
                .putString(ConfigContract.KEY_COMPONENTS, bottom.toString())
                .putString(ConfigContract.KEY_HONEYCOMB_COMPONENTS,
                        installedTargets(byPackage, DEFAULT_HONEYCOMB_PACKAGES).toString())
                .putString(ConfigContract.KEY_SIDE_COMPONENTS,
                        installedTargets(byPackage, DEFAULT_SIDE_PACKAGES).toString())
                .commit();
    }

    private static JSONArray installedTargets(Map<String, ComponentName> byPackage,
                                               String[] packages) {
        JSONArray result = new JSONArray();
        for (String packageName : packages) {
            ComponentName component = byPackage.get(packageName);
            if (component != null) {
                result.put(new AppTarget(component.flattenToString()).toJson());
            }
        }
        return result;
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

    @SuppressLint("ApplySharedPref")
    static void migrateSideLayoutModeIfNeeded(SharedPreferences preferences) {
        if (preferences.contains(ConfigContract.KEY_SIDE_LAYOUT_MODE)) return;
        int mode = preferences.getBoolean(ConfigContract.KEY_SIDE_FAN_LIST,
                ConfigContract.DEFAULT_SIDE_FAN_LIST)
                ? ConfigContract.SIDE_LAYOUT_FAN : ConfigContract.SIDE_LAYOUT_LIST;
        preferences.edit().putInt(ConfigContract.KEY_SIDE_LAYOUT_MODE, mode).commit();
    }

    @SuppressLint("ApplySharedPref")
    static void migrateSelectedAppNameIfNeeded(SharedPreferences preferences) {
        if (preferences.contains(ConfigContract.KEY_SHOW_SELECTED_APP_NAME)) return;
        boolean show = preferences.getBoolean(ConfigContract.KEY_SIDE_SHOW_APP_NAMES,
                ConfigContract.DEFAULT_SHOW_SELECTED_APP_NAME);
        preferences.edit().putBoolean(
                ConfigContract.KEY_SHOW_SELECTED_APP_NAME, show).commit();
    }

    @SuppressLint("ApplySharedPref")
    static void migrateIndependentSideTargetsIfNeeded(SharedPreferences preferences) {
        if (preferences.getBoolean(KEY_SIDE_TARGETS_INDEPENDENT_MIGRATED, false)) return;
        String side = preferences.getString(ConfigContract.KEY_SIDE_COMPONENTS, "[]");
        boolean sideEmpty = side == null || side.isEmpty() || "[]".equals(side);
        boolean followedHoneycomb = preferences.getBoolean(
                ConfigContract.KEY_SIDE_FOLLOW_HONEYCOMB, true);
        SharedPreferences.Editor editor = preferences.edit()
                .putBoolean(ConfigContract.KEY_SIDE_FOLLOW_HONEYCOMB, false)
                .putBoolean(KEY_SIDE_TARGETS_INDEPENDENT_MIGRATED, true);
        if (followedHoneycomb && sideEmpty) {
            editor.putString(ConfigContract.KEY_SIDE_COMPONENTS, preferences.getString(
                    ConfigContract.KEY_HONEYCOMB_COMPONENTS, "[]"));
        }
        editor.commit();
    }

    @SuppressLint("ApplySharedPref")
    static void migrateOrientationBehaviorIfNeeded(SharedPreferences preferences) {
        if (preferences.getBoolean(KEY_ORIENTATION_BEHAVIOR_MIGRATED, false)) return;
        boolean sideMaster = preferences.getBoolean(ConfigContract.KEY_SIDE_GESTURE_ENABLED,
                ConfigContract.DEFAULT_SIDE_GESTURE_ENABLED);
        boolean bottomPortrait = preferences.getBoolean(
                ConfigContract.KEY_BOTTOM_PORTRAIT_ENABLED,
                ConfigContract.DEFAULT_BOTTOM_PORTRAIT_ENABLED);
        boolean bottomLandscape = preferences.getBoolean(
                ConfigContract.KEY_BOTTOM_LANDSCAPE_ENABLED,
                ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_ENABLED);
        boolean sidePortrait = sideMaster && preferences.getBoolean(
                ConfigContract.KEY_SIDE_PORTRAIT_ENABLED,
                ConfigContract.DEFAULT_SIDE_PORTRAIT_ENABLED);
        boolean sideLandscape = sideMaster && preferences.getBoolean(
                ConfigContract.KEY_SIDE_LANDSCAPE_ENABLED,
                ConfigContract.DEFAULT_SIDE_LANDSCAPE_ENABLED);
        boolean honeycomb = preferences.getBoolean(ConfigContract.KEY_HONEYCOMB_ENABLED,
                ConfigContract.DEFAULT_HONEYCOMB_ENABLED);
        boolean honeycombLandscape = preferences.getBoolean(
                ConfigContract.KEY_HONEYCOMB_LANDSCAPE_ENABLED,
                ConfigContract.DEFAULT_HONEYCOMB_LANDSCAPE_ENABLED);
        int sideLayout = preferences.getInt(ConfigContract.KEY_SIDE_LAYOUT_MODE,
                ConfigContract.DEFAULT_SIDE_LAYOUT_MODE);
        boolean sideFullscreen = preferences.getBoolean(ConfigContract.KEY_SIDE_FULLSCREEN,
                ConfigContract.DEFAULT_SIDE_FULLSCREEN);
        boolean bottomFullscreen = preferences.getBoolean(
                ConfigContract.KEY_BOTTOM_FULLSCREEN,
                ConfigContract.DEFAULT_BOTTOM_FULLSCREEN);
        boolean bottomHoneycombFreeform = preferences.getBoolean(
                ConfigContract.KEY_BOTTOM_HONEYCOMB_FREEFORM,
                ConfigContract.DEFAULT_BOTTOM_HONEYCOMB_FREEFORM);
        preferences.edit()
                .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_ENABLED, bottomPortrait)
                .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_ENABLED, bottomLandscape)
                .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED,
                        honeycomb && bottomPortrait)
                .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED,
                        honeycomb && honeycombLandscape && bottomLandscape)
                .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_FULLSCREEN,
                        bottomFullscreen)
                .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_FULLSCREEN,
                        bottomFullscreen)
                .putBoolean(ConfigContract.KEY_BOTTOM_PORTRAIT_HONEYCOMB_FREEFORM,
                        bottomHoneycombFreeform)
                .putBoolean(ConfigContract.KEY_BOTTOM_LANDSCAPE_HONEYCOMB_FREEFORM,
                        bottomHoneycombFreeform)
                .putBoolean(ConfigContract.KEY_SIDE_GESTURE_ENABLED, true)
                .putBoolean(ConfigContract.KEY_SIDE_PORTRAIT_ENABLED, sidePortrait)
                .putBoolean(ConfigContract.KEY_SIDE_LANDSCAPE_ENABLED, sideLandscape)
                .putInt(ConfigContract.KEY_SIDE_PORTRAIT_LAYOUT_MODE, sideLayout)
                .putInt(ConfigContract.KEY_SIDE_LANDSCAPE_LAYOUT_MODE, sideLayout)
                .putBoolean(ConfigContract.KEY_SIDE_PORTRAIT_FULLSCREEN, sideFullscreen)
                .putBoolean(ConfigContract.KEY_SIDE_LANDSCAPE_FULLSCREEN, sideFullscreen)
                .putBoolean(ConfigContract.KEY_SIDE_ANIMATIONS_ENABLED,
                        preferences.getBoolean(ConfigContract.KEY_FAN_ANIMATIONS_ENABLED,
                                ConfigContract.DEFAULT_SIDE_ANIMATIONS_ENABLED))
                .putInt(ConfigContract.KEY_SIDE_ANIMATION_SPEED,
                        preferences.getInt(ConfigContract.KEY_FAN_ANIMATION_SPEED,
                                ConfigContract.DEFAULT_SIDE_ANIMATION_SPEED))
                .putInt(ConfigContract.KEY_SIDE_REVEAL_AMOUNT,
                        preferences.getInt(ConfigContract.KEY_FAN_REVEAL_AMOUNT,
                                ConfigContract.DEFAULT_SIDE_REVEAL_AMOUNT))
                .putInt(ConfigContract.KEY_SIDE_ROTATION_DEGREES,
                        preferences.getInt(ConfigContract.KEY_FAN_ROTATION_DEGREES,
                                ConfigContract.DEFAULT_SIDE_ROTATION_DEGREES))
                .putInt(ConfigContract.KEY_SIDE_SELECTION_SCALE_PERCENT,
                        preferences.getInt(ConfigContract.KEY_FAN_SELECTION_SCALE_PERCENT,
                                ConfigContract.DEFAULT_SIDE_SELECTION_SCALE_PERCENT))
                .putBoolean(ConfigContract.KEY_SIDE_SELECTION_RING,
                        preferences.getBoolean(ConfigContract.KEY_FAN_SELECTION_RING,
                                ConfigContract.DEFAULT_SIDE_SELECTION_RING))
                .putBoolean(KEY_ORIENTATION_BEHAVIOR_MIGRATED, true)
                .commit();
    }

    @SuppressLint("ApplySharedPref")
    static void ensureNativeWindowScaleDefaults(SharedPreferences preferences) {
        SharedPreferences.Editor editor = null;
        if (!preferences.contains(ConfigContract.KEY_PORTRAIT_PROPORTIONAL_SIZE_ENABLED)) {
            editor = preferences.edit().putBoolean(
                    ConfigContract.KEY_PORTRAIT_PROPORTIONAL_SIZE_ENABLED,
                    ConfigContract.DEFAULT_PORTRAIT_PROPORTIONAL_SIZE_ENABLED);
        }
        if (!preferences.contains(ConfigContract.KEY_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED)) {
            editor = (editor == null ? preferences.edit() : editor).putBoolean(
                    ConfigContract.KEY_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED,
                    ConfigContract.DEFAULT_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED);
        }
        if (!preferences.contains(ConfigContract.KEY_PORTRAIT_NATIVE_SCALE_PERCENT)) {
            editor = (editor == null ? preferences.edit() : editor).putInt(
                    ConfigContract.KEY_PORTRAIT_NATIVE_SCALE_PERCENT,
                    ConfigContract.DEFAULT_NATIVE_WINDOW_SCALE_PERCENT);
        }
        if (!preferences.contains(ConfigContract.KEY_LANDSCAPE_NATIVE_SCALE_PERCENT)) {
            editor = (editor == null ? preferences.edit() : editor).putInt(
                    ConfigContract.KEY_LANDSCAPE_NATIVE_SCALE_PERCENT,
                    ConfigContract.DEFAULT_NATIVE_WINDOW_SCALE_PERCENT);
        }
        if (editor != null) editor.commit();
    }
}
