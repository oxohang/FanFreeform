package com.oxohang.fanfreeform.xposed;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.oxohang.fanfreeform.config.ConfigContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Overlay for the configured pressure area and the Thinking Orbs renderers. */
final class PressureDebugOverlayController {
    private static final int TYPE_SYSTEM_UI_OVERLAY = 2024;
    private final Context context;
    private final Handler mainHandler;
    private final WindowManager windowManager;
    private PressureDebugOverlayView view;
    private boolean attached;

    PressureDebugOverlayController(Context context, Handler mainHandler) {
        this.context = context;
        this.mainHandler = mainHandler;
        windowManager = context.getSystemService(WindowManager.class);
    }

    void show(int centerXPercent, int centerYPercent, int radiusPercent) {
        if (!mainHandler.getLooper().isCurrentThread()) {
            mainHandler.post(() -> show(centerXPercent, centerYPercent, radiusPercent));
            return;
        }
        if (!ensureView()) return;
        view.configure(centerXPercent, centerYPercent, radiusPercent);
        view.setTargetVisible(true);
        view.setOrbActive(false);
    }

    void showOrb(int centerXPercent, int centerYPercent, int radiusPercent,
                 boolean showTarget, int theme, int sizePercent) {
        if (!mainHandler.getLooper().isCurrentThread()) {
            mainHandler.post(() -> showOrb(centerXPercent, centerYPercent, radiusPercent,
                    showTarget, theme, sizePercent));
            return;
        }
        if (!ensureView()) return;
        view.configure(centerXPercent, centerYPercent, radiusPercent);
        view.setOrbTheme(theme);
        view.setOrbSizePercent(sizePercent);
        view.setTargetVisible(showTarget);
        view.setOrbActive(true);
    }

    void setOrbTheme(int theme) {
        if (!mainHandler.getLooper().isCurrentThread()) {
            mainHandler.post(() -> setOrbTheme(theme));
            return;
        }
        if (view != null) view.setOrbTheme(theme);
    }

    void setOrbSizePercent(int sizePercent) {
        if (!mainHandler.getLooper().isCurrentThread()) {
            mainHandler.post(() -> setOrbSizePercent(sizePercent));
            return;
        }
        if (view != null) view.setOrbSizePercent(sizePercent);
    }

    void setOrbIntensity(float intensity) {
        if (!mainHandler.getLooper().isCurrentThread()) {
            mainHandler.post(() -> setOrbIntensity(intensity));
            return;
        }
        if (view != null) view.setOrbIntensity(intensity);
    }

    void hideOrb() {
        if (!mainHandler.getLooper().isCurrentThread()) {
            mainHandler.post(this::hideOrb);
            return;
        }
        if (view == null) return;
        view.setOrbActive(false);
        if (!view.isTargetVisible()) removeInternal();
    }

    void remove() {
        if (!mainHandler.getLooper().isCurrentThread()) {
            mainHandler.post(this::remove);
            return;
        }
        removeInternal();
    }

    private boolean ensureView() {
        if (windowManager == null) return false;
        if (attached && view != null) return true;
        PressureDebugOverlayView next = new PressureDebugOverlayView(context);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                TYPE_SYSTEM_UI_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.setFitInsetsTypes(0);
        params.setTitle("HyperGesturePressureOrb");
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams
                .LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        try {
            windowManager.addView(next, params);
            view = next;
            attached = true;
            return true;
        } catch (Throwable error) {
            Log.e("Cannot attach pressure debug overlay", error);
            return false;
        }
    }

    private void removeInternal() {
        PressureDebugOverlayView current = view;
        view = null;
        if (!attached || current == null || windowManager == null) {
            attached = false;
            return;
        }
        attached = false;
        try {
            windowManager.removeViewImmediate(current);
        } catch (Throwable error) {
            Log.e("Cannot remove pressure debug overlay", error);
        }
    }

    private static final class PressureDebugOverlayView extends View {
        private static final float TWO_PI = (float) (Math.PI * 2.0);
        private static final int DOTS_PER_ORBIT = 40;
        private final Paint targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint orbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int centerXPercent;
        private int centerYPercent;
        private int radiusPercent;
        private boolean targetVisible;
        private boolean orbActive;
        private int orbTheme = ConfigContract.DEFAULT_PRESSURE_ORB_THEME;
        private int orbSizePercent = ConfigContract.DEFAULT_PRESSURE_ORB_SIZE_PERCENT;
        private float orbIntensity = 0.35f;
        private long orbStartedAt;

