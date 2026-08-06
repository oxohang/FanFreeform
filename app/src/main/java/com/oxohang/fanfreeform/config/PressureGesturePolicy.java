package com.oxohang.fanfreeform.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure geometry and calibration rules shared by the settings page and SystemUI hook. */
public final class PressureGesturePolicy {
    private PressureGesturePolicy() {}

    public static boolean isInsideCircle(float x, float y, float width, float height,
                                         int centerXPercent, int centerYPercent,
                                         int radiusPercent) {
        if (!(width > 0f) || !(height > 0f) || !Float.isFinite(x) || !Float.isFinite(y)) {
            return false;
        }
        float centerX = width * clamp(centerXPercent, 0, 100) / 100f;
        float centerY = height * clamp(centerYPercent, 0, 100) / 100f;
        float radius = Math.min(width, height)
                * clamp(radiusPercent, ConfigContract.MIN_PRESSURE_RADIUS_PERCENT,
                ConfigContract.MAX_PRESSURE_RADIUS_PERCENT) / 100f;
        float dx = x - centerX;
        float dy = y - centerY;
        return dx * dx + dy * dy <= radius * radius;
    }

    public static float positiveDelta(float baseline, float sample) {
        if (!Float.isFinite(baseline) || !Float.isFinite(sample)) return 0f;
        float delta = sample - baseline;
        return delta > 0f ? delta : 0f;
    }

    public static CalibrationResult calibrate(List<Float> rawDeltas) {
        ArrayList<Float> valid = new ArrayList<>();
        if (rawDeltas != null) {
            for (Float raw : rawDeltas) {
                if (raw != null && Float.isFinite(raw) && raw > 0f) valid.add(raw);
            }
        }
        if (valid.size() < 3) return CalibrationResult.invalid(valid.size());
        Collections.sort(valid);
        int middle = valid.size() / 2;
        float median = (valid.size() % 2 == 0)
                ? (valid.get(middle - 1) + valid.get(middle)) / 2f
                : valid.get(middle);
        float threshold = (valid.get(0) + median) / 2f;
        if (!Float.isFinite(threshold) || threshold <= 0f) {
            return CalibrationResult.invalid(valid.size());
        }
        return new CalibrationResult(true, threshold, valid.size());
    }

    public static final class CalibrationResult {
        public final boolean valid;
        public final float threshold;
        public final int validCount;

        private CalibrationResult(boolean valid, float threshold, int validCount) {
            this.valid = valid;
            this.threshold = threshold;
            this.validCount = validCount;
        }

        static CalibrationResult invalid(int validCount) {
            return new CalibrationResult(false, 0f, validCount);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
