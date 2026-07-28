package com.oxohang.fanfreeform.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;

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
}

