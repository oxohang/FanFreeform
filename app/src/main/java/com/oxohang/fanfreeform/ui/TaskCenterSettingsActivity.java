package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.Switch;
import android.widget.TextView;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;

public final class TaskCenterSettingsActivity extends Activity {
    private ConfigStore store;
    private SharedPreferences prefs;
    private TaskCardPreviewView preview;
    private int cardWidth;
    private int cardHeight;
    private int cardCorner;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        prefs = store.preferences();
        int legacySize = prefs.getInt(ConfigContract.KEY_SIDE_TASK_CARD_SIZE_DP,
                ConfigContract.DEFAULT_SIDE_TASK_CARD_SIZE_DP);
        cardWidth = prefs.getInt(ConfigContract.KEY_SIDE_TASK_CARD_WIDTH_DP, legacySize);
        cardHeight = prefs.getInt(ConfigContract.KEY_SIDE_TASK_CARD_HEIGHT_DP,
                Math.round(legacySize * 1.67f));
        cardCorner = prefs.getInt(ConfigContract.KEY_SIDE_TASK_CARD_CORNER_DP,
                ConfigContract.DEFAULT_SIDE_TASK_CARD_CORNER_DP);
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

        root.addView(header("任务中心"));
        TextView subtitle = text("侧滑呼出横向最近任务；移入下方空白区后松手退出",
                14, Ui.MUTED, Typeface.NORMAL);
        subtitle.setPadding(Ui.dp(this, 44), 0, 0, Ui.dp(this, 16));
        root.addView(subtitle);

