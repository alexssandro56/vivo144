package com.example.data.models

sealed class UnlockStatus {
    object Ready : UnlockStatus()
    data class Applying(val progress: Float = 0.5f, val stepDescription: String = "Applying display compositor configurations…") : UnlockStatus()
    data class Unlocked(val targetFps: Int = 144, val refreshRateHz: Int = 144, val timestamp: Long = System.currentTimeMillis()) : UnlockStatus()
    data class Error(val message: String, val requiresAdbPermission: Boolean = false, val commandToRun: String? = null) : UnlockStatus()
    object Restored : UnlockStatus()

    val isApplying: Boolean get() = this is Applying
    val isUnlocked: Boolean get() = this is Unlocked
}
