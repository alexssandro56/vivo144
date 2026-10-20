package com.vivo144.app;

import android.content.pm.PackageManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

/**
 * Runs a shell command as the shell user (uid 2000) through Shizuku.
 *
 * Shizuku.newProcess is private in API 13.x (the library wants you to use a
 * UserService), so it is reached by reflection. It is only used for short,
 * native commands (settings / pm / cmd), never for app_process in a loop.
 */
final class Shell {
    static final class Result {
        final int code;
        final String out, err;
        Result(int code, String out, String err) { this.code = code; this.out = out; this.err = err; }
        boolean ok() { return code == 0; }
        String all() { return (out + (err.isEmpty() ? "" : "\n" + err)).trim(); }
    }

    private static Method newProcess;

    static boolean ready() {
        try {
            return Rms.waitForShizuku(1500)
                && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    static Result exec(String cmd) {
        if (!ready()) return new Result(-1, "", "Shizuku não está rodando ou sem permissão");
        try {
            if (newProcess == null) {
                Method m = Shizuku.class.getDeclaredMethod(
                    "newProcess", String[].class, String[].class, String.class);
                m.setAccessible(true);
                newProcess = m;
            }
            final Process p = (Process) newProcess.invoke(
                null, new Object[]{new String[]{"sh", "-c", cmd}, null, null});
            // Read stderr on its own thread so a full pipe can't block us.
            final ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
            Thread errThread = new Thread(() -> copy(p.getErrorStream(), errBuf));
            errThread.start();
            ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
            copy(p.getInputStream(), outBuf);
            int code = p.waitFor();
            errThread.join(2000);
            return new Result(code, outBuf.toString().trim(), errBuf.toString().trim());
        } catch (Throwable t) {
            Throwable c = t.getCause() != null ? t.getCause() : t;
            return new Result(-1, "", "Erro no Shizuku: " + c);
        }
    }

    private static void copy(InputStream in, ByteArrayOutputStream out) {
        byte[] buf = new byte[4096];
        int n;
        try {
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (Exception ignored) {
        } finally {
            try { in.close(); } catch (Exception ignored) { }
        }
    }

    /** Single-quote a value for sh. */
    static String q(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}
