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

public final class AppPickerActivity extends Activity {
    public static final String EXTRA_COMPONENT = "component";
    public static final String EXTRA_TARGET = "target";
    public static final String EXTRA_MODE = "mode";
    public static final int MODE_APPS = 0;
    public static final int MODE_SHORTCUTS = 1;
    private final ArrayList<Entry> all = new ArrayList<>();
    private final ArrayList<Entry> filtered = new ArrayList<>();
    private AppAdapter adapter;
    private ConfigStore store;
    private String searchText = "";
    private int mode;
    private TextView empty;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        mode = getIntent().getIntExtra(EXTRA_MODE, MODE_APPS);
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), 0);
        root.setBackgroundColor(0xfff4f5fa);
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText(mode == MODE_SHORTCUTS ? "添加快捷方式" : "添加应用");
        title.setTextColor(Ui.TEXT);
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 16));
        titleRow.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView refresh = new TextView(this);
        refresh.setText("刷新");
        refresh.setTextColor(Ui.ACCENT);
        refresh.setTextSize(15);
        refresh.setTypeface(null, android.graphics.Typeface.BOLD);
        refresh.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), 0, Ui.dp(this, 16));
        refresh.setOnClickListener(view -> refreshCatalog());
        titleRow.addView(refresh);
        root.addView(titleRow);

        EditText search = new EditText(this);
        search.setHint(mode == MODE_SHORTCUTS ? "搜索快捷方式" : "搜索应用");
        search.setSingleLine(true);
        search.setTextSize(16);
        search.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        search.setBackground(Ui.rounded(this, Color.WHITE, 16));
        root.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));

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
        loadApps();
        if (mode == MODE_APPS && store.getActivityCatalog().isEmpty()) refreshCatalog();
        if (mode == MODE_SHORTCUTS && store.getShortcutCatalog().isEmpty()) refreshCatalog();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchText = s.toString();
                filter(searchText);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            Entry entry = filtered.get(position);
            Intent result = new Intent().putExtra(EXTRA_TARGET, entry.target.toJson().toString());
            setResult(RESULT_OK, result);
            finish();
        });
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
            return row;
        }
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
