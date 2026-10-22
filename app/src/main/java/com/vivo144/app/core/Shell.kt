package com.vivo144.app.core

import rikka.shizuku.Shizuku
import java.lang.reflect.Method
import kotlin.concurrent.thread

/**
 * Shell commands as uid 2000 through Shizuku. Shizuku.newProcess is private
 * in API 13.x, so it is reached by reflection. Only used for short native
 * commands (settings / pm / appops / cmd), never in a tight loop.
 */
object Shell {
    data class Result(val code: Int, val out: String, val err: String) {
        val ok get() = code == 0
        val all get() = (out + if (err.isEmpty()) "" else "\n$err").trim()
    }

    private val newProcess: Method by lazy {
        Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
        ).apply { isAccessible = true }
    }

    fun ready() = ShizukuState.ready(1500)

    fun exec(cmd: String): Result {
        if (!ready()) return Result(-1, "", "Shizuku não está rodando ou sem permissão")
        return try {
            val p = newProcess.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            var err = ""
            val t = thread { err = p.errorStream.bufferedReader().use { it.readText() } }
            val out = p.inputStream.bufferedReader().use { it.readText() }
            val code = p.waitFor()
            t.join(2000)
            Result(code, out.trim(), err.trim())
        } catch (t: Throwable) {
            Result(-1, "", "Erro no Shizuku: ${t.cause ?: t}")
        }
    }

    fun q(s: String) = "'" + s.replace("'", "'\\''") + "'"
}
