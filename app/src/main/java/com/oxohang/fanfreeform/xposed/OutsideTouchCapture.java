package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class OutsideTouchCapture {
    private static final int TYPE_APPLICATION_OVERLAY = 2038;
    private static final int TYPE_MAGNIFICATION_OVERLAY = 2027;
    private static final int TYPE_NAVIGATION_BAR_PANEL = 2024;

    interface TouchListener {
        void onTouch(int action, float x, float y, long downTime, long eventTime);
    }

    private final Context context;
    private final Handler mainHandler;
    private final WindowManager windowManager;
    private final TouchListener touchListener;
    private final List<View> views = new ArrayList<>();
    private volatile List<Rect> activeRegions = new ArrayList<>();
    private List<Rect> currentWindows = new ArrayList<>();
    private final Rect currentIme = new Rect();
    private boolean hasCurrentIme;
    private int currentLeftPassThrough;
    private int currentRightPassThrough;
    private int currentWindowType;
    private volatile boolean capturing;
    private int activeGeneration;

    OutsideTouchCapture(Context context, Handler mainHandler, TouchListener touchListener) {
        this.context = context;
        this.mainHandler = mainHandler;
        this.touchListener = touchListener;
        windowManager = context.getSystemService(WindowManager.class);
    }

    void update(List<Rect> windows, Rect ime, int leftPassThrough, int rightPassThrough) {
        List<Rect> nextWindows = copyRects(windows);
        Rect nextIme = ime == null ? null : new Rect(ime);
        runOnMain(() -> updateNow(nextWindows, nextIme,
                Math.max(0, leftPassThrough), Math.max(0, rightPassThrough)));
    }

    boolean captures(float x, float y) {
        for (Rect region : activeRegions) {
            if (region.contains((int) x, (int) y)) return true;
        }
        return false;
    }

    void remove() {
        runOnMain(this::removeNow);
    }

    private void updateNow(List<Rect> windows, Rect ime,
                           int leftPassThrough, int rightPassThrough) {
        Rect display = windowManager.getCurrentWindowMetrics().getBounds();
        List<Rect> clippedWindows = clippedWindows(windows, display);
        Rect clippedIme = clipped(ime, display);
        if (clippedWindows.isEmpty()) {
            removeNow();
            return;
        }
        if (currentWindows.equals(clippedWindows)
                && hasCurrentIme == (clippedIme != null)
                && (clippedIme == null || currentIme.equals(clippedIme))
                && currentLeftPassThrough == leftPassThrough
                && currentRightPassThrough == rightPassThrough
                && capturing) {
            return;
        }

        List<Rect> regions = outsideRegions(display, clippedWindows, clippedIme,
                leftPassThrough, rightPassThrough);
        if (updateAttachedRegions(regions, clippedWindows, clippedIme,
                leftPassThrough, rightPassThrough)) {
            return;
        }

        removeNow();
        int generation = activeGeneration;
        // Application-overlay is above app/freeform windows but below StatusBar and
        // NotificationShade. This keeps system panels touchable without weakening
        // outside-tap blocking. Retain the old types only as compatibility fallbacks.
        if (!attach(regions, clippedWindows.size(), TYPE_APPLICATION_OVERLAY, generation)
                && !attach(regions, clippedWindows.size(),
                TYPE_NAVIGATION_BAR_PANEL, generation)
                && !attach(regions, clippedWindows.size(),
                TYPE_MAGNIFICATION_OVERLAY, generation)) {
            Log.i("Outside touch capture unavailable; using input-monitor fallback");
            return;
        }
        commitState(regions, clippedWindows, clippedIme,
                leftPassThrough, rightPassThrough);
    }

    private boolean updateAttachedRegions(List<Rect> regions, List<Rect> windows,
                                          Rect ime, int leftPassThrough,
                                          int rightPassThrough) {
        if (!capturing || currentWindowType == 0 || views.size() != regions.size()) {
            return false;
        }
        try {
            for (int index = 0; index < regions.size(); index++) {
                windowManager.updateViewLayout(views.get(index),
                        params(regions.get(index), currentWindowType, index));
            }
            commitState(regions, windows, ime, leftPassThrough, rightPassThrough);
            return true;
        } catch (Throwable error) {
            Log.e("Cannot move outside touch capture in place", error);
            return false;
        }
    }

    private void commitState(List<Rect> regions, List<Rect> windows, Rect ime,
                             int leftPassThrough, int rightPassThrough) {
        currentWindows = copyRects(windows);
        currentLeftPassThrough = leftPassThrough;
        currentRightPassThrough = rightPassThrough;
        activeRegions = copyRects(regions);
        hasCurrentIme = ime != null;
        if (ime == null) currentIme.setEmpty();
        else currentIme.set(ime);
    }

    private boolean attach(List<Rect> regions, int windowCount, int type,
                           int generation) {
        try {
            for (int index = 0; index < regions.size(); index++) {
                Rect region = regions.get(index);
                View view = captureView(index, generation);
                // Track first so cleanup can still reach a view when addView throws
                // after WindowManagerGlobal has already registered it.
                views.add(view);
                windowManager.addView(view, params(region, type, index));
            }
            capturing = !views.isEmpty();
            if (capturing) {
                currentWindowType = type;
                Log.i("Outside touch capture attached regions=" + views.size()
                        + " visibleWindows=" + windowCount + " type=" + type);
            }
            return capturing;
        } catch (Throwable error) {
            removeViews();
            Log.e("Cannot attach outside touch capture type=" + type, error);
            return false;
        }
    }

    private View captureView(int index, int generation) {
        View view = new View(context);
        view.setClickable(true);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        view.setOnTouchListener((target, event) -> {
            if (generation != activeGeneration) {
                // An IME animation can request several region updates before a newly
                // added view reports itself attached. If an obsolete view ever survives
                // that race, remove it on first contact instead of leaving a dead strip.
                removeStaleView(target, generation);
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                Log.i("Outside capture consumed down region=" + index);
            }
            touchListener.onTouch(event.getActionMasked(), event.getRawX(), event.getRawY(),
                    event.getDownTime(), event.getEventTime());
            return true;
        });
        return view;
    }

    private WindowManager.LayoutParams params(Rect region, int type, int index) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                region.width(), region.height(), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = region.left;
        params.y = region.top;
        params.setTitle("FanFreeformOutside" + index);
        params.setFitInsetsTypes(0);
        params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        return params;
    }

    private void removeNow() {
        activeGeneration++;
        removeViews();
        currentWindows = new ArrayList<>();
        currentIme.setEmpty();
        hasCurrentIme = false;
        currentLeftPassThrough = 0;
        currentRightPassThrough = 0;
        currentWindowType = 0;
    }

    private void removeViews() {
        List<View> removing = new ArrayList<>(views);
        views.clear();
        capturing = false;
        activeRegions = new ArrayList<>();
        for (View view : removing) {
            if (view == null) continue;
            try {
                // WindowManager already knows about a view immediately after addView,
                // even before View#isAttachedToWindow becomes true. Skipping it in that
                // interval leaks an invisible touch window, most often while IME bounds
                // are changing quickly.
                windowManager.removeViewImmediate(view);
            } catch (Throwable error) {
                Log.e("Cannot remove outside touch capture", error);
            }
        }
    }

    private void removeStaleView(View view, int generation) {
        try {
            windowManager.removeViewImmediate(view);
            Log.i("Removed stale outside capture generation=" + generation
                    + " active=" + activeGeneration);
        } catch (Throwable error) {
            Log.e("Cannot remove stale outside touch capture", error);
        }
    }

    private void runOnMain(Runnable runnable) {
        if (mainHandler.getLooper().isCurrentThread()) {
            runnable.run();
            return;
        }
        CountDownLatch completed = new CountDownLatch(1);
        mainHandler.post(() -> {
            try {
                runnable.run();
            } finally {
                completed.countDown();
            }
        });
        try {
            completed.await(250L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }

    static List<Rect> outsideRegions(Rect display, List<Rect> windows, Rect ime,
                                     int leftPassThrough, int rightPassThrough) {
        int captureLeft = Math.max(display.left,
                Math.min(display.right, display.left + leftPassThrough));
        int captureRight = Math.max(captureLeft,
                Math.min(display.right, display.right - rightPassThrough));
        int captureBottom = ime == null ? display.bottom
                : Math.max(display.top, Math.min(display.bottom, ime.top));
        List<Rect> regions = new ArrayList<>();
        addIfNonEmpty(regions, captureLeft, display.top, captureRight, captureBottom);
        for (Rect window : windows) {
            regions = subtract(regions, window);
        }
        return regions;
    }

    private static List<Rect> subtract(List<Rect> sources, Rect cut) {
        List<Rect> result = new ArrayList<>(sources.size() + 4);
        for (Rect source : sources) {
            Rect intersection = new Rect(source);
            if (!intersection.intersect(cut)) {
                result.add(source);
                continue;
            }
            addIfNonEmpty(result, source.left, source.top, source.right, intersection.top);
            addIfNonEmpty(result, source.left, intersection.bottom,
                    source.right, source.bottom);
            addIfNonEmpty(result, source.left, intersection.top,
                    intersection.left, intersection.bottom);
            addIfNonEmpty(result, intersection.right, intersection.top,
                    source.right, intersection.bottom);
        }
        return result;
    }

    private static List<Rect> clippedWindows(List<Rect> windows, Rect display) {
        List<Rect> result = new ArrayList<>();
        for (Rect window : windows) {
            Rect clipped = clipped(window, display);
            if (clipped != null && !result.contains(clipped)) result.add(clipped);
        }
        result.sort(Comparator.comparingInt((Rect rect) -> rect.top)
                .thenComparingInt(rect -> rect.left)
                .thenComparingInt(rect -> rect.bottom)
                .thenComparingInt(rect -> rect.right));
        return result;
    }

    private static Rect clipped(Rect source, Rect display) {
        if (source == null) return null;
        Rect result = new Rect(source);
        if (!result.intersect(display) || result.isEmpty()) return null;
        return result;
    }

    private static List<Rect> copyRects(List<Rect> source) {
        List<Rect> copy = new ArrayList<>();
        if (source == null) return copy;
        for (Rect rect : source) {
            if (rect != null && !rect.isEmpty()) copy.add(new Rect(rect));
        }
        return copy;
    }

    private static void addIfNonEmpty(List<Rect> regions,
                                      int left, int top, int right, int bottom) {
        if (right > left && bottom > top) regions.add(new Rect(left, top, right, bottom));
    }
}
