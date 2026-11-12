package com.example.moonlightlatencyfix;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.IOException;

public class LatencyService extends Service {
    public static final String INIT = "com.example.moonlightlatencyfix.INIT";
    public static final String CYCLE_MODE = "com.example.moonlightlatencyfix.CYCLE_MODE";
    public static final String TOGGLE = "com.example.moonlightlatencyfix.TOGGLE";
    public static final String EXIT = "com.example.moonlightlatencyfix.EXIT";
    public static final String START = "com.example.moonlightlatencyfix.START";
    public static final String STOP = "com.example.moonlightlatencyfix.STOP";
    public static final String STATUS = "com.example.moonlightlatencyfix.STATUS";
    public static final String EXTRA_RUNNING = "running";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_MODE = "mode";
    public static final String MODE_NORMAL = "normal";
    public static final String MODE_BOOST = "boost";
    public static final String MODE_COMPUTE = "compute";
    private static volatile String activeMode = MODE_NORMAL;
    public static String getActiveMode() { return activeMode; }
    private static final String CHANNEL = "latency_status";
    private static final int NOTIFICATION_ID = 1001;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static volatile boolean running = false;
    public static boolean isRunning() { return running; }
    private Process worker;
    private int generation = 0;
    private String message = "Optimization stopped";

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Optimization status", NotificationManager.IMPORTANCE_LOW));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? INIT : intent.getAction();
        if (EXIT.equals(action)) {
            stopWorker();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        // Keep an idle foreground notification so Start is available from the shade.
        startForeground(NOTIFICATION_ID, notification(message));
        if (CYCLE_MODE.equals(action)) {
            if (!running) {
                activeMode = MODE_NORMAL.equals(activeMode) ? MODE_BOOST
                    : MODE_BOOST.equals(activeMode) ? MODE_COMPUTE : MODE_NORMAL;
                message = "Ready: " + modeLabel(activeMode);
            }
        } else if (STOP.equals(action) || (TOGGLE.equals(action) && running)) {
            stopWorker();
        } else if (START.equals(action) || TOGGLE.equals(action)) {
            if (worker == null || !worker.isAlive()) {
                if (START.equals(action) && intent.hasExtra(EXTRA_MODE)) {
                    String requested = intent.getStringExtra(EXTRA_MODE);
                    activeMode = MODE_COMPUTE.equals(requested) ? MODE_COMPUTE
                        : MODE_BOOST.equals(requested) ? MODE_BOOST : MODE_NORMAL;
                }
                startWorker();
            }
        }
        updateNotification();
        broadcastStatus();
        return START_NOT_STICKY;
    }

    private String modeLabel(String mode) {
        return MODE_COMPUTE.equals(mode) ? "Compute Boost (1,000 + 10 ms)"
            : MODE_BOOST.equals(mode) ? "Boost (1 ms)" : "Normal (10 ms)";
    }

    private void startWorker() {
        final int currentGeneration = ++generation;
        try {
            // Same shell workload as the successful ADB experiment.
            // The app can only control the affinity of its own child process.
            String loop;
            if (MODE_COMPUTE.equals(activeMode)) {
                loop = "while true; do i=0; while [ $i -lt 1000 ]; do i=$((i+1)); done; sleep 0.01; done";
            } else {
                String interval = MODE_BOOST.equals(activeMode) ? "0.001" : "0.01";
                loop = "while true; do sleep " + interval + "; done";
            }
            worker = new ProcessBuilder("/system/bin/sh", "-c",
                "exec taskset 1 sh -c '" + loop + "'")
                .redirectErrorStream(true).start();
            running = true;
            message = MODE_COMPUTE.equals(activeMode)
                ? "Compute Boost running: CPU 0, 1,000 iterations + 10 ms"
                : MODE_BOOST.equals(activeMode)
                    ? "Boost running: CPU 0, 1 ms wake-up loop"
                    : "Normal running: CPU 0, 10 ms wake-up loop";
            updateNotification();
            final Process observed = worker;
            new Thread(() -> {
                String output = "";
                int code = -1;
                try {
                    byte[] bytes = new byte[2048];
                    int count = observed.getInputStream().read(bytes);
                    if (count > 0) output = new String(bytes, 0, count).trim();
                    code = observed.waitFor();
                } catch (Exception e) { output = e.toString(); }
                final String result = output;
                final int exitCode = code;
                handler.post(() -> {
                    if (generation != currentGeneration) return;
                    worker = null;
                    running = false;
                    message = "Worker exited (code " + exitCode + "): " + result;
                    Log.e("MoonlightLatencyFix", message);
                    updateNotification();
                    broadcastStatus();
                    // Keep notification available for restart.
                });
            }, "WorkerMonitor").start();
        } catch (IOException e) {
            worker = null;
            running = false;
            message = "Start error: " + e.getMessage();
            updateNotification();
            broadcastStatus();
            // Keep notification available for restart.
        }
    }

    private void stopWorker() {
        ++generation;
        running = false;
        if (worker != null) {
            worker.destroy();
            worker = null;
        }
        message = "Optimization stopped";
        broadcastStatus();
    }

    private PendingIntent serviceAction(String action, int requestCode) {
        Intent intent = new Intent(this, LatencyService.class).setAction(action);
        return PendingIntent.getService(this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification notification(String status) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openAction = PendingIntent.getActivity(this, 2, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Moonlight Latency Fix · " + modeLabel(activeMode))
            .setContentText(status)
            .setStyle(new Notification.BigTextStyle().bigText(status
                + "\\nMode: tap Mode to cycle through all 3 options while stopped."))
            .setContentIntent(openAction)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_manage,
                "Mode", serviceAction(CYCLE_MODE, 3)).build())
            .addAction(new Notification.Action.Builder(
                running ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                running ? "Stop" : "Start", serviceAction(TOGGLE, 4)).build())
            .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel,
                "Exit", serviceAction(EXIT, 5)).build())
            .build();
    }

    private void updateNotification() {
        getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, notification(message));
    }

    private void broadcastStatus() {
        Intent status = new Intent(STATUS).setPackage(getPackageName());
        status.putExtra(EXTRA_RUNNING, worker != null && worker.isAlive());
        status.putExtra(EXTRA_MESSAGE, message);
        status.putExtra(EXTRA_MODE, activeMode);
        sendBroadcast(status);
    }

    @Override public void onDestroy() { stopWorker(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
