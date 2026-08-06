package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;
import com.oxohang.fanfreeform.diagnostics.CompatibilityReport;

public final class MiscSettingsActivity extends Activity {
    private static final String BUG_REPORT_URL =
            "https://github.com/oxohang/FanFreeform/issues/new";
    private static final int MAX_PREFILLED_REPORT_CHARS = 12000;
    private ConfigStore store;
    private TextView status;
    private Button reload;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(content());
    }

    @Override protected void onResume() {
        super.onResume();
        updateStatus();
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

        LinearLayout processCard = card();
        processCard.addView(text("版本 " + versionLabel(), 15, Ui.TEXT, Typeface.BOLD));
        status = text("", 13, Ui.MUTED, Typeface.NORMAL);
        status.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 12));
        processCard.addView(status);
        reload = new Button(this);
        reload.setText("一键重载 Hook 进程");
        reload.setOnClickListener(view -> reloadProcesses());
        processCard.addView(reload, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        TextView reloadNote = text("固定热重载系统界面和小米桌面，不会重启手机。",
                13, Ui.MUTED, Typeface.NORMAL);
        reloadNote.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 12));
        processCard.addView(reloadNote);
        processCard.addView(Ui.divider(this));
        Button report = new Button(this);
        report.setText("一键导出兼容性报告");
        report.setOnClickListener(view -> shareCompatibilityReport());
        processCard.addView(report, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        TextView reportNote = text("用于排查其他 HyperOS 版本，包含系统与桌面版本、"
                        + "Hook 状态、缺失接口和配置摘要；不包含联系人及快捷方式内容。",
                13, Ui.MUTED, Typeface.NORMAL);
        reportNote.setPadding(0, Ui.dp(this, 8), 0, 0);
        processCard.addView(reportNote);
        processCard.addView(Ui.divider(this));
        processCard.addView(recentsClearButtonRow());
        processCard.addView(Ui.divider(this));
        processCard.addView(launcherIconRow());
        root.addView(processCard);

        LinearLayout feedbackCard = card();
        feedbackCard.addView(text("问题反馈", 17, Ui.TEXT, Typeface.BOLD));
        TextView feedbackNote = text("自动填写设备、系统、模块版本、Hook 状态和诊断摘要；"
                        + "提交前只需补充复现步骤、预期结果和实际结果。",
                13, Ui.MUTED, Typeface.NORMAL);
        feedbackNote.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 12));
        feedbackCard.addView(feedbackNote);
        Button feedback = new Button(this);
        feedback.setText("提交 Bug 反馈");
        feedback.setAllCaps(false);
        feedback.setOnClickListener(view -> openBugFeedback());
        feedbackCard.addView(feedback, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        root.addView(feedbackCard, layoutParams(14));

        Ui.addGlobalResetOption(this, root, () -> {
            store.resetTuning();
            setLauncherIconVisible(true);
        });
        return scroll;
    }

    private void openBugFeedback() {
        try {
            // Refresh the local report first so the existing “导出兼容性报告” action and
            // the issue body are based on the same snapshot.
            CompatibilityReport.create(this, store);
            String report = CompatibilityReport.build(this, store);
            String body = buildIssueBody(report);
            Uri issue = Uri.parse(BUG_REPORT_URL).buildUpon()
                    .appendQueryParameter("template", "bug_report.md")
                    .appendQueryParameter("title", "[Bug] ")
                    .appendQueryParameter("body", body)
                    .build();
            startActivity(new Intent(Intent.ACTION_VIEW, issue));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "未找到浏览器，请先安装浏览器后重试",
                    Toast.LENGTH_LONG).show();
        } catch (Throwable error) {
            Toast.makeText(this, "准备反馈信息失败：" + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private String buildIssueBody(String report) {
        String safeReport = report == null ? "" : report;
        boolean truncated = safeReport.length() > MAX_PREFILLED_REPORT_CHARS;
        if (truncated) {
            int half = MAX_PREFILLED_REPORT_CHARS / 2;
            safeReport = safeReport.substring(0, half)
                    + "\n\n...[报告中间内容因 GitHub 页面长度限制省略]...\n\n"
                    + safeReport.substring(safeReport.length() - half);
        }
        String reportNote = truncated
                ? "\n> 报告过长，正文只保留了首尾内容；如需完整报告，请使用应用内“导出兼容性报告”后作为附件补充。\n"
                : "";
        return "### 问题描述\n"
                + "<!-- 请先用一句话说明问题。 -->\n\n"
                + "### 复现步骤\n"
                + "1. \n2. \n3. \n\n"
                + "### 预期结果\n"
                + "<!-- 原本应该发生什么？ -->\n\n"
                + "### 实际结果\n"
                + "<!-- 实际发生了什么？是否完全失效、偶发失效或表现异常？ -->\n\n"
                + "### 自动诊断信息\n"
                + "以下内容由应用自动生成，请不要删除：\n\n"
                + "<details>\n<summary>展开兼容性报告</summary>\n\n"
                + "```text\n"
                + safeReport
                + "\n```\n\n</details>\n"
                + reportNote
                + "### 提交前检查\n"
                + "- [ ] 我已填写可复现问题的步骤。\n"
                + "- [ ] 我已填写预期结果和实际结果。\n"
                + "- [ ] 我已确认没有重复提交相同问题。\n";
    }

    private View recentsClearButtonRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("隐藏系统任务中心清除键", 16, Ui.TEXT, Typeface.BOLD));
        labels.addView(text("隐藏 HyperOS 后台底部的 ×/清除全部按钮，不影响单个任务操作",
                13, Ui.MUTED, Typeface.NORMAL));
        row.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(store.preferences().getBoolean(
                ConfigContract.KEY_HIDE_SYSTEM_RECENTS_CLEAR,
                ConfigContract.DEFAULT_HIDE_SYSTEM_RECENTS_CLEAR));
        toggle.setOnCheckedChangeListener((button, checked) ->
                store.putBoolean(ConfigContract.KEY_HIDE_SYSTEM_RECENTS_CLEAR, checked));
        row.addView(toggle);
        return row;
    }

    private View launcherIconRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 12), 0, 0);
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("在桌面显示软件图标", 16, Ui.TEXT, Typeface.BOLD));
        labels.addView(text("关闭后不再出现于桌面应用列表",
                13, Ui.MUTED, Typeface.NORMAL));
        row.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(isLauncherIconVisible());
        toggle.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                setLauncherIconVisible(true);
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("隐藏桌面图标？")
                    .setMessage("隐藏后仍可通过 hypergesture://settings 或 ADB 打开设置。")
                    .setNegativeButton("取消", (dialog, which) -> toggle.setChecked(true))
                    .setOnCancelListener(dialog -> toggle.setChecked(true))
                    .setPositiveButton("确认隐藏", (dialog, which) ->
                            setLauncherIconVisible(false))
                    .show();
        });
        row.addView(toggle);
        return row;
    }

    private boolean isLauncherIconVisible() {
        int state = getPackageManager().getComponentEnabledSetting(launcherAlias());
        return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

    private void setLauncherIconVisible(boolean visible) {
        getPackageManager().setComponentEnabledSetting(launcherAlias(),
                visible ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        Toast.makeText(this, visible ? "已显示桌面图标" : "已隐藏桌面图标",
                Toast.LENGTH_SHORT).show();
    }

    private ComponentName launcherAlias() {
        return new ComponentName(getPackageName(), getPackageName() + ".LauncherAlias");
    }

    private void shareCompatibilityReport() {
        try {
            Uri report = CompatibilityReport.create(this, store);
            Intent share = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "Hyper手势兼容性报告")
                    .putExtra(Intent.EXTRA_STREAM, report)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            share.setClipData(ClipData.newRawUri("Hyper手势兼容性报告", report));
            startActivity(Intent.createChooser(share, "发送兼容性报告"));
        } catch (Throwable error) {
            Toast.makeText(this, "导出失败：" + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String versionLabel() {
        try {
            android.content.pm.PackageInfo info = getPackageManager()
                    .getPackageInfo(getPackageName(), 0);
            return info.versionName + " · build " + info.getLongVersionCode();
        } catch (Exception ignored) {
            return "未知";
        }
    }

    private void reloadProcesses() {
        reload.setEnabled(false);
        reload.setText("正在重载…");
        new Thread(() -> {
            String error = null;
            try {
                runRoot("killall com.android.systemui");
                Thread.sleep(650L);
                runRoot("killall com.miui.home");
                Thread.sleep(450L);
                Intent home = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(home);
            } catch (Exception exception) {
                error = exception.getMessage();
            }
            String result = error;
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(this, result == null
                                ? "已热重载系统界面和小米桌面"
                                : "重载失败：" + result,
                        Toast.LENGTH_LONG).show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    reload.setEnabled(true);
                    reload.setText("一键重载 Hook 进程");
                    updateStatus();
                }, 5000L);
            });
        }, "hook-reload").start();
    }

    private static void runRoot(String command) throws Exception {
        Process process = new ProcessBuilder("su", "-c", command).start();
        int code = process.waitFor();
        if (code != 0) throw new IllegalStateException(command + " 退出码 " + code);
    }

    private void updateStatus() {
        if (status == null) return;
        String value = store.preferences().getString(ConfigContract.KEY_INTERFACE_STATUS, "");
        long time = store.preferences().getLong(ConfigContract.KEY_INTERFACE_TIME, 0);
        String relative = time <= 0 ? "暂无连接时间"
                : DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS).toString();
        status.setText((value == null || value.isEmpty() ? "Hook 尚未连接" : value)
                + " · " + relative);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 16),
                Ui.dp(this, 18), Ui.dp(this, 16));
        card.setBackground(Ui.rounded(this, Ui.SURFACE, 20));
        return card;
    }

    private LinearLayout.LayoutParams layoutParams(int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Ui.dp(this, top);
        return params;
    }

    private View header() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 52)));
        header.addView(text("杂项设置", 26, Ui.TEXT, Typeface.BOLD));
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
