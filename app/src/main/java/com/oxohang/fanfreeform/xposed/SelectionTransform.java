package com.oxohang.fanfreeform.xposed;

import com.oxohang.fanfreeform.config.ConfigContract;

final class SelectionTransform {
    private SelectionTransform() { }

    static float rotation(int level, int direction, float pulse) {
        float amount = amount(level);
        return direction * amount * (2.2f + 2.4f * pulse);
    }

    static float scaleX(int level, float pulse) {
        float amount = amount(level);
        return 1f + amount * (0.018f + 0.022f * pulse);
    }

    static float scaleY(int level, float pulse) {
        float amount = amount(level);
        return 1f - amount * (0.010f + 0.012f * pulse);
    }

    private static float amount(int level) {
        if (level <= ConfigContract.SELECTION_TRANSFORM_OFF) return 0f;
        if (level == ConfigContract.SELECTION_TRANSFORM_SUBTLE) return 0.55f;
        if (level == ConfigContract.SELECTION_TRANSFORM_STANDARD) return 1f;
        return 1.45f;
    }
}