        PressureDebugOverlayView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            targetPaint.setStyle(Paint.Style.STROKE);
            targetPaint.setStrokeWidth(dp(context, 3));
            targetPaint.setColor(0xff5a67f2);
            targetPaint.setPathEffect(new DashPathEffect(
                    new float[]{dp(context, 12), dp(context, 8)}, 0));
            orbPaint.setStyle(Paint.Style.FILL);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeCap(Paint.Cap.ROUND);
        }

        void configure(int centerXPercent, int centerYPercent, int radiusPercent) {
            this.centerXPercent = centerXPercent;
            this.centerYPercent = centerYPercent;
            this.radiusPercent = radiusPercent;
            invalidate();
        }

        void setTargetVisible(boolean visible) {
            targetVisible = visible;
            invalidate();
        }

        boolean isTargetVisible() {
            return targetVisible;
        }

        void setOrbActive(boolean active) {
            if (active && !orbActive) orbStartedAt = SystemClock.uptimeMillis();
            orbActive = active;
            invalidate();
        }

        void setOrbTheme(int theme) {
            orbTheme = Math.max(ConfigContract.PRESSURE_ORB_ORBITS,
                    Math.min(ConfigContract.PRESSURE_ORB_MORPH, theme));
            invalidate();
        }

        void setOrbSizePercent(int sizePercent) {
            orbSizePercent = Math.max(ConfigContract.MIN_PRESSURE_ORB_SIZE_PERCENT,
                    Math.min(ConfigContract.MAX_PRESSURE_ORB_SIZE_PERCENT, sizePercent));
            invalidate();
        }

        void setOrbIntensity(float intensity) {
            orbIntensity = Math.max(0f, Math.min(1f, intensity));
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float centerX = getWidth() * centerXPercent / 100f;
            float centerY = getHeight() * centerYPercent / 100f;
            if (targetVisible) {
                float radius = Math.min(getWidth(), getHeight()) * radiusPercent / 100f;
                canvas.drawCircle(centerX, centerY, radius, targetPaint);
            }
            if (!orbActive) return;
            drawOrb(canvas, centerX, centerY);
            postInvalidateOnAnimation();
        }

        @Override protected void onSizeChanged(int width, int height,
                                                int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            Log.i("Pressure overlay geometry=" + width + "x" + height);
        }

        private void drawOrb(Canvas canvas, float centerX, float centerY) {
            float elapsed = (SystemClock.uptimeMillis() - orbStartedAt) / 1000f;
            float size = dp(getContext(), 64f) * orbSizePercent / 100f;
            float time = elapsed * speedForTheme(orbTheme);
            ArrayList<Dot> dots = new ArrayList<>();
            ArrayList<Line> lines = new ArrayList<>();
            switch (orbTheme) {
                case ConfigContract.PRESSURE_ORB_GLOBE:
                    drawGlobe(dots, centerX, centerY, size, time);
                    break;
                case ConfigContract.PRESSURE_ORB_RUBIK:
                    drawRubik(dots, centerX, centerY, size, time);
                    break;
                case ConfigContract.PRESSURE_ORB_WAVE:
                    drawWave(dots, centerX, centerY, size, time);
                    break;
                case ConfigContract.PRESSURE_ORB_WEB:
                    drawWeb(dots, lines, centerX, centerY, size, time);
                    break;
                case ConfigContract.PRESSURE_ORB_BRAID:
                    drawBraid(dots, centerX, centerY, size, time);
                    break;
                case ConfigContract.PRESSURE_ORB_RIBBON:
                    drawBands(dots, centerX, centerY, size, time, false);
                    break;
                case ConfigContract.PRESSURE_ORB_RING:
                    drawBands(dots, centerX, centerY, size, time, true);
                    break;
                case ConfigContract.PRESSURE_ORB_MORPH:
                    drawMorph(dots, centerX, centerY, size, time);
                    break;
                case ConfigContract.PRESSURE_ORB_ORBITS:
                default:
                    drawOrbits(dots, centerX, centerY, size, time);
                    break;
            }
            drawLines(canvas, lines);
            drawDots(canvas, dots, minRadiusForTheme());
        }

