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
import android.widget.ScrollView;
import android.widget.TextView;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;

public final class GestureSettingsActivity extends Activity {
    public static final String EXTRA_MODE = "mode";
    public static final String MODE_BOTTOM = "bottom";
    public static final String MODE_SIDE = "side";

    private SharedPreferences prefs;
    private boolean sideMode;
    private LinearLayout entries;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        sideMode = MODE_SIDE.equals(getIntent().getStringExtra(EXTRA_MODE));
        prefs = new ConfigStore(this).preferences();
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().setNavigationBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(content());
    }

    @Override protected void onResume() {
        super.onResume();
        if (entries != null) populateEntries();
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
        root.addView(header(sideMode ? "侧滑手势" : "底角斜滑"));
        TextView subtitle = text(sideMode
                        ? "侧滑只有一段；横竖屏可分别选择布局和启动方式"
                        : "一段扇形与二段蜂窝；横竖屏分别控制",
                14, Ui.MUTED, Typeface.NORMAL);
        subtitle.setPadding(Ui.dp(this, 44), 0, 0, Ui.dp(this, 16));
        root.addView(subtitle);
        entries = new LinearLayout(this);
        entries.setOrientation(LinearLayout.VERTICAL);
        root.addView(entries);
        populateEntries();
        return scroll;
    }

    private void populateEntries() {
        entries.removeAllViews();
        if (sideMode) populateSide();
        else populateBottom();
    }

    private void populateBottom() {
        entries.addView(entry("竖屏行为", bottomSummary(false),
                () -> openDetail(GestureDetailSettingsActivity.BOTTOM_PORTRAIT)));
        entries.addView(entry("横屏行为", bottomSummary(true),
                () -> openDetail(GestureDetailSettingsActivity.BOTTOM_LANDSCAPE)));
        entries.addView(entry("触发与选择", "触发区域、一段距离、蜂窝停顿时间与选择",
                () -> openDetail(GestureDetailSettingsActivity.BOTTOM_TRIGGER)));
        entries.addView(entry("应用与扇形排列", "已选 "
                        + new ConfigStore(this).getTargets().size() + " 项 · 图标与排数",
                () -> openDetail(GestureDetailSettingsActivity.BOTTOM_LAYOUT)));
        entries.addView(entry("扇形动效设置", "呼出、旋转、选中放大、淡出与阴影",
                () -> openAnimation(AnimationSettingsActivity.MODE_BOTTOM)));
    }

    private void populateSide() {
        entries.addView(entry("竖屏行为与布局", sideSummary(false),
                () -> openDetail(GestureDetailSettingsActivity.SIDE_PORTRAIT)));
        entries.addView(entry("横屏行为与布局", sideSummary(true),
                () -> openDetail(GestureDetailSettingsActivity.SIDE_LANDSCAPE)));
        entries.addView(entry("方向与触发", "允许方向、触发距离与停留",
                () -> openDetail(GestureDetailSettingsActivity.SIDE_TRIGGER)));
        entries.addView(entry("应用与布局参数", "已选 "
                        + new ConfigStore(this).getSideTargets().size()
                        + " 项 · 图标、环形大小与跟手位置",
                () -> openDetail(GestureDetailSettingsActivity.SIDE_LAYOUT)));
        entries.addView(entry("侧滑动效设置", "呼出、选择放大、旋转与取消淡出",
                () -> openAnimation(AnimationSettingsActivity.MODE_SIDE)));
    }

    private String bottomSummary(boolean landscape) {
        String firstKey = landscape ? ConfigContract.KEY_BOTTOM_LANDSCAPE_ENABLED
                : ConfigContract.KEY_BOTTOM_PORTRAIT_ENABLED;
        String secondKey = landscape
                ? ConfigContract.KEY_BOTTOM_LANDSCAPE_SECOND_STAGE_ENABLED
                : ConfigContract.KEY_BOTTOM_PORTRAIT_SECOND_STAGE_ENABLED;
        String fullscreenKey = landscape ? ConfigContract.KEY_BOTTOM_LANDSCAPE_FULLSCREEN
                : ConfigContract.KEY_BOTTOM_PORTRAIT_FULLSCREEN;
        String freeformKey = landscape
                ? ConfigContract.KEY_BOTTOM_LANDSCAPE_HONEYCOMB_FREEFORM
                : ConfigContract.KEY_BOTTOM_PORTRAIT_HONEYCOMB_FREEFORM;
        boolean first = prefs.getBoolean(firstKey, !landscape);
        boolean second = prefs.getBoolean(secondKey, !landscape);
        String firstText = first ? (prefs.getBoolean(fullscreenKey, false)
                ? "一段全屏" : "一段小窗") : "一段关闭";
        String secondText = second ? (prefs.getBoolean(freeformKey, true)
                ? "二段小窗" : "二段全屏") : "二段关闭";
        return firstText + " · " + secondText;
    }

    private String sideSummary(boolean landscape) {
        String enabledKey = landscape ? ConfigContract.KEY_SIDE_LANDSCAPE_ENABLED
                : ConfigContract.KEY_SIDE_PORTRAIT_ENABLED;
        if (!prefs.getBoolean(enabledKey, !landscape)) return "已关闭";
        String fullscreenKey = landscape ? ConfigContract.KEY_SIDE_LANDSCAPE_FULLSCREEN
                : ConfigContract.KEY_SIDE_PORTRAIT_FULLSCREEN;
        String layoutKey = landscape ? ConfigContract.KEY_SIDE_LANDSCAPE_LAYOUT_MODE
                : ConfigContract.KEY_SIDE_PORTRAIT_LAYOUT_MODE;
        int layout = prefs.getInt(layoutKey, ConfigContract.DEFAULT_SIDE_LAYOUT_MODE);
        return layoutName(layout) + " · "
                + (prefs.getBoolean(fullscreenKey, false) ? "全屏启动" : "小窗启动");
    }

    private String layoutName(int mode) {
        if (mode == ConfigContract.SIDE_LAYOUT_LIST) return "纵向列表";
        if (mode == ConfigContract.SIDE_LAYOUT_RING) return "环形";
        if (mode == ConfigContract.SIDE_LAYOUT_FAN) return "扇形";
        if (mode == ConfigContract.SIDE_LAYOUT_TASKS) return "任务中心";
        if (mode == ConfigContract.SIDE_LAYOUT_SYSTEM_RECENTS) return "系统任务中心";
        return "蜂窝";
    }

    private View entry(String title, String subtitle, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 10),
                Ui.dp(this, 18), Ui.dp(this, 10));
        card.setBackground(Ui.rounded(this, Ui.SURFACE, 20));
        card.setElevation(Ui.dp(this, 1));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        row.setOnClickListener(view -> action.run());
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 17, Ui.TEXT, Typeface.BOLD));
        labels.addView(text(subtitle, 13, Ui.MUTED, Typeface.NORMAL));
        row.addView(labels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text("›", 28, Ui.ACCENT, Typeface.NORMAL));
        card.addView(row);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = Ui.dp(this, 12);
        card.setLayoutParams(params);
        return card;
    }

    private void openDetail(String section) {
        Intent intent = new Intent(this, GestureDetailSettingsActivity.class);
        intent.putExtra(GestureDetailSettingsActivity.EXTRA_SECTION, section);
        startActivity(intent);
    }

    private void openAnimation(String mode) {
        Intent intent = new Intent(this, AnimationSettingsActivity.class);
        intent.putExtra(AnimationSettingsActivity.EXTRA_MODE, mode);
        startActivity(intent);
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

    private TextView text(String value, float size, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(null, style);
        return text;
    }
}
