package com.oxohang.fanfreeform.xposed;

final class GestureGeometry {
    enum Corner { LEFT, RIGHT }

    private GestureGeometry() {}

    static Corner cornerAt(float x, float y, int width, int height, float hotSize) {
        if (y < height - hotSize) return null;
        if (x <= hotSize) return Corner.LEFT;
        if (x >= width - hotSize) return Corner.RIGHT;
        return null;
    }

    static boolean movesInward(Corner corner, float downX, float downY, float x, float y) {
        float horizontal = corner == Corner.LEFT ? x - downX : downX - x;
        float upward = downY - y;
        return horizontal > 0 && upward > 0;
    }

    static float distance(float downX, float downY, float x, float y) {
        return (float) Math.hypot(x - downX, y - downY);
    }

    static int selection(Corner corner, float x, float y, int width, int height,
                         int itemCount, float minimumRadius) {
        if (itemCount <= 0) return -1;
        float inward = corner == Corner.LEFT ? x : width - x;
        float upward = height - y;
        if (inward <= 0 || upward <= 0 || Math.hypot(inward, upward) < minimumRadius) return -1;
        double angle = Math.toDegrees(Math.atan2(upward, inward));
        final double minAngle = 12.0;
        final double maxAngle = 82.0;
        if (angle < minAngle - 8 || angle > maxAngle + 8) return -1;
        double normalized = (Math.max(minAngle, Math.min(maxAngle, angle)) - minAngle) / (maxAngle - minAngle);
        int index = itemCount == 1 ? 0 : (int) Math.round(normalized * (itemCount - 1));
        return Math.max(0, Math.min(itemCount - 1, index));
    }
}

