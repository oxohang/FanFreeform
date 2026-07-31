package com.oxohang.fanfreeform.xposed;

final class FreeformOrientationPolicy {
    private static final int SCREEN_ORIENTATION_LANDSCAPE = 0;
    private static final int SCREEN_ORIENTATION_PORTRAIT = 1;
    private static final int SCREEN_ORIENTATION_SENSOR_LANDSCAPE = 6;
    private static final int SCREEN_ORIENTATION_REVERSE_LANDSCAPE = 8;
    private static final int SCREEN_ORIENTATION_REVERSE_PORTRAIT = 9;
    private static final int SCREEN_ORIENTATION_USER_LANDSCAPE = 11;
    private static final int SCREEN_ORIENTATION_USER_PORTRAIT = 12;

    private FreeformOrientationPolicy() { }

    static boolean shouldPreserveNativeLandscapeRestore(
            int displayWidth, int displayHeight, int requestedOrientation,
            int restoreWidth, int restoreHeight, int currentWidth, int currentHeight,
            boolean forcedLandscapeFromLockedPortrait) {
        if (displayWidth <= 0 || displayHeight <= 0) {
            return false;
        }
        if (displayWidth > displayHeight && !forcedLandscapeFromLockedPortrait) return false;
        if (isFixedLandscape(requestedOrientation)) return true;
        if (restoreWidth > 0 && restoreHeight > 0) return restoreWidth > restoreHeight;
        return currentWidth > 0 && currentHeight > 0 && currentWidth > currentHeight;
    }

    static boolean isFixedLandscape(int orientation) {
        return orientation == SCREEN_ORIENTATION_LANDSCAPE
                || orientation == SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                || orientation == SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                || orientation == SCREEN_ORIENTATION_USER_LANDSCAPE;
    }

    static boolean isFixedPortrait(int orientation) {
        return orientation == SCREEN_ORIENTATION_PORTRAIT
                || orientation == SCREEN_ORIENTATION_REVERSE_PORTRAIT
                || orientation == SCREEN_ORIENTATION_USER_PORTRAIT;
    }

    static boolean shouldUseNativeLandscapeLaunch(
            boolean rotationLockedToPortrait, int requestedOrientation) {
        return rotationLockedToPortrait && isFixedLandscape(requestedOrientation);
    }
}
