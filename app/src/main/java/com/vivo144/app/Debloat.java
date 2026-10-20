package com.vivo144.app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Per-user removal of CN preinstalls through Shizuku (shell uid).
 *
 * Order: `pm uninstall -k --user 0` (reversible with install-existing), and if
 * vivo refuses with DELETE_FAILED_USER_RESTRICTED, `pm disable-user`. There is
 * deliberately no raw `service call package <n>`: the code differs per build
 * and a wrong number calls a different PackageManager method; disable-user
 * reaches the same setApplicationEnabledSetting with the same shell rights.
 */
final class Debloat {
    static final class App {
        final String pkg, name, desc;
        App(String pkg, String name, String desc) { this.pkg = pkg; this.name = name; this.desc = desc; }
    }

    /** Never touched, whatever is typed. */
    static final Set<String> PROTECTED = new HashSet<>(Arrays.asList(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.providers.settings",
        "com.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.bbk.launcher2",
        "com.vivo.gamewatch",   // Ultra Game Mode / chip de display
        "com.vivo.pem",         // energia e estabilidade
        "com.vivo.sps",
        "com.vivo.abe",
        "com.vivo.daemonService",
        "com.vivo.rms",
        "com.vivo.systemuiplugin",
        "com.vivo.upslide",     // central de controle
        "com.vivo.permissionmanager",
        "moe.shizuku.privileged.api",
        "com.vivo144.app"
    ));

    static final App[] CN_LIST = {
        new App("com.xtc.originwidget", "Origin Widgets extras", "Widgets chineses"),
        new App("com.vivo.browser.novel.widget", "Widget de novels", "Livros/novels chineses"),
        new App("com.unionpay.tsmservice", "UnionPay TSM", "NFC de bancos chineses"),
        new App("com.vivo.familycare.widget", "FamilyCare widget", "Cuidado familiar (CN)"),
        new App("com.sina.news.vivo.widget", "Sina News widget", "Notícias Sina"),
        new App("com.baidu.carlife.vivo", "Baidu CarLife", "Espelhamento automotivo chinês"),
        new App("com.tencent.mobileqq", "Tencent QQ", "Mensageiro chinês"),
        new App("com.kugou.android", "KuGou Music", "Música chinesa"),
        new App("com.vivo.hiboard", "Jovi / HiBoard", "Tela à esquerda da inicial"),
        new App("com.baidu.input_vivo", "Teclado Baidu", "Instale e ative outro teclado antes!"),
        new App("com.vivo.browser", "Navegador vivo CN", "Navegador com feeds chineses"),
        new App("com.vivo.magazine", "Revista da tela de bloqueio", "Papéis de parede rotativos"),
        new App("com.vivo.wallet", "vivo Wallet CN", "Carteira só para a China"),
    };

    enum State { INSTALLED, DISABLED, REMOVED, ABSENT }

    /** pkg -> state, with three `pm list` calls total. */
    static java.util.Map<String, State> states() {
        java.util.Map<String, State> m = new java.util.HashMap<>();
        Set<String> all = list("pm list packages -u --user 0");     // incl. removed-for-user
        Set<String> inst = list("pm list packages --user 0");
        Set<String> dis = list("pm list packages -d --user 0");
        for (App a : CN_LIST) {
            State s;
            if (dis.contains(a.pkg)) s = State.DISABLED;
            else if (inst.contains(a.pkg)) s = State.INSTALLED;
            else if (all.contains(a.pkg)) s = State.REMOVED;
            else s = State.ABSENT;
            m.put(a.pkg, s);
        }
        return m;
    }

    private static Set<String> list(String cmd) {
        Set<String> s = new HashSet<>();
        Shell.Result r = Shell.exec(cmd);
        for (String line : r.out.split("\n")) {
            line = line.trim();
            if (line.startsWith("package:")) s.add(line.substring(8));
        }
        return s;
    }

    static boolean validName(String pkg) {
        return pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+") || pkg.equals("android");
    }

    /** Returns a reason to refuse, or null if it is OK to touch this package. */
    static String guard(String pkg) {
        if (!validName(pkg)) return "Nome de pacote inválido: " + pkg;
        if (PROTECTED.contains(pkg)) return "Bloqueado: " + pkg + " é essencial para o sistema.";
        Shell.Result ime = Shell.exec("settings get secure default_input_method");
        if (ime.out.startsWith(pkg + "/")) {
            return "Bloqueado: " + pkg + " é o teclado atual. Instale/ative outro teclado e selecione-o antes "
                + "(sem teclado você não consegue digitar o PIN da tela de bloqueio).";
        }
        Shell.Result home = Shell.exec("cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1");
        if (home.out.startsWith(pkg + "/")) return "Bloqueado: " + pkg + " é o launcher atual.";
        return null;
    }

    static String remove(String pkg) {
        String no = guard(pkg);
        if (no != null) return no;
        Shell.Result u = Shell.exec("pm uninstall -k --user 0 " + Shell.q(pkg));
        if (u.all().contains("Success")) return pkg + ": removido para o usuário 0 (reversível).";
        Shell.Result d = Shell.exec("pm disable-user --user 0 " + Shell.q(pkg));
        if (d.all().contains("disabled")) {
            return pkg + ": a vivo bloqueou a desinstalação (" + shortErr(u) + "), então foi DESATIVADO.";
        }
        return pkg + ": falhou.\nuninstall: " + u.all() + "\ndisable-user: " + d.all();
    }

    static String restore(String pkg) {
        if (!validName(pkg)) return "Nome de pacote inválido: " + pkg;
        Shell.Result i = Shell.exec("cmd package install-existing --user 0 " + Shell.q(pkg));
        Shell.Result e = Shell.exec("pm enable --user 0 " + Shell.q(pkg));
        return pkg + ":\n" + i.all() + "\n" + e.all();
    }

    private static String shortErr(Shell.Result r) {
        String s = r.all();
        int a = s.indexOf('['), b = s.indexOf(']');
        return a >= 0 && b > a ? s.substring(a + 1, b) : s;
    }
}
