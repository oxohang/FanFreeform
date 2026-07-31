package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;

public final class DashboardActivity extends Activity {
    private ConfigStore store;
    private SharedPreferences prefs;
    private TextView status;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        prefs = store.preferences();
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().setNavigationBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(content());
    }

    @Override protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private View content() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xfff4f5fa);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 36));
        scroll.addView(root);

        root.addView(text("Hyper手势", 30, Ui.TEXT, Typeface.BOLD));
        TextView version = text("HyperOS 3 快捷小窗 · " + versionLabel(),
                14, Ui.MUTED, Typeface.NORMAL);
        version.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 16));
        root.addView(version);

        LinearLayout statusCard = card();
        statusCard.addView(text("Hook 状态", 13, Ui.MUTED, Typeface.BOLD));
        status = text("", 16, Ui.TEXT, Typeface.BOLD);
        status.setPadding(0, Ui.dp(this, 6), 0, 0);
        statusCard.addView(status);
        root.addView(statusCard, params(0));

        LinearLayout enabled = card();
        LinearLayout enabledRow = row();
        LinearLayout labels = labels("启用手势", "总开关、震动、图标形状与桌面入口");
        labels.setOnClickListener(view -> open(GeneralSettingsActivity.class));
        enabledRow.addView(labels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView arrow = text("设置  ›", 14, Ui.ACCENT, Typeface.BOLD);
        arrow.setOnClickListener(view -> open(GeneralSettingsActivity.class));
        enabledRow.addView(arrow);
        Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(ConfigContract.KEY_ENABLED,
                ConfigContract.DEFAULT_ENABLED));
        toggle.setOnCheckedChangeListener((button, checked) ->
                store.putBoolean(ConfigContract.KEY_ENABLED, checked));
        enabledRow.addView(toggle);
        enabled.addView(enabledRow);
        root.addView(enabled, params(14));

        root.addView(entry("底角斜滑", "扇形小窗、二段蜂窝与动效",
                () -> openGesture(GestureSettingsActivity.MODE_BOTTOM)), params(12));
        root.addView(entry("侧滑手势", "方向、布局、应用来源与启动方式",
                () -> openGesture(GestureSettingsActivity.MODE_SIDE)), params(12));
        root.addView(entry("蜂窝应用", "应用清单、圆盘位置、背景与独立动效",
                () -> open(HoneycombSettingsActivity.class)), params(12));
        root.addView(entry("任务中心", "任务数量、卡片布局与独立动效",
                () -> open(TaskCenterSettingsActivity.class)), params(12));
        root.addView(entry("小窗位置设置", "统一调整小窗大小和初始位置",
                () -> open(WindowSettingsActivity.class)), params(12));
        root.addView(entry("窗外点击设置", "四个方向统一的单击和双击动作",
                () -> open(OutsideSettingsActivity.class)), params(12));
        root.addView(entry("杂项设置", "一键热重载、恢复默认与诊断信息",
                () -> open(MiscSettingsActivity.class)), params(12));
        Ui.addGlobalResetOption(this, root, store::resetTuning);
        return scroll;
    }

    private View entry(String title, String subtitle, Runnable click) {
        LinearLayout card = card();
        LinearLayout line = row();
        line.setOnClickListener(view -> click.run());
        line.addView(labels(title, subtitle), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        line.addView(text("›", 28, Ui.ACCENT, Typeface.NORMAL));
        card.addView(line);
        return card;
    }

    private void updateStatus() {
        if (status == null) return;
        String value = prefs.getString(ConfigContract.KEY_INTERFACE_STATUS, "");
        long time = prefs.getLong(ConfigContract.KEY_INTERFACE_TIME, 0L);
        if (value == null || value.isEmpty()) {
            status.setText("等待 SystemUI 与模块连接");
            status.setTextColor(0xffa16c20);
        } else {
            CharSequence relative = time <= 0 ? "" : " · " + DateUtils.getRelativeTimeSpanString(
                    time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            status.setText(value + relative);
            status.setTextColor(0xff287d4d);
        }
    }

    private void openGesture(String mode) {
        Intent intent = new Intent(this, GestureSettingsActivity.class);
        intent.putExtra(GestureSettingsActivity.EXTRA_MODE, mode);
        startActivity(intent);
    }

    private void open(Class<? extends Activity> activity) {
        startActivity(new Intent(this, activity));
    }

    private String versionLabel() {
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName + " (" + info.getLongVersionCode() + ")";
        } catch (Exception ignored) {
            return "未知版本";
        }
    }

    private LinearLayout labels(String title, String subtitle) {
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 17, Ui.TEXT, Typeface.BOLD));
        labels.addView(text(subtitle, 13, Ui.MUTED, Typeface.NORMAL));
        return labels;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 10), Ui.dp(this, 18), Ui.dp(this, 10));
        card.setBackground(Ui.rounded(this, Ui.SURFACE, 20));
        card.setElevation(Ui.dp(this, 1));
        return card;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        return row;
    }

    private LinearLayout.LayoutParams params(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(this, top);
        return p;
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
