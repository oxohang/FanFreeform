package com.oxohang.fanfreeform.xposed;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;

import java.util.Collections;
import java.util.List;

final class SideWheelOverlayView extends View {
    interface Listener {
        void onLaunch(int index);
        void onDismiss();
        void onSelectionChanged(int index);
    }

    private static final long IDLE_TIMEOUT_MS = 5000L;
    private static final int POSITION_SCALE = 10000;

    private final Paint railPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Path iconClipPath = new Path();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OverScroller scroller;
    private final int touchSlop;
    private final int minimumFlingVelocity;
    private final Runnable timeout = this::dismiss;

    private List<RuntimeTarget> targets = Collections.emptyList();
    private GestureGeometry.Corner side = GestureGeometry.Corner.LEFT;
    private Listener listener;
    private float centerX;
    private float centerY;
    private float rowHeight;
    private float iconDiameter;
    private float hitWidth;
    private float viewportHeight;
    private float position;
    private boolean showNames;
    private boolean showBackdrop;
    private boolean forceCircularIcons = true;
    private float downX;
    private float downY;
    private float lastY;
    private boolean moved;
    private VelocityTracker velocityTracker;
    private ValueAnimator snapAnimator;
    private int lastSelection = -1;
    private int motionGeneration;

    SideWheelOverlayView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        minimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        scroller = new OverScroller(context, new DecelerateInterpolator());

