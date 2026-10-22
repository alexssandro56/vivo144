package com.example.data.repositories

import com.example.data.models.PerformanceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PerformanceProfileRepository {
    val activeProfile: Flow<PerformanceProfile>
    suspend fun getActiveProfile(): PerformanceProfile
    suspend fun setActiveProfile(profile: PerformanceProfile)
    fun getAllProfiles(): List<PerformanceProfile>
}

class PerformanceProfileRepositoryImpl(
    private val userPreferencesRepository: UserPreferencesRepository
) : PerformanceProfileRepository {

    override val activeProfile: Flow<PerformanceProfile> =
        userPreferencesRepository.userPreferencesFlow.map { prefs ->
            when (prefs.targetFps) {
                144 -> PerformanceProfile.ULTRA_PERFORMANCE
                120 -> PerformanceProfile.BALANCED
                60 -> PerformanceProfile.BATTERY_SAVER
                else -> PerformanceProfile.ULTRA_PERFORMANCE
            }
        }

    override suspend fun getActiveProfile(): PerformanceProfile {
        val targetFps = userPreferencesRepository.getTargetFps()
        return when (targetFps) {
            144 -> PerformanceProfile.ULTRA_PERFORMANCE
            120 -> PerformanceProfile.BALANCED
            60 -> PerformanceProfile.BATTERY_SAVER
            else -> PerformanceProfile.ULTRA_PERFORMANCE
        }
    }

    override suspend fun setActiveProfile(profile: PerformanceProfile) {
        userPreferencesRepository.saveFpsConfig(
            targetFps = profile.targetFps,
            refreshRate = profile.refreshRate.toFloat(),
            isUnlocked = true
        )
    }

    override fun getAllProfiles(): List<PerformanceProfile> {
        return PerformanceProfile.ALL_PROFILES
    }
}
