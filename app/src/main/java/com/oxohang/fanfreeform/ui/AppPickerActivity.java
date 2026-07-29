package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.oxohang.fanfreeform.config.AppTarget;
import com.oxohang.fanfreeform.config.ConfigStore;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashSet;

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
    private final ArrayList<Entry> all = new ArrayList<>();
    private final ArrayList<Entry> filtered = new ArrayList<>();
    private AppAdapter adapter;
    private ConfigStore store;
    private String searchText = "";
    private int mode;
    private boolean combinedMode;
    private TextView empty;
    private TextView selectionCount;
    private TextView titleText;
    private EditText searchInput;
    private Button appsTab;
    private Button shortcutsTab;
    private boolean multi;
    private int maximum;
    private final LinkedHashSet<AppTarget> selectedTargets = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
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
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), 0);
        root.setBackgroundColor(0xfff4f5fa);
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleText = new TextView(this);
        titleText.setTextColor(Ui.TEXT);
        titleText.setTextSize(26);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 16));
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

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(16);
        searchInput.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        searchInput.setBackground(Ui.rounded(this, Color.WHITE, 16));
        root.addView(searchInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));

        if (multi) {
            LinearLayout actions = new LinearLayout(this);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            actions.setPadding(0, Ui.dp(this, 8), 0, 0);
            selectionCount = new TextView(this);
            selectionCount.setTextColor(Ui.MUTED);
            selectionCount.setTextSize(13);
            actions.addView(selectionCount, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            Button allButton = compactButton("全选结果");
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
        all.clear();
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
                all.add(new Entry(new AppTarget(component.flattenToString()), label,
                        result.activityInfo.packageName, result.loadIcon(pm)));
            }
            for (ConfigStore.ActivityCatalogEntry activity : store.getActivityCatalog()) {
                ComponentName component = ComponentName.unflattenFromString(activity.component);
                if (component == null) continue;
                String key = activity.component + "#" + activity.userId;
                if (!seen.add(key)) continue;
                try {
                    all.add(new Entry(new AppTarget(activity.component, activity.userId),
                            activity.label, component.getPackageName() + " · 双开",
                            pm.getApplicationIcon(component.getPackageName())));
                } catch (Exception ignored) { }
            }
        }
        if (mode == MODE_SHORTCUTS) {
            for (ConfigStore.ShortcutCatalogEntry shortcut : store.getShortcutCatalog()) {
                try {
                    all.add(new Entry(AppTarget.shortcut(shortcut.packageName, shortcut.shortcutId,
                            shortcut.label), shortcut.label, shortcut.packageName + " · 快捷方式",
                            pm.getApplicationIcon(shortcut.packageName)));
                } catch (Exception ignored) { }
            }
        }
        Collator collator = Collator.getInstance(Locale.CHINA);
        all.sort(Comparator.comparing(entry -> entry.label, collator));
        filter(searchText);
    }

    private void filter(String text) {
        String needle = text.trim().toLowerCase(Locale.ROOT);
        filtered.clear();
        for (Entry entry : all) {
            if (needle.isEmpty() || entry.label.toLowerCase(Locale.ROOT).contains(needle)
                    || entry.secondary.toLowerCase(Locale.ROOT).contains(needle)) filtered.add(entry);
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
            if (show) {
                empty.setText(mode == MODE_SHORTCUTS
                        ? "暂未读取到可用快捷方式。正在从小米桌面刷新，稍后可点右上角“刷新”。"
                        : "没有找到匹配的应用。");
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void refreshCatalog() {
        if (mode == MODE_APPS) {
            store.requestActivityCatalog();
            new Handler().postDelayed(this::loadApps, 900L);
            return;
        }
        store.requestShortcutCatalog();
        new Handler().postDelayed(this::loadApps, 900L);
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
            titleText.setText(combinedMode ? "选择小窗应用"
                    : mode == MODE_SHORTCUTS ? "添加快捷方式" : "添加应用");
        }
        if (searchInput != null) searchInput.setHint(
                mode == MODE_SHORTCUTS ? "搜索快捷方式" : "搜索应用");
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
            row.setBackground(Ui.rounded(AppPickerActivity.this, Color.WHITE, 16));
            Entry entry = getItem(position);
            row.setClickable(true);
            row.setFocusable(false);
            row.setOnClickListener(view -> selectEntry(entry));
            ImageView icon = new ImageView(AppPickerActivity.this);
            icon.setImageDrawable(entry.icon);
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

    private static final class Entry {
        final AppTarget target;
        final String label;
        final String secondary;
        final Drawable icon;
        Entry(AppTarget target, String label, String secondary, Drawable icon) {
            this.target = target;
            this.label = label;
            this.secondary = secondary;
            this.icon = icon;
        }
    }
}
