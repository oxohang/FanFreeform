package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.oxohang.fanfreeform.config.AppTarget;
import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("SetTextI18n")
public final class MainActivity extends Activity {
    private static final int REQUEST_PICK_APP = 41;
    private static final String[] ACTION_LABELS = {"无操作", "关闭", "挂起到右上角", "全屏"};

    private ConfigStore store;
    private SharedPreferences prefs;
    private final ArrayList<AppTarget> targets = new ArrayList<>();
    private LinearLayout appsContainer;
    private TextView appsHint;
    private TextView statusText;
    private WindowPreviewView preview;
    private GesturePreviewView gesturePreview;
    private TextView bottomGestureSummary;
    private TextView sideGestureSummary;
    private Switch sideGestureSwitch;
    private int widthPercent;
    private int heightPercent;
    private int positionX;
    private int positionY;
    private int hotWidthPercent;
    private int hotHeightPercent;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        prefs = store.preferences();
        targets.addAll(store.getTargets());
        widthPercent = prefs.getInt(ConfigContract.KEY_WIDTH_PERCENT, ConfigContract.DEFAULT_WIDTH_PERCENT);
        heightPercent = prefs.getInt(ConfigContract.KEY_HEIGHT_PERCENT, ConfigContract.DEFAULT_HEIGHT_PERCENT);
        positionX = prefs.getInt(ConfigContract.KEY_POSITION_X, ConfigContract.DEFAULT_POSITION_X);
        positionY = prefs.getInt(ConfigContract.KEY_POSITION_Y, ConfigContract.DEFAULT_POSITION_Y);
        hotWidthPercent = prefs.getInt(ConfigContract.KEY_HOT_WIDTH_PERCENT, ConfigContract.DEFAULT_HOT_WIDTH_PERCENT);
        hotHeightPercent = prefs.getInt(ConfigContract.KEY_HOT_HEIGHT_PERCENT, ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT);

        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().setNavigationBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        hotWidthPercent = prefs.getInt(ConfigContract.KEY_HOT_WIDTH_PERCENT,
                ConfigContract.DEFAULT_HOT_WIDTH_PERCENT);
        hotHeightPercent = prefs.getInt(ConfigContract.KEY_HOT_HEIGHT_PERCENT,
                ConfigContract.DEFAULT_HOT_HEIGHT_PERCENT);
        if (gesturePreview != null) gesturePreview.update(hotWidthPercent, hotHeightPercent);
        updateGestureSummaries();
        updateStatus();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xfff4f5fa);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 36));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("随用随走", 30, Ui.TEXT, Typeface.BOLD);
        root.addView(title);
        TextView subtitle = text("HyperOS 3 原生快捷小窗", 15, Ui.MUTED, Typeface.NORMAL);
        subtitle.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 18));
        root.addView(subtitle);

        LinearLayout statusCard = card();
        TextView statusTitle = text("接口状态", 13, Ui.MUTED, Typeface.BOLD);
        statusCard.addView(statusTitle);
        statusText = text("等待 SystemUI 连接", 16, Ui.TEXT, Typeface.BOLD);
        statusText.setPadding(0, Ui.dp(this, 6), 0, 0);
        statusCard.addView(statusText);
        root.addView(statusCard, cardParams(0));

        LinearLayout switchCard = card();
        LinearLayout enabledRow = row();
        LinearLayout enabledText = new LinearLayout(this);
        enabledText.setOrientation(LinearLayout.VERTICAL);
        enabledText.addView(text("启用快捷手势", 17, Ui.TEXT, Typeface.BOLD));
        enabledText.addView(text("底角斜滑与侧滑列表共用快捷应用", 13, Ui.MUTED, Typeface.NORMAL));
        enabledRow.addView(enabledText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch enabled = new Switch(this);
        enabled.setChecked(prefs.getBoolean(ConfigContract.KEY_ENABLED, ConfigContract.DEFAULT_ENABLED));
        enabled.setOnCheckedChangeListener((button, checked) -> store.putBoolean(ConfigContract.KEY_ENABLED, checked));
        enabledRow.addView(enabled);
        switchCard.addView(enabledRow);
        switchCard.addView(Ui.divider(this));
        LinearLayout hapticRow = row();
        LinearLayout hapticText = new LinearLayout(this);
        hapticText.setOrientation(LinearLayout.VERTICAL);
        hapticText.addView(text("震动反馈", 17, Ui.TEXT, Typeface.BOLD));
        hapticText.addView(text("手势达到触发距离时反馈", 13, Ui.MUTED, Typeface.NORMAL));
        hapticRow.addView(hapticText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch haptic = new Switch(this);
        haptic.setChecked(prefs.getBoolean(ConfigContract.KEY_HAPTIC, ConfigContract.DEFAULT_HAPTIC));
        haptic.setOnCheckedChangeListener((button, checked) -> store.putBoolean(ConfigContract.KEY_HAPTIC, checked));
        hapticRow.addView(haptic);
        switchCard.addView(hapticRow);
        switchCard.addView(Ui.divider(this));
        LinearLayout shadowRow = row();
        LinearLayout shadowText = new LinearLayout(this);
        shadowText.setOrientation(LinearLayout.VERTICAL);
        shadowText.addView(text("扇形背景阴影", 17, Ui.TEXT, Typeface.BOLD));
        shadowText.addView(text("呼出时显示扇形区域的渐变暗影", 13, Ui.MUTED, Typeface.NORMAL));
        shadowRow.addView(shadowText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch shadow = new Switch(this);
        shadow.setChecked(prefs.getBoolean(ConfigContract.KEY_FAN_SHADOW, ConfigContract.DEFAULT_FAN_SHADOW));
        shadow.setOnCheckedChangeListener((button, checked) -> store.putBoolean(ConfigContract.KEY_FAN_SHADOW, checked));
        shadowRow.addView(shadow);
        switchCard.addView(shadowRow);
        root.addView(switchCard, cardParams(14));

        LinearLayout appsCard = card();
        LinearLayout appsHeader = row();
        LinearLayout appsTitleGroup = new LinearLayout(this);
        appsTitleGroup.setOrientation(LinearLayout.VERTICAL);
        appsTitleGroup.addView(text("快捷应用", 18, Ui.TEXT, Typeface.BOLD));
        appsHint = text("", 13, Ui.MUTED, Typeface.NORMAL);
        appsTitleGroup.addView(appsHint);
        appsHeader.addView(appsTitleGroup, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button add = compactButton("添加");
        add.setOnClickListener(view -> {
            if (targets.size() >= 8) {
                Toast.makeText(this, "最多添加 8 个应用", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivityForResult(new Intent(this, AppPickerActivity.class), REQUEST_PICK_APP);
        });
        appsHeader.addView(add);
        appsCard.addView(appsHeader);
        appsContainer = new LinearLayout(this);
        appsContainer.setOrientation(LinearLayout.VERTICAL);
        appsCard.addView(appsContainer);
        renderApps();
        root.addView(appsCard, cardParams(14));

        LinearLayout gestureCard = card();
        gestureCard.addView(text("手势方式", 18, Ui.TEXT, Typeface.BOLD));
        gesturePreview = new GesturePreviewView(this);
        gesturePreview.update(hotWidthPercent, hotHeightPercent);
        gestureCard.addView(gesturePreview,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 150)));

        LinearLayout bottomGestureRow = row();
        bottomGestureRow.setOnClickListener(view -> openGestureSettings(
                GestureSettingsActivity.MODE_BOTTOM));
        LinearLayout bottomGestureText = new LinearLayout(this);
        bottomGestureText.setOrientation(LinearLayout.VERTICAL);
        bottomGestureText.addView(text("底角斜滑", 16, Ui.TEXT, Typeface.BOLD));
        bottomGestureSummary = text("", 13, Ui.MUTED, Typeface.NORMAL);
        bottomGestureText.addView(bottomGestureSummary);
        bottomGestureRow.addView(bottomGestureText,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView bottomArrow = text("设置  ›", 14, Ui.ACCENT, Typeface.BOLD);
        bottomGestureRow.addView(bottomArrow);
        gestureCard.addView(bottomGestureRow);
        gestureCard.addView(Ui.divider(this));

        LinearLayout sideGestureRow = row();
        LinearLayout sideGestureText = new LinearLayout(this);
        sideGestureText.setOrientation(LinearLayout.VERTICAL);
        sideGestureText.addView(text("侧滑列表", 16, Ui.TEXT, Typeface.BOLD));
        sideGestureSummary = text("", 13, Ui.MUTED, Typeface.NORMAL);
        sideGestureText.addView(sideGestureSummary);
        sideGestureText.setOnClickListener(view -> openGestureSettings(
                GestureSettingsActivity.MODE_SIDE));
        sideGestureRow.addView(sideGestureText,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView sideSettings = text("设置", 14, Ui.ACCENT, Typeface.BOLD);
        sideSettings.setPadding(Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12));
        sideSettings.setOnClickListener(view -> openGestureSettings(
                GestureSettingsActivity.MODE_SIDE));
        sideGestureRow.addView(sideSettings);
        sideGestureSwitch = new Switch(this);
        sideGestureSwitch.setChecked(prefs.getBoolean(ConfigContract.KEY_SIDE_GESTURE_ENABLED,
                ConfigContract.DEFAULT_SIDE_GESTURE_ENABLED));
        sideGestureSwitch.setOnCheckedChangeListener((button, checked) ->
                store.putBoolean(ConfigContract.KEY_SIDE_GESTURE_ENABLED, checked));
        sideGestureRow.addView(sideGestureSwitch);
        gestureCard.addView(sideGestureRow);
        TextView gestureNote = text("两种手势上下分区：底角始终贴住物理边缘；侧滑只在其上方工作，短滑仍是系统返回。", 13, Ui.MUTED, Typeface.NORMAL);
        gestureNote.setPadding(0, Ui.dp(this, 2), 0, 0);
        gestureCard.addView(gestureNote);
        updateGestureSummaries();
        root.addView(gestureCard, cardParams(14));

        LinearLayout windowCard = card();
        windowCard.addView(text("小窗初始大小与位置", 18, Ui.TEXT, Typeface.BOLD));
        preview = new WindowPreviewView(this);
        preview.update(widthPercent, heightPercent, positionX, positionY);
        windowCard.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 250)));
        windowCard.addView(slider("宽度", ConfigContract.KEY_WIDTH_PERCENT, 40, 90, widthPercent, value -> value + "%"));
        windowCard.addView(slider("高度", ConfigContract.KEY_HEIGHT_PERCENT, 35, 85, heightPercent, value -> value + "%"));
        windowCard.addView(slider("水平位置", ConfigContract.KEY_POSITION_X, 0, 100, positionX, MainActivity::positionLabel));
        windowCard.addView(slider("垂直位置", ConfigContract.KEY_POSITION_Y, 0, 100, positionY, MainActivity::positionLabel));
        TextView boundsNote = text("实际启动时会自动避开状态栏、导航区域和屏幕边缘。", 13, Ui.MUTED, Typeface.NORMAL);
        boundsNote.setPadding(0, Ui.dp(this, 8), 0, 0);
        windowCard.addView(boundsNote);
        root.addView(windowCard, cardParams(14));

        LinearLayout behaviorCard = card();
        behaviorCard.addView(text("窗外快捷操作", 18, Ui.TEXT, Typeface.BOLD));
        behaviorCard.addView(actionRow("窗外·单击", ConfigContract.KEY_OUTSIDE_SINGLE_ACTION,
                ConfigContract.DEFAULT_OUTSIDE_SINGLE_ACTION));
        behaviorCard.addView(Ui.divider(this));
        behaviorCard.addView(actionRow("窗外·双击", ConfigContract.KEY_OUTSIDE_DOUBLE_ACTION,
                ConfigContract.DEFAULT_OUTSIDE_DOUBLE_ACTION));
        TextView behavior = text("小窗实际边界外四个方向全部使用同一组动作。左右边缘的点按正常执行窗外动作，实际侧滑仍优先交给系统；窗外滑动不执行动作。", 14, Ui.MUTED, Typeface.NORMAL);
        behavior.setLineSpacing(0, 1.18f);
        behavior.setPadding(0, Ui.dp(this, 8), 0, 0);
        behaviorCard.addView(behavior);
        root.addView(behaviorCard, cardParams(14));

        Button reset = compactButton("恢复默认参数");
        reset.setTextColor(Ui.MUTED);
        reset.setOnClickListener(view -> {
            store.resetTuning();
            recreate();
        });
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52));
        resetParams.topMargin = Ui.dp(this, 18);
        root.addView(reset, resetParams);
        return scroll;
    }

    private void openGestureSettings(String mode) {
        Intent intent = new Intent(this, GestureSettingsActivity.class);
        intent.putExtra(GestureSettingsActivity.EXTRA_MODE, mode);
        startActivity(intent);
    }

    private void updateGestureSummaries() {
        if (bottomGestureSummary != null) {
            int trigger = prefs.getInt(ConfigContract.KEY_TRIGGER_PERCENT,
                    ConfigContract.DEFAULT_TRIGGER_PERCENT);
            int icon = prefs.getInt(ConfigContract.KEY_ICON_SIZE_DP,
                    ConfigContract.DEFAULT_ICON_SIZE_DP);
            bottomGestureSummary.setText("贴边热区 " + hotWidthPercent + "% × "
                    + hotHeightPercent + "% · 距离 " + trigger + "% · " + icon + "dp");
        }
        if (sideGestureSummary != null) {
            int trigger = prefs.getInt(ConfigContract.KEY_SIDE_TRIGGER_PERCENT,
                    ConfigContract.DEFAULT_SIDE_TRIGGER_PERCENT);
            int safeTop = prefs.getInt(ConfigContract.KEY_SIDE_TOP_SAFE_MARGIN_PERCENT,
                    ConfigContract.DEFAULT_SIDE_TOP_SAFE_MARGIN_PERCENT);
            boolean names = prefs.getBoolean(ConfigContract.KEY_SIDE_SHOW_APP_NAMES,
                    ConfigContract.DEFAULT_SIDE_SHOW_APP_NAMES);
            sideGestureSummary.setText("长滑 " + trigger + "% · 顶部安全 "
                    + safeTop + "% · 名称" + (names ? "常显" : "选中显示"));
        }
        if (sideGestureSwitch != null) {
            boolean enabled = prefs.getBoolean(ConfigContract.KEY_SIDE_GESTURE_ENABLED,
                    ConfigContract.DEFAULT_SIDE_GESTURE_ENABLED);
            if (sideGestureSwitch.isChecked() != enabled) {
                sideGestureSwitch.setChecked(enabled);
            }
        }
    }

    private View actionRow(String title, String key, int defaultAction) {
        LinearLayout actionRow = row();
        TextView label = text(title, 15, Ui.TEXT, Typeface.BOLD);
        actionRow.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ACTION_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Math.max(0, Math.min(ACTION_LABELS.length - 1,
                prefs.getInt(key, defaultAction))), false);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (prefs.getInt(key, defaultAction) != position) store.putInt(key, position);
            }

            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        actionRow.addView(spinner, new LinearLayout.LayoutParams(Ui.dp(this, 116), Ui.dp(this, 52)));
        return actionRow;
    }

    private void renderApps() {
        appsContainer.removeAllViews();
        appsHint.setText(targets.size() + " / 8" + (targets.size() < 3 ? " · 至少选择 3 个" : " · 长按拖动排序"));
        if (targets.isEmpty()) {
            TextView empty = text("尚未选择应用，点击右上角“添加”。", 14, Ui.MUTED, Typeface.NORMAL);
            empty.setPadding(0, Ui.dp(this, 18), 0, Ui.dp(this, 6));
            appsContainer.addView(empty);
            return;
        }
        PackageManager pm = getPackageManager();
        for (AppTarget target : new ArrayList<>(targets)) {
            appsContainer.addView(Ui.divider(this));
            LinearLayout row = row();
            row.setTag(target);
            row.setOnDragListener((view, event) -> onAppDrag(target, event));

            ImageView icon = new ImageView(this);
            TextView label = text(target.packageName(), 16, Ui.TEXT, Typeface.BOLD);
            TextView packageName = text(target.packageName(), 12, Ui.MUTED, Typeface.NORMAL);
            try {
                ActivityInfo info = pm.getActivityInfo(target.componentName(), 0);
                Drawable drawable = info.loadIcon(pm);
                icon.setImageDrawable(drawable);
                CharSequence loaded = info.loadLabel(pm);
                if (loaded != null) label.setText(loaded);
            } catch (Exception ignored) {
                label.setText(target.packageName() + "（已卸载）");
            }
            row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));
            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setPadding(Ui.dp(this, 12), 0, 0, 0);
            labels.addView(label);
            labels.addView(packageName);
            row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            TextView drag = text("≡", 26, Ui.MUTED, Typeface.NORMAL);
            drag.setGravity(Gravity.CENTER);
            drag.setContentDescription("长按拖动排序");
            drag.setOnLongClickListener(view -> view.startDragAndDrop(
                    ClipData.newPlainText("component", target.component),
                    new View.DragShadowBuilder(row), target, 0));
            row.addView(drag, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));

            Button remove = compactButton("移除");
            remove.setEnabled(targets.size() > 3 || targets.size() < 3);
            remove.setOnClickListener(view -> {
                if (targets.size() == 3) {
                    Toast.makeText(this, "至少保留 3 个应用", Toast.LENGTH_SHORT).show();
                    return;
                }
                targets.remove(target);
                saveTargets();
            });
            row.addView(remove);
            appsContainer.addView(row);
        }
    }

    private boolean onAppDrag(AppTarget dropTarget, DragEvent event) {
        if (!(event.getLocalState() instanceof AppTarget)) return false;
        if (event.getAction() == DragEvent.ACTION_DROP) {
            AppTarget dragged = (AppTarget) event.getLocalState();
            int from = targets.indexOf(dragged);
            int to = targets.indexOf(dropTarget);
            if (from >= 0 && to >= 0 && from != to) {
                targets.remove(from);
                targets.add(Math.min(to, targets.size()), dragged);
                saveTargets();
            }
        }
        return true;
    }

    private void saveTargets() {
        store.setTargets(targets);
        renderApps();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_APP || resultCode != RESULT_OK || data == null) return;
        String flattened = data.getStringExtra(AppPickerActivity.EXTRA_COMPONENT);
        ComponentName component = ComponentName.unflattenFromString(flattened == null ? "" : flattened);
        if (component == null) return;
        AppTarget target = new AppTarget(component.flattenToString());
        if (targets.contains(target)) {
            Toast.makeText(this, "该应用已经在列表中", Toast.LENGTH_SHORT).show();
            return;
        }
        if (targets.size() >= 8) return;
        targets.add(target);
        saveTargets();
    }

    private View slider(String title, String key, int min, int max, int current, ValueLabel valueLabel) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, Ui.dp(this, 14), 0, 0);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView label = text(title, 15, Ui.TEXT, Typeface.BOLD);
        TextView value = text(valueLabel.label(current), 14, Ui.ACCENT, Typeface.BOLD);
        value.setGravity(Gravity.END);
        header.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(value);
        group.addView(header);

        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(Math.max(0, Math.min(max - min, current - min)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int resolved = min + progress;
                value.setText(valueLabel.label(resolved));
                if (!fromUser) return;
                store.putInt(key, resolved);
                if (ConfigContract.KEY_WIDTH_PERCENT.equals(key)) widthPercent = resolved;
                if (ConfigContract.KEY_HEIGHT_PERCENT.equals(key)) heightPercent = resolved;
                if (ConfigContract.KEY_POSITION_X.equals(key)) positionX = resolved;
                if (ConfigContract.KEY_POSITION_Y.equals(key)) positionY = resolved;
                if (ConfigContract.KEY_HOT_WIDTH_PERCENT.equals(key)) hotWidthPercent = resolved;
                if (ConfigContract.KEY_HOT_HEIGHT_PERCENT.equals(key)) hotHeightPercent = resolved;
                if (preview != null) preview.update(widthPercent, heightPercent, positionX, positionY);
                if (gesturePreview != null) gesturePreview.update(hotWidthPercent, hotHeightPercent);
            }
        });
        group.addView(seek);
        return group;
    }

    private void updateStatus() {
        if (statusText == null) return;
        String status = prefs.getString(ConfigContract.KEY_INTERFACE_STATUS, "");
        long time = prefs.getLong(ConfigContract.KEY_INTERFACE_TIME, 0L);
        if (status == null || status.isEmpty()) {
            statusText.setText("等待启用模块并重启 SystemUI");
            statusText.setTextColor(0xffa16c20);
        } else {
            String relative = time == 0 ? "" : " · " + DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            statusText.setText(status + relative);
            statusText.setTextColor(0xff287d4d);
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 17), Ui.dp(this, 18), Ui.dp(this, 17));
        card.setBackground(Ui.rounded(this, Ui.SURFACE, 20));
        card.setElevation(Ui.dp(this, 1));
        return card;
    }

    private LinearLayout.LayoutParams cardParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Ui.dp(this, topMarginDp);
        return params;
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

    private Button compactButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(13);
        button.setTextColor(Ui.ACCENT);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        button.setBackground(Ui.rounded(this, 0xffeef0ff, 12));
        return button;
    }

    private static String positionLabel(int value) {
        if (value == 0) return "起点";
        if (value == 50) return "居中";
        if (value == 100) return "终点";
        return value + "%";
    }

    private interface ValueLabel {
        String label(int value);
    }
}
