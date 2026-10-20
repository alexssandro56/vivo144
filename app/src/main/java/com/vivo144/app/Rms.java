package com.vivo144.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Parcel;
import android.provider.Settings;
import android.view.Display;
import android.view.WindowManager;

import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.SystemServiceHelper;

/**
 * Port of RmsVote.java (AndroPlus-org/vivo-HighRefresh-NoRoot, MIT) that runs
 * inside the app instead of through adb + app_process.
 *
 * vivo's "rms" binder takes raw Parcels with no interface token and checks no
 * uid, so any process that can reach it may vote. A normal app usually can't
 * look the service up (SELinux), so we try that first and otherwise go through
 * Shizuku, which forwards the transaction as the shell user like `adb shell`.
 */
final class Rms {
    static final int TX_RELEASE = 24, TX_GET = 26, TX_ACQUIRE_V2 = 31;
    static final int FLAG_ONEWAY = 1;
    // Same defaults as the script: caller 0 = system_server, so the vote
    // outlives us and stays until released or reboot.
    static final int CALLER = 0;
    static final long HANDLE = 900001;

    static final class NeedShizuku extends Exception {
        NeedShizuku(String msg) { super(msg); }
    }

    static final class NeedPermission extends Exception {
        NeedPermission() { super("Permissão do Shizuku não concedida"); }
    }

    private static IBinder cached;
    static String mode = "?";

    static synchronized IBinder binder() throws Exception {
        if (cached != null && cached.isBinderAlive()) return cached;
        cached = null;

        try {
            IBinder b = (IBinder) Class.forName("android.os.ServiceManager")
                .getMethod("checkService", String.class).invoke(null, "rms");
            if (b != null) {
                getRate(b);
                mode = "direto (sem Shizuku)";
                return cached = b;
            }
        } catch (Throwable ignored) {
        }

        if (!waitForShizuku(3000)) {
            throw new NeedShizuku("Shizuku não está rodando. Abra o app Shizuku e inicie-o por depuração sem fio.");
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            throw new NeedPermission();
        }
        IBinder raw = SystemServiceHelper.getSystemService("rms");
        if (raw == null) throw new IllegalStateException("Serviço rms não encontrado (este aparelho é vivo/iQOO?)");
        mode = "Shizuku";
        return cached = new ShizukuBinderWrapper(raw);
    }

    /** The Shizuku server hands us its binder shortly after our process starts. */
    static boolean waitForShizuku(long timeoutMs) {
        if (Shizuku.pingBinder()) return true;
        final CountDownLatch latch = new CountDownLatch(1);
        Shizuku.OnBinderReceivedListener l = latch::countDown;
        Shizuku.addBinderReceivedListenerSticky(l);
        try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        } finally {
            Shizuku.removeBinderReceivedListener(l);
        }
        return Shizuku.pingBinder();
    }

    static float getRate() throws Exception {
        return getRate(binder());
    }

    private static float getRate(IBinder b) throws Exception {
        Parcel d = Parcel.obtain(), r = Parcel.obtain();
        try {
            b.transact(TX_GET, d, r, 0);
            r.readException();
            return r.readFloat();
        } finally {
            d.recycle();
            r.recycle();
        }
    }

    /** Equivalent of `vivo-rms.sh set <fps>`. Returns the rate afterwards. */
    static float set(int fps) throws Exception {
        IBinder b = binder();
        Parcel d = Parcel.obtain();
        try {
            // Field order mirrors RefreshRateRequester.RmsManager.acquireRefreshRateV2.
            d.writeLong(HANDLE);
            d.writeString("FixRate"); // scene
            d.writeString("RmsVote"); // tag
            d.writeInt(-1);           // display: all
            d.writeInt(0);            // reqResolution
            d.writeInt(fps);
            d.writeInt(0);            // reqDFps
            d.writeInt(0);            // reqConfigBits
            d.writeInt(0);            // reqStates
            d.writeInt(100);          // prio
            d.writeInt(0);            // dur
            d.writeInt(CALLER);
            d.writeInt(0);            // client
            d.writeInt(15);           // flags
            d.writeInt(0);            // arg1
            d.writeInt(0);            // arg2
            b.transact(TX_ACQUIRE_V2, d, null, FLAG_ONEWAY);
        } finally {
            d.recycle();
        }
        Thread.sleep(500);
        return getRate(b);
    }

    /** Equivalent of `vivo-rms.sh release 0 900001`. Returns the rate afterwards. */
    static float release() throws Exception {
        IBinder b = binder();
        Parcel d = Parcel.obtain();
        try {
            d.writeInt(CALLER);
            d.writeLong(HANDLE);
            b.transact(TX_RELEASE, d, null, FLAG_ONEWAY);
        } finally {
            d.recycle();
        }
        Thread.sleep(500);
        return getRate(b);
    }

    /**
     * Who is driving the panel right now, e.g. "RmsVote|144", or
     * "reqFps:<activity>|<fps>" when the foreground app sets its own display
     * mode. In that second case the app's request wins over any vote, so
     * re-voting in a loop does not help.
     */
    static String reason(Context c) {
        final String key = "vivo_rms_active_request_reason";
        try {
            String s = Settings.Secure.getString(c.getContentResolver(), key);
            if (s != null && !s.isEmpty()) return s;
        } catch (Throwable ignored) {
            // Hidden keys are not readable by apps targeting Android 12+.
        }
        if (!Shell.ready()) return "? (precisa do Shizuku)";
        Shell.Result r = Shell.exec("settings get secure " + key);
        return r.ok() && !r.out.isEmpty() ? r.out : "?";
    }

    /** Refresh rates the panel advertises (public API, no privilege needed). */
    static String supportedRates(Context c) {
        try {
            WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
            Display d = wm.getDefaultDisplay();
            TreeSet<Integer> rates = new TreeSet<>();
            for (Display.Mode m : d.getSupportedModes()) rates.add(Math.round(m.getRefreshRate()));
            StringBuilder sb = new StringBuilder();
            for (int r : rates) sb.append(sb.length() == 0 ? "" : ", ").append(r);
            return sb.toString();
        } catch (Throwable t) {
            return "?";
        }
    }

    static String fmt(float rate) {
        return (rate == (int) rate) ? String.valueOf((int) rate) : String.valueOf(rate);
    }
}