        LinearLayout card = card();
        card.addView(text("任务布局", 18, Ui.TEXT, Typeface.BOLD));
        card.addView(layoutModeRow());
        card.addView(Ui.divider(this));
        card.addView(reverseOrderRow());
        card.addView(Ui.divider(this));
        card.addView(showNameRow());
        card.addView(Ui.divider(this));
        TextView cardSizeTitle = text("卡片模式尺寸", 16, Ui.TEXT, Typeface.BOLD);
        cardSizeTitle.setPadding(0, Ui.dp(this, 12), 0, 0);
        card.addView(cardSizeTitle);
        preview = new TaskCardPreviewView(this);
        preview.update(cardWidth, cardHeight, cardCorner);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 230));
        previewParams.topMargin = Ui.dp(this, 14);
        card.addView(preview, previewParams);
        card.addView(slider("最近运行任务数",
                ConfigContract.KEY_SIDE_TASK_MAX_COUNT,
                ConfigContract.MIN_SIDE_TASK_MAX_COUNT,
                ConfigContract.MAX_SIDE_TASK_MAX_COUNT,
                prefs.getInt(ConfigContract.KEY_SIDE_TASK_MAX_COUNT,
                        ConfigContract.DEFAULT_SIDE_TASK_MAX_COUNT), value -> value + " 个"));
        card.addView(slider("卡片宽度", ConfigContract.KEY_SIDE_TASK_CARD_WIDTH_DP,
                ConfigContract.MIN_SIDE_TASK_CARD_WIDTH_DP,
                ConfigContract.MAX_SIDE_TASK_CARD_WIDTH_DP,
                cardWidth, value -> value + "dp"));
        card.addView(slider("卡片高度", ConfigContract.KEY_SIDE_TASK_CARD_HEIGHT_DP,
                ConfigContract.MIN_SIDE_TASK_CARD_HEIGHT_DP,
                ConfigContract.MAX_SIDE_TASK_CARD_HEIGHT_DP,
                cardHeight, value -> value + "dp"));
        card.addView(slider("卡片圆角", ConfigContract.KEY_SIDE_TASK_CARD_CORNER_DP,
                ConfigContract.MIN_SIDE_TASK_CARD_CORNER_DP,
                ConfigContract.MAX_SIDE_TASK_CARD_CORNER_DP,
                cardCorner, value -> value + "dp"));
        card.addView(Ui.divider(this));
        TextView iconSizeTitle = text("图标模式尺寸", 16, Ui.TEXT, Typeface.BOLD);
        iconSizeTitle.setPadding(0, Ui.dp(this, 12), 0, 0);
        card.addView(iconSizeTitle);
        card.addView(slider("任务图标大小", ConfigContract.KEY_SIDE_TASK_ICON_SIZE_DP,
                ConfigContract.MIN_SIDE_TASK_ICON_SIZE_DP,
                ConfigContract.MAX_SIDE_TASK_ICON_SIZE_DP,
                prefs.getInt(ConfigContract.KEY_SIDE_TASK_ICON_SIZE_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_ICON_SIZE_DP), value -> value + "dp"));
        card.addView(slider("图标间距", ConfigContract.KEY_SIDE_TASK_ICON_GAP_DP,
                ConfigContract.MIN_SIDE_TASK_ICON_GAP_DP,
                ConfigContract.MAX_SIDE_TASK_ICON_GAP_DP,
                prefs.getInt(ConfigContract.KEY_SIDE_TASK_ICON_GAP_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_ICON_GAP_DP), value -> value + "dp"));
        card.addView(slider("卡片与手指距离",
                ConfigContract.KEY_SIDE_TASK_FINGER_OFFSET_DP,
                ConfigContract.MIN_SIDE_TASK_FINGER_OFFSET_DP,
                ConfigContract.MAX_SIDE_TASK_FINGER_OFFSET_DP,
                prefs.getInt(ConfigContract.KEY_SIDE_TASK_FINGER_OFFSET_DP,
                        ConfigContract.DEFAULT_SIDE_TASK_FINGER_OFFSET_DP), value -> value + "dp"));
        card.addView(downwardToleranceRow());
        TextView note = text("卡片和图标的横向跟手、切换速度与实际启动映射保持不变。",
                13, Ui.MUTED, Typeface.NORMAL);
        note.setPadding(0, Ui.dp(this, 14), 0, 0);
        card.addView(note);
        root.addView(card);

        LinearLayout motionCard = card();
        LinearLayout motionEntry = new LinearLayout(this);
        motionEntry.setGravity(Gravity.CENTER_VERTICAL);
        motionEntry.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        motionEntry.setOnClickListener(view -> startActivity(
                new Intent(this, TaskCenterAnimationSettingsActivity.class)));
        LinearLayout motionLabels = new LinearLayout(this);
        motionLabels.setOrientation(LinearLayout.VERTICAL);
        motionLabels.addView(text("任务中心动效设置", 17, Ui.TEXT, Typeface.BOLD));
        motionLabels.addView(text("跟手速度、吸附、放大与回弹",
                13, Ui.MUTED, Typeface.NORMAL));
        motionEntry.addView(motionLabels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        motionEntry.addView(text("设置  ›", 14, Ui.ACCENT, Typeface.BOLD));
        motionCard.addView(motionEntry);
        LinearLayout.LayoutParams motionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        motionParams.topMargin = Ui.dp(this, 14);
        root.addView(motionCard, motionParams);

        Ui.addResetOption(this, root,
                "将恢复任务数量、布局、排列方向、尺寸、下移容错和位置；动效参数会保留。",
                store::resetTaskCenterLayoutSettings);
        return scroll;
    }

    private View layoutModeRow() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 10));
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        RadioButton flat = radio("横向卡片 · 显示任务预览",
                ConfigContract.SIDE_TASK_LAYOUT_FLAT);
        RadioButton icons = radio("横向图标 · 只显示运行应用图标",
                ConfigContract.SIDE_TASK_LAYOUT_ICONS);
        group.addView(flat);
        group.addView(icons);
        int current = prefs.getInt(ConfigContract.KEY_SIDE_TASK_LAYOUT_MODE,
                ConfigContract.DEFAULT_SIDE_TASK_LAYOUT_MODE);
        group.check(current == ConfigContract.SIDE_TASK_LAYOUT_ICONS
                ? icons.getId() : flat.getId());
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            int value = checkedId == icons.getId()
                    ? ConfigContract.SIDE_TASK_LAYOUT_ICONS
                    : ConfigContract.SIDE_TASK_LAYOUT_FLAT;
            store.putInt(ConfigContract.KEY_SIDE_TASK_LAYOUT_MODE, value);
        });
        section.addView(group);
        return section;
    }

    private View reverseOrderRow() {
        LinearLayout settingRow = new LinearLayout(this);
        settingRow.setGravity(Gravity.CENTER_VERTICAL);
        settingRow.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("反向排列最近任务", 16, Ui.TEXT, Typeface.BOLD));
        labels.addView(text("只反转图标顺序，手指选择方向保持自然",
                13, Ui.MUTED, Typeface.NORMAL));
        settingRow.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(ConfigContract.KEY_SIDE_TASK_REVERSE_ORDER,
                ConfigContract.DEFAULT_SIDE_TASK_REVERSE_ORDER));
        toggle.setOnCheckedChangeListener((button, checked) -> store.putBoolean(
                ConfigContract.KEY_SIDE_TASK_REVERSE_ORDER, checked));
        settingRow.addView(toggle);
        return settingRow;
    }

    private View motionModeRow() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        section.addView(text("选择动效", 16, Ui.TEXT, Typeface.BOLD));
        section.addView(text("三种动效共用同一套准确的任务命中逻辑",
                13, Ui.MUTED, Typeface.NORMAL));
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
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
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            int value = checkedId == apple.getId()
                    ? ConfigContract.SIDE_TASK_MOTION_APPLE
                    : checkedId == marble.getId()
                    ? ConfigContract.SIDE_TASK_MOTION_MARBLE
                    : ConfigContract.SIDE_TASK_MOTION_MAGNETIC;
            store.putInt(ConfigContract.KEY_SIDE_TASK_MOTION_MODE, value);
        });
        section.addView(group);
        return section;
    }

    private View showNameRow() {
        LinearLayout settingRow = new LinearLayout(this);
        settingRow.setGravity(Gravity.CENTER_VERTICAL);
        settingRow.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("显示任务名称", 16, Ui.TEXT, Typeface.BOLD));
        labels.addView(text("卡片显示完整标题；图标模式只显示当前任务名称",
                13, Ui.MUTED, Typeface.NORMAL));
        settingRow.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(ConfigContract.KEY_SIDE_TASK_SHOW_NAME,
                ConfigContract.DEFAULT_SIDE_TASK_SHOW_NAME));
        toggle.setOnCheckedChangeListener((button, checked) ->
                store.putBoolean(ConfigContract.KEY_SIDE_TASK_SHOW_NAME, checked));
        settingRow.addView(toggle);
        return settingRow;
    }

    private View downwardToleranceRow() {
        LinearLayout settingRow = new LinearLayout(this);
        settingRow.setGravity(Gravity.CENTER_VERTICAL);
        settingRow.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 2));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("扩大下移容错", 16, Ui.TEXT, Typeface.BOLD));
        labels.addView(text("横向选择时允许手指向下偏移约两倍，再往下才退出",
                13, Ui.MUTED, Typeface.NORMAL));
        settingRow.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(
                ConfigContract.KEY_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE,
                ConfigContract.DEFAULT_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE));
        toggle.setOnCheckedChangeListener((button, checked) -> store.putBoolean(
                ConfigContract.KEY_SIDE_TASK_EXTENDED_DOWNWARD_TOLERANCE, checked));
        settingRow.addView(toggle);
        return settingRow;
    }

    private RadioButton radio(String title, int value) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setTag(value);
        button.setText(title);
        button.setTextSize(15);
        button.setTextColor(Ui.TEXT);
        button.setMinHeight(Ui.dp(this, 46));
        return button;
    }

    private View slider(String title, String key, int min, int max, int current,
                        Label labeler) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, Ui.dp(this, 14), 0, 0);
        LinearLayout line = new LinearLayout(this);
        TextView titleView = text(title, 15, Ui.TEXT, Typeface.BOLD);
        TextView value = text(labeler.label(current), 14, Ui.ACCENT, Typeface.BOLD);
        value.setGravity(Gravity.END);
        line.addView(titleView, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        line.addView(value);
        group.addView(line);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(Math.max(0, Math.min(max - min, current - min)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override public void onProgressChanged(SeekBar seekBar, int progress,
                                                    boolean fromUser) {
                int resolved = min + progress;
                value.setText(labeler.label(resolved));
                if (!fromUser) return;
                store.putInt(key, resolved);
                if (ConfigContract.KEY_SIDE_TASK_CARD_WIDTH_DP.equals(key)) {
                    cardWidth = resolved;
                } else if (ConfigContract.KEY_SIDE_TASK_CARD_HEIGHT_DP.equals(key)) {
                    cardHeight = resolved;
                } else if (ConfigContract.KEY_SIDE_TASK_CARD_CORNER_DP.equals(key)) {
                    cardCorner = resolved;
                }
                if (preview != null) preview.update(cardWidth, cardHeight, cardCorner);
            }
        });
        group.addView(seek);
        return group;
    }

    private View header(String title) {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("返回");
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 52)));
        header.addView(text(title, 26, Ui.TEXT, Typeface.BOLD));
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

    private interface Label { String label(int value); }
}
