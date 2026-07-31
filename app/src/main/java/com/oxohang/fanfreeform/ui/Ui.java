package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

final class Ui {
    static final int TEXT = Color.rgb(31, 34, 48);
    static final int MUTED = Color.rgb(112, 116, 134);
    static final int ACCENT = Color.rgb(90, 103, 242);
    static final int SURFACE = Color.WHITE;

    private Ui() {}

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable rounded(Context context, int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    static View divider(Context context) {
        View view = new View(context);
        view.setBackgroundColor(Color.rgb(235, 237, 244));
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 1)));
        return view;
    }

    static void addResetOption(Activity activity, LinearLayout parent,
                               String message, Runnable resetAction) {
        Button reset = new Button(activity);
        reset.setText("恢复本页默认设置");
        reset.setTextColor(MUTED);
        reset.setAllCaps(false);
        reset.setBackground(rounded(activity, SURFACE, 18));
        reset.setOnClickListener(view -> new AlertDialog.Builder(activity)
                .setTitle("恢复默认设置？")
                .setMessage(message)
                .setNegativeButton("取消", null)
                .setPositiveButton("确认恢复", (dialog, which) -> {
                    resetAction.run();
                    Toast.makeText(activity, "已恢复本页默认设置",
                            Toast.LENGTH_SHORT).show();
                    activity.recreate();
                })
                .show());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 52));
        params.topMargin = dp(activity, 18);
        parent.addView(reset, params);
    }

    static void addGlobalResetOption(Activity activity, LinearLayout parent,
                                     Runnable resetAction) {
        Button reset = new Button(activity);
        reset.setText("恢复全部默认设置");
        reset.setTextColor(MUTED);
        reset.setAllCaps(false);
        reset.setBackground(rounded(activity, SURFACE, 18));
        reset.setOnClickListener(view -> new AlertDialog.Builder(activity)
                .setTitle("恢复全部默认设置？")
                .setMessage("所有手势、动画、小窗位置和窗外点击参数将恢复默认值；已选应用和快捷方式会保留。")
                .setNegativeButton("取消", null)
                .setPositiveButton("确认恢复", (dialog, which) -> {
                    resetAction.run();
                    Toast.makeText(activity, "已恢复全部默认设置",
                            Toast.LENGTH_SHORT).show();
                    activity.recreate();
                })
                .show());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 52));
        params.topMargin = dp(activity, 18);
        parent.addView(reset, params);
    }
}
