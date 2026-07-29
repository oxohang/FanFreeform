package com.oxohang.fanfreeform.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.oxohang.fanfreeform.R;

final class SupportQrDialog {
    private SupportQrDialog() { }

    static void show(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(Ui.dp(context, 20), Ui.dp(context, 20),
                Ui.dp(context, 20), Ui.dp(context, 16));
        content.setBackground(Ui.rounded(context, Color.WHITE, 24));

        TextView title = text(context, "感谢支持 Hyper手势", 21, Ui.TEXT, Typeface.BOLD);
        content.addView(title);
        TextView subtitle = text(context, "每一杯支持，都会变成继续打磨体验的动力 ☕",
                14, Ui.MUTED, Typeface.NORMAL);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, Ui.dp(context, 6), 0, Ui.dp(context, 14));
        content.addView(subtitle);

        ImageView qr = new ImageView(context);
        qr.setImageResource(R.drawable.wechat_support_qr);
        qr.setAdjustViewBounds(true);
        qr.setScaleType(ImageView.ScaleType.FIT_CENTER);
        content.addView(qr, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button close = new Button(context);
        close.setText("关闭");
        close.setTextColor(Color.WHITE);
        close.setTextSize(15);
        close.setAllCaps(false);
        close.setBackground(Ui.rounded(context, Ui.ACCENT, 14));
        close.setOnClickListener(view -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 48));
        closeParams.topMargin = Ui.dp(context, 14);
        content.addView(close, closeParams);

        dialog.setContentView(content);
        dialog.setCanceledOnTouchOutside(true);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dialog.setOnShowListener(ignored -> {
            Window shownWindow = dialog.getWindow();
            if (shownWindow == null) return;
            int width = Math.round(context.getResources().getDisplayMetrics().widthPixels * 0.90f);
            shownWindow.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            android.view.WindowManager.LayoutParams attributes = shownWindow.getAttributes();
            attributes.dimAmount = 0.48f;
            shownWindow.setAttributes(attributes);
        });
        dialog.show();
    }

    private static TextView text(Context context, String value, float size,
                                 int color, int style) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(null, style);
        return text;
    }
}
