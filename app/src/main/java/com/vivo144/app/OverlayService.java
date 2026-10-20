package com.vivo144.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Small draggable overlay that shows the real panel rate (rms tx 26, one
 * binder call, no process spawned) and, every few seconds, the "reason".
 * Tap it to vote again. It does NOT re-vote on its own: when a game sets its
 * own display mode (reason "reqFps:..."), the game's request wins anyway.
 */
public class OverlayService extends Service {
    static volatile boolean running;

    private WindowManager wm;
    private TextView view;
    private HandlerThread worker;
    private Handler bg;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private int tick;
    private String lastReason = "…";

    static void start(Context c) {
        Intent i = new Intent(c, OverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }

    static void stop(Context c) {
        c.stopService(new Intent(c, OverlayService.class));
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();
        goForeground();
        running = true;

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 30;
        lp.y = 120;

        view = new TextView(this);
        view.setText("… Hz");
        view.setTextSize(11f);
        view.setTextColor(Color.WHITE);
        view.setBackgroundColor(0xCC121212);
        view.setPadding(20, 10, 20, 10);
        view.setOnTouchListener(new android.view.View.OnTouchListener() {
            int x0, y0;
            float tx, ty;

            @Override
            public boolean onTouch(android.view.View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x0 = lp.x; y0 = lp.y; tx = e.getRawX(); ty = e.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        lp.x = x0 + (int) (e.getRawX() - tx);
                        lp.y = y0 + (int) (e.getRawY() - ty);
                        wm.updateViewLayout(view, lp);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (Math.abs(e.getRawX() - tx) < 12 && Math.abs(e.getRawY() - ty) < 12) {
                            bg.post(() -> {
                                try { Rms.set(Prefs.fps(OverlayService.this)); Prefs.on(OverlayService.this, true); }
                                catch (Throwable ignored) { }
                            });
                        }
                        return true;
                }
                return false;
            }
        });
        wm.addView(view, lp);

        worker = new HandlerThread("overlay");
        worker.start();
        bg = new Handler(worker.getLooper());
        bg.post(poll);
    }

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            String rate;
            boolean target = false;
            try {
                float r = Rms.getRate();
                rate = Rms.fmt(r);
                target = Math.round(r) >= Prefs.fps(OverlayService.this);
            } catch (Throwable t) {
                rate = "?";
            }
            // The reason may need a `settings` call via Shizuku; do it less often.
            if (tick++ % 3 == 0) lastReason = Rms.reason(OverlayService.this);
            final String text = rate + " Hz | " + lastReason;
            final int color = target ? 0xFF00FF88 : 0xFFFFAA00;
            ui.post(() -> {
                if (view != null) { view.setText(text); view.setTextColor(color); }
            });
            bg.postDelayed(this, 2000);
        }
    };

    private void goForeground() {
        String ch = "overlay";
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(ch, "Monitor de Hz", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0,
            new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, ch)
            .setContentTitle("Monitor de Hz ativo")
            .setContentText("Toque no indicador flutuante para votar de novo")
            .setSmallIcon(R.drawable.ic_tile)
            .setContentIntent(open)
            .setOngoing(true)
            .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(144, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(144, n);
        }
    }

    @Override
    public void onDestroy() {
        running = false;
        if (bg != null) bg.removeCallbacksAndMessages(null);
        if (worker != null) worker.quitSafely();
        if (view != null) {
            try { wm.removeView(view); } catch (Throwable ignored) { }
            view = null;
        }
        super.onDestroy();
    }
}
