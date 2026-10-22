package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_profiles")
data class GameProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameName: String,
    val packageName: String,
    val targetFps: Int,
    val refreshRate: Int,
    val performanceProfileId: String,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
