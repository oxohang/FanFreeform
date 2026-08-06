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
              float radius, float iconDiameter, boolean showSelectedName,
              boolean showBackdrop,
              boolean animationsEnabled, int animationSpeed,
              int revealAmount, int rotationDegrees, int selectionScalePercent,
              boolean showSelectionRing, int layoutMode, int outerCapacity,
              int middleCapacity, int innerCapacity,
              boolean forceCircularIcons) {
        showInternal(targets, corner, radius, iconDiameter, showBackdrop,
                false, false, false, 0f, 0f, 0f, 0f, showSelectedName,
                animationsEnabled, animationSpeed, revealAmount, rotationDegrees,
                selectionScalePercent, showSelectionRing, layoutMode, outerCapacity,
                middleCapacity, innerCapacity,
                forceCircularIcons);
    }

    void showSideList(List<RuntimeTarget> targets, GestureGeometry.Corner side,
                      float centerX, float listTop, float rowHeight, float iconDiameter,
                      boolean showNames, boolean showBackdrop,
                      boolean animationsEnabled, int animationSpeed,
                      int revealAmount, int rotationDegrees, int selectionScalePercent,
                      boolean showSelectionRing, boolean forceCircularIcons) {
        showInternal(targets, side, 0f, iconDiameter, showBackdrop,
                true, false, false, centerX, listTop, rowHeight, 0f, showNames,
                animationsEnabled, animationSpeed, revealAmount, rotationDegrees,
                selectionScalePercent, showSelectionRing, 0, 7, 6, 5,
                forceCircularIcons);
    }

    void showSideFanList(List<RuntimeTarget> targets, GestureGeometry.Corner side,
                         float apexX, float centerY, float radius, float iconDiameter,
                         boolean showNames, boolean showBackdrop,
                         boolean animationsEnabled, int animationSpeed,
                         int revealAmount, int rotationDegrees, int selectionScalePercent,
                         boolean showSelectionRing, boolean forceCircularIcons) {
        showInternal(targets, side, 0f, iconDiameter, showBackdrop,
                true, true, false, apexX, centerY, 0f, radius, showNames,
                animationsEnabled, animationSpeed, revealAmount, rotationDegrees,
                selectionScalePercent, showSelectionRing, 0, 7, 6, 5,
                forceCircularIcons);
    }

    void showSideRingList(List<RuntimeTarget> targets, GestureGeometry.Corner side,
                          float centerX, float centerY, float radius, float iconDiameter,
                          boolean showNames, boolean showBackdrop,
                          boolean animationsEnabled, int animationSpeed,
                          int revealAmount, int rotationDegrees, int selectionScalePercent,
                          boolean showSelectionRing, boolean forceCircularIcons) {
        showInternal(targets, side, 0f, iconDiameter, showBackdrop,
                true, false, true, centerX, centerY, 0f, radius, showNames,
                animationsEnabled, animationSpeed, revealAmount, rotationDegrees,
                selectionScalePercent, showSelectionRing, 0, 7, 6, 5,
                forceCircularIcons);
    }

    void showTouchableSideRingList(List<RuntimeTarget> targets, GestureGeometry.Corner side,
                                   float centerX, float centerY, float radius,
                                   float iconDiameter, boolean showNames,
                                   boolean showBackdrop, boolean animationsEnabled,
                                   int animationSpeed, int revealAmount,
                                   int rotationDegrees, int selectionScalePercent,
                                   boolean showSelectionRing, boolean forceCircularIcons,
                                   WheelListener listener) {
        runOnMain(() -> {
            removeNow();
            FanOverlayView fanView = new FanOverlayView(context);
            fanView.configureSideRingList(targets, side, centerX, centerY, radius,
                    iconDiameter, showNames, showBackdrop, animationsEnabled,
                    animationSpeed, revealAmount, rotationDegrees,
                    selectionScalePercent, showSelectionRing);
            fanView.setForceCircularIcons(forceCircularIcons);
            fanView.setSelectionTransformLevel(0);
            fanView.setTouchListener(new FanOverlayView.TouchListener() {
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
            attachFanView(fanView, animationSpeed, animationsEnabled, true);
            wheelVisible = attached;
            if (!attached) listener.onDismiss();
        });
    }

    private void showInternal(List<RuntimeTarget> targets, GestureGeometry.Corner corner,
                              float radius, float iconDiameter, boolean showBackdrop,
                              boolean sideListLayout, boolean sideFanListLayout,
                              boolean sideRingListLayout,
                              float centerX, float listTop, float rowHeight,
                              float sideFanRadius,
                              boolean showNames, boolean animationsEnabled,
                              int animationSpeed, int revealAmount,
                              int rotationDegrees, int selectionScalePercent,
                              boolean showSelectionRing, int layoutMode, int outerCapacity,
                              int middleCapacity, int innerCapacity,
                              boolean forceCircularIcons) {
        runOnMain(() -> {
            removeNow();
            FanOverlayView fanView = new FanOverlayView(context);
            if (sideRingListLayout) {
                fanView.configureSideRingList(targets, corner, centerX, listTop,
                        sideFanRadius, iconDiameter, showNames, showBackdrop,
                        animationsEnabled, animationSpeed, revealAmount, rotationDegrees,
                        selectionScalePercent, showSelectionRing);
            } else if (sideFanListLayout) {
                fanView.configureSideFanList(targets, corner, centerX, listTop,
                        sideFanRadius, iconDiameter, showNames, showBackdrop,
                        animationsEnabled, animationSpeed, revealAmount, rotationDegrees,
                        selectionScalePercent,
                        showSelectionRing);
            } else if (sideListLayout) {
                fanView.configureSideList(targets, corner, centerX, listTop, rowHeight,
                        iconDiameter, showNames, showBackdrop, animationsEnabled,
                        animationSpeed, revealAmount, rotationDegrees,
                        selectionScalePercent, showSelectionRing);
            } else {
                fanView.configure(targets, corner, radius, iconDiameter, showNames,
                        showBackdrop,
                        animationsEnabled, animationSpeed, revealAmount,
                        rotationDegrees, selectionScalePercent, showSelectionRing,
                        layoutMode, outerCapacity, middleCapacity, innerCapacity);
            }
            fanView.setForceCircularIcons(forceCircularIcons);
            fanView.setSelectionTransformLevel(0);
            attachFanView(fanView, animationSpeed, animationsEnabled, false);
        });
    }

    private void attachFanView(FanOverlayView fanView, int animationSpeed,
                               boolean animationsEnabled, boolean touchable) {
            view = fanView;
            view.setAlpha(0f);
            WindowManager.LayoutParams params = params(TYPE_NAVIGATION_BAR_PANEL, touchable);
            try {
                windowManager.addView(view, params);
            } catch (Throwable first) {
                try {
                    windowManager.addView(view, params(2038, touchable));
                } catch (Throwable second) {
                    Log.e("Cannot attach fan overlay", second);
                    view = null;
                    attached = false;
                    return;
                }
            }
            attached = true;
            wheelVisible = false;
            view.animate().alpha(1f).setDuration(animationDuration(110, animationSpeed,
                    animationsEnabled)).start();
    }

    void showWheel(List<RuntimeTarget> targets, GestureGeometry.Corner side,
                   float centerX, float centerY, float rowHeight, float iconDiameter,
                   int selected, boolean showNames, boolean showBackdrop,
                   boolean forceCircularIcons,
                   WheelListener listener) {
        runOnMain(() -> {
            removeNow();
            SideWheelOverlayView wheel = new SideWheelOverlayView(context);
            wheel.setForceCircularIcons(forceCircularIcons);
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

    void setSelectionTransformLevel(int level) {
        runOnMain(() -> {
            if (attached && view instanceof FanOverlayView) {
                ((FanOverlayView) view).setSelectionTransformLevel(level);
            }
        });
    }

    void confirmAndHide(int selected) {
        runOnMain(() -> {
            if (!attached || !(view instanceof FanOverlayView)) return;
            FanOverlayView fanView = (FanOverlayView) view;
            fanView.playConfirmation(selected);
            View target = view;
            mainHandler.postDelayed(() -> {
                if (view == target) removeNow();
            }, fanView.confirmationDurationMs());
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
            long duration = 90L;
            if (target instanceof FanOverlayView
                    && ((FanOverlayView) target).playDismissal()) {
                duration = ((FanOverlayView) target).dismissalDurationMs();
            }
            target.animate().alpha(0f).setDuration(duration).withEndAction(() -> {
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

    private static long animationDuration(long base, int speed, boolean enabled) {
        if (!enabled) return 1L;
        return Math.max(45L, Math.round(base * 100f / Math.max(1, speed)));
    }
}
