package com.oxohang.fanfreeform.xposed;

final class GestureGeometry {
    enum Corner { LEFT, RIGHT }
    enum OutsideRegion { INSIDE, OUTSIDE }

    static final class Point {
        final float x;
        final float y;

        Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private GestureGeometry() {}

    static Corner cornerAt(float x, float y, int width, int height,
                           float hotWidth, float hotHeight) {
        return cornerAt(x, y, width, height, hotWidth, hotHeight, 0f, 0f);
    }

    static Corner cornerAt(float x, float y, int width, int height,
                           float hotWidth, float hotHeight,
                           float leftInset, float rightInset) {
        if (y < height - hotHeight) return null;
        if (x >= leftInset && x <= leftInset + hotWidth) return Corner.LEFT;
        if (x <= width - rightInset && x >= width - rightInset - hotWidth) return Corner.RIGHT;
        return null;
    }

    static Corner sideAt(float x, int width, float leftWidth, float rightWidth) {
        if (x >= 0f && x <= leftWidth) return Corner.LEFT;
        if (x <= width && x >= width - rightWidth) return Corner.RIGHT;
        return null;
    }

    static Corner sideAt(float x, float y, int width, float leftWidth, float rightWidth,
                         float safeTop, float safeBottom, float minimumHeight) {
        if (safeBottom - safeTop < minimumHeight || y < safeTop || y > safeBottom) {
            return null;
        }
        return sideAt(x, width, leftWidth, rightWidth);
    }

    static float distance(float downX, float downY, float x, float y) {
        return (float) Math.hypot(x - downX, y - downY);
    }

    static Point iconCenter(Corner corner, int index, int itemCount, int width, int height,
                            float radius) {
        double degrees = itemCount <= 1 ? 47.0 : 12.0 + (70.0 * index / (itemCount - 1));
        double radians = Math.toRadians(degrees);
        float horizontal = (float) (radius * Math.cos(radians));
        float vertical = (float) (radius * Math.sin(radians));
        return new Point(corner == Corner.LEFT ? horizontal : width - horizontal,
                height - vertical);
    }

    static int selection(Corner corner, float x, float y, int width, int height,
                         int itemCount, float radius, float iconDiameter, float tolerance) {
        if (itemCount <= 0) return -1;
        float hitRadius = iconDiameter / 2f + tolerance;
        float hitRadiusSquared = hitRadius * hitRadius;
        for (int index = 0; index < itemCount; index++) {
            Point center = iconCenter(corner, index, itemCount, width, height, radius);
            if (squaredDistance(x, y, center.x, center.y) <= hitRadiusSquared) {
                return index;
            }
        }
        return -1;
    }

    static Point sideIconCenter(Corner side, int index, int itemCount,
                                int width, float originY, float radius) {
        double degrees = itemCount <= 1 ? 0.0 : -55.0 + (110.0 * index / (itemCount - 1));
        double radians = Math.toRadians(degrees);
        float horizontal = (float) (radius * Math.cos(radians));
        float vertical = (float) (radius * Math.sin(radians));
        return new Point(side == Corner.LEFT ? horizontal : width - horizontal,
                originY + vertical);
    }

    static float adjustedSideOriginY(float requestedY, int height, float radius,
                                     float iconDiameter, float safeTop, float safeBottom) {
        float span = (float) (radius * Math.sin(Math.toRadians(55.0)))
                + iconDiameter / 2f + 8f;
        float minimum = safeTop + span;
        float maximum = height - safeBottom - span;
        if (minimum > maximum) return (safeTop + height - safeBottom) / 2f;
        return Math.max(minimum, Math.min(maximum, requestedY));
    }

    static int sideSelection(Corner side, float x, float y, int width,
                             int itemCount, float originY, float radius,
                             float iconDiameter, float tolerance) {
        if (itemCount <= 0) return -1;
        float hitRadius = iconDiameter / 2f + tolerance;
        float hitRadiusSquared = hitRadius * hitRadius;
        for (int index = 0; index < itemCount; index++) {
            Point center = sideIconCenter(side, index, itemCount, width, originY, radius);
            if (squaredDistance(x, y, center.x, center.y) <= hitRadiusSquared) return index;
        }
        return -1;
    }

    static float sideListTop(float requestedCenterY, int itemCount, float rowHeight,
                             float safeTop, float safeBottom) {
        if (itemCount <= 0 || rowHeight <= 0f) return safeTop;
        float listHeight = itemCount * rowHeight;
        if (listHeight >= safeBottom - safeTop) return safeTop;
        return Math.max(safeTop,
                Math.min(safeBottom - listHeight, requestedCenterY - listHeight / 2f));
    }

    static Point sideListIconCenter(Corner side, int index, int width, float listTop,
                                    float rowHeight, float iconDiameter, float edgeMargin) {
        float x = edgeMargin + iconDiameter / 2f;
        return new Point(side == Corner.LEFT ? x : width - x,
                listTop + (index + 0.5f) * rowHeight);
    }

    static int sideListSelection(float y, int itemCount, float listTop, float rowHeight) {
        if (itemCount <= 0 || rowHeight <= 0f || y < listTop
                || y >= listTop + itemCount * rowHeight) return -1;
        int index = (int) ((y - listTop) / rowHeight);
        return index >= 0 && index < itemCount ? index : -1;
    }

    static float effectiveRadius(int itemCount, float configuredRadius,
                                 float minimumDiameter, float gap) {
        if (itemCount <= 1) return configuredRadius;
        double stepRadians = Math.toRadians(70.0 / (itemCount - 1));
        float required = (float) ((minimumDiameter + gap) / (2.0 * Math.sin(stepRadians / 2.0)));
        return Math.max(configuredRadius, required);
    }

    static float effectiveIconDiameter(int itemCount, float radius,
                                       float configuredDiameter, float minimumDiameter, float gap) {
        if (itemCount <= 1) return configuredDiameter;
        double stepRadians = Math.toRadians(70.0 / (itemCount - 1));
        float centerDistance = (float) (2.0 * radius * Math.sin(stepRadians / 2.0));
        return Math.max(minimumDiameter, Math.min(configuredDiameter, centerDistance - gap));
    }

    static OutsideRegion outsideRegion(float x, float y, int left, int top, int right, int bottom) {
        if (x >= left && x <= right && y >= top && y <= bottom) return OutsideRegion.INSIDE;
        return OutsideRegion.OUTSIDE;
    }

    static boolean inSideGestureReserve(float x, int width, float leftReserve, float rightReserve) {
        return x <= leftReserve || x >= width - rightReserve;
    }

    private static float squaredDistance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return dx * dx + dy * dy;
    }
}
