package com.example.moonlightlatencyfixroot;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(18,18,22), FG=Color.rgb(242,242,246), MUTED=Color.rgb(170,170,180);
    private static final int GREEN=Color.rgb(30,150,82), RED=Color.rgb(190,55,65), GRAY=Color.rgb(75,75,82);
    private TextView status, little, big;
    private Button off, boost, govDefault, govPerformance, refAuto, ref60, ref90;
    private android.content.SharedPreferences state;
    private int cpuMode=2, governorMode=2, refreshMode=3;
    private String littleValue="Unknown", bigValue="Unknown";
    private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){cpuMode=i.getIntExtra("cpuMode",2);governorMode=i.getIntExtra("governorMode",2);refreshMode=i.getIntExtra("refreshMode",3);littleValue=i.getStringExtra("littleValue");bigValue=i.getStringExtra("bigValue");render(i.getStringExtra("message"));}};
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(10));return d;}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private void section(LinearLayout parent,String name){TextView t=text(name,18,FG);t.setTypeface(null,Typeface.BOLD);t.setPadding(0,dp(18),0,dp(10));parent.addView(t);}
    private Button button(LinearLayout row,String name){Button b=new Button(this);b.setText(name);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setTypeface(null,Typeface.BOLD);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(50),1);p.setMargins(dp(3),0,dp(3),0);row.addView(b,p);return b;}
    private LinearLayout row(LinearLayout parent){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);parent.addView(r,new LinearLayout.LayoutParams(-1,-2));return r;}
    private void send(String action){try {startForegroundService(new Intent(this,LatencyService.class).setAction(action));status.setText("Applying " + action + "...");}catch(Exception e){status.setText("Error: "+e.getMessage());}}
    private void style(Button b,boolean selected){b.setEnabled(true);b.setBackground(shape(selected?GREEN:GRAY));b.setAlpha(1f);}
    private String mhz(String value){try{return String.format(java.util.Locale.US,"%d MHz",Long.parseLong(value)/1000);}catch(Exception e){return "Unavailable";}}
    private void render(String msg){
        little.setText("Little CPU minimum     "+mhz(littleValue));
        big.setText("Big CPU minimum          "+mhz(bigValue));
        style(off,cpuMode==0);style(boost,cpuMode==1);
        style(govDefault,governorMode==0);style(govPerformance,governorMode==1);
        style(refAuto,refreshMode==0);style(ref60,refreshMode==1);style(ref90,refreshMode==2);
        status.setText(msg==null?"Reading actual system settings...":msg);
    }
    @Override public void onCreate(Bundle saved){super.onCreate(saved);
        state=getSharedPreferences("ui_state",MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(20),dp(28),dp(20),dp(30));l.setBackgroundColor(BG);scroll.addView(l);
        TextView title=text("MoonlightLatencyFix-ROOT",23,FG);title.setTypeface(null,Typeface.BOLD);l.addView(title);
        TextView sub=text("Root CPU & display controls for low-latency Moonlight streaming",14,MUTED);sub.setPadding(0,dp(8),0,dp(12));l.addView(sub);
        status=text("Checking root...",15,FG);l.addView(status);
        section(l,"CPU Boost");LinearLayout cpu=row(l);off=button(cpu,"OFF");boost=button(cpu,"BOOST");
        little=text("",14,FG);little.setPadding(dp(6),dp(14),0,dp(7));l.addView(little);
        big=text("",14,FG);big.setPadding(dp(6),0,0,dp(6));l.addView(big);
        TextView explain=text("Boost sets minimum Little/Big CPU frequencies to 1,800 / 1,100 MHz. The CPU can still scale above these values. No busy loop is used.",13,MUTED);l.addView(explain);
        section(l,"CPU Governor");LinearLayout governors=row(l);govDefault=button(governors,"Default");govPerformance=button(governors,"Performance");
        TextView govInfo=text("Independent of CPU Boost. Default restores the original governor. Performance is available only if supported by the kernel.",13,MUTED);govInfo.setPadding(0,dp(7),0,0);l.addView(govInfo);
        section(l,"Display Refresh Rate");LinearLayout refresh=row(l);refAuto=button(refresh,"AUTO");ref60=button(refresh,"60 Hz");ref90=button(refresh,"90 Hz");
        TextView refreshInfo=text("Auto removes the system refresh-rate overrides. Refresh settings are independent of CPU Boost.",13,MUTED);refreshInfo.setPadding(0,dp(7),0,0);l.addView(refreshInfo);
        section(l,"Safety");
        TextView safety=text("Close Notification & Restore Defaults restores original CPU minimum frequencies and governors, sets refresh to Auto, then stops the background service and removes the persistent notification. Recovery is attempted on next launch after an unexpected process death.",13,MUTED);
        l.addView(safety);
        TextView warningTitle=text("WARNING",13,RED);warningTitle.setTypeface(null,Typeface.BOLD);warningTitle.setPadding(0,dp(18),0,dp(5));l.addView(warningTitle);
        TextView warning=text("Before uninstalling, tap CLOSE NOTIFICATION & RESTORE DEFAULTS. Uninstalling or force-stopping the app while BOOST or other overrides are active may leave modified settings in place until a reboot or manual restoration. Android does not guarantee cleanup during uninstall.",13,MUTED);l.addView(warning);
        Button exit=new Button(this);exit.setText("CLOSE NOTIFICATION & RESTORE DEFAULTS");exit.setAllCaps(false);exit.setTextColor(Color.WHITE);exit.setTextSize(11);exit.setTypeface(null,Typeface.BOLD);exit.setGravity(Gravity.CENTER);exit.setBackground(shape(RED));
        android.graphics.drawable.Drawable power=getDrawable(R.drawable.ic_power_white);
        if(power!=null){power.setBounds(0,0,dp(18),dp(18));exit.setCompoundDrawables(power,null,null,null);exit.setCompoundDrawablePadding(dp(7));}
        exit.setPadding(dp(9),0,dp(9),0);
        LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(58));ep.topMargin=dp(18);l.addView(exit,ep);
        off.setOnClickListener(v->send(LatencyService.CPU_OFF));boost.setOnClickListener(v->send(LatencyService.CPU_BOOST));
        govDefault.setOnClickListener(v->send(LatencyService.GOV_DEFAULT));govPerformance.setOnClickListener(v->send(LatencyService.GOV_PERFORMANCE));
        refAuto.setOnClickListener(v->send(LatencyService.REF_AUTO));ref60.setOnClickListener(v->send(LatencyService.REF_60));ref90.setOnClickListener(v->send(LatencyService.REF_90));
        exit.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle("Close notification & restore defaults?")
            .setMessage("Restore CPU settings and Auto refresh, then stop the background service and remove the notification? Always use this before uninstalling the app.")
            .setNegativeButton("Cancel",null).setPositiveButton("Restore & Close",(d,w)->send(LatencyService.EXIT)).show());
        setContentView(scroll);render(null);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},101);
        else send(LatencyService.INIT);
    }
    @Override public void onRequestPermissionsResult(int req,String[] p,int[] grants){super.onRequestPermissionsResult(req,p,grants);
        if(req==101 && grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED)send(LatencyService.INIT);
        else if(req==101)status.setText("Enable notifications to access controls from Moonlight.");
    }
    @Override protected void onStart(){super.onStart();IntentFilter f=new IntentFilter(LatencyService.STATUS);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);
        send(LatencyService.REFRESH_STATUS);
    }
    @Override protected void onStop(){unregisterReceiver(receiver);super.onStop();}
}
