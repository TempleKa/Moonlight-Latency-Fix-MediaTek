package com.example.moonlightlatencyfixroot;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.RemoteViews;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LatencyService extends Service {
    static final String REFRESH_STATUS="REFRESH_STATUS";
    static final String INIT="INIT", CPU_OFF="CPU_OFF", CPU_BOOST="CPU_BOOST";
    static final String GOV_DEFAULT="GOV_DEFAULT", GOV_PERFORMANCE="GOV_PERFORMANCE";
    static final String REF_AUTO="REF_AUTO", REF_60="REF_60", REF_90="REF_90", EXIT="EXIT";
    static final String STATUS="com.example.moonlightlatencyfixroot.STATUS";
    private static final String CHANNEL="root_controls";
    private static final int ID=2001;
    private final ExecutorService queue=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private RootControl root;
    private android.content.SharedPreferences state;
    private volatile String message="Ready";
    private volatile boolean closing=false;
    private volatile int cpuMode=2, governorMode=2, refreshMode=3;
    private volatile String littleValue="", bigValue="";
    private boolean initialized=false;
    @Override public void onCreate() {
        super.onCreate();
        root=new RootControl(this);
        state=getSharedPreferences("ui_state",MODE_PRIVATE);
        NotificationChannel ch=new NotificationChannel(CHANNEL,"Moonlight ROOT controls",NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }
    private PendingIntent action(String a, int code) {
        return PendingIntent.getService(this,code,new Intent(this,LatencyService.class).setAction(a),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private RemoteViews panel() {
        RemoteViews v=new RemoteViews(getPackageName(),R.layout.notification_controls);
        v.setTextViewText(R.id.cpu_off,"OFF"+(cpuMode==0?" ✓":""));
        v.setTextViewText(R.id.cpu_boost,"BOOST"+(cpuMode==1?" ✓":""));
        v.setTextViewText(R.id.ref_auto,"AUTO"+(refreshMode==0?" ✓":""));
        v.setTextViewText(R.id.ref_60,"60 Hz"+(refreshMode==1?" ✓":""));
        v.setTextViewText(R.id.ref_90,"90 Hz"+(refreshMode==2?" ✓":""));
        int green=0xFF1E9652, gray=0xFF4B4B52;
        v.setInt(R.id.cpu_off,"setBackgroundColor",cpuMode==0?green:gray);
        v.setInt(R.id.cpu_boost,"setBackgroundColor",cpuMode==1?green:gray);
        v.setInt(R.id.ref_auto,"setBackgroundColor",refreshMode==0?green:gray);
        v.setInt(R.id.ref_60,"setBackgroundColor",refreshMode==1?green:gray);
        v.setInt(R.id.ref_90,"setBackgroundColor",refreshMode==2?green:gray);
        v.setOnClickPendingIntent(R.id.cpu_off,action(CPU_OFF,11));
        v.setOnClickPendingIntent(R.id.cpu_boost,action(CPU_BOOST,12));
        v.setOnClickPendingIntent(R.id.ref_auto,action(REF_AUTO,13));
        v.setOnClickPendingIntent(R.id.ref_60,action(REF_60,14));
        v.setOnClickPendingIntent(R.id.ref_90,action(REF_90,15));
        return v;
    }
    private Notification notification() {
        PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String summary=(cpuMode==2?"CPU CUSTOM":cpuMode==1?"Boost ON":"Boost OFF")+" · "+
            (refreshMode==0?"AUTO":refreshMode==1?"60 Hz":refreshMode==2?"90 Hz":"Refresh custom")+" · "+
            (governorMode==1?"Performance":governorMode==0?"Default":"Governor custom");
        return new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("MoonlightLatencyFix-ROOT")
            .setContentText(summary+" · "+message)
            .setContentIntent(open)
            .setOngoing(true).setOnlyAlertOnce(true)
            .setCustomContentView(panel()).setCustomBigContentView(panel())
            .setStyle(new Notification.DecoratedCustomViewStyle())
            .build();
    }
    private void update() {
        getSystemService(NotificationManager.class).notify(ID,notification());
        Intent i=new Intent(STATUS).setPackage(getPackageName());
        i.putExtra("message",message);
        i.putExtra("cpuMode",cpuMode);
        i.putExtra("governorMode",governorMode);
        i.putExtra("refreshMode",refreshMode);
        i.putExtra("littleValue",littleValue);
        i.putExtra("bigValue",bigValue);
        sendBroadcast(i);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        String a=intent==null?INIT:intent.getAction();
        if (a==null) a=INIT;
        if (closing) return START_NOT_STICKY;
        startForeground(ID,notification());
        final String command=a;
        queue.execute(()->{
            try {
                // If a prior process died while settings were modified, restore first.
                if (command.equals(INIT) && !initialized && state.getBoolean("unclean",false)) {
                    root.restoreAll();
                    initialized=true;
                    state.edit().putBoolean("unclean",false).putBoolean("boost",false)
                        .putBoolean("performance",false).putString("refresh","auto").commit();
                    message="Recovered defaults after interrupted session";
                } else if (command.equals(CPU_OFF) || command.equals(CPU_BOOST)) {
                    boolean on=command.equals(CPU_BOOST);
                    root.setBoost(on);
                    state.edit().putBoolean("boost",on).putBoolean("unclean",true).commit();
                    message=on?"CPU Boost enabled":"CPU Boost disabled";
                } else if (command.equals(GOV_DEFAULT) || command.equals(GOV_PERFORMANCE)) {
                    boolean on=command.equals(GOV_PERFORMANCE);
                    root.setGovernor(on);
                    if (state.getBoolean("boost",false)) root.setBoost(true);
                    state.edit().putBoolean("performance",on).putBoolean("unclean",true).commit();
                    message=on?"Performance governor enabled":"Default governor restored";
                } else if (command.equals(REF_AUTO)||command.equals(REF_60)||command.equals(REF_90)) {
                    String v=command.equals(REF_AUTO)?"auto":command.equals(REF_60)?"60":"90";
                    root.setRefresh(v);
                    state.edit().putString("refresh",v).putBoolean("unclean",true).commit();
                    message="Refresh: "+v;
                } else if (command.equals(EXIT)) {
                    root.restoreAll();
                    state.edit().clear().commit();
                    message="Defaults restored";
                    closing=true;
                } else if (command.equals(INIT)) {
                    root.snapshot();
                    initialized=true;
                    message="Root ready";
                }
            } catch(Exception e) { message="ERROR: "+e.getMessage(); }
            // Always synchronize UI and notification with the actual CPU policy,
            // including after verification errors or partially applied commands.
            try {
                String[] minimums=root.cpuMinimums();
                littleValue=minimums[0];bigValue=minimums[1];
                cpuMode=root.cpuMode();
                if (cpuMode!=2) state.edit().putBoolean("boost",cpuMode==1).commit();
            } catch(Exception e) {
                cpuMode=2;littleValue="";bigValue="";
                message += " | CPU read: " + e.getMessage();
            }
            try {
                governorMode=root.governorMode();
                if (governorMode!=2) state.edit().putBoolean("performance",governorMode==1).commit();
            } catch(Exception e) {governorMode=2;message += " | Governor read: " + e.getMessage();}
            try {
                refreshMode=root.refreshMode();
                if (refreshMode!=3) state.edit().putString("refresh",refreshMode==0?"auto":refreshMode==1?"60":"90").commit();
            } catch(Exception e) {refreshMode=3;message += " | Refresh read: " + e.getMessage();}
            main.post(()->{
                if (closing) {
                    update();
                    stopForeground(STOP_FOREGROUND_REMOVE);
                    stopSelf();
                } else update();
            });
        });
        return START_STICKY;
    }
    @Override public void onDestroy() {
        // Best effort: Android Force Stop / SIGKILL does not call onDestroy.
        // The next INIT will detect an unclean session and restore defaults.
        queue.shutdown();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i) {return null;}
}
