package com.oxohang.fanfreeform.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.oxohang.fanfreeform.config.AppTarget;
import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;
import com.oxohang.fanfreeform.config.ShortcutIconLoader;

import org.json.JSONArray;

import java.util.ArrayList;

@SuppressLint("SetTextI18n")
public final class TargetManagerActivity extends Activity {
    public static final String EXTRA_KIND = "kind";
    public static final String KIND_FAN = "fan";
    public static final String KIND_SIDE = "side";
    public static final String KIND_HONEYCOMB = "honeycomb";
    private static final int REQUEST_PICK_TARGETS = 121;

    private final ArrayList<AppTarget> targets = new ArrayList<>();
    private ConfigStore store;
    private SharedPreferences prefs;
    private String kind;
    private TextView countText;
    private LinearLayout targetContainer;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        prefs = store.preferences();
        kind = getIntent().getStringExtra(EXTRA_KIND);
        if (!KIND_SIDE.equals(kind) && !KIND_HONEYCOMB.equals(kind)) kind = KIND_FAN;
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().setNavigationBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(content());
        reloadTargets();
    }

    @Override protected void onResume() {
        super.onResume();
        if (targetContainer != null) reloadTargets();
    }

    private View content() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(0xfff4f5fa);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20),
                Ui.dp(this, 104));
        scroll.addView(root);

        LinearLayout header = row();
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("返回");
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 52)));
        header.addView(text(title(), 26, Ui.TEXT, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header);

        countText = text("", 14, Ui.MUTED, Typeface.NORMAL);
        countText.setPadding(Ui.dp(this, 44), 0, 0, Ui.dp(this, 16));
        root.addView(countText);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        card.setBackground(Ui.rounded(this, Color.WHITE, 20));
        card.setElevation(Ui.dp(this, 1));
        targetContainer = new LinearLayout(this);
        targetContainer.setOrientation(LinearLayout.VERTICAL);
        card.addView(targetContainer);
        root.addView(card);
        Ui.addResetOption(this, root,
                "将用当前手机上可用的预设应用替换这个清单，当前排序和快捷方式会被覆盖。",
                this::resetTargets);

        frame.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView add = text("＋", 32, Color.WHITE, Typeface.NORMAL);
        add.setGravity(Gravity.CENTER);
        add.setContentDescription("添加应用或快捷方式");
        add.setBackground(Ui.rounded(this, Ui.ACCENT, 30));
        add.setElevation(Ui.dp(this, 8));
        add.setOnClickListener(view -> openPicker());
        FrameLayout.LayoutParams addParams = new FrameLayout.LayoutParams(
                Ui.dp(this, 58), Ui.dp(this, 58), Gravity.END | Gravity.BOTTOM);
        addParams.rightMargin = Ui.dp(this, 24);
        addParams.bottomMargin = Ui.dp(this, 26);
        frame.addView(add, addParams);
        return frame;
    }

    private void reloadTargets() {
        targets.clear();
        if (KIND_SIDE.equals(kind)) targets.addAll(store.getSideTargets());
        else if (KIND_HONEYCOMB.equals(kind)) targets.addAll(store.getHoneycombTargets());
        else targets.addAll(store.getTargets());
        renderTargets();
    }

    private void renderTargets() {
        if (targetContainer == null) return;
        targetContainer.removeAllViews();
        countText.setText("已选 " + targets.size() + " / " + maximum()
                + " · 长按图标可调整顺序");
        if (targets.isEmpty()) {
            TextView empty = text("尚未选择应用，点击右下角 ＋ 添加。",
                    14, Ui.MUTED, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(Ui.dp(this, 12), Ui.dp(this, 38), Ui.dp(this, 12),
                    Ui.dp(this, 38));
            targetContainer.addView(empty);
            return;
        }
        for (AppTarget target : new ArrayList<>(targets)) {
            if (targetContainer.getChildCount() > 0) targetContainer.addView(Ui.divider(this));
            LinearLayout item = row();
            item.setOnDragListener((view, event) -> onDrop(target, event));
            ImageView icon = new ImageView(this);
            TargetPresentation presentation = presentation(target);
            icon.setImageDrawable(presentation.icon);
            icon.setOnLongClickListener(view -> view.startDragAndDrop(
                    ClipData.newPlainText("target", target.toJson().toString()),
                    new View.DragShadowBuilder(item), target, 0));
            item.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setPadding(Ui.dp(this, 12), 0, 0, 0);
            labels.addView(text(presentation.label, 16, Ui.TEXT, Typeface.BOLD));
            labels.addView(text(presentation.secondary, 12, Ui.MUTED, Typeface.NORMAL));
            item.addView(labels, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            Button remove = compactButton("移除");
            remove.setOnClickListener(view -> {
                targets.remove(target);
                saveTargets();
            });
            item.addView(remove);
            targetContainer.addView(item);
        }
    }

    private TargetPresentation presentation(AppTarget target) {
        PackageManager pm = getPackageManager();
        Drawable icon = getApplicationInfo().loadIcon(pm);
        String label = target.isShortcut() && !target.shortcutLabel.isEmpty()
                ? target.shortcutLabel : target.packageName();
        String secondary = target.packageName() + (target.isShortcut() ? " · 快捷方式" : "");
        try {
            if (target.isShortcut()) {
                icon = ShortcutIconLoader.load(this, target.packageName(),
                        target.shortcutId, target.userId);
            } else if (target.componentName() != null) {
                ActivityInfo info = pm.getActivityInfo(target.componentName(), 0);
                CharSequence resolvedLabel = info.loadLabel(pm);
                if (resolvedLabel != null) label = resolvedLabel.toString();
                icon = info.loadIcon(pm);
                if (target.userId != 0) secondary = target.packageName() + " · 双开";
            }
        } catch (Exception ignored) {
            label = label + "（已不可用）";
        }
        return new TargetPresentation(label, secondary, icon);
    }

    private boolean onDrop(AppTarget dropTarget, DragEvent event) {
        if (!(event.getLocalState() instanceof AppTarget)) return false;
        if (event.getAction() == DragEvent.ACTION_DROP) {
            AppTarget dragged = (AppTarget) event.getLocalState();
            int from = targets.indexOf(dragged);
            int to = targets.indexOf(dropTarget);
            if (from >= 0 && to >= 0 && from != to) {
                targets.remove(from);
                targets.add(Math.min(to, targets.size()), dragged);
                saveTargets();
            }
        }
        return true;
    }

    private void openPicker() {
        JSONArray selected = new JSONArray();
        for (AppTarget target : targets) selected.put(target.toJson());
        Intent intent = new Intent(this, AppPickerActivity.class);
        intent.putExtra(AppPickerActivity.EXTRA_MODE,
                AppPickerActivity.MODE_APPS_AND_SHORTCUTS);
        intent.putExtra(AppPickerActivity.EXTRA_MULTI, true);
        intent.putExtra(AppPickerActivity.EXTRA_SELECTED, selected.toString());
        intent.putExtra(AppPickerActivity.EXTRA_MAX, maximum());
        startActivityForResult(intent, REQUEST_PICK_TARGETS);
    }

    @Override @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_TARGETS || resultCode != RESULT_OK || data == null) return;
        targets.clear();
        try {
            JSONArray array = new JSONArray(data.getStringExtra(AppPickerActivity.EXTRA_TARGETS));
            for (int index = 0; index < array.length() && targets.size() < maximum(); index++) {
                AppTarget target = AppTarget.fromJson(array.optJSONObject(index));
                if (target == null) continue;
                if (!targets.contains(target)) targets.add(target);
            }
        } catch (Exception ignored) { }
        saveTargets();
    }

    private void saveTargets() {
        if (KIND_SIDE.equals(kind)) store.setSideTargets(targets);
        else if (KIND_HONEYCOMB.equals(kind)) store.setHoneycombTargets(targets);
        else store.setTargets(targets);
        renderTargets();
    }

    private void resetTargets() {
        if (KIND_SIDE.equals(kind)) store.resetSideTargets();
        else if (KIND_HONEYCOMB.equals(kind)) store.resetHoneycombTargets();
        else store.resetBottomTargets();
        reloadTargets();
    }

    private int maximum() {
        if (KIND_SIDE.equals(kind)) return prefs.getInt(ConfigContract.KEY_SIDE_MAX_TARGETS,
                ConfigContract.DEFAULT_SIDE_MAX_TARGETS);
        if (KIND_HONEYCOMB.equals(kind)) return prefs.getInt(
                ConfigContract.KEY_HONEYCOMB_MAX_TARGETS,
                ConfigContract.DEFAULT_HONEYCOMB_MAX_TARGETS);
        return prefs.getInt(ConfigContract.KEY_FAN_MAX_TARGETS,
                ConfigContract.DEFAULT_FAN_MAX_TARGETS);
    }

    private String title() {
        if (KIND_SIDE.equals(kind)) return "侧滑应用与快捷方式";
        if (KIND_HONEYCOMB.equals(kind)) return "蜂窝应用与快捷方式";
        return "小窗应用与快捷方式";
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));
        return row;
    }

    private Button compactButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(12);
        button.setTextColor(Ui.ACCENT);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        button.setBackground(Ui.rounded(this, 0xffeef0ff, 10));
        return button;
    }

    private TextView text(String value, float size, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(null, style);
        return text;
    }

    private static final class TargetPresentation {
        final String label;
        final String secondary;
        final Drawable icon;

        TargetPresentation(String label, String secondary, Drawable icon) {
            this.label = label;
            this.secondary = secondary;
            this.icon = icon;
        }
    }
}
