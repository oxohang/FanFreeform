package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.content.SharedPreferences;
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
import android.widget.TextView;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;

public final class TaskCenterAnimationSettingsActivity extends Activity {
    private static final String[] ANIMATION_SPEEDS = {
            "很快", "较快", "标准", "柔和", "慢速"
    };
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
        TextView subtitle = text("调整卡片和图标共用的选择手感",
                14, Ui.MUTED, Typeface.NORMAL);
        subtitle.setPadding(Ui.dp(this, 44), 0, 0, Ui.dp(this, 16));
        root.addView(subtitle);
        LinearLayout card = card();
        card.addView(text("选择动效", 18, Ui.TEXT, Typeface.BOLD));
        RadioGroup group = new RadioGroup(this);
        RadioButton apple = radio("苹果流畅 · 轻吸附、近似 1:1 跟手",
                ConfigContract.SIDE_TASK_MOTION_APPLE);
        RadioButton magnetic = radio("磁吸轨道 · 吸入、上浮与短回弹",
                ConfigContract.SIDE_TASK_MOTION_MAGNETIC);
        RadioButton marble = radio("弹珠磁场 · 明显吸附与连带波动",
                ConfigContract.SIDE_TASK_MOTION_MARBLE);
        group.addView(apple);
        group.addView(magnetic);
        group.addView(marble);
        int current = prefs.getInt(ConfigContract.KEY_SIDE_TASK_MOTION_MODE,
                ConfigContract.DEFAULT_SIDE_TASK_MOTION_MODE);
        group.check(current == ConfigContract.SIDE_TASK_MOTION_APPLE
                ? apple.getId() : current == ConfigContract.SIDE_TASK_MOTION_MARBLE
                ? marble.getId() : magnetic.getId());
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> store.putInt(
                ConfigContract.KEY_SIDE_TASK_MOTION_MODE,
                checkedId == apple.getId() ? ConfigContract.SIDE_TASK_MOTION_APPLE
                        : checkedId == marble.getId() ? ConfigContract.SIDE_TASK_MOTION_MARBLE
                        : ConfigContract.SIDE_TASK_MOTION_MAGNETIC));
        card.addView(group);
        card.addView(Ui.divider(this));
        card.addView(overallAnimationSpeedSlider());
        card.addView(Ui.divider(this));
        card.addView(speedSlider());
        root.addView(card);
        Ui.addResetOption(this, root,
                "将恢复任务中心选择动效和拖动速度默认值。",
                store::resetTaskCenterAnimationSettings);
        return scroll;
    }

    private View overallAnimationSpeedSlider() {
        int current = prefs.getInt(ConfigContract.KEY_SIDE_TASK_ANIMATION_SPEED,
                ConfigContract.DEFAULT_SIDE_TASK_ANIMATION_SPEED);
        current = Math.max(0, Math.min(4, current));
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 8));
        LinearLayout heading = new LinearLayout(this);
        heading.addView(text("整体动画速度", 15, Ui.TEXT, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView value = text(ANIMATION_SPEEDS[current], 14, Ui.ACCENT, Typeface.BOLD);
        heading.addView(value);
        group.addView(heading);
        SeekBar seek = new SeekBar(this);
        seek.setMax(4);
        seek.setProgress(current);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override public void onProgressChanged(SeekBar seekBar, int progress,
                                                    boolean fromUser) {
                int resolved = Math.max(0, Math.min(4, progress));
                value.setText(ANIMATION_SPEEDS[resolved]);
                if (fromUser) store.putInt(
                        ConfigContract.KEY_SIDE_TASK_ANIMATION_SPEED, resolved);
            }
        });
        group.addView(seek);
        TextView note = text("同时调整呼出、选中回弹、确认和关闭",
                12, Ui.MUTED, Typeface.NORMAL);
        note.setPadding(0, 0, 0, Ui.dp(this, 4));
        group.addView(note);
        return group;
    }

    private View speedSlider() {
        int min = ConfigContract.MIN_SIDE_TASK_SWIPE_SPEED_PERCENT;
        int max = ConfigContract.MAX_SIDE_TASK_SWIPE_SPEED_PERCENT;
        int current = prefs.getInt(ConfigContract.KEY_SIDE_TASK_SWIPE_SPEED_PERCENT,
                ConfigContract.DEFAULT_SIDE_TASK_SWIPE_SPEED_PERCENT);
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, Ui.dp(this, 14), 0, 0);
        LinearLayout heading = new LinearLayout(this);
        heading.addView(text("任务拖动速度", 15, Ui.TEXT, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView value = text(speedLabel(current), 14, Ui.ACCENT, Typeface.BOLD);
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
                value.setText(speedLabel(resolved));
                if (fromUser) store.putInt(
                        ConfigContract.KEY_SIDE_TASK_SWIPE_SPEED_PERCENT, resolved);
            }
        });
        group.addView(seek);
        return group;
    }

    private String speedLabel(int value) {
        return String.format(java.util.Locale.CHINA, "%.1f×", value / 100f);
    }

    private RadioButton radio(String title, int value) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setTag(value);
        button.setText(title);
        button.setTextColor(Ui.TEXT);
        button.setTextSize(15);
        button.setMinHeight(Ui.dp(this, 46));
        return button;
    }

    private View header() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 52)));
        header.addView(text("任务中心动效", 26, Ui.TEXT, Typeface.BOLD));
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
}
