package com.oxohang.fanfreeform.config;

public final class WindowSizePolicy {
    private WindowSizePolicy() { }

    public static int[] scaleNativeLogicalPixels(int nativeLogicalWidth,
                                                  int nativeLogicalHeight,
                                                  int maximumVisualWidth,
                                                  int maximumVisualHeight,
                                                  float freeformScale,
                                                  int userScalePercent) {
        if (nativeLogicalWidth <= 0 || nativeLogicalHeight <= 0
                || maximumVisualWidth <= 0 || maximumVisualHeight <= 0) {
            return new int[]{1, 1};
        }
        float normalizedUserScale = Math.max(0, userScalePercent) / 100f;
        float normalizedFreeformScale = freeformScale > 0f ? freeformScale : 1f;
        float requestedWidth = nativeLogicalWidth * normalizedUserScale;
        float requestedHeight = nativeLogicalHeight * normalizedUserScale;
        float visualWidth = requestedWidth * normalizedFreeformScale;
        float visualHeight = requestedHeight * normalizedFreeformScale;
        float fit = Math.min(1f, Math.min(maximumVisualWidth / visualWidth,
                maximumVisualHeight / visualHeight));
        return new int[]{
                Math.max(1, Math.round(requestedWidth * fit)),
                Math.max(1, Math.round(requestedHeight * fit))
        };
    }
}
