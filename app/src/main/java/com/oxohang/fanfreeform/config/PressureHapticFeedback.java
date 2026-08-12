package com.oxohang.fanfreeform.config;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public final class PressureHapticFeedback {
    private PressureHapticFeedback() {}

    public static void vibrate(Context context) {
        Vibrator vibrator = vibrator(context);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        VibrationEffect effect = systemEffect(vibrator);
        vibrator.vibrate(effect);
    }

    private static VibrationEffect systemEffect(Vibrator vibrator) {
        int effectId = VibrationEffect.EFFECT_CLICK;
        int support = vibrator.areEffectsSupported(effectId)[0];
        if (support != Vibrator.VIBRATION_EFFECT_SUPPORT_NO) {
            return VibrationEffect.createPredefined(effectId);
        }
        // Some vendor HALs do not advertise predefined effects even though they
        // can still play a short one-shot pulse.
        return VibrationEffect.createOneShot(35L, 180);
    }

    private static Vibrator vibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = context.getSystemService(VibratorManager.class);
            return manager == null ? null : manager.getDefaultVibrator();
        }
        return context.getSystemService(Vibrator.class);
    }
}
