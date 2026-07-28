package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AppPickerActivity extends Activity {
    public static final String EXTRA_COMPONENT = "component";
    public static final String EXTRA_IS_SHORTCUT = "is_shortcut";
    private final ArrayList<Entry> all = new ArrayList<>();
    private final ArrayList<Entry> filtered = new ArrayList<>();
    private AppAdapter adapter;
    private boolean showShortcuts;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), 0);
        root.setBackgroundColor(0xfff4f5fa);

        TextView title = new TextView(this);
        title.setText("选择应用");
        title.setTextColor(Ui.TEXT);
        title.setTextSize(26);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 8));
        root.addView(title);

        // 模式切换：应用 / 快捷方式
        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setPadding(0, 0, 0, Ui.dp(this, 12));

        Button appMode = new Button(this);
        appMode.setText("应用");
        appMode.setTextSize(14);
        appMode.setTypeface(null, android.graphics.Typeface.BOLD);
        appMode.setPadding(Ui.dp(this, 16), Ui.dp(this, 6), Ui.dp(this, 16), Ui.dp(this, 6));
        appMode.setBackground(Ui.rounded(this, 0xff6572f6, 12));
        appMode.setTextColor(Color.WHITE);

        Button shortcutMode = new Button(this);
        shortcutMode.setText("快捷方式");
        shortcutMode.setTextSize(14);
        shortcutMode.setPadding(Ui.dp(this, 16), Ui.dp(this, 6), Ui.dp(this, 16), Ui.dp(this, 6));
        shortcutMode.setBackground(Ui.rounded(this, 0xffe8e8f0, 12));
        shortcutMode.setTextColor(Ui.MUTED);

        EditText search = new EditText(this);
        search.setHint("搜索");
        search.setSingleLine(true);
        search.setTextSize(16);
        search.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        search.setBackground(Ui.rounded(this, Color.WHITE, 16));

        Runnable updateMode = () -> {
            boolean sc = showShortcuts;
            appMode.setBackground(Ui.rounded(this, sc ? 0xffe8e8f0 : 0xff6572f6, 12));
            appMode.setTextColor(sc ? Ui.MUTED : Color.WHITE);
            shortcutMode.setBackground(Ui.rounded(this, sc ? 0xff6572f6 : 0xffe8e8f0, 12));
            shortcutMode.setTextColor(sc ? Color.WHITE : Ui.MUTED);
            loadEntries();
            String hint = search.getText().toString();
            filter(hint);
        };

        appMode.setOnClickListener(v -> { showShortcuts = false; updateMode.run(); });
        shortcutMode.setOnClickListener(v -> { showShortcuts = true; updateMode.run(); });

        modeRow.addView(appMode);
        modeRow.addView(shortcutMode);
        root.addView(modeRow);
        root.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(Ui.dp(this, 8));
        list.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 16));
        list.setClipToPadding(false);
        adapter = new AppAdapter();
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);

        loadEntries();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            Entry entry = filtered.get(position);
            Intent result = new Intent()
                    .putExtra(EXTRA_COMPONENT, entry.component.flattenToString())
                    .putExtra(EXTRA_IS_SHORTCUT, entry.isShortcut);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    @SuppressWarnings("deprecation")
    private void loadEntries() {
        all.clear();
        PackageManager pm = getPackageManager();
        if (showShortcuts) {
            // 显示所有 exported activity
            Intent query = new Intent(Intent.ACTION_MAIN);
            List<ResolveInfo> results = pm.queryIntentActivities(query, PackageManager.GET_ACTIVITIES);
            Set<String> seen = new HashSet<>();
            for (ResolveInfo result : results) {
                if (result.activityInfo == null || getPackageName().equals(result.activityInfo.packageName)) continue;
                ComponentName component = new ComponentName(result.activityInfo.packageName, result.activityInfo.name);
                String key = component.flattenToString();
                if (!seen.add(key)) continue;
                CharSequence labelValue = result.loadLabel(pm);
                String label = labelValue == null ? result.activityInfo.name : labelValue.toString();
                Drawable icon = result.loadIcon(pm);
                String parentApp = result.activityInfo.packageName;
                all.add(new Entry(component, label, icon, true, parentApp));
            }
        } else {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> results = pm.queryIntentActivities(query, 0);
            Set<String> seen = new HashSet<>();
            for (ResolveInfo result : results) {
                if (result.activityInfo == null || getPackageName().equals(result.activityInfo.packageName)) continue;
                ComponentName component = new ComponentName(result.activityInfo.packageName, result.activityInfo.name);
                if (!seen.add(component.flattenToString())) continue;
                CharSequence labelValue = result.loadLabel(pm);
                String label = labelValue == null ? result.activityInfo.packageName : labelValue.toString();
                Drawable icon = result.loadIcon(pm);
                all.add(new Entry(component, label, icon, false, null));
            }
        }
        Collator collator = Collator.getInstance(Locale.CHINA);
        all.sort(Comparator.comparing(entry -> entry.label, collator));
        filter("");
    }

    private void filter(String text) {
        String needle = text.trim().toLowerCase(Locale.ROOT);
        filtered.clear();
        for (Entry entry : all) {
            if (needle.isEmpty() || entry.label.toLowerCase(Locale.ROOT).contains(needle)
                    || entry.component.getPackageName().toLowerCase(Locale.ROOT).contains(needle)) {
                filtered.add(entry);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private final class AppAdapter extends BaseAdapter {
        @Override public int getCount() { return filtered.size(); }
        @Override public Entry getItem(int position) { return filtered.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
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
            TextView extra = new TextView(AppPickerActivity.this);
            if (entry.isShortcut && entry.parentApp != null) {
                extra.setText(entry.parentApp + " · 快捷方式");
            } else {
                extra.setText(entry.component.getPackageName());
            }
            extra.setTextColor(Ui.MUTED);
            extra.setTextSize(12);
            text.addView(label);
            text.addView(extra);
            row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            return row;
        }
    }

    private static final class Entry {
        final ComponentName component;
        final String label;
        final Drawable icon;
        final boolean isShortcut;
        final String parentApp;
        Entry(ComponentName component, String label, Drawable icon, boolean isShortcut, String parentApp) {
            this.component = component;
            this.label = label;
            this.icon = icon;
            this.isShortcut = isShortcut;
            this.parentApp = parentApp;
        }
    }
}
