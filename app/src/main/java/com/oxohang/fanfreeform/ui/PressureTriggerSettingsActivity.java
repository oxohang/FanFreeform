package com.oxohang.fanfreeform.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.oxohang.fanfreeform.config.AppTarget;
import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;
import com.oxohang.fanfreeform.config.PressureTrigger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("SetTextI18n")
public final class PressureTriggerSettingsActivity extends Activity {
    public static final String EXTRA_TRIGGER_ID = "pressure_trigger_id";
    private static final int REQUEST_PICK_TARGET = 271;

    private ConfigStore store;
    private PressureTrigger trigger;
    private TextView actionSummary;
    private TextView targetSummary;
    private Switch freeformToggle;
    private Switch heavyPressToggle;
    private boolean syncingLaunchSwitches;
    private TriggerPreviewView preview;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        int id = getIntent().getIntExtra(EXTRA_TRIGGER_ID, -1);
        trigger = findTrigger(id);
        if (trigger == null) {
            finish();
            return;
        }
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().setNavigationBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildContent());
    }

    @Override protected void onResume() {
        super.onResume();
        if (trigger != null) {
            trigger = findTrigger(trigger.id);
            if (trigger != null) updateSummaries();
        }
    }

    private View buildContent() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(0xfff4f5fa);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 34));
        scroll.addView(root);
        frame.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout header = row();
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("返回");
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 52)));
        header.addView(text("触发区域 " + trigger.id, 26, Ui.TEXT, Typeface.BOLD));
        root.addView(header);
        TextView subtitle = text("把这个区域当作独立的重压入口，动作不会影响其他触发区域",
                14, Ui.MUTED, Typeface.NORMAL);
        subtitle.setPadding(Ui.dp(this, 44), 0, 0, Ui.dp(this, 16));
        root.addView(subtitle);

        LinearLayout basic = card();
        basic.addView(toggleRow("启用此触发区域", "关闭后只停用这个圆形区域，不影响其他区域"));
        root.addView(basic);

        LinearLayout action = card();
        action.addView(text("单重按动作", 18, Ui.TEXT, Typeface.BOLD));
        LinearLayout actionRow = row();
        actionRow.addView(labels("执行动作", "气压越过校准阈值后立即执行"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        actionSummary = text("", 14, Ui.ACCENT, Typeface.BOLD);
        actionRow.addView(actionSummary);
        actionRow.addView(text("›", 28, Ui.ACCENT, Typeface.NORMAL));
        actionRow.setOnClickListener(view -> showActionDialog());
        action.addView(actionRow);
        LinearLayout targetRow = row();
        targetRow.addView(labels("单一应用或快捷键", "仅在上面的动作选择为此项时生效"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        targetSummary = text("", 13, Ui.ACCENT, Typeface.BOLD);
        targetRow.addView(targetSummary);
        targetRow.addView(text("选择 ›", 13, Ui.ACCENT, Typeface.BOLD));
        targetRow.setOnClickListener(view -> pickSingleTarget());
        action.addView(targetRow);
        root.addView(action, cardParams());

        LinearLayout launch = card();
        launch.addView(text("应用启动", 18, Ui.TEXT, Typeface.BOLD));
        freeformToggle = new Switch(this);
        LinearLayout freeformRow = row();
        freeformRow.addView(labels("普通启动：以小窗打开应用",
                "普通松手选中图标时按此方式启动"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        freeformRow.addView(freeformToggle);
        launch.addView(freeformRow);
        heavyPressToggle = new Switch(this);
        LinearLayout heavyRow = row();
        heavyRow.addView(labels("启用重压启动",
                "选中图标后再次重按，才反转为另一种启动方式；默认关闭"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        heavyRow.addView(heavyPressToggle);
        launch.addView(heavyRow);
        freeformToggle.setOnCheckedChangeListener((button, checked) -> {
            if (syncingLaunchSwitches) return;
            replace(trigger.withLaunchMode(checked, trigger.heavyLaunchEnabled));
            updateLaunchSwitches();
        });
        heavyPressToggle.setOnCheckedChangeListener((button, checked) -> {
            if (syncingLaunchSwitches) return;
            replace(trigger.withLaunchMode(trigger.openAsFreeform, checked));
            updateLaunchSwitches();
        });
        updateLaunchSwitches();
        root.addView(launch, cardParams());

        LinearLayout previewCard = card();
        previewCard.addView(text("触发范围", 18, Ui.TEXT, Typeface.BOLD));
        previewCard.addView(text("可在屏幕百分比范围内移动这个圆形区域",
                13, Ui.MUTED, Typeface.NORMAL));
        preview = new TriggerPreviewView(this);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 230));
        previewParams.topMargin = Ui.dp(this, 10);
        previewCard.addView(preview, previewParams);
        previewCard.addView(slider("圆心水平位置", 0, 100, trigger.centerXPercent,
                value -> replace(trigger.withGeometry(value, trigger.centerYPercent,
                        trigger.radiusPercent)), value -> value + "%"));
        previewCard.addView(slider("圆心垂直位置", 0, 100, trigger.centerYPercent,
                value -> replace(trigger.withGeometry(trigger.centerXPercent, value,
                        trigger.radiusPercent)), value -> value + "%"));
        previewCard.addView(slider("圆形范围半径", ConfigContract.MIN_PRESSURE_RADIUS_PERCENT,
                ConfigContract.MAX_PRESSURE_RADIUS_PERCENT, trigger.radiusPercent,
                value -> replace(trigger.withGeometry(trigger.centerXPercent,
                        trigger.centerYPercent, value)), value -> value + "% 短边"));
        root.addView(previewCard, cardParams());

        Button delete = button("删除此触发区域");
        delete.setTextColor(0xffb14242);
        delete.setOnClickListener(view -> deleteTrigger());
        root.addView(delete, buttonParams(14));
        updateSummaries();
        return frame;
    }

    private String findOverlap(PressureTrigger updated) {
        for (PressureTrigger other : store.getPressureTriggers()) {
            if (other.id == updated.id || !other.enabled || !updated.enabled) continue;
            double distance = Math.hypot(
                    other.centerXPercent - updated.centerXPercent,
                    other.centerYPercent - updated.centerYPercent);
            if (distance < other.radiusPercent + updated.radiusPercent) {
                return "该区域与触发区域 " + other.id + " 重叠；命中优先级按列表顺序";
            }
        }
        return null;
    }

    private PressureTrigger findTrigger(int id) {
        for (PressureTrigger candidate : store.getPressureTriggers()) {
            if (candidate.id == id) return candidate;
        }
        return null;
    }

    private void replace(PressureTrigger updated) {
        trigger = updated;
        String overlap = findOverlap(updated);
        if (overlap != null) Toast.makeText(this, overlap, Toast.LENGTH_SHORT).show();
        ArrayList<PressureTrigger> all = new ArrayList<>(store.getPressureTriggers());
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id == updated.id) {
                all.set(i, updated);
                store.setPressureTriggers(all);
                updateSummaries();
                if (preview != null) preview.invalidate();
                return;
            }
        }
    }

    private void updateSummaries() {
        if (trigger == null) return;
        if (actionSummary != null) actionSummary.setText(
                PressureGestureSettingsActivity.pressureActionLabel(trigger.action));
        if (targetSummary != null) targetSummary.setText(trigger.target == null
                ? "未选择" : targetLabel(trigger.target));
        updateLaunchSwitches();
    }

    private String targetLabel(AppTarget target) {
        if (target.isShortcut()) {
            return target.shortcutLabel == null || target.shortcutLabel.isEmpty()
                    ? "快捷方式" : target.shortcutLabel;
        }
        String packageName = target.packageName();
        if (packageName.isEmpty()) return "应用";
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo application = packageManager.getApplicationInfo(packageName, 0);
            CharSequence label = packageManager.getApplicationLabel(application);
            if (label != null && label.length() > 0) return label.toString();
        } catch (PackageManager.NameNotFoundException ignored) { }
        return packageName;
    }

    private void showActionDialog() {
        String[] choices = {
                "蜂窝应用", "圆形应用快捷选择", "返回主屏幕", "锁屏", "截图", "返回",
                "打开单一应用/快捷键"
        };
        new AlertDialog.Builder(this)
                .setTitle("选择单重按动作")
                .setSingleChoiceItems(choices, trigger.action, (dialog, which) -> {
                    if (which == ConfigContract.PRESSURE_ACTION_SINGLE_TARGET) {
                        replace(trigger.withAction(which, trigger.target));
                        dialog.dismiss();
                        pickSingleTarget();
                    } else {
                        replace(trigger.withAction(which, null));
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void pickSingleTarget() {
        Intent intent = new Intent(this, AppPickerActivity.class);
        intent.putExtra(AppPickerActivity.EXTRA_MODE,
                AppPickerActivity.MODE_APPS_AND_SHORTCUTS);
        startActivityForResult(intent, REQUEST_PICK_TARGET);
    }

    @Override @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_TARGET || resultCode != RESULT_OK || data == null) return;
        try {
            AppTarget target = AppTarget.fromJson(new JSONObject(
                    data.getStringExtra(AppPickerActivity.EXTRA_TARGET)));
            if (target != null) replace(trigger.withAction(
                    ConfigContract.PRESSURE_ACTION_SINGLE_TARGET, target));
        } catch (Exception ignored) {
            Toast.makeText(this, "没有选择可用目标", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLaunchSwitches() {
        if (freeformToggle == null || heavyPressToggle == null) return;
        boolean freeform = trigger.openAsFreeform;
        boolean heavyLaunch = trigger.heavyLaunchEnabled;
        syncingLaunchSwitches = true;
        freeformToggle.setChecked(freeform);
        heavyPressToggle.setChecked(heavyLaunch);
        syncingLaunchSwitches = false;
    }

    private View toggleRow(String title, String subtitle) {
        LinearLayout setting = row();
        setting.addView(labels(title, subtitle), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(trigger.enabled);
        toggle.setOnCheckedChangeListener((button, checked) -> replace(trigger.withEnabled(checked)));
        setting.addView(toggle);
        return setting;
    }

    private View slider(String title, int min, int max, int current,
                        IntConsumer changed, ValueLabel labeler) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, Ui.dp(this, 12), 0, 0);
        LinearLayout header = row();
        header.addView(text(title, 15, Ui.TEXT, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView valueView = text(labeler.label(current), 14, Ui.ACCENT, Typeface.BOLD);
        header.addView(valueView);
        group.addView(header);
        SeekBar seek = new SeekBar(this);
        seek.setMax(Math.max(1, max - min));
        seek.setProgress(Math.max(0, Math.min(seek.getMax(), current - min)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = Math.min(max, min + progress);
                valueView.setText(labeler.label(value));
                if (fromUser) changed.accept(value);
            }
        });
        group.addView(seek);
        return group;
    }

    private void deleteTrigger() {
        List<PressureTrigger> all = store.getPressureTriggers();
        if (all.size() <= 1) {
            Toast.makeText(this, "至少保留一个触发区域；不使用时可以关闭它",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("删除触发区域？")
                .setMessage("删除后这个区域的位置和动作配置都会移除。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    ArrayList<PressureTrigger> remaining = new ArrayList<>();
                    for (PressureTrigger candidate : all) {
                        if (candidate.id != trigger.id) remaining.add(candidate);
                    }
                    int nextId = 1;
                    ArrayList<PressureTrigger> renumbered = new ArrayList<>();
                    for (PressureTrigger candidate : remaining) {
                        renumbered.add(new PressureTrigger(nextId++, candidate.enabled,
                                candidate.centerXPercent, candidate.centerYPercent,
                                candidate.radiusPercent, candidate.action, candidate.target,
                                candidate.openAsFreeform, candidate.heavyLaunchEnabled));
                    }
                    store.setPressureTriggers(renumbered);
                    finish();
                }).show();
    }

    private interface IntConsumer { void accept(int value); }
    private interface ValueLabel { String label(int value); }

    private final class TriggerPreviewView extends View {
        private final Paint screenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        TriggerPreviewView(Context context) {
            super(context);
            screenPaint.setColor(0xfff7f8fc);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(Ui.dp(context, 3));
            PathEffect dash = new DashPathEffect(new float[]{Ui.dp(context, 8), Ui.dp(context, 5)}, 0);
            circlePaint.setPathEffect(dash);
            centerPaint.setColor(0x555a67f2);
        }
        @Override protected void onDraw(Canvas canvas) {
            canvas.drawColor(0xffeef0f8);
            float left = getWidth() * .12f, top = getHeight() * .08f;
            float right = getWidth() * .88f, bottom = getHeight() * .92f;
            canvas.drawRoundRect(left, top, right, bottom, Ui.dp(getContext(), 22),
                    Ui.dp(getContext(), 22), screenPaint);
            float cx = left + (right - left) * trigger.centerXPercent / 100f;
            float cy = top + (bottom - top) * trigger.centerYPercent / 100f;
            float radius = Math.min(right - left, bottom - top)
                    * trigger.radiusPercent / 100f;
            circlePaint.setColor(trigger.enabled ? Ui.ACCENT : 0xff9da3b4);
            centerPaint.setColor(trigger.enabled ? 0x555a67f2 : 0x335f6475);
            canvas.drawCircle(cx, cy, radius, circlePaint);
            canvas.drawCircle(cx, cy, Ui.dp(getContext(), 5), centerPaint);
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16));
        card.setBackground(Ui.rounded(this, Ui.SURFACE, 20));
        card.setElevation(Ui.dp(this, 1));
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Ui.dp(this, 14);
        return params;
    }

    private LinearLayout.LayoutParams buttonParams(int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50));
        params.topMargin = Ui.dp(this, top);
        return params;
    }

    private Button button(String title) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextColor(Ui.ACCENT);
        button.setAllCaps(false);
        button.setBackground(Ui.rounded(this, 0xffeef0ff, 16));
        return button;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        return row;
    }

    private LinearLayout labels(String title, String subtitle) {
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 16, Ui.TEXT, Typeface.BOLD));
        labels.addView(text(subtitle, 13, Ui.MUTED, Typeface.NORMAL));
        return labels;
    }

    private TextView text(String value, float size, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(null, style);
        return text;
    }
}
