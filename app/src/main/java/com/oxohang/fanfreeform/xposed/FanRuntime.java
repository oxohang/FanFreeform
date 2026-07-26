package com.oxohang.fanfreeform.xposed;

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.oxohang.fanfreeform.config.ConfigContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

final class FanRuntime {
    private enum State { IDLE, ARMED, ACTIVE }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final FanOverlayController overlay;
    private final HyperOsFreeformBridge freeform;
    private final float density;
    private volatile GestureConfig config = GestureConfig.defaults();
    private volatile List<RuntimeTarget> targets = Collections.emptyList();
    private State state = State.IDLE;
    private GestureGeometry.Corner corner;
    private float downX;
    private float downY;
    private int selected = -1;
    private boolean alreadyPilfered;

    FanRuntime(Context context, ClassLoader classLoader) {
        this.context = context;
        density = context.getResources().getDisplayMetrics().density;
        overlay = new FanOverlayController(context, mainHandler);
        freeform = new HyperOsFreeformBridge(context, classLoader, mainHandler);
        reloadConfig();
        context.getContentResolver().registerContentObserver(ConfigContract.URI, false,
                new ContentObserver(mainHandler) {
                    @Override public void onChange(boolean selfChange) { reloadConfig(); }
                });
    }

    void setFreeformController(Object controller) {
        freeform.setController(controller);
        reportStatus("HyperOS 3 原生接口已连接");
    }

    void reportInputReady() {
        reportStatus(freeform.hasController() ? "HyperOS 3 原生接口已连接" : "手势接口已连接，等待小窗控制器");
    }

    void onTaskAppeared(int taskId) {
        freeform.onTaskAppeared(taskId);
    }

    void onTaskInfo(Object info) {
        freeform.onTaskInfo(info);
    }

    void onTaskVanished(int taskId) {
        freeform.onTaskVanished(taskId);
    }

    void onMotion(MotionEvent event, Object inputMonitor) {
        try {
            handleMotion(event, inputMonitor);
        } catch (Throwable error) {
            reset();
            Log.e("Gesture event failed safely", error);
        }
    }

    private void handleMotion(MotionEvent event, Object inputMonitor) {
        int action = event.getActionMasked();
        int width = displayBounds().width();
        int height = displayBounds().height();
        float x = event.getX();
        float y = event.getY();

        if (action == MotionEvent.ACTION_DOWN) {
            reset();
            GestureGeometry.Corner downCorner = GestureGeometry.cornerAt(x, y, width, height, 58 * density);
            Rect tracked = freeform.trackedBounds();
            if (tracked != null) {
                if (tracked.contains((int) x, (int) y)) return;
                pilfer(inputMonitor);
                freeform.dismissTracked();
                if (downCorner != null && canStart()) {
                    arm(downCorner, x, y, true);
                }
                return;
            }
            if (downCorner != null && canStart()) arm(downCorner, x, y, false);
            return;
        }

        if (action == MotionEvent.ACTION_POINTER_DOWN || event.getPointerCount() > 1) {
            reset();
            return;
        }

        if (state == State.ARMED && action == MotionEvent.ACTION_MOVE) {
            float distance = GestureGeometry.distance(downX, downY, x, y);
            float threshold = Math.min(width, height) * config.triggerPercent / 100f;
            if (!GestureGeometry.movesInward(corner, downX, downY, x, y)) {
                if (distance > 32 * density) reset();
                return;
            }
            if (distance >= threshold) {
                if (!alreadyPilfered) pilfer(inputMonitor);
                state = State.ACTIVE;
                overlay.show(targets, corner);
                if (config.haptic) vibrateTick();
                updateSelection(x, y, width, height, threshold * 0.8f);
            }
            return;
        }

        if (state == State.ACTIVE && action == MotionEvent.ACTION_MOVE) {
            float threshold = Math.min(width, height) * config.triggerPercent / 100f;
            updateSelection(x, y, width, height, threshold * 0.8f);
            return;
        }

        if (action == MotionEvent.ACTION_UP) {
            if (state == State.ACTIVE && selected >= 0 && selected < targets.size()) {
                RuntimeTarget target = targets.get(selected);
                freeform.launch(target, config);
            }
            reset();
        } else if (action == MotionEvent.ACTION_CANCEL) {
            reset();
        }
    }

