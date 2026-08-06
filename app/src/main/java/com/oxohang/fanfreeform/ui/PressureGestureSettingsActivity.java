package com.oxohang.fanfreeform.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;
import com.oxohang.fanfreeform.config.PressureHapticFeedback;

@SuppressLint("SetTextI18n")
public final class PressureGestureSettingsActivity extends Activity {
    private ConfigStore store;
    private SharedPreferences prefs;
    private SensorManager sensorManager;
    private Sensor pressureSensor;

    private PressurePreviewView preview;
    private TextView calibrationStatus;
    private TextView actionSummary;
    private TextView orbThemeSummary;
    private TextView pressureTargetsSummary;
    private Button calibrationButton;
    private Switch enabledToggle;
    private boolean calibrationActive;
    private final Handler calibrationHandler = new Handler(Looper.getMainLooper());
    private final Runnable calibrationStatusPoll = this::pollCalibrationStatus;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        prefs = store.preferences();
        sensorManager = getSystemService(SensorManager.class);
        pressureSensor = sensorManager == null ? null
                : sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().setNavigationBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildContent());
    }

    @Override protected void onPause() {
        super.onPause();
        if (calibrationActive) cancelCalibration(false);
        calibrationHandler.removeCallbacks(calibrationStatusPoll);
    }

    @Override protected void onResume() {
        super.onResume();
        if (prefs.getBoolean(ConfigContract.KEY_PRESSURE_CALIBRATION_ACTIVE,
                ConfigContract.DEFAULT_PRESSURE_CALIBRATION_ACTIVE)) {
            calibrationActive = true;
            if (preview != null) preview.setCalibrationActive(true);
            if (calibrationButton != null) calibrationButton.setText("取消校准");
            calibrationHandler.removeCallbacks(calibrationStatusPoll);
            calibrationHandler.post(calibrationStatusPoll);
        }
        updateActionSummary();
        updateOrbThemeSummary();
        updatePressureTargetsSummary();
        updateCalibrationStatus();
    }

    @Override protected void onDestroy() {
        calibrationHandler.removeCallbacks(calibrationStatusPoll);
        super.onDestroy();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xfff4f5fa);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 36));
        scroll.addView(root);

        LinearLayout header = row();
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 52)));
        header.addView(text("按压手势", 26, Ui.TEXT, Typeface.BOLD));
        root.addView(header);
        TextView subtitle = text("轻触确认位置，重按触发动作；仅竖屏生效",
                14, Ui.MUTED, Typeface.NORMAL);
        subtitle.setPadding(Ui.dp(this, 44), 0, 0, Ui.dp(this, 16));
        root.addView(subtitle);

        LinearLayout enableCard = card();
        LinearLayout enableRow = row();
        enableRow.addView(labels("启用按压手势",
                "落指只确认位置，气压升高达到校准值才执行动作"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        enabledToggle = new Switch(this);
        enabledToggle.setChecked(prefs.getBoolean(ConfigContract.KEY_PRESSURE_GESTURE_ENABLED,
                ConfigContract.DEFAULT_PRESSURE_GESTURE_ENABLED));
        enabledToggle.setOnCheckedChangeListener((button, checked) -> {
            if (checked && !prefs.getBoolean(ConfigContract.KEY_PRESSURE_CALIBRATED,
                    ConfigContract.DEFAULT_PRESSURE_CALIBRATED)) {
                button.setChecked(false);
                Toast.makeText(this, "请先完成气压校准", Toast.LENGTH_SHORT).show();
                return;
            }
            store.putBoolean(ConfigContract.KEY_PRESSURE_GESTURE_ENABLED, checked);
        });
        enableRow.addView(enabledToggle);
        enableCard.addView(enableRow);
        root.addView(enableCard);

        LinearLayout actionCard = card();
        actionCard.addView(text("单重按", 18, Ui.TEXT, Typeface.BOLD));
        LinearLayout actionRow = row();
        LinearLayout actionLabels = labels("执行动作", "识别到一个气压上升波形后执行");
        actionRow.addView(actionLabels, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        actionSummary = text("", 14, Ui.ACCENT, Typeface.BOLD);
        actionRow.addView(actionSummary);
        actionRow.addView(text("›", 28, Ui.ACCENT, Typeface.NORMAL));
        actionRow.setOnClickListener(view -> showPressureActionDialog());
        actionCard.addView(actionRow);
        root.addView(actionCard, cardParams());

        LinearLayout orbThemeCard = card();
        orbThemeCard.addView(text("按压动效主题", 18, Ui.TEXT, Typeface.BOLD));
        LinearLayout orbThemeRow = row();
        orbThemeRow.addView(labels("点阵动效", "按压时显示网页参考中的九种状态之一"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        orbThemeSummary = text("", 14, Ui.ACCENT, Typeface.BOLD);
        orbThemeRow.addView(orbThemeSummary);
        orbThemeRow.addView(text("›", 28, Ui.ACCENT, Typeface.NORMAL));
        orbThemeRow.setOnClickListener(view -> showOrbThemeDialog());
        orbThemeCard.addView(orbThemeRow);
        orbThemeCard.addView(slider("动效大小", ConfigContract.KEY_PRESSURE_ORB_SIZE_PERCENT,
                ConfigContract.MIN_PRESSURE_ORB_SIZE_PERCENT,
                ConfigContract.MAX_PRESSURE_ORB_SIZE_PERCENT,
                prefs.getInt(ConfigContract.KEY_PRESSURE_ORB_SIZE_PERCENT,
                        ConfigContract.DEFAULT_PRESSURE_ORB_SIZE_PERCENT), 1,
                value -> value + "%", () -> { }));
        root.addView(orbThemeCard, cardParams());

        LinearLayout launchCard = card();
        launchCard.addView(text("应用启动", 18, Ui.TEXT, Typeface.BOLD));
        launchCard.addView(toggleRow("以小窗打开应用", "关闭时使用全屏打开；默认关闭",
                ConfigContract.KEY_PRESSURE_OPEN_AS_FREEFORM,
                ConfigContract.DEFAULT_PRESSURE_OPEN_AS_FREEFORM));
        LinearLayout pressureTargetsRow = row();
        pressureTargetsRow.addView(labels("圆形应用快捷选择",
                "按压手势自己的独立应用与快捷方式清单"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        pressureTargetsSummary = text("", 14, Ui.ACCENT, Typeface.BOLD);
        pressureTargetsRow.addView(pressureTargetsSummary);
        pressureTargetsRow.addView(text("设置 ›", 14, Ui.ACCENT, Typeface.BOLD));
        pressureTargetsRow.setOnClickListener(view -> {
            android.content.Intent intent = new android.content.Intent(this,
                    TargetManagerActivity.class);
            intent.putExtra(TargetManagerActivity.EXTRA_KIND,
                    TargetManagerActivity.KIND_PRESSURE);
            startActivity(intent);
        });
        launchCard.addView(pressureTargetsRow);
        root.addView(launchCard, cardParams());

        LinearLayout hapticCard = card();
        hapticCard.addView(text("按压触感", 18, Ui.TEXT, Typeface.BOLD));
        hapticCard.addView(text("只在气压越过校准阈值时播放一段系统按键触感；总开关仍受“启用手势”页面的震动开关控制。",
                13, Ui.MUTED, Typeface.NORMAL));
        hapticCard.addView(toggleRow("重按系统反馈", "气压越过校准阈值时播放一次系统按键触感",
                ConfigContract.KEY_PRESSURE_SECOND_HAPTIC_ENABLED,
                ConfigContract.DEFAULT_PRESSURE_SECOND_HAPTIC_ENABLED));
        Button testPress = button("测试系统重按反馈");
        testPress.setOnClickListener(view -> testPressureHaptic());
        hapticCard.addView(testPress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 46)));
        root.addView(hapticCard, cardParams());

        LinearLayout previewCard = card();
        previewCard.addView(text("圆形按压范围", 18, Ui.TEXT, Typeface.BOLD));
        previewCard.addView(text("位置和大小使用屏幕百分比，按压识别仅在竖屏生效",
                13, Ui.MUTED, Typeface.NORMAL));
        preview = new PressurePreviewView(this);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 280));
        previewParams.topMargin = Ui.dp(this, 12);
        previewCard.addView(preview, previewParams);
        previewCard.addView(toggleRow("显示按压范围（测试）",
                "仅显示按压手势的目标圆，关闭后立即移除",
                ConfigContract.KEY_PRESSURE_SHOW_POSITION,
                ConfigContract.DEFAULT_PRESSURE_SHOW_POSITION));
        root.addView(previewCard, cardParams());

        LinearLayout tuning = card();
        tuning.addView(text("按压参数", 18, Ui.TEXT, Typeface.BOLD));
        tuning.addView(slider("圆心水平位置", ConfigContract.KEY_PRESSURE_CENTER_X_PERCENT,
                0, 100, prefs.getInt(ConfigContract.KEY_PRESSURE_CENTER_X_PERCENT,
                        ConfigContract.DEFAULT_PRESSURE_CENTER_X_PERCENT), 1,
                value -> value + "%", () -> preview.update()));
        tuning.addView(slider("圆心垂直位置", ConfigContract.KEY_PRESSURE_CENTER_Y_PERCENT,
                0, 100, prefs.getInt(ConfigContract.KEY_PRESSURE_CENTER_Y_PERCENT,
                        ConfigContract.DEFAULT_PRESSURE_CENTER_Y_PERCENT), 1,
                value -> value + "%", () -> preview.update()));
        tuning.addView(slider("圆形范围半径", ConfigContract.KEY_PRESSURE_RADIUS_PERCENT,
                ConfigContract.MIN_PRESSURE_RADIUS_PERCENT,
                ConfigContract.MAX_PRESSURE_RADIUS_PERCENT,
                prefs.getInt(ConfigContract.KEY_PRESSURE_RADIUS_PERCENT,
                        ConfigContract.DEFAULT_PRESSURE_RADIUS_PERCENT), 1,
                value -> value + "% 短边", () -> preview.update()));
        root.addView(tuning, cardParams());

        LinearLayout calibration = card();
        calibration.addView(text("气压校准", 18, Ui.TEXT, Typeface.BOLD));
        calibration.addView(text("开始后请在当前屏幕实际设置的按压圆形范围内完成 5 次按压，" +
                "不要按页面中的预览图。每次只记录按压期间的正向气压变化；气压变低的样本会被剔除，" +
                "最后用有效样本的最低值和中间值取平均作为阈值。校准期间会自动显示实际圆形位置。",
                13, Ui.MUTED, Typeface.NORMAL));
        calibrationStatus = text("", 14, Ui.TEXT, Typeface.BOLD);
        calibrationStatus.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 6));
        calibration.addView(calibrationStatus);
        calibrationButton = button("开始 5 次校准");
        calibrationButton.setOnClickListener(view -> {
            if (calibrationActive) cancelCalibration(true);
            else startCalibration();
        });
        calibration.addView(calibrationButton,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        Ui.dp(this, 50)));
        Button reset = button("清除校准结果");
        reset.setTextColor(Ui.MUTED);
        reset.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle("清除按压校准？")
                .setMessage("清除后按压手势将暂时无法触发，需重新完成 5 次校准。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清除", (dialog, which) -> clearCalibration())
                .show());
        calibration.addView(reset,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        Ui.dp(this, 50)));
        root.addView(calibration, cardParams());

        updateCalibrationStatus();
        updateActionSummary();
        updateOrbThemeSummary();
        updatePressureTargetsSummary();
        preview.update();
        return scroll;
    }

    private void showPressureActionDialog() {
        String[] choices = {"蜂窝应用", "圆形应用快捷选择", "返回主屏幕"};
        String key = ConfigContract.KEY_PRESSURE_ACTION;
        int current = prefs.getInt(key, ConfigContract.DEFAULT_PRESSURE_ACTION);
        current = Math.max(0, Math.min(choices.length - 1, current));
        new AlertDialog.Builder(this)
                .setTitle("选择重按动作")
                .setSingleChoiceItems(choices, current, (dialog, which) -> {
                    store.putInt(key, which);
                    updateActionSummary();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateActionSummary() {
        if (actionSummary == null) return;
        int action = prefs.getInt(ConfigContract.KEY_PRESSURE_ACTION,
                ConfigContract.DEFAULT_PRESSURE_ACTION);
        actionSummary.setText(pressureActionLabel(action));
    }

    private void showOrbThemeDialog() {
        String[] choices = orbThemeChoices();
        int current = prefs.getInt(ConfigContract.KEY_PRESSURE_ORB_THEME,
                ConfigContract.DEFAULT_PRESSURE_ORB_THEME);
        current = Math.max(ConfigContract.PRESSURE_ORB_ORBITS,
                Math.min(ConfigContract.PRESSURE_ORB_MORPH, current));
        new AlertDialog.Builder(this)
                .setTitle("选择按压动效主题")
                .setSingleChoiceItems(choices, current, (dialog, which) -> {
                    store.putInt(ConfigContract.KEY_PRESSURE_ORB_THEME, which);
                    updateOrbThemeSummary();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateOrbThemeSummary() {
        if (orbThemeSummary == null) return;
        int theme = prefs.getInt(ConfigContract.KEY_PRESSURE_ORB_THEME,
                ConfigContract.DEFAULT_PRESSURE_ORB_THEME);
        theme = Math.max(ConfigContract.PRESSURE_ORB_ORBITS,
                Math.min(ConfigContract.PRESSURE_ORB_MORPH, theme));
        orbThemeSummary.setText(orbThemeChoices()[theme]);
    }

    private static String[] orbThemeChoices() {
        return new String[]{
                "旋转点阵 · Working",
                "经纬球体 · Searching",
                "魔方解题 · Solving",
                "流动波形 · Listening",
                "连接网络 · Connecting",
                "交织编织 · Weaving",
                "飘带组合 · Composing",
                "呼吸圆环 · Thinking",
                "形态变换 · Shaping"
        };
    }

    private static String pressureActionLabel(int action) {
        if (action == ConfigContract.PRESSURE_ACTION_CIRCULAR) return "圆形快捷选择";
        if (action == ConfigContract.PRESSURE_ACTION_HOME) return "返回主屏幕";
        return "蜂窝应用";
    }

    private void testPressureHaptic() {
        try {
            PressureHapticFeedback.vibrate(this);
        } catch (Throwable error) {
            Toast.makeText(this, "当前设备无法播放此触感", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePressureTargetsSummary() {
        if (pressureTargetsSummary == null) return;
        pressureTargetsSummary.setText(store.getPressureTargets().size() + " 个");
    }

    private void startCalibration() {
        if (pressureSensor == null) {
            calibrationStatus.setText("当前手机没有可用的气压传感器");
            calibrationStatus.setTextColor(0xffb14242);
            return;
        }
        prefs.edit()
                .putBoolean(ConfigContract.KEY_PRESSURE_GESTURE_ENABLED, false)
                .putBoolean(ConfigContract.KEY_PRESSURE_CALIBRATED, false)
                .putFloat(ConfigContract.KEY_PRESSURE_THRESHOLD,
                        ConfigContract.DEFAULT_PRESSURE_THRESHOLD)
                .putInt(ConfigContract.KEY_PRESSURE_CALIBRATION_VALID_COUNT,
                        ConfigContract.DEFAULT_PRESSURE_CALIBRATION_VALID_COUNT)
                .putBoolean(ConfigContract.KEY_PRESSURE_CALIBRATION_ACTIVE, true)
                .putInt(ConfigContract.KEY_PRESSURE_CALIBRATION_ATTEMPTS,
                        ConfigContract.DEFAULT_PRESSURE_CALIBRATION_ATTEMPTS)
                .putFloat(ConfigContract.KEY_PRESSURE_CALIBRATION_LAST_DELTA,
                        ConfigContract.DEFAULT_PRESSURE_CALIBRATION_LAST_DELTA)
                .putBoolean(ConfigContract.KEY_PRESSURE_CALIBRATION_LAST_VALID,
                        ConfigContract.DEFAULT_PRESSURE_CALIBRATION_LAST_VALID)
                .apply();
        store.notifyChanged();
        if (enabledToggle != null) enabledToggle.setChecked(false);
        calibrationActive = true;
        calibrationButton.setText("取消校准");
        preview.setCalibrationActive(true);
        calibrationStatus.setText("请在当前屏幕的实际圆形范围内完成第 1/5 次按压");
        calibrationStatus.setTextColor(Ui.ACCENT);
        calibrationHandler.removeCallbacks(calibrationStatusPoll);
        calibrationHandler.post(calibrationStatusPoll);
    }

    private void cancelCalibration(boolean showMessage) {
        calibrationActive = false;
        prefs.edit()
                .putBoolean(ConfigContract.KEY_PRESSURE_CALIBRATION_ACTIVE, false)
                .putInt(ConfigContract.KEY_PRESSURE_CALIBRATION_ATTEMPTS,
                        ConfigContract.DEFAULT_PRESSURE_CALIBRATION_ATTEMPTS)
                .apply();
        store.notifyChanged();
        calibrationHandler.removeCallbacks(calibrationStatusPoll);
        if (preview != null) preview.setCalibrationActive(false);
        if (calibrationButton != null) calibrationButton.setText("开始 5 次校准");
        if (showMessage && calibrationStatus != null) {
            calibrationStatus.setText("已取消校准");
            calibrationStatus.setTextColor(Ui.MUTED);
        }
    }

    private void pollCalibrationStatus() {
        if (calibrationActive) {
            boolean active = prefs.getBoolean(ConfigContract.KEY_PRESSURE_CALIBRATION_ACTIVE,
                    ConfigContract.DEFAULT_PRESSURE_CALIBRATION_ACTIVE);
            if (!active) {
                calibrationActive = false;
                if (preview != null) preview.setCalibrationActive(false);
                if (calibrationButton != null) {
                    calibrationButton.setText("重新开始 5 次校准");
                }
                updateCalibrationStatus();
                return;
            }
            int attempts = prefs.getInt(ConfigContract.KEY_PRESSURE_CALIBRATION_ATTEMPTS,
                    ConfigContract.DEFAULT_PRESSURE_CALIBRATION_ATTEMPTS);
            boolean lastValid = prefs.getBoolean(
                    ConfigContract.KEY_PRESSURE_CALIBRATION_LAST_VALID,
                    ConfigContract.DEFAULT_PRESSURE_CALIBRATION_LAST_VALID);
            float lastDelta = prefs.getFloat(ConfigContract.KEY_PRESSURE_CALIBRATION_LAST_DELTA,
                    ConfigContract.DEFAULT_PRESSURE_CALIBRATION_LAST_DELTA);
            if (attempts <= 0) {
                calibrationStatus.setText("请在当前屏幕的实际圆形范围内完成第 1/5 次按压");
            } else if (lastValid) {
                calibrationStatus.setText("第 " + attempts + "/5 次有效，气压升高 "
                        + String.format(java.util.Locale.US, "%.3f", lastDelta)
                        + " hPa。请进行下一次按压");
            } else {
                calibrationStatus.setText("第 " + attempts
                        + "/5 次无效：气压没有升高，已剔除。请进行下一次按压");
            }
            calibrationStatus.setTextColor(Ui.ACCENT);
            calibrationHandler.postDelayed(calibrationStatusPoll, 250L);
            return;
        }
    }

    private void clearCalibration() {
        cancelCalibration(false);
        prefs.edit()
                .putBoolean(ConfigContract.KEY_PRESSURE_GESTURE_ENABLED, false)
                .putBoolean(ConfigContract.KEY_PRESSURE_CALIBRATED, false)
                .putFloat(ConfigContract.KEY_PRESSURE_THRESHOLD,
                        ConfigContract.DEFAULT_PRESSURE_THRESHOLD)
                .putInt(ConfigContract.KEY_PRESSURE_CALIBRATION_VALID_COUNT,
                        ConfigContract.DEFAULT_PRESSURE_CALIBRATION_VALID_COUNT)
                .putBoolean(ConfigContract.KEY_PRESSURE_CALIBRATION_ACTIVE,
                        ConfigContract.DEFAULT_PRESSURE_CALIBRATION_ACTIVE)
                .putInt(ConfigContract.KEY_PRESSURE_CALIBRATION_ATTEMPTS,
                        ConfigContract.DEFAULT_PRESSURE_CALIBRATION_ATTEMPTS)
                .putFloat(ConfigContract.KEY_PRESSURE_CALIBRATION_LAST_DELTA,
                        ConfigContract.DEFAULT_PRESSURE_CALIBRATION_LAST_DELTA)
                .putBoolean(ConfigContract.KEY_PRESSURE_CALIBRATION_LAST_VALID,
                        ConfigContract.DEFAULT_PRESSURE_CALIBRATION_LAST_VALID)
                .apply();
        store.notifyChanged();
        if (enabledToggle != null) enabledToggle.setChecked(false);
        updateCalibrationStatus();
        Toast.makeText(this, "已清除校准结果", Toast.LENGTH_SHORT).show();
    }

    private void updateCalibrationStatus() {
        if (calibrationStatus == null) return;
        if (pressureSensor == null) {
            calibrationStatus.setText("当前手机没有可用的气压传感器");
            calibrationStatus.setTextColor(0xffb14242);
            return;
        }
        boolean calibrated = prefs.getBoolean(ConfigContract.KEY_PRESSURE_CALIBRATED,
                ConfigContract.DEFAULT_PRESSURE_CALIBRATED);
        boolean active = prefs.getBoolean(ConfigContract.KEY_PRESSURE_CALIBRATION_ACTIVE,
                ConfigContract.DEFAULT_PRESSURE_CALIBRATION_ACTIVE);
        if (active) {
            int attempts = prefs.getInt(ConfigContract.KEY_PRESSURE_CALIBRATION_ATTEMPTS,
                    ConfigContract.DEFAULT_PRESSURE_CALIBRATION_ATTEMPTS);
            calibrationStatus.setText("正在进行实际屏幕校准：第 " + Math.min(5, attempts + 1)
                    + "/5 次按压");
            calibrationStatus.setTextColor(Ui.ACCENT);
            return;
        }
        if (!calibrated) {
            int attempts = prefs.getInt(ConfigContract.KEY_PRESSURE_CALIBRATION_ATTEMPTS,
                    ConfigContract.DEFAULT_PRESSURE_CALIBRATION_ATTEMPTS);
            calibrationStatus.setText(attempts >= 5
                    ? "校准失败：有效样本不足 3 个，请重新校准"
                    : "尚未校准：完成 5 次按压后才能启用");
            calibrationStatus.setTextColor(0xffa16c20);
            return;
        }
        float threshold = prefs.getFloat(ConfigContract.KEY_PRESSURE_THRESHOLD,
                ConfigContract.DEFAULT_PRESSURE_THRESHOLD);
        int count = prefs.getInt(ConfigContract.KEY_PRESSURE_CALIBRATION_VALID_COUNT,
                ConfigContract.DEFAULT_PRESSURE_CALIBRATION_VALID_COUNT);
        calibrationStatus.setText("已校准：" + count + " 个有效样本，阈值 "
                + String.format(java.util.Locale.US, "%.3f", threshold) + " hPa");
        calibrationStatus.setTextColor(0xff287d4d);
    }

    private View slider(String title, String key, int min, int max, int current, int step,
                        ValueLabel labeler, Runnable changed) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setPadding(0, Ui.dp(this, 14), 0, 0);
        LinearLayout header = row();
        TextView titleView = text(title, 15, Ui.TEXT, Typeface.BOLD);
        TextView valueView = text(labeler.label(current), 14, Ui.ACCENT, Typeface.BOLD);
        header.addView(titleView, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(valueView);
        group.addView(header);
        SeekBar seek = new SeekBar(this);
        int safeStep = Math.max(1, step);
        seek.setMax(Math.max(1, (max - min) / safeStep));
        seek.setProgress(Math.max(0, Math.min(seek.getMax(),
                Math.round((current - min) / (float) safeStep))));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            @Override public void onProgressChanged(SeekBar seekBar, int progress,
                                                    boolean fromUser) {
                int value = Math.min(max, min + progress * safeStep);
                valueView.setText(labeler.label(value));
                if (fromUser) {
                    store.putInt(key, value);
                    changed.run();
                }
            }
        });
        group.addView(seek);
        return group;
    }

    private interface ValueLabel { String label(int value); }

    private final class PressurePreviewView extends View {
        private final Paint screenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean calibrationCapture;
        private boolean pressInside;

        PressurePreviewView(Context context) {
            super(context);
            setFocusable(true);
            screenPaint.setStyle(Paint.Style.FILL);
            screenPaint.setColor(0xfff7f8fc);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(Ui.dp(context, 3));
            circlePaint.setColor(Ui.ACCENT);
            PathEffect dash = new DashPathEffect(new float[]{Ui.dp(context, 8), Ui.dp(context, 5)}, 0);
            circlePaint.setPathEffect(dash);
            centerPaint.setStyle(Paint.Style.FILL);
            centerPaint.setColor(0x555a67f2);
            labelPaint.setColor(Ui.MUTED);
            labelPaint.setTextSize(Ui.dp(context, 12));
            labelPaint.setTextAlign(Paint.Align.CENTER);
        }

        void setCalibrationActive(boolean active) {
            calibrationCapture = active;
            pressInside = false;
            invalidate();
        }

        void update() { invalidate(); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(0xffeef0f8);
            float left = getWidth() * 0.12f;
            float top = getHeight() * 0.06f;
            float right = getWidth() * 0.88f;
            float bottom = getHeight() * 0.94f;
            canvas.drawRoundRect(left, top, right, bottom, Ui.dp(getContext(), 22),
                    Ui.dp(getContext(), 22), screenPaint);
            float cx = left + (right - left) * prefs.getInt(
                    ConfigContract.KEY_PRESSURE_CENTER_X_PERCENT,
                    ConfigContract.DEFAULT_PRESSURE_CENTER_X_PERCENT) / 100f;
            float cy = top + (bottom - top) * prefs.getInt(
                    ConfigContract.KEY_PRESSURE_CENTER_Y_PERCENT,
                    ConfigContract.DEFAULT_PRESSURE_CENTER_Y_PERCENT) / 100f;
            float radius = Math.min(right - left, bottom - top)
                    * prefs.getInt(ConfigContract.KEY_PRESSURE_RADIUS_PERCENT,
                    ConfigContract.DEFAULT_PRESSURE_RADIUS_PERCENT) / 100f;
            if (pressInside) circlePaint.setColor(0xff287d4d);
            else circlePaint.setColor(Ui.ACCENT);
            canvas.drawCircle(cx, cy, radius, circlePaint);
            canvas.drawCircle(cx, cy, Ui.dp(getContext(), 5), centerPaint);
            canvas.drawText(calibrationCapture ? "实际屏幕校准中（此处不是校准区域）" : "按压范围预览",
                    getWidth() / 2f, bottom - Ui.dp(getContext(), 12), labelPaint);
        }
    }

    private Button button(String title) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextColor(Ui.ACCENT);
        button.setAllCaps(false);
        button.setBackground(Ui.rounded(this, 0xffeef0ff, 16));
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16));
        card.setBackground(Ui.rounded(this, Ui.SURFACE, 20));
        card.setElevation(Ui.dp(this, 1));
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Ui.dp(this, 14);
        return params;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        return row;
    }

    private LinearLayout labels(String title, String subtitle) {
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 16, Ui.TEXT, Typeface.BOLD));
        labels.addView(text(subtitle, 13, Ui.MUTED, Typeface.NORMAL));
        return labels;
    }

    private View toggleRow(String title, String subtitle, String key, boolean defaultValue) {
        LinearLayout setting = row();
        setting.addView(labels(title, subtitle), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(key, defaultValue));
        toggle.setOnCheckedChangeListener((button, checked) -> store.putBoolean(key, checked));
        setting.addView(toggle);
        return setting;
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
