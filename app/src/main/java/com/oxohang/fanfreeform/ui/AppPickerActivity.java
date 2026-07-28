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
    private final ArrayList<Entry> all = new ArrayList<>();
    private final ArrayList<Entry> filtered = new ArrayList<>();
    private AppAdapter adapter;

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
        title.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 16));
        root.addView(title);

        EditText search = new EditText(this);
        search.setHint("搜索应用");
        search.setSingleLine(true);
        search.setTextSize(16);
        search.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        search.setBackground(Ui.rounded(this, Color.WHITE, 16));
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

        loadApps();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            Entry entry = filtered.get(position);
            Intent result = new Intent().putExtra(EXTRA_COMPONENT, entry.component.flattenToString());
            setResult(RESULT_OK, result);
            finish();
        });
    }

    @SuppressWarnings("deprecation")
    private void loadApps() {
        PackageManager pm = getPackageManager();
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
            all.add(new Entry(component, label, icon));
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
            TextView packageName = new TextView(AppPickerActivity.this);
            packageName.setText(entry.component.getPackageName());
            packageName.setTextColor(Ui.MUTED);
            packageName.setTextSize(12);
            text.addView(label);
            text.addView(packageName);
            row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            return row;
        }
    }

    private static final class Entry {
        final ComponentName component;
        final String label;
        final Drawable icon;
        Entry(ComponentName component, String label, Drawable icon) {
            this.component = component;
            this.label = label;
            this.icon = icon;
        }
    }
}
