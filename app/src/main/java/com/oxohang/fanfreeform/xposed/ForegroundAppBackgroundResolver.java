package com.oxohang.fanfreeform.xposed;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ForegroundAppBackgroundResolver {
    interface Callback { void onResolved(int color); }

    private static final int MAX_CACHE_SIZE = 32;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "HyperGesture-AppBackground");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, Integer> CACHE =
            new LinkedHashMap<String, Integer>(MAX_CACHE_SIZE, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            };

    private ForegroundAppBackgroundResolver() { }

    static int fallback(Context context) {
        boolean night = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return night ? 0xff15161a : 0xfff4f5fa;
    }

    static void request(Context context, Callback callback) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            int result = resolve(appContext);
            if (callback != null) callback.onResolved(result);
        });
    }

    @SuppressWarnings("deprecation")
    private static int resolve(Context context) {
        int fallback = fallback(context);
        try {
            ActivityManager manager = context.getSystemService(ActivityManager.class);
            List<ActivityManager.RunningTaskInfo> tasks = manager == null
                    ? null : manager.getRunningTasks(1);
            if (tasks == null || tasks.isEmpty()) return fallback;
            ComponentName component = tasks.get(0).topActivity;
            if (component == null) return fallback;
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo application = packageManager.getApplicationInfo(
                    component.getPackageName(), 0);
            long lastUpdateTime = packageManager.getPackageInfo(
                    component.getPackageName(), 0).lastUpdateTime;
            int night = context.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK;
            String key = component.flattenToShortString() + ':' + lastUpdateTime
                    + ':' + night;
            synchronized (CACHE) {
                Integer cached = CACHE.get(key);
                if (cached != null) return cached;
            }
            ActivityInfo activity = packageManager.getActivityInfo(component, 0);
            int theme = activity.theme != 0 ? activity.theme : application.theme;
            if (theme == 0) return fallback;
            Context packageContext = context.createPackageContext(
                    component.getPackageName(), Context.CONTEXT_IGNORE_SECURITY);
            ContextThemeWrapper themed = new ContextThemeWrapper(packageContext, theme);
            int[] attrs = {android.R.attr.windowBackground};
            android.content.res.TypedArray array = themed.obtainStyledAttributes(attrs);
            Drawable drawable;
            try {
                drawable = array.getDrawable(0);
            } finally {
                array.recycle();
            }
            int color = colorFromDrawable(drawable, fallback);
            synchronized (CACHE) { CACHE.put(key, color); }
            return color;
        } catch (Throwable error) {
            Log.e("Cannot resolve foreground app background", error);
            return fallback;
        }
    }

    private static int colorFromDrawable(Drawable drawable, int fallback) {
        if (drawable == null) return fallback;
        if (drawable instanceof ColorDrawable) {
            return opaque(((ColorDrawable) drawable).getColor(), fallback);
        }
        try {
            Bitmap bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
            drawable.draw(canvas);
            long red = 0L;
            long green = 0L;
            long blue = 0L;
            long weight = 0L;
            for (int y = 0; y < bitmap.getHeight(); y++) {
                for (int x = 0; x < bitmap.getWidth(); x++) {
                    int pixel = bitmap.getPixel(x, y);
                    int alpha = Color.alpha(pixel);
                    red += (long) Color.red(pixel) * alpha;
                    green += (long) Color.green(pixel) * alpha;
                    blue += (long) Color.blue(pixel) * alpha;
                    weight += alpha;
                }
            }
            bitmap.recycle();
            if (weight <= 0L) return fallback;
            return Color.rgb((int) (red / weight), (int) (green / weight),
                    (int) (blue / weight));
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int opaque(int color, int fallback) {
        int alpha = Color.alpha(color);
        if (alpha >= 255) return color | 0xff000000;
        float a = alpha / 255f;
        return Color.rgb(
                Math.round(Color.red(color) * a + Color.red(fallback) * (1f - a)),
                Math.round(Color.green(color) * a + Color.green(fallback) * (1f - a)),
                Math.round(Color.blue(color) * a + Color.blue(fallback) * (1f - a)));
    }
}
