package com.oxohang.fanfreeform.xposed;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class BlurredWallpaperCache {
    interface Callback { void onReady(Bitmap bitmap); }

    private static final int DOWNSAMPLE = 6;
    private static final int MAX_ENTRIES = 3;
    private static final Object LOCK = new Object();
    private static final ExecutorService BLUR_EXECUTOR = Executors.newSingleThreadExecutor(
            runnable -> new Thread(runnable, "fanfreeform-wallpaper-blur"));
    private static final LinkedHashMap<Key, Bitmap> CACHE = new LinkedHashMap<>(
            MAX_ENTRIES, 0.75f, true);
    private static final Map<Key, List<WeakReference<Callback>>> WAITERS =
            new LinkedHashMap<>();
    private static int generation;

    private BlurredWallpaperCache() { }

    @SuppressLint("MissingPermission")
    static Bitmap getOrRequest(Context context, int blurDp, Callback callback) {
        Context appContext = context.getApplicationContext();
        if (appContext == null) appContext = context;
        int width = Math.max(48,
                appContext.getResources().getDisplayMetrics().widthPixels / DOWNSAMPLE);
        int height = Math.max(96,
                appContext.getResources().getDisplayMetrics().heightPixels / DOWNSAMPLE);
        int radius = Math.max(1, blurDp / DOWNSAMPLE);
        int wallpaperId = -1;
        try {
            wallpaperId = WallpaperManager.getInstance(appContext).getWallpaperId(
                    WallpaperManager.FLAG_SYSTEM);
        } catch (Throwable ignored) { }
        Key key;
        synchronized (LOCK) {
            key = new Key(generation, wallpaperId, width, height, radius);
            Bitmap cached = CACHE.get(key);
            if (cached != null && !cached.isRecycled()) return cached;
            List<WeakReference<Callback>> callbacks = WAITERS.get(key);
            if (callbacks != null) {
                callbacks.add(new WeakReference<>(callback));
                return null;
            }
            callbacks = new ArrayList<>();
            callbacks.add(new WeakReference<>(callback));
            WAITERS.put(key, callbacks);
        }
        Context sourceContext = appContext;
        BLUR_EXECUTOR.execute(() -> build(sourceContext, key));
        return null;
    }

    static void clear() {
        synchronized (LOCK) {
            generation++;
            CACHE.clear();
            WAITERS.clear();
        }
    }

    @SuppressLint("MissingPermission")
    private static void build(Context context, Key key) {
        Bitmap result = null;
        try {
            Drawable source = WallpaperManager.getInstance(context).getDrawable();
            result = Bitmap.createBitmap(key.width, key.height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            source.setBounds(0, 0, key.width, key.height);
            source.draw(canvas);
            blur(result, key.radius, 2);
        } catch (Throwable error) {
            Log.e("Cannot prepare cached blurred wallpaper", error);
        }
        List<WeakReference<Callback>> callbacks;
        synchronized (LOCK) {
            callbacks = WAITERS.remove(key);
            if (result != null && key.generation == generation) {
                CACHE.put(key, result);
                while (CACHE.size() > MAX_ENTRIES) {
                    Key oldest = CACHE.keySet().iterator().next();
                    CACHE.remove(oldest);
                }
            }
        }
        if (callbacks == null || result == null) return;
        for (WeakReference<Callback> reference : callbacks) {
            Callback callback = reference.get();
            if (callback != null) callback.onReady(result);
        }
    }

    private static void blur(Bitmap bitmap, int radius, int iterations) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] source = new int[width * height];
        int[] target = new int[source.length];
        bitmap.getPixels(source, 0, width, 0, 0, width, height);
        for (int iteration = 0; iteration < iterations; iteration++) {
            horizontal(source, target, width, height, radius);
            vertical(target, source, width, height, radius);
        }
        bitmap.setPixels(source, 0, width, 0, 0, width, height);
    }

    private static void horizontal(int[] source, int[] target, int width, int height,
                                   int radius) {
        int samples = radius * 2 + 1;
        for (int y = 0; y < height; y++) {
            int row = y * width;
            long a = 0, r = 0, g = 0, b = 0;
            for (int offset = -radius; offset <= radius; offset++) {
                int color = source[row + clamp(offset, 0, width - 1)];
                a += color >>> 24; r += color >> 16 & 255;
                g += color >> 8 & 255; b += color & 255;
            }
            for (int x = 0; x < width; x++) {
                target[row + x] = color(a, r, g, b, samples);
                int remove = source[row + clamp(x - radius, 0, width - 1)];
                int add = source[row + clamp(x + radius + 1, 0, width - 1)];
                a += (add >>> 24) - (remove >>> 24);
                r += (add >> 16 & 255) - (remove >> 16 & 255);
                g += (add >> 8 & 255) - (remove >> 8 & 255);
                b += (add & 255) - (remove & 255);
            }
        }
    }

    private static void vertical(int[] source, int[] target, int width, int height,
                                 int radius) {
        int samples = radius * 2 + 1;
        for (int x = 0; x < width; x++) {
            long a = 0, r = 0, g = 0, b = 0;
            for (int offset = -radius; offset <= radius; offset++) {
                int color = source[clamp(offset, 0, height - 1) * width + x];
                a += color >>> 24; r += color >> 16 & 255;
                g += color >> 8 & 255; b += color & 255;
            }
            for (int y = 0; y < height; y++) {
                target[y * width + x] = color(a, r, g, b, samples);
                int remove = source[clamp(y - radius, 0, height - 1) * width + x];
                int add = source[clamp(y + radius + 1, 0, height - 1) * width + x];
                a += (add >>> 24) - (remove >>> 24);
                r += (add >> 16 & 255) - (remove >> 16 & 255);
                g += (add >> 8 & 255) - (remove >> 8 & 255);
                b += (add & 255) - (remove & 255);
            }
        }
    }

    private static int color(long a, long r, long g, long b, int samples) {
        return ((int) (a / samples) << 24) | ((int) (r / samples) << 16)
                | ((int) (g / samples) << 8) | (int) (b / samples);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class Key {
        final int wallpaperId;
        final int generation;
        final int width;
        final int height;
        final int radius;

        Key(int generation, int wallpaperId, int width, int height, int radius) {
            this.generation = generation;
            this.wallpaperId = wallpaperId;
            this.width = width;
            this.height = height;
            this.radius = radius;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key)) return false;
            Key key = (Key) other;
            return generation == key.generation && wallpaperId == key.wallpaperId
                    && width == key.width
                    && height == key.height && radius == key.radius;
        }

        @Override public int hashCode() {
            int result = generation;
            result = 31 * result + wallpaperId;
            result = 31 * result + width;
            result = 31 * result + height;
            return 31 * result + radius;
        }
    }
}
