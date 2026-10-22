package com.example.data.models

data class AdbStatus(
    val isDeveloperOptionsEnabled: Boolean,
    val isUsbDebuggingEnabled: Boolean,
    val isWirelessDebuggingEnabled: Boolean,
    val hasWriteSecureSettings: Boolean,
    val connectionStateText: String,
    val isAdbAvailable: Boolean,
    val permissionCommand: String,
    val vivoSpecificCommand: String,
    val localIpAddress: String = "127.0.0.1"
) {
    val isFullyAuthorized: Boolean
        get() = hasWriteSecureSettings
}

