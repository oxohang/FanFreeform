package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;
import com.oxohang.fanfreeform.config.WindowPositionPolicy;

public final class WindowSettingsActivity extends Activity {
    private ConfigStore store;
    private SharedPreferences prefs;
    private WindowPreviewView portraitPreview;
    private WindowPreviewView landscapePreview;
    private LinearLayout customControls;
    private LinearLayout portraitControls;
    private LinearLayout landscapeControls;
    private LinearLayout portraitProportionalControls;
    private LinearLayout portraitIndependentControls;
    private LinearLayout landscapeProportionalControls;
    private LinearLayout landscapeIndependentControls;
    private boolean portraitCustomEnabled;
    private boolean landscapeCustomEnabled;
    private boolean portraitProportionalEnabled;
    private boolean landscapeProportionalEnabled;
    private int width, height, positionX, positionY;
    private int landscapeWidth, landscapeHeight, landscapePositionX, landscapePositionY;
    private int portraitNativeScale, landscapeNativeScale;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this); prefs = store.preferences();
        width = prefs.getInt(ConfigContract.KEY_WIDTH_PERCENT, ConfigContract.DEFAULT_WIDTH_PERCENT);
        height = prefs.getInt(ConfigContract.KEY_HEIGHT_PERCENT, ConfigContract.DEFAULT_HEIGHT_PERCENT);
        positionX = WindowPositionPolicy.clampPercent(prefs.getInt(
                ConfigContract.KEY_POSITION_X, ConfigContract.DEFAULT_POSITION_X));
        positionY = WindowPositionPolicy.clampPercent(prefs.getInt(
                ConfigContract.KEY_POSITION_Y, ConfigContract.DEFAULT_POSITION_Y));
        landscapeWidth = prefs.getInt(ConfigContract.KEY_LANDSCAPE_WIDTH_PERCENT,
                ConfigContract.DEFAULT_LANDSCAPE_WIDTH_PERCENT);
        landscapeHeight = prefs.getInt(ConfigContract.KEY_LANDSCAPE_HEIGHT_PERCENT,
                ConfigContract.DEFAULT_LANDSCAPE_HEIGHT_PERCENT);
        landscapePositionX = WindowPositionPolicy.clampPercent(prefs.getInt(
                ConfigContract.KEY_LANDSCAPE_POSITION_X,
                ConfigContract.DEFAULT_LANDSCAPE_POSITION_X));
        landscapePositionY = WindowPositionPolicy.clampPercent(prefs.getInt(
                ConfigContract.KEY_LANDSCAPE_POSITION_Y,
                ConfigContract.DEFAULT_LANDSCAPE_POSITION_Y));
        portraitCustomEnabled = prefs.getBoolean(
                ConfigContract.KEY_CUSTOM_WINDOW_PORTRAIT_ENABLED,
                prefs.getBoolean(ConfigContract.KEY_CUSTOM_WINDOW_BOUNDS_ENABLED,
                        ConfigContract.DEFAULT_CUSTOM_WINDOW_PORTRAIT_ENABLED));
        landscapeCustomEnabled = prefs.getBoolean(
                ConfigContract.KEY_CUSTOM_WINDOW_LANDSCAPE_ENABLED,
                ConfigContract.DEFAULT_CUSTOM_WINDOW_LANDSCAPE_ENABLED);
        portraitProportionalEnabled = prefs.getBoolean(
                ConfigContract.KEY_PORTRAIT_PROPORTIONAL_SIZE_ENABLED,
                ConfigContract.DEFAULT_PORTRAIT_PROPORTIONAL_SIZE_ENABLED);
        landscapeProportionalEnabled = prefs.getBoolean(
                ConfigContract.KEY_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED,
                ConfigContract.DEFAULT_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED);
        portraitNativeScale = prefs.getInt(
                ConfigContract.KEY_PORTRAIT_NATIVE_SCALE_PERCENT,
                ConfigContract.DEFAULT_NATIVE_WINDOW_SCALE_PERCENT);
        landscapeNativeScale = prefs.getInt(
                ConfigContract.KEY_LANDSCAPE_NATIVE_SCALE_PERCENT,
                ConfigContract.DEFAULT_NATIVE_WINDOW_SCALE_PERCENT);
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(content());
    }

    private View content() {
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(0xfff4f5fa);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this,20),Ui.dp(this,14),Ui.dp(this,20),Ui.dp(this,36)); scroll.addView(root);
        root.addView(header("小窗位置设置"));

        customControls = new LinearLayout(this);
        customControls.setOrientation(LinearLayout.VERTICAL);
        LinearLayout portraitCard = card();
        portraitCard.addView(directionToggle("竖屏小窗", "关闭后由 HyperOS 管理竖屏位置",
                ConfigContract.KEY_CUSTOM_WINDOW_PORTRAIT_ENABLED,
                portraitCustomEnabled, true));
        portraitControls = new LinearLayout(this);
        portraitControls.setOrientation(LinearLayout.VERTICAL);
        portraitControls.addView(proportionalToggle(true));
        portraitPreview = new WindowPreviewView(this);
        updatePreviews();
        portraitControls.addView(portraitPreview,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,280)));
        portraitProportionalControls = new LinearLayout(this);
        portraitProportionalControls.setOrientation(LinearLayout.VERTICAL);
        portraitProportionalControls.addView(slider("系统尺寸缩放",
                ConfigContract.KEY_PORTRAIT_NATIVE_SCALE_PERCENT,
                ConfigContract.MIN_NATIVE_WINDOW_SCALE_PERCENT,
                ConfigContract.MAX_NATIVE_WINDOW_SCALE_PERCENT,
                portraitNativeScale,v->v+"%"));
        portraitControls.addView(portraitProportionalControls);
        portraitIndependentControls = new LinearLayout(this);
        portraitIndependentControls.setOrientation(LinearLayout.VERTICAL);
        portraitIndependentControls.addView(slider("宽度",ConfigContract.KEY_WIDTH_PERCENT,40,90,width,v->v+"%"));
        portraitIndependentControls.addView(slider("高度",ConfigContract.KEY_HEIGHT_PERCENT,35,85,height,v->v+"%"));
        portraitControls.addView(portraitIndependentControls);
        portraitControls.addView(slider("水平位置",ConfigContract.KEY_POSITION_X,0,100,positionX,WindowPositionPolicy::percentageLabel));
        portraitControls.addView(slider("垂直位置",ConfigContract.KEY_POSITION_Y,0,100,positionY,WindowPositionPolicy::percentageLabel));
        portraitCard.addView(portraitControls);
        customControls.addView(portraitCard);

        LinearLayout landscapeCard = card();
        landscapeCard.addView(directionToggle("横屏小窗", "关闭后由 HyperOS 管理横屏位置",
                ConfigContract.KEY_CUSTOM_WINDOW_LANDSCAPE_ENABLED,
                landscapeCustomEnabled, false));
        landscapeControls = new LinearLayout(this);
        landscapeControls.setOrientation(LinearLayout.VERTICAL);
        landscapeControls.addView(proportionalToggle(false));
        landscapePreview = new WindowPreviewView(this);
        landscapePreview.setLandscape(true);
        updatePreviews();
        landscapeControls.addView(landscapePreview,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,190)));
        landscapeProportionalControls = new LinearLayout(this);
        landscapeProportionalControls.setOrientation(LinearLayout.VERTICAL);
        landscapeProportionalControls.addView(slider("系统尺寸缩放",
                ConfigContract.KEY_LANDSCAPE_NATIVE_SCALE_PERCENT,
                ConfigContract.MIN_NATIVE_WINDOW_SCALE_PERCENT,
                ConfigContract.MAX_NATIVE_WINDOW_SCALE_PERCENT,
                landscapeNativeScale,v->v+"%"));
        landscapeControls.addView(landscapeProportionalControls);
        landscapeIndependentControls = new LinearLayout(this);
        landscapeIndependentControls.setOrientation(LinearLayout.VERTICAL);
        landscapeIndependentControls.addView(slider("宽度",ConfigContract.KEY_LANDSCAPE_WIDTH_PERCENT,
                ConfigContract.MIN_LANDSCAPE_WIDTH_PERCENT,
                ConfigContract.MAX_LANDSCAPE_WIDTH_PERCENT,landscapeWidth,v->v+"%"));
        landscapeIndependentControls.addView(slider("高度",ConfigContract.KEY_LANDSCAPE_HEIGHT_PERCENT,
                35,85,landscapeHeight,v->v+"%"));
        landscapeControls.addView(landscapeIndependentControls);
        landscapeControls.addView(slider("水平位置",ConfigContract.KEY_LANDSCAPE_POSITION_X,
                0,100,landscapePositionX,WindowPositionPolicy::percentageLabel));
        landscapeControls.addView(slider("垂直位置",ConfigContract.KEY_LANDSCAPE_POSITION_Y,
                0,100,landscapePositionY,WindowPositionPolicy::percentageLabel));
        landscapeCard.addView(landscapeControls);
        LinearLayout.LayoutParams landscapeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        landscapeParams.topMargin=Ui.dp(this,14);
        customControls.addView(landscapeCard,landscapeParams);
        TextView note=text("两个方向独立生效；关闭的方向完全交由 HyperOS 管理。",13,Ui.MUTED,Typeface.NORMAL);
        note.setPadding(0,Ui.dp(this,12),0,0); customControls.addView(note);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controlsParams.topMargin = Ui.dp(this, 14);
        root.addView(customControls, controlsParams);
        updateCustomControls();
        Ui.addResetOption(this, root,
                "将恢复默认大小与位置，并重新开启竖屏、关闭横屏自定义位置。",
                store::resetWindowSettings);
        return scroll;
    }

    private void updateCustomControls() {
        updateDirectionControls(portraitControls, portraitCustomEnabled);
        updateDirectionControls(landscapeControls, landscapeCustomEnabled);
        updateSizeModeControls();
    }

    private void updateSizeModeControls() {
        if (portraitProportionalControls != null) {
            portraitProportionalControls.setVisibility(
                    portraitProportionalEnabled ? View.VISIBLE : View.GONE);
        }
        if (portraitIndependentControls != null) {
            portraitIndependentControls.setVisibility(
                    portraitProportionalEnabled ? View.GONE : View.VISIBLE);
        }
        if (landscapeProportionalControls != null) {
            landscapeProportionalControls.setVisibility(
                    landscapeProportionalEnabled ? View.VISIBLE : View.GONE);
        }
        if (landscapeIndependentControls != null) {
            landscapeIndependentControls.setVisibility(
                    landscapeProportionalEnabled ? View.GONE : View.VISIBLE);
        }
        updatePreviews();
    }

    private void updateDirectionControls(LinearLayout controls, boolean enabled) {
        if (controls == null) return;
        controls.setAlpha(enabled ? 1f : 0.42f);
        setEnabled(controls, enabled);
    }

    private View directionToggle(String title, String subtitle, String key,
                                 boolean checked, boolean portrait) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 18, Ui.TEXT, Typeface.BOLD));
        labels.addView(text(subtitle, 13, Ui.MUTED, Typeface.NORMAL));
        row.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener((button, enabled) -> {
            if (portrait) portraitCustomEnabled = enabled;
            else landscapeCustomEnabled = enabled;
            store.putBoolean(key, enabled);
            updateCustomControls();
        });
        row.addView(toggle);
        return row;
    }

    private View proportionalToggle(boolean portrait) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 4));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("跟随系统原生尺寸", 16, Ui.TEXT, Typeface.BOLD));
        labels.addView(text("读取 HyperOS 原生小窗，仅等比缩放",
                13, Ui.MUTED, Typeface.NORMAL));
        row.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        boolean checked = portrait ? portraitProportionalEnabled
                : landscapeProportionalEnabled;
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener((button, enabled) -> {
            if (portrait) {
                portraitProportionalEnabled = enabled;
                store.putBoolean(ConfigContract.KEY_PORTRAIT_PROPORTIONAL_SIZE_ENABLED,
                        enabled);
            } else {
                landscapeProportionalEnabled = enabled;
                store.putBoolean(ConfigContract.KEY_LANDSCAPE_PROPORTIONAL_SIZE_ENABLED,
                        enabled);
            }
            updateSizeModeControls();
        });
        row.addView(toggle);
        return row;
    }

    private void updatePreviews() {
        if (portraitPreview != null) {
            portraitPreview.setVisibility(View.VISIBLE);
            portraitPreview.update(width, height, positionX, positionY,
                    portraitProportionalEnabled, portraitNativeScale);
        }
        if (landscapePreview != null) {
            landscapePreview.setVisibility(View.VISIBLE);
            landscapePreview.update(landscapeWidth, landscapeHeight,
                    landscapePositionX, landscapePositionY,
                    landscapeProportionalEnabled, landscapeNativeScale);
        }
    }

    private void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            setEnabled(group.getChildAt(index), enabled);
        }
    }

    private View slider(String title,String key,int min,int max,int current,Label labeler){
        LinearLayout group=new LinearLayout(this);group.setOrientation(LinearLayout.VERTICAL);group.setPadding(0,Ui.dp(this,14),0,0);
        LinearLayout line=new LinearLayout(this);TextView titleView=text(title,15,Ui.TEXT,Typeface.BOLD);
        TextView value=text(labeler.label(current),14,Ui.ACCENT,Typeface.BOLD);value.setGravity(Gravity.END);
        line.addView(titleView,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));line.addView(value);group.addView(line);
        SeekBar seek=new SeekBar(this);seek.setMax(max-min);seek.setProgress(current-min);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}
            public void onProgressChanged(SeekBar b,int p,boolean user){int resolved=min+p;value.setText(labeler.label(resolved));if(!user)return;
                store.putInt(key,resolved);if(key.equals(ConfigContract.KEY_WIDTH_PERCENT))width=resolved;if(key.equals(ConfigContract.KEY_HEIGHT_PERCENT))height=resolved;
                if(key.equals(ConfigContract.KEY_POSITION_X))positionX=resolved;if(key.equals(ConfigContract.KEY_POSITION_Y))positionY=resolved;
                if(key.equals(ConfigContract.KEY_LANDSCAPE_WIDTH_PERCENT))landscapeWidth=resolved;
                if(key.equals(ConfigContract.KEY_LANDSCAPE_HEIGHT_PERCENT))landscapeHeight=resolved;
                if(key.equals(ConfigContract.KEY_LANDSCAPE_POSITION_X))landscapePositionX=resolved;
                if(key.equals(ConfigContract.KEY_LANDSCAPE_POSITION_Y))landscapePositionY=resolved;
                if(key.equals(ConfigContract.KEY_PORTRAIT_NATIVE_SCALE_PERCENT))portraitNativeScale=resolved;
                if(key.equals(ConfigContract.KEY_LANDSCAPE_NATIVE_SCALE_PERCENT))landscapeNativeScale=resolved;
                updatePreviews();}});group.addView(seek);return group;
    }
    private View header(String title){LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);TextView b=text("‹",38,Ui.TEXT,Typeface.NORMAL);b.setGravity(Gravity.CENTER);b.setOnClickListener(v->finish());h.addView(b,new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,52)));h.addView(text(title,26,Ui.TEXT,Typeface.BOLD));return h;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,16));c.setBackground(Ui.rounded(this,Ui.SURFACE,20));return c;}
    private TextView text(String v,float s,int c,int st){TextView t=new TextView(this);t.setText(v);t.setTextSize(s);t.setTextColor(c);t.setTypeface(null,st);return t;}
    private interface Label{String label(int value);}
}
