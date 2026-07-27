package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.List;

final class FanOverlayController {
    private static final int TYPE_NAVIGATION_BAR_PANEL = 2024;
    private final Context context;
    private final Handler mainHandler;
    private final WindowManager windowManager;
    private FanOverlayView view;
    private boolean attached;

    FanOverlayController(Context context, Handler mainHandler) {
        this.context = context;
        this.mainHandler = mainHandler;
        this.windowManager = context.getSystemService(WindowManager.class);
    }

    void show(List<RuntimeTarget> targets, GestureGeometry.Corner corner,
              float radius, float iconDiameter, boolean showBackdrop) {
        showInternal(targets, corner, radius, iconDiameter, showBackdrop, false, 0f);
    }

    void showSide(List<RuntimeTarget> targets, GestureGeometry.Corner side,
                  float originY, float radius, float iconDiameter, boolean showBackdrop) {
        showInternal(targets, side, radius, iconDiameter, showBackdrop, true, originY);
    }

    private void showInternal(List<RuntimeTarget> targets, GestureGeometry.Corner corner,
                              float radius, float iconDiameter, boolean showBackdrop,
                              boolean sideLayout, float originY) {
        runOnMain(() -> {
            removeNow();
            view = new FanOverlayView(context);
            if (sideLayout) {
                view.configureSide(targets, corner, originY, radius,
                        iconDiameter, showBackdrop);
            } else {
                view.configure(targets, corner, radius, iconDiameter, showBackdrop);
            }
            view.setAlpha(0f);
            WindowManager.LayoutParams params = params(TYPE_NAVIGATION_BAR_PANEL);
            try {
                windowManager.addView(view, params);
            } catch (Throwable first) {
                try {
                    windowManager.addView(view, params(2038));
                } catch (Throwable second) {
                    Log.e("Cannot attach fan overlay", second);
                    view = null;
                    attached = false;
                    return;
                }
            }
            attached = true;
            view.animate().alpha(1f).setDuration(110).start();
        });
    }

    void update(int selected, float x, float y) {
        runOnMain(() -> {
            if (attached && view != null) view.updateSelection(selected, x, y);
        });
    }

    void hide() {
        runOnMain(() -> {
            if (!attached || view == null) return;
            FanOverlayView target = view;
            target.animate().alpha(0f).setDuration(90).withEndAction(() -> {
                if (view == target) removeNow();
            }).start();
        });
    }

    void removeNow() {
        if (attached && view != null) {
            try {
                view.animate().cancel();
                windowManager.removeViewImmediate(view);
            } catch (Throwable error) {
                Log.e("Cannot remove fan overlay", error);
            }
        }
        attached = false;
        view = null;
    }

    private WindowManager.LayoutParams params(int type) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.setTitle("FanFreeformOverlay");
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        return params;
    }

    private void runOnMain(Runnable runnable) {
        if (mainHandler.getLooper().isCurrentThread()) runnable.run();
        else mainHandler.post(runnable);
    }
}
