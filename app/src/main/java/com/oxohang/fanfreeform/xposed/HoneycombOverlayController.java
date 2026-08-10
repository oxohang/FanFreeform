package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.List;

final class HoneycombOverlayController {
    interface Listener {
        void onLaunch(RuntimeTarget target);
        void onClosed();
        default void onSelectionChanged(RuntimeTarget target) { }
    }

    private final Context context;
    private final Handler mainHandler;
    private final WindowManager windowManager;
    private final Object moveLock = new Object();
    private HoneycombOverlayView view;
    private boolean attached;
    private int windowTop;
    private HoneycombOverlayView pendingMoveView;
    private float pendingMoveX;
    private float pendingMoveY;
    private boolean moveFrameScheduled;
    private final Runnable deliverPendingMove = () -> {
        HoneycombOverlayView target;
        float x;
        float y;
        synchronized (moveLock) {
            target = pendingMoveView;
            x = pendingMoveX;
            y = pendingMoveY;
            pendingMoveView = null;
            moveFrameScheduled = false;
        }
        if (attached && target != null && view == target) {
            target.onExternalMove(toLocalX(x), toLocalY(y));
        }
    };

    HoneycombOverlayController(Context context, Handler mainHandler) {
        this.context = context;
        this.mainHandler = mainHandler;
        windowManager = context.getSystemService(WindowManager.class);
    }

    boolean show(List<RuntimeTarget> targets, GestureGeometry.Corner corner,
                 float anchorX, float anchorY, GestureConfig config, Listener listener) {
        return show(targets, corner, anchorX, anchorY, config, false, listener);
    }

    boolean show(List<RuntimeTarget> targets, GestureGeometry.Corner corner,
                 float anchorX, float anchorY, GestureConfig config,
                 boolean forceBrowseMode, Listener listener) {
        return show(targets, corner, anchorX, anchorY, config, forceBrowseMode,
                false, listener);
    }

    boolean show(List<RuntimeTarget> targets, GestureGeometry.Corner corner,
                 float anchorX, float anchorY, GestureConfig config,
                 boolean forceBrowseMode, boolean requireMoveBeforeSelection,
                 Listener listener) {
        removeNow();
        if (windowManager == null || targets.isEmpty()) return false;
        HoneycombOverlayView next = new HoneycombOverlayView(context);
        windowTop = 0;
        next.configure(targets, corner, toLocalX(anchorX), toLocalY(anchorY), config,
                forceBrowseMode,
                new HoneycombOverlayView.Listener() {
            @Override public void onLaunch(RuntimeTarget target) {
                removeNow();
                listener.onLaunch(target);
            }

            @Override public void onClosed() {
                removeNow();
                listener.onClosed();
            }

            @Override public void onSelectionChanged(RuntimeTarget target) {
                listener.onSelectionChanged(target);
            }
        });
        if (requireMoveBeforeSelection) {
            next.requireMoveBeforeSelection(toLocalX(anchorX), toLocalY(anchorY));
        }
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                displayHeight(),
                2038,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.y = windowTop;
        params.setFitInsetsTypes(0);
        params.setTitle("HyperGestureHoneycomb");
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams
                .LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        boolean blurRequested = config.honeycombLiveBlurEnabled
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
        if (blurRequested) {
            params.flags |= WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
            try {
                params.setBlurBehindRadius(Math.round(config.honeycombLiveBlurDp
                        * context.getResources().getDisplayMetrics().density));
            } catch (Throwable error) {
                params.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
                blurRequested = false;
                Log.e("Live blur is unavailable; using color fallback", error);
            }
        }
        try {
            try {
                windowManager.addView(next, params);
            } catch (Throwable blurError) {
                if (!blurRequested) throw blurError;
                Log.e("Cannot attach honeycomb with live blur; retrying", blurError);
                params.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
                windowManager.addView(next, params);
            }
            view = next;
            attached = true;
            if (config.honeycombAppBackgroundEnabled) {
                ForegroundAppBackgroundResolver.request(context, color -> next.post(() -> {
                    if (attached && view == next) next.setAppBackgroundColor(color);
                }));
            }
            next.playEntry();
            return true;
        } catch (Throwable error) {
            Log.e("Cannot attach honeycomb overlay", error);
            next.releaseResources();
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
        if (!attached || current == null) return;
        synchronized (moveLock) {
            pendingMoveView = current;
            pendingMoveX = x;
            pendingMoveY = y;
            if (moveFrameScheduled) return;
            moveFrameScheduled = true;
        }
        current.postOnAnimation(deliverPendingMove);
    }

    void externalUp(float x, float y, boolean cancelled) {
        HoneycombOverlayView current = view;
        cancelPendingMove(current);
        runOnViewThread(current, () -> {
            float localX = toLocalX(x);
            float localY = toLocalY(y);
            if (!cancelled) current.onExternalMove(localX, localY);
            current.onExternalUp(localX, localY, cancelled);
        });
    }

    void externalCancel() {
        HoneycombOverlayView current = view;
        cancelPendingMove(current);
        runOnViewThread(current, current::onExternalCancel);
    }

    void setPaused(boolean paused) {
        HoneycombOverlayView current = view;
        runOnViewThread(current, () -> current.setInteractionPaused(paused));
    }

    void dismiss() {
        HoneycombOverlayView current = view;
        runOnViewThread(current, current::playDismissal);
    }

    void removeNow() {
        HoneycombOverlayView current = view;
        cancelPendingMove(current);
        view = null;
        if (!attached || current == null || windowManager == null) {
            attached = false;
            return;
        }
        attached = false;
        windowTop = 0;
        mainHandler.removeCallbacksAndMessages(current);
        Runnable removal = () -> {
            try {
                windowManager.removeViewImmediate(current);
            } catch (Throwable error) {
                Log.e("Cannot remove honeycomb overlay", error);
            } finally {
                current.releaseResources();
            }
        };
        Handler owner = current.getHandler();
        if (owner != null && owner.getLooper() != Looper.myLooper()) owner.post(removal);
        else removal.run();
    }

    private void runOnViewThread(HoneycombOverlayView current, Runnable action) {
        if (!attached || current == null) return;
        Handler owner = current.getHandler();
        if (owner != null && owner.getLooper() != Looper.myLooper()) {
            owner.post(() -> {
                if (attached && view == current) action.run();
            });
        } else if (attached && view == current) {
            action.run();
        }
    }

    private void cancelPendingMove(HoneycombOverlayView current) {
        if (current != null) current.removeCallbacks(deliverPendingMove);
        synchronized (moveLock) {
            if (pendingMoveView == current) pendingMoveView = null;
            moveFrameScheduled = false;
        }
    }

    private float toLocalX(float screenX) { return screenX; }

    private float toLocalY(float screenY) { return screenY - windowTop; }
}
