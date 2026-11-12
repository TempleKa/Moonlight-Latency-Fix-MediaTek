package com.example.moonlightlatencyfix;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.RadioButton;

public class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(18, 18, 22);
    private static final int FOREGROUND = Color.rgb(242, 242, 246);
    private static final int MUTED = Color.rgb(170, 170, 180);
    private static final int GREEN = Color.rgb(30, 150, 82);
    private static final int RED = Color.rgb(190, 55, 65);
    private static final int DISABLED = Color.rgb(75, 75, 82);
    private static final int NOTIFICATION_PERMISSION_REQUEST = 101;
    private boolean pendingStart = false;
    private TextView status;
    private Button start;
    private Button stop;
    private RadioGroup modes;
    private RadioButton normalMode;
    private RadioButton boostMode;
    private RadioButton computeMode;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            boolean running = intent.getBooleanExtra(LatencyService.EXTRA_RUNNING, false);
            if (!running) {
                String selected = intent.getStringExtra(LatencyService.EXTRA_MODE);
                if (selected != null) modes.check(LatencyService.MODE_COMPUTE.equals(selected)
                    ? computeMode.getId() : LatencyService.MODE_BOOST.equals(selected)
                    ? boostMode.getId() : normalMode.getId());
            }
            renderState(running, intent.getStringExtra(LatencyService.EXTRA_MESSAGE));
        }
    };

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable background(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(12));
        return shape;
    }

    private Button makeButton(String text, LinearLayout layout) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(54));
        params.topMargin = dp(12);
        layout.addView(button, params);
        return button;
    }

    private void renderState(boolean running, String message) {
        start.setEnabled(!running);
        stop.setEnabled(running);
        modes.setEnabled(!running);
        normalMode.setEnabled(!running);
        boostMode.setEnabled(!running);
        computeMode.setEnabled(!running);
        start.setBackground(background(running ? DISABLED : GREEN));
        stop.setBackground(background(running ? RED : DISABLED));
        start.setAlpha(running ? 0.6f : 1f);
        stop.setAlpha(running ? 1f : 0.6f);
        status.setText(message != null && !message.isEmpty() ? message : (running ? "Optimization running" : "Optimization stopped"));
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(BACKGROUND);
        layout.setPadding(dp(24), dp(36), dp(24), dp(24));

        TextView heading = new TextView(this);
        heading.setText("Moonlight Latency Fix");
        heading.setTextColor(FOREGROUND);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextSize(24);
        layout.addView(heading);

        TextView description = new TextView(this);
        description.setText("Keeps CPU 0 (Cortex-A55) active using a selectable workload. No root required.\n\nChoose a mode before starting optimization.");
        description.setTextColor(MUTED);
        description.setTextSize(16);
        description.setPadding(0, dp(24), 0, dp(20));
        layout.addView(description);

        status = new TextView(this);
        status.setTextColor(FOREGROUND);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 0, 0, dp(12));
        layout.addView(status);

        modes = new RadioGroup(this);
        modes.setOrientation(RadioGroup.VERTICAL);
        normalMode = new RadioButton(this);
        normalMode.setText("Normal · 10 ms (recommended)");
        normalMode.setTextColor(FOREGROUND);
        normalMode.setTextSize(16);
        normalMode.setId(android.view.View.generateViewId());
        boostMode = new RadioButton(this);
        boostMode.setText("Boost · 1 ms (experimental)");
        boostMode.setTextColor(FOREGROUND);
        boostMode.setTextSize(16);
        boostMode.setId(android.view.View.generateViewId());
        computeMode = new RadioButton(this);
        computeMode.setText("Compute Boost · 1,000 iterations + 10 ms (experimental)");
        computeMode.setTextColor(FOREGROUND);
        computeMode.setTextSize(16);
        computeMode.setId(android.view.View.generateViewId());
        modes.addView(normalMode);
        modes.addView(boostMode);
        modes.addView(computeMode);
        modes.check(normalMode.getId());
        layout.addView(modes);

        start = makeButton("Start optimization", layout);
        stop = makeButton("Stop optimization", layout);

        start.setOnClickListener(v -> {
            if (!hasNotificationPermission()) {
                pendingStart = true;
                requestNotificationPermission();
                return;
            }
            launchOptimization();
        });
        stop.setOnClickListener(v -> {
            try {
                startService(new Intent(this, LatencyService.class).setAction(LatencyService.STOP));
                renderState(false, "Optimization stopped");
            } catch (Exception e) {
                renderState(true, "Stop failed: " + e.getMessage());
            }
        });

        TextView foot = new TextView(this);
        foot.setText("Notification controls: Mode cycles through Normal, Boost and Compute Boost when stopped. Start/Stop toggles optimization. Exit removes the notification. Experimental modes may increase CPU use, heat, and battery drain.");
        foot.setTextColor(MUTED);
        foot.setTextSize(13);
        foot.setPadding(0, dp(28), 0, 0);
        layout.addView(foot);

        setContentView(layout);
        renderState(LatencyService.isRunning(), null);
        // Request permission first; create the notification only after the result.
        // This avoids racing the initial permission dialog with the first notification.
        if (hasNotificationPermission()) initializeNotification();
        else requestNotificationPermission();
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < 33
            || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void initializeNotification() {
        try {
            startForegroundService(new Intent(this, LatencyService.class)
                .setAction(LatencyService.INIT));
        } catch (Exception e) {
            renderState(LatencyService.isRunning(),
                "Notification initialization failed: " + e.getMessage());
        }
    }

    private void launchOptimization() {
        try {
            String selectedMode = computeMode.isChecked() ? LatencyService.MODE_COMPUTE
                : boostMode.isChecked() ? LatencyService.MODE_BOOST : LatencyService.MODE_NORMAL;
            startForegroundService(new Intent(this, LatencyService.class)
                .setAction(LatencyService.START)
                .putExtra(LatencyService.EXTRA_MODE, selectedMode));
            renderState(true, "Starting optimization...");
        } catch (Exception e) {
            renderState(false, "Start failed: " + e.getMessage());
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != NOTIFICATION_PERMISSION_REQUEST) return;
        boolean granted = grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            // Initialize after Android closes the permission dialog.
            if (pendingStart) launchOptimization();
            else initializeNotification();
        } else {
            renderState(LatencyService.isRunning(),
                "Allow notifications in Android Settings to use notification controls.");
        }
        pendingStart = false;
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(LatencyService.STATUS);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, filter);
        {
            String active = LatencyService.getActiveMode();
            modes.check(LatencyService.MODE_COMPUTE.equals(active) ? computeMode.getId()
                : LatencyService.MODE_BOOST.equals(active) ? boostMode.getId() : normalMode.getId());
        }
        renderState(LatencyService.isRunning(), null);
    }
    @Override protected void onStop() {
        unregisterReceiver(receiver);
        super.onStop();
    }
}
