package com.vivo144.app;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick Settings tile: tap to toggle, like typing 144on / 144off. */
public class RefreshTile extends TileService {
    private volatile boolean busy;

    static void update(Context c) {
        TileService.requestListeningState(c, new ComponentName(c, RefreshTile.class));
    }

    @Override
    public void onStartListening() {
        render(Prefs.on(this), null);
    }

    @Override
    public void onClick() {
        if (busy) return;
        busy = true;
        final boolean turnOn = !Prefs.on(this);
        final int fps = Prefs.fps(this);
        render(turnOn, "…");
        new Thread(() -> {
            try {
                float rate = turnOn ? Rms.set(fps) : Rms.release();
                Prefs.on(this, turnOn);
                render(turnOn, Rms.fmt(rate) + " Hz");
            } catch (Throwable e) {
                render(Prefs.on(this), "erro");
                openApp();
            } finally {
                busy = false;
            }
        }).start();
    }

    private void render(boolean on, String sub) {
        Tile t = getQsTile();
        if (t == null) return;
        t.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.setLabel(Prefs.fps(this) + " Hz");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            t.setSubtitle(sub != null ? sub : (on ? "Ligado" : "Desligado"));
        }
        t.updateTile();
    }

    /** Shizuku missing or no permission: the app explains what to do. */
    @SuppressWarnings("deprecation")
    private void openApp() {
        Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE));
        } else {
            startActivityAndCollapse(i);
        }
    }
}
