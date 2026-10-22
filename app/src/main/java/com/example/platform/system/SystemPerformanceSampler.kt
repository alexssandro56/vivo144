package com.example.platform.system

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.example.data.models.SystemPerformanceMetrics
import java.io.RandomAccessFile

class SystemPerformanceSampler(private val context: Context) {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private var lastTotalCpuTime: Long = 0
    private var lastIdleCpuTime: Long = 0

    fun sampleMetrics(): SystemPerformanceMetrics {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalRamBytes = memoryInfo.totalMem
        val availRamBytes = memoryInfo.availMem
        val usedRamBytes = totalRamBytes - availRamBytes

        val totalGb = totalRamBytes.toFloat() / (1024f * 1024f * 1024f)
        val usedGb = usedRamBytes.toFloat() / (1024f * 1024f * 1024f)
        val availGb = availRamBytes.toFloat() / (1024f * 1024f * 1024f)

        val ramPercent = if (totalRamBytes > 0) {
            ((usedRamBytes.toDouble() / totalRamBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val cpuUsage = calculateCpuUsage()
        val activeThreads = Thread.activeCount()

        val gpuRenderer = try {
            val hardware = Build.HARDWARE
            val board = Build.BOARD
            if (hardware.isNotBlank() && hardware != "unknown") "$hardware ($board)" else "Adreno / Mali GPU"
        } catch (_: Exception) {
            null
        }

        val health = when {
            ramPercent > 90 -> "Memory Pressure"
            cpuUsage != null && cpuUsage > 85 -> "Heavy Load"
            else -> "Optimal"
        }

        return SystemPerformanceMetrics(
            cpuUsagePercent = cpuUsage,
            gpuUsagePercent = null, // GPU usage percentage is not exposed in public Android APIs without kernel/root profiling
            gpuRendererName = gpuRenderer,
            ramUsagePercent = ramPercent,
            usedRamGb = usedGb,
            totalRamGb = totalGb,
            availableRamGb = availGb,
            performanceHealth = health,
            activeThreadsCount = activeThreads,
            cpuCoresCount = Runtime.getRuntime().availableProcessors()
        )
    }

    private fun calculateCpuUsage(): Int? {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()

            val toks = load.split("\\s+".toRegex())
            if (toks.size >= 9) {
                val user = toks[1].toLong()
                val nice = toks[2].toLong()
                val system = toks[3].toLong()
                val idle = toks[4].toLong()
                val iowait = toks[5].toLong()
                val irq = toks[6].toLong()
                val softirq = toks[7].toLong()

                val currentIdle = idle + iowait
                val currentTotal = user + nice + system + currentIdle + irq + softirq

                val totalDiff = currentTotal - lastTotalCpuTime
                val idleDiff = currentIdle - lastIdleCpuTime

                lastTotalCpuTime = currentTotal
                lastIdleCpuTime = currentIdle

                if (totalDiff > 0) {
                    val cpu = (((totalDiff - idleDiff).toDouble() / totalDiff.toDouble()) * 100).toInt()
                    cpu.coerceIn(0, 100)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            // Android 8+ strict SELinux blocks /proc/stat in unprivileged app sandbox
            null
        }
    }
}