        private void drawOrbits(List<Dot> dots, float cx, float cy, float size, float time) {
            Projector projector = projector(time * .12f, .3f, cx, cy, 1f);
            float scale = radiusScale(size, .6f);
            for (int orbit = 0; orbit < 12; orbit++) {
                float k = noise(orbit, 1.7f);
                float z = noise(orbit, 5.2f);
                float d = noise(orbit, 8.9f);
                float radius = size * .5f * .82f * (.45f + .52f * k);
                float f = k * TWO_PI;
                float v = (float) Math.acos(2f * z - 1f);
                float w = (float) Math.sin(v) * (float) Math.cos(f);
                float front = (float) Math.cos(v);
                float x = (float) Math.sin(v) * (float) Math.sin(f);
                float e = -front;
                float nn = w;
                float length = Math.max(1e-6f, (float) Math.sqrt(e * e + nn * nn));
                e /= length;
                nn /= length;
                float j = front * 0f - x * nn;
                float u = x * e - w * 0f;
                float a = w * nn - front * e;
                float band = (.25f + .55f * d) * (d > .5f ? 1f : -1f);
                for (int point = 0; point < DOTS_PER_ORBIT; point++) {
                    float angle = point / (float) DOTS_PER_ORBIT * TWO_PI;
                    float[] xyz = {
                            (e * (float) Math.cos(angle) + j * (float) Math.sin(angle)) * radius,
                            (nn * (float) Math.cos(angle) + u * (float) Math.sin(angle)) * radius,
                            (a * (float) Math.sin(angle)) * radius
                    };
                    float[] p = projector.project(xyz[0], xyz[1], xyz[2]);
                    float depth = (p[2] / Math.max(1f, radius) + 1f) / 2f;
                    dots.add(new Dot(p[0], p[1], p[2], .9f * scale, .72f,
                            .5f * (.4f + .6f * depth)));
                }
                for (int particle = 0; particle < 3; particle++) {
                    float angle = time * band + particle / 3f * TWO_PI + z * 6f;
                    float[] xyz = {
                            (e * (float) Math.cos(angle) + j * (float) Math.sin(angle)) * radius,
                            (nn * (float) Math.cos(angle) + u * (float) Math.sin(angle)) * radius,
                            (a * (float) Math.sin(angle)) * radius
                    };
                    float[] p = projector.project(xyz[0], xyz[1], xyz[2]);
                    float depth = (p[2] / Math.max(1f, radius) + 1f) / 2f;
                    dots.add(new Dot(p[0], p[1], p[2],
                            (1.2f + 1.6f * depth) * scale, .3f - .22f * depth,
                            1f));
                }
            }
        }

        private void drawGlobe(List<Dot> dots, float cx, float cy, float size, float time) {
            float half = size / 2f;
            float scale = half * .82f;
            float tilt = .4f + .06f * (float) Math.sin(time * .35f);
            Projector projector = projector(time * .5f, tilt, cx, cy, scale);
            float scan = time * (.5f + 1.2f * 4.08f);
            float dotScale = radiusScale(size, .6f) * 1.15f;
            for (int lat = 0; lat <= 11; lat++) {
                float latitude = (float) (-Math.PI / 2 + lat / 11f * Math.PI);
                float cosLat = (float) Math.cos(latitude);
                float sinLat = (float) Math.sin(latitude);
                int longitudes = Math.max(1, Math.round(Math.abs(cosLat) * 29));
                for (int lon = 0; lon < longitudes; lon++) {
                    float angle = lon / (float) longitudes * TWO_PI;
                    float[] p = projector.project(cosLat * (float) Math.cos(angle),
                            sinLat, cosLat * (float) Math.sin(angle));
                    float depth = (p[2] + 1f) / 2f;
                    float distance = angularDistance(angle + time * .5f, scan);
                    float active = (float) Math.exp(-(distance * distance) / .18f)
                            * Math.max(0f, p[2]);
                    dots.add(new Dot(p[0], p[1], p[2],
                            (.6f + 1.955f * depth + active) * dotScale,
                            .62f - .54f * depth,
                            .45f + .55f * Math.min(1f, active)));
                }
            }
        }

        private void drawRubik(List<Dot> dots, float cx, float cy, float size, float time) {
            float scale = size / 2f * .82f;
            Projector projector = projector(time * .55f,
                    .35f + .1f * (float) Math.sin(time * .9f), cx, cy, scale);
            Move[] moves = createMoves(14);
            Stage stage = stage(time, 14, .42f, 1.2f);
            float dotScale = radiusScale(size, .6f) * 1.05f;
            for (int lat = 0; lat <= 9; lat++) {
                float latitude = (float) (-Math.PI / 2 + lat / 9f * Math.PI);
                float cosLat = (float) Math.cos(latitude);
                float sinLat = (float) Math.sin(latitude);
                int longitudes = Math.max(1, Math.round(Math.abs(cosLat) * 24));
                for (int lon = 0; lon < longitudes; lon++) {
                    float angle = lon / (float) longitudes * TWO_PI;
                    Rotated rotated = applyMoves(new float[]{
                            cosLat * (float) Math.cos(angle), sinLat,
                            cosLat * (float) Math.sin(angle)}, moves, stage);
                    float[] p = projector.project(rotated.x, rotated.y, rotated.z);
                    float depth = (p[2] + 1f) / 2f;
                    dots.add(new Dot(p[0], p[1], p[2],
                            (.63f + 1.785f * depth + (rotated.active ? .315f : 0f))
                                    * dotScale,
                            .62f - .54f * depth - (rotated.active ? .14f : 0f),
                            1f));
                }
            }
        }

