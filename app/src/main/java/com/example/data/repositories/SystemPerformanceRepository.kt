package com.example.data.repositories

import com.example.data.models.FpsMetrics
import com.example.data.models.SystemPerformanceMetrics
import com.example.platform.fps.RealtimeFpsSampler
import com.example.platform.system.SystemPerformanceSampler
import kotlinx.coroutines.flow.Flow

interface SystemPerformanceRepository {
    fun observeFpsMetrics(targetRefreshRate: Float = 144.0f): Flow<FpsMetrics>
    fun sampleSystemPerformance(): SystemPerformanceMetrics
}

class SystemPerformanceRepositoryImpl(
    private val realtimeFpsSampler: RealtimeFpsSampler,
    private val systemPerformanceSampler: SystemPerformanceSampler
) : SystemPerformanceRepository {

    override fun observeFpsMetrics(targetRefreshRate: Float): Flow<FpsMetrics> {
        return realtimeFpsSampler.observeFps(targetRefreshRate)
    }

    override fun sampleSystemPerformance(): SystemPerformanceMetrics {
        return systemPerformanceSampler.sampleMetrics()
    }
}
