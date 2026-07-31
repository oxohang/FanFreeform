package com.oxohang.fanfreeform.xposed;

import com.oxohang.fanfreeform.config.ConfigContract;

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
        return iconCenter(corner, index, itemCount, width, height, radius, false);
    }

    static Point iconCenter(Corner corner, int index, int itemCount, int width, int height,
                            float radius, boolean fixedSevenRows) {
        return iconCenter(corner, index, itemCount, width, height, radius,
                fixedSevenRows ? ConfigContract.FAN_LAYOUT_FIXED_SEVEN
                        : ConfigContract.FAN_LAYOUT_SMART,
                7, 6, 5);
    }

    static Point iconCenter(Corner corner, int index, int itemCount, int width, int height,
                            float radius, int layoutMode, int outerCapacity,
                            int middleCapacity, int innerCapacity) {
        int[][] rows = fanRows(itemCount, layoutMode, outerCapacity,
                middleCapacity, innerCapacity);
        if (rows.length == 0) return new Point(corner == Corner.LEFT ? 0f : width, height);
        int row = 0;
        int start = 0;
        while (row < rows.length - 1 && index >= start + rows[row][0]) {
            start += rows[row][0];
            row++;
        }
        int rowCount = Math.max(1, rows[row][0]);
        int slotCount = Math.max(1, rows[row][1]);
        int radialIndex = rows[row][2];
        int rowIndex = Math.max(0, index - start);
        double inset = rows.length > 1 && (radialIndex & 1) == 1 ? 3.5 : 0.0;
        boolean custom = layoutMode == ConfigContract.FAN_LAYOUT_CUSTOM;
        double degrees;
        if (custom) {
            degrees = slotCount <= 1 ? 12.0 + inset
                    : 12.0 + inset
                    + ((70.0 - inset * 2.0) * rowIndex / (slotCount - 1));
        } else {
            degrees = rowCount <= 1 ? 47.0
                    : 12.0 + inset
                    + ((70.0 - inset * 2.0) * rowIndex / (rowCount - 1));
        }
        double radians = Math.toRadians(degrees);
        float rowRadius = custom || rows.length > 1
                ? radius * (0.68f + 0.20f * radialIndex) : radius;
        float horizontal = (float) (rowRadius * Math.cos(radians));
        float vertical = (float) (rowRadius * Math.sin(radians));
        return new Point(corner == Corner.LEFT ? horizontal : width - horizontal,
                height - vertical);
    }

    private static int[][] fanRows(int itemCount, int layoutMode, int outerCapacity,
                                   int middleCapacity, int innerCapacity) {
        int safeCount = Math.max(0, itemCount);
        if (safeCount == 0) return new int[0][0];
        if (layoutMode != ConfigContract.FAN_LAYOUT_CUSTOM) {
            boolean fixedSeven = layoutMode == ConfigContract.FAN_LAYOUT_FIXED_SEVEN;
            int[] counts = fanRowCounts(safeCount, fixedSeven);
            int[][] rows = new int[counts.length][3];
            for (int row = 0; row < counts.length; row++) {
                rows[row][0] = counts[row];
                rows[row][1] = counts[row];
                rows[row][2] = row;
            }
            return rows;
        }
        int outer = clampRowCapacity(outerCapacity,
                ConfigContract.MAX_FAN_CUSTOM_OUTER_COUNT);
        int middle = clampRowCapacity(middleCapacity,
                ConfigContract.MAX_FAN_CUSTOM_MIDDLE_COUNT);
        int inner = clampRowCapacity(innerCapacity,
                ConfigContract.MAX_FAN_CUSTOM_INNER_COUNT);
        int[] capacities = {inner, middle, outer};
        int[] actual = new int[3];
        int remaining = safeCount;
        for (int row = 0; row < capacities.length && remaining > 0; row++) {
            actual[row] = Math.min(capacities[row], remaining);
            remaining -= actual[row];
        }
        if (remaining > 0) actual[2] += remaining;
        int activeRows = 0;
        for (int count : actual) if (count > 0) activeRows++;
        int[][] rows = new int[activeRows][3];
        int output = 0;
        for (int row = 0; row < actual.length; row++) {
            if (actual[row] <= 0) continue;
            rows[output][0] = actual[row];
            rows[output][1] = actual[row];
            rows[output][2] = row;
            output++;
        }
        return rows;
    }

    private static int clampRowCapacity(int value, int maximum) {
        return Math.max(0, Math.min(maximum, value));
    }

    static int fanRowCount(int itemCount) {
        return fanRowCount(itemCount, false);
    }

    static int fanRowCount(int itemCount, boolean fixedSevenRows) {
        if (fixedSevenRows) {
            return Math.max(1, (Math.max(1, itemCount) + 6) / 7);
        }
        return Math.max(1, Math.min(3, (Math.max(1, itemCount) + 7) / 8));
    }

    static int[] fanRowCounts(int itemCount) {
        return fanRowCounts(itemCount, false);
    }

    static int[] fanRowCounts(int itemCount, boolean fixedSevenRows) {
        int safeCount = Math.max(0, itemCount);
        int rows = fanRowCount(safeCount, fixedSevenRows);
        int[] counts = new int[rows];
        if (safeCount == 0) return counts;
        if (fixedSevenRows) {
            int remaining = safeCount;
            for (int row = 0; row < rows; row++) {
                counts[row] = Math.min(7, remaining);
                remaining -= counts[row];
            }
            return counts;
        }
        int base = safeCount / rows;
        int remainder = safeCount % rows;
        for (int row = 0; row < rows; row++) {
            counts[row] = base + (row >= rows - remainder ? 1 : 0);
        }
        return counts;
    }

    static int selection(Corner corner, float x, float y, int width, int height,
                         int itemCount, float radius, float iconDiameter, float tolerance) {
        return selection(corner, x, y, width, height, itemCount, radius,
                iconDiameter, tolerance, false);
    }

    static int selection(Corner corner, float x, float y, int width, int height,
                         int itemCount, float radius, float iconDiameter, float tolerance,
                         boolean fixedSevenRows) {
        if (itemCount <= 0) return -1;
        float hitRadius = iconDiameter / 2f + tolerance;
        float hitRadiusSquared = hitRadius * hitRadius;
        for (int index = 0; index < itemCount; index++) {
            Point center = iconCenter(corner, index, itemCount, width, height, radius,
                    fixedSevenRows);
            if (squaredDistance(x, y, center.x, center.y) <= hitRadiusSquared) {
                return index;
            }
        }
        return -1;
    }

    static int selection(Corner corner, float x, float y, int width, int height,
                         int itemCount, float radius, float iconDiameter, float tolerance,
                         int layoutMode, int outerCapacity, int middleCapacity,
                         int innerCapacity) {
        if (itemCount <= 0) return -1;
        float hitRadius = iconDiameter / 2f + tolerance;
        float hitRadiusSquared = hitRadius * hitRadius;
        for (int index = 0; index < itemCount; index++) {
            Point center = iconCenter(corner, index, itemCount, width, height, radius,
                    layoutMode, outerCapacity, middleCapacity, innerCapacity);
            if (squaredDistance(x, y, center.x, center.y) <= hitRadiusSquared) {
                return index;
            }
        }
        return -1;
    }

    static float fanOutermostEdgeDistance(Corner corner, int itemCount,
                                          int width, int height, float radius,
                                          float iconDiameter, float safetyMargin,
                                          int layoutMode, int outerCapacity,
                                          int middleCapacity, int innerCapacity) {
        float originX = corner == Corner.LEFT ? 0f : width;
        float outermostCenter = 0f;
        for (int index = 0; index < itemCount; index++) {
            Point center = iconCenter(corner, index, itemCount, width, height, radius,
                    layoutMode, outerCapacity, middleCapacity, innerCapacity);
            outermostCenter = Math.max(outermostCenter,
                    distance(originX, height, center.x, center.y));
        }
        return outermostCenter + Math.max(0f, iconDiameter) / 2f
                + Math.max(0f, safetyMargin);
    }

    static boolean beyondFanEdge(Corner corner, float x, float y,
                                 int width, int height, float minimumDistance) {
        float originX = corner == Corner.LEFT ? 0f : width;
        return distance(originX, height, x, y) >= Math.max(0f, minimumDistance);
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

    static float sideListTopForAnchor(float requestedY, int itemCount, int anchorIndex,
                                      float rowHeight, float safeTop, float safeBottom) {
        if (itemCount <= 0 || rowHeight <= 0f) return safeTop;
        int anchor = Math.max(0, Math.min(itemCount - 1, anchorIndex));
        float listHeight = itemCount * rowHeight;
        if (listHeight >= safeBottom - safeTop) return safeTop;
        float requestedTop = requestedY - (anchor + 0.5f) * rowHeight;
        return Math.max(safeTop, Math.min(safeBottom - listHeight, requestedTop));
    }

    static float sideListCenterX(Corner side, float pointerX, int width,
                                 float iconDiameter, float edgeMargin,
                                 boolean followFinger) {
        float minimum = edgeMargin + iconDiameter / 2f;
        float maximum = width - minimum;
        if (!followFinger) return side == Corner.LEFT ? minimum : maximum;
        return Math.max(minimum, Math.min(maximum, pointerX));
    }

    static Point sideListIconCenter(Corner side, int index, int width, float listTop,
                                    float rowHeight, float iconDiameter, float edgeMargin) {
        float x = edgeMargin + iconDiameter / 2f;
        return new Point(side == Corner.LEFT ? x : width - x,
                listTop + (index + 0.5f) * rowHeight);
    }

    static Point sideListIconCenter(int index, float centerX, float listTop,
                                    float rowHeight) {
        return new Point(centerX, listTop + (index + 0.5f) * rowHeight);
    }

    static Point sideFanIconCenter(Corner side, int index, int itemCount, int width,
                                   float apexX, float centerY, float radius) {
        if (itemCount <= 0) return new Point(apexX, centerY);
        int anchor = (itemCount - 1) / 2;
        float maximumOffset = Math.max(1f, Math.max(anchor, itemCount - 1 - anchor));
        double degrees = 58.0 * (index - anchor) / maximumOffset;
        double radians = Math.toRadians(degrees);
        float horizontal = (float) (radius * Math.cos(radians));
        float vertical = (float) (radius * Math.sin(radians));
        return new Point(side == Corner.LEFT ? horizontal : width - horizontal,
                centerY + vertical);
    }

    static float sideFanCenterY(float requestedY, int itemCount, float radius,
                                float iconDiameter, float safeTop, float safeBottom) {
        if (itemCount <= 0) return requestedY;
        int anchor = (itemCount - 1) / 2;
        float maximumOffset = Math.max(1f, Math.max(anchor, itemCount - 1 - anchor));
        float minOffset = 0f;
        float maxOffset = 0f;
        for (int index = 0; index < itemCount; index++) {
            double degrees = 58.0 * (index - anchor) / maximumOffset;
            float offset = (float) (radius * Math.sin(Math.toRadians(degrees)));
            minOffset = Math.min(minOffset, offset);
            maxOffset = Math.max(maxOffset, offset);
        }
        float halfIcon = iconDiameter / 2f + 8f;
        float minimum = safeTop + halfIcon - minOffset;
        float maximum = safeBottom - halfIcon - maxOffset;
        if (minimum > maximum) return (safeTop + safeBottom) / 2f;
        return Math.max(minimum, Math.min(maximum, requestedY));
    }

    static int sideFanSelection(Corner side, float x, float y, int width,
                                int itemCount, float apexX, float centerY, float radius,
                                float iconDiameter, float tolerance) {
        if (itemCount <= 0) return -1;
        float hitRadius = iconDiameter / 2f + tolerance;
        float hitRadiusSquared = hitRadius * hitRadius;
        int nearest = -1;
        float nearestDistance = Float.MAX_VALUE;
        for (int index = 0; index < itemCount; index++) {
            Point center = sideFanIconCenter(side, index, itemCount, width,
                    apexX, centerY, radius);
            float distance = squaredDistance(x, y, center.x, center.y);
            if (distance <= hitRadiusSquared && distance < nearestDistance) {
                nearest = index;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    static float sideRingRadius(int itemCount, float iconDiameter, float gap) {
        if (itemCount <= 1) return iconDiameter * 1.45f;
        double halfStep = Math.PI / itemCount;
        float required = (float) ((iconDiameter + gap) / (2.0 * Math.sin(halfStep)));
        return Math.max(iconDiameter * 1.45f, required);
    }

    static float scaledSideRingRadius(float automaticRadius, int sizePercent,
                                      float maximumRadius) {
        float scale = Math.max(0.6f, Math.min(1.8f, sizePercent / 100f));
        return Math.max(1f, Math.min(Math.max(1f, maximumRadius),
                Math.max(1f, automaticRadius) * scale));
    }

    static Point sideRingCenter(float requestedX, float requestedY, float radius,
                                float iconDiameter, float safeLeft, float safeTop,
                                float safeRight, float safeBottom) {
        float outer = radius + iconDiameter / 2f;
        float minX = safeLeft + outer;
        float maxX = safeRight - outer;
        float minY = safeTop + outer;
        float maxY = safeBottom - outer;
        float x = minX > maxX ? (safeLeft + safeRight) / 2f
                : Math.max(minX, Math.min(maxX, requestedX));
        float y = minY > maxY ? (safeTop + safeBottom) / 2f
                : Math.max(minY, Math.min(maxY, requestedY));
        return new Point(x, y);
    }

    static Point sideRingIconCenter(int index, int itemCount,
                                    float centerX, float centerY, float radius) {
        if (itemCount <= 0) return new Point(centerX, centerY);
        double radians = -Math.PI / 2.0 + 2.0 * Math.PI * index / itemCount;
        return new Point(centerX + (float) (radius * Math.cos(radians)),
                centerY + (float) (radius * Math.sin(radians)));
    }

    static int sideRingSelection(float x, float y, int itemCount,
                                 float centerX, float centerY, float radius,
                                 float iconDiameter, float tolerance) {
        if (itemCount <= 0) return -1;
        float hitRadius = iconDiameter / 2f + tolerance;
        float hitRadiusSquared = hitRadius * hitRadius;
        int nearest = -1;
        float nearestDistance = Float.MAX_VALUE;
        for (int index = 0; index < itemCount; index++) {
            Point center = sideRingIconCenter(index, itemCount, centerX, centerY, radius);
            float distance = squaredDistance(x, y, center.x, center.y);
            if (distance <= hitRadiusSquared && distance < nearestDistance) {
                nearest = index;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    static int sideListSelection(float y, int itemCount, float listTop, float rowHeight) {
        if (itemCount <= 0 || rowHeight <= 0f || y < listTop
                || y >= listTop + itemCount * rowHeight) return -1;
        int index = (int) ((y - listTop) / rowHeight);
        return index >= 0 && index < itemCount ? index : -1;
    }

    static boolean insideSideList(float x, float y, float centerX, float hitWidth,
                                  int itemCount, float listTop, float rowHeight) {
        if (itemCount <= 0 || hitWidth <= 0f || rowHeight <= 0f) return false;
        return Math.abs(x - centerX) <= hitWidth / 2f
                && y >= listTop && y < listTop + itemCount * rowHeight;
    }

    static float sideReverseDistance(Corner side, float activationX, float pointerX) {
        return side == Corner.LEFT
                ? Math.max(0f, activationX - pointerX)
                : Math.max(0f, pointerX - activationX);
    }

    static float sideListOuterBoundary(Corner side, float centerX, float hitWidth) {
        float half = Math.max(0f, hitWidth) / 2f;
        return side == Corner.LEFT ? centerX - half : centerX + half;
    }

    static float sideListOpacity(float reverseDistance, float cancelDistance) {
        if (cancelDistance <= 0f) return 0f;
        float progress = Math.max(0f, Math.min(1f, reverseDistance / cancelDistance));
        return 1f - progress;
    }

    static boolean outsideSideListVertically(float y, int itemCount,
                                             float listTop, float rowHeight) {
        return itemCount <= 0 || rowHeight <= 0f || y < listTop
                || y >= listTop + itemCount * rowHeight;
    }

    static boolean beyondSideListInward(Corner side, float x, float centerX,
                                        float hitWidth) {
        float half = hitWidth / 2f;
        return side == Corner.LEFT ? x > centerX + half : x < centerX - half;
    }

    static int wheelSelection(float position, int itemCount) {
        if (itemCount <= 0) return -1;
        return Math.max(0, Math.min(itemCount - 1, Math.round(position)));
    }

    static float effectiveRadius(int itemCount, float configuredRadius,
                                 float minimumDiameter, float gap) {
        return effectiveRadius(itemCount, configuredRadius, minimumDiameter, gap, false);
    }

    static float effectiveRadius(int itemCount, float configuredRadius,
                                 float minimumDiameter, float gap,
                                 boolean fixedSevenRows) {
        return effectiveRadius(itemCount, configuredRadius, minimumDiameter, gap,
                fixedSevenRows ? ConfigContract.FAN_LAYOUT_FIXED_SEVEN
                        : ConfigContract.FAN_LAYOUT_SMART,
                7, 6, 5);
    }

    static float effectiveRadius(int itemCount, float configuredRadius,
                                 float minimumDiameter, float gap, int layoutMode,
                                 int outerCapacity, int middleCapacity, int innerCapacity) {
        int[][] rows = fanRows(itemCount, layoutMode, outerCapacity,
                middleCapacity, innerCapacity);
        float requiredRadius = configuredRadius;
        for (int row = 0; row < rows.length; row++) {
            int actualCount = rows[row][0];
            int slotCount = rows[row][1];
            int radialIndex = rows[row][2];
            if (actualCount <= 1 || slotCount <= 1) continue;
            double inset = rows.length > 1 && (radialIndex & 1) == 1 ? 3.5 : 0.0;
            double span = layoutMode == ConfigContract.FAN_LAYOUT_CUSTOM
                    ? 70.0 - inset * 2.0 : 58.0 - inset * 2.0;
            double stepRadians = Math.toRadians(span / (slotCount - 1));
            float rowFactor = layoutMode == ConfigContract.FAN_LAYOUT_CUSTOM
                    || rows.length > 1 ? 0.68f + 0.20f * radialIndex : 1f;
            float required = (float) ((minimumDiameter + gap)
                    / (2.0 * Math.sin(stepRadians / 2.0) * rowFactor));
            requiredRadius = Math.max(requiredRadius, required);
        }
        return requiredRadius;
    }

    static float effectiveIconDiameter(int itemCount, float radius,
                                       float configuredDiameter, float minimumDiameter, float gap) {
        return effectiveIconDiameter(itemCount, radius, configuredDiameter,
                minimumDiameter, gap, false);
    }

    static float effectiveIconDiameter(int itemCount, float radius,
                                       float configuredDiameter, float minimumDiameter, float gap,
                                       boolean fixedSevenRows) {
        return effectiveIconDiameter(itemCount, radius, configuredDiameter,
                minimumDiameter, gap,
                fixedSevenRows ? ConfigContract.FAN_LAYOUT_FIXED_SEVEN
                        : ConfigContract.FAN_LAYOUT_SMART,
                7, 6, 5);
    }

    static float effectiveIconDiameter(int itemCount, float radius,
                                       float configuredDiameter, float minimumDiameter, float gap,
                                       int layoutMode, int outerCapacity,
                                       int middleCapacity, int innerCapacity) {
        int[][] rows = fanRows(itemCount, layoutMode, outerCapacity,
                middleCapacity, innerCapacity);
        float available = configuredDiameter;
        for (int row = 0; row < rows.length; row++) {
            int actualCount = rows[row][0];
            int slotCount = rows[row][1];
            int radialIndex = rows[row][2];
            if (actualCount <= 1 || slotCount <= 1) continue;
            double inset = rows.length > 1 && (radialIndex & 1) == 1 ? 3.5 : 0.0;
            double span = layoutMode == ConfigContract.FAN_LAYOUT_CUSTOM
                    ? 70.0 - inset * 2.0 : 58.0 - inset * 2.0;
            double stepRadians = Math.toRadians(span / (slotCount - 1));
            float rowRadius = layoutMode == ConfigContract.FAN_LAYOUT_CUSTOM
                    || rows.length > 1
                    ? radius * (0.68f + 0.20f * radialIndex) : radius;
            float centerDistance = (float) (2.0 * rowRadius * Math.sin(stepRadians / 2.0));
            available = Math.min(available, centerDistance - gap);
        }
        return Math.max(minimumDiameter, available);
    }

    static OutsideRegion outsideRegion(float x, float y, int left, int top, int right, int bottom) {
        if (x >= left && x <= right && y >= top && y <= bottom) return OutsideRegion.INSIDE;
        return OutsideRegion.OUTSIDE;
    }

    static boolean insideEither(float x, float y,
                                int firstLeft, int firstTop, int firstRight, int firstBottom,
                                int secondLeft, int secondTop, int secondRight, int secondBottom,
                                boolean hasSecond) {
        if (outsideRegion(x, y, firstLeft, firstTop, firstRight, firstBottom)
                == OutsideRegion.INSIDE) return true;
        return hasSecond && outsideRegion(x, y, secondLeft, secondTop,
                secondRight, secondBottom) == OutsideRegion.INSIDE;
    }

    static boolean inSideGestureReserve(float x, int width, float leftReserve, float rightReserve) {
        return x <= leftReserve || x >= width - rightReserve;
    }

    static boolean shouldYieldSideTap(boolean fromLeft, float downX, float downY,
                                      float x, float y, float swipeDecisionDistance,
                                      float tapRetentionDistance) {
        float horizontal = fromLeft ? x - downX : downX - x;
        float vertical = Math.abs(y - downY);
        if (horizontal >= swipeDecisionDistance && horizontal >= vertical * 1.1f) {
            return true;
        }
        return distance(downX, downY, x, y) > tapRetentionDistance;
    }

    static boolean isIntentionalSideSwipe(Corner side, float downX, float downY,
                                           float x, float y, float decisionDistance) {
        if (side == null) return false;
        float inward = side == Corner.LEFT ? x - downX : downX - x;
        float vertical = Math.abs(y - downY);
        return inward >= decisionDistance && inward >= vertical * 1.1f;
    }

    private static float squaredDistance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return dx * dx + dy * dy;
    }
}
