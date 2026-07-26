package com.oxohang.fanfreeform.xposed;

import android.content.ComponentName;
import android.os.Bundle;

import com.oxohang.fanfreeform.config.ConfigContract;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class GestureConfig {
    final boolean enabled;
    final boolean haptic;
    final int triggerPercent;
    final int widthPercent;
    final int heightPercent;
    final int positionX;
    final int positionY;
    final List<ComponentName> components;

    private GestureConfig(boolean enabled, boolean haptic, int triggerPercent,
                          int widthPercent, int heightPercent, int positionX,
                          int positionY, List<ComponentName> components) {
        this.enabled = enabled;
        this.haptic = haptic;
        this.triggerPercent = clamp(triggerPercent, 6, 24);
        this.widthPercent = clamp(widthPercent, 40, 90);
        this.heightPercent = clamp(heightPercent, 35, 85);
        this.positionX = clamp(positionX, 0, 100);
        this.positionY = clamp(positionY, 0, 100);
        this.components = Collections.unmodifiableList(components);
    }

    static GestureConfig defaults() {
        return new GestureConfig(true, true, ConfigContract.DEFAULT_TRIGGER_PERCENT,
                ConfigContract.DEFAULT_WIDTH_PERCENT, ConfigContract.DEFAULT_HEIGHT_PERCENT,
                ConfigContract.DEFAULT_POSITION_X, ConfigContract.DEFAULT_POSITION_Y,
                new ArrayList<>());
    }

    static GestureConfig from(Bundle bundle) {
        if (bundle == null) return defaults();
        ArrayList<ComponentName> components = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(bundle.getString(ConfigContract.KEY_COMPONENTS, "[]"));
            for (int i = 0; i < Math.min(8, array.length()); i++) {
                ComponentName component = ComponentName.unflattenFromString(array.optString(i));
                if (component != null && !components.contains(component)) components.add(component);
            }
        } catch (Exception ignored) {}
        return new GestureConfig(
                bundle.getBoolean(ConfigContract.KEY_ENABLED, ConfigContract.DEFAULT_ENABLED),
                bundle.getBoolean(ConfigContract.KEY_HAPTIC, ConfigContract.DEFAULT_HAPTIC),
                bundle.getInt(ConfigContract.KEY_TRIGGER_PERCENT, ConfigContract.DEFAULT_TRIGGER_PERCENT),
                bundle.getInt(ConfigContract.KEY_WIDTH_PERCENT, ConfigContract.DEFAULT_WIDTH_PERCENT),
                bundle.getInt(ConfigContract.KEY_HEIGHT_PERCENT, ConfigContract.DEFAULT_HEIGHT_PERCENT),
                bundle.getInt(ConfigContract.KEY_POSITION_X, ConfigContract.DEFAULT_POSITION_X),
                bundle.getInt(ConfigContract.KEY_POSITION_Y, ConfigContract.DEFAULT_POSITION_Y),
                components);
    }

    boolean ready() {
        return enabled && components.size() >= 3;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

