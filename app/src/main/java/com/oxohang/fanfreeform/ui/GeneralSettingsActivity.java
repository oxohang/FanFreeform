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
import android.widget.Switch;
import android.widget.TextView;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;

public final class GeneralSettingsActivity extends Activity {
    private ConfigStore store;
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this);
        prefs = store.preferences();
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xfff4f5fa);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 36));
        scroll.addView(root);
        root.addView(header("启用手势"));
        LinearLayout card = card();
        card.addView(toggle("启用 Hyper手势", "同时管理底角与侧滑手势",
                ConfigContract.KEY_ENABLED, ConfigContract.DEFAULT_ENABLED));
        card.addView(Ui.divider(this));
        card.addView(toggle("震动反馈", "所有手势和蜂窝选择共用此开关",
                ConfigContract.KEY_HAPTIC, ConfigContract.DEFAULT_HAPTIC));
        card.addView(Ui.divider(this));
        card.addView(toggle("显示选中应用名称", "统一用于底角和侧滑布局",
                ConfigContract.KEY_SHOW_SELECTED_APP_NAME,
                ConfigContract.DEFAULT_SHOW_SELECTED_APP_NAME));
        card.addView(Ui.divider(this));
        card.addView(toggle("强制圆形图标", "关闭后保留应用图标自带的圆角矩形或异形轮廓",
                ConfigContract.KEY_FORCE_CIRCULAR_ICONS,
                ConfigContract.DEFAULT_FORCE_CIRCULAR_ICONS));
        root.addView(card);
        Ui.addResetOption(this, root,
                "将恢复总开关、震动、应用名称和图标形状的默认值。",
                store::resetGeneralSettings);
        returnContent(scroll);
    }

    private void returnContent(View view) { setContentView(view); }
    private View header(String title) {
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹", 38, Ui.TEXT, Typeface.NORMAL); back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish()); header.addView(back,
                new LinearLayout.LayoutParams(Ui.dp(this,44), Ui.dp(this,52)));
        header.addView(text(title,26,Ui.TEXT,Typeface.BOLD)); return header;
    }
    private View toggle(String title,String subtitle,String key,boolean def) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0,Ui.dp(this,12),0,Ui.dp(this,12));
        LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title,16,Ui.TEXT,Typeface.BOLD)); labels.addView(text(subtitle,13,Ui.MUTED,Typeface.NORMAL));
        row.addView(labels,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        Switch toggle = new Switch(this); toggle.setChecked(prefs.getBoolean(key,def));
        toggle.setOnCheckedChangeListener((b,c)->store.putBoolean(key,c)); row.addView(toggle); return row;
    }
    private LinearLayout card(){ LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(Ui.dp(this,18),Ui.dp(this,10),Ui.dp(this,18),Ui.dp(this,10));c.setBackground(Ui.rounded(this,Ui.SURFACE,20));return c; }
    private TextView text(String v,float s,int c,int st){TextView t=new TextView(this);t.setText(v);t.setTextSize(s);t.setTextColor(c);t.setTypeface(null,st);return t;}
}
