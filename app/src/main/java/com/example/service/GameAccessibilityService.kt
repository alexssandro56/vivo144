package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.example.VivoFpsApplication
import com.example.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class GameAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastForegroundPackage: String? = null

    private val appContainer: AppContainer
        get() = (application as? VivoFpsApplication)?.appContainer
            ?: AppContainer(applicationContext)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == lastForegroundPackage) return
        lastForegroundPackage = packageName

        // Skip self, system ui, launchers
        if (packageName == applicationContext.packageName ||
            packageName.startsWith("com.android.systemui") ||
            packageName.contains("launcher")
        ) {
            return
        }

        serviceScope.launch {
            val overlayPrefs = appContainer.userPreferencesRepository.overlayPreferencesFlow.value
            if (!overlayPrefs.autoLaunchOnGames) return@launch

            if (!Settings.canDrawOverlays(this@GameAccessibilityService)) return@launch

            // Check if active package is a game from profiles
            val gameProfiles = appContainer.gameProfileRepository.getAllGameProfiles().firstOrNull() ?: emptyList()
            val matchingGame = gameProfiles.firstOrNull { it.packageName.equals(packageName, ignoreCase = true) && it.enabled }
                ?: KNOWN_GAMES.firstOrNull { it.equals(packageName, ignoreCase = true) }

            if (matchingGame != null) {
                // Auto-start FPS overlay over active game
                FpsOverlayService.start(this@GameAccessibilityService)
            }
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        val KNOWN_GAMES = setOf(
            "com.dts.freefireth",
            "com.tencent.ig",
            "com.pubg.imobile",
            "com.activision.callofduty.shooter",
            "com.miHoYo.GenshinImpact",
            "com.mobile.legends",
            "com.riotgames.league.wildrift",
            "com.epicgames.fortnite",
            "com.krafton.bgmi",
            "com.ea.gp.apexlegendsmobilefps",
            "com.kurogame.wutheringwaves.global",
            "com.carxtech.sr"
        )

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${GameAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(expectedServiceName) || enabledServices.contains(GameAccessibilityService::class.java.simpleName)
        }
    }
}
