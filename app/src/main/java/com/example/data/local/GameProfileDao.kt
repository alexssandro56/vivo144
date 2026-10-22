package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProfileDao {

    @Query("SELECT * FROM game_profiles ORDER BY gameName ASC")
    fun getAllProfiles(): Flow<List<GameProfileEntity>>

    @Query("SELECT * FROM game_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): GameProfileEntity?

    @Query("SELECT * FROM game_profiles WHERE packageName = :packageName LIMIT 1")
    suspend fun getProfileByPackage(packageName: String): GameProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: GameProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: GameProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: GameProfileEntity)

    @Query("DELETE FROM game_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    @Query("UPDATE game_profiles SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM game_profiles")
    suspend fun getCount(): Int
}
