package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import java.util.List;

final class TaskSwitcherOverlayController {
    interface Listener {
        void onLaunch(RecentTaskPreview task);
        void onClosed();
    }

    private final Context context;
    private final Handler mainHandler;
    private final WindowManager windowManager;
    private final Object moveLock = new Object();
    private TaskSwitcherOverlayView view;
    private boolean attached;
    private TaskSwitcherOverlayView pendingMoveView;
    private float pendingMoveX;
    private float pendingMoveY;
    private boolean moveFrameScheduled;
    private final Runnable deliverPendingMove = () -> {
        TaskSwitcherOverlayView target;
        float x;
        float y;
        synchronized (moveLock) {
            target = pendingMoveView;
            x = pendingMoveX;
            y = pendingMoveY;
            pendingMoveView = null;
            moveFrameScheduled = false;
        }
        if (attached && target != null && view == target) target.onExternalMove(x, y);
    };

    TaskSwitcherOverlayController(Context context, Handler mainHandler) {
        this.context = context;
        this.mainHandler = mainHandler;
        windowManager = context.getSystemService(WindowManager.class);
    }

    boolean show(List<RecentTaskPreview> tasks, GestureGeometry.Corner corner,
                 float anchorX, float anchorY,
                 GestureConfig config, Listener listener) {
        removeNow();
        if (windowManager == null || tasks == null || tasks.isEmpty()) return false;
        TaskSwitcherOverlayView next = new TaskSwitcherOverlayView(context);
        next.configure(tasks, corner, anchorX, anchorY, config,
                new TaskSwitcherOverlayView.Listener() {
            @Override public void onLaunch(RecentTaskPreview task) {
                removeNow();
                listener.onLaunch(task);
            }

            @Override public void onClosed() {
                removeNow();
                listener.onClosed();
            }
        });
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                windowManager.getCurrentWindowMetrics().getBounds().height(),
                2038,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.setFitInsetsTypes(0);
        params.setTitle("HyperGestureTaskSwitcher");
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
                    Log.e("Cannot hide navigation handle for task switcher", error);
                }
            });
            next.playEntry();
            return true;
        } catch (Throwable error) {
            Log.e("Cannot attach task switcher", error);
            next.releaseResources();
            view = null;
            attached = false;
            return false;
        }
    }

    boolean isVisible() { return attached && view != null; }

    void externalMove(float x, float y) {
        TaskSwitcherOverlayView current = view;
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
        TaskSwitcherOverlayView current = view;
        cancelPendingMove(current);
        runOnViewThread(current, () -> current.onExternalUp(x, y, cancelled));
    }

    void externalCancel() {
        TaskSwitcherOverlayView current = view;
        cancelPendingMove(current);
        runOnViewThread(current, current::onExternalCancel);
    }

    void dismiss() {
        TaskSwitcherOverlayView current = view;
        runOnViewThread(current, current::playDismissal);
    }

    void removeNow() {
        TaskSwitcherOverlayView current = view;
        cancelPendingMove(current);
        view = null;
        if (!attached || current == null || windowManager == null) {
            attached = false;
            return;
        }
        attached = false;
        mainHandler.removeCallbacksAndMessages(current);
        Runnable removal = () -> {
            try {
                windowManager.removeViewImmediate(current);
            } catch (Throwable error) {
                Log.e("Cannot remove task switcher", error);
            } finally {
                current.releaseResources();
            }
        };
        Handler owner = current.getHandler();
        if (owner != null && owner.getLooper() != Looper.myLooper()) owner.post(removal);
        else removal.run();
    }

    private void runOnViewThread(TaskSwitcherOverlayView current, Runnable action) {
        if (!attached || current == null) return;
        Handler owner = current.getHandler();
        if (owner != null && owner.getLooper() != Looper.myLooper()) {
            owner.post(() -> {
                if (attached && view == current) action.run();
            });
        } else if (attached && view == current) action.run();
    }

    private void cancelPendingMove(TaskSwitcherOverlayView current) {
        if (current != null) current.removeCallbacks(deliverPendingMove);
        synchronized (moveLock) {
            if (pendingMoveView == current) pendingMoveView = null;
            moveFrameScheduled = false;
        }
    }
}
