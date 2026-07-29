package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.TextPaint;
import android.view.View;
import android.view.WindowInsets;

import java.util.Collections;
import java.util.List;

final class FanOverlayView extends View {
    private final Paint backdrop = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint railPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path iconClipPath = new Path();
    private float fanRadius;
    private boolean fixedSevenRows;
    private float iconDiameter;
    private List<RuntimeTarget> targets = Collections.emptyList();
    private GestureGeometry.Corner corner = GestureGeometry.Corner.LEFT;
    private int selected = -1;
    private float pointerX;
    private float pointerY;
    private boolean showBackdrop = true;
    private boolean sideListLayout;
    private boolean sideFanListLayout;
    private boolean sideRingListLayout;
    private float sideListTop;
    private float sideRowHeight;
    private float sideListCenterX;
    private float sideFanRadius;
    private boolean showSideNames;
    private boolean animationsEnabled;
    private int animationSpeed = 100;
    private int revealAmount = 42;
    private int rotationDegrees = 24;
    private int selectionScalePercent = 18;
    private boolean showSelectionRing = true;
    private long revealStartedAt;
    private int animatedSelection = -1;
    private long selectionStartedAt;
    private int confirmationSelection = -1;
    private long confirmationStartedAt;
    private boolean dismissing;
    private long dismissalStartedAt;

