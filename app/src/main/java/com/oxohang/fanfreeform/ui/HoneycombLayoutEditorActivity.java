package com.oxohang.fanfreeform.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.oxohang.fanfreeform.config.AppTarget;
import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;
import com.oxohang.fanfreeform.config.HoneycombSlotLayout;
import com.oxohang.fanfreeform.config.ShortcutIconLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("SetTextI18n")
public final class HoneycombLayoutEditorActivity extends Activity {
    private final ArrayList<AppTarget> targets = new ArrayList<>();
    private final ArrayList<Drawable> icons = new ArrayList<>();
    private final ExecutorService iconLoader = Executors.newSingleThreadExecutor();
    private ConfigStore store;
    private HoneycombEditorView preview;
    private TextView editButton;
    private boolean editing;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        targets.addAll(store.getHoneycombTargets());
        Drawable fallback = getApplicationInfo().loadIcon(getPackageManager());
        for (int index = 0; index < targets.size(); index++) icons.add(fallback);

        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().setNavigationBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildContent());
        loadIcons();
    }

    @Override protected void onDestroy() {
        iconLoader.shutdownNow();
        super.onDestroy();
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20),
                Ui.dp(this, 24));
        root.setBackgroundColor(0xfff4f5fa);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("返回");
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 52)));
        header.addView(text("蜂窝位置", 26, Ui.TEXT, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        editButton = text("编辑", 15, Ui.ACCENT, Typeface.BOLD);
        editButton.setGravity(Gravity.CENTER);
        editButton.setBackground(Ui.rounded(this, Color.WHITE, 16));
        editButton.setOnClickListener(view -> setEditing(!editing));
        header.addView(editButton, new LinearLayout.LayoutParams(
                Ui.dp(this, 66), Ui.dp(this, 42)));
        root.addView(header);

        TextView hint = text("排列与实际蜂窝一致。点击编辑后，长按图标并拖到另一个格子即可交换。",
                14, Ui.MUTED, Typeface.NORMAL);
        hint.setPadding(Ui.dp(this, 44), 0, Ui.dp(this, 8), Ui.dp(this, 14));
        root.addView(hint);

        SharedPreferences prefs = store.preferences();
        float iconSize = prefs.getInt(ConfigContract.KEY_HONEYCOMB_ICON_SIZE_DP,
                ConfigContract.DEFAULT_HONEYCOMB_ICON_SIZE_DP);
        float spacing = prefs.getInt(ConfigContract.KEY_HONEYCOMB_SPACING_DP,
                ConfigContract.DEFAULT_HONEYCOMB_SPACING_DP);
        preview = new HoneycombEditorView(this);
        preview.setBackground(Ui.rounded(this, 0xff15161b, 24));
        preview.setItems(icons, iconSize, spacing);
        preview.setOnSwapListener((from, to) -> {
            HoneycombSlotLayout.swap(targets, from, to);
            HoneycombSlotLayout.swap(icons, from, to);
            store.setHoneycombTargets(targets);
            preview.setItems(icons, iconSize, spacing);
        });
        root.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    private void setEditing(boolean editing) {
        this.editing = editing;
        editButton.setText(editing ? "完成" : "编辑");
        editButton.setTextColor(editing ? Color.WHITE : Ui.ACCENT);
        editButton.setBackground(Ui.rounded(this, editing ? Ui.ACCENT : Color.WHITE, 16));
        preview.setEditing(editing);
    }

    private void loadIcons() {
        ArrayList<AppTarget> snapshot = new ArrayList<>(targets);
        iconLoader.execute(() -> {
            ArrayList<Drawable> loaded = new ArrayList<>(snapshot.size());
            for (AppTarget target : snapshot) loaded.add(resolveIcon(target));
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (!snapshot.equals(targets)) {
                    loadIcons();
                    return;
                }
                icons.clear();
                icons.addAll(loaded);
                preview.setItems(icons,
                        store.preferences().getInt(ConfigContract.KEY_HONEYCOMB_ICON_SIZE_DP,
                                ConfigContract.DEFAULT_HONEYCOMB_ICON_SIZE_DP),
                        store.preferences().getInt(ConfigContract.KEY_HONEYCOMB_SPACING_DP,
                                ConfigContract.DEFAULT_HONEYCOMB_SPACING_DP));
            });
        });
    }

    private Drawable resolveIcon(AppTarget target) {
        PackageManager manager = getPackageManager();
        try {
            if (target.isShortcut()) {
                return ShortcutIconLoader.load(this, target.packageName(), target.shortcutId,
                        target.userId);
            }
            if (target.componentName() != null) {
                ActivityInfo info = manager.getActivityInfo(target.componentName(), 0);
                return info.loadIcon(manager);
            }
        } catch (Throwable ignored) { }
        return getApplicationInfo().loadIcon(manager);
    }

    private TextView text(String value, float sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private static final class HoneycombEditorView extends View {
        interface OnSwapListener { void onSwap(int from, int to); }

        private final float density;
        private final Paint platePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint slotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Rect oldBounds = new Rect();
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final int touchSlop;
        private List<Drawable> icons = Collections.emptyList();
        private List<HoneycombSlotLayout.Point> points = Collections.emptyList();
        private float configuredIconDp;
        private float configuredSpacingDp;
        private float centerX;
        private float centerY;
        private float scale = 1f;
        private float iconRadius;
        private boolean editing;
        private int downIndex = -1;
        private int dragIndex = -1;
        private int targetIndex = -1;
        private float downX;
        private float downY;
        private float dragX;
        private float dragY;
        private OnSwapListener listener;
        private final Runnable beginDrag = () -> {
            if (!editing || downIndex < 0) return;
            dragIndex = downIndex;
            targetIndex = downIndex;
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            invalidate();
        };

        HoneycombEditorView(android.content.Context context) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;
            touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            platePaint.setColor(0xff272930);
            slotPaint.setColor(0x66ffffff);
            slotPaint.setStyle(Paint.Style.STROKE);
            slotPaint.setStrokeWidth(1.5f * density);
            targetPaint.setColor(0xff7783ff);
            targetPaint.setStyle(Paint.Style.STROKE);
            targetPaint.setStrokeWidth(3f * density);
            setMinimumHeight(Math.round(360f * density));
        }

        void setItems(List<Drawable> values, float iconDp, float spacingDp) {
            icons = new ArrayList<>(values);
            configuredIconDp = iconDp;
            configuredSpacingDp = spacingDp;
            recalculate();
            invalidate();
        }

        void setEditing(boolean value) {
            editing = value;
            if (!value) cancelDrag();
            invalidate();
        }

        void setOnSwapListener(OnSwapListener value) { listener = value; }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            recalculate();
        }

        private void recalculate() {
            float pitch = Math.max(configuredIconDp + 4f, configuredSpacingDp) * density;
            points = HoneycombSlotLayout.compactPoints(icons.size(), pitch);
            centerX = getWidth() * 0.5f;
            centerY = getHeight() * 0.5f;
            float maxX = 0f;
            float maxY = 0f;
            for (HoneycombSlotLayout.Point point : points) {
                maxX = Math.max(maxX, Math.abs(point.x));
                maxY = Math.max(maxY, Math.abs(point.y));
            }
            float rawRadius = configuredIconDp * density * 0.5f;
            float fitX = (getWidth() - 28f * density) / Math.max(1f, maxX * 2f + rawRadius * 2f);
            float fitY = (getHeight() - 28f * density) / Math.max(1f, maxY * 2f + rawRadius * 2f);
            scale = Math.min(1f, Math.max(0.12f, Math.min(fitX, fitY)));
            iconRadius = rawRadius * scale;
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (int index = 0; index < points.size(); index++) {
                HoneycombSlotLayout.Point point = points.get(index);
                float x = centerX + point.x * scale;
                float y = centerY + point.y * scale;
                if (editing) canvas.drawCircle(x, y, iconRadius + 4f * density, slotPaint);
                if (index == dragIndex) continue;
                drawIcon(canvas, icons.get(index), x, y, iconRadius, 255);
            }
            if (targetIndex >= 0 && targetIndex < points.size() && dragIndex >= 0) {
                HoneycombSlotLayout.Point target = points.get(targetIndex);
                canvas.drawCircle(centerX + target.x * scale, centerY + target.y * scale,
                        iconRadius + 7f * density, targetPaint);
            }
            if (dragIndex >= 0 && dragIndex < icons.size()) {
                drawIcon(canvas, icons.get(dragIndex), dragX, dragY,
                        iconRadius * 1.12f, 235);
            }
        }

        private void drawIcon(Canvas canvas, Drawable drawable, float x, float y,
                              float radius, int alpha) {
            canvas.drawCircle(x, y, radius, platePaint);
            if (drawable == null) return;
            oldBounds.set(drawable.getBounds());
            int inset = Math.round(radius * 0.10f);
            drawable.setBounds(Math.round(x - radius) + inset, Math.round(y - radius) + inset,
                    Math.round(x + radius) - inset, Math.round(y + radius) - inset);
            int oldAlpha = drawable.getAlpha();
            drawable.setAlpha(alpha);
            drawable.draw(canvas);
            drawable.setAlpha(oldAlpha);
            drawable.setBounds(oldBounds);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (!editing || points.isEmpty()) return true;
            float x = event.getX();
            float y = event.getY();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = dragX = x;
                    downY = dragY = y;
                    downIndex = nearestAt(x, y, iconRadius * 1.25f);
                    if (downIndex >= 0) handler.postDelayed(beginDrag, 360L);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    dragX = x;
                    dragY = y;
                    if (dragIndex < 0 && Math.hypot(x - downX, y - downY) > touchSlop) {
                        handler.removeCallbacks(beginDrag);
                        downIndex = -1;
                    } else if (dragIndex >= 0) {
                        int next = nearestAt(x, y, Math.max(iconRadius * 1.45f,
                                configuredSpacingDp * density * scale * 0.58f));
                        if (next != targetIndex) {
                            targetIndex = next;
                            if (next >= 0) {
                                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                            }
                        }
                        getParent().requestDisallowInterceptTouchEvent(true);
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    handler.removeCallbacks(beginDrag);
                    if (dragIndex >= 0 && targetIndex >= 0 && dragIndex != targetIndex
                            && listener != null) listener.onSwap(dragIndex, targetIndex);
                    cancelDrag();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    cancelDrag();
                    return true;
                default:
                    return true;
            }
        }

        private int nearestAt(float x, float y, float radius) {
            ArrayList<HoneycombSlotLayout.Point> screen = new ArrayList<>(points.size());
            for (HoneycombSlotLayout.Point point : points) {
                screen.add(new HoneycombSlotLayout.Point(centerX + point.x * scale,
                        centerY + point.y * scale));
            }
            return HoneycombSlotLayout.nearest(screen, x, y, radius);
        }

        private void cancelDrag() {
            handler.removeCallbacks(beginDrag);
            downIndex = -1;
            dragIndex = -1;
            targetIndex = -1;
            invalidate();
        }
    }
}
