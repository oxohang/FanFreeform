package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import java.util.List;

final class HoneycombOverlayController {
    interface Listener {
        void onLaunch(RuntimeTarget target);
        void onClosed();
    }

    private final Context context;
    private final Handler mainHandler;
    private final WindowManager windowManager;
    private HoneycombOverlayView view;
    private boolean attached;
    private int windowTop;

    HoneycombOverlayController(Context context, Handler mainHandler) {
        this.context = context;
        this.mainHandler = mainHandler;
        windowManager = context.getSystemService(WindowManager.class);
    }

    boolean show(List<RuntimeTarget> targets, GestureGeometry.Corner corner,
                 float anchorX, float anchorY, GestureConfig config, Listener listener) {
        removeNow();
        if (windowManager == null || targets.isEmpty()) return false;
        HoneycombOverlayView next = new HoneycombOverlayView(context);
        windowTop = 0;
        next.configure(targets, corner, toLocalX(anchorX), toLocalY(anchorY), config,
                new HoneycombOverlayView.Listener() {
            @Override public void onLaunch(RuntimeTarget target) {
                removeNow();
                listener.onLaunch(target);
            }

            @Override public void onClosed() {
                removeNow();
                listener.onClosed();
            }
        });
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                displayHeight(),
                2038,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.y = windowTop;
        params.setFitInsetsTypes(0);
        params.setTitle("HyperGestureHoneycomb");
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams
                .LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        try {
            next.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            windowManager.addView(next, params);
            view = next;
            attached = true;
            next.post(() -> {
                try {
                    WindowInsetsController controller = next.getWindowInsetsController();
                    if (controller != null) {
                        controller.setSystemBarsBehavior(WindowInsetsController
                                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                        controller.hide(WindowInsets.Type.navigationBars());
                    }
                } catch (Throwable error) {
                    Log.e("Cannot hide navigation handle for honeycomb", error);
                }
            });
            next.playEntry();
            return true;
        } catch (Throwable error) {
            Log.e("Cannot attach honeycomb overlay", error);
            view = null;
            attached = false;
            return false;
        }
    }

    private int displayHeight() {
        return windowManager == null ? context.getResources().getDisplayMetrics().heightPixels
                : windowManager.getCurrentWindowMetrics().getBounds().height();
    }

    boolean isVisible() { return attached && view != null; }

    void externalMove(float x, float y) {
        HoneycombOverlayView current = view;
        if (attached && current != null) current.onExternalMove(toLocalX(x), toLocalY(y));
    }

    void externalUp(float x, float y, boolean cancelled) {
        HoneycombOverlayView current = view;
        if (attached && current != null) current.onExternalUp(
                toLocalX(x), toLocalY(y), cancelled);
    }

    void setPaused(boolean paused) {
        HoneycombOverlayView current = view;
        if (attached && current != null) current.setInteractionPaused(paused);
    }

    void dismiss() {
        HoneycombOverlayView current = view;
        if (attached && current != null) current.playDismissal();
    }

    void removeNow() {
        HoneycombOverlayView current = view;
        view = null;
        if (!attached || current == null || windowManager == null) {
            attached = false;
            return;
        }
        attached = false;
        windowTop = 0;
        mainHandler.removeCallbacksAndMessages(current);
        try {
            windowManager.removeViewImmediate(current);
        } catch (Throwable error) {
            Log.e("Cannot remove honeycomb overlay", error);
        }
    }

    private float toLocalX(float screenX) { return screenX; }

    private float toLocalY(float screenY) { return screenY - windowTop; }
}
