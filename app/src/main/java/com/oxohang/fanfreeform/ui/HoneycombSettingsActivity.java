package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;

@SuppressLint("SetTextI18n")
public final class HoneycombSettingsActivity extends Activity {
    private static final String[] SPEEDS = {"很快", "较快", "标准", "柔和", "慢速"};
    private static final String[] INERTIA = {"低", "中", "高"};

    private ConfigStore store;
    private SharedPreferences prefs;
    private TextView targetCountText;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        prefs = store.preferences();
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().setNavigationBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildContent());
    }

    @Override protected void onResume() {
        super.onResume();
        if (targetCountText != null) {
            targetCountText.setText("已选 " + store.getHoneycombTargets().size() + " 项");
        }
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xfff4f5fa);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 36));
        scroll.addView(root);

        LinearLayout header = row();
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 52)));
        header.addView(text("蜂窝应用与动效", 26, Ui.TEXT, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header);
        TextView subtitle = text("底角扇形后继续向内滑，进入 Apple Watch 风格应用总览",
                14, Ui.MUTED, Typeface.NORMAL);
        subtitle.setPadding(Ui.dp(this, 44), 0, 0, Ui.dp(this, 16));
        root.addView(subtitle);

        LinearLayout enableCard = card();
        LinearLayout enableRow = row();
        LinearLayout enableLabels = labels("启用蜂窝总览", "选中应用后关闭已有小窗，再全屏启动");
        enableRow.addView(enableLabels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch enabled = new Switch(this);
        enabled.setChecked(prefs.getBoolean(ConfigContract.KEY_HONEYCOMB_ENABLED,
                ConfigContract.DEFAULT_HONEYCOMB_ENABLED));
        enabled.setOnCheckedChangeListener((button, checked) ->
                store.putBoolean(ConfigContract.KEY_HONEYCOMB_ENABLED, checked));
        enableRow.addView(enabled);
        enableCard.addView(enableRow);
        root.addView(enableCard);

        LinearLayout appsCard = card();
        LinearLayout appsEntry = row();
        appsEntry.setOnClickListener(view -> {
            Intent intent = new Intent(this, TargetManagerActivity.class);
            intent.putExtra(TargetManagerActivity.EXTRA_KIND,
                    TargetManagerActivity.KIND_HONEYCOMB);
            startActivity(intent);
        });
        LinearLayout appLabels = new LinearLayout(this);
        appLabels.setOrientation(LinearLayout.VERTICAL);
        appLabels.addView(text("蜂窝应用", 18, Ui.TEXT, Typeface.BOLD));
        targetCountText = text("已选 " + store.getHoneycombTargets().size() + " 项",
                13, Ui.MUTED, Typeface.NORMAL);
        appLabels.addView(targetCountText);
        appsEntry.addView(appLabels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        appsEntry.addView(text("管理  ›", 14, Ui.ACCENT, Typeface.BOLD));
        appsCard.addView(appsEntry);
        root.addView(appsCard, cardParams());

        LinearLayout behavior = card();
        behavior.addView(text("操作方式", 18, Ui.TEXT, Typeface.BOLD));
        RadioGroup modes = new RadioGroup(this);
        modes.setOrientation(RadioGroup.VERTICAL);
        RadioButton browse = radio("浏览模式 · 松手保持，拖动、缩放、点击启动",
                ConfigContract.HONEYCOMB_MODE_BROWSE);
        RadioButton hold = radio("按住滑选 · 滑过图标，松手立即全屏启动",
                ConfigContract.HONEYCOMB_MODE_HOLD);
        modes.addView(browse);
        modes.addView(hold);
        int mode = prefs.getInt(ConfigContract.KEY_HONEYCOMB_MODE,
                ConfigContract.DEFAULT_HONEYCOMB_MODE);
        modes.check(mode == ConfigContract.HONEYCOMB_MODE_HOLD ? hold.getId() : browse.getId());
        modes.setOnCheckedChangeListener((group, checkedId) -> store.putInt(
                ConfigContract.KEY_HONEYCOMB_MODE,
                checkedId == hold.getId() ? ConfigContract.HONEYCOMB_MODE_HOLD
                        : ConfigContract.HONEYCOMB_MODE_BROWSE));
        behavior.addView(modes);
        behavior.addView(Ui.divider(this));
        LinearLayout blankRow = row();
        blankRow.addView(labels("点击空白关闭", "仅浏览模式生效"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch blank = new Switch(this);
        blank.setChecked(prefs.getBoolean(ConfigContract.KEY_HONEYCOMB_EMPTY_TAP_CLOSE,
                ConfigContract.DEFAULT_HONEYCOMB_EMPTY_TAP_CLOSE));
        blank.setOnCheckedChangeListener((button, checked) -> store.putBoolean(
                ConfigContract.KEY_HONEYCOMB_EMPTY_TAP_CLOSE, checked));
        blankRow.addView(blank);
        behavior.addView(blankRow);
        root.addView(behavior, cardParams());

        LinearLayout tuning = card();
        tuning.addView(text("布局与动效", 18, Ui.TEXT, Typeface.BOLD));
        tuning.addView(toggleRow("跟随手指出现",
                "关闭后使用下方设置的固定位置",
                ConfigContract.KEY_HONEYCOMB_FOLLOW_FINGER,
                ConfigContract.DEFAULT_HONEYCOMB_FOLLOW_FINGER));
        tuning.addView(toggleRow("横屏启用",
                "默认关闭；开启后横屏可从扇形进入蜂窝",
                ConfigContract.KEY_HONEYCOMB_LANDSCAPE_ENABLED,
                ConfigContract.DEFAULT_HONEYCOMB_LANDSCAPE_ENABLED));
        tuning.addView(slider("固定水平位置", ConfigContract.KEY_HONEYCOMB_FIXED_X_PERCENT,
                0, 100, prefs.getInt(ConfigContract.KEY_HONEYCOMB_FIXED_X_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_FIXED_X_PERCENT),
                value -> value + "%"));
        tuning.addView(slider("固定垂直位置", ConfigContract.KEY_HONEYCOMB_FIXED_Y_PERCENT,
                0, 100, prefs.getInt(ConfigContract.KEY_HONEYCOMB_FIXED_Y_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_FIXED_Y_PERCENT),
                value -> value + "%"));
        tuning.addView(slider("应用数量上限", ConfigContract.KEY_HONEYCOMB_MAX_TARGETS,
                1, 60, prefs.getInt(ConfigContract.KEY_HONEYCOMB_MAX_TARGETS, 36),
                value -> value + " 个"));
        tuning.addView(slider("蜂窝触发距离", ConfigContract.KEY_HONEYCOMB_TRIGGER_DP,
                ConfigContract.MIN_HONEYCOMB_TRIGGER_DP,
                ConfigContract.MAX_HONEYCOMB_TRIGGER_DP,
                prefs.getInt(ConfigContract.KEY_HONEYCOMB_TRIGGER_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_TRIGGER_DP), value -> value + "dp"));
        tuning.addView(slider("基础图标大小", ConfigContract.KEY_HONEYCOMB_ICON_SIZE_DP,
                ConfigContract.MIN_HONEYCOMB_ICON_SIZE_DP,
                ConfigContract.MAX_HONEYCOMB_ICON_SIZE_DP,
                prefs.getInt(ConfigContract.KEY_HONEYCOMB_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_ICON_SIZE_DP), value -> value + "dp"));
        tuning.addView(slider("蜂窝圆盘大小", ConfigContract.KEY_HONEYCOMB_DISC_SIZE_PERCENT,
                ConfigContract.MIN_HONEYCOMB_DISC_SIZE_PERCENT,
                ConfigContract.MAX_HONEYCOMB_DISC_SIZE_PERCENT,
                prefs.getInt(ConfigContract.KEY_HONEYCOMB_DISC_SIZE_PERCENT,
                        ConfigContract.DEFAULT_HONEYCOMB_DISC_SIZE_PERCENT),
                value -> value + "% 屏幕宽度"));
        tuning.addView(slider("蜂窝间距", ConfigContract.KEY_HONEYCOMB_SPACING_DP,
                ConfigContract.MIN_HONEYCOMB_SPACING_DP,
                ConfigContract.MAX_HONEYCOMB_SPACING_DP,
                prefs.getInt(ConfigContract.KEY_HONEYCOMB_SPACING_DP,
                        ConfigContract.DEFAULT_HONEYCOMB_SPACING_DP), value -> value + "dp"));
        tuning.addView(slider("动画速度", ConfigContract.KEY_HONEYCOMB_ANIMATION_SPEED,
                0, 4, prefs.getInt(ConfigContract.KEY_HONEYCOMB_ANIMATION_SPEED,
                        ConfigContract.DEFAULT_HONEYCOMB_ANIMATION_SPEED), value -> SPEEDS[value]));
        tuning.addView(slider("惯性强度", ConfigContract.KEY_HONEYCOMB_INERTIA,
                0, 2, prefs.getInt(ConfigContract.KEY_HONEYCOMB_INERTIA,
                        ConfigContract.DEFAULT_HONEYCOMB_INERTIA), value -> INERTIA[value]));
        tuning.addView(slider("中心放大", ConfigContract.KEY_HONEYCOMB_CENTER_SCALE,
                ConfigContract.MIN_HONEYCOMB_CENTER_SCALE,
                ConfigContract.MAX_HONEYCOMB_CENTER_SCALE,
                prefs.getInt(ConfigContract.KEY_HONEYCOMB_CENTER_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_CENTER_SCALE), value -> value + "%"));
        tuning.addView(slider("边缘缩小", ConfigContract.KEY_HONEYCOMB_EDGE_SCALE,
                ConfigContract.MIN_HONEYCOMB_EDGE_SCALE,
                ConfigContract.MAX_HONEYCOMB_EDGE_SCALE,
                prefs.getInt(ConfigContract.KEY_HONEYCOMB_EDGE_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_EDGE_SCALE), value -> value + "%"));
        tuning.addView(slider("选中放大", ConfigContract.KEY_HONEYCOMB_SELECTION_SCALE,
                ConfigContract.MIN_HONEYCOMB_SELECTION_SCALE,
                ConfigContract.MAX_HONEYCOMB_SELECTION_SCALE,
                prefs.getInt(ConfigContract.KEY_HONEYCOMB_SELECTION_SCALE,
                        ConfigContract.DEFAULT_HONEYCOMB_SELECTION_SCALE), value -> value + "%"));
        tuning.addView(toggleRow("显示当前应用名称",
                "按住滑选时固定显示在蜂窝圆盘上方",
                ConfigContract.KEY_HONEYCOMB_SHOW_SELECTED_NAME,
                ConfigContract.DEFAULT_HONEYCOMB_SHOW_SELECTED_NAME));
        LinearLayout blackRow = row();
        blackRow.addView(labels("使用纯黑背景", "默认关闭，使用壁纸高斯模糊"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch black = new Switch(this);
        black.setChecked(prefs.getInt(ConfigContract.KEY_HONEYCOMB_BACKGROUND_STYLE, 0) == 1);
        black.setOnCheckedChangeListener((button, checked) -> store.putInt(
                ConfigContract.KEY_HONEYCOMB_BACKGROUND_STYLE, checked ? 1 : 0));
        blackRow.addView(black);
        tuning.addView(blackRow);
        tuning.addView(slider("壁纸模糊强度", ConfigContract.KEY_HONEYCOMB_BLUR_DP,
                0, 60, prefs.getInt(ConfigContract.KEY_HONEYCOMB_BLUR_DP, 36),
                value -> value + "dp"));
        tuning.addView(slider("背景压暗", ConfigContract.KEY_HONEYCOMB_DIM_PERCENT,
                0, 60, prefs.getInt(ConfigContract.KEY_HONEYCOMB_DIM_PERCENT, 22),
                value -> value + "%"));
        Button reset = compactButton("恢复 Apple 风格默认值");
        reset.setOnClickListener(view -> {
            store.resetHoneycombTuning();
            recreate();
        });
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48));
        resetParams.topMargin = Ui.dp(this, 14);
        tuning.addView(reset, resetParams);
        root.addView(tuning, cardParams());
        return scroll;
    }

    private View slider(String title, String key, int min, int max, int current,
                        ValueLabel valueLabel) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, Ui.dp(this, 14), 0, 0);
        LinearLayout header = row();
        TextView label = text(title, 15, Ui.TEXT, Typeface.BOLD);
        TextView value = text(valueLabel.label(current), 14, Ui.ACCENT, Typeface.BOLD);
        header.addView(label, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(value);
        group.addView(header);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(Math.max(0, Math.min(max - min, current - min)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override public void onProgressChanged(SeekBar seekBar, int progress,
                                                    boolean fromUser) {
                int resolved = min + progress;
                value.setText(valueLabel.label(resolved));
                if (fromUser) store.putInt(key, resolved);
            }
        });
        group.addView(seek);
        return group;
    }

    private RadioButton radio(String title, int id) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setTag(id);
        button.setText(title);
        button.setTextColor(Ui.TEXT);
        button.setTextSize(15);
        button.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        return button;
    }

    private LinearLayout labels(String title, String subtitle) {
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 16, Ui.TEXT, Typeface.BOLD));
        labels.addView(text(subtitle, 13, Ui.MUTED, Typeface.NORMAL));
        return labels;
    }

    private View toggleRow(String title, String subtitle, String key, boolean defaultValue) {
        LinearLayout setting = row();
        setting.addView(labels(title, subtitle), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(key, defaultValue));
        toggle.setOnCheckedChangeListener((button, checked) -> store.putBoolean(key, checked));
        setting.addView(toggle);
        return setting;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 17), Ui.dp(this, 18), Ui.dp(this, 17));
        card.setBackground(Ui.rounded(this, Color.WHITE, 20));
        card.setElevation(Ui.dp(this, 1));
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Ui.dp(this, 14);
        return params;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        return row;
    }

    private TextView text(String value, float size, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(null, style);
        return text;
    }

    private Button compactButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(13);
        button.setTextColor(Ui.ACCENT);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        button.setBackground(Ui.rounded(this, 0xffeef0ff, 12));
        return button;
    }

    private interface ValueLabel { String label(int value); }
}
