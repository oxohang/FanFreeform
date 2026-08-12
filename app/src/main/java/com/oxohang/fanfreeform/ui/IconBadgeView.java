package com.oxohang.fanfreeform.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** An app icon with the same shortcut and dual-app badges used by the runtime overlays. */
final class IconBadgeView extends FrameLayout {
    private final ImageView icon;
    private final BadgeView shortcutBadge;
    private final BadgeView dualBadge;

    IconBadgeView(Context context) {
        super(context);
        setClipChildren(false);
        setClipToPadding(false);

        icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        addView(icon, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        shortcutBadge = new BadgeView(context, false);
        dualBadge = new BadgeView(context, true);
        addView(shortcutBadge, badgeParams(false));
        addView(dualBadge, badgeParams(false));
    }

    void setIcon(Drawable drawable, boolean shortcut, boolean dual) {
        icon.setImageDrawable(drawable);
        shortcutBadge.setVisibility(shortcut ? VISIBLE : GONE);
        dualBadge.setVisibility(dual ? VISIBLE : GONE);
        shortcutBadge.setLayoutParams(badgeParams(!dual));
        dualBadge.setLayoutParams(badgeParams(!dual || !shortcut));
    }

    private FrameLayout.LayoutParams badgeParams(boolean bottom) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                Ui.dp(getContext(), 18), Ui.dp(getContext(), 18));
        params.gravity = Gravity.END | (bottom ? Gravity.BOTTOM : Gravity.TOP);
        return params;
    }

    private static final class BadgeView extends View {
        private final boolean dual;

        BadgeView(Context context, boolean dual) {
            super(context);
            this.dual = dual;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            IconBadgeRenderer.drawBadge(canvas, getWidth() * 0.5f, getHeight() * 0.5f,
                    Math.min(getWidth(), getHeight()) * 0.78f, 1f, dual);
        }
    }
}
