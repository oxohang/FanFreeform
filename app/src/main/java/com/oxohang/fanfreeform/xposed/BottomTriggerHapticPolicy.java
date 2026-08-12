package com.oxohang.fanfreeform.xposed;

final class BottomTriggerHapticPolicy {
    private BottomTriggerHapticPolicy() { }

    static boolean shouldVibrate(boolean globalHaptic, boolean bottomTriggerHaptic) {
        return globalHaptic && bottomTriggerHaptic;
    }
}
