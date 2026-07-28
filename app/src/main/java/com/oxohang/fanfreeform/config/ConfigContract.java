package com.oxohang.fanfreeform.config;

import android.net.Uri;

public final class ConfigContract {
    private ConfigContract() {}

    public static final String AUTHORITY = "com.oxohang.fanfreeform.config";
    public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/state");
    public static final String PREFS = "fan_config";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_HAPTIC = "haptic";
    public static final String KEY_FAN_SHADOW = "fan_shadow";
    public static final String KEY_SIDE_GESTURE_ENABLED = "side_gesture_enabled";
    public static final String KEY_SIDE_TRIGGER_PERCENT = "side_trigger_percent";
    public static final String KEY_SIDE_ICON_SIZE_DP = "side_icon_size_dp";
    public static final String KEY_SIDE_TOP_SAFE_MARGIN_PERCENT = "side_top_safe_margin_percent";
    public static final String KEY_SIDE_SHOW_APP_NAMES = "side_show_app_names";
    public static final String KEY_SIDE_FOLLOW_FINGER = "side_follow_finger";
    public static final String KEY_SIDE_WHEEL_MODE = "side_wheel_mode";
    public static final String KEY_SIDE_REVERSE_CANCEL_PERCENT = "side_reverse_cancel_percent";
    public static final String KEY_TRIGGER_PERCENT = "trigger_percent";
    public static final String KEY_SELECTION_RADIUS_PERCENT = "selection_radius_percent";
    public static final String KEY_HOT_WIDTH_PERCENT = "hot_width_percent";
    public static final String KEY_HOT_HEIGHT_PERCENT = "hot_height_percent";
    public static final String KEY_ICON_SIZE_DP = "icon_size_dp";
    public static final String KEY_WIDTH_PERCENT = "width_percent";
    public static final String KEY_HEIGHT_PERCENT = "height_percent";
    public static final String KEY_POSITION_X = "position_x";
    public static final String KEY_POSITION_Y = "position_y";
    public static final String KEY_COMPONENTS = "components";
    public static final String KEY_INTERFACE_STATUS = "interface_status";
    public static final String KEY_INTERFACE_TIME = "interface_time";
    public static final String KEY_OUTSIDE_SINGLE_ACTION = "outside_single_action";
    public static final String KEY_OUTSIDE_DOUBLE_ACTION = "outside_double_action";

    public static final int ACTION_NONE = 0;
    public static final int ACTION_CLOSE = 1;
    public static final int ACTION_PIN = 2;
    public static final int ACTION_FULLSCREEN = 3;

    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_HAPTIC = true;
    public static final boolean DEFAULT_FAN_SHADOW = true;
    public static final boolean DEFAULT_SIDE_GESTURE_ENABLED = false;
    public static final int DEFAULT_SIDE_TRIGGER_PERCENT = 30;
    public static final int DEFAULT_SIDE_ICON_SIZE_DP = 46;
    public static final int DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT = 20;
    public static final boolean DEFAULT_SIDE_SHOW_APP_NAMES = false;
    public static final boolean DEFAULT_SIDE_FOLLOW_FINGER = true;
    public static final boolean DEFAULT_SIDE_WHEEL_MODE = false;
    public static final int DEFAULT_SIDE_REVERSE_CANCEL_PERCENT = 12;
    public static final int MIN_SIDE_REVERSE_CANCEL_PERCENT = 5;
    public static final int MAX_SIDE_REVERSE_CANCEL_PERCENT = 25;
    public static final int DEFAULT_TRIGGER_PERCENT = 12;
    public static final int DEFAULT_SELECTION_RADIUS_PERCENT = 58;
    public static final int DEFAULT_HOT_WIDTH_PERCENT = 12;
    public static final int DEFAULT_HOT_HEIGHT_PERCENT = 7;
    public static final int MAX_HOT_HEIGHT_PERCENT = 20;
    public static final int DEFAULT_ICON_SIZE_DP = 46;
    public static final int DEFAULT_WIDTH_PERCENT = 62;
    public static final int DEFAULT_HEIGHT_PERCENT = 58;
    public static final int DEFAULT_POSITION_X = 50;
    public static final int DEFAULT_POSITION_Y = 50;
    public static final int DEFAULT_OUTSIDE_SINGLE_ACTION = ACTION_CLOSE;
    public static final int DEFAULT_OUTSIDE_DOUBLE_ACTION = ACTION_PIN;
}
