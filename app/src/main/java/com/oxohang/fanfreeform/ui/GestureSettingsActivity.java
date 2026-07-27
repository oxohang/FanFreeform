package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

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
        TextView title = text(sideMode ? "侧滑列表" : "底角斜滑",
                26, Ui.TEXT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header);

        TextView subtitle = text(sideMode
                        ? "短滑保留 HyperOS 返回，长滑呼出纵向应用列表"
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
                35, 75, prefs.getInt(ConfigContract.KEY_SELECTION_RADIUS_PERCENT,
                        ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT), value -> value + "%"));
        card.addView(slider("图标大小", ConfigContract.KEY_ICON_SIZE_DP,
                34, 64, prefs.getInt(ConfigContract.KEY_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_ICON_SIZE_DP), value -> value + "dp"));
        TextView note = text("底角区域始终从屏幕物理边缘开始，不再为侧滑手势向内避让。",
                13, Ui.MUTED, Typeface.NORMAL);
        note.setPadding(0, Ui.dp(this, 10), 0, 0);
        card.addView(note);
    }

    private void buildSideSettings(LinearLayout card) {
        card.addView(text("侧滑参数", 18, Ui.TEXT, Typeface.BOLD));
        card.addView(slider("长滑触发距离", ConfigContract.KEY_SIDE_TRIGGER_PERCENT,
                18, 50, prefs.getInt(ConfigContract.KEY_SIDE_TRIGGER_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TRIGGER_PERCENT), value -> value + "%"));
        card.addView(slider("列表图标大小", ConfigContract.KEY_SIDE_ICON_SIZE_DP,
                34, 64, prefs.getInt(ConfigContract.KEY_SIDE_ICON_SIZE_DP,
                        prefs.getInt(ConfigContract.KEY_ICON_SIZE_DP,
                                ConfigContract.DEFAULT_SIDE_ICON_SIZE_DP)),
                value -> value + "dp"));
        card.addView(slider("顶部安全距离", ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT,
                8, 35, prefs.getInt(ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT,
                        ConfigContract.DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT),
                value -> value + "%"));
        card.addView(Ui.divider(this));
        LinearLayout namesRow = row();
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("显示应用名称", 16, Ui.TEXT, Typeface.BOLD));
        labels.addView(text("关闭后只在选中图标时浮现名称",
                13, Ui.MUTED, Typeface.NORMAL));
        namesRow.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch names = new Switch(this);
        names.setChecked(prefs.getBoolean(ConfigContract.KEY_SIDE_SHOW_APP_NAMES,
                ConfigContract.DEFAULT_SIDE_SHOW_APP_NAMES));
        names.setOnCheckedChangeListener((button, checked) ->
                store.putBoolean(ConfigContract.KEY_SIDE_SHOW_APP_NAMES, checked));
        namesRow.addView(names);
        card.addView(namesRow);
        TextView note = text("侧滑区域从顶部安全距离以下开始，止于底角热区上方并保留间隔。",
                13, Ui.MUTED, Typeface.NORMAL);
        note.setPadding(0, Ui.dp(this, 8), 0, 0);
        card.addView(note);
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
