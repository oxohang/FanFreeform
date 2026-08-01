package com.oxohang.fanfreeform.ui;

import android.app.Activity;
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

public final class HoneycombAnimationSettingsActivity extends Activity {
    private static final String[] SPEEDS = {"很快", "较快", "标准", "柔和", "慢速"};
    private static final String[] INERTIA = {"低", "中", "高"};
    private ConfigStore store;
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        prefs = store.preferences();
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().setNavigationBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(content());
    }

    private View content() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xfff4f5fa);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 14),
                Ui.dp(this, 20), Ui.dp(this, 36));
        scroll.addView(root);
        root.addView(header());
        TextView subtitle = text("调整 Apple Watch 风格的整体位移、凸起和聚焦反馈",
                14, Ui.MUTED, Typeface.NORMAL);
        subtitle.setPadding(Ui.dp(this, 44), 0, 0, Ui.dp(this, 16));
        root.addView(subtitle);
        LinearLayout card = card();
        card.addView(slider("动画速度", ConfigContract.KEY_HONEYCOMB_ANIMATION_SPEED,
                0, 4, ConfigContract.DEFAULT_HONEYCOMB_ANIMATION_SPEED,
                value -> SPEEDS[value]));
        card.addView(systemAnimationToggle());
        card.addView(slider("惯性强度", ConfigContract.KEY_HONEYCOMB_INERTIA,
                0, 2, ConfigContract.DEFAULT_HONEYCOMB_INERTIA,
                value -> INERTIA[value]));
        card.addView(slider("中心凸起", ConfigContract.KEY_HONEYCOMB_CENTER_SCALE,
                ConfigContract.MIN_HONEYCOMB_CENTER_SCALE,
                ConfigContract.MAX_HONEYCOMB_CENTER_SCALE,
                ConfigContract.DEFAULT_HONEYCOMB_CENTER_SCALE, value -> value + "%"));
        card.addView(slider("边缘缩小", ConfigContract.KEY_HONEYCOMB_EDGE_SCALE,
                ConfigContract.MIN_HONEYCOMB_EDGE_SCALE,
                ConfigContract.MAX_HONEYCOMB_EDGE_SCALE,
                ConfigContract.DEFAULT_HONEYCOMB_EDGE_SCALE, value -> value + "%"));
        card.addView(slider("选中放大", ConfigContract.KEY_HONEYCOMB_SELECTION_SCALE,
                ConfigContract.MIN_HONEYCOMB_SELECTION_SCALE,
                ConfigContract.MAX_HONEYCOMB_SELECTION_SCALE,
                ConfigContract.DEFAULT_HONEYCOMB_SELECTION_SCALE, value -> value + "%"));
        root.addView(card);
        Ui.addResetOption(this, root,
                "将恢复蜂窝动画、全屏启动、惯性、凸起和缩放默认值。",
                store::resetHoneycombAnimationSettings);
        return scroll;
    }

    private View systemAnimationToggle() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 8));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("中心展开系统动画", 15, Ui.TEXT, Typeface.BOLD));
        labels.addView(text("关闭后使用 HyperOS 默认全屏启动动画",
                12, Ui.MUTED, Typeface.NORMAL));
        row.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(
                ConfigContract.KEY_HONEYCOMB_CENTERED_SYSTEM_ANIMATION,
                ConfigContract.DEFAULT_HONEYCOMB_CENTERED_SYSTEM_ANIMATION));
        toggle.setOnCheckedChangeListener((button, checked) -> store.putBoolean(
                ConfigContract.KEY_HONEYCOMB_CENTERED_SYSTEM_ANIMATION, checked));
        row.addView(toggle);
        return row;
    }

    private View slider(String title, String key, int min, int max,
                        int defaultValue, Label labeler) {
        int current = prefs.getInt(key, defaultValue);
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, Ui.dp(this, 14), 0, 0);
        LinearLayout heading = new LinearLayout(this);
        heading.addView(text(title, 15, Ui.TEXT, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView value = text(labeler.text(current), 14, Ui.ACCENT, Typeface.BOLD);
        heading.addView(value);
        group.addView(heading);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(Math.max(0, Math.min(max - min, current - min)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override public void onProgressChanged(SeekBar seekBar, int progress,
                                                    boolean fromUser) {
                int resolved = min + progress;
                value.setText(labeler.text(resolved));
                if (fromUser) store.putInt(key, resolved);
            }
        });
        group.addView(seek);
        return group;
    }

    private View header() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 52)));
        header.addView(text("蜂窝动效", 26, Ui.TEXT, Typeface.BOLD));
        return header;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 16),
                Ui.dp(this, 18), Ui.dp(this, 16));
        card.setBackground(Ui.rounded(this, Ui.SURFACE, 20));
        return card;
    }

    private TextView text(String value, float size, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(null, style);
        return text;
    }

    private interface Label { String text(int value); }
}
