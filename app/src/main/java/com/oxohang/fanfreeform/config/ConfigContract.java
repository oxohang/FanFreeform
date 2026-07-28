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
    public static final String KEY_FAN_ANIMATIONS_ENABLED = "fan_animations_enabled";
    public static final String KEY_FAN_ANIMATION_SPEED = "fan_animation_speed";
    public static final String KEY_FAN_REVEAL_AMOUNT = "fan_reveal_amount";
    public static final String KEY_FAN_ROTATION_DEGREES = "fan_rotation_degrees";
    public static final String KEY_FAN_SELECTION_SCALE_PERCENT =
            "fan_selection_scale_percent";
    public static final String KEY_FAN_SELECTION_RING = "fan_selection_ring";
    public static final String KEY_SIDE_GESTURE_ENABLED = "side_gesture_enabled";
    public static final String KEY_SIDE_TRIGGER_PERCENT = "side_trigger_percent";
    public static final String KEY_SIDE_ICON_SIZE_DP = "side_icon_size_dp";
    public static final String KEY_SIDE_TOP_SAFE_MARGIN_PERCENT = "side_top_safe_margin_percent";
    public static final String KEY_SHOW_SELECTED_APP_NAME = "show_selected_app_name";
    public static final String KEY_SIDE_SHOW_APP_NAMES = "side_show_app_names";
    public static final String KEY_SIDE_FOLLOW_FINGER = "side_follow_finger";
    public static final String KEY_SIDE_FAN_LIST = "side_fan_list";
    public static final String KEY_SIDE_LAYOUT_MODE = "side_layout_mode";
    public static final String KEY_SIDE_RING_SIZE_PERCENT = "side_ring_size_percent";
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
    public static final String KEY_SHORTCUT_CATALOG = "shortcut_catalog";
    public static final String KEY_SHORTCUT_CATALOG_REQUEST = "shortcut_catalog_request";
    public static final String KEY_ACTIVITY_CATALOG = "activity_catalog";
    public static final String KEY_ACTIVITY_CATALOG_REQUEST = "activity_catalog_request";
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
    public static final boolean DEFAULT_FAN_ANIMATIONS_ENABLED = true;
    public static final int DEFAULT_FAN_ANIMATION_SPEED = 100;
    public static final int MIN_FAN_ANIMATION_SPEED = 75;
    public static final int MAX_FAN_ANIMATION_SPEED = 160;
    public static final int DEFAULT_FAN_REVEAL_AMOUNT = 42;
    public static final int MIN_FAN_REVEAL_AMOUNT = 15;
    public static final int MAX_FAN_REVEAL_AMOUNT = 130;
    public static final int DEFAULT_FAN_ROTATION_DEGREES = 24;
    public static final int MIN_FAN_ROTATION_DEGREES = 0;
    public static final int MAX_FAN_ROTATION_DEGREES = 90;
    public static final int DEFAULT_FAN_SELECTION_SCALE_PERCENT = 18;
    public static final int MIN_FAN_SELECTION_SCALE_PERCENT = 5;
    public static final int MAX_FAN_SELECTION_SCALE_PERCENT = 40;
    public static final boolean DEFAULT_FAN_SELECTION_RING = true;
    public static final boolean DEFAULT_SIDE_GESTURE_ENABLED = false;
    public static final int DEFAULT_SIDE_TRIGGER_PERCENT = 30;
    public static final int DEFAULT_SIDE_ICON_SIZE_DP = 46;
    public static final int DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT = 20;
    public static final boolean DEFAULT_SHOW_SELECTED_APP_NAME = false;
    public static final boolean DEFAULT_SIDE_SHOW_APP_NAMES =
            DEFAULT_SHOW_SELECTED_APP_NAME;
    public static final boolean DEFAULT_SIDE_FOLLOW_FINGER = true;
    public static final boolean DEFAULT_SIDE_FAN_LIST = false;
    public static final int SIDE_LAYOUT_LIST = 0;
    public static final int SIDE_LAYOUT_RING = 1;
    public static final int SIDE_LAYOUT_FAN = 2;
    public static final int DEFAULT_SIDE_LAYOUT_MODE = SIDE_LAYOUT_LIST;
    public static final int DEFAULT_SIDE_RING_SIZE_PERCENT = 100;
    public static final int MIN_SIDE_RING_SIZE_PERCENT = 60;
    public static final int MAX_SIDE_RING_SIZE_PERCENT = 180;
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