        private void drawWave(List<Dot> dots, float cx, float cy, float size, float time) {
            float scale = size / 2f * .874f;
            Projector projector = projector(time * .18f, .38f, cx, cy, 1f);
            float dotScale = radiusScale(size, .6f);
            for (int ring = 0; ring <= 9; ring++) {
                float latitude = (float) (-Math.PI / 2 + ring / 9f * Math.PI);
                float cosLat = (float) Math.cos(latitude);
                float sinLat = (float) Math.sin(latitude);
                float wave = .62f * (float) Math.sin(time * 2.1f - ring * .52f)
                        + .38f * (float) Math.sin(time * 1.27f + ring * .83f);
                float ringScale = scale * (.88f + .105f * wave);
                int longitudes = Math.max(1, Math.round(Math.abs(cosLat) * 23));
                for (int lon = 0; lon < longitudes; lon++) {
                    float angle = lon / (float) longitudes * TWO_PI;
                    float[] p = projector.project(cosLat * (float) Math.cos(angle)
                                    * ringScale, sinLat * ringScale,
                            cosLat * (float) Math.sin(angle) * ringScale);
                    float depth = (p[2] / scale + 1f) / 2f;
                    float active = Math.max(0f, wave);
                    dots.add(new Dot(p[0], p[1], p[2],
                            (.6f + 1.7f * depth) * (1f + .4f * active) * dotScale,
                            .66f - .56f * depth - .1f * active,
                            1f));
                }
            }
        }

        private void drawBraid(List<Dot> dots, float cx, float cy, float size, float time) {
            float scale = size / 2f * .76f;
            Projector projector = projector(time * .4f, .3f, cx, cy, 1f);
            float dotScale = radiusScale(size, .6f);
            for (int ghost = 0; ghost < 75; ghost++) {
                float[] sphere = spherePoint(ghost, 75);
                float[] p = projector.project(sphere[0] * scale, sphere[1] * scale,
                        sphere[2] * scale);
                float depth = (p[2] / scale + 1f) / 2f;
                dots.add(new Dot(p[0], p[1], p[2], .8f * dotScale, .78f,
                        .1f + .22f * depth));
            }
            int strands = 3;
            int points = 26;
            for (int strand = 0; strand < strands; strand++) {
                float phase = strand / 3f * TWO_PI;
                for (int point = 0; point < points; point++) {
                    float d = (fract(point / (float) points + time * .045f) * 2f - 1f) * .96f;
                    float a = (float) Math.sqrt(Math.max(0f, 1f - d * d));
                    float fade = Math.min(1f, (1f - Math.abs(d)) / .1f);
                    float angle = (float) (d * Math.PI * 3f + phase);
                    float wobble = 1f + .075f * (float) Math.sin(d * Math.PI * 3f * 2f
                            + phase * 2f + time * .8f);
                    float radial = a * scale * wobble;
                    float[] p = projector.project((float) Math.cos(angle) * radial,
                            d * scale * wobble, (float) Math.sin(angle) * radial);
                    float depth = (p[2] / scale + 1f) / 2f;
                    dots.add(new Dot(p[0], p[1], p[2],
                            (1.2f + 1.8f * depth) * dotScale,
                            .55f - .45f * depth, fade * (.45f + .55f * depth)));
                }
            }
        }