    private void arm(GestureGeometry.Corner corner, float x, float y, boolean alreadyPilfered) {
        state = State.ARMED;
        this.corner = corner;
        downX = x;
        downY = y;
        this.alreadyPilfered = alreadyPilfered;
    }

    private void updateSelection(float x, float y, int width, int height, float minimumRadius) {
        selected = GestureGeometry.selection(corner, x, y, width, height, targets.size(), minimumRadius);
        overlay.update(selected, x, y);
    }

    private void reset() {
        if (state == State.ACTIVE) overlay.hide();
        state = State.IDLE;
        corner = null;
        selected = -1;
        alreadyPilfered = false;
    }

    private boolean canStart() {
        if (!config.ready() || targets.size() < 3
                || (context.getDisplay() != null && context.getDisplay().getDisplayId() != 0)) return false;
        PowerManager power = context.getSystemService(PowerManager.class);
        KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
        return (power == null || power.isInteractive()) && (keyguard == null || !keyguard.isKeyguardLocked());
    }

    private Rect displayBounds() {
        WindowManager windowManager = context.getSystemService(WindowManager.class);
        return new Rect(windowManager.getCurrentWindowMetrics().getBounds());
    }

    private void pilfer(Object inputMonitor) {
        if (inputMonitor == null) return;
        try {
            XposedHelpers.callMethod(inputMonitor, "pilferPointers");
        } catch (Throwable error) {
            Log.e("Cannot pilfer input pointers", error);
        }
    }

    private void vibrateTick() {
        try {
            VibrationEffect effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK);
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                VibratorManager manager = context.getSystemService(VibratorManager.class);
                if (manager != null) manager.getDefaultVibrator().vibrate(effect);
            } else {
                Vibrator vibrator = context.getSystemService(Vibrator.class);
                if (vibrator != null) vibrator.vibrate(effect);
            }
        } catch (Throwable error) {
            Log.e("Haptic feedback failed", error);
        }
    }

    private void reloadConfig() {
        try {
            Bundle bundle = context.getContentResolver().call(ConfigContract.URI, "get", null, null);
            GestureConfig next = GestureConfig.from(bundle);
            ArrayList<RuntimeTarget> resolved = new ArrayList<>();
            PackageManager packageManager = context.getPackageManager();
            for (ComponentName component : next.components) {
                try {
                    ActivityInfo info = packageManager.getActivityInfo(component, 0);
                    CharSequence label = info.loadLabel(packageManager);
                    resolved.add(new RuntimeTarget(component,
                            label == null ? component.getPackageName() : label.toString(),
                            info.loadIcon(packageManager)));
                } catch (Throwable error) {
                    Log.e("Configured activity is unavailable: " + component.flattenToShortString(), error);
                }
            }
            config = next;
            targets = Collections.unmodifiableList(resolved);
            if (!next.enabled || resolved.size() < 3) reset();
            Log.i("Configuration loaded apps=" + resolved.size() + " trigger=" + next.triggerPercent + "%");
        } catch (Throwable error) {
            config = GestureConfig.defaults();
            targets = Collections.emptyList();
            reset();
            Log.e("Cannot read module configuration", error);
        }
    }

    private void reportStatus(String value) {
        try {
            Bundle extras = new Bundle();
            extras.putString(ConfigContract.KEY_INTERFACE_STATUS, value);
            context.getContentResolver().call(ConfigContract.URI, "report", null, extras);
        } catch (Throwable error) {
            Log.e("Cannot report interface status", error);
        }
    }
}
