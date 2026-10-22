package com.example.domain.usecases

import com.example.data.models.AdbStatus
import com.example.platform.adb.AdbManager

class CheckAdbConnectionUseCase(private val adbManager: AdbManager) {
    operator fun invoke(): AdbStatus = adbManager.getAdbStatus()
}
