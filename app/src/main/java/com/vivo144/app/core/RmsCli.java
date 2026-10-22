package com.vivo144.app.core;

import android.os.IBinder;
import android.os.Parcel;

/**
 * Entry point run as the shell user, through Shizuku:
 *   CLASSPATH=<this apk> app_process / com.vivo144.app.core.RmsCli <service> set 144|release|get
 * Shell may look rms up even where SELinux hides it from apps. Same raw
 * Parcel layout as Rms.kt (no interface token, caller 0 = system_server).
 * Must not touch any app Context: there is no Application here.
 */
public final class RmsCli {
    private static final int TX_RELEASE = 24, TX_GET = 26, TX_ACQUIRE_V2 = 31;
    private static final long HANDLE = 900001L;

    public static void main(String[] args) throws Exception {
        String name = args.length > 0 ? args[0] : "rms";
        Class<?> sm = Class.forName("android.os.ServiceManager");
        IBinder b = (IBinder) sm.getMethod("getService", String.class).invoke(null, name);
        if (b == null) b = (IBinder) sm.getMethod("checkService", String.class).invoke(null, name);
        if (b == null) {
            System.out.println("error=" + name + " not found");
            System.exit(2);
        }
        String cmd = args.length > 1 ? args[1] : "get";
        if (cmd.equals("set")) {
            int fps = Integer.parseInt(args[2]);
            Parcel d = Parcel.obtain();
            d.writeLong(HANDLE);
            d.writeString("FixRate");
            d.writeString("Vivo144");
            d.writeInt(-1);
            d.writeInt(0);
            d.writeInt(fps);
            d.writeInt(0);
            d.writeInt(0);
            d.writeInt(0);
            d.writeInt(100);
            d.writeInt(0);
            d.writeInt(0);
            d.writeInt(0);
            d.writeInt(15);
            d.writeInt(0);
            d.writeInt(0);
            b.transact(TX_ACQUIRE_V2, d, null, IBinder.FLAG_ONEWAY);
            d.recycle();
            Thread.sleep(300);
        } else if (cmd.equals("release")) {
            Parcel d = Parcel.obtain();
            d.writeInt(0);
            d.writeLong(HANDLE);
            b.transact(TX_RELEASE, d, null, IBinder.FLAG_ONEWAY);
            d.recycle();
            Thread.sleep(300);
        }
        Parcel d = Parcel.obtain(), r = Parcel.obtain();
        b.transact(TX_GET, d, r, 0);
        r.readException();
        System.out.println("rate=" + r.readFloat());
        d.recycle();
        r.recycle();
    }
}
