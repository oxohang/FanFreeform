package com.oxohang.fanfreeform.diagnostics;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CompatibilityReport {
    public static final String AUTHORITY = "com.oxohang.fanfreeform.reports";
    public static final String FILE_NAME = "compatibility-report.txt";

    private CompatibilityReport() { }

    public static Uri create(Context context, ConfigStore store) throws Exception {
        File directory = new File(context.getCacheDir(), "reports");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("无法创建报告目录");
        }
        File report = new File(directory, FILE_NAME);
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(report, false), StandardCharsets.UTF_8)) {
            writer.write(build(context, store));
        }
        return Uri.parse("content://" + AUTHORITY + "/" + FILE_NAME);
    }

    static String build(Context context, ConfigStore store) {
        StringBuilder out = new StringBuilder();
        line(out, "Hyper手势兼容性报告");
        line(out, "生成时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                .format(new Date()));
        line(out, "说明: 报告不包含快捷方式名称、联系人和用户选择的应用清单。");

        section(out, "设备");
        line(out, "厂商: " + Build.MANUFACTURER);
        line(out, "品牌: " + Build.BRAND);
        line(out, "型号: " + Build.MODEL);
        line(out, "设备: " + Build.DEVICE);
        line(out, "产品: " + Build.PRODUCT);
        line(out, "Android: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
        line(out, "安全补丁: " + Build.VERSION.SECURITY_PATCH);
        line(out, "增量版本: " + Build.VERSION.INCREMENTAL);
        line(out, "Fingerprint: " + Build.FINGERPRINT);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        line(out, "当前显示: " + metrics.widthPixels + "x" + metrics.heightPixels
                + " @" + metrics.densityDpi + "dpi");

        section(out, "关键软件包");
        appendPackage(out, context, context.getPackageName(), "Hyper手势");
        appendPackage(out, context, "com.android.systemui", "系统界面");
        appendPackage(out, context, "com.miui.home", "小米桌面");
        appendOptionalPackage(out, context, "org.lsposed.manager", "LSPosed Manager");

        SharedPreferences preferences = store.preferences();
        section(out, "Hook 状态");
        line(out, "接口状态: " + preferences.getString(
                ConfigContract.KEY_INTERFACE_STATUS, "未报告"));
        line(out, "最后连接时间: " + preferences.getLong(
                ConfigContract.KEY_INTERFACE_TIME, 0L));
        appendDiagnostics(out, "SystemUI", preferences.getString(
                ConfigContract.KEY_DIAGNOSTICS_SYSTEM_UI, ""));
        appendDiagnostics(out, "MiuiHome", preferences.getString(
                ConfigContract.KEY_DIAGNOSTICS_MIUI_HOME, ""));

        section(out, "有效配置摘要");
        try {
            Bundle config = context.getContentResolver().call(
                    ConfigContract.URI, "get", null, null);
            if (config != null) {
                List<String> keys = new ArrayList<>(config.keySet());
                Collections.sort(keys);
                for (String key : keys) {
                    Object value = config.get(key);
                    if (value instanceof Boolean || value instanceof Number) {
                        line(out, key + "=" + value);
                    }
                }
            }
        } catch (Throwable error) {
            line(out, "配置读取失败: " + error.getClass().getName()
                    + ": " + error.getMessage());
        }
        line(out, "bottom_target_count=" + store.getTargets().size());
        line(out, "side_target_count=" + store.getSideTargets().size());
        line(out, "honeycomb_target_count=" + store.getHoneycombTargets().size());
        return out.toString();
    }

    @SuppressWarnings("deprecation")
    private static void appendPackage(StringBuilder out, Context context,
                                      String packageName, String label) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            line(out, label + ": " + packageName + " " + info.versionName
                    + " (" + info.getLongVersionCode() + ")");
            ApplicationInfo application = info.applicationInfo;
            if (application != null) {
                line(out, "  base=" + application.sourceDir);
                if (application.splitSourceDirs != null) {
                    for (String split : application.splitSourceDirs) {
                        line(out, "  split=" + split);
                    }
                }
            }
        } catch (Throwable error) {
            line(out, label + ": " + packageName + " <不可用> "
                    + error.getClass().getSimpleName());
        }
    }

    private static void appendOptionalPackage(StringBuilder out, Context context,
                                              String packageName, String label) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            appendPackage(out, context, packageName, label);
        } catch (PackageManager.NameNotFoundException ignored) { }
    }

    private static void appendDiagnostics(StringBuilder out, String process, String raw) {
        line(out, "-- " + process + " --");
        if (raw == null || raw.isEmpty()) {
            line(out, "暂无诊断记录；请先使用“一键重载 Hook 进程”。");
            return;
        }
        // Dynamic shortcut identifiers may contain a contact or account id. They are not
        // needed for ROM compatibility analysis, so redact the trailing identifier.
        String redacted = raw.replaceAll(
                "(?i)(shortcut(?: dispatched| started)?[^\\n]*/)[^\\s]+", "$1<redacted>");
        line(out, redacted);
    }

    private static void section(StringBuilder out, String title) {
        out.append('\n').append("=== ").append(title).append(" ===\n");
    }

    private static void line(StringBuilder out, String value) {
        out.append(value == null ? "" : value).append('\n');
    }
}
