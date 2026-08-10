package com.oxohang.fanfreeform.xposed;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;

import com.oxohang.fanfreeform.config.ConfigContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TaskSwitcherOverlayView extends View {
    interface Listener {
        void onLaunch(RecentTaskPreview task);
        void onClosed();
    }

    private final float density;
    private final int touchSlop;
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wallpaperPaint = new Paint(Paint.ANTI_ALIAS_FLAG
            | Paint.FILTER_BITMAP_FLAG);
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint snapshotPaint = new Paint(Paint.ANTI_ALIAS_FLAG
            | Paint.FILTER_BITMAP_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clipPath = new Path();
    private final Rect source = new Rect();
    private final Rect iconOldBounds = new Rect();
    private final RectF destination = new RectF();
    private final RectF wallpaperBounds = new RectF();
    private final RectF cardBounds = new RectF();
    private Shader fallbackCardShader;
    private List<RecentTaskPreview> tasks = Collections.emptyList();
    private Listener listener;
    private Bitmap wallpaper;
    private float anchorX;
    private float anchorY;
    private float pointerY;
    private float focusX;
    private float focusY;
    private float cardWidth;
    private float cardHeight;
    private float cardCorner;
    private float iconSize;
    private float iconGap;
    private float fingerOffset;
    private float dismissDistance;
    private float inwardDirection;
    private float scrollPosition;
    private float displayPosition;
    private float lastPointerX;
    private float lastInward;
    private float fingerVelocity;
    private float currentPulse;
    private float settleStartPosition;
    private float entryProgress;
    private float confirmationProgress;
    private int backgroundStyle;
    private int dimPercent;
    private int blurDp;
    private int layoutMode;
    private int motionMode;
    private int swipeSpeedPercent;
    private int animationSpeedIndex;
    private int selected = -1;
    private int lastHapticIndex = -1;
    private int selectionDirection = 1;
    private long lastMoveNanos;
    private long selectionPulseStartNanos;
    private boolean hapticEnabled;
    private boolean showTaskName;
    private boolean movedForSelection;
    private boolean closing;
    private boolean released;
    private ValueAnimator animator;
    private ValueAnimator confirmationAnimator;
    private MotionProfile motionProfile = MotionProfile.magnetic();
    private final BlurredWallpaperCache.Callback wallpaperCallback = bitmap -> post(() -> {
        if (backgroundStyle != ConfigContract.HONEYCOMB_BACKGROUND_BLUR) return;
        wallpaper = bitmap;
        invalidate();
    });

    TaskSwitcherOverlayView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setFocusableInTouchMode(true);
        cardPaint.setColor(0xff242733);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.LEFT);
        labelPaint.setTextSize(15f * density);
    }

    void configure(List<RecentTaskPreview> tasks, GestureGeometry.Corner corner,
                   float anchorX, float anchorY,
                   GestureConfig config, Listener listener) {
        ArrayList<RecentTaskPreview> orderedTasks = tasks == null
                ? new ArrayList<>() : new ArrayList<>(tasks);
        if (config.sideTaskReverseOrder) Collections.reverse(orderedTasks);
        this.tasks = orderedTasks;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.pointerY = anchorY;
        this.lastPointerX = anchorX;
        this.listener = listener;
        hapticEnabled = config.haptic;
        showTaskName = config.sideTaskShowName;
        cardWidth = config.sideTaskCardWidthDp * density;
        cardHeight = config.sideTaskCardHeightDp * density;
        cardCorner = config.sideTaskCardCornerDp * density;
        iconSize = config.sideTaskIconSizeDp * density;
        iconGap = config.sideTaskIconGapDp * density;
        fingerOffset = config.sideTaskFingerOffsetDp * density;
        float contentHeight = config.sideTaskLayoutMode == ConfigContract.SIDE_TASK_LAYOUT_ICONS
                ? iconSize : cardHeight;
        dismissDistance = TaskSelectionPolicy.downwardDismissDistance(
                Math.max(44f * density, contentHeight * 0.22f),
                config.sideTaskExtendedDownwardTolerance);
        inwardDirection = corner == GestureGeometry.Corner.RIGHT ? -1f : 1f;
        layoutMode = config.sideTaskLayoutMode;
        motionMode = config.sideTaskMotionMode;
        motionProfile = MotionProfile.forMode(motionMode);
        swipeSpeedPercent = config.sideTaskSwipeSpeedPercent;
        animationSpeedIndex = config.sideTaskAnimationSpeed;
        backgroundStyle = config.honeycombBackgroundStyle;
        dimPercent = config.honeycombDimPercent;
        blurDp = config.honeycombBlurDp;
        if (backgroundStyle == ConfigContract.HONEYCOMB_BACKGROUND_BLUR) loadWallpaper();
    }

    void playEntry() {
        requestFocus();
        animateProgress(0f, 1f, TaskCenterAnimationPolicy.durationMs(245L,
                animationSpeedIndex), false);
    }

    void onExternalMove(float x, float y) {
        if (closing) return;
        pointerY = y;
        float contentWidth = layoutMode == ConfigContract.SIDE_TASK_LAYOUT_ICONS
                ? iconSize : cardWidth;
        float horizontalMargin = 18f * density + contentWidth / 2f;
        focusX = clamp(x, horizontalMargin, getWidth() - horizontalMargin);
        if (y > anchorY + dismissDistance) {
            // Do not accumulate horizontal distance while the pointer is in the
            // dismissal zone. Returning to the rail should resume from the
            // latest pointer coordinate rather than jump across several tasks.
            lastPointerX = x;
            if (selected != -1) {
                selected = -1;
                invalidate();
            }
            return;
        }
        float inward = (x - anchorX) * inwardDirection;
        if (Math.abs(inward) > touchSlop * 1.35f) movedForSelection = true;
        float baseStep = layoutMode == ConfigContract.SIDE_TASK_LAYOUT_ICONS
                ? clamp((iconSize + iconGap) * 0.72f,
                30f * density, 150f * density)
                : clamp(cardWidth * 0.22f, 40f * density, 72f * density);
        float step = baseStep * 100f / swipeSpeedPercent;
        float inwardDelta = (x - lastPointerX) * inwardDirection;
        scrollPosition = TaskSelectionPolicy.advancePosition(
                scrollPosition, inwardDelta, step, tasks.size());
        lastPointerX = x;
        updateFingerVelocity(inward);
        displayPosition = magneticPosition(scrollPosition);
        int next = TaskSelectionPolicy.nearestIndex(
                displayPosition, tasks.size(), movedForSelection);
        if (next != selected) {
            int previous = selected;
            selected = next;
            if (next >= 0) {
                selectionDirection = previous < 0 || next >= previous ? 1 : -1;
                selectionPulseStartNanos = System.nanoTime();
            }
            if (hapticEnabled && next >= 0 && next != lastHapticIndex) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                lastHapticIndex = next;
            }
        }
        invalidate();
    }

    void onExternalUp(float x, float y, boolean cancelled) {
        if (closing) return;
        onExternalMove(x, y);
        if (cancelled || selected < 0 || selected >= tasks.size()) playDismissal();
        else playConfirmation(selected);
    }

    void onExternalCancel() { playDismissal(); }

    @Override protected void onSizeChanged(int width, int height,
                                           int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        fallbackCardShader = new LinearGradient(0f, 0f, Math.max(1, width),
                Math.max(1, height), 0xff3b3f52, 0xff171922, Shader.TileMode.CLAMP);
        float contentWidth = layoutMode == ConfigContract.SIDE_TASK_LAYOUT_ICONS
                ? iconSize : cardWidth;
        float contentHeight = layoutMode == ConfigContract.SIDE_TASK_LAYOUT_ICONS
                ? iconSize : cardHeight;
        float horizontalMargin = 18f * density + contentWidth / 2f;
        focusX = clamp(anchorX, horizontalMargin, width - horizontalMargin);
        float preferred = anchorY - fingerOffset - contentHeight * 0.18f;
        float verticalMargin = contentHeight / 2f + 56f * density;
        focusY = clamp(preferred, verticalMargin, height - verticalMargin);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground(canvas);
        currentPulse = selectionPulse();
        int pivot = tasks.isEmpty() ? -1 : Math.max(0, Math.min(tasks.size() - 1,
                Math.round(displayPosition)));
        for (int distance = tasks.size(); distance >= 0; distance--) {
            int before = pivot - distance;
            int after = pivot + distance;
            if (before >= 0 && before < tasks.size() && before != selected) {
                drawTask(canvas, before, false);
            }
            if (after != before && after >= 0 && after < tasks.size()
                    && after != selected) {
                drawTask(canvas, after, false);
            }
        }
        if (selected >= 0 && selected < tasks.size()) drawTask(canvas, selected, true);
        if (currentPulse > 0.001f && !closing) postInvalidateOnAnimation();
    }

    private void drawBackground(Canvas canvas) {
        float visible = entryProgress * (1f - confirmationProgress * 0.15f);
        if (wallpaper != null && backgroundStyle == ConfigContract.HONEYCOMB_BACKGROUND_BLUR) {
            wallpaperPaint.setAlpha(Math.round(255f * visible));
            wallpaperBounds.set(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(wallpaper, null, wallpaperBounds, wallpaperPaint);
            backgroundPaint.setColor(Color.BLACK);
            backgroundPaint.setAlpha(Math.round(255f * dimPercent / 100f * visible));
        } else {
            backgroundPaint.setColor(Color.BLACK);
            backgroundPaint.setAlpha(Math.round(242f * visible));
        }
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);
    }

    private void drawTask(Canvas canvas, int index, boolean active) {
        if (layoutMode == ConfigContract.SIDE_TASK_LAYOUT_ICONS) {
            drawTaskIcon(canvas, index, active);
            return;
        }
        float relative = index - displayPosition;
        float spacing = cardWidth + 14f * density;
        float centerX = focusX + relative * spacing * inwardDirection;
        float visibleMargin = cardWidth * 0.72f;
        if (centerX < -visibleMargin || centerX > getWidth() + visibleMargin) return;

        float distance = Math.abs(relative);
        if (!active) {
            float influence = clamp(1f - distance / 1.65f, 0f, 1f);
            float outward = Math.signum(relative) * inwardDirection;
            centerX += outward * motionProfile.neighborShiftDp * density * influence;
            if (motionMode == ConfigContract.SIDE_TASK_MOTION_MARBLE) {
                float directionalWave = 0.65f * outward
                        + 0.35f * selectionDirection * inwardDirection;
                centerX += directionalWave * motionProfile.waveShiftDp * density
                        * currentPulse / (1f + distance);
            }
        }
        float scale = entryProgress;
        if (active) {
            scale *= 1f + motionProfile.selectedScale
                    + motionProfile.pulseScale * currentPulse
                    + confirmationProgress * 0.055f;
        }
        float alpha = clamp(1f - Math.max(0f, distance - 1f) * 0.22f,
                0.30f, 1f) * entryProgress;
        if (alpha <= 0.01f || scale <= 0.01f) return;
        float width = cardWidth * scale;
        float height = cardHeight * scale;
        float centerY = focusY - (active ? motionProfile.liftDp * density
                * (0.65f + 0.35f * currentPulse) : 0f);
        cardBounds.set(centerX - width / 2f, centerY - height / 2f,
                centerX + width / 2f, centerY + height / 2f);
        int save = canvas.save();
        cardPaint.setAlpha(Math.round(255f * alpha));
        if (active) {
            cardPaint.setShadowLayer(20f * density, 0, 6f * density, 0x99000000);
        } else cardPaint.clearShadowLayer();
        float corner = cardCorner * Math.max(0.75f, scale);
        canvas.drawRoundRect(cardBounds, corner, corner, cardPaint);
        clipPath.reset();
        clipPath.addRoundRect(cardBounds, corner, corner, Path.Direction.CW);
        canvas.clipPath(clipPath);
        RecentTaskPreview task = tasks.get(index);
        snapshotPaint.setAlpha(Math.round(255f * alpha));
        if (task.snapshot != null && !task.snapshot.isRecycled()) {
            drawCenterCrop(canvas, task.snapshot, cardBounds);
        } else {
            if (fallbackCardShader == null) {
                fallbackCardShader = new LinearGradient(0f, 0f, Math.max(1, getWidth()),
                        Math.max(1, getHeight()), 0xff3b3f52, 0xff171922,
                        Shader.TileMode.CLAMP);
            }
            cardPaint.setShader(fallbackCardShader);
            canvas.drawRect(cardBounds, cardPaint);
            cardPaint.setShader(null);
            drawIcon(canvas, task.icon, centerX, centerY,
                    Math.min(width, height) * 0.30f, alpha);
        }
        canvas.restoreToCount(save);
        drawTaskHeader(canvas, task, cardBounds, scale, alpha);
    }

    private void drawTaskIcon(Canvas canvas, int index, boolean active) {
        float relative = index - displayPosition;
        float distance = Math.abs(relative);
        float spacing = iconSize + iconGap;
        float centerX = focusX + relative * spacing * inwardDirection;
        float visibleMargin = iconSize * 1.5f;
        if (centerX < -visibleMargin || centerX > getWidth() + visibleMargin) return;
        if (!active) {
            float influence = clamp(1f - distance / 1.8f, 0f, 1f);
            float outward = Math.signum(relative) * inwardDirection;
            centerX += outward * motionProfile.neighborShiftDp * density * influence;
            if (motionMode == ConfigContract.SIDE_TASK_MOTION_MARBLE) {
                centerX += outward * motionProfile.waveShiftDp * density
                        * currentPulse / (1f + distance);
            }
        }
        float scale = clamp(1f - distance * 0.08f, 0.74f, 1f) * entryProgress;
        if (active) {
            scale *= 1.18f + motionProfile.selectedScale
                    + motionProfile.pulseScale * currentPulse
                    + confirmationProgress * 0.08f;
        }
        float alpha = clamp(1f - Math.max(0f, distance - 1.5f) * 0.16f,
                0.35f, 1f) * entryProgress;
        float lift = active ? (6f + motionProfile.liftDp
                * (0.65f + 0.35f * currentPulse)) * density : 0f;
        float centerY = focusY - lift;
        float resolvedSize = iconSize * scale;
        RecentTaskPreview task = tasks.get(index);
        drawIcon(canvas, task.icon, centerX, centerY, resolvedSize, alpha);
        if (active) drawSelectedIconName(canvas, task, centerX, centerY, resolvedSize, alpha);
    }

    private void drawSelectedIconName(Canvas canvas, RecentTaskPreview task,
                                      float centerX, float centerY,
                                      float resolvedSize, float alpha) {
        if (!showTaskName || entryProgress < 0.1f) return;
        String label = task.label == null ? task.packageName : task.label.toString();
        labelPaint.setTextSize(15f * density);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setAlpha(Math.round(255f * alpha));
        String fitted = fitLabel(label, getWidth() * 0.68f);
        float baselineCenter = Math.max(24f * density,
                centerY - resolvedSize / 2f - 20f * density);
        labelPaint.setShadowLayer(6f * density, 0, 2f * density, 0xaa000000);
        Paint.FontMetrics metrics = labelPaint.getFontMetrics();
        canvas.drawText(fitted, centerX,
                baselineCenter - (metrics.ascent + metrics.descent) / 2f, labelPaint);
        labelPaint.clearShadowLayer();
        labelPaint.setAlpha(255);
        labelPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawCenterCrop(Canvas canvas, Bitmap bitmap, RectF card) {
        float sourceRatio = bitmap.getWidth() / (float) bitmap.getHeight();
        float targetRatio = card.width() / card.height();
        if (sourceRatio > targetRatio) {
            int cropWidth = Math.round(bitmap.getHeight() * targetRatio);
            int left = (bitmap.getWidth() - cropWidth) / 2;
            source.set(left, 0, left + cropWidth, bitmap.getHeight());
        } else {
            int cropHeight = Math.round(bitmap.getWidth() / targetRatio);
            int top = (bitmap.getHeight() - cropHeight) / 2;
            source.set(0, top, bitmap.getWidth(), top + cropHeight);
        }
        destination.set(card);
        canvas.drawBitmap(bitmap, source, destination, snapshotPaint);
    }

    private void drawIcon(Canvas canvas, Drawable icon, float x, float y,
                          float size, float alpha) {
        if (icon == null) return;
        icon.copyBounds(iconOldBounds);
        int half = Math.round(size / 2f);
        icon.setBounds(Math.round(x) - half, Math.round(y) - half,
                Math.round(x) + half, Math.round(y) + half);
        icon.setAlpha(Math.round(255f * alpha));
        icon.draw(canvas);
        icon.setAlpha(255);
        icon.setBounds(iconOldBounds);
    }

    private void drawTaskHeader(Canvas canvas, RecentTaskPreview task, RectF card,
                                float cardScale, float alpha) {
        if (!showTaskName || entryProgress < 0.1f || card.width() < 32f * density) return;
        float headerScale = clamp(cardScale, 0.72f, 1.08f);
        float iconSize = 23f * density * headerScale;
        float gap = 8f * density * headerScale;
        float left = card.left + 3f * density * headerScale;
        float centerY = Math.max(iconSize / 2f + 8f * density,
                card.top - 17f * density * headerScale);
        float iconX = left + iconSize / 2f;
        drawIcon(canvas, task.icon, iconX, centerY, iconSize, alpha);

        String label = task.label == null ? task.packageName : task.label.toString();
        labelPaint.setTextSize(14f * density * headerScale);
        labelPaint.setAlpha(Math.round(255f * alpha));
        float textX = left + iconSize + gap;
        float maxTextWidth = Math.max(0f, card.right - textX - 3f * density);
        String fitted = fitLabel(label, maxTextWidth);
        labelPaint.setShadowLayer(5f * density, 0, 2f * density, 0x99000000);
        Paint.FontMetrics metrics = labelPaint.getFontMetrics();
        canvas.drawText(fitted, textX,
                centerY - (metrics.ascent + metrics.descent) / 2f, labelPaint);
        labelPaint.clearShadowLayer();
        labelPaint.setAlpha(255);
    }

    private String fitLabel(String label, float maximumWidth) {
        if (label == null || maximumWidth <= 0f) return "";
        if (labelPaint.measureText(label) <= maximumWidth) return label;
        String suffix = "...";
        float available = Math.max(0f, maximumWidth - labelPaint.measureText(suffix));
        int count = labelPaint.breakText(label, true, available, null);
        return count <= 0 ? suffix : label.substring(0, count) + suffix;
    }

    private void playConfirmation(int index) {
        if (closing) return;
        closing = true;
        RecentTaskPreview task = tasks.get(index);
        settleStartPosition = displayPosition;
        ValueAnimator confirm = ValueAnimator.ofFloat(0f, 1f);
        confirmationAnimator = confirm;
        confirm.setDuration(TaskCenterAnimationPolicy.durationMs(
                motionProfile.confirmDurationMs, animationSpeedIndex));
        confirm.setInterpolator(new LinearInterpolator());
        confirm.addUpdateListener(value -> {
            float progress = (Float) value.getAnimatedValue();
            confirmationProgress = progress;
            float settled = settleProgress(progress, motionProfile.settleOvershoot);
            displayPosition = settleStartPosition
                    + (index - settleStartPosition) * settled;
            invalidate();
        });
        confirm.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (!released && listener != null) listener.onLaunch(task);
            }
        });
        confirm.start();
    }

    void playDismissal() {
        if (closing) return;
        closing = true;
        animateProgress(entryProgress, 0f,
                TaskCenterAnimationPolicy.durationMs(155L, animationSpeedIndex), true);
    }

    private void animateProgress(float from, float to, long duration, boolean closeAfter) {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(duration);
        animator.setInterpolator(closeAfter
                ? new DecelerateInterpolator(1.25f)
                : new PathInterpolator(0.16f, 1f, 0.30f, 1f));
        animator.addUpdateListener(value -> {
            entryProgress = (Float) value.getAnimatedValue();
            invalidate();
        });
        if (closeAfter) animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (!released && listener != null) listener.onClosed();
            }
        });
        animator.start();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (closing) return true;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            anchorX = event.getX();
            anchorY = pointerY = event.getY();
            movedForSelection = false;
            selected = -1;
            scrollPosition = 0f;
            displayPosition = 0f;
            lastPointerX = event.getX();
            lastInward = 0f;
            fingerVelocity = 0f;
            lastMoveNanos = 0L;
        } else if (action == MotionEvent.ACTION_MOVE) {
            onExternalMove(event.getX(), event.getY());
        } else if (action == MotionEvent.ACTION_UP) {
            onExternalUp(event.getX(), event.getY(), false);
        } else if (action == MotionEvent.ACTION_CANCEL) {
            onExternalCancel();
        }
        return true;
    }

    void releaseResources() {
        released = true;
        listener = null;
        if (animator != null) animator.cancel();
        if (confirmationAnimator != null) confirmationAnimator.cancel();
        for (RecentTaskPreview task : tasks) {
            Bitmap snapshot = task.snapshot;
            if (snapshot != null && !snapshot.isRecycled()) snapshot.recycle();
        }
        tasks = Collections.emptyList();
        // The blurred wallpaper is owned by the shared process cache.
        wallpaper = null;
    }

    private void updateFingerVelocity(float inward) {
        long now = System.nanoTime();
        if (lastMoveNanos != 0L) {
            float elapsedSeconds = Math.max(0.001f,
                    (now - lastMoveNanos) / 1_000_000_000f);
            float instant = Math.abs(inward - lastInward) / elapsedSeconds;
            fingerVelocity = fingerVelocity * 0.68f + instant * 0.32f;
        }
        lastInward = inward;
        lastMoveNanos = now;
    }

    private float magneticPosition(float rawPosition) {
        float nearest = Math.round(rawPosition);
        float delta = rawPosition - nearest;
        float normalized = clamp(Math.abs(delta) * 2f, 0f, 1f);
        float curved = motionProfile.magnetRetention * normalized
                + (1f - motionProfile.magnetRetention) * normalized * normalized;
        float warpedDelta = Math.signum(delta) * curved * 0.5f;
        float speedRelease = clamp((fingerVelocity - 720f * density)
                / (1_650f * density), 0f, 1f);
        float resolvedDelta = warpedDelta + (delta - warpedDelta) * speedRelease;
        return clamp(nearest + resolvedDelta, 0f, Math.max(0, tasks.size() - 1));
    }

    private float selectionPulse() {
        if (selectionPulseStartNanos == 0L || selected < 0) return 0f;
        float elapsed = (System.nanoTime() - selectionPulseStartNanos) / 1_000_000f;
        float progress = elapsed / TaskCenterAnimationPolicy.durationMs(
                motionProfile.pulseDurationMs, animationSpeedIndex);
        if (progress >= 1f) return 0f;
        float pulse = (float) Math.sin(Math.PI * clamp(progress, 0f, 1f));
        if (motionMode == ConfigContract.SIDE_TASK_MOTION_MARBLE) {
            pulse *= 1f + 0.20f * (float) Math.sin(progress * Math.PI * 3f);
        }
        return Math.max(0f, pulse);
    }

    private static float settleProgress(float progress, float overshoot) {
        float remaining = 1f - progress;
        float base = 1f - remaining * remaining * remaining;
        return base + (float) Math.sin(Math.PI * progress)
                * remaining * overshoot;
    }

    private void loadWallpaper() {
        wallpaper = BlurredWallpaperCache.getOrRequest(getContext(), blurDp,
                wallpaperCallback);
    }

    private static final class MotionProfile {
        final float magnetRetention;
        final float selectedScale;
        final float liftDp;
        final float neighborShiftDp;
        final float pulseScale;
        final float waveShiftDp;
        final float settleOvershoot;
        final long pulseDurationMs;
        final long confirmDurationMs;

        MotionProfile(float magnetRetention, float selectedScale, float liftDp,
                      float neighborShiftDp, float pulseScale, float waveShiftDp,
                      float settleOvershoot, long pulseDurationMs,
                      long confirmDurationMs) {
            this.magnetRetention = magnetRetention;
            this.selectedScale = selectedScale;
            this.liftDp = liftDp;
            this.neighborShiftDp = neighborShiftDp;
            this.pulseScale = pulseScale;
            this.waveShiftDp = waveShiftDp;
            this.settleOvershoot = settleOvershoot;
            this.pulseDurationMs = pulseDurationMs;
            this.confirmDurationMs = confirmDurationMs;
        }

        static MotionProfile forMode(int mode) {
            if (mode == ConfigContract.SIDE_TASK_MOTION_APPLE) {
                return new MotionProfile(0.84f, 0.05f, 4f,
                        3f, 0.008f, 0f, 0f, 120L, 135L);
            }
            if (mode == ConfigContract.SIDE_TASK_MOTION_MARBLE) {
                return new MotionProfile(0.25f, 0.11f, 12f,
                        15f, 0.050f, 18f, 0.18f, 210L, 190L);
            }
            return magnetic();
        }

        static MotionProfile magnetic() {
            return new MotionProfile(0.48f, 0.08f, 8f,
                    9f, 0.026f, 5f, 0.08f, 165L, 165L);
        }
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