    FanOverlayView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        iconShadowPaint.setColor(0x01000000);
        iconShadowPaint.setShadowLayer(dp(9), 0, dp(3), 0x5c000000);
        iconStrokePaint.setStyle(Paint.Style.STROKE);
        iconStrokePaint.setStrokeWidth(dp(1));
        iconStrokePaint.setColor(0x88ffffff);
        selectedPaint.setColor(0xff6572f6);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(dp(3));
        selectedPaint.setShadowLayer(dp(10), 0, dp(3), 0x52000000);
        selectedFillPaint.setColor(0x386572f6);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(14));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        pillPaint.setColor(0xdd20263f);
        railPaint.setColor(0xb820263f);
        railPaint.setShadowLayer(dp(14), 0, dp(4), 0x50000000);
    }

    void configure(List<RuntimeTarget> targets, GestureGeometry.Corner corner,
                   float radius, float iconDiameter, boolean showSelectedName,
                   boolean showBackdrop,
                   boolean animationsEnabled, int animationSpeed,
                   int revealAmount, int rotationDegrees,
                   int selectionScalePercent, boolean showSelectionRing,
                   boolean fixedSevenRows) {
        this.targets = targets;
        this.corner = corner;
        this.fanRadius = radius;
        this.iconDiameter = iconDiameter;
        this.showBackdrop = showBackdrop;
        this.sideListLayout = false;
        this.sideFanListLayout = false;
        this.sideRingListLayout = false;
        this.sideListTop = 0f;
        this.sideRowHeight = 0f;
        this.sideListCenterX = 0f;
        this.showSideNames = showSelectedName;
        this.animationsEnabled = animationsEnabled;
        this.animationSpeed = Math.max(75, Math.min(160, animationSpeed));
        this.revealAmount = Math.max(15, Math.min(130, revealAmount));
        this.rotationDegrees = Math.max(0, Math.min(90, rotationDegrees));
        this.selectionScalePercent = Math.max(5, Math.min(40,
                selectionScalePercent));
        this.showSelectionRing = showSelectionRing;
        this.fixedSevenRows = fixedSevenRows;
        this.revealStartedAt = SystemClock.uptimeMillis();
        this.animatedSelection = -1;
        this.confirmationSelection = -1;
        this.dismissing = false;
        selected = -1;
        updateBackdropShader();
        invalidate();
    }

    void configureSideList(List<RuntimeTarget> targets, GestureGeometry.Corner side,
                           float centerX, float listTop, float rowHeight, float iconDiameter,
                           boolean showNames, boolean showBackdrop,
                           boolean animationsEnabled, int animationSpeed,
                           int revealAmount, int rotationDegrees,
                           int selectionScalePercent, boolean showSelectionRing) {
        this.targets = targets;
        this.corner = side;
        this.fanRadius = 0f;
        this.iconDiameter = iconDiameter;
        this.showBackdrop = showBackdrop;
        this.sideListLayout = true;
        this.sideFanListLayout = false;
        this.sideRingListLayout = false;
        this.sideListTop = listTop;
        this.sideRowHeight = rowHeight;
        this.sideListCenterX = centerX;
        this.showSideNames = showNames;
        this.animationsEnabled = animationsEnabled;
        this.animationSpeed = Math.max(75, Math.min(160, animationSpeed));
        this.revealAmount = Math.max(15, Math.min(130, revealAmount));
        this.rotationDegrees = Math.max(0, Math.min(90, rotationDegrees));
        this.selectionScalePercent = Math.max(5, Math.min(40,
                selectionScalePercent));
        this.showSelectionRing = showSelectionRing;
        this.revealStartedAt = SystemClock.uptimeMillis();
        this.animatedSelection = -1;
        this.confirmationSelection = -1;
        this.dismissing = false;
        selected = -1;
        invalidate();
    }

    void configureSideFanList(List<RuntimeTarget> targets, GestureGeometry.Corner side,
                              float apexX, float centerY, float radius, float iconDiameter,
                              boolean showNames, boolean showBackdrop,
                              boolean animationsEnabled, int animationSpeed,
                              int revealAmount, int rotationDegrees,
                              int selectionScalePercent, boolean showSelectionRing) {
        this.targets = targets;
        this.corner = side;
        this.fanRadius = 0f;
        this.iconDiameter = iconDiameter;
        this.showBackdrop = showBackdrop;
        this.sideListLayout = true;
        this.sideFanListLayout = true;
        this.sideRingListLayout = false;
        this.sideListCenterX = apexX;
        this.sideListTop = centerY;
        this.sideRowHeight = 0f;
        this.sideFanRadius = radius;
        this.showSideNames = showNames;
        this.animationsEnabled = animationsEnabled;
        this.animationSpeed = Math.max(75, Math.min(160, animationSpeed));
        this.revealAmount = Math.max(15, Math.min(130, revealAmount));
        this.rotationDegrees = Math.max(0, Math.min(90, rotationDegrees));
        this.selectionScalePercent = Math.max(5, Math.min(40,
                selectionScalePercent));
        this.showSelectionRing = showSelectionRing;
        this.revealStartedAt = SystemClock.uptimeMillis();
        this.animatedSelection = -1;
        this.confirmationSelection = -1;
        this.dismissing = false;
        selected = -1;
        invalidate();
    }

    void configureSideRingList(List<RuntimeTarget> targets, GestureGeometry.Corner side,
                               float centerX, float centerY, float radius,
                               float iconDiameter, boolean showNames,
                               boolean showBackdrop, boolean animationsEnabled,
                               int animationSpeed, int revealAmount,
                               int rotationDegrees, int selectionScalePercent,
                               boolean showSelectionRing) {
        this.targets = targets;
        this.corner = side;
        this.fanRadius = 0f;
        this.iconDiameter = iconDiameter;
        this.showBackdrop = showBackdrop;
        this.sideListLayout = true;
        this.sideFanListLayout = false;
        this.sideRingListLayout = true;
        this.sideListCenterX = centerX;
        this.sideListTop = centerY;
        this.sideRowHeight = 0f;
        this.sideFanRadius = radius;
        this.showSideNames = showNames;
        this.animationsEnabled = animationsEnabled;
        this.animationSpeed = Math.max(75, Math.min(160, animationSpeed));
        this.revealAmount = Math.max(15, Math.min(130, revealAmount));
        this.rotationDegrees = Math.max(0, Math.min(90, rotationDegrees));
        this.selectionScalePercent = Math.max(5, Math.min(40,
                selectionScalePercent));
        this.showSelectionRing = showSelectionRing;
        this.revealStartedAt = SystemClock.uptimeMillis();
        this.animatedSelection = -1;
        this.confirmationSelection = -1;
        this.dismissing = false;
        selected = -1;
        invalidate();
    }

    void updateSelection(int selected, float x, float y) {
        if (this.selected != selected && selected >= 0) {
            animatedSelection = selected;
            selectionStartedAt = SystemClock.uptimeMillis();
        }
        this.selected = selected;
        pointerX = x;
        pointerY = y;
        invalidate();
    }

    void playConfirmation(int selected) {
        confirmationSelection = selected;
        confirmationStartedAt = SystemClock.uptimeMillis();
        invalidate();
    }

    long confirmationDurationMs() {
        return animationsEnabled ? duration(150L) : 1L;
    }

    boolean playDismissal() {
        if (!animationsEnabled || (sideListLayout && !sideRingListLayout)) return false;
        dismissing = true;
        dismissalStartedAt = SystemClock.uptimeMillis();
        invalidate();
        return true;
    }

    long dismissalDurationMs() {
        return animationsEnabled ? duration(150L) : 1L;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (targets.isEmpty()) return;
        if (sideListLayout) {
            drawSideList(canvas);
            return;
        }
        float originX = corner == GestureGeometry.Corner.LEFT ? 0 : getWidth();
        float drawOriginY = getHeight();
        float radius = fanRadius > 0 ? fanRadius : Math.min(getWidth(), getHeight()) * 0.58f;
        if (showBackdrop) canvas.drawCircle(originX, drawOriginY,
                backdropRadius(radius), backdrop);

        long now = SystemClock.uptimeMillis();
        float revealProgress = animationsEnabled
                ? progress(revealStartedAt, duration(170L), now) : 1f;
        float reveal = animationsEnabled ? easeOutBack(revealProgress) : 1f;
        boolean keepAnimating = revealProgress < 1f;
        float dismissalProgress = dismissing
                ? easeOut(progress(dismissalStartedAt, dismissalDurationMs(), now)) : 0f;
        if (dismissing && dismissalProgress < 1f) keepAnimating = true;
        float startFactor = 1f - revealAmount / 100f;
        for (int i = 0; i < targets.size(); i++) {
            GestureGeometry.Point center = GestureGeometry.iconCenter(corner, i, targets.size(),
                    getWidth(), getHeight(), radius, fixedSevenRows);
            float iconOriginX = corner == GestureGeometry.Corner.LEFT ? 0f : getWidth();
            float iconOriginY = getHeight();
            float revealFactor = startFactor + (1f - startFactor) * reveal;
            revealFactor *= 1f - 0.32f * dismissalProgress;
            float x = iconOriginX + (center.x - iconOriginX) * revealFactor;
            float y = iconOriginY + (center.y - iconOriginY) * revealFactor;
            boolean active = i == selected;
            float pop = selectionPop(i, now);
            if (pop > 1f || revealRotationActive(i, now)) keepAnimating = true;
            float diameter = iconDiameter * (active ? selectionScale() : 1f)
                    * (0.78f + 0.22f * reveal) * pop
                    * (1f - 0.22f * dismissalProgress);
            float iconRadius = diameter / 2f;
            int save = canvas.save();
            canvas.rotate(revealRotation(i, now), x, y);
            canvas.drawCircle(x, y, iconRadius, iconShadowPaint);
            drawCircularIcon(canvas, targets.get(i).icon, x, y, diameter);
            canvas.drawCircle(x, y, iconRadius - dp(0.5f), iconStrokePaint);
            if (active && showSelectionRing) {
                canvas.drawCircle(x, y, iconRadius + dp(6), selectedPaint);
            }
            canvas.restoreToCount(save);
        }

        if (showSideNames && selected >= 0 && selected < targets.size()) {
            String label = targets.get(selected).label;
            float padding = dp(14);
            float width = textPaint.measureText(label) + padding * 2;
            float centerX = Math.max(width / 2 + dp(10),
                    Math.min(getWidth() - width / 2 - dp(10), pointerX));
            float centerY = Math.max(dp(42), pointerY - dp(58));
            canvas.drawRoundRect(centerX - width / 2, centerY - dp(22), centerX + width / 2,
                    centerY + dp(12), dp(17), dp(17), pillPaint);
            canvas.drawText(label, centerX, centerY, textPaint);
        }
        if (keepAnimating) postInvalidateOnAnimation();
    }

    private void drawSideList(Canvas canvas) {
        float railPadding = dp(8);
        float railLeft = sideListCenterX - iconDiameter / 2f - railPadding;
        float railRight = sideListCenterX + iconDiameter / 2f + railPadding;
        float railTop = sideListTop + dp(2);
        float railBottom = sideListTop + targets.size() * sideRowHeight - dp(2);
        if (showBackdrop && !sideFanListLayout && !sideRingListLayout) {
            canvas.drawRoundRect(railLeft, railTop, railRight, railBottom,
                    dp(22), dp(22), railPaint);
        }

        long now = SystemClock.uptimeMillis();
        float revealProgress = animationsEnabled
                ? progress(revealStartedAt, duration(170L), now) : 1f;
        float reveal = animationsEnabled ? easeOutBack(revealProgress) : 1f;
        boolean keepAnimating = revealProgress < 1f;
        float dismissalProgress = dismissing && sideRingListLayout
                ? easeOut(progress(dismissalStartedAt, dismissalDurationMs(), now)) : 0f;
        if (dismissing && dismissalProgress < 1f) keepAnimating = true;
        float startFactor = 1f - revealAmount / 100f;
        int anchor = (targets.size() - 1) / 2;
        float iconOriginX = sideRingListLayout ? sideListCenterX
                : corner == GestureGeometry.Corner.LEFT ? 0f : getWidth();
        float iconOriginY = sideRingListLayout ? sideListTop
                : sideFanListLayout ? sideListTop
                : sideListTop + (anchor + 0.5f) * sideRowHeight;
        for (int i = 0; i < targets.size(); i++) {
            GestureGeometry.Point center = sideRingListLayout
                    ? GestureGeometry.sideRingIconCenter(i, targets.size(),
                    sideListCenterX, sideListTop, sideFanRadius)
                    : sideFanListLayout
                    ? GestureGeometry.sideFanIconCenter(corner, i, targets.size(),
                    getWidth(), sideListCenterX, sideListTop, sideFanRadius)
                    : GestureGeometry.sideListIconCenter(
                    i, sideListCenterX, sideListTop, sideRowHeight);
            float revealFactor = startFactor + (1f - startFactor) * reveal;
            revealFactor *= 1f - 0.38f * dismissalProgress;
            float x = iconOriginX + (center.x - iconOriginX) * revealFactor;
            float y = iconOriginY + (center.y - iconOriginY) * revealFactor;
            boolean active = i == selected;
            float pop = selectionPop(i, now);
            if (selectionAnimationActive(i, now) || revealRotationActive(i, now)) {
                keepAnimating = true;
            }
            float diameter = iconDiameter * (active ? selectionScale() : 1f)
                    * (0.78f + 0.22f * reveal) * pop
                    * (1f - 0.24f * dismissalProgress);
            float radius = diameter / 2f;
            int save = canvas.save();
            canvas.rotate(revealRotation(i, now), x, y);
            if (active && showSelectionRing) {
                canvas.drawCircle(x, y, radius + dp(7), selectedFillPaint);
            }
            canvas.drawCircle(x, y, radius, iconShadowPaint);
            drawCircularIcon(canvas, targets.get(i).icon, x, y, diameter);
            canvas.drawCircle(x, y, radius - dp(0.5f), iconStrokePaint);
            if (active && showSelectionRing) {
                canvas.drawCircle(x, y, radius + dp(5), selectedPaint);
            }
            canvas.restoreToCount(save);
            if (showSideNames && active && !sideRingListLayout) {
                drawSideLabel(canvas, targets.get(i).label, x, y,
                        diameter, active);
            }
        }
        if (showSideNames && sideRingListLayout
                && selected >= 0 && selected < targets.size()) {
            drawFixedRingLabel(canvas, targets.get(selected).label);
        }
        if (keepAnimating) postInvalidateOnAnimation();
    }

    private void drawFixedRingLabel(Canvas canvas, String label) {
        float maxTextWidth = Math.min(dp(190), getWidth() * 0.50f);
        CharSequence fitted = TextUtils.ellipsize(label, textPaint,
                maxTextWidth, TextUtils.TruncateAt.END);
        float textWidth = textPaint.measureText(fitted, 0, fitted.length());
        float padding = dp(13);
        float boxHeight = dp(36);
        float centerX = Math.max(textWidth / 2f + padding + dp(10),
                Math.min(getWidth() - textWidth / 2f - padding - dp(10),
                        sideListCenterX));
        WindowInsets insets = getRootWindowInsets();
        float safeTop = insets == null ? dp(24)
                : insets.getInsets(WindowInsets.Type.statusBars()).top;
        float centerY = Math.max(safeTop + boxHeight / 2f + dp(8),
                sideListTop - sideFanRadius - iconDiameter / 2f - dp(22));
        pillPaint.setColor(0xf020263f);
        canvas.drawRoundRect(centerX - textWidth / 2f - padding,
                centerY - boxHeight / 2f, centerX + textWidth / 2f + padding,
                centerY + boxHeight / 2f, boxHeight / 2f, boxHeight / 2f,
                pillPaint);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = centerY - (metrics.ascent + metrics.descent) / 2f;
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(fitted, 0, fitted.length(), centerX, baseline, textPaint);
    }

    private void drawSideLabel(Canvas canvas, String label, float iconX, float iconY,
                               float diameter, boolean active) {
        float maxTextWidth = Math.min(dp(180), getWidth() * 0.46f);
        CharSequence fitted = TextUtils.ellipsize(label, textPaint,
                maxTextWidth, TextUtils.TruncateAt.END);
        float textWidth = textPaint.measureText(fitted, 0, fitted.length());
        float horizontalPadding = dp(13);
        float gap = dp(10);
        float boxWidth = textWidth + horizontalPadding * 2f;
        float boxHeight = dp(36);
        float left;
        float right;
        if (iconX < getWidth() / 2f) {
            left = iconX + diameter / 2f + gap;
            right = Math.min(getWidth() - dp(10), left + boxWidth);
            left = right - boxWidth;
        } else {
            right = iconX - diameter / 2f - gap;
            left = Math.max(dp(10), right - boxWidth);
            right = left + boxWidth;
        }
        pillPaint.setColor(active ? 0xf020263f : 0xc820263f);
        canvas.drawRoundRect(left, iconY - boxHeight / 2f, right,
                iconY + boxHeight / 2f, boxHeight / 2f, boxHeight / 2f, pillPaint);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = iconY - (metrics.ascent + metrics.descent) / 2f;
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(fitted, 0, fitted.length(), (left + right) / 2f,
                baseline, textPaint);
    }

    private void drawCircularIcon(Canvas canvas, Drawable drawable, float centerX, float centerY,
                                  float diameter) {
        if (drawable == null) return;
        float radius = diameter / 2f;
        int intrinsicWidth = Math.max(1, drawable.getIntrinsicWidth());
        int intrinsicHeight = Math.max(1, drawable.getIntrinsicHeight());
        float target = diameter * 1.16f;
        float scale = Math.max(target / intrinsicWidth, target / intrinsicHeight);
        int drawWidth = Math.round(intrinsicWidth * scale);
        int drawHeight = Math.round(intrinsicHeight * scale);
        Rect old = drawable.copyBounds();
        int save = canvas.save();
        iconClipPath.reset();
        iconClipPath.addCircle(centerX, centerY, radius, Path.Direction.CW);
        canvas.clipPath(iconClipPath);
        drawable.setBounds(Math.round(centerX - drawWidth / 2f),
                Math.round(centerY - drawHeight / 2f),
                Math.round(centerX + drawWidth / 2f),
                Math.round(centerY + drawHeight / 2f));
        drawable.draw(canvas);
        canvas.restoreToCount(save);
        drawable.setBounds(old);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float selectionPop(int index, long now) {
        if (!animationsEnabled) return 1f;
        if (index == confirmationSelection) {
            float p = progress(confirmationStartedAt, duration(150L), now);
            float amount = 0.18f;
            return 1f + amount * (float) Math.sin(Math.PI * p);
        }
        if (index != animatedSelection) return 1f;
        float p = progress(selectionStartedAt, duration(110L), now);
        float amount = 0.10f;
        return 1f + amount * (float) Math.sin(Math.PI * p);
    }

    private float selectionScale() {
        return 1f + selectionScalePercent / 100f;
    }

    private boolean selectionAnimationActive(int index, long now) {
        if (!animationsEnabled) return false;
        if (index == confirmationSelection) {
            return now < confirmationStartedAt + duration(150L);
        }
        return index == animatedSelection
                && now < selectionStartedAt + duration(110L);
    }

    private boolean revealRotationActive(int index, long now) {
        if (!animationsEnabled) return false;
        long start = revealStartedAt + index * revealStaggerMs();
        return now < start + duration(150L);
    }

    private float revealRotation(int index, long now) {
        if (!animationsEnabled) return 0f;
        long start = revealStartedAt + index * revealStaggerMs();
        float p = easeOutBack(progress(start, duration(150L), now));
        if (p >= 1f) return 0f;
        float direction = corner == GestureGeometry.Corner.LEFT ? 1f : -1f;
        return direction * rotationDegrees * (1f - p);
    }

    private long revealStaggerMs() {
        return Math.max(8L, Math.round(12L * 100f / animationSpeed));
    }

    private long duration(long base) {
        return Math.max(45L, Math.round(base * 100f / animationSpeed));
    }

    private static float progress(long startedAt, long duration, long now) {
        if (startedAt <= 0L) return 1f;
        return Math.max(0f, Math.min(1f, (now - startedAt) / (float) Math.max(1L, duration)));
    }

    private static float easeOut(float value) {
        float inverse = 1f - value;
        return 1f - inverse * inverse;
    }

    private static float easeOutBack(float value) {
        float shifted = value - 1f;
        return 1f + 2.70158f * shifted * shifted * shifted
                + 1.70158f * shifted * shifted;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        updateBackdropShader();
    }

    private void updateBackdropShader() {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        float radius = fanRadius > 0
                ? fanRadius : Math.min(getWidth(), getHeight()) * 0.58f;
        float originX = corner == GestureGeometry.Corner.LEFT ? 0 : getWidth();
        float drawOriginY = getHeight();
        backdrop.setShader(new RadialGradient(originX, drawOriginY, backdropRadius(radius),
                new int[]{0x99202742, 0x77202742, 0x00202742},
                new float[]{0f, 0.72f, 1f}, Shader.TileMode.CLAMP));
    }

    private float backdropRadius(float radius) {
        float originX = corner == GestureGeometry.Corner.LEFT ? 0f : getWidth();
        float originY = getHeight();
        float outermostCenter = 0f;
        for (int index = 0; index < targets.size(); index++) {
            GestureGeometry.Point center = GestureGeometry.iconCenter(corner, index,
                    targets.size(), getWidth(), getHeight(), radius, fixedSevenRows);
            outermostCenter = Math.max(outermostCenter,
                    (float) Math.hypot(center.x - originX, center.y - originY));
        }
        if (outermostCenter <= 0f) outermostCenter = radius;
        return outermostCenter + iconDiameter / 2f + dp(35);
    }
}
