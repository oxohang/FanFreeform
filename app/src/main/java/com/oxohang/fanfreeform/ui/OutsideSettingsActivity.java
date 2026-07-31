package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;

public final class OutsideSettingsActivity extends Activity {
    private static final String[] LABELS = {
            "无操作", "关闭小窗", "挂起到右上角", "全屏", "小窗贴边"
    };
    private static final int[] VALUES = {
            ConfigContract.ACTION_NONE, ConfigContract.ACTION_CLOSE,
            ConfigContract.ACTION_PIN, ConfigContract.ACTION_FULLSCREEN,
            ConfigContract.ACTION_EDGE_PIN
    };
    private ConfigStore store;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(content());
    }

    private View content() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xfff4f5fa);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 14),
                Ui.dp(this, 20), Ui.dp(this, 36));
        scroll.addView(root);
        root.addView(header());

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 16),
                Ui.dp(this, 18), Ui.dp(this, 16));
        card.setBackground(Ui.rounded(this, Ui.SURFACE, 20));
        card.addView(enabledRow("竖屏窗外点击", "关闭后竖屏使用 HyperOS 原生模式",
                ConfigContract.KEY_OUTSIDE_PORTRAIT_ENABLED,
                ConfigContract.DEFAULT_OUTSIDE_PORTRAIT_ENABLED, true));
        card.addView(Ui.divider(this));
        card.addView(enabledRow("横屏窗外点击", "关闭后横屏使用 HyperOS 原生模式",
                ConfigContract.KEY_OUTSIDE_LANDSCAPE_ENABLED,
                ConfigContract.DEFAULT_OUTSIDE_LANDSCAPE_ENABLED, false));
        card.addView(Ui.divider(this));
        card.addView(action("窗外单击", ConfigContract.KEY_OUTSIDE_SINGLE_ACTION,
                ConfigContract.DEFAULT_OUTSIDE_SINGLE_ACTION));
        card.addView(Ui.divider(this));
        card.addView(action("窗外双击", ConfigContract.KEY_OUTSIDE_DOUBLE_ACTION,
                ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION));
        card.addView(Ui.divider(this));
        card.addView(tapWindowSlider());
        TextView note = text("竖屏和横屏独立控制；关闭的方向会移除 Hyper手势触摸层，完全交还 HyperOS。"
                        + "\n\n数值越短，单击执行越快；数值越长，双击越容易识别。"
                        + "\n\n小窗上、下、左、右外部是一个整体，只识别点击。"
                        + "系统返回侧滑保持更高优先级；输入法存在时第一次点击先收起键盘。",
                14, Ui.MUTED, Typeface.NORMAL);
        note.setLineSpacing(0, 1.2f);
        note.setPadding(0, Ui.dp(this, 12), 0, 0);
        card.addView(note);
        root.addView(card);
        Ui.addResetOption(this, root,
                "将重新开启竖屏、关闭横屏窗外点击，并恢复动作和判定时间。",
                store::resetOutsideSettings);
        return scroll;
    }

    private View enabledRow(String title, String subtitle, String key,
                            boolean defaultValue, boolean useLegacyFallback) {
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 16, Ui.TEXT, Typeface.BOLD));
        labels.addView(text(subtitle, 13, Ui.MUTED, Typeface.NORMAL));
        line.addView(labels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch enabled = new Switch(this);
        boolean fallback = useLegacyFallback
                ? store.preferences().getBoolean(ConfigContract.KEY_OUTSIDE_ENABLED,
                ConfigContract.DEFAULT_OUTSIDE_ENABLED) : defaultValue;
        enabled.setChecked(store.preferences().getBoolean(key, fallback));
        enabled.setOnCheckedChangeListener((button, checked) ->
                store.putBoolean(key, checked));
        line.addView(enabled);
        return line;
    }

    private View tapWindowSlider() {
        int minimum = ConfigContract.MIN_OUTSIDE_TAP_WINDOW_MS;
        int maximum = ConfigContract.MAX_OUTSIDE_TAP_WINDOW_MS;
        int current = store.preferences().getInt(ConfigContract.KEY_OUTSIDE_TAP_WINDOW_MS,
                ConfigContract.DEFAULT_OUTSIDE_TAP_WINDOW_MS);
        current = Math.max(minimum, Math.min(maximum, current));
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 4));
        LinearLayout line = new LinearLayout(this);
        TextView label = text("单双击判定时间", 16, Ui.TEXT, Typeface.BOLD);
        TextView value = text(current + "ms", 14, Ui.ACCENT, Typeface.BOLD);
        value.setGravity(Gravity.END);
        line.addView(label, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        line.addView(value);
        group.addView(line);
        SeekBar seek = new SeekBar(this);
        seek.setMax(maximum - minimum);
        seek.setProgress(current - minimum);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override public void onProgressChanged(SeekBar seekBar, int progress,
                                                    boolean fromUser) {
                int resolved = minimum + progress;
                value.setText(resolved + "ms");
                if (fromUser) store.putInt(ConfigContract.KEY_OUTSIDE_TAP_WINDOW_MS, resolved);
            }
        });
        group.addView(seek);
        return group;
    }

    private View action(String title, String key, int defaultValue) {
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        line.addView(text(title, 16, Ui.TEXT, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, LABELS));
        int current = store.preferences().getInt(key, defaultValue);
        int selected = 0;
        for (int index = 0; index < VALUES.length; index++) {
            if (VALUES[index] == current) selected = index;
        }
        spinner.setSelection(selected);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                                                 View view, int position, long id) {
                store.putInt(key, VALUES[position]);
            }
        });
        line.addView(spinner);
        return line;
    }

    private View header() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 52)));
        header.addView(text("窗外点击设置", 26, Ui.TEXT, Typeface.BOLD));
        return header;
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
