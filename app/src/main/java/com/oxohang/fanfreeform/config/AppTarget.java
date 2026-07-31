package com.oxohang.fanfreeform.config;

import android.content.ComponentName;

import org.json.JSONObject;

import java.util.Objects;

public final class AppTarget {
    public final String component;
    public final String shortcutPackage;
    public final String shortcutId;
    public final String shortcutLabel;
    public final String shortcutIntentUri;
    public final int userId;

    public AppTarget(String component) {
        this(component, 0);
    }

    public AppTarget(String component, int userId) {
        this.component = component;
        this.shortcutPackage = "";
        this.shortcutId = "";
        this.shortcutLabel = "";
        this.shortcutIntentUri = "";
        this.userId = Math.max(0, userId);
    }

    private AppTarget(String shortcutPackage, String shortcutId, String shortcutLabel,
                      String shortcutIntentUri, int userId) {
        this.component = "";
        this.shortcutPackage = shortcutPackage == null ? "" : shortcutPackage;
        this.shortcutId = shortcutId == null ? "" : shortcutId;
        this.shortcutLabel = shortcutLabel == null ? "" : shortcutLabel;
        this.shortcutIntentUri = shortcutIntentUri == null ? "" : shortcutIntentUri;
        this.userId = Math.max(0, userId);
    }

    public static AppTarget shortcut(String packageName, String shortcutId, String label) {
        return new AppTarget(packageName, shortcutId, label, "", 0);
    }

    public static AppTarget launcherShortcut(String packageName, String shortcutId,
                                             String label, String intentUri, int userId) {
        return new AppTarget(packageName, shortcutId, label, intentUri, userId);
    }

    public boolean isShortcut() {
        return !shortcutPackage.isEmpty() && !shortcutId.isEmpty();
    }

    public boolean isLauncherShortcut() {
        return isShortcut() && !shortcutIntentUri.isEmpty();
    }

    public ComponentName componentName() {
        if (isShortcut()) return null;
        return ComponentName.unflattenFromString(component);
    }

    public String packageName() {
        if (isShortcut()) return shortcutPackage;
        ComponentName name = componentName();
        return name == null ? "" : name.getPackageName();
    }

    public JSONObject toJson() {
        JSONObject out = new JSONObject();
        try {
            if (isShortcut()) {
                out.put("type", "shortcut");
                out.put("package", shortcutPackage);
                out.put("id", shortcutId);
                out.put("label", shortcutLabel);
                if (isLauncherShortcut()) {
                    out.put("type", "launcher_shortcut");
                    out.put("intent", shortcutIntentUri);
                    out.put("userId", userId);
                }
            } else {
                out.put("type", "activity");
                out.put("component", component);
                out.put("userId", userId);
            }
        } catch (Exception ignored) { }
        return out;
    }

    public static AppTarget fromJson(JSONObject value) {
        if (value == null) return null;
        String type = value.optString("type");
        if ("shortcut".equals(type) || "launcher_shortcut".equals(type)) {
            AppTarget target = "launcher_shortcut".equals(type)
                    ? launcherShortcut(value.optString("package"), value.optString("id"),
                    value.optString("label"), value.optString("intent"),
                    value.optInt("userId", 0))
                    : shortcut(value.optString("package"), value.optString("id"),
                    value.optString("label"));
            return target.isShortcut() ? target : null;
        }
        AppTarget target = new AppTarget(value.optString("component"),
                value.optInt("userId", 0));
        return target.componentName() == null ? null : target;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AppTarget)) return false;
        AppTarget target = (AppTarget) other;
        return Objects.equals(component, target.component)
                && Objects.equals(shortcutPackage, target.shortcutPackage)
                && Objects.equals(shortcutId, target.shortcutId)
                && Objects.equals(shortcutIntentUri, target.shortcutIntentUri)
                && userId == target.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(component, shortcutPackage, shortcutId, shortcutIntentUri, userId);
    }
}
