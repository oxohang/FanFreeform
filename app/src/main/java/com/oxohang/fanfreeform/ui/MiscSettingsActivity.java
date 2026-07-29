package com.oxohang.fanfreeform.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.oxohang.fanfreeform.config.ConfigContract;
import com.oxohang.fanfreeform.config.ConfigStore;

public final class MiscSettingsActivity extends Activity {
    private ConfigStore store; private TextView status; private Button reload;
    @Override protected void onCreate(Bundle state){super.onCreate(state);store=new ConfigStore(this);getWindow().setStatusBarColor(0xfff4f5fa);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);setContentView(content());}
    @Override protected void onResume(){super.onResume();updateStatus();}
    private View content(){ScrollView s=new ScrollView(this);s.setBackgroundColor(0xfff4f5fa);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(Ui.dp(this,20),Ui.dp(this,14),Ui.dp(this,20),Ui.dp(this,36));s.addView(r);r.addView(header());LinearLayout c=card();c.addView(text("版本 " + versionLabel(),15,Ui.TEXT,Typeface.BOLD));status=text("",13,Ui.MUTED,Typeface.NORMAL);status.setPadding(0,Ui.dp(this,8),0,Ui.dp(this,12));c.addView(status);reload=new Button(this);reload.setText("一键重载 Hook 进程");reload.setOnClickListener(v->reloadProcesses());c.addView(reload,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,52)));TextView note=text("固定热重载系统界面和小米桌面，不会重启手机。",13,Ui.MUTED,Typeface.NORMAL);note.setPadding(0,Ui.dp(this,8),0,0);c.addView(note);r.addView(c);LinearLayout defaults=card();Button reset=new Button(this);reset.setText("恢复默认参数（保留应用清单）");reset.setOnClickListener(v->{store.resetTuning();Toast.makeText(this,"已恢复默认参数",Toast.LENGTH_SHORT).show();});defaults.addView(reset);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=Ui.dp(this,12);r.addView(defaults,p);return s;}
    private String versionLabel(){try{android.content.pm.PackageInfo i=getPackageManager().getPackageInfo(getPackageName(),0);return i.versionName+" · build "+i.getLongVersionCode();}catch(Exception ignored){return "未知";}}
    private void reloadProcesses(){reload.setEnabled(false);reload.setText("正在重载…");new Thread(()->{String error=null;try{runRoot("killall com.android.systemui");Thread.sleep(650L);runRoot("killall com.miui.home");Thread.sleep(450L);Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(home);}catch(Exception e){error=e.getMessage();}String result=error;new Handler(Looper.getMainLooper()).post(()->{Toast.makeText(this,result==null?"已热重载系统界面和小米桌面":"重载失败："+result,Toast.LENGTH_LONG).show();new Handler(Looper.getMainLooper()).postDelayed(()->{reload.setEnabled(true);reload.setText("一键重载 Hook 进程");updateStatus();},5000L);});},"hook-reload").start();}
    private static void runRoot(String command)throws Exception{Process p=new ProcessBuilder("su","-c",command).start();int code=p.waitFor();if(code!=0)throw new IllegalStateException(command+" 退出码 "+code);}
    private void updateStatus(){if(status==null)return;String value=store.preferences().getString(ConfigContract.KEY_INTERFACE_STATUS,"");long time=store.preferences().getLong(ConfigContract.KEY_INTERFACE_TIME,0);String relative=time<=0?"暂无连接时间":DateUtils.getRelativeTimeSpanString(time,System.currentTimeMillis(),DateUtils.MINUTE_IN_MILLIS).toString();status.setText((value==null||value.isEmpty()?"Hook 尚未连接":value)+" · "+relative);}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,16));c.setBackground(Ui.rounded(this,Ui.SURFACE,20));return c;}
    private View header(){LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);TextView b=text("‹",38,Ui.TEXT,Typeface.NORMAL);b.setGravity(Gravity.CENTER);b.setOnClickListener(v->finish());h.addView(b,new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,52)));h.addView(text("杂项设置",26,Ui.TEXT,Typeface.BOLD));return h;}
    private TextView text(String v,float s,int c,int st){TextView t=new TextView(this);t.setText(v);t.setTextSize(s);t.setTextColor(c);t.setTypeface(null,st);return t;}
}
