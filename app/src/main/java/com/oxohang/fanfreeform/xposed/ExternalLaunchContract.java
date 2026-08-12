package com.oxohang.fanfreeform.xposed;

/** IPC contract used by companion launchers such as fly. */
final class ExternalLaunchContract {
    static final String ACTION_LAUNCH =
            "com.oxohang.fanfreeform.action.LAUNCH";
    static final String EXTRA_PACKAGE = "package_name";
    static final String EXTRA_COMPONENT = "component_name";
    static final String EXTRA_INTENT_URI = "intent_uri";
    static final String EXTRA_SOURCE_PACKAGE = "source_package";
    static final String FLY_PACKAGE = "com.fly.MZ";

    private ExternalLaunchContract() { }
}
