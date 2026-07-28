package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.List;

final class FanOverlayController {
    interface WheelListener {
        void onLaunch(int index);
        void onDismiss();
        void onSelectionChanged(int index);
    }

    private static final int TYPE_NAVIGATION_BAR_PANEL = 2024;
    private final Context context;
    private final Handler mainHandler;
    private final WindowManager windowManager;
    private View view;
    private boolean attached;
    private volatile boolean wheelVisible;

    FanOverlayController(Context context, Handler mainHandler) {
        this.context = context;
        this.mainHandler = mainHandler;
        this.windowManager = context.getSystemService(WindowManager.class);
    }

    void show(List<RuntimeTarget> targets, GestureGeometry.Corner corner,
              float radius, float iconDiameter, boolean showBackdrop) {
        showInternal(targets, corner, radius, iconDiameter, showBackdrop,
                false, 0f, 0f, 0f, false);
    }

    void showSideList(List<RuntimeTarget> targets, GestureGeometry.Corner side,
                      float centerX, float listTop, float rowHeight, float iconDiameter,
                      boolean showNames, boolean showBackdrop) {
        showInternal(targets, side, 0f, iconDiameter, showBackdrop,
                true, centerX, listTop, rowHeight, showNames);
    }

    private void showInternal(List<RuntimeTarget> targets, GestureGeometry.Corner corner,
                              float radius, float iconDiameter, boolean showBackdrop,
                              boolean sideListLayout, float centerX, float listTop,
                              float rowHeight,
                              boolean showNames) {
        runOnMain(() -> {
            removeNow();
            FanOverlayView fanView = new FanOverlayView(context);
            if (sideListLayout) {
                fanView.configureSideList(targets, corner, centerX, listTop, rowHeight,
                        iconDiameter, showNames, showBackdrop);
            } else {
                fanView.configure(targets, corner, radius, iconDiameter, showBackdrop);
            }
            view = fanView;
            view.setAlpha(0f);
            WindowManager.LayoutParams params = params(TYPE_NAVIGATION_BAR_PANEL, false);
            try {
                windowManager.addView(view, params);
            } catch (Throwable first) {
                try {
                    windowManager.addView(view, params(2038, false));
                } catch (Throwable second) {
                    Log.e("Cannot attach fan overlay", second);
                    view = null;
                    attached = false;
                    return;
                }
            }
            attached = true;
            wheelVisible = false;
            view.animate().alpha(1f).setDuration(110).start();
        });
    }

    void showWheel(List<RuntimeTarget> targets, GestureGeometry.Corner side,
                   float centerX, float centerY, float rowHeight, float iconDiameter,
                   int selected, boolean showNames, boolean showBackdrop,
                   WheelListener listener) {
        runOnMain(() -> {
            removeNow();
            SideWheelOverlayView wheel = new SideWheelOverlayView(context);
            wheel.configure(targets, side, centerX, centerY, rowHeight, iconDiameter,
                    selected, showNames, showBackdrop,
                    new SideWheelOverlayView.Listener() {
                        @Override public void onLaunch(int index) {
                            removeNow();
                            listener.onLaunch(index);
                        }

                        @Override public void onDismiss() {
                            removeNow();
                            listener.onDismiss();
                        }

                        @Override public void onSelectionChanged(int index) {
                            listener.onSelectionChanged(index);
                        }
                    });
            view = wheel;
            view.setAlpha(0f);
            WindowManager.LayoutParams params = params(TYPE_NAVIGATION_BAR_PANEL, true);
            try {
                windowManager.addView(view, params);
            } catch (Throwable first) {
                try {
                    windowManager.addView(view, params(2038, true));
                } catch (Throwable second) {
                    Log.e("Cannot attach side wheel overlay", second);
                    view = null;
                    attached = false;
                    wheelVisible = false;
                    listener.onDismiss();
                    return;
                }
            }
            attached = true;
            wheelVisible = true;
            view.animate().alpha(1f).setDuration(100).start();
        });
    }

    void update(int selected, float x, float y) {
        runOnMain(() -> {
            if (attached && view instanceof FanOverlayView) {
                ((FanOverlayView) view).updateSelection(selected, x, y);
            }
        });
    }

    /**
     * 播放底部扇形选中图标的启动动画，动画结束后移除 overlay 并回调。
     */
    void animateLaunch(int selected, Runnable onAnimationEnd) {
        runOnMain(() -> {
            if (attached && view instanceof FanOverlayView) {
                ((FanOverlayView) view).startLaunchAnimation(selected, () -> {
                    removeNow();
                    if (onAnimationEnd != null) onAnimationEnd.run();
                });
            } else {
                if (onAnimationEnd != null) onAnimationEnd.run();
            }
        });
    }

    void setOpacity(float opacity) {
        float safeOpacity = Math.max(0f, Math.min(1f, opacity));
        runOnMain(() -> {
            if (!attached || view == null || wheelVisible) return;
            view.animate().cancel();
            view.setAlpha(safeOpacity);
        });
    }

    boolean isWheelVisible() {
        return wheelVisible;
    }

    void hide() {
        runOnMain(() -> {
            if (!attached || view == null) return;
            View target = view;
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
        wheelVisible = false;
        view = null;
    }

    private WindowManager.LayoutParams params(int type, boolean touchable) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        if (!touchable) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                flags,
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
