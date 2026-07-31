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
    public static final String KEY_FORCE_CIRCULAR_ICONS = "force_circular_icons";
    public static final String KEY_FAN_FIXED_SEVEN_ROWS = "fan_fixed_seven_rows";
    public static final String KEY_FAN_LAYOUT_MODE = "fan_layout_mode";
    public static final String KEY_FAN_CUSTOM_OUTER_COUNT = "fan_custom_outer_count";
    public static final String KEY_FAN_CUSTOM_MIDDLE_COUNT = "fan_custom_middle_count";
    public static final String KEY_FAN_CUSTOM_INNER_COUNT = "fan_custom_inner_count";
    public static final String KEY_BOTTOM_PORTRAIT_ENABLED = "bottom_portrait_enabled";
    public static final String KEY_BOTTOM_LANDSCAPE_ENABLED = "bottom_landscape_enabled";
    public static final String KEY_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED =
            "bottom_portrait_second_stage_enabled";
    public static final String KEY_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED =
            "bottom_landscape_second_stage_enabled";
    public static final String KEY_BOTTOM_PORTRAIT_FULLSCREEN =
            "bottom_portrait_fullscreen";
    public static final String KEY_BOTTOM_LANDSCAPE_FULLSCREEN =
            "bottom_landscape_fullscreen";
    public static final String KEY_BOTTOM_PORTRAIT_HONEYCOMB_FREEFORM =
            "bottom_portrait_honeycomb_freeform";
    public static final String KEY_BOTTOM_LANDSCAPE_HONEYCOMB_FREEFORM =
            "bottom_landscape_honeycomb_freeform";
    public static final String KEY_SIDE_GESTURE_ENABLED = "side_gesture_enabled";
    public static final String KEY_SIDE_PORTRAIT_ENABLED = "side_portrait_enabled";
    public static final String KEY_SIDE_LANDSCAPE_ENABLED = "side_landscape_enabled";
    public static final String KEY_SIDE_PORTRAIT_LAYOUT_MODE =
            "side_portrait_layout_mode";
    public static final String KEY_SIDE_LANDSCAPE_LAYOUT_MODE =
            "side_landscape_layout_mode";
    public static final String KEY_SIDE_PORTRAIT_FULLSCREEN =
            "side_portrait_fullscreen";
    public static final String KEY_SIDE_LANDSCAPE_FULLSCREEN =
            "side_landscape_fullscreen";
    public static final String KEY_SIDE_ANIMATIONS_ENABLED = "side_animations_enabled";
    public static final String KEY_SIDE_ANIMATION_SPEED = "side_animation_speed";
    public static final String KEY_SIDE_REVEAL_AMOUNT = "side_reveal_amount";
    public static final String KEY_SIDE_ROTATION_DEGREES = "side_rotation_degrees";
    public static final String KEY_SIDE_SELECTION_SCALE_PERCENT =
            "side_selection_scale_percent";
    public static final String KEY_SIDE_SELECTION_RING = "side_selection_ring";
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
    public static final String KEY_SIDE_HOLD_ENABLED = "side_hold_enabled";
    public static final String KEY_SIDE_HOLD_DELAY_MS = "side_hold_delay_ms";
    public static final String KEY_SIDE_TASK_MAX_COUNT = "side_task_max_count";
    public static final String KEY_SIDE_TASK_CARD_SIZE_DP = "side_task_card_size_dp";
    public static final String KEY_SIDE_TASK_CARD_WIDTH_DP = "side_task_card_width_dp";
    public static final String KEY_SIDE_TASK_CARD_HEIGHT_DP = "side_task_card_height_dp";
    public static final String KEY_SIDE_TASK_CARD_CORNER_DP = "side_task_card_corner_dp";
    public static final String KEY_SIDE_TASK_ICON_SIZE_DP = "side_task_icon_size_dp";
    public static final String KEY_SIDE_TASK_ICON_GAP_DP = "side_task_icon_gap_dp";
    public static final String KEY_SIDE_TASK_FINGER_OFFSET_DP =
            "side_task_finger_offset_dp";
    public static final String KEY_SIDE_TASK_LAYOUT_MODE = "side_task_layout_mode";
    public static final String KEY_SIDE_TASK_SHOW_NAME = "side_task_show_name";
    public static final String KEY_SIDE_TASK_MOTION_MODE = "side_task_motion_mode";
    public static final String KEY_SIDE_TASK_SWIPE_SPEED_PERCENT =
            "side_task_swipe_speed_percent";
    public static final String KEY_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE =
            "side_task_extended_downward_tolerance";
    public static final String KEY_HIDE_SYSTEM_RECENTS_CLEAR =
            "hide_system_recents_clear";
    public static final String KEY_TRIGGER_PERCENT = "trigger_percent";
    public static final String KEY_SELECTION_RADIUS_PERCENT = "selection_radius_percent";
    public static final String KEY_HOT_WIDTH_PERCENT = "hot_width_percent";
    public static final String KEY_HOT_HEIGHT_PERCENT = "hot_height_percent";
    public static final String KEY_ICON_SIZE_DP = "icon_size_dp";
    public static final String KEY_CUSTOM_WINDOW_BOUNDS_ENABLED =
            "custom_window_bounds_enabled";
    public static final String KEY_CUSTOM_WINDOW_PORTRAIT_ENABLED =
            "custom_window_portrait_enabled";
    public static final String KEY_CUSTOM_WINDOW_LANDSCAPE_ENABLED =
            "custom_window_landscape_enabled";
    public static final String KEY_PORTRAIT_PROPORTIONAL_SIZE_ENABLED =
            "portrait_proportional_size_enabled";
    public static final String KEY_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED =
            "landscape_proportional_size_enabled";
    public static final String KEY_PORTRAIT_OVERALL_SIZE_PERCENT =
            "portrait_overall_size_percent";
    public static final String KEY_LANDSCAPE_OVERALL_SIZE_PERCENT =
            "landscape_overall_size_percent";
    public static final String KEY_PORTRAIT_NATIVE_SCALE_PERCENT =
            "portrait_native_scale_percent";
    public static final String KEY_LANDSCAPE_NATIVE_SCALE_PERCENT =
            "landscape_native_scale_percent";
    public static final String KEY_WIDTH_PERCENT = "width_percent";
    public static final String KEY_HEIGHT_PERCENT = "height_percent";
    public static final String KEY_POSITION_X = "position_x";
    public static final String KEY_POSITION_Y = "position_y";
    public static final String KEY_LANDSCAPE_WIDTH_PERCENT = "landscape_width_percent";
    public static final String KEY_LANDSCAPE_HEIGHT_PERCENT = "landscape_height_percent";
    public static final String KEY_LANDSCAPE_POSITION_X = "landscape_position_x";
    public static final String KEY_LANDSCAPE_POSITION_Y = "landscape_position_y";
    public static final String KEY_COMPONENTS = "components";
    public static final String KEY_SHORTCUT_CATALOG = "shortcut_catalog";
    public static final String KEY_SHORTCUT_CATALOG_REQUEST = "shortcut_catalog_request";
    public static final String KEY_ACTIVITY_CATALOG = "activity_catalog";
    public static final String KEY_ACTIVITY_CATALOG_REQUEST = "activity_catalog_request";
    public static final String KEY_INTERFACE_STATUS = "interface_status";
    public static final String KEY_INTERFACE_TIME = "interface_time";
    public static final String KEY_DIAGNOSTICS_SYSTEM_UI = "diagnostics_system_ui";
    public static final String KEY_DIAGNOSTICS_MIUI_HOME = "diagnostics_miui_home";
    public static final String EXTRA_DIAGNOSTIC_PROCESS = "diagnostic_process";
    public static final String EXTRA_DIAGNOSTIC_TEXT = "diagnostic_text";
    public static final String KEY_OUTSIDE_SINGLE_ACTION = "outside_single_action";
    public static final String KEY_OUTSIDE_DOUBLE_ACTION = "outside_double_action";
    public static final String KEY_OUTSIDE_TAP_WINDOW_MS = "outside_tap_window_ms";
    public static final String KEY_OUTSIDE_ENABLED = "outside_enabled";
    public static final String KEY_OUTSIDE_PORTRAIT_ENABLED =
            "outside_portrait_enabled";
    public static final String KEY_OUTSIDE_LANDSCAPE_ENABLED =
            "outside_landscape_enabled";
    public static final String KEY_HONEYCOMB_ENABLED = "honeycomb_enabled";
    public static final String KEY_HONEYCOMB_COMPONENTS = "honeycomb_components";
    public static final String KEY_HONEYCOMB_MODE = "honeycomb_mode";
    public static final String KEY_HONEYCOMB_TRIGGER_DP = "honeycomb_trigger_dp";
    public static final String KEY_HONEYCOMB_ICON_SIZE_DP = "honeycomb_icon_size_dp";
    public static final String KEY_HONEYCOMB_SPACING_DP = "honeycomb_spacing_dp";
    public static final String KEY_HONEYCOMB_ANIMATION_SPEED = "honeycomb_animation_speed";
    public static final String KEY_HONEYCOMB_INERTIA = "honeycomb_inertia";
    public static final String KEY_HONEYCOMB_CENTER_SCALE = "honeycomb_center_scale";
    public static final String KEY_HONEYCOMB_EDGE_SCALE = "honeycomb_edge_scale";
    public static final String KEY_HONEYCOMB_SELECTION_SCALE = "honeycomb_selection_scale";
    public static final String KEY_HONEYCOMB_SHOW_SELECTED_NAME =
            "honeycomb_show_selected_name";
    public static final String KEY_HONEYCOMB_EMPTY_TAP_CLOSE = "honeycomb_empty_tap_close";
    public static final String KEY_FAN_MAX_TARGETS = "fan_max_targets";
    public static final String KEY_HONEYCOMB_MAX_TARGETS = "honeycomb_max_targets";
    public static final String KEY_SIDE_COMPONENTS = "side_components";
    public static final String KEY_SIDE_FOLLOW_HONEYCOMB = "side_follow_honeycomb";
    public static final String KEY_SIDE_MAX_TARGETS = "side_max_targets";
    public static final String KEY_SIDE_DIRECTION_HORIZONTAL = "side_direction_horizontal";
    public static final String KEY_SIDE_DIRECTION_UP = "side_direction_up";
    public static final String KEY_SIDE_DIRECTION_DOWN = "side_direction_down";
    public static final String KEY_SIDE_FULLSCREEN = "side_honeycomb_fullscreen";
    @Deprecated
    public static final String KEY_SIDE_HONEYCOMB_FULLSCREEN = KEY_SIDE_FULLSCREEN;
    public static final String KEY_BOTTOM_FULLSCREEN = "bottom_fullscreen";
    public static final String KEY_BOTTOM_HONEYCOMB_FREEFORM =
            "bottom_honeycomb_freeform";
    public static final String KEY_HONEYCOMB_FOLLOW_FINGER = "honeycomb_follow_finger";
    public static final String KEY_HONEYCOMB_LANDSCAPE_ENABLED =
            "honeycomb_landscape_enabled";
    public static final String KEY_HONEYCOMB_FIXED_X_PERCENT =
            "honeycomb_fixed_x_percent";
    public static final String KEY_HONEYCOMB_FIXED_Y_PERCENT =
            "honeycomb_fixed_y_percent";
    public static final String KEY_HONEYCOMB_BACKGROUND_STYLE = "honeycomb_background_style";
    public static final String KEY_HONEYCOMB_BLUR_DP = "honeycomb_blur_dp";
    public static final String KEY_HONEYCOMB_DIM_PERCENT = "honeycomb_dim_percent";
    public static final String KEY_HONEYCOMB_RETREAT_DP = "honeycomb_retreat_dp";
    public static final String KEY_HONEYCOMB_DISC_SIZE_PERCENT =
            "honeycomb_disc_size_percent";

    public static final int ACTION_NONE = 0;
    public static final int ACTION_CLOSE = 1;
    public static final int ACTION_PIN = 2;
    public static final int ACTION_FULLSCREEN = 3;
    public static final int ACTION_EDGE_PIN = 4;

    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_HAPTIC = true;
    public static final boolean DEFAULT_FAN_SHADOW = true;
    public static final boolean DEFAULT_FAN_ANIMATIONS_ENABLED = true;
    public static final int DEFAULT_FAN_ANIMATION_SPEED = 140;
    public static final int MIN_FAN_ANIMATION_SPEED = 75;
    public static final int MAX_FAN_ANIMATION_SPEED = 160;
    public static final int DEFAULT_FAN_REVEAL_AMOUNT = 49;
    public static final int MIN_FAN_REVEAL_AMOUNT = 15;
    public static final int MAX_FAN_REVEAL_AMOUNT = 130;
    public static final int DEFAULT_FAN_ROTATION_DEGREES = 48;
    public static final int MIN_FAN_ROTATION_DEGREES = 0;
    public static final int MAX_FAN_ROTATION_DEGREES = 90;
    public static final int DEFAULT_FAN_SELECTION_SCALE_PERCENT = 40;
    public static final int MIN_FAN_SELECTION_SCALE_PERCENT = 5;
    public static final int MAX_FAN_SELECTION_SCALE_PERCENT = 40;
    public static final boolean DEFAULT_FAN_SELECTION_RING = false;
    public static final boolean DEFAULT_FORCE_CIRCULAR_ICONS = true;
    public static final boolean DEFAULT_FAN_FIXED_SEVEN_ROWS = false;
    public static final int FAN_LAYOUT_SMART = 0;
    public static final int FAN_LAYOUT_FIXED_SEVEN = 1;
    public static final int FAN_LAYOUT_CUSTOM = 2;
    public static final int DEFAULT_FAN_LAYOUT_MODE = FAN_LAYOUT_SMART;
    public static final int DEFAULT_FAN_CUSTOM_OUTER_COUNT = 7;
    public static final int DEFAULT_FAN_CUSTOM_MIDDLE_COUNT = 6;
    public static final int DEFAULT_FAN_CUSTOM_INNER_COUNT = 5;
    public static final int MIN_FAN_CUSTOM_ROW_COUNT = 0;
    public static final int MAX_FAN_CUSTOM_INNER_COUNT = 8;
    public static final int MAX_FAN_CUSTOM_MIDDLE_COUNT = 10;
    public static final int MAX_FAN_CUSTOM_OUTER_COUNT = 14;
    public static final boolean DEFAULT_BOTTOM_PORTRAIT_ENABLED = true;
    public static final boolean DEFAULT_BOTTOM_LANDSCAPE_ENABLED = false;
    public static final boolean DEFAULT_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED = true;
    public static final boolean DEFAULT_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED = false;
    public static final boolean DEFAULT_SIDE_GESTURE_ENABLED = true;
    public static final boolean DEFAULT_SIDE_PORTRAIT_ENABLED = true;
    public static final boolean DEFAULT_SIDE_LANDSCAPE_ENABLED = false;
    public static final boolean DEFAULT_SIDE_ANIMATIONS_ENABLED = true;
    public static final int DEFAULT_SIDE_ANIMATION_SPEED = DEFAULT_FAN_ANIMATION_SPEED;
    public static final int DEFAULT_SIDE_REVEAL_AMOUNT = DEFAULT_FAN_REVEAL_AMOUNT;
    public static final int DEFAULT_SIDE_ROTATION_DEGREES = DEFAULT_FAN_ROTATION_DEGREES;
    public static final int DEFAULT_SIDE_SELECTION_SCALE_PERCENT =
            DEFAULT_FAN_SELECTION_SCALE_PERCENT;
    public static final boolean DEFAULT_SIDE_SELECTION_RING = false;
    public static final int DEFAULT_SIDE_TRIGGER_PERCENT = 47;
    public static final int MIN_SIDE_TRIGGER_PERCENT = 20;
    public static final int MAX_SIDE_TRIGGER_PERCENT = 100;
    public static final int DEFAULT_SIDE_ICON_SIZE_DP = 34;
    public static final int DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT = 20;
    public static final boolean DEFAULT_SHOW_SELECTED_APP_NAME = false;
    public static final boolean DEFAULT_SIDE_SHOW_APP_NAMES =
            DEFAULT_SHOW_SELECTED_APP_NAME;
    public static final boolean DEFAULT_SIDE_FOLLOW_FINGER = true;
    public static final boolean DEFAULT_SIDE_FAN_LIST = true;
    public static final int SIDE_LAYOUT_LIST = 0;
    public static final int SIDE_LAYOUT_RING = 1;
    public static final int SIDE_LAYOUT_FAN = 2;
    public static final int SIDE_LAYOUT_HONEYCOMB = 3;
    public static final int SIDE_LAYOUT_TASKS = 4;
    public static final int SIDE_LAYOUT_SYSTEM_RECENTS = 5;
    public static final int DEFAULT_SIDE_LAYOUT_MODE = SIDE_LAYOUT_HONEYCOMB;
    public static final int DEFAULT_SIDE_RING_SIZE_PERCENT = 97;
    public static final int MIN_SIDE_RING_SIZE_PERCENT = 60;
    public static final int MAX_SIDE_RING_SIZE_PERCENT = 180;
    public static final boolean DEFAULT_SIDE_WHEEL_MODE = false;
    public static final int DEFAULT_SIDE_REVERSE_CANCEL_PERCENT = 12;
    public static final int MIN_SIDE_REVERSE_CANCEL_PERCENT = 5;
    public static final int MAX_SIDE_REVERSE_CANCEL_PERCENT = 25;
    public static final boolean DEFAULT_SIDE_HOLD_ENABLED = true;
    public static final int DEFAULT_SIDE_HOLD_DELAY_MS = 360;
    public static final int MIN_SIDE_HOLD_DELAY_MS = 150;
    public static final int MAX_SIDE_HOLD_DELAY_MS = 1200;
    public static final int DEFAULT_SIDE_TASK_MAX_COUNT = 6;
    public static final int MIN_SIDE_TASK_MAX_COUNT = 2;
    public static final int MAX_SIDE_TASK_MAX_COUNT = 10;
    public static final int DEFAULT_SIDE_TASK_CARD_SIZE_DP = 112;
    public static final int MIN_SIDE_TASK_CARD_SIZE_DP = 80;
    public static final int MAX_SIDE_TASK_CARD_SIZE_DP = 150;
    public static final int DEFAULT_SIDE_TASK_CARD_WIDTH_DP = 112;
    public static final int MIN_SIDE_TASK_CARD_WIDTH_DP = 72;
    public static final int MAX_SIDE_TASK_CARD_WIDTH_DP = 360;
    public static final int DEFAULT_SIDE_TASK_CARD_HEIGHT_DP = 187;
    public static final int MIN_SIDE_TASK_CARD_HEIGHT_DP = 110;
    public static final int MAX_SIDE_TASK_CARD_HEIGHT_DP = 700;
    public static final int DEFAULT_SIDE_TASK_CARD_CORNER_DP = 22;
    public static final int MIN_SIDE_TASK_CARD_CORNER_DP = 0;
    public static final int MAX_SIDE_TASK_CARD_CORNER_DP = 48;
    public static final int DEFAULT_SIDE_TASK_ICON_SIZE_DP = 64;
    public static final int MIN_SIDE_TASK_ICON_SIZE_DP = 32;
    public static final int MAX_SIDE_TASK_ICON_SIZE_DP = 120;
    public static final int DEFAULT_SIDE_TASK_ICON_GAP_DP = 16;
    public static final int MIN_SIDE_TASK_ICON_GAP_DP = 0;
    public static final int MAX_SIDE_TASK_ICON_GAP_DP = 80;
    public static final int DEFAULT_SIDE_TASK_FINGER_OFFSET_DP = 105;
    public static final int MIN_SIDE_TASK_FINGER_OFFSET_DP = 60;
    public static final int MAX_SIDE_TASK_FINGER_OFFSET_DP = 180;
    public static final int SIDE_TASK_LAYOUT_FLAT = 0;
    public static final int SIDE_TASK_LAYOUT_ICONS = 1;
    public static final int DEFAULT_SIDE_TASK_LAYOUT_MODE = SIDE_TASK_LAYOUT_FLAT;
    public static final boolean DEFAULT_SIDE_TASK_SHOW_NAME = true;
    public static final int SIDE_TASK_MOTION_APPLE = 0;
    public static final int SIDE_TASK_MOTION_MAGNETIC = 1;
    public static final int SIDE_TASK_MOTION_MARBLE = 2;
    public static final int DEFAULT_SIDE_TASK_MOTION_MODE = SIDE_TASK_MOTION_MAGNETIC;
    public static final int DEFAULT_SIDE_TASK_SWIPE_SPEED_PERCENT = 180;
    public static final int MIN_SIDE_TASK_SWIPE_SPEED_PERCENT = 100;
    public static final int MAX_SIDE_TASK_SWIPE_SPEED_PERCENT = 300;
    public static final boolean DEFAULT_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE = true;
    public static final boolean DEFAULT_HIDE_SYSTEM_RECENTS_CLEAR = false;
    public static final int DEFAULT_TRIGGER_PERCENT = 17;
    public static final int DEFAULT_SELECTION_RADIUS_PERCENT = 75;
    public static final int MIN_SELECTION_RADIUS_PERCENT = 1;
    public static final int MAX_SELECTION_RADIUS_PERCENT = 150;
    public static final int DEFAULT_HOT_WIDTH_PERCENT = 8;
    public static final int DEFAULT_HOT_HEIGHT_PERCENT = 8;
    public static final int MAX_HOT_HEIGHT_PERCENT = 20;
    public static final int DEFAULT_ICON_SIZE_DP = 37;
    public static final boolean DEFAULT_CUSTOM_WINDOW_BOUNDS_ENABLED = true;
    public static final boolean DEFAULT_CUSTOM_WINDOW_PORTRAIT_ENABLED = true;
    public static final boolean DEFAULT_CUSTOM_WINDOW_LANDSCAPE_ENABLED = false;
    public static final boolean DEFAULT_PORTRAIT_PROPORTIONAL_SIZE_ENABLED = true;
    public static final boolean DEFAULT_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED = true;
    public static final int MIN_PORTRAIT_OVERALL_SIZE_PERCENT = 35;
    public static final int MAX_PORTRAIT_OVERALL_SIZE_PERCENT = 90;
    public static final int DEFAULT_PORTRAIT_OVERALL_SIZE_PERCENT = 74;
    public static final int MIN_LANDSCAPE_OVERALL_SIZE_PERCENT = 20;
    public static final int MAX_LANDSCAPE_OVERALL_SIZE_PERCENT = 90;
    public static final int DEFAULT_LANDSCAPE_OVERALL_SIZE_PERCENT = 41;
    public static final int MIN_NATIVE_WINDOW_SCALE_PERCENT = 50;
    public static final int DEFAULT_NATIVE_WINDOW_SCALE_PERCENT = 100;
    public static final int MAX_NATIVE_WINDOW_SCALE_PERCENT = 150;
    public static final int DEFAULT_WIDTH_PERCENT = 85;
    public static final int DEFAULT_HEIGHT_PERCENT = 65;
    public static final int DEFAULT_POSITION_X = 50;
    public static final int DEFAULT_POSITION_Y = 50;
    public static final int DEFAULT_LANDSCAPE_WIDTH_PERCENT = 20;
    public static final int MIN_LANDSCAPE_WIDTH_PERCENT = 20;
    public static final int MAX_LANDSCAPE_WIDTH_PERCENT = 90;
    public static final int DEFAULT_LANDSCAPE_HEIGHT_PERCENT = 85;
    public static final int DEFAULT_LANDSCAPE_POSITION_X = 0;
    public static final int DEFAULT_LANDSCAPE_POSITION_Y = 50;
    public static final int DEFAULT_OUTSIDE_SINGLE_ACTION = ACTION_CLOSE;
    public static final int DEFAULT_OUTSIDE_DOUBLE_ACTION = ACTION_EDGE_PIN;
    public static final int DEFAULT_OUTSIDE_TAP_WINDOW_MS = 180;
    public static final boolean DEFAULT_OUTSIDE_ENABLED = true;
    public static final boolean DEFAULT_OUTSIDE_PORTRAIT_ENABLED = true;
    public static final boolean DEFAULT_OUTSIDE_LANDSCAPE_ENABLED = false;
    public static final int MIN_OUTSIDE_TAP_WINDOW_MS = 120;
    public static final int MAX_OUTSIDE_TAP_WINDOW_MS = 450;
    public static final int HONEYCOMB_MODE_BROWSE = 0;
    public static final int HONEYCOMB_MODE_HOLD = 1;
    public static final boolean DEFAULT_HONEYCOMB_ENABLED = true;
    public static final int DEFAULT_HONEYCOMB_MODE = HONEYCOMB_MODE_HOLD;
    public static final int DEFAULT_HONEYCOMB_TRIGGER_DP = 198;
    public static final int MIN_HONEYCOMB_TRIGGER_DP = 120;
    public static final int MAX_HONEYCOMB_TRIGGER_DP = 600;
    public static final int DEFAULT_HONEYCOMB_ICON_SIZE_DP = 40;
    public static final int MIN_HONEYCOMB_ICON_SIZE_DP = 20;
    public static final int MAX_HONEYCOMB_ICON_SIZE_DP = 100;
    public static final int DEFAULT_HONEYCOMB_SPACING_DP = 50;
    public static final int MIN_HONEYCOMB_SPACING_DP = 24;
    public static final int MAX_HONEYCOMB_SPACING_DP = 120;
    public static final int DEFAULT_HONEYCOMB_ANIMATION_SPEED = 2;
    public static final int DEFAULT_HONEYCOMB_INERTIA = 1;
    public static final int DEFAULT_HONEYCOMB_CENTER_SCALE = 124;
    public static final int MIN_HONEYCOMB_CENTER_SCALE = 105;
    public static final int MAX_HONEYCOMB_CENTER_SCALE = 160;
    public static final int DEFAULT_HONEYCOMB_EDGE_SCALE = 90;
    public static final int MIN_HONEYCOMB_EDGE_SCALE = 40;
    public static final int MAX_HONEYCOMB_EDGE_SCALE = 90;
    public static final int DEFAULT_HONEYCOMB_SELECTION_SCALE = 130;
    public static final int MIN_HONEYCOMB_SELECTION_SCALE = 105;
    public static final int MAX_HONEYCOMB_SELECTION_SCALE = 160;
    public static final boolean DEFAULT_HONEYCOMB_EMPTY_TAP_CLOSE = true;
    public static final boolean DEFAULT_HONEYCOMB_SHOW_SELECTED_NAME = true;
    public static final int DEFAULT_FAN_MAX_TARGETS = 15;
    public static final int MIN_FAN_MAX_TARGETS = 3;
    public static final int MAX_FAN_MAX_TARGETS = 24;
    public static final int DEFAULT_HONEYCOMB_MAX_TARGETS = 59;
    public static final int MIN_HONEYCOMB_MAX_TARGETS = 1;
    public static final int MAX_HONEYCOMB_MAX_TARGETS = 60;
    public static final boolean DEFAULT_SIDE_FOLLOW_HONEYCOMB = false;
    public static final int DEFAULT_SIDE_MAX_TARGETS = 10;
    public static final int MIN_SIDE_MAX_TARGETS = 1;
    public static final int MAX_SIDE_MAX_TARGETS = 36;
    public static final boolean DEFAULT_SIDE_DIRECTION_HORIZONTAL = true;
    public static final boolean DEFAULT_SIDE_DIRECTION_UP = true;
    public static final boolean DEFAULT_SIDE_DIRECTION_DOWN = true;
    public static final boolean DEFAULT_SIDE_FULLSCREEN = false;
    public static final boolean DEFAULT_SIDE_PORTRAIT_FULLSCREEN = false;
    public static final boolean DEFAULT_SIDE_LANDSCAPE_FULLSCREEN = false;
    @Deprecated
    public static final boolean DEFAULT_SIDE_HONEYCOMB_FULLSCREEN = DEFAULT_SIDE_FULLSCREEN;
    public static final boolean DEFAULT_BOTTOM_FULLSCREEN = false;
    public static final boolean DEFAULT_BOTTOM_HONEYCOMB_FREEFORM = true;
    public static final boolean DEFAULT_BOTTOM_PORTRAIT_FULLSCREEN = false;
    public static final boolean DEFAULT_BOTTOM_LANDSCAPE_FULLSCREEN = false;
    public static final boolean DEFAULT_BOTTOM_PORTRAIT_HONEYCOMB_FREEFORM = true;
    public static final boolean DEFAULT_BOTTOM_LANDSCAPE_HONEYCOMB_FREEFORM = true;
    public static final boolean DEFAULT_HONEYCOMB_FOLLOW_FINGER = false;
    public static final boolean DEFAULT_HONEYCOMB_LANDSCAPE_ENABLED = false;
    public static final int DEFAULT_HONEYCOMB_FIXED_X_PERCENT = 50;
    public static final int DEFAULT_HONEYCOMB_FIXED_Y_PERCENT = 60;
    public static final int HONEYCOMB_BACKGROUND_BLUR = 0;
    public static final int HONEYCOMB_BACKGROUND_BLACK = 1;
    public static final int DEFAULT_HONEYCOMB_BACKGROUND_STYLE = HONEYCOMB_BACKGROUND_BLUR;
    public static final int DEFAULT_HONEYCOMB_BLUR_DP = 36;
    public static final int DEFAULT_HONEYCOMB_DIM_PERCENT = 22;
    public static final int DEFAULT_HONEYCOMB_RETREAT_DP = 120;
    public static final int MIN_HONEYCOMB_RETREAT_DP = 20;
    public static final int MAX_HONEYCOMB_RETREAT_DP = 120;
    public static final int DEFAULT_HONEYCOMB_DISC_SIZE_PERCENT = 60;
    public static final int MIN_HONEYCOMB_DISC_SIZE_PERCENT = 50;
    public static final int MAX_HONEYCOMB_DISC_SIZE_PERCENT = 100;
}
