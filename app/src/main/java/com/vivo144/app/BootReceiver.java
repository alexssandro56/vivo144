package com.vivo144.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * The rms vote is held in memory and cleared on reboot. If 144 Hz was on and
 * "reapply" is checked, vote again after boot. This works in direct mode; in
 * Shizuku mode it only works if Shizuku is already running (e.g. it was
 * started with its own boot option), otherwise it silently does nothing and
 * the tile is turned back off.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        final Context c = context.getApplicationContext();
        if (!Prefs.on(c)) return;
        if (!Prefs.reapply(c)) {
            Prefs.on(c, false); // the vote did not survive the reboot
            RefreshTile.update(c);
            return;
        }
        final PendingResult pr = goAsync();
        new Thread(() -> {
            try {
                // Give rms a moment to come up after boot.
                Thread.sleep(8000);
                Rms.set(Prefs.fps(c));
            } catch (Throwable t) {
                Prefs.on(c, false);
            } finally {
                RefreshTile.update(c);
                pr.finish();
            }
        }).start();
    }
}
