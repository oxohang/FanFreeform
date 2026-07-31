package com.oxohang.fanfreeform.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import com.oxohang.fanfreeform.config.FanRowAllocation;

@SuppressLint("SetTextI18n")
public final class FanLayoutSettingsActivity extends Activity {
    private ConfigStore store;
    private SharedPreferences prefs;
    private LinearLayout customControls;
    private RowSlider outerSlider;
    private RowSlider middleSlider;
    private RowSlider innerSlider;
    private TextView capacitySummary;
    private int selectedCount;
    private boolean updatingRows;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        prefs = store.preferences();
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
        scroll.addView(root);

        LinearLayout header = row();
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("返回");
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 52)));
        header.addView(text("扇形排列", 26, Ui.TEXT, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header);

        TextView subtitle = text("自定义模式依次填满内排、中排、外排；未满的新一排从底部向上排列",
                14, Ui.MUTED, Typeface.NORMAL);
        subtitle.setPadding(Ui.dp(this, 44), 0, 0, Ui.dp(this, 16));
        root.addView(subtitle);

        LinearLayout modeCard = card();
        modeCard.addView(text("排列模式", 18, Ui.TEXT, Typeface.BOLD));
        RadioGroup group = new RadioGroup(this);
        RadioButton smart = radio("智能排布 · 根据数量自动平衡每排",
                ConfigContract.FAN_LAYOUT_SMART);
        RadioButton custom = radio("自定义 · 将已选应用分配到最多三排",
                ConfigContract.FAN_LAYOUT_CUSTOM);
        group.addView(smart);
        group.addView(custom);
        int mode = prefs.getInt(ConfigContract.KEY_FAN_LAYOUT_MODE,
                prefs.getBoolean(ConfigContract.KEY_FAN_FIXED_SEVEN_ROWS,
                        ConfigContract.DEFAULT_FAN_FIXED_SEVEN_ROWS)
                        ? ConfigContract.FAN_LAYOUT_SMART
                        : ConfigContract.DEFAULT_FAN_LAYOUT_MODE);
        if (mode == ConfigContract.FAN_LAYOUT_FIXED_SEVEN) {
            mode = ConfigContract.FAN_LAYOUT_SMART;
            store.putInt(ConfigContract.KEY_FAN_LAYOUT_MODE, mode);
            store.putBoolean(ConfigContract.KEY_FAN_FIXED_SEVEN_ROWS, false);
        }
        group.check(mode == ConfigContract.FAN_LAYOUT_CUSTOM ? custom.getId()
                : smart.getId());
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            int selectedMode = checkedId == custom.getId() ? ConfigContract.FAN_LAYOUT_CUSTOM
                    : ConfigContract.FAN_LAYOUT_SMART;
            store.putInt(ConfigContract.KEY_FAN_LAYOUT_MODE, selectedMode);
            store.putBoolean(ConfigContract.KEY_FAN_FIXED_SEVEN_ROWS, false);
            setCustomEnabled(selectedMode == ConfigContract.FAN_LAYOUT_CUSTOM);
        });
        modeCard.addView(group);
        root.addView(modeCard);

        LinearLayout customCard = card();
        selectedCount = store.getTargets().size();
        customCard.addView(text("自定义排数", 18, Ui.TEXT, Typeface.BOLD));
        TextView selectedSummary = text("已选择小窗应用：" + selectedCount + " 个",
                14, Ui.ACCENT, Typeface.BOLD);
        selectedSummary.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 2));
        customCard.addView(selectedSummary);
        customControls = new LinearLayout(this);
        customControls.setOrientation(LinearLayout.VERTICAL);
        int[] initial = FanRowAllocation.normalize(selectedCount,
                prefs.getInt(ConfigContract.KEY_FAN_CUSTOM_INNER_COUNT,
                        ConfigContract.DEFAULT_FAN_CUSTOM_INNER_COUNT),
                prefs.getInt(ConfigContract.KEY_FAN_CUSTOM_MIDDLE_COUNT,
                        ConfigContract.DEFAULT_FAN_CUSTOM_MIDDLE_COUNT));
        innerSlider = new RowSlider("内排 · 最多 8 个", initial[0], value -> applyAllocation(
                FanRowAllocation.afterInnerChanged(selectedCount, value), true));
        middleSlider = new RowSlider("中排 · 最多 10 个", initial[1], value -> applyAllocation(
                FanRowAllocation.afterMiddleChanged(selectedCount,
                        innerSlider.value(), value), true));
        outerSlider = new RowSlider("外排 · 最多 14 个", initial[2], value -> applyAllocation(
                FanRowAllocation.afterOuterChanged(selectedCount,
                        innerSlider.value(), value), true));
        applyAllocation(initial, false);
        customControls.addView(innerSlider.view);
        customControls.addView(middleSlider.view);
        customControls.addView(outerSlider.view);
        capacitySummary = text("", 13, Ui.MUTED, Typeface.NORMAL);
        capacitySummary.setPadding(0, Ui.dp(this, 10), 0, 0);
        customControls.addView(capacitySummary);
        customCard.addView(customControls);
        LinearLayout.LayoutParams customParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        customParams.topMargin = Ui.dp(this, 14);
        root.addView(customCard, customParams);
        updateCapacitySummary();
        setCustomEnabled(mode == ConfigContract.FAN_LAYOUT_CUSTOM);

        Ui.addResetOption(this, root,
                "将恢复智能排布；自定义模式会优先安排内排，再安排中排和外排。已选应用不会改变。",
                store::resetFanLayoutSettings);
        return scroll;
    }

    private void saveRows() {
        if (outerSlider == null || middleSlider == null || innerSlider == null) return;
        store.putFanRowAllocation(innerSlider.value(), middleSlider.value(),
                outerSlider.value());
        updateCapacitySummary();
    }

    private void applyAllocation(int[] allocation, boolean save) {
        if (allocation == null || allocation.length < 3 || updatingRows) return;
        updatingRows = true;
        int[] normalized = FanRowAllocation.normalize(selectedCount,
                allocation[0], allocation[1]);
        int inner = normalized[0];
        int remaining = selectedCount - inner;
        int middle = normalized[1];
        int outer = normalized[2];
        innerSlider.setRangeAndValue(FanRowAllocation.minimumInner(selectedCount),
                FanRowAllocation.maximumInner(selectedCount), inner);
        middleSlider.setRangeAndValue(FanRowAllocation.minimumMiddle(remaining),
                FanRowAllocation.maximumMiddle(remaining), middle);
        outerSlider.setRangeAndValue(FanRowAllocation.minimumOuter(remaining),
                FanRowAllocation.maximumOuter(remaining), outer);
        updatingRows = false;
        if (save) saveRows();
        else updateCapacitySummary();
    }

    private void updateCapacitySummary() {
        if (capacitySummary == null) return;
        capacitySummary.setText("内 " + innerSlider.value() + " / 中 "
                + middleSlider.value() + " / 外 " + outerSlider.value()
                + " · 共 " + selectedCount + " 个");
    }

    private void setCustomEnabled(boolean enabled) {
        if (customControls == null) return;
        customControls.setAlpha(enabled ? 1f : 0.42f);
        setEnabled(customControls, enabled);
    }

    private void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            setEnabled(group.getChildAt(index), enabled);
        }
    }

    private final class RowSlider {
        final LinearLayout view;
        final SeekBar seek;
        final TextView valueText;
        final ValueChanged listener;
        int minimum;
        int maximum;

        RowSlider(String title, int initial, ValueChanged listener) {
            this.listener = listener;
            minimum = 0;
            maximum = Math.max(0, selectedCount);
            view = new LinearLayout(FanLayoutSettingsActivity.this);
            view.setOrientation(LinearLayout.VERTICAL);
            view.setPadding(0, Ui.dp(FanLayoutSettingsActivity.this, 12), 0, 0);
            LinearLayout heading = row();
            heading.addView(text(title, 15, Ui.TEXT, Typeface.BOLD),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            valueText = text(initial + " 个", 14, Ui.ACCENT, Typeface.BOLD);
            heading.addView(valueText);
            view.addView(heading);
            seek = new SeekBar(FanLayoutSettingsActivity.this);
            seek.setMax(maximum - minimum);
            seek.setProgress(clamp(initial, minimum, maximum) - minimum);
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { }
                @Override public void onProgressChanged(SeekBar seekBar, int progress,
                                                        boolean fromUser) {
                    int resolved = clamp(minimum + progress, minimum, maximum);
                    if (progress != resolved - minimum) {
                        seekBar.setProgress(resolved - minimum);
                        return;
                    }
                    valueText.setText(resolved + " 个");
                    if (fromUser) listener.changed(resolved);
                }
            });
            view.addView(seek);
        }

        int value() { return clamp(minimum + seek.getProgress(), minimum, maximum); }

        void setRangeAndValue(int min, int max, int value) {
            minimum = Math.max(0, min);
            maximum = Math.max(minimum, max);
            seek.setMax(maximum - minimum);
            seek.setProgress(clamp(value, minimum, maximum) - minimum);
            valueText.setText(value() + " 个");
        }
    }

    private RadioButton radio(String label, int tag) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setTag(tag);
        button.setText(label);
        button.setTextColor(Ui.TEXT);
        button.setTextSize(15);
        button.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 17),
                Ui.dp(this, 18), Ui.dp(this, 17));
        card.setBackground(Ui.rounded(this, Color.WHITE, 20));
        card.setElevation(Ui.dp(this, 1));
        return card;
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private interface ValueChanged { void changed(int value); }
}
