package com.oxohang.fanfreeform.config;

import android.content.ComponentName;

import java.util.Objects;

public final class AppTarget {
    public final String component;
    public final boolean isShortcut;

    public AppTarget(String component) {
        this(component, false);
    }

    public AppTarget(String component, boolean isShortcut) {
        this.component = component;
        this.isShortcut = isShortcut;
    }

    public ComponentName componentName() {
        return ComponentName.unflattenFromString(component);
    }

    public String packageName() {
        ComponentName name = componentName();
        return name == null ? "" : name.getPackageName();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AppTarget && Objects.equals(component, ((AppTarget) other).component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(component);
    }
}

