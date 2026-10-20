package com.vivo144.app;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("vivo144", Context.MODE_PRIVATE);
    }

    static int fps(Context c) { return p(c).getInt("fps", 144); }
    static void fps(Context c, int v) { p(c).edit().putInt("fps", v).apply(); }

    static boolean on(Context c) { return p(c).getBoolean("on", false); }
    static void on(Context c, boolean v) { p(c).edit().putBoolean("on", v).apply(); }

    /** Votes live in system_server memory and vanish on reboot; re-vote at boot. */
    static boolean reapply(Context c) { return p(c).getBoolean("reapply", true); }
    static void reapply(Context c, boolean v) { p(c).edit().putBoolean("reapply", v).apply(); }
}
