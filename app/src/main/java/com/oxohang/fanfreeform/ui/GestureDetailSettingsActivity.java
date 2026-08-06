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

public final class GestureDetailSettingsActivity extends Activity {
    public static final String EXTRA_SECTION = "section";
    public static final String BOTTOM_PORTRAIT = "bottom_portrait";
    public static final String BOTTOM_LANDSCAPE = "bottom_landscape";
    public static final String BOTTOM_TRIGGER = "bottom_trigger";
    public static final String BOTTOM_LAYOUT = "bottom_layout";
    public static final String SIDE_PORTRAIT = "side_portrait";
    public static final String SIDE_LANDSCAPE = "side_landscape";
    public static final String SIDE_TRIGGER = "side_trigger";
    public static final String SIDE_LAYOUT = "side_layout";

    private ConfigStore store;
    private SharedPreferences prefs;
    private String section;
    private TextView targetCount;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        prefs = store.preferences();
        section = getIntent().getStringExtra(EXTRA_SECTION);
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().setNavigationBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(content());
    }

    @Override protected void onResume() {
        super.onResume();
        if (targetCount != null) {
            int count = SIDE_LAYOUT.equals(section) ? store.getSideTargets().size()
                    : store.getTargets().size();
            targetCount.setText("已选 " + count + " 项");
        }
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
        root.addView(header(title()));
        TextView subtitle = text(subtitle(), 14, Ui.MUTED, Typeface.NORMAL);
        subtitle.setPadding(Ui.dp(this, 44), 0, 0, Ui.dp(this, 16));
        root.addView(subtitle);
        LinearLayout card = card();
        Runnable reset;
        String resetMessage;
        if (BOTTOM_PORTRAIT.equals(section) || BOTTOM_LANDSCAPE.equals(section)) {
            boolean landscape = BOTTOM_LANDSCAPE.equals(section);
            buildBottomBehavior(card, landscape);
            reset = () -> store.resetBottomOrientationSettings(landscape);
            resetMessage = "将恢复本方向的一段、二段和启动方式默认值。";
        } else if (BOTTOM_TRIGGER.equals(section)) {
            buildBottomTrigger(card);
            reset = store::resetBottomTriggerSettings;
            resetMessage = "将恢复底角触发区域、分段距离和选择距离默认值。";
        } else if (BOTTOM_LAYOUT.equals(section)) {
            buildBottomLayout(card);
            reset = store::resetBottomLayoutSettings;
            resetMessage = "将恢复扇形图标与排布参数；已选应用会保留。";
        } else if (SIDE_PORTRAIT.equals(section) || SIDE_LANDSCAPE.equals(section)) {
            boolean landscape = SIDE_LANDSCAPE.equals(section);
            buildSideBehavior(card, landscape);
            reset = () -> store.resetSideOrientationSettings(landscape);
            resetMessage = "将恢复本方向的侧滑开关、布局和启动方式默认值。";
        } else if (SIDE_TRIGGER.equals(section)) {
            buildSideTrigger(card);
            reset = store::resetSideTriggerSettings;
            resetMessage = "将恢复侧滑方向、安全距离、触发距离和停留参数。";
        } else {
            buildSideLayout(card);
            reset = store::resetSideLayoutSettings;
            resetMessage = "将恢复侧滑图标、环形尺寸和跟手参数；已选应用会保留。";
        }
        root.addView(card);
        Ui.addResetOption(this, root, resetMessage, reset);
        return scroll;
    }

    private void buildBottomBehavior(LinearLayout card, boolean landscape) {
        String firstEnabled = landscape ? ConfigContract.KEY_BOTTOM_LANDSCAPE_ENABLED
                : ConfigContract.KEY_BOTTOM_PORTRAIT_ENABLED;
        String firstFullscreen = landscape ? ConfigContract.KEY_BOTTOM_LANDSCAPE_FULLSCREEN
                : ConfigContract.KEY_BOTTOM_PORTRAIT_FULLSCREEN;
        String secondEnabled = landscape
                ? ConfigContract.KEY_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED
                : ConfigContract.KEY_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED;
        String secondFreeform = landscape
                ? ConfigContract.KEY_BOTTOM_LANDSCAPE_HONEYCOMB_FREEFORM
                : ConfigContract.KEY_BOTTOM_PORTRAIT_HONEYCOMB_FREEFORM;
        card.addView(text("一段扇形", 18, Ui.TEXT, Typeface.BOLD));
        LinearLayout firstLaunch = launchMode(firstFullscreen, false,
                prefs.getBoolean(firstFullscreen, false));
        card.addView(toggleWithChild("启用一段手势", "斜滑后显示扇形应用",
                firstEnabled, !landscape, firstLaunch));
        card.addView(firstLaunch);
        if (!landscape) {
            card.addView(toggle("一段重压启动", "选中一段应用后重压，反转当前启动方式",
                    ConfigContract.KEY_BOTTOM_PORTRAIT_FIRST_PRESSURE_LAUNCH,
                    ConfigContract.DEFAULT_BOTTOM_PORTRAIT_FIRST_PRESSURE_LAUNCH));
            card.addView(text("需先在“按压手势”中完成气压校准并启用。",
                    13, Ui.MUTED, Typeface.NORMAL));
        }
        card.addView(Ui.divider(this));
        card.addView(text("二段蜂窝", 18, Ui.TEXT, Typeface.BOLD));
        LinearLayout secondLaunch = launchMode(secondFreeform, true,
                prefs.getBoolean(secondFreeform, true));
        card.addView(toggleWithChild("启用二段手势", "越过扇形后进入蜂窝应用",
                secondEnabled, !landscape, secondLaunch));
        card.addView(secondLaunch);
        if (!landscape) {
            card.addView(toggle("二段重压启动", "蜂窝中选中应用后重压，反转当前启动方式",
                    ConfigContract.KEY_BOTTOM_PORTRAIT_SECOND_PRESSURE_LAUNCH,
                    ConfigContract.DEFAULT_BOTTOM_PORTRAIT_SECOND_PRESSURE_LAUNCH));
        }
    }

    private void buildSideBehavior(LinearLayout card, boolean landscape) {
        String enabled = landscape ? ConfigContract.KEY_SIDE_LANDSCAPE_ENABLED
                : ConfigContract.KEY_SIDE_PORTRAIT_ENABLED;
        String fullscreen = landscape ? ConfigContract.KEY_SIDE_LANDSCAPE_FULLSCREEN
                : ConfigContract.KEY_SIDE_PORTRAIT_FULLSCREEN;
        String layout = landscape ? ConfigContract.KEY_SIDE_LANDSCAPE_LAYOUT_MODE
                : ConfigContract.KEY_SIDE_PORTRAIT_LAYOUT_MODE;
        LinearLayout children = new LinearLayout(this);
        children.setOrientation(LinearLayout.VERTICAL);
        children.addView(text("启动方式", 16, Ui.TEXT, Typeface.BOLD));
        children.addView(launchMode(fullscreen, false, prefs.getBoolean(fullscreen, false)));
        children.addView(Ui.divider(this));
        children.addView(text("列表形式", 16, Ui.TEXT, Typeface.BOLD));
        children.addView(sideLayoutMode(layout));
        card.addView(toggleWithChild("启用侧滑", "此方向只有一段手势",
                enabled, !landscape, children));
        card.addView(children);
    }

    private void buildBottomTrigger(LinearLayout card) {
        card.addView(text("触发区域", 18, Ui.TEXT, Typeface.BOLD));
        GesturePreviewView preview = new GesturePreviewView(this);
        preview.update(prefs.getInt(ConfigContract.KEY_HOT_WIDTH_PERCENT,
                        ConfigContract.DEFAULT_HOT_WIDTH_PERCENT),
                prefs.getInt(ConfigContract.KEY_HOT_HEIGHT_PERCENT,
                        ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT));
        card.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 150)));
        card.addView(slider("触发区宽度", ConfigContract.KEY_HOT_WIDTH_PERCENT,
                5, 20, ConfigContract.DEFAULT_HOT_WIDTH_PERCENT, value -> value + "%"));
        card.addView(slider("触发区高度", ConfigContract.KEY_HOT_HEIGHT_PERCENT,
                3, ConfigContract.MAX_HOT_HEIGHT_PERCENT,
                ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT, value -> value + "%"));
        card.addView(Ui.divider(this));
        card.addView(text("分段距离", 18, Ui.TEXT, Typeface.BOLD));
        card.addView(slider("一段扇形距离", ConfigContract.KEY_TRIGGER_PERCENT,
                6, 24, ConfigContract.DEFAULT_TRIGGER_PERCENT, value -> value + "%"));
        card.addView(slider("二段蜂窝距离", ConfigContract.KEY_HONEYCOMB_TRIGGER_DP,
                ConfigContract.MIN_HONEYCOMB_TRIGGER_DP,
                ConfigContract.MAX_HONEYCOMB_TRIGGER_DP,
                ConfigContract.DEFAULT_HONEYCOMB_TRIGGER_DP, value -> value + "dp"));
        card.addView(slider("二段回退距离", ConfigContract.KEY_HONEYCOMB_RETREAT_DP,
                ConfigContract.MIN_HONEYCOMB_RETREAT_DP,
                ConfigContract.MAX_HONEYCOMB_RETREAT_DP,
                ConfigContract.DEFAULT_HONEYCOMB_RETREAT_DP, value -> value + "dp"));
        card.addView(slider("图标选择距离", ConfigContract.KEY_SELECTION_RADIUS_PERCENT,
                ConfigContract.MIN_SELECTION_RADIUS_PERCENT,
                ConfigContract.MAX_SELECTION_RADIUS_PERCENT,
                ConfigContract.DEFAULT_SELECTION_RADIUS_PERCENT, value -> value + "%"));
    }

    private void buildBottomLayout(LinearLayout card) {
        card.addView(targetManager("小窗应用与快捷方式", TargetManagerActivity.KIND_FAN));
        card.addView(Ui.divider(this));
        card.addView(navigationRow("扇形排列", "智能排布或自定义内、中、外三排",
                () -> startActivity(new Intent(this, FanLayoutSettingsActivity.class))));
        card.addView(slider("图标大小", ConfigContract.KEY_ICON_SIZE_DP,
                34, 64, ConfigContract.DEFAULT_ICON_SIZE_DP, value -> value + "dp"));
        card.addView(slider("扇形目标上限", ConfigContract.KEY_FAN_MAX_TARGETS,
                3, 24, ConfigContract.DEFAULT_FAN_MAX_TARGETS, value -> value + " 个"));
    }

    private void buildSideTrigger(LinearLayout card) {
        card.addView(text("允许方向", 18, Ui.TEXT, Typeface.BOLD));
        card.addView(toggle("横向向内", "允许水平长滑",
                ConfigContract.KEY_SIDE_DIRECTION_HORIZONTAL, true));
        card.addView(toggle("斜向上", "允许向内斜上长滑",
                ConfigContract.KEY_SIDE_DIRECTION_UP, true));
        card.addView(toggle("斜向下", "允许向内斜下长滑",
                ConfigContract.KEY_SIDE_DIRECTION_DOWN, true));
        card.addView(Ui.divider(this));
        card.addView(text("触发判定", 18, Ui.TEXT, Typeface.BOLD));
        card.addView(slider("顶部安全距离", ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT,
                8, 35, ConfigContract.DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT,
                value -> value + "%"));
        card.addView(slider("侧滑触发距离", ConfigContract.KEY_SIDE_TRIGGER_PERCENT,
                ConfigContract.MIN_SIDE_TRIGGER_PERCENT,
                ConfigContract.MAX_SIDE_TRIGGER_PERCENT,
                ConfigContract.DEFAULT_SIDE_TRIGGER_PERCENT, value -> value + "%"));
        card.addView(slider("反向取消距离", ConfigContract.KEY_SIDE_REVERSE_CANCEL_PERCENT,
                ConfigContract.MIN_SIDE_REVERSE_CANCEL_PERCENT,
                ConfigContract.MAX_SIDE_REVERSE_CANCEL_PERCENT,
                ConfigContract.DEFAULT_SIDE_REVERSE_CANCEL_PERCENT, value -> value + "%"));
        card.addView(toggle("蜂窝停留触发", "仅当前方向选择蜂窝布局时生效",
                ConfigContract.KEY_SIDE_HOLD_ENABLED,
                ConfigContract.DEFAULT_SIDE_HOLD_ENABLED));
        card.addView(slider("停留时间", ConfigContract.KEY_SIDE_HOLD_DELAY_MS,
                ConfigContract.MIN_SIDE_HOLD_DELAY_MS,
                ConfigContract.MAX_SIDE_HOLD_DELAY_MS,
                ConfigContract.DEFAULT_SIDE_HOLD_DELAY_MS, value -> value + "ms"));
    }

    private void buildSideLayout(LinearLayout card) {
        card.addView(targetManager("侧滑应用与快捷方式", TargetManagerActivity.KIND_SIDE));
        card.addView(slider("应用上限", ConfigContract.KEY_SIDE_MAX_TARGETS,
                1, 36, ConfigContract.DEFAULT_SIDE_MAX_TARGETS, value -> value + " 个"));
        card.addView(slider("图标大小", ConfigContract.KEY_SIDE_ICON_SIZE_DP,
                34, 64, ConfigContract.DEFAULT_SIDE_ICON_SIZE_DP, value -> value + "dp"));
        card.addView(slider("环形大小", ConfigContract.KEY_SIDE_RING_SIZE_PERCENT,
                ConfigContract.MIN_SIDE_RING_SIZE_PERCENT,
                ConfigContract.MAX_SIDE_RING_SIZE_PERCENT,
                ConfigContract.DEFAULT_SIDE_RING_SIZE_PERCENT, value -> value + "%"));
        card.addView(toggle("跟随手指显示", "关闭后使用布局的固定位置",
                ConfigContract.KEY_SIDE_FOLLOW_FINGER,
                ConfigContract.DEFAULT_SIDE_FOLLOW_FINGER));
    }

    private LinearLayout launchMode(String key, boolean storedValueMeansFreeform,
                                    boolean current) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 10));
        RadioGroup group = new RadioGroup(this);
        RadioButton freeform = radio("小窗启动", 0);
        RadioButton fullscreen = radio("全屏启动", 1);
        group.addView(freeform);
        group.addView(fullscreen);
        boolean freeformSelected = storedValueMeansFreeform ? current : !current;
        group.check(freeformSelected ? freeform.getId() : fullscreen.getId());
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            boolean choseFreeform = checkedId == freeform.getId();
            store.putBoolean(key, storedValueMeansFreeform ? choseFreeform : !choseFreeform);
        });
        section.addView(group);
        return section;
    }

    private View sideLayoutMode(String key) {
        RadioGroup group = new RadioGroup(this);
        int[] modes = {ConfigContract.SIDE_LAYOUT_LIST, ConfigContract.SIDE_LAYOUT_RING,
                ConfigContract.SIDE_LAYOUT_FAN, ConfigContract.SIDE_LAYOUT_HONEYCOMB,
                ConfigContract.SIDE_LAYOUT_TASKS,
                ConfigContract.SIDE_LAYOUT_SYSTEM_RECENTS};
        String[] labels = {"纵向列表", "环形", "扇形", "蜂窝",
                "Hyper任务中心", "HyperOS 系统任务中心"};
        int current = prefs.getInt(key, ConfigContract.DEFAULT_SIDE_LAYOUT_MODE);
        for (int index = 0; index < modes.length; index++) {
            RadioButton button = radio(labels[index], modes[index]);
            button.setChecked(current == modes[index]);
            group.addView(button);
        }
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            View selected = radioGroup.findViewById(checkedId);
            if (selected != null && selected.getTag() instanceof Integer) {
                store.putInt(key, (Integer) selected.getTag());
            }
        });
        return group;
    }

    private View toggleWithChild(String title, String subtitle, String key,
                                 boolean defaultValue, View child) {
        LinearLayout row = toggle(title, subtitle, key, defaultValue);
        Switch toggle = (Switch) row.getChildAt(1);
        child.setVisibility(toggle.isChecked() ? View.VISIBLE : View.GONE);
        toggle.setOnCheckedChangeListener((button, checked) -> {
            store.putBoolean(key, checked);
            child.setVisibility(checked ? View.VISIBLE : View.GONE);
        });
        return row;
    }

    private LinearLayout toggle(String title, String subtitle, String key,
                                boolean defaultValue) {
        LinearLayout row = row();
        LinearLayout labels = labels(title, subtitle);
        row.addView(labels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(key, defaultValue));
        toggle.setOnCheckedChangeListener((button, checked) -> store.putBoolean(key, checked));
        row.addView(toggle);
        return row;
    }

    private View targetManager(String title, String kind) {
        LinearLayout row = row();
        row.setOnClickListener(view -> {
            Intent intent = new Intent(this, TargetManagerActivity.class);
            intent.putExtra(TargetManagerActivity.EXTRA_KIND, kind);
            startActivity(intent);
        });
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 16, Ui.TEXT, Typeface.BOLD));
        targetCount = text("已选 0 项", 13, Ui.MUTED, Typeface.NORMAL);
        labels.addView(targetCount);
        row.addView(labels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text("管理  ›", 14, Ui.ACCENT, Typeface.BOLD));
        return row;
    }

    private View navigationRow(String title, String subtitle, Runnable action) {
        LinearLayout row = row();
        row.setOnClickListener(view -> action.run());
        row.addView(labels(title, subtitle), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text("设置  ›", 14, Ui.ACCENT, Typeface.BOLD));
        return row;
    }

    private View slider(String title, String key, int min, int max,
                        int defaultValue, Label labeler) {
        int current = prefs.getInt(key, defaultValue);
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 2));
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

    private RadioButton radio(String title, int value) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setTag(value);
        button.setText(title);
        button.setTextSize(15);
        button.setTextColor(Ui.TEXT);
        button.setMinHeight(Ui.dp(this, 44));
        return button;
    }

    private String title() {
        if (BOTTOM_PORTRAIT.equals(section)) return "底角 · 竖屏行为";
        if (BOTTOM_LANDSCAPE.equals(section)) return "底角 · 横屏行为";
        if (BOTTOM_TRIGGER.equals(section)) return "底角 · 触发与选择";
        if (BOTTOM_LAYOUT.equals(section)) return "底角 · 应用与排列";
        if (SIDE_PORTRAIT.equals(section)) return "侧滑 · 竖屏行为";
        if (SIDE_LANDSCAPE.equals(section)) return "侧滑 · 横屏行为";
        if (SIDE_TRIGGER.equals(section)) return "侧滑 · 方向与触发";
        return "侧滑 · 应用与布局";
    }

    private String subtitle() {
        if (BOTTOM_PORTRAIT.equals(section) || BOTTOM_LANDSCAPE.equals(section)) {
            return "分别控制一段扇形、二段蜂窝及各自启动方式";
        }
        if (SIDE_PORTRAIT.equals(section) || SIDE_LANDSCAPE.equals(section)) {
            return "侧滑只有一段，此处设置该方向的布局和启动方式";
        }
        if (BOTTOM_TRIGGER.equals(section)) return "只调整手势判定，不改变动画效果";
        if (SIDE_TRIGGER.equals(section)) return "短滑保留 HyperOS 返回，达到距离后接管";
        return "应用内容与图标布局参数";
    }

    private View header(String title) {
        LinearLayout header = row();
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("返回");
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 52)));
        header.addView(text(title, 25, Ui.TEXT, Typeface.BOLD));
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

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
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

    private interface Label { String text(int value); }
}
