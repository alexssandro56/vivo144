package com.example.domain.usecases

import com.example.data.models.UnlockStatus
import com.example.data.repositories.FpsUnlockRepository

class UnlockFpsUseCase(private val repository: FpsUnlockRepository) {
    suspend operator fun invoke(targetFps: Int = 144, refreshRateHz: Int = 144): Result<UnlockStatus> {
        return repository.applyFpsConfiguration(targetFps, refreshRateHz)
    }
}
