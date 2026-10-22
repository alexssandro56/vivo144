package com.example.data.repositories

import com.example.data.local.GameProfileDao
import com.example.data.local.GameProfileEntity
import com.example.data.models.GameProfile
import com.example.data.models.PerformanceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface GameProfileRepository {
    fun getAllGameProfiles(): Flow<List<GameProfile>>
    suspend fun getGameProfileById(id: Long): GameProfile?
    suspend fun addGameProfile(gameProfile: GameProfile): Long
    suspend fun updateGameProfile(gameProfile: GameProfile)
    suspend fun deleteGameProfile(id: Long)
    suspend fun toggleGameProfile(id: Long, enabled: Boolean)
    suspend fun initializeDefaultsIfEmpty()
}

class GameProfileRepositoryImpl(
    private val gameProfileDao: GameProfileDao
) : GameProfileRepository {

    override fun getAllGameProfiles(): Flow<List<GameProfile>> {
        return gameProfileDao.getAllProfiles().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getGameProfileById(id: Long): GameProfile? = withContext(Dispatchers.IO) {
        gameProfileDao.getProfileById(id)?.toDomain()
    }

    override suspend fun addGameProfile(gameProfile: GameProfile): Long = withContext(Dispatchers.IO) {
        gameProfileDao.insertProfile(gameProfile.toEntity())
    }

    override suspend fun updateGameProfile(gameProfile: GameProfile) = withContext(Dispatchers.IO) {
        gameProfileDao.updateProfile(gameProfile.toEntity())
    }

    override suspend fun deleteGameProfile(id: Long) = withContext(Dispatchers.IO) {
        gameProfileDao.deleteProfileById(id)
    }

    override suspend fun toggleGameProfile(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        gameProfileDao.setEnabled(id, enabled)
    }

    override suspend fun initializeDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        if (gameProfileDao.getCount() == 0) {
            GameProfile.DEFAULT_GAMES.forEach { game ->
                gameProfileDao.insertProfile(game.toEntity().copy(id = 0))
            }
        }
    }

    private fun GameProfileEntity.toDomain(): GameProfile {
        val perfProfile = PerformanceProfile.getById(performanceProfileId)
        return GameProfile(
            id = id,
            gameName = gameName,
            packageName = packageName,
            targetFps = targetFps,
            refreshRate = refreshRate,
            performanceProfile = perfProfile,
            enabled = enabled
        )
    }

    private fun GameProfile.toEntity(): GameProfileEntity {
        return GameProfileEntity(
            id = id,
            gameName = gameName,
            packageName = packageName,
            targetFps = targetFps,
            refreshRate = refreshRate,
            performanceProfileId = performanceProfile.id,
            enabled = enabled
        )
    }
}
