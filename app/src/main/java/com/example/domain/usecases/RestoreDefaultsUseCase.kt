package com.example.domain.usecases

import com.example.data.models.UnlockStatus
import com.example.data.repositories.FpsUnlockRepository

class RestoreDefaultsUseCase(private val repository: FpsUnlockRepository) {
    suspend operator fun invoke(): Result<UnlockStatus> {
        return repository.restoreDefaults()
    }
}
