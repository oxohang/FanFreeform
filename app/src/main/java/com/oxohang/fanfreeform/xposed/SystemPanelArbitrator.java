package com.oxohang.fanfreeform.xposed;

final class SystemPanelArbitrator {
    private SystemPanelArbitrator() { }

    static boolean acceptShadeExpansion(boolean alreadyExpanded, long now,
                                        long trustedTouchUntil) {
        return alreadyExpanded || now < trustedTouchUntil;
    }
}
