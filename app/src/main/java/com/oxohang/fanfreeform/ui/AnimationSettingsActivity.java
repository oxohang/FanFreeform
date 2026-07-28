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

public final class AnimationSettingsActivity extends Activity {
    private ConfigStore store;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        preferences = store.preferences();
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
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 36));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("返回");
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 52)));
        header.addView(text("动画效果", 26, Ui.TEXT, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header);

        TextView subtitle = text("作用于底角扇形与侧滑列表，不改变 HyperOS 原生小窗过渡。", 14,
                Ui.MUTED, Typeface.NORMAL);
        subtitle.setPadding(Ui.dp(this, 44), 0, 0, Ui.dp(this, 16));
        root.addView(subtitle);

        LinearLayout card = card();
        LinearLayout enabledRow = row();
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("扇形动画", 17, Ui.TEXT, Typeface.BOLD));
        labels.addView(text("图标轻微四散呼出，选择时放大并回弹", 13,
                Ui.MUTED, Typeface.NORMAL));
        enabledRow.addView(labels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch enabled = new Switch(this);
        enabled.setChecked(preferences.getBoolean(ConfigContract.KEY_FAN_ANIMATIONS_ENABLED,
                ConfigContract.DEFAULT_FAN_ANIMATIONS_ENABLED));
        enabled.setOnCheckedChangeListener((button, checked) ->
                store.putBoolean(ConfigContract.KEY_FAN_ANIMATIONS_ENABLED, checked));
        enabledRow.addView(enabled);
        card.addView(enabledRow);
        card.addView(Ui.divider(this));
        int speed = preferences.getInt(ConfigContract.KEY_FAN_ANIMATION_SPEED,
                ConfigContract.DEFAULT_FAN_ANIMATION_SPEED);
        card.addView(slider("动画速度", ConfigContract.KEY_FAN_ANIMATION_SPEED, speed,
                ConfigContract.MIN_FAN_ANIMATION_SPEED,
                ConfigContract.MAX_FAN_ANIMATION_SPEED, "%"));
        card.addView(Ui.divider(this));
        int revealAmount = preferences.getInt(ConfigContract.KEY_FAN_REVEAL_AMOUNT,
                ConfigContract.DEFAULT_FAN_REVEAL_AMOUNT);
        card.addView(slider("图标呼出幅度", ConfigContract.KEY_FAN_REVEAL_AMOUNT,
                revealAmount, ConfigContract.MIN_FAN_REVEAL_AMOUNT,
                ConfigContract.MAX_FAN_REVEAL_AMOUNT, "%"));
        card.addView(Ui.divider(this));
        int rotation = preferences.getInt(ConfigContract.KEY_FAN_ROTATION_DEGREES,
                ConfigContract.DEFAULT_FAN_ROTATION_DEGREES);
        card.addView(slider("图标旋转幅度", ConfigContract.KEY_FAN_ROTATION_DEGREES,
                rotation, ConfigContract.MIN_FAN_ROTATION_DEGREES,
                ConfigContract.MAX_FAN_ROTATION_DEGREES, "°"));
        card.addView(Ui.divider(this));
        int selectionScale = preferences.getInt(
                ConfigContract.KEY_FAN_SELECTION_SCALE_PERCENT,
                ConfigContract.DEFAULT_FAN_SELECTION_SCALE_PERCENT);
        card.addView(slider("应用选择放大", ConfigContract.KEY_FAN_SELECTION_SCALE_PERCENT,
                selectionScale, ConfigContract.MIN_FAN_SELECTION_SCALE_PERCENT,
                ConfigContract.MAX_FAN_SELECTION_SCALE_PERCENT, "%"));
        card.addView(Ui.divider(this));
        LinearLayout ringRow = row();
        LinearLayout ringLabels = new LinearLayout(this);
        ringLabels.setOrientation(LinearLayout.VERTICAL);
        ringLabels.addView(text("应用选择蓝色圈", 16, Ui.TEXT, Typeface.BOLD));
        ringLabels.addView(text("关闭后只保留图标放大与旋转反馈", 13,
                Ui.MUTED, Typeface.NORMAL));
        ringRow.addView(ringLabels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch ring = new Switch(this);
        ring.setChecked(preferences.getBoolean(ConfigContract.KEY_FAN_SELECTION_RING,
                ConfigContract.DEFAULT_FAN_SELECTION_RING));
        ring.setOnCheckedChangeListener((button, checked) ->
                store.putBoolean(ConfigContract.KEY_FAN_SELECTION_RING, checked));
        ringRow.addView(ring);
        card.addView(ringRow);
        TextView note = text("动画速度越高越快；其余数值控制动效幅度。确认应用不会被动效阻塞。", 13,
                Ui.MUTED, Typeface.NORMAL);
        note.setPadding(0, Ui.dp(this, 10), 0, 0);
        card.addView(note);
        root.addView(card);
        return scroll;
    }

    private View slider(String title, String key, int current, int min, int max,
                        String suffix) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, Ui.dp(this, 14), 0, 0);
        LinearLayout header = new LinearLayout(this);
        TextView label = text(title, 15, Ui.TEXT, Typeface.BOLD);
        TextView value = text(current + suffix, 14, Ui.ACCENT, Typeface.BOLD);
        value.setGravity(Gravity.END);
        header.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(value);
        group.addView(header);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(Math.max(0, Math.min(max - min, current - min)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar view) { }
            @Override public void onStopTrackingTouch(SeekBar view) { }
            @Override public void onProgressChanged(SeekBar view, int progress, boolean fromUser) {
                int resolved = min + progress;
                value.setText(resolved + suffix);
                if (fromUser) store.putInt(key, resolved);
            }
        });
        group.addView(seek);
        return group;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16));
        card.setBackground(Ui.rounded(this, Ui.SURFACE, 18));
        return card;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Ui.dp(this, 52));
        return row;
    }

    private TextView text(String value, int size, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(null, style);
        return view;
    }
}
