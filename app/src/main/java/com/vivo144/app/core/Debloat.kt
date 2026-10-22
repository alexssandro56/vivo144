package com.vivo144.app.core

/**
 * Per-user removal of CN preinstalls through Shizuku. `pm uninstall -k --user 0`
 * first (reversible with install-existing); when vivo answers
 * DELETE_FAILED_USER_RESTRICTED, `pm disable-user`. No raw `service call
 * package <n>`: the number changes per build and a wrong one calls another method.
 */
object Debloat {
    data class Bloat(val pkg: String, val name: String, val desc: String)
    enum class State { INSTALLED, DISABLED, REMOVED, ABSENT, UNKNOWN }

    private val PROTECTED = setOf(
        "android", "com.android.systemui", "com.android.settings", "com.android.phone",
        "com.android.providers.settings", "com.android.packageinstaller",
        "com.android.permissioncontroller", "com.bbk.launcher2",
        "com.vivo.gamewatch", "com.vivo.pem", "com.vivo.sps", "com.vivo.abe",
        "com.vivo.daemonService", "com.vivo.rms", "com.vivo.systemuiplugin",
        "com.vivo.upslide", "com.vivo.permissionmanager", "com.iqoo.secure",
        "moe.shizuku.privileged.api", "com.vivo144.app"
    )

    val LIST = listOf(
        Bloat("com.xtc.originwidget", "Origin Widgets extras", "Widgets chineses"),
        Bloat("com.vivo.browser.novel.widget", "Widget de novels", "Livros/novels chineses"),
        Bloat("com.unionpay.tsmservice", "UnionPay TSM", "NFC de bancos chineses"),
        Bloat("com.vivo.familycare.widget", "FamilyCare", "Cuidado familiar (CN)"),
        Bloat("com.sina.news.vivo.widget", "Sina News", "Notícias Sina"),
        Bloat("com.baidu.carlife.vivo", "Baidu CarLife", "Espelhamento automotivo chinês"),
        Bloat("com.tencent.mobileqq", "Tencent QQ", "Mensageiro chinês"),
        Bloat("com.kugou.android", "KuGou Music", "Música chinesa"),
        Bloat("com.vivo.hiboard", "Jovi / HiBoard", "Tela à esquerda da inicial"),
        Bloat("com.baidu.input_vivo", "Teclado Baidu", "Ative outro teclado antes"),
        Bloat("com.vivo.browser", "Navegador vivo CN", "Navegador com feeds chineses"),
        Bloat("com.vivo.magazine", "Revista da tela de bloqueio", "Papéis de parede rotativos"),
        Bloat("com.vivo.wallet", "vivo Wallet CN", "Carteira só para a China"),
    )

    fun states(): Map<String, State> {
        if (!Shell.ready()) return LIST.associate { it.pkg to State.UNKNOWN }
        val all = list("pm list packages -u --user 0")
        val inst = list("pm list packages --user 0")
        val dis = list("pm list packages -d --user 0")
        return LIST.associate {
            it.pkg to when (it.pkg) {
                in dis -> State.DISABLED
                in inst -> State.INSTALLED
                in all -> State.REMOVED
                else -> State.ABSENT
            }
        }
    }

    private fun list(cmd: String) = Shell.exec(cmd).out.lineSequence()
        .map { it.trim() }.filter { it.startsWith("package:") }.map { it.removePrefix("package:") }.toSet()

    private fun valid(pkg: String) = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+").matches(pkg)

    /** A reason to refuse, or null. */
    private fun guard(pkg: String): String? {
        if (!valid(pkg)) return "Nome de pacote inválido: $pkg"
        if (pkg in PROTECTED) return "Bloqueado: $pkg é essencial para o sistema."
        if (Shell.exec("settings get secure default_input_method").out.startsWith("$pkg/"))
            return "Bloqueado: $pkg é o teclado atual. Ative e selecione outro teclado antes (sem teclado não dá para digitar o PIN)."
        val home = Shell.exec(
            "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1"
        ).out
        if (home.startsWith("$pkg/")) return "Bloqueado: $pkg é o launcher atual."
        return null
    }

    fun remove(pkg: String): String {
        if (!Shell.ready()) return "Shizuku não está pronto."
        guard(pkg)?.let { return it }
        val u = Shell.exec("pm uninstall -k --user 0 ${Shell.q(pkg)}")
        if ("Success" in u.all) return "$pkg removido (dá para restaurar)."
        val d = Shell.exec("pm disable-user --user 0 ${Shell.q(pkg)}")
        if ("disabled" in d.all) return "$pkg desativado (a vivo bloqueou a remoção)."
        return "$pkg: falhou — ${u.all} / ${d.all}"
    }

    fun restore(pkg: String): String {
        if (!Shell.ready()) return "Shizuku não está pronto."
        if (!valid(pkg)) return "Nome de pacote inválido: $pkg"
        Shell.exec("cmd package install-existing --user 0 ${Shell.q(pkg)}")
        val e = Shell.exec("pm enable --user 0 ${Shell.q(pkg)}")
        return if (e.ok) "$pkg restaurado." else "$pkg: ${e.all}"
    }

    /** Removes every listed package that is still installed. */
    fun removeAllPresent(): String {
        val st = states()
        val todo = LIST.filter { st[it.pkg] == State.INSTALLED }
        if (todo.isEmpty()) return "Nada para remover."
        return todo.joinToString("\n") { remove(it.pkg) }
    }
}
