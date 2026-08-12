package com.oxohang.fanfreeform.config;

import org.json.JSONObject;

/** One independently configurable pressure hit area. */
public final class PressureTrigger {
    public final int id;
    public final boolean enabled;
    public final int centerXPercent;
    public final int centerYPercent;
    public final int radiusPercent;
    public final int action;
    public final AppTarget target;
    /** Per-trigger launch mode: true opens the picked app as a freeform window. */
    public final boolean openAsFreeform;
    /** Per-trigger heavy-press inversion: true lets a second heavy press flip the mode. */
    public final boolean heavyLaunchEnabled;

    public PressureTrigger(int id, boolean enabled, int centerXPercent, int centerYPercent,
                           int radiusPercent, int action, AppTarget target) {
        this(id, enabled, centerXPercent, centerYPercent, radiusPercent, action, target,
                ConfigContract.DEFAULT_PRESSURE_OPEN_AS_FREEFORM,
                ConfigContract.DEFAULT_PRESSURE_HEAVY_LAUNCH_ENABLED);
    }

    public PressureTrigger(int id, boolean enabled, int centerXPercent, int centerYPercent,
                           int radiusPercent, int action, AppTarget target,
                           boolean openAsFreeform, boolean heavyLaunchEnabled) {
        this.id = Math.max(1, id);
        this.enabled = enabled;
        this.centerXPercent = clamp(centerXPercent, 0, 100);
        this.centerYPercent = clamp(centerYPercent, 0, 100);
        this.radiusPercent = clamp(radiusPercent, ConfigContract.MIN_PRESSURE_RADIUS_PERCENT,
                ConfigContract.MAX_PRESSURE_RADIUS_PERCENT);
        this.action = supportedAction(action);
        this.target = target;
        this.openAsFreeform = openAsFreeform;
        this.heavyLaunchEnabled = heavyLaunchEnabled;
    }

    public static PressureTrigger defaultTrigger() {
        return new PressureTrigger(1, true,
                ConfigContract.DEFAULT_PRESSURE_CENTER_X_PERCENT,
                ConfigContract.DEFAULT_PRESSURE_CENTER_Y_PERCENT,
                ConfigContract.DEFAULT_PRESSURE_RADIUS_PERCENT,
                ConfigContract.DEFAULT_PRESSURE_ACTION, null);
    }

    public PressureTrigger withEnabled(boolean value) {
        return new PressureTrigger(id, value, centerXPercent, centerYPercent, radiusPercent,
                action, target, openAsFreeform, heavyLaunchEnabled);
    }

    public PressureTrigger withGeometry(int centerX, int centerY, int radius) {
        return new PressureTrigger(id, enabled, centerX, centerY, radius, action, target,
                openAsFreeform, heavyLaunchEnabled);
    }

    public PressureTrigger withAction(int value, AppTarget nextTarget) {
        return new PressureTrigger(id, enabled, centerXPercent, centerYPercent, radiusPercent,
                value, nextTarget, openAsFreeform, heavyLaunchEnabled);
    }

    public PressureTrigger withLaunchMode(boolean openAsFreeform, boolean heavyLaunchEnabled) {
        return new PressureTrigger(id, enabled, centerXPercent, centerYPercent, radiusPercent,
                action, target, openAsFreeform, heavyLaunchEnabled);
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put("id", id);
            object.put("enabled", enabled);
            object.put("centerX", centerXPercent);
            object.put("centerY", centerYPercent);
            object.put("radius", radiusPercent);
            object.put("action", action);
            object.put("openAsFreeform", openAsFreeform);
            object.put("heavyLaunch", heavyLaunchEnabled);
            if (target != null) object.put("target", target.toJson());
        } catch (Exception ignored) { }
        return object;
    }

    public static PressureTrigger fromJson(JSONObject object) {
        if (object == null) return null;
        int id = object.optInt("id", 1);
        int centerX = object.has("centerX")
                ? object.optInt("centerX", ConfigContract.DEFAULT_PRESSURE_CENTER_X_PERCENT)
                : object.optInt("center_x", ConfigContract.DEFAULT_PRESSURE_CENTER_X_PERCENT);
        int centerY = object.has("centerY")
                ? object.optInt("centerY", ConfigContract.DEFAULT_PRESSURE_CENTER_Y_PERCENT)
                : object.optInt("center_y", ConfigContract.DEFAULT_PRESSURE_CENTER_Y_PERCENT);
        int radius = object.has("radius")
                ? object.optInt("radius", ConfigContract.DEFAULT_PRESSURE_RADIUS_PERCENT)
                : object.optInt("radius_percent", ConfigContract.DEFAULT_PRESSURE_RADIUS_PERCENT);
        AppTarget target = AppTarget.fromJson(object.optJSONObject("target"));
        boolean openAsFreeform = object.optBoolean("openAsFreeform",
                ConfigContract.DEFAULT_PRESSURE_OPEN_AS_FREEFORM);
        boolean heavyLaunch = object.optBoolean("heavyLaunch",
                ConfigContract.DEFAULT_PRESSURE_HEAVY_LAUNCH_ENABLED);
        return new PressureTrigger(id, object.optBoolean("enabled", true), centerX, centerY,
                radius, object.optInt("action", ConfigContract.DEFAULT_PRESSURE_ACTION), target,
                openAsFreeform, heavyLaunch);
    }

    private static int supportedAction(int value) {
        return value >= ConfigContract.PRESSURE_ACTION_HONEYCOMB
                && value <= ConfigContract.PRESSURE_ACTION_SINGLE_TARGET
                ? value : ConfigContract.DEFAULT_PRESSURE_ACTION;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
