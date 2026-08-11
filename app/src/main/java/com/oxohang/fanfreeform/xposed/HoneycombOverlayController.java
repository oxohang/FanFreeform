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
    private HoneycombOverlayContainer windowView;
    private WindowManager.LayoutParams windowParams;
    private boolean liveBlurRequested;
    private boolean liveBlurApplied;
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
        HoneycombOverlayContainer next = new HoneycombOverlayContainer(context);
        HoneycombOverlayView foreground = next.foreground();
        windowTop = 0;
        next.configureBackground(config);
        foreground.configure(targets, corner, toLocalX(anchorX), toLocalY(anchorY), config,
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

            @Override public void onInteractionChanged(boolean active) {
                setLiveBlurSuppressed(active);
            }
        });
        if (requireMoveBeforeSelection) {
            foreground.requireMoveBeforeSelection(toLocalX(anchorX), toLocalY(anchorY));
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
        try {
            float refreshRate = windowManager.getDefaultDisplay().getRefreshRate();
            if (refreshRate > 0f) params.preferredRefreshRate = refreshRate;
        } catch (Throwable ignored) { }
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
            view = foreground;
            windowView = next;
            windowParams = params;
            liveBlurRequested = blurRequested;
            liveBlurApplied = blurRequested;
            attached = true;
            if (config.honeycombAppBackgroundEnabled) {
                ForegroundAppBackgroundResolver.request(context, color -> next.post(() -> {
                    if (attached && windowView == next) next.setAppBackgroundColor(color);
                }));
            }
            next.playEntry();
            return true;
        } catch (Throwable error) {
            Log.e("Cannot attach honeycomb overlay", error);
            next.releaseResources();
            view = null;
            windowView = null;
            windowParams = null;
            liveBlurRequested = false;
            liveBlurApplied = false;
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
        boolean deliverImmediately = false;
        float immediateX = x;
        float immediateY = y;
        synchronized (moveLock) {
            pendingMoveView = current;
            pendingMoveX = x;
            pendingMoveY = y;
            if (moveFrameScheduled) return;
            moveFrameScheduled = true;
            // The first sample of a gesture is already on the view's owner thread in
            // the normal SystemUI path. Apply that sample now to avoid adding a full
            // frame of startup latency, then keep the frame gate closed so later MOVE
            // events are still collapsed to the latest position once per frame.
            Handler owner = current.getHandler();
            if (owner != null && owner.getLooper() == Looper.myLooper()) {
                pendingMoveView = null;
                deliverImmediately = true;
            }
        }
        if (deliverImmediately) {
            current.onExternalMove(toLocalX(immediateX), toLocalY(immediateY));
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

    private void setLiveBlurSuppressed(boolean suppressed) {
        if (!attached || !liveBlurRequested || windowView == null || windowParams == null) return;
        boolean shouldApply = !suppressed;
        if (liveBlurApplied == shouldApply) return;
        if (shouldApply) windowParams.flags |= WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        else windowParams.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        try {
            windowManager.updateViewLayout(windowView, windowParams);
            liveBlurApplied = shouldApply;
        } catch (Throwable error) {
            Log.e("Cannot toggle honeycomb live blur during interaction", error);
        }
    }

    void removeNow() {
        HoneycombOverlayView current = view;
        HoneycombOverlayContainer currentWindow = windowView;
        cancelPendingMove(current);
        view = null;
        windowView = null;
        windowParams = null;
        liveBlurRequested = false;
        liveBlurApplied = false;
        if (!attached || current == null || currentWindow == null || windowManager == null) {
            attached = false;
            return;
        }
        attached = false;
        windowTop = 0;
        mainHandler.removeCallbacksAndMessages(current);
        Runnable removal = () -> {
            try {
                windowManager.removeViewImmediate(currentWindow);
            } catch (Throwable error) {
                Log.e("Cannot remove honeycomb overlay", error);
            } finally {
                currentWindow.releaseResources();
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
