package com.oxohang.fanfreeform.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Pure honeycomb slot allocation shared by the runtime overlay and settings preview. */
public final class HoneycombSlotLayout {
    private static final float SQRT_THREE_OVER_TWO = 0.8660254f;

    public static final class Point {
        public final float x;
        public final float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private HoneycombSlotLayout() { }

    public static List<Point> compactPoints(int count, float pitch) {
        if (count <= 0 || pitch <= 0f) return Collections.emptyList();
        int[] capacities = circularRowCapacities(count);
        ArrayList<Point> result = new ArrayList<>(count);
        float verticalPitch = pitch * SQRT_THREE_OVER_TWO;
        float centerRow = (capacities.length - 1) * 0.5f;
        for (int row = 0; row < capacities.length; row++) {
            int capacity = capacities[row];
            float y = (row - centerRow) * verticalPitch;
            int latticeRow = row - capacities.length / 2;
            float stagger = Math.floorMod(latticeRow, 2) == 0 ? 0f : 0.5f;
            ArrayList<Point> rowPoints = new ArrayList<>(capacity + 4);
            for (int column = -capacity - 2; column <= capacity + 2; column++) {
                rowPoints.add(new Point((column + stagger) * pitch, y));
            }
            rowPoints.sort(Comparator.comparingDouble(point -> Math.abs(point.x)));
            result.addAll(rowPoints.subList(0, capacity));
        }
        recenter(result);
        result.sort(Comparator.comparingDouble(HoneycombSlotLayout::squaredRadius)
                .thenComparingDouble(point -> Math.atan2(point.y, point.x)));
        return result;
    }

    public static int nearest(List<Point> points, float x, float y, float radius) {
        int best = -1;
        float bestDistance = radius * radius;
        for (int index = 0; index < points.size(); index++) {
            Point point = points.get(index);
            float dx = x - point.x;
            float dy = y - point.y;
            float distance = dx * dx + dy * dy;
            if (distance <= bestDistance) {
                best = index;
                bestDistance = distance;
            }
        }
        return best;
    }

    public static <T> void swap(List<T> values, int first, int second) {
        if (values == null || first < 0 || second < 0 || first >= values.size()
                || second >= values.size() || first == second) return;
        Collections.swap(values, first, second);
    }

    private static void recenter(ArrayList<Point> points) {
        if (points.isEmpty()) return;
        float sumX = 0f;
        float sumY = 0f;
        for (Point point : points) {
            sumX += point.x;
            sumY += point.y;
        }
        float centerX = sumX / points.size();
        float centerY = sumY / points.size();
        for (int index = 0; index < points.size(); index++) {
            Point point = points.get(index);
            points.set(index, new Point(point.x - centerX, point.y - centerY));
        }
    }

    private static int[] circularRowCapacities(int count) {
        int maximumRows = Math.max(1, Math.min(count,
                (int) Math.ceil(Math.sqrt(count) * 1.65f) + 2));
        int[] best = null;
        float bestScore = Float.MAX_VALUE;
        for (int rows = 1; rows <= maximumRows; rows++) {
            if (rows > count || (rows % 2 == 0 && count % 2 != 0)) continue;
            int[] candidate = allocateCircularRows(count, rows);
            int maximumCapacity = 0;
            for (int capacity : candidate) maximumCapacity = Math.max(maximumCapacity, capacity);
            float width = maximumCapacity;
            float height = (rows - 1) * SQRT_THREE_OVER_TWO + 1f;
            float aspectPenalty = Math.abs((float) Math.log(width / height));
            float targetRows = 1.20f * (float) Math.sqrt(count);
            float densityPenalty = Math.abs(rows - targetRows) * 0.018f;
            float score = aspectPenalty + densityPenalty;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best == null ? new int[] {count} : best;
    }

    private static int[] allocateCircularRows(int count, int rows) {
        int[] capacities = new int[rows];
        float[] targets = new float[rows];
        for (int row = 0; row < rows; row++) capacities[row] = 1;
        float center = (rows - 1) * 0.5f;
        float radius = Math.max(0.5f, rows * 0.5f);
        float weightSum = 0f;
        for (int row = 0; row < rows; row++) {
            float normalized = (row - center) / radius;
            targets[row] = (float) Math.sqrt(Math.max(0f, 1f - normalized * normalized));
            weightSum += targets[row];
        }
        int remaining = count - rows;
        for (int row = 0; row < rows; row++) {
            targets[row] = 1f + remaining * targets[row] / Math.max(0.001f, weightSum);
        }
        while (remaining > 0) {
            int bestRow = 0;
            float bestDeficit = -Float.MAX_VALUE;
            int bestMirrorImbalance = Integer.MAX_VALUE;
            for (int row = 0; row < rows; row++) {
                float deficit = targets[row] - capacities[row];
                int mirror = rows - 1 - row;
                int imbalance = Math.abs(capacities[row] + 1 - capacities[mirror]);
                if (deficit > bestDeficit + 0.0001f
                        || (Math.abs(deficit - bestDeficit) <= 0.0001f
                        && imbalance < bestMirrorImbalance)) {
                    bestDeficit = deficit;
                    bestRow = row;
                    bestMirrorImbalance = imbalance;
                }
            }
            capacities[bestRow]++;
            remaining--;
        }
        return capacities;
    }

    private static float squaredRadius(Point point) {
        return point.x * point.x + point.y * point.y;
    }
}