        railPaint.setColor(0xd020263f);
        railPaint.setShadowLayer(dp(16), 0, dp(4), 0x58000000);
        centerPaint.setColor(0x426572f6);
        centerPaint.setStyle(Paint.Style.FILL);
        iconShadowPaint.setColor(0x01000000);
        iconShadowPaint.setShadowLayer(dp(10), 0, dp(3), 0x66000000);
        iconStrokePaint.setStyle(Paint.Style.STROKE);
        iconStrokePaint.setStrokeWidth(dp(1));
        iconStrokePaint.setColor(0x99ffffff);
        pillPaint.setColor(0xf020263f);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(14));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    void setForceCircularIcons(boolean forceCircularIcons) {
        this.forceCircularIcons = forceCircularIcons;
    }

    void configure(List<RuntimeTarget> targets, GestureGeometry.Corner side,
                   float centerX, float centerY, float rowHeight, float iconDiameter,
                   int selected, boolean showNames, boolean showBackdrop,
                   Listener listener) {
        this.targets = targets;
        this.side = side;
        this.centerX = centerX;
        this.centerY = centerY;
        this.rowHeight = rowHeight;
        this.iconDiameter = iconDiameter;
        this.position = Math.max(0, Math.min(targets.size() - 1, selected));
        this.showNames = showNames;
        this.showBackdrop = showBackdrop;
        this.listener = listener;
        hitWidth = Math.max(iconDiameter + dp(32), dp(72));
        viewportHeight = Math.min(targets.size(), 5) * rowHeight;
        lastSelection = GestureGeometry.wheelSelection(position, targets.size());
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        restartTimeout();
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(timeout);
        stopMotion();
        recycleVelocityTracker();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();
        if (action == MotionEvent.ACTION_DOWN) {
            if (!insideWheel(x, y)) {
                dismiss();
                return true;
            }
            stopMotion();
            handler.removeCallbacks(timeout);
            downX = x;
            downY = y;
            lastY = y;
            moved = false;
            recycleVelocityTracker();
            velocityTracker = VelocityTracker.obtain();
            velocityTracker.addMovement(event);
            return true;
        }
        if (velocityTracker != null) velocityTracker.addMovement(event);
        if (action == MotionEvent.ACTION_MOVE) {
            float dy = y - lastY;
            if (!moved && GestureGeometry.distance(downX, downY, x, y) >= touchSlop) {
                moved = true;
            }
            setPositionWithResistance(position - dy / rowHeight);
            lastY = y;
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            if (!moved && Math.abs(y - centerY) <= rowHeight / 2f) {
                recycleVelocityTracker();
                return performClick();
            }
            float velocityY = 0f;
            if (velocityTracker != null) {
                velocityTracker.computeCurrentVelocity(1000);
                velocityY = velocityTracker.getYVelocity();
            }
            recycleVelocityTracker();
            if (moved && Math.abs(velocityY) >= minimumFlingVelocity) {
                fling(velocityY);
            } else {
                snapToNearest();
            }
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            recycleVelocityTracker();
            snapToNearest();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        int index = GestureGeometry.wheelSelection(position, targets.size());
        if (listener != null && index >= 0) listener.onLaunch(index);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (targets.isEmpty()) return;
        float railLeft = centerX - hitWidth / 2f;
        float railRight = centerX + hitWidth / 2f;
        float railTop = centerY - viewportHeight / 2f;
        float railBottom = centerY + viewportHeight / 2f;
        if (showBackdrop) {
            canvas.drawRoundRect(railLeft, railTop, railRight, railBottom,
                    dp(24), dp(24), railPaint);
        }
        canvas.drawRoundRect(railLeft - dp(2), centerY - rowHeight * 0.46f,
                railRight + dp(2), centerY + rowHeight * 0.46f,
                dp(22), dp(22), centerPaint);

        int active = GestureGeometry.wheelSelection(position, targets.size());
        for (int i = 0; i < targets.size(); i++) {
            float distance = i - position;
            float y = centerY + distance * rowHeight;
            if (y < railTop - rowHeight || y > railBottom + rowHeight) continue;
            float absoluteDistance = Math.abs(distance);
            float scale = Math.max(0.72f, 1f - absoluteDistance * 0.12f);
            int alpha = Math.round(255 * Math.max(0.25f, 1f - absoluteDistance * 0.22f));
            float diameter = iconDiameter * scale;
            drawIcon(canvas, targets.get(i), centerX, y, diameter, alpha);
            if (showNames || i == active) {
                drawLabel(canvas, targets.get(i).label, y, diameter,
                        i == active, alpha);
            }
        }
    }

    private void drawIcon(Canvas canvas, RuntimeTarget target, float x, float y,
                          float diameter, int alpha) {
        Drawable drawable = target == null ? null : target.icon;
        float radius = diameter / 2f;
        iconShadowPaint.setAlpha(alpha);
        iconStrokePaint.setAlpha(alpha);
        if (forceCircularIcons) canvas.drawCircle(x, y, radius, iconShadowPaint);
        if (drawable != null) {
            int oldAlpha = drawable.getAlpha();
            Rect oldBounds = drawable.copyBounds();
            int intrinsicWidth = Math.max(1, drawable.getIntrinsicWidth());
            int intrinsicHeight = Math.max(1, drawable.getIntrinsicHeight());
            float targetSize = diameter * (forceCircularIcons ? 1.16f : 0.94f);
            float scale = forceCircularIcons
                    ? Math.max(targetSize / intrinsicWidth, targetSize / intrinsicHeight)
                    : Math.min(targetSize / intrinsicWidth, targetSize / intrinsicHeight);
            int drawWidth = Math.round(intrinsicWidth * scale);
            int drawHeight = Math.round(intrinsicHeight * scale);
            int save = canvas.save();
            if (forceCircularIcons) {
                iconClipPath.reset();
                iconClipPath.addCircle(x, y, radius, Path.Direction.CW);
                canvas.clipPath(iconClipPath);
            }
            drawable.setAlpha(alpha);
            drawable.setBounds(Math.round(x - drawWidth / 2f),
                    Math.round(y - drawHeight / 2f),
                    Math.round(x + drawWidth / 2f),
                    Math.round(y + drawHeight / 2f));
            drawable.draw(canvas);
            canvas.restoreToCount(save);
            drawable.setAlpha(oldAlpha);
            drawable.setBounds(oldBounds);
        }
        if (forceCircularIcons) {
            canvas.drawCircle(x, y, radius - dp(0.5f), iconStrokePaint);
        }
        if (target != null && target.isShortcut()) {
            ShortcutBadgeRenderer.draw(canvas, x, y, diameter, alpha / 255f,
                    getResources().getDisplayMetrics().density);
        }
    }

    private void drawLabel(Canvas canvas, String label, float iconY,
                           float diameter, boolean active, int alpha) {
        float maxTextWidth = Math.min(dp(180), getWidth() * 0.46f);
        CharSequence fitted = TextUtils.ellipsize(label, textPaint,
                maxTextWidth, TextUtils.TruncateAt.END);
        float textWidth = textPaint.measureText(fitted, 0, fitted.length());
        float boxWidth = textWidth + dp(26);
        float boxHeight = dp(36);
        float gap = dp(10);
        float left;
        float right;
        if (side == GestureGeometry.Corner.LEFT) {
            left = centerX + diameter / 2f + gap;
            right = Math.min(getWidth() - dp(10), left + boxWidth);
            left = right - boxWidth;
        } else {
            right = centerX - diameter / 2f - gap;
            left = Math.max(dp(10), right - boxWidth);
            right = left + boxWidth;
        }
        pillPaint.setColor(active ? 0xf020263f : 0xc020263f);
        pillPaint.setAlpha(alpha);
        textPaint.setAlpha(alpha);
        canvas.drawRoundRect(left, iconY - boxHeight / 2f, right,
                iconY + boxHeight / 2f, boxHeight / 2f, boxHeight / 2f, pillPaint);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = iconY - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(fitted, 0, fitted.length(), (left + right) / 2f,
                baseline, textPaint);
        textPaint.setAlpha(255);
        pillPaint.setAlpha(255);
    }

    private boolean insideWheel(float x, float y) {
        return Math.abs(x - centerX) <= hitWidth / 2f
                && Math.abs(y - centerY) <= viewportHeight / 2f;
    }

    private void setPositionWithResistance(float next) {
        float maximum = Math.max(0, targets.size() - 1);
        if (next < 0f) next *= 0.28f;
        else if (next > maximum) next = maximum + (next - maximum) * 0.28f;
        setPosition(next);
    }

    private void setPosition(float next) {
        position = next;
        int selection = GestureGeometry.wheelSelection(position, targets.size());
        if (selection != lastSelection) {
            lastSelection = selection;
            if (listener != null) listener.onSelectionChanged(selection);
        }
        invalidate();
    }

    private void fling(float velocityY) {
        int maximum = Math.max(0, targets.size() - 1) * POSITION_SCALE;
        int start = Math.round(Math.max(0f,
                Math.min(targets.size() - 1, position)) * POSITION_SCALE);
        int velocity = Math.round((-velocityY / rowHeight) * POSITION_SCALE);
        scroller.fling(0, start, 0, velocity, 0, 0, 0, maximum);
        int generation = ++motionGeneration;
        postOnAnimation(() -> continueFling(generation));
    }

    private void continueFling(int generation) {
        if (generation != motionGeneration) return;
        if (!scroller.computeScrollOffset()) {
            snapToNearest();
            return;
        }
        setPosition(scroller.getCurrY() / (float) POSITION_SCALE);
        postOnAnimation(() -> continueFling(generation));
    }

    private void snapToNearest() {
        scroller.abortAnimation();
        float target = GestureGeometry.wheelSelection(position, targets.size());
        if (Math.abs(position - target) < 0.001f) {
            setPosition(target);
            restartTimeout();
            return;
        }
        if (snapAnimator != null) snapAnimator.cancel();
        snapAnimator = ValueAnimator.ofFloat(position, target);
        snapAnimator.setDuration(180L);
        snapAnimator.setInterpolator(new DecelerateInterpolator());
        snapAnimator.addUpdateListener(animation ->
                setPosition((float) animation.getAnimatedValue()));
        snapAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                restartTimeout();
            }
        });
        snapAnimator.start();
    }

    private void stopMotion() {
        motionGeneration++;
        scroller.abortAnimation();
        if (snapAnimator != null) {
            snapAnimator.cancel();
            snapAnimator = null;
        }
    }

    private void restartTimeout() {
        handler.removeCallbacks(timeout);
        handler.postDelayed(timeout, IDLE_TIMEOUT_MS);
    }

    private void dismiss() {
        handler.removeCallbacks(timeout);
        if (listener != null) listener.onDismiss();
    }

    private void recycleVelocityTracker() {
        if (velocityTracker == null) return;
        velocityTracker.recycle();
        velocityTracker = null;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