        private void drawBands(List<Dot> dots, float cx, float cy, float size,
                               float time, boolean faceOn) {
            float scale = size / 2f * .78f;
            float spin = 0f;
            float tilt = .3f;
            Projector projector = projector(time * .1f * spin, tilt, cx, cy, 1f);
            float dotScale = radiusScale(size, .6f);
            int ghostCount = faceOn ? 0 : 38;
            for (int ghost = 0; ghost < ghostCount; ghost++) {
                float[] sphere = spherePoint(ghost, ghostCount);
                float[] p = projector.project(sphere[0] * scale, sphere[1] * scale,
                        sphere[2] * scale);
                float depth = (p[2] / scale + 1f) / 2f;
                dots.add(new Dot(p[0], p[1], p[2], .8f * dotScale, .78f,
                        .1f + .22f * depth));
            }
            float y = time * .24f * spin;
            float k = faceOn ? -.3f : .55f;
            float cosY = (float) Math.cos(y);
            float sinY = (float) Math.sin(y);
            float f = -sinY * (float) Math.sin(k);
            float v = (float) Math.cos(k);
            float w = cosY * (float) Math.sin(k);
            float s = -sinY * v;
            float x = -cosY * w;
            float e = cosY * v;
            float wobble = faceOn ? .368f : 1f;
            float bandScale = faceOn ? scale / (1f + .85f * wobble) : scale;
            int lanes = 2;
            int segments = 44;
            int bands = Math.max(1, Math.round(lanes * (faceOn ? 3.627f : 3.9f)));
            for (int band = 0; band < bands; band++) {
                float offset = (band - (bands - 1) / 2f) * .075f;
                float edge = Math.abs(band - (bands - 1) / 2f)
                        / Math.max(1f, (bands - 1) / 2f);
                for (int segment = 0; segment < segments; segment++) {
                    float angle = segment / (float) segments * TWO_PI;
                    float curve = (.16f * (float) Math.sin(angle * 3f - time * 1.7f
                            + band * .22f) + .07f * (float) Math.sin(angle * 5f
                            + time * 1.1f)) * wobble;
                    float bandT = faceOn ? 1f + curve : 1f;
                    float bandI = faceOn ? offset : offset + curve;
                    float xx = cosY * (float) Math.cos(angle)
                            + f * (float) Math.sin(angle) + s * bandI;
                    float yy = (float) Math.cos(angle) + v * (float) Math.sin(angle)
                            + x * bandI;
                    float zz = sinY * (float) Math.cos(angle)
                            + w * (float) Math.sin(angle) + e * bandI;
                    float length = Math.max(1e-6f, (float) Math.sqrt(xx * xx + yy * yy
                            + zz * zz));
                    float radius = bandScale * bandT;
                    float[] p = projector.project(xx / length * radius,
                            yy / length * radius, zz / length * radius);
                    float depth = (p[2] / scale + 1f) / 2f;
                    float baseRadius = faceOn ? 1.0516f : .935f;
                    float depthRadius = faceOn ? 1.6252f : 1.445f;
                    dots.add(new Dot(p[0], p[1], p[2],
                            (baseRadius + depthRadius * depth)
                                    * (1f - .25f * edge) * dotScale,
                            .52f - .44f * depth + (faceOn ? .18f * edge : 0f),
                            .4f + .6f * depth));
                }
            }
        }

        private void drawWeb(List<Dot> dots, List<Line> lines, float cx, float cy,
                             float size, float time) {
            float scale = size / 2f * .8f;
            Projector projector = projector(time * .12f, .32f, cx, cy, scale);
            int nodeCount = 41;
            float[][] nodes = new float[nodeCount][3];
            for (int node = 0; node < nodeCount; node++) {
                float[] sphere = spherePoint(node, nodeCount);
                float vx = sphere[0] + .3f * (noise2(node * .31f + 9f,
                        time * .24f) - .5f) * 2f;
                float vy = sphere[1] + .3f * (noise2(node * .53f + 27f,
                        time * .21f) - .5f) * 2f;
                float vz = sphere[2] + .3f * (noise2(node * .77f + 55f,
                        time * .27f) - .5f) * 2f;
                float length = Math.max(1e-6f, (float) Math.sqrt(vx * vx + vy * vy + vz * vz));
                nodes[node][0] = vx / length;
                nodes[node][1] = vy / length;
                nodes[node][2] = vz / length;
            }
            for (int left = 0; left < nodeCount; left++) {
                for (int right = left + 1; right < nodeCount; right++) {
                    float dx = nodes[left][0] - nodes[right][0];
                    float dy = nodes[left][1] - nodes[right][1];
                    float dz = nodes[left][2] - nodes[right][2];
                    float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (distance >= .72f) continue;
                    float[] p1 = projector.project(nodes[left][0], nodes[left][1], nodes[left][2]);
                    float[] p2 = projector.project(nodes[right][0], nodes[right][1], nodes[right][2]);
                    float depth = (p1[2] + p2[2] + 2f) / 4f;
                    lines.add(new Line(p1[0], p1[1], p2[0], p2[1], .42f,
                            (1f - distance / .72f) * (.3f + .55f * depth),
                            Math.max(.6f, .8f * radiusScale(size, .6f))));
                }
            }
            for (int node = 0; node < nodeCount; node++) {
                float[] p = projector.project(nodes[node][0], nodes[node][1], nodes[node][2]);
                float depth = (p[2] + 1f) / 2f;
                float grow = 1f + .25f * (float) Math.sin(time * 1.4f + node * 2.7f);
                dots.add(new Dot(p[0], p[1], p[2],
                        (1.33f + 1.71f * depth) * grow * radiusScale(size, .6f),
                        .55f - .45f * depth, 1f));
            }
            for (int signal = 0; signal < 5; signal++) {
                int tick = (int) Math.floor(time * .55f + signal * 7.31f);
                int from = Math.floorMod((int) Math.floor(noise(tick, signal * 3.1f + 1.7f)
                        * nodeCount), nodeCount);
                int to = Math.floorMod((int) Math.floor(noise(tick, signal * 5.7f + 4.2f)
                        * nodeCount), nodeCount);
                if (from == to) continue;
                float amount = fract(time * .55f + signal * 7.31f);
                float vx = lerp(nodes[from][0], nodes[to][0], amount);
                float vy = lerp(nodes[from][1], nodes[to][1], amount);
                float vz = lerp(nodes[from][2], nodes[to][2], amount);
                float length = Math.max(1e-6f, (float) Math.sqrt(vx * vx + vy * vy + vz * vz));
                float[] p = projector.project(vx / length, vy / length, vz / length);
                float depth = (p[2] + 1f) / 2f;
                dots.add(new Dot(p[0], p[1], p[2],
                        (1.33f * 1.5f + 1.71f * depth) * radiusScale(size, .6f),
                        .05f, .5f + .5f * depth));
            }
        }

