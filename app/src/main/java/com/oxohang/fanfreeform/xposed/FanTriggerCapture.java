package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.robv.android.xposed.XposedHelpers;

final class FanTriggerCapture {
    private static final int TYPE_NAVIGATION_BAR_PANEL = 2024;
    private static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT = 1;
    private static final long TAP_TARGET_SETTLE_MS = 16L;
    private static final long TAP_DURATION_MS = 16L;
    private static final long GESTURE_HANDOFF_TIMEOUT_MS = 1200L;
    private static final ExecutorService TAP_INJECTOR = Executors.newSingleThreadExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "fanfreeform-tap-injector");
                thread.setDaemon(true);
                thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
                return thread;
            });

    interface Listener {
        void onTouch(MotionEvent event);
        void onAvailabilityChanged(boolean available);
    }

    private final Context context;
    private final Handler mainHandler;
    private final WindowManager windowManager;
    private final Listener listener;
    private View leftView;
    private View rightView;
    private volatile boolean capturing;
    private volatile long injectedDownTime;
    private volatile long injectedUntil;
    private int width;
    private int height;
    private int leftInset;
    private int rightInset;
    private boolean desiredEnabled;
    private int desiredHotWidthPercent;
    private int desiredHotHeightPercent;
    private int desiredLeftInset;
    private int desiredRightInset;
    private volatile boolean passthroughInProgress;
    private volatile boolean gestureHandoffInProgress;
    private final List<MotionEvent> capturedEvents = new ArrayList<>();

    FanTriggerCapture(Context context, Handler mainHandler, Listener listener) {
        this.context = context;
        this.mainHandler = mainHandler;
        this.listener = listener;
        windowManager = context.getSystemService(WindowManager.class);
    }

    void update(boolean enabled, int hotWidthPercent, int hotHeightPercent,
                int leftInset, int rightInset) {
        runOnMain(() -> updateNow(enabled, hotWidthPercent, hotHeightPercent,
                leftInset, rightInset));
    }

    boolean isCapturing() {
        return capturing;
    }

    void remove() {
        runOnMain(this::removeNow);
    }

    boolean isInjectedEvent(MotionEvent event) {
        if (event == null || SystemClock.uptimeMillis() > injectedUntil) return false;
        return event.getDownTime() == injectedDownTime;
    }

    boolean isPassthroughInProgress() {
        return passthroughInProgress;
    }

    void passthroughTap(float x, float y, int displayId) {
        runOnMain(() -> passthroughTapNow(x, y, displayId));
    }

    /**
     * For a fallback window, the DOWN has already been delivered to this window while
     * the direction arbiter was deciding. Replay that prefix and forward the remaining
     * stream to the application once the gesture is known not to be a fan swipe.
     */
    void passthroughCapturedGesture(int displayId) {
        runOnMain(() -> passthroughCapturedGestureNow(displayId));
    }

    void dispatchBack(int displayId) {
        runOnMain(() -> dispatchBackNow(displayId));
    }

    void dispatchKeyCode(int keyCode, int displayId) {
        runOnMain(() -> dispatchKeyCodeNow(keyCode, displayId));
    }

    private void updateNow(boolean enabled, int hotWidthPercent, int hotHeightPercent,
                           int leftInset, int rightInset) {
        desiredEnabled = enabled;
        desiredHotWidthPercent = hotWidthPercent;
        desiredHotHeightPercent = hotHeightPercent;
        desiredLeftInset = Math.max(0, leftInset);
        desiredRightInset = Math.max(0, rightInset);
        if (passthroughInProgress) return;
        if (!enabled) {
            removeNow();
            return;
        }
        Rect bounds = windowManager.getCurrentWindowMetrics().getBounds();
        int nextWidth = Math.max(1, bounds.width() * hotWidthPercent / 100);
        int nextHeight = Math.max(1, bounds.height() * hotHeightPercent / 100);
        if (capturing && nextWidth == width && nextHeight == height
                && this.leftInset == desiredLeftInset
                && this.rightInset == desiredRightInset) return;
        removeNow();
        width = nextWidth;
        height = nextHeight;
        this.leftInset = desiredLeftInset;
        this.rightInset = desiredRightInset;
        boolean attached = attach(TYPE_NAVIGATION_BAR_PANEL) || attach(2038);
        listener.onAvailabilityChanged(attached);
        if (!attached) {
            Log.i("Corner trigger capture unavailable; using pilfer fallback");
        }
    }

    private void passthroughTapNow(float x, float y, int displayId) {
        if (passthroughInProgress) return;
        passthroughInProgress = true;
        // Updating FLAG_NOT_TOUCHABLE in place is asynchronous in WindowManagerService.
        // A tap injected immediately afterwards can still target this transparent
        // window, which makes the bottom-corner area look dead. Remove the capture
        // windows first so input targeting has an unambiguous application target.
        removeNow();
        long downTime = SystemClock.uptimeMillis() + TAP_TARGET_SETTLE_MS;
        injectedDownTime = downTime;
        injectedUntil = downTime + 1000L;
        mainHandler.postDelayed(() -> {
            try {
                TAP_INJECTOR.execute(() -> {
                    boolean downAccepted = injectTapEvent(MotionEvent.ACTION_DOWN,
                            downTime, downTime, x, y, displayId);
                    boolean upAccepted = false;
                    if (downAccepted) {
                        SystemClock.sleep(TAP_DURATION_MS);
                        upAccepted = injectTapEvent(MotionEvent.ACTION_UP,
                                downTime, downTime + TAP_DURATION_MS, x, y, displayId);
                        if (!upAccepted) {
                            injectTapEvent(MotionEvent.ACTION_CANCEL, downTime,
                                    SystemClock.uptimeMillis(), x, y, displayId);
                        }
                    }
                    boolean accepted = downAccepted && upAccepted;
                    mainHandler.post(() -> finishTapPassthrough(accepted));
                });
            } catch (Throwable error) {
                Log.e("Cannot schedule application tap passthrough", error);
                finishTapPassthrough(false);
            }
        }, TAP_TARGET_SETTLE_MS);
        Log.i("Replaying corner tap to application x=" + Math.round(x)
                + " y=" + Math.round(y));
    }

    private void passthroughCapturedGestureNow(int displayId) {
        if (passthroughInProgress || !capturing || capturedEvents.isEmpty()) return;
        passthroughInProgress = true;
        gestureHandoffInProgress = true;
        MotionEvent first = capturedEvents.get(0);
        injectedDownTime = first.getDownTime();
        injectedUntil = SystemClock.uptimeMillis() + 2000L;
        List<MotionEvent> prefix = copyCapturedEvents();
        // Keep receiving the physical stream while the window is being handed off. The
        // replay is injected while the capture windows are temporarily untouchable, so
        // WindowManager targets the underlying application instead of this view.
        if (!setCaptureTouchable(false)) {
            removeNow();
            gestureHandoffInProgress = true;
        }
        enqueueGestureEvents(prefix, displayId, false);
        mainHandler.removeCallbacks(gestureHandoffTimeout);
        mainHandler.postDelayed(gestureHandoffTimeout, GESTURE_HANDOFF_TIMEOUT_MS);
        Log.i("Corner non-fan gesture handed off events=" + prefix.size());
    }

    private final Runnable gestureHandoffTimeout = this::finishGestureHandoff;

    private List<MotionEvent> copyCapturedEvents() {
        List<MotionEvent> copy = new ArrayList<>(capturedEvents.size());
        for (MotionEvent event : capturedEvents) copy.add(MotionEvent.obtain(event));
        return copy;
    }

    private void enqueueGestureEvent(MotionEvent event, int displayId) {
        if (!gestureHandoffInProgress) {
            event.recycle();
            return;
        }
        List<MotionEvent> single = new ArrayList<>(1);
        single.add(event);
        enqueueGestureEvents(single, displayId,
                event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL);
    }

    private void enqueueGestureEvents(List<MotionEvent> events, int displayId,
                                      boolean terminal) {
        try {
            TAP_INJECTOR.execute(() -> {
                boolean accepted = true;
                for (MotionEvent event : events) {
                    try {
                        accepted &= injectMotionEvent(event, displayId);
                    } finally {
                        event.recycle();
                    }
                }
                if (!accepted) {
                    Log.i("Corner non-fan gesture replay rejected");
                }
                if (terminal) mainHandler.post(this::finishGestureHandoff);
            });
        } catch (Throwable error) {
            for (MotionEvent event : events) event.recycle();
            Log.e("Cannot schedule corner gesture handoff", error);
            finishGestureHandoff();
        }
    }

    private void finishGestureHandoff() {
        mainHandler.removeCallbacks(gestureHandoffTimeout);
        gestureHandoffInProgress = false;
        passthroughInProgress = false;
        clearCapturedEvents();
        boolean restored = !capturing || !desiredEnabled || setCaptureTouchable(true);
        if (!restored) removeNow();
        updateNow(desiredEnabled, desiredHotWidthPercent, desiredHotHeightPercent,
                desiredLeftInset, desiredRightInset);
        Log.i("Corner non-fan gesture handoff finished");
    }

    private void finishTapPassthrough(boolean accepted) {
        passthroughInProgress = false;
        updateNow(desiredEnabled, desiredHotWidthPercent, desiredHotHeightPercent,
                desiredLeftInset, desiredRightInset);
        Log.i("Corner application tap passthrough accepted=" + accepted
                + " captureRestored=" + (desiredEnabled && capturing));
    }

    private boolean injectTapEvent(int action, long downTime, long eventTime,
                                   float x, float y, int displayId) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        try {
            return injectMotionEvent(event, displayId);
        } catch (Throwable error) {
            Log.e("Cannot replay application tap action=" + action, error);
            return false;
        } finally {
            event.recycle();
        }
    }

    private boolean injectMotionEvent(MotionEvent source, int displayId) {
        MotionEvent event = MotionEvent.obtain(source);
        try {
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            try {
                XposedHelpers.callMethod(event, "setDisplayId", displayId);
            } catch (Throwable ignored) {}
            Object inputManager = context.getSystemService(Context.INPUT_SERVICE);
            Object result = XposedHelpers.callMethod(inputManager,
                    "injectInputEvent", event, INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Throwable error) {
            Log.e("Cannot replay application motion action="
                    + source.getActionMasked(), error);
            return false;
        } finally {
            event.recycle();
        }
    }

    private void dispatchBackNow(int displayId) {
        long downTime = SystemClock.uptimeMillis();
        injectKeyEvent(KeyEvent.ACTION_DOWN, downTime, downTime, KeyEvent.KEYCODE_BACK,
                displayId);
        injectKeyEvent(KeyEvent.ACTION_UP, downTime, downTime + 16L, KeyEvent.KEYCODE_BACK,
                displayId);
        Log.i("Captured side swipe dispatched as system back");
    }

    private void dispatchKeyCodeNow(int keyCode, int displayId) {
        long downTime = SystemClock.uptimeMillis();
        injectKeyEvent(KeyEvent.ACTION_DOWN, downTime, downTime, keyCode, displayId);
        injectKeyEvent(KeyEvent.ACTION_UP, downTime, downTime + 16L, keyCode, displayId);
        Log.i("Dispatched system key code=" + keyCode);
    }

    private void injectKeyEvent(int action, long downTime, long eventTime, int keyCode,
                                 int displayId) {
        KeyEvent event = new KeyEvent(downTime, eventTime, action, keyCode,
                0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                InputDevice.SOURCE_KEYBOARD);
        try {
            try {
                XposedHelpers.callMethod(event, "setDisplayId", displayId);
            } catch (Throwable ignored) {}
            Object inputManager = context.getSystemService(Context.INPUT_SERVICE);
            Object result = XposedHelpers.callMethod(inputManager,
                    "injectInputEvent", event, 0);
            if (result instanceof Boolean && !((Boolean) result)) {
                Log.i("Injected system key was rejected code=" + keyCode + " action=" + action);
            }
        } catch (Throwable error) {
            Log.e("Cannot dispatch system key code=" + keyCode + " action=" + action, error);
        }
    }

    private boolean attach(int type) {
        View left = captureView("LEFT");
        View right = captureView("RIGHT");
        try {
            windowManager.addView(left, params(type, Gravity.BOTTOM | Gravity.START,
                    "FanFreeformTriggerLeft", leftInset));
            windowManager.addView(right, params(type, Gravity.BOTTOM | Gravity.END,
                    "FanFreeformTriggerRight", rightInset));
            leftView = left;
            rightView = right;
            capturing = true;
            Log.i("Corner trigger capture attached size=" + width + "x" + height
                    + " inset=" + leftInset + "/" + rightInset + " type=" + type);
            return true;
        } catch (Throwable error) {
            removeView(left);
            removeView(right);
            leftView = null;
            rightView = null;
            capturing = false;
            Log.e("Cannot attach corner trigger capture type=" + type, error);
            return false;
        }
    }

    private View captureView(String corner) {
        View view = new View(context);
        view.setClickable(true);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        view.setOnTouchListener((target, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                Log.i("Corner trigger window received down corner=" + corner);
            }
            MotionEvent screenEvent = MotionEvent.obtain(event);
            try {
                screenEvent.offsetLocation(event.getRawX() - event.getX(),
                        event.getRawY() - event.getY());
                recordCapturedEvent(screenEvent);
                if (gestureHandoffInProgress) {
                    enqueueGestureEvent(MotionEvent.obtain(screenEvent),
                            context.getDisplay() == null ? 0 : context.getDisplay().getDisplayId());
                } else {
                    listener.onTouch(screenEvent);
                }
            } finally {
                screenEvent.recycle();
            }
            return true;
        });
        return view;
    }

    private void recordCapturedEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) clearCapturedEvents();
        if (capturedEvents.size() < 160) {
            capturedEvents.add(MotionEvent.obtain(event));
        } else if (capturedEvents.size() == 160) {
            // Preserve DOWN and keep the newest motion samples bounded on very long drags.
            MotionEvent oldestMove = capturedEvents.remove(1);
            oldestMove.recycle();
            capturedEvents.add(MotionEvent.obtain(event));
        }
        if (!gestureHandoffInProgress
                && (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL)) {
            clearCapturedEvents();
        }
    }

    private WindowManager.LayoutParams params(int type, int gravity, String title,
                                              int horizontalInset) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = gravity;
        params.x = horizontalInset;
        params.setTitle(title);
        params.setFitInsetsTypes(0);
        params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        return params;
    }

    private void removeNow() {
        mainHandler.removeCallbacks(gestureHandoffTimeout);
        removeView(leftView);
        removeView(rightView);
        leftView = null;
        rightView = null;
        capturing = false;
        gestureHandoffInProgress = false;
        clearCapturedEvents();
    }

    private boolean setCaptureTouchable(boolean touchable) {
        boolean leftUpdated = setViewTouchable(leftView, touchable);
        boolean rightUpdated = setViewTouchable(rightView, touchable);
        return leftUpdated && rightUpdated;
    }

    private boolean setViewTouchable(View view, boolean touchable) {
        if (view == null) return false;
        try {
            if (!(view.getLayoutParams() instanceof WindowManager.LayoutParams)) return false;
            WindowManager.LayoutParams params =
                    (WindowManager.LayoutParams) view.getLayoutParams();
            if (touchable) {
                params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            } else {
                params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            }
            windowManager.updateViewLayout(view, params);
            return true;
        } catch (Throwable error) {
            Log.e("Cannot change corner trigger touchability", error);
            return false;
        }
    }

    private void clearCapturedEvents() {
        for (MotionEvent event : capturedEvents) event.recycle();
        capturedEvents.clear();
    }

    private void removeView(View view) {
        if (view == null) return;
        try {
            windowManager.removeViewImmediate(view);
        } catch (Throwable error) {
            Log.e("Cannot remove corner trigger capture", error);
        }
    }

    private void runOnMain(Runnable runnable) {
        if (mainHandler.getLooper().isCurrentThread()) runnable.run();
        else mainHandler.post(runnable);
    }
}
