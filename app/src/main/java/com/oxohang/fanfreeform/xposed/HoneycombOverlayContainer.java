package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.widget.FrameLayout;

final class HoneycombOverlayContainer extends FrameLayout {
    private final HoneycombOverlayBackgroundView backgroundView;
    private final HoneycombOverlayView foregroundView;

    HoneycombOverlayContainer(Context context) {
        super(context);
        setWillNotDraw(true);
        backgroundView = new HoneycombOverlayBackgroundView(context);
        foregroundView = new HoneycombOverlayView(context);
        addView(backgroundView, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(foregroundView, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    HoneycombOverlayView foreground() {
        return foregroundView;
    }

    void configureBackground(GestureConfig config) {
        backgroundView.configure(config);
        foregroundView.setBackgroundLayer(backgroundView);
    }

    void setAppBackgroundColor(int color) {
        backgroundView.setAppBackgroundColor(color);
        foregroundView.setAppBackgroundColor(color);
    }

    void playEntry() {
        foregroundView.playEntry();
    }

    void releaseResources() {
        foregroundView.releaseResources();
        backgroundView.releaseResources();
    }
}
