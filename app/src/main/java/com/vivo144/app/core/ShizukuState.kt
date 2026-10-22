package com.vivo144.app.core

import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Live Shizuku status for the UI, plus a blocking wait for background code. */
object ShizukuState {
    enum class Status { NOT_RUNNING, NO_PERMISSION, READY }

    private const val REQ = 144
    private val _status = MutableStateFlow(Status.NOT_RUNNING)
    val status: StateFlow<Status> = _status

    fun init() {
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener { refresh() }
        Shizuku.addRequestPermissionResultListener { code, _ -> if (code == REQ) refresh() }
    }

    fun refresh(): Status {
        val s = try {
            when {
                !Shizuku.pingBinder() -> Status.NOT_RUNNING
                Shizuku.isPreV11() -> Status.NOT_RUNNING
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> Status.READY
                else -> Status.NO_PERMISSION
            }
        } catch (t: Throwable) {
            Status.NOT_RUNNING
        }
        _status.value = s
        return s
    }

    fun request() {
        if (refresh() == Status.NO_PERMISSION) {
            try { Shizuku.requestPermission(REQ) } catch (_: Throwable) { }
        }
    }

    /** Call from a background thread: the binder arrives shortly after process start. */
    fun ready(timeoutMs: Long = 2000): Boolean {
        if (refresh() == Status.READY) return true
        if (!Shizuku.pingBinder()) {
            val latch = CountDownLatch(1)
            val l = Shizuku.OnBinderReceivedListener { latch.countDown() }
            Shizuku.addBinderReceivedListenerSticky(l)
            try { latch.await(timeoutMs, TimeUnit.MILLISECONDS) } catch (_: InterruptedException) { }
            Shizuku.removeBinderReceivedListener(l)
        }
        return refresh() == Status.READY
    }
}
