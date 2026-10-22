package com.example

import android.app.Application
import com.example.data.AppContainer
import com.example.data.models.GameProfile
import com.vivo144.app.core.Apps
import com.vivo144.app.core.Prefs
import com.vivo144.app.core.Rms
import com.vivo144.app.core.ShizukuState
import com.vivo144.app.core.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VivoFpsApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // vivo144 engine (rms vote, automatic mode, brightness/Power tools)
        Rms.app = this
        Prefs.init(this)
        Util.init(this)
        ShizukuState.init()

        appContainer = AppContainer(this)
        syncGameProfiles()
    }

    /**
     * The game profiles screen is the single source of truth for the automatic
     * mode: enabled profiles become the watched games, each with its own rate.
     * On first run, installed games are added as profiles.
     */
    private fun syncGameProfiles() {
        val repo = appContainer.gameProfileRepository
        appScope.launch {
            try {
                repo.initializeDefaultsIfEmpty()
                if (!Prefs.profilesSeeded) {
                    val known = repo.getAllGameProfiles().first().map { it.packageName }.toSet()
                    Apps.launchable(this@VivoFpsApplication)
                        .filter { it.isGame && it.pkg !in known }
                        .forEach { repo.addGameProfile(GameProfile(gameName = it.label, packageName = it.pkg)) }
                    Prefs.profilesSeeded = true
                }
                repo.getAllGameProfiles().collect { list ->
                    val on = list.filter { it.enabled }
                    Prefs.games = on.map { it.packageName }.toSet()
                    Prefs.gameFps = on.associate { it.packageName to maxOf(it.refreshRate, it.targetFps).coerceIn(60, 165) }
                }
            } catch (_: Throwable) {
            }
        }
    }

    companion object {
        lateinit var instance: VivoFpsApplication
            private set
    }
}
