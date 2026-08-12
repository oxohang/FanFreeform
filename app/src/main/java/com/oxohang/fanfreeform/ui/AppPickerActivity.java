package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.database.ContentObserver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.oxohang.fanfreeform.config.AppTarget;
import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;
import com.oxohang.fanfreeform.config.ShortcutIconLoader;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONObject;

public final class AppPickerActivity extends Activity {
    public static final String EXTRA_COMPONENT = "component";
    public static final String EXTRA_TARGET = "target";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_MULTI = "multi";
    public static final String EXTRA_SELECTED = "selected";
    public static final String EXTRA_MAX = "max";
    public static final String EXTRA_TARGETS = "targets";
    public static final int MODE_APPS = 0;
    public static final int MODE_SHORTCUTS = 1;
    public static final int MODE_APPS_AND_SHORTCUTS = 2;
    private static final int APP_CATEGORY_ALL = 0;
    private static final int APP_CATEGORY_USER = 1;
    private static final int APP_CATEGORY_SYSTEM = 2;
    private static final int APP_CATEGORY_DUAL = 3;
    private final ArrayList<Entry> all = new ArrayList<>();
    private final ArrayList<Entry> filtered = new ArrayList<>();
    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Drawable placeholderIcon = new ColorDrawable(0xffeef0f5);
    private Future<?> loadTask;
    private volatile int loadGeneration;
    private volatile boolean destroyed;
    private AppAdapter adapter;
    private ConfigStore store;
    private String searchText = "";
    private volatile int mode;
    private boolean combinedMode;
    private TextView empty;
    private TextView selectionCount;
    private TextView titleText;
    private EditText searchInput;
    private Button appsTab;
    private Button shortcutsTab;
    private HorizontalScrollView appCategoryScroll;
    private TextView categoryHint;
    private TextView allCategory;
    private TextView userCategory;
    private TextView systemCategory;
    private TextView dualCategory;
    private boolean multi;
    private int maximum;
    private int appCategory = APP_CATEGORY_USER;
    private final LinkedHashSet<AppTarget> selectedTargets = new LinkedHashSet<>();
    private ContentObserver catalogObserver;
    private Runnable catalogTimeout;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        catalogObserver = new ContentObserver(mainHandler) {
            @Override public void onChange(boolean selfChange) {
                loadApps();
            }
        };
        getContentResolver().registerContentObserver(
                ConfigContract.CATALOG_URI, false, catalogObserver);
        int requestedMode = getIntent().getIntExtra(EXTRA_MODE, MODE_APPS);
        combinedMode = requestedMode == MODE_APPS_AND_SHORTCUTS;
        mode = combinedMode ? MODE_APPS : requestedMode;
        multi = getIntent().getBooleanExtra(EXTRA_MULTI, false);
        maximum = Math.max(0, getIntent().getIntExtra(EXTRA_MAX, 60));
        try {
            JSONArray selected = new JSONArray(getIntent().getStringExtra(EXTRA_SELECTED));
            for (int i = 0; i < selected.length(); i++) {
                AppTarget target = AppTarget.fromJson(selected.optJSONObject(i));
                if (target != null) selectedTargets.add(target);
            }
        } catch (Exception ignored) { }
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 10), Ui.dp(this, 18), 0);
        root.setBackgroundColor(0xfff4f5fa);
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextColor(Ui.TEXT);
        back.setTextSize(38);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("返回");
        back.setOnClickListener(view -> finish());
        titleRow.addView(back, new LinearLayout.LayoutParams(
                Ui.dp(this, 42), Ui.dp(this, 52)));
        titleText = new TextView(this);
        titleText.setTextColor(Ui.TEXT);
        titleText.setTextSize(25);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setPadding(Ui.dp(this, 2), Ui.dp(this, 12), 0, Ui.dp(this, 16));
        titleRow.addView(titleText, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView refresh = new TextView(this);
        refresh.setText("刷新");
        refresh.setTextColor(Ui.ACCENT);
        refresh.setTextSize(15);
        refresh.setTypeface(null, android.graphics.Typeface.BOLD);
        refresh.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), 0, Ui.dp(this, 16));
        refresh.setOnClickListener(view -> refreshCatalog());
        titleRow.addView(refresh);
        if (multi) {
            TextView save = new TextView(this);
            save.setText("保存");
            save.setTextColor(Ui.ACCENT);
            save.setTextSize(15);
            save.setTypeface(null, android.graphics.Typeface.BOLD);
            save.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), 0, Ui.dp(this, 16));
            save.setOnClickListener(view -> finishMulti());
            titleRow.addView(save);
        }
        root.addView(titleRow);

        if (combinedMode) {
            LinearLayout tabs = new LinearLayout(this);
            tabs.setOrientation(LinearLayout.HORIZONTAL);
            tabs.setPadding(0, 0, 0, Ui.dp(this, 10));
            appsTab = compactButton("应用");
            appsTab.setOnClickListener(view -> switchMode(MODE_APPS));
            tabs.addView(appsTab, new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1));
            shortcutsTab = compactButton("快捷方式");
            shortcutsTab.setOnClickListener(view -> switchMode(MODE_SHORTCUTS));
            LinearLayout.LayoutParams shortcutParams = new LinearLayout.LayoutParams(
                    0, Ui.dp(this, 42), 1);
            shortcutParams.leftMargin = Ui.dp(this, 8);
            tabs.addView(shortcutsTab, shortcutParams);
            root.addView(tabs);
        }

        buildAppCategories(root);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(16);
        searchInput.setHintTextColor(Ui.MUTED);
        searchInput.setHint("搜索应用");
        searchInput.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        searchInput.setBackground(Ui.rounded(this, Color.WHITE, 16));
        root.addView(searchInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));

        if (multi) {
            LinearLayout actions = new LinearLayout(this);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            actions.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
            actions.setBackground(Ui.rounded(this, Color.WHITE, 16));
            selectionCount = new TextView(this);
            selectionCount.setTextColor(Ui.MUTED);
            selectionCount.setTextSize(13);
            actions.addView(selectionCount, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            Button allButton = compactButton("全选当前分类");
            allButton.setOnClickListener(view -> selectFiltered());
            actions.addView(allButton);
            Button inverse = compactButton("反选");
            inverse.setOnClickListener(view -> invertFiltered());
            actions.addView(inverse);
            Button clear = compactButton("清空");
            clear.setOnClickListener(view -> {
                selectedTargets.clear();
                selectionChanged();
            });
            actions.addView(clear);
            root.addView(actions);
            selectionChanged();
        }

        empty = new TextView(this);
        empty.setTextColor(Ui.MUTED);
        empty.setTextSize(14);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(Ui.dp(this, 24), Ui.dp(this, 30), Ui.dp(this, 24), Ui.dp(this, 30));
        root.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(Ui.dp(this, 8));
        list.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 16));
        list.setClipToPadding(false);
        adapter = new AppAdapter();
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        updateModeUi();
        loadApps();
        if (mode == MODE_APPS && store.getActivityCatalog().isEmpty()) refreshCatalog();
        if (mode == MODE_SHORTCUTS && store.getShortcutCatalog().isEmpty()) refreshCatalog();
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchText = s.toString();
                filter(searchText);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void buildAppCategories(LinearLayout root) {
        appCategoryScroll = new HorizontalScrollView(this);
        appCategoryScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout categories = new LinearLayout(this);
        categories.setOrientation(LinearLayout.HORIZONTAL);
        categories.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 8));
        allCategory = categoryButton("全部", APP_CATEGORY_ALL);
        userCategory = categoryButton("用户应用", APP_CATEGORY_USER);
        systemCategory = categoryButton("系统应用", APP_CATEGORY_SYSTEM);
        dualCategory = categoryButton("双开应用", APP_CATEGORY_DUAL);
        categories.addView(allCategory, categoryParams());
        categories.addView(userCategory, categoryParams());
        categories.addView(systemCategory, categoryParams());
        categories.addView(dualCategory, categoryParams());
        appCategoryScroll.addView(categories, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(appCategoryScroll);

        categoryHint = new TextView(this);
        categoryHint.setTextColor(Ui.MUTED);
        categoryHint.setTextSize(12);
        categoryHint.setPadding(Ui.dp(this, 2), 0, 0, Ui.dp(this, 8));
        root.addView(categoryHint);
    }

    private TextView categoryButton(String label, int category) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(13);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setPadding(Ui.dp(this, 15), 0, Ui.dp(this, 15), 0);
        button.setMinWidth(Ui.dp(this, 82));
        button.setOnClickListener(view -> {
            appCategory = category;
            updateCategoryUi();
            filter(searchText);
        });
        return button;
    }

    private LinearLayout.LayoutParams categoryParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 38));
        params.leftMargin = Ui.dp(this, 8);
        return params;
    }

    private void updateCategoryUi() {
        if (appCategoryScroll == null) return;
        boolean visible = mode == MODE_APPS;
        appCategoryScroll.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (categoryHint != null) categoryHint.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) return;
        allCategory.setText("全部 " + all.size());
        userCategory.setText("用户应用 " + countCategory(APP_CATEGORY_USER));
        systemCategory.setText("系统应用 " + countCategory(APP_CATEGORY_SYSTEM));
        dualCategory.setText("双开应用 " + countCategory(APP_CATEGORY_DUAL));
        styleCategory(allCategory, appCategory == APP_CATEGORY_ALL);
        styleCategory(userCategory, appCategory == APP_CATEGORY_USER);
        styleCategory(systemCategory, appCategory == APP_CATEGORY_SYSTEM);
        styleCategory(dualCategory, appCategory == APP_CATEGORY_DUAL);
        categoryHint.setText(categoryDescription());
    }

    private int countCategory(int category) {
        int count = 0;
        for (Entry entry : all) {
            if (matchesCategory(entry, category)) count++;
        }
        return count;
    }

    private void styleCategory(TextView button, boolean selected) {
        button.setTextColor(selected ? Color.WHITE : Ui.ACCENT);
        button.setBackground(Ui.rounded(this, selected ? Ui.ACCENT : 0xffeef0ff, 13));
    }

    private String categoryDescription() {
        switch (appCategory) {
            case APP_CATEGORY_SYSTEM:
                return "只显示带启动入口的系统应用";
            case APP_CATEGORY_DUAL:
                return "只显示从小米桌面读取到的双开应用";
            case APP_CATEGORY_USER:
                return "只显示普通用户应用";
            default:
                return "用户应用、系统应用和双开应用一览";
        }
    }

    private void toggle(AppTarget target) {
        if (selectedTargets.remove(target)) {
            selectionChanged();
            return;
        }
        if (selectedTargets.size() >= maximum) {
            android.widget.Toast.makeText(this, "已达到上限 " + maximum,
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        selectedTargets.add(target);
        selectionChanged();
    }

    private void selectFiltered() {
        for (Entry entry : filtered) {
            if (selectedTargets.size() >= maximum) break;
            selectedTargets.add(entry.target);
        }
        selectionChanged();
    }

    private void invertFiltered() {
        for (Entry entry : filtered) {
            if (selectedTargets.contains(entry.target)) selectedTargets.remove(entry.target);
            else if (selectedTargets.size() < maximum) selectedTargets.add(entry.target);
        }
        selectionChanged();
    }

    private void selectionChanged() {
        if (selectionCount != null) selectionCount.setText("已选 " + selectedTargets.size()
                + " / " + maximum);
        if (adapter != null) filter(searchText);
    }

    private void finishMulti() {
        JSONArray array = new JSONArray();
        for (AppTarget target : selectedTargets) array.put(target.toJson());
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_TARGETS, array.toString()));
        finish();
    }

    private void toggle(AppTarget target) {
        if (selectedTargets.remove(target)) {
            selectionChanged();
            return;
        }
        if (selectedTargets.size() >= maximum) {
            android.widget.Toast.makeText(this, "已达到上限 " + maximum,
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        selectedTargets.add(target);
        selectionChanged();
    }

    private void selectFiltered() {
        for (Entry entry : filtered) {
            if (selectedTargets.size() >= maximum) break;
            selectedTargets.add(entry.target);
        }
        selectionChanged();
    }

    private void invertFiltered() {
        for (Entry entry : filtered) {
            if (selectedTargets.contains(entry.target)) selectedTargets.remove(entry.target);
            else if (selectedTargets.size() < maximum) selectedTargets.add(entry.target);
        }
        selectionChanged();
    }

    private void selectionChanged() {
        if (selectionCount != null) selectionCount.setText("已选 " + selectedTargets.size()
                + " / " + maximum);
        if (adapter != null) filter(searchText);
    }

    private void finishMulti() {
        JSONArray array = new JSONArray();
        for (AppTarget target : selectedTargets) array.put(target.toJson());
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_TARGETS, array.toString()));
        finish();
    }

    @SuppressWarnings("deprecation")
    private void loadApps() {
        if (destroyed) return;
        final int gen = ++loadGeneration;
        all.clear();
        filtered.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        if (empty != null) {
            empty.setVisibility(View.VISIBLE);
            empty.setText("正在加载…");
        }
        if (loadTask != null) loadTask.cancel(true);
        loadTask = loadExecutor.submit(() -> {
            List<Entry> entries = collectEntries();
            mainHandler.post(() -> {
                if (destroyed || gen != loadGeneration) return;
                all.addAll(entries);
                filter(searchText);
            });
            List<Drawable> icons = new ArrayList<>(entries.size());
            for (Entry entry : entries) {
                if (destroyed || gen != loadGeneration || Thread.currentThread().isInterrupted()) return;
                icons.add(loadIconFor(entry));
            }
            if (destroyed || gen != loadGeneration) return;
            mainHandler.post(() -> {
                if (destroyed || gen != loadGeneration) return;
                for (int i = 0; i < entries.size(); i++) {
                    entries.get(i).icon = icons.get(i);
                }
                if (adapter != null) adapter.notifyDataSetChanged();
            });
        });
    }

    @SuppressWarnings("deprecation")
    private List<Entry> collectEntries() {
        List<Entry> entries = new ArrayList<>();
        PackageManager pm = getPackageManager();
        if (mode == MODE_APPS) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> results = pm.queryIntentActivities(query, 0);
            Set<String> seen = new HashSet<>();
            for (ResolveInfo result : results) {
                if (result.activityInfo == null || getPackageName().equals(result.activityInfo.packageName)) continue;
                ComponentName component = new ComponentName(result.activityInfo.packageName, result.activityInfo.name);
                if (!seen.add(component.flattenToString())) continue;
                CharSequence labelValue = result.loadLabel(pm);
                String label = labelValue == null ? result.activityInfo.packageName : labelValue.toString();
                boolean systemApp = isSystemApplication(result.activityInfo.applicationInfo);
                String secondary = result.activityInfo.packageName
                        + (systemApp ? " · 系统应用" : "");
                entries.add(new Entry(new AppTarget(component.flattenToString()), label,
                        secondary, systemApp, pm2 -> result.loadIcon(pm2)));
            }
            for (ConfigStore.ActivityCatalogEntry activity : store.getActivityCatalog()) {
                ComponentName component = ComponentName.unflattenFromString(activity.component);
                if (component == null) continue;
                String key = activity.component + "#" + activity.userId;
                if (!seen.add(key)) continue;
                try {
                    String packageName = component.getPackageName();
                    entries.add(new Entry(new AppTarget(activity.component, activity.userId),
                            activity.label, packageName + " · 双开", false,
                            pm2 -> pm2.getApplicationIcon(packageName)));
                } catch (Exception ignored) { }
            }
        }
        if (mode == MODE_SHORTCUTS) {
            for (ConfigStore.ShortcutCatalogEntry shortcut : store.getShortcutCatalog()) {
                try {
                    AppTarget target = shortcut.launcherShortcut
                            ? AppTarget.launcherShortcut(shortcut.packageName,
                            shortcut.shortcutId, shortcut.label, shortcut.intentUri,
                            shortcut.userId)
                            : AppTarget.shortcut(shortcut.packageName, shortcut.shortcutId,
                            shortcut.label);
                    entries.add(new Entry(target, shortcut.label,
                            shortcut.packageName + (shortcut.launcherShortcut
                                    ? " · 桌面快捷方式" : " · 快捷方式"),
                            false,
                            pm2 -> ShortcutIconLoader.load(AppPickerActivity.this,
                                    shortcut.packageName, shortcut.shortcutId,
                                    shortcut.userId)));
                } catch (Exception ignored) { }
            }
        }
        return entries;
    }

    private static boolean isSystemApplication(ApplicationInfo info) {
        if (info == null) return false;
        int flags = info.flags;
        return (flags & ApplicationInfo.FLAG_SYSTEM) != 0
                || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
    }

    private Drawable loadIconFor(Entry entry) {
        try {
            return entry.iconLoader.load(getPackageManager());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void filter(String text) {
        String needle = text.trim().toLowerCase(Locale.ROOT);
        filtered.clear();
        for (Entry entry : all) {
            boolean categoryMatches = mode != MODE_APPS || matchesCategory(entry, appCategory);
            boolean textMatches = needle.isEmpty()
                    || entry.label.toLowerCase(Locale.ROOT).contains(needle)
                    || entry.secondary.toLowerCase(Locale.ROOT).contains(needle);
            if (categoryMatches && textMatches) filtered.add(entry);
        }
        ArrayList<AppTarget> selectedOrder = new ArrayList<>(selectedTargets);
        Collator collator = Collator.getInstance(Locale.CHINA);
        filtered.sort((left, right) -> {
            int leftSelected = selectedOrder.indexOf(left.target);
            int rightSelected = selectedOrder.indexOf(right.target);
            if (leftSelected >= 0 && rightSelected < 0) return -1;
            if (leftSelected < 0 && rightSelected >= 0) return 1;
            if (leftSelected >= 0) return Integer.compare(leftSelected, rightSelected);
            return collator.compare(left.label, right.label);
        });
        if (empty != null) {
            boolean show = filtered.isEmpty();
            empty.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) empty.setText(emptyText(needle));
        }
        updateCategoryUi();
        adapter.notifyDataSetChanged();
    }

    private boolean matchesCategory(Entry entry, int category) {
        if (category == APP_CATEGORY_ALL) return true;
        if (category == APP_CATEGORY_DUAL) return entry.target.userId != 0;
        if (category == APP_CATEGORY_SYSTEM) {
            return entry.target.userId == 0 && entry.systemApp;
        }
        return entry.target.userId == 0 && !entry.systemApp;
    }

    private String emptyText(String needle) {
        if (mode == MODE_SHORTCUTS) {
            return "暂未读取到可用快捷方式。\n请先在小米桌面刷新快捷方式目录。";
        }
        if (!needle.isEmpty()) return "当前分类没有匹配的应用。";
        return "当前分类暂无应用。\n可以切换上方分类或点击右上角刷新。";
    }

    private void refreshCatalog() {
        if (mode == MODE_APPS) {
            store.requestActivityCatalog();
            loadApps();
            scheduleCatalogTimeout();
            return;
        }
        store.requestShortcutCatalog();
        loadApps();
        scheduleCatalogTimeout();
    }

    private void scheduleCatalogTimeout() {
        if (catalogTimeout != null) mainHandler.removeCallbacks(catalogTimeout);
        catalogTimeout = () -> {
            if (destroyed) return;
            boolean empty = mode == MODE_APPS
                    ? store.getActivityCatalog().isEmpty()
                    : store.getShortcutCatalog().isEmpty();
            if (empty) {
                if (mode == MODE_APPS) store.requestActivityCatalog();
                else store.requestShortcutCatalog();
                loadApps();
            }
        };
        mainHandler.postDelayed(catalogTimeout, 2000L);
    }

    private void switchMode(int nextMode) {
        if (!combinedMode || mode == nextMode) return;
        mode = nextMode;
        searchText = "";
        if (searchInput != null) searchInput.setText("");
        updateModeUi();
        loadApps();
        if (mode == MODE_APPS && store.getActivityCatalog().isEmpty()) refreshCatalog();
        if (mode == MODE_SHORTCUTS && store.getShortcutCatalog().isEmpty()) refreshCatalog();
    }

    private void updateModeUi() {
        if (titleText != null) {
            titleText.setText(mode == MODE_SHORTCUTS ? "选择快捷方式" : "选择应用");
        }
        if (searchInput != null) searchInput.setHint(
                mode == MODE_SHORTCUTS ? "搜索快捷方式" : "搜索应用");
        updateCategoryUi();
        if (appsTab != null) {
            appsTab.setTextColor(mode == MODE_APPS ? Color.WHITE : Ui.ACCENT);
            appsTab.setBackground(Ui.rounded(this,
                    mode == MODE_APPS ? Ui.ACCENT : 0xffeef0ff, 12));
        }
        if (shortcutsTab != null) {
            shortcutsTab.setTextColor(mode == MODE_SHORTCUTS ? Color.WHITE : Ui.ACCENT);
            shortcutsTab.setBackground(Ui.rounded(this,
                    mode == MODE_SHORTCUTS ? Ui.ACCENT : 0xffeef0ff, 12));
        }
    }

    private final class AppAdapter extends BaseAdapter {
        @Override public int getCount() { return filtered.size(); }
        @Override public Entry getItem(int position) { return filtered.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row = new LinearLayout(AppPickerActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(AppPickerActivity.this, 14), Ui.dp(AppPickerActivity.this, 10), Ui.dp(AppPickerActivity.this, 14), Ui.dp(AppPickerActivity.this, 10));
            Entry entry = getItem(position);
            row.setBackground(Ui.rounded(AppPickerActivity.this,
                    selectedTargets.contains(entry.target) ? 0xfff0f1ff : Color.WHITE,
                    16));
            row.setClickable(true);
            row.setFocusable(false);
            row.setOnClickListener(view -> selectEntry(entry));
            IconBadgeView icon = new IconBadgeView(AppPickerActivity.this);
            icon.setIcon(entry.icon != null ? entry.icon : placeholderIcon,
                    entry.target.isShortcut(), entry.target.userId != 0);
            row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(AppPickerActivity.this, 44), Ui.dp(AppPickerActivity.this, 44)));
            LinearLayout text = new LinearLayout(AppPickerActivity.this);
            text.setOrientation(LinearLayout.VERTICAL);
            text.setPadding(Ui.dp(AppPickerActivity.this, 14), 0, 0, 0);
            TextView label = new TextView(AppPickerActivity.this);
            label.setText(entry.label);
            label.setTextColor(Ui.TEXT);
            label.setTextSize(16);
            TextView secondary = new TextView(AppPickerActivity.this);
            secondary.setText(entry.secondary);
            secondary.setTextColor(Ui.MUTED);
            secondary.setTextSize(12);
            text.addView(label);
            text.addView(secondary);
            row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            if (multi) {
                CheckBox check = new CheckBox(AppPickerActivity.this);
                check.setChecked(selectedTargets.contains(entry.target));
                check.setFocusable(false);
                check.setFocusableInTouchMode(false);
                check.setOnClickListener(view -> toggle(entry.target));
                row.addView(check);
            }
            return row;
        }
    }

    private void selectEntry(Entry entry) {
        if (multi) {
            toggle(entry.target);
            return;
        }
        Intent result = new Intent().putExtra(EXTRA_TARGET,
                entry.target.toJson().toString());
        setResult(RESULT_OK, result);
        finish();
    }

    private Button compactButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(12);
        button.setTextColor(Ui.ACCENT);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        button.setBackground(Ui.rounded(this, 0xffeef0ff, 10));
        return button;
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (catalogObserver != null) {
            try { getContentResolver().unregisterContentObserver(catalogObserver); }
            catch (Throwable ignored) { }
            catalogObserver = null;
        }
        loadGeneration++;
        mainHandler.removeCallbacksAndMessages(null);
        if (loadTask != null) loadTask.cancel(true);
        loadExecutor.shutdownNow();
        super.onDestroy();
    }

    private static final class Entry {
        final AppTarget target;
        final String label;
        final String secondary;
        final boolean systemApp;
        final IconLoader iconLoader;
        Drawable icon;
        Entry(AppTarget target, String label, String secondary, boolean systemApp,
              IconLoader iconLoader) {
            this.target = target;
            this.label = label;
            this.secondary = secondary;
            this.systemApp = systemApp;
            this.iconLoader = iconLoader;
        }
    }

    private interface IconLoader {
        Drawable load(PackageManager pm) throws Exception;
    }
}
