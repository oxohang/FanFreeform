package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Button;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;

@SuppressLint("SetTextI18n")
public final class GestureSettingsActivity extends Activity {
    public static final String EXTRA_MODE = "mode";
    public static final String MODE_BOTTOM = "bottom";
    public static final String MODE_SIDE = "side";

    private ConfigStore store;
    private SharedPreferences prefs;
    private boolean sideMode;
    private int hotWidthPercent;
    private int hotHeightPercent;
    private GesturePreviewView gesturePreview;
    private TextView targetCountText;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        sideMode = MODE_SIDE.equals(getIntent().getStringExtra(EXTRA_MODE));
        store = new ConfigStore(this);
        prefs = store.preferences();
        hotWidthPercent = prefs.getInt(ConfigContract.KEY_HOT_WIDTH_PERCENT,
                ConfigContract.DEFAULT_HOT_WIDTH_PERCENT);
        hotHeightPercent = prefs.getInt(ConfigContract.KEY_HOT_HEIGHT_PERCENT,
                ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT);
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().setNavigationBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildContent());
    }

    @Override protected void onResume() {
        super.onResume();
        updateTargetCount();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xfff4f5fa);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 14),
                Ui.dp(this, 20), Ui.dp(this, 36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("返回");
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(
                Ui.dp(this, 44), Ui.dp(this, 52)));
        TextView title = text(sideMode ? "侧滑选择" : "底角斜滑",
                26, Ui.TEXT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header);

        TextView subtitle = text(sideMode
                        ? "短滑保留 HyperOS 返回，长滑呼出所选应用布局"
                        : "贴住左右物理底角，向屏幕内侧斜上方滑动",
                14, Ui.MUTED, Typeface.NORMAL);
        subtitle.setPadding(Ui.dp(this, 44), 0, 0, Ui.dp(this, 16));
        root.addView(subtitle);

        LinearLayout card = card();
        if (sideMode) buildSideSettings(card);
        else buildBottomSettings(card);
        root.addView(card);
        return scroll;
    }

    private void buildBottomSettings(LinearLayout card) {
        card.addView(text("底角触发范围", 18, Ui.TEXT, Typeface.BOLD));
        card.addView(targetManagerRow("小窗应用", TargetManagerActivity.KIND_FAN));
        card.addView(Ui.divider(this));
        card.addView(booleanRow("竖屏启用", "竖屏时允许底角斜滑",
                ConfigContract.KEY_BOTTOM_PORTRAIT_ENABLED,
                ConfigContract.DEFAULT_BOTTOM_PORTRAIT_ENABLED));
        card.addView(booleanRow("横屏启用", "横屏时允许底角斜滑",
                ConfigContract.KEY_BOTTOM_LANDSCAPE_ENABLED,
                ConfigContract.DEFAULT_BOTTOM_LANDSCAPE_ENABLED));
        card.addView(booleanRow("二段蜂窝小窗启动",
                "开启后从底角二段蜂窝选中的应用以小窗打开；关闭时全屏打开",
                ConfigContract.KEY_BOTTOM_HONEYCOMB_FREEFORM,
                ConfigContract.DEFAULT_BOTTOM_HONEYCOMB_FREEFORM));
        card.addView(booleanRow("每排固定 7 个",
                "关闭时使用智能排布；开启后按 7+1、7+2 逐排填充",
                ConfigContract.KEY_FAN_FIXED_SEVEN_ROWS,
                ConfigContract.DEFAULT_FAN_FIXED_SEVEN_ROWS));
        card.addView(Ui.divider(this));
        gesturePreview = new GesturePreviewView(this);
        gesturePreview.update(hotWidthPercent, hotHeightPercent);
        card.addView(gesturePreview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 160)));
        card.addView(slider("触发区宽度", ConfigContract.KEY_HOT_WIDTH_PERCENT,
                5, 20, hotWidthPercent, value -> value + "%"));
        card.addView(slider("触发区高度", ConfigContract.KEY_HOT_HEIGHT_PERCENT,
                3, ConfigContract.MAX_HOT_HEIGHT_PERCENT, hotHeightPercent,
                value -> value + "%"));
        card.addView(slider("触发距离", ConfigContract.KEY_TRIGGER_PERCENT,
                6, 24, prefs.getInt(ConfigContract.KEY_TRIGGER_PERCENT,
                        ConfigContract.DEFAULT_TRIGGER_PERCENT), value -> value + "%"));
        card.addView(slider("选择距离", ConfigContract.KEY_SELECTION_RADIUS_PERCENT,
                ConfigContract.MIN_SELECTION_RADIUS_PERCENT,
                ConfigContract.MAX_SELECTION_RADIUS_PERCENT,
                prefs.getInt(ConfigContract.KEY_SELECTION_RADIUS_PERCENT,
                        ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT), value -> value + "%"));
        card.addView(slider("图标大小", ConfigContract.KEY_ICON_SIZE_DP,
                34, 64, prefs.getInt(ConfigContract.KEY_ICON_SIZE_DP,
                ConfigContract.DEFAULT_ICON_SIZE_DP), value -> value + "dp"));
        card.addView(slider("扇形目标上限", ConfigContract.KEY_FAN_MAX_TARGETS,
                3, 24, prefs.getInt(ConfigContract.KEY_FAN_MAX_TARGETS, 8),
                value -> value + " 个"));
        Button animation = new Button(this);
        animation.setText("扇形呼出与选择动效");
        animation.setOnClickListener(view -> startActivity(
                new Intent(this, AnimationSettingsActivity.class)));
        card.addView(animation);
        card.addView(Ui.divider(this));
        card.addView(selectedNameRow());
        TextView note = text("底角区域始终从屏幕物理边缘开始，不再为侧滑手势向内避让。",
                13, Ui.MUTED, Typeface.NORMAL);
        note.setPadding(0, Ui.dp(this, 10), 0, 0);
        card.addView(note);
    }

    private void buildSideSettings(LinearLayout card) {
        card.addView(text("侧滑参数", 18, Ui.TEXT, Typeface.BOLD));
        card.addView(booleanRow("启用侧滑手势", "短滑仍保留 HyperOS 原生返回",
                ConfigContract.KEY_SIDE_GESTURE_ENABLED,
                ConfigContract.DEFAULT_SIDE_GESTURE_ENABLED));
        card.addView(booleanRow("竖屏启用", "竖屏时允许侧滑选择",
                ConfigContract.KEY_SIDE_PORTRAIT_ENABLED,
                ConfigContract.DEFAULT_SIDE_PORTRAIT_ENABLED));
        card.addView(booleanRow("横屏启用", "横屏时允许侧滑选择",
                ConfigContract.KEY_SIDE_LANDSCAPE_ENABLED,
                ConfigContract.DEFAULT_SIDE_LANDSCAPE_ENABLED));
        card.addView(Ui.divider(this));
        card.addView(booleanRow("横向向内", "允许水平长滑",
                ConfigContract.KEY_SIDE_DIRECTION_HORIZONTAL, true));
        card.addView(booleanRow("斜向上", "允许向内斜上长滑",
                ConfigContract.KEY_SIDE_DIRECTION_UP, true));
        card.addView(booleanRow("斜向下", "允许向内斜下长滑",
                ConfigContract.KEY_SIDE_DIRECTION_DOWN, true));
        card.addView(Ui.divider(this));
        card.addView(targetManagerRow("侧滑应用", TargetManagerActivity.KIND_SIDE));
        card.addView(slider("侧滑应用上限", ConfigContract.KEY_SIDE_MAX_TARGETS,
                1, 36, prefs.getInt(ConfigContract.KEY_SIDE_MAX_TARGETS, 12),
                value -> value + " 个"));
        card.addView(sideLayoutModeRow());
        card.addView(Ui.divider(this));
        card.addView(slider("长滑触发距离", ConfigContract.KEY_SIDE_TRIGGER_PERCENT,
                18, 50, prefs.getInt(ConfigContract.KEY_SIDE_TRIGGER_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TRIGGER_PERCENT), value -> value + "%"));
        card.addView(slider("侧滑图标大小", ConfigContract.KEY_SIDE_ICON_SIZE_DP,
                34, 64, prefs.getInt(ConfigContract.KEY_SIDE_ICON_SIZE_DP,
                        prefs.getInt(ConfigContract.KEY_ICON_SIZE_DP,
                                ConfigContract.DEFAULT_SIDE_ICON_SIZE_DP)),
                value -> value + "dp"));
        card.addView(slider("环形大小", ConfigContract.KEY_SIDE_RING_SIZE_PERCENT,
                ConfigContract.MIN_SIDE_RING_SIZE_PERCENT,
                ConfigContract.MAX_SIDE_RING_SIZE_PERCENT,
                prefs.getInt(ConfigContract.KEY_SIDE_RING_SIZE_PERCENT,
                        ConfigContract.DEFAULT_SIDE_RING_SIZE_PERCENT),
                value -> value + "%"));
        card.addView(slider("顶部安全距离", ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT,
                8, 35, prefs.getInt(ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT),
                value -> value + "%"));
        card.addView(slider("反向取消距离", ConfigContract.KEY_SIDE_REVERSE_CANCEL_PERCENT,
                ConfigContract.MIN_SIDE_REVERSE_CANCEL_PERCENT,
                ConfigContract.MAX_SIDE_REVERSE_CANCEL_PERCENT,
                prefs.getInt(ConfigContract.KEY_SIDE_REVERSE_CANCEL_PERCENT,
                        ConfigContract.DEFAULT_SIDE_REVERSE_CANCEL_PERCENT),
                value -> value + "%"));
        card.addView(Ui.divider(this));
        card.addView(booleanRow("跟随手指显示", "列表在长滑达到阈值时以手指位置为中心",
                ConfigContract.KEY_SIDE_FOLLOW_FINGER,
                ConfigContract.DEFAULT_SIDE_FOLLOW_FINGER));
        card.addView(Ui.divider(this));
        card.addView(selectedNameRow());
        card.addView(Ui.divider(this));
        card.addView(booleanRow("侧滑蜂窝全屏启动",
                "关闭时整个侧滑蜂窝都以小窗启动",
                ConfigContract.KEY_SIDE_HONEYCOMB_FULLSCREEN, false));
        TextView note = text("侧滑区域从顶部安全距离以下开始，止于底角热区上方并保留间隔。",
                13, Ui.MUTED, Typeface.NORMAL);
        note.setPadding(0, Ui.dp(this, 8), 0, 0);
        card.addView(note);
    }

    private View selectedNameRow() {
        return booleanRow("显示选中应用名称",
                "统一用于底角扇形和侧滑的所有布局",
                ConfigContract.KEY_SHOW_SELECTED_APP_NAME,
                ConfigContract.DEFAULT_SHOW_SELECTED_APP_NAME);
    }

    private View targetManagerRow(String title, String kind) {
        LinearLayout settingRow = row();
        settingRow.setOnClickListener(view -> {
            Intent intent = new Intent(this, TargetManagerActivity.class);
            intent.putExtra(TargetManagerActivity.EXTRA_KIND, kind);
            startActivity(intent);
        });
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 16, Ui.TEXT, Typeface.BOLD));
        targetCountText = text("已选 0 项", 13, Ui.MUTED, Typeface.NORMAL);
        labels.addView(targetCountText);
        settingRow.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        settingRow.addView(text("管理  ›", 14, Ui.ACCENT, Typeface.BOLD));
        return settingRow;
    }

    private void updateTargetCount() {
        if (targetCountText == null || store == null) return;
        int count = sideMode ? store.getSideTargets().size() : store.getTargets().size();
        targetCountText.setText("已选 " + count + " 项");
    }

    private View sideLayoutModeRow() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        section.addView(text("选择布局", 16, Ui.TEXT, Typeface.BOLD));
        section.addView(text("纵向、环形、扇形共用侧滑应用；蜂窝使用独立蜂窝清单",
                13, Ui.MUTED, Typeface.NORMAL));
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        int[] modes = {ConfigContract.SIDE_LAYOUT_LIST,
                ConfigContract.SIDE_LAYOUT_RING, ConfigContract.SIDE_LAYOUT_FAN,
                ConfigContract.SIDE_LAYOUT_HONEYCOMB};
        String[] labels = {"纵向列表", "环形", "扇形", "蜂窝"};
        int selectedMode = prefs.getInt(ConfigContract.KEY_SIDE_LAYOUT_MODE,
                prefs.getBoolean(ConfigContract.KEY_SIDE_FAN_LIST,
                        ConfigContract.DEFAULT_SIDE_FAN_LIST)
                        ? ConfigContract.SIDE_LAYOUT_FAN
                        : ConfigContract.DEFAULT_SIDE_LAYOUT_MODE);
        for (int index = 0; index < modes.length; index++) {
            RadioButton option = new RadioButton(this);
            option.setId(View.generateViewId());
            option.setTag(modes[index]);
            option.setText(labels[index]);
            option.setTextSize(15);
            option.setTextColor(Ui.TEXT);
            option.setChecked(selectedMode == modes[index]);
            group.addView(option, new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 44)));
        }
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            View checked = radioGroup.findViewById(checkedId);
            if (checked != null && checked.getTag() instanceof Integer) {
                store.putInt(ConfigContract.KEY_SIDE_LAYOUT_MODE,
                        (Integer) checked.getTag());
            }
        });
        section.addView(group);
        return section;
    }

    private View booleanRow(String title, String subtitle, String key, boolean defaultValue) {
        LinearLayout settingRow = row();
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 16, Ui.TEXT, Typeface.BOLD));
        labels.addView(text(subtitle, 13, Ui.MUTED, Typeface.NORMAL));
        settingRow.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(key, defaultValue));
        toggle.setOnCheckedChangeListener((button, checked) ->
                store.putBoolean(key, checked));
        settingRow.addView(toggle);
        return settingRow;
    }

    private View slider(String title, String key, int min, int max, int current,
                        ValueLabel valueLabel) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 2));
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView label = text(title, 15, Ui.TEXT, Typeface.BOLD);
        TextView value = text(valueLabel.label(current), 14, Ui.ACCENT, Typeface.BOLD);
        value.setGravity(Gravity.END);
        header.addView(label, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(value);
        group.addView(header);

        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(Math.max(0, Math.min(max - min, current - min)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int resolved = min + progress;
                value.setText(valueLabel.label(resolved));
                if (!fromUser) return;
                store.putInt(key, resolved);
                if (ConfigContract.KEY_HOT_WIDTH_PERCENT.equals(key)) {
                    hotWidthPercent = resolved;
                } else if (ConfigContract.KEY_HOT_HEIGHT_PERCENT.equals(key)) {
                    hotHeightPercent = resolved;
                }
                if (gesturePreview != null) {
                    gesturePreview.update(hotWidthPercent, hotHeightPercent);
                }
            }
        });
        group.addView(seek);
        return group;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 17),
                Ui.dp(this, 18), Ui.dp(this, 17));
        card.setBackground(Ui.rounded(this, Ui.SURFACE, 20));
        card.setElevation(Ui.dp(this, 1));
        return card;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
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

    private interface ValueLabel {
        String label(int value);
    }
}