        private void drawMorph(List<Dot> dots, float cx, float cy, float size, float time) {
            float phaseLength = 1.4f + .9f;
            float cycle = fract(time / phaseLength / 3f) * phaseLength * 3f;
            int shapeIndex = Math.min(2, (int) Math.floor(cycle / phaseLength));
            float phase = cycle - shapeIndex * phaseLength;
            float blend = phase > 1.4f ? smoothStep((phase - 1.4f) / .9f) : 0f;
            float spread = 1.45f;
            float[][] shape = new float[160][2];
            for (int index = 0; index < shape.length; index++) {
                float progress = index / (float) shape.length;
                float[] current = shapePoint(shapeIndex, progress);
                float[] next = shapePoint((shapeIndex + 1) % 3, progress);
                shape[index][0] = (current[0] + (next[0] - current[0]) * blend) * spread;
                shape[index][1] = (current[1] + (next[1] - current[1]) * blend) * spread;
            }
            float[] lengths = new float[shape.length];
            float total = 0f;
            for (int index = 0; index < shape.length; index++) {
                float[] a = shape[index];
                float[] b = shape[(index + 1) % shape.length];
                lengths[index] = (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
                total += lengths[index];
            }
            int dotsCount = Math.max(6, Math.round(34f * .702f));
            float dotRadius = .021f * 1.35f * spread;
            float wobble = 1f + .02f * (float) Math.sin(phase * 3.1f);
            float accumulated = 0f;
            int segment = 0;
            for (int index = 0; index < dotsCount; index++) {
                float distance = index / (float) dotsCount * total;
                while (segment < shape.length - 1
                        && accumulated + lengths[segment] < distance) {
                    accumulated += lengths[segment++];
                }
                float[] a = shape[segment];
                float[] b = shape[(segment + 1) % shape.length];
                float local = lengths[segment] == 0f ? 0f
                        : Math.min(1f, (distance - accumulated) / lengths[segment]);
                float x = a[0] + (b[0] - a[0]) * local;
                float y = a[1] + (b[1] - a[1]) * local;
                dots.add(new Dot(cx + x * size * wobble,
                        cy + y * size * wobble, 0f,
                        Math.max(.35f, dotRadius * size), .1f, 1f));
            }
        }

        private void drawLines(Canvas canvas, List<Line> lines) {
            boolean dark = isDarkTheme();
            for (Line line : lines) {
                if (line.alpha < .02f) continue;
                linePaint.setColor(shade(line.white, dark));
                linePaint.setAlpha(alpha(line.alpha));
                linePaint.setStrokeWidth(line.width);
                canvas.drawLine(line.x1, line.y1, line.x2, line.y2, linePaint);
            }
        }

        private void drawDots(Canvas canvas, List<Dot> dots, float minRadius) {
            dots.sort(Comparator.comparingDouble(dot -> dot.z));
            boolean dark = isDarkTheme();
            for (Dot dot : dots) {
                if (dot.alpha < .02f) continue;
                orbPaint.setColor(shade(dot.white, dark));
                orbPaint.setAlpha(alpha(dot.alpha));
                canvas.drawCircle(dot.x, dot.y, Math.max(minRadius, dot.radius), orbPaint);
            }
        }

        private float minRadiusForTheme() {
            return orbTheme == ConfigContract.PRESSURE_ORB_MORPH ? .25f : .3f;
        }

        private boolean isDarkTheme() {
            return (getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        }

        private static int shade(float white, boolean dark) {
            float value = clamp01(dark ? 1f - white : white);
            int channel = Math.max(0, Math.min(255, Math.round(value * 255f)));
            return Color.rgb(channel, channel, channel);
        }

        private static int alpha(float value) {
            return Math.max(0, Math.min(255, Math.round(clamp01(value) * 255f)));
        }

        private static float speedForTheme(int theme) {
            switch (theme) {
                case ConfigContract.PRESSURE_ORB_GLOBE: return 2.015f;
                case ConfigContract.PRESSURE_ORB_RUBIK: return 1.82f;
                case ConfigContract.PRESSURE_ORB_WAVE: return 4.388f;
                case ConfigContract.PRESSURE_ORB_WEB: return 3.315f;
                case ConfigContract.PRESSURE_ORB_BRAID: return 1.625f;
                case ConfigContract.PRESSURE_ORB_RIBBON: return 2.34f;
                case ConfigContract.PRESSURE_ORB_RING: return 3.24f;
                case ConfigContract.PRESSURE_ORB_MORPH: return 2.405f;
                case ConfigContract.PRESSURE_ORB_ORBITS:
                default: return 1.885f;
            }
        }

        private static Projector projector(float yaw, float tilt,
                                           float centerX, float centerY, float scale) {
            return new Projector(yaw, tilt, centerX, centerY, scale);
        }

        private static float radiusScale(float size, float power) {
            return (float) Math.pow(size / 300f, power);
        }

        private static float[] spherePoint(int index, int count) {
            float golden = (float) (Math.PI * (3.0 - Math.sqrt(5.0)));
            float y = 1f - 2f * (index + .5f) / count;
            float radius = (float) Math.sqrt(Math.max(0f, 1f - y * y));
            float theta = index * golden;
            return new float[]{radius * (float) Math.cos(theta), y,
                    radius * (float) Math.sin(theta)};
        }

        private static float noise(float x, float y) {
            double value = Math.sin(x * 12.9898 + y * 78.233) * 43758.5453;
            return (float) (value - Math.floor(value));
        }

        private static float noise2(float x, float y) {
            int ix = (int) Math.floor(x);
            int iy = (int) Math.floor(y);
            float fx = smoothStep(x - ix);
            float fy = smoothStep(y - iy);
            float a = noise(ix, iy);
            float b = noise(ix + 1, iy);
            float c = noise(ix, iy + 1);
            float d = noise(ix + 1, iy + 1);
            return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
        }

        private static float fract(float value) {
            return value - (float) Math.floor(value);
        }

        private static float smoothStep(float value) {
            value = clamp01(value);
            return value * value * (3f - 2f * value);
        }

        private static float clamp01(float value) {
            return Math.max(0f, Math.min(1f, value));
        }

        private static float lerp(float a, float b, float amount) {
            return a + (b - a) * amount;
        }

        private static float angularDistance(float a, float b) {
            return (float) Math.atan2(Math.sin(a - b), Math.cos(a - b));
        }

        private static Move[] createMoves(int count) {
            Move[] moves = new Move[count];
            for (int index = 0; index < count; index++) {
                int axis = Math.min(2, (int) Math.floor(noise(index, 2.3f) * 3f));
                float layer = -1f + .5f * Math.min(3f,
                        (float) Math.floor(noise(index, 5.9f) * 4f));
                float direction = noise(index, 7.7f) < .5f ? 1f : -1f;
                moves[index] = new Move(axis, layer, layer + .5f,
                        direction * (float) (Math.PI / 2));
            }
            return moves;
        }

        private static Stage stage(float time, int count, float amount, float rest) {
            float cycle = 2f * count * amount + rest;
            float current = time % cycle;
            float[] values = new float[count];
            Arrays.fill(values, 0f);
            int active = -1;
            if (current < 2f * count * amount) {
                int index = (int) Math.floor(current / amount);
                float local = (current - index * amount) / amount;
                float eased = 1f - (float) Math.pow(1f - Math.min(1f, local / .7f), 3);
                if (index < count) {
                    for (int i = 0; i < index; i++) values[i] = 1f;
                    values[index] = eased;
                    active = index;
                } else {
                    int reverse = 2 * count - 1 - index;
                    for (int i = 0; i < reverse; i++) values[i] = 1f;
                    values[reverse] = 1f - eased;
                    active = reverse;
                }
            }
            return new Stage(values, active);
        }

        private static Rotated applyMoves(float[] vector, Move[] moves, Stage stage) {
            float x = vector[0];
            float y = vector[1];
            float z = vector[2];
            boolean active = false;
            for (int index = 0; index < moves.length; index++) {
                if (stage.amount[index] <= 0f) continue;
                Move move = moves[index];
                float coordinate = move.axis == 0 ? x : move.axis == 1 ? y : z;
                if (coordinate < move.low || coordinate >= move.high) continue;
                if (index == stage.active) active = true;
                float angle = move.angle * stage.amount[index];
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                if (move.axis == 0) {
                    float nextY = y * cos - z * sin;
                    z = y * sin + z * cos;
                    y = nextY;
                } else if (move.axis == 1) {
                    float nextX = x * cos + z * sin;
                    z = -x * sin + z * cos;
                    x = nextX;
                } else {
                    float nextX = x * cos - y * sin;
                    y = x * sin + y * cos;
                    x = nextX;
                }
            }
            return new Rotated(x, y, z, active);
        }

        private static float[] shapePoint(int shape, float progress) {
            if (shape == 0) {
                float angle = (float) (-Math.PI / 2 + progress * TWO_PI);
                return new float[]{(float) Math.cos(angle) * .24f,
                        (float) Math.sin(angle) * .24f};
            }
            if (shape == 1) {
                return pointOnPath(new float[][]{{0f, -.26f}, {.24f, .16f},
                        {-.24f, .16f}}, progress);
            }
            return pointOnPath(new float[][]{{0f, -.2f}, {.2f, -.2f}, {.2f, .2f},
                    {-.2f, .2f}, {-.2f, -.2f}}, progress);
        }

        private static float[] pointOnPath(float[][] path, float progress) {
            float total = 0f;
            float[] lengths = new float[path.length];
            for (int index = 0; index < path.length; index++) {
                float[] a = path[index];
                float[] b = path[(index + 1) % path.length];
                lengths[index] = (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
                total += lengths[index];
            }
            float distance = progress * total;
            int segment = 0;
            while (segment < path.length - 1 && distance > lengths[segment]) {
                distance -= lengths[segment++];
            }
            float[] a = path[segment];
            float[] b = path[(segment + 1) % path.length];
            float amount = lengths[segment] == 0f ? 0f : distance / lengths[segment];
            return new float[]{lerp(a[0], b[0], amount), lerp(a[1], b[1], amount)};
        }

        private static float dp(Context context, float value) {
            return value * context.getResources().getDisplayMetrics().density;
        }

        private static final class Projector {
            final float sinTilt;
            final float cosTilt;
            final float sinYaw;
            final float cosYaw;
            final float centerX;
            final float centerY;
            final float scale;

            Projector(float yaw, float tilt, float centerX, float centerY, float scale) {
                sinTilt = (float) Math.sin(tilt);
                cosTilt = (float) Math.cos(tilt);
                sinYaw = (float) Math.sin(yaw);
                cosYaw = (float) Math.cos(yaw);
                this.centerX = centerX;
                this.centerY = centerY;
                this.scale = scale;
            }

            float[] project(float x, float y, float z) {
                float p = x * cosYaw + z * sinYaw;
                float g = -x * sinYaw + z * cosYaw;
                float screenY = y * cosTilt - g * sinTilt;
                float depth = y * sinTilt + g * cosTilt;
                return new float[]{centerX + p * scale, centerY - screenY * scale, depth};
            }
        }

        private static final class Dot {
            final float x;
            final float y;
            final float z;
            final float radius;
            final float white;
            final float alpha;

            Dot(float x, float y, float z, float radius, float white, float alpha) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.radius = radius;
                this.white = white;
                this.alpha = alpha;
            }
        }

        private static final class Line {
            final float x1;
            final float y1;
            final float x2;
            final float y2;
            final float white;
            final float alpha;
            final float width;

            Line(float x1, float y1, float x2, float y2,
                 float white, float alpha, float width) {
                this.x1 = x1;
                this.y1 = y1;
                this.x2 = x2;
                this.y2 = y2;
                this.white = white;
                this.alpha = alpha;
                this.width = width;
            }
        }

        private static final class Move {
            final int axis;
            final float low;
            final float high;
            final float angle;

            Move(int axis, float low, float high, float angle) {
                this.axis = axis;
                this.low = low;
                this.high = high;
                this.angle = angle;
            }
        }

        private static final class Stage {
            final float[] amount;
            final int active;

            Stage(float[] amount, int active) {
                this.amount = amount;
                this.active = active;
            }
        }

        private static final class Rotated {
            final float x;
            final float y;
            final float z;
            final boolean active;

            Rotated(float x, float y, float z, boolean active) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.active = active;
            }
        }
    }
}
