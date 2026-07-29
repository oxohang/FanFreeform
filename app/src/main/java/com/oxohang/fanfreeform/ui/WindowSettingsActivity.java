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
import android.widget.TextView;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;

public final class WindowSettingsActivity extends Activity {
    private ConfigStore store;
    private SharedPreferences prefs;
    private WindowPreviewView portraitPreview;
    private WindowPreviewView landscapePreview;
    private int width, height, positionX, positionY;
    private int landscapeWidth, landscapeHeight, landscapePositionX, landscapePositionY;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ConfigStore(this); prefs = store.preferences();
        width = prefs.getInt(ConfigContract.KEY_WIDTH_PERCENT, ConfigContract.DEFAULT_WIDTH_PERCENT);
        height = prefs.getInt(ConfigContract.KEY_HEIGHT_PERCENT, ConfigContract.DEFAULT_HEIGHT_PERCENT);
        positionX = prefs.getInt(ConfigContract.KEY_POSITION_X, ConfigContract.DEFAULT_POSITION_X);
        positionY = prefs.getInt(ConfigContract.KEY_POSITION_Y, ConfigContract.DEFAULT_POSITION_Y);
        landscapeWidth = prefs.getInt(ConfigContract.KEY_LANDSCAPE_WIDTH_PERCENT,
                ConfigContract.DEFAULT_LANDSCAPE_WIDTH_PERCENT);
        landscapeHeight = prefs.getInt(ConfigContract.KEY_LANDSCAPE_HEIGHT_PERCENT,
                ConfigContract.DEFAULT_LANDSCAPE_HEIGHT_PERCENT);
        landscapePositionX = prefs.getInt(ConfigContract.KEY_LANDSCAPE_POSITION_X,
                ConfigContract.DEFAULT_LANDSCAPE_POSITION_X);
        landscapePositionY = prefs.getInt(ConfigContract.KEY_LANDSCAPE_POSITION_Y,
                ConfigContract.DEFAULT_LANDSCAPE_POSITION_Y);
        getWindow().setStatusBarColor(0xfff4f5fa);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(content());
    }

    private View content() {
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(0xfff4f5fa);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this,20),Ui.dp(this,14),Ui.dp(this,20),Ui.dp(this,36)); scroll.addView(root);
        root.addView(header("小窗位置设置"));
        LinearLayout portraitCard = card();
        portraitCard.addView(text("竖屏小窗",18,Ui.TEXT,Typeface.BOLD));
        portraitPreview = new WindowPreviewView(this);
        portraitPreview.update(width,height,positionX,positionY);
        portraitCard.addView(portraitPreview,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,280)));
        portraitCard.addView(slider("宽度",ConfigContract.KEY_WIDTH_PERCENT,40,90,width,v->v+"%"));
        portraitCard.addView(slider("高度",ConfigContract.KEY_HEIGHT_PERCENT,35,85,height,v->v+"%"));
        portraitCard.addView(slider("水平位置",ConfigContract.KEY_POSITION_X,0,100,positionX,WindowSettingsActivity::positionLabel));
        portraitCard.addView(slider("垂直位置",ConfigContract.KEY_POSITION_Y,0,100,positionY,WindowSettingsActivity::positionLabel));
        root.addView(portraitCard);

        LinearLayout landscapeCard = card();
        landscapeCard.addView(text("横屏小窗",18,Ui.TEXT,Typeface.BOLD));
        landscapePreview = new WindowPreviewView(this);
        landscapePreview.setLandscape(true);
        landscapePreview.update(landscapeWidth,landscapeHeight,
                landscapePositionX,landscapePositionY);
        landscapeCard.addView(landscapePreview,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,190)));
        landscapeCard.addView(slider("宽度",ConfigContract.KEY_LANDSCAPE_WIDTH_PERCENT,
                ConfigContract.MIN_LANDSCAPE_WIDTH_PERCENT,
                ConfigContract.MAX_LANDSCAPE_WIDTH_PERCENT,landscapeWidth,v->v+"%"));
        landscapeCard.addView(slider("高度",ConfigContract.KEY_LANDSCAPE_HEIGHT_PERCENT,
                35,85,landscapeHeight,v->v+"%"));
        landscapeCard.addView(slider("水平位置",ConfigContract.KEY_LANDSCAPE_POSITION_X,
                0,100,landscapePositionX,WindowSettingsActivity::positionLabel));
        landscapeCard.addView(slider("垂直位置",ConfigContract.KEY_LANDSCAPE_POSITION_Y,
                0,100,landscapePositionY,WindowSettingsActivity::positionLabel));
        LinearLayout.LayoutParams landscapeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        landscapeParams.topMargin=Ui.dp(this,14);
        root.addView(landscapeCard,landscapeParams);
        TextView note=text("应用、快捷方式和挂起恢复会自动使用当前屏幕方向对应的设置。",13,Ui.MUTED,Typeface.NORMAL);
        note.setPadding(0,Ui.dp(this,12),0,0); root.addView(note); return scroll;
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
                if(portraitPreview!=null)portraitPreview.update(width,height,positionX,positionY);
                if(landscapePreview!=null)landscapePreview.update(landscapeWidth,landscapeHeight,
                        landscapePositionX,landscapePositionY);}});group.addView(seek);return group;
    }
    private static String positionLabel(int v){if(v<34)return "偏左 / 偏上";if(v>66)return "偏右 / 偏下";return "居中";}
    private View header(String title){LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);TextView b=text("‹",38,Ui.TEXT,Typeface.NORMAL);b.setGravity(Gravity.CENTER);b.setOnClickListener(v->finish());h.addView(b,new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,52)));h.addView(text(title,26,Ui.TEXT,Typeface.BOLD));return h;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,16));c.setBackground(Ui.rounded(this,Ui.SURFACE,20));return c;}
    private TextView text(String v,float s,int c,int st){TextView t=new TextView(this);t.setText(v);t.setTextSize(s);t.setTextColor(c);t.setTypeface(null,st);return t;}
    private interface Label{String label(int value);}
}
