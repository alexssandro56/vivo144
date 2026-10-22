package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppContainer
import com.example.data.models.PerformanceProfile
import com.example.data.models.TemperatureStatus
import com.example.data.models.ThermalInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun testAppNameResource() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Vivo iQOO 144FPS", appName)
  }

  @Test
  fun testAppContainerInitialization() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = AppContainer(context)
    val specs = container.deviceDiagnosticsRepository.getDeviceSpecs()
    assertNotNull(specs)
    assertNotNull(specs.model)
    assertTrue(specs.supportedRefreshRates.isNotEmpty())
  }

  @Test
  fun testUserPreferencesConfigDefault() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = AppContainer(context)
    val config = container.userPreferencesRepository.fpsConfigFlow.value
    assertEquals(144, config.targetFps)
    assertEquals(144, config.refreshRateHz)
  }

  @Test
  fun testThermalClassification() {
    val optimal = ThermalInfo.fromCelsius(36.5f, 0)
    assertEquals(TemperatureStatus.OPTIMAL, optimal.thermalSeverity)

    val warm = ThermalInfo.fromCelsius(42.0f, 0)
    assertEquals(TemperatureStatus.WARM, warm.thermalSeverity)

    val high = ThermalInfo.fromCelsius(47.0f, 0)
    assertEquals(TemperatureStatus.HIGH, high.thermalSeverity)

    val critical = ThermalInfo.fromCelsius(52.0f, 0)
    assertEquals(TemperatureStatus.CRITICAL, critical.thermalSeverity)
  }

  @Test
  fun testPerformanceProfiles() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = AppContainer(context)

    val profile = container.performanceProfileRepository.getActiveProfile()
    assertNotNull(profile)

    container.performanceProfileRepository.setActiveProfile(PerformanceProfile.BALANCED)
    val updated = container.performanceProfileRepository.getActiveProfile()
    assertEquals(120, updated.targetFps)
    assertEquals(120, updated.refreshRate)
  }

  @Test
  fun testGameProfilesInitialization() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = AppContainer(context)

    container.gameProfileRepository.initializeDefaultsIfEmpty()
    val games = container.gameProfileRepository.getAllGameProfiles().first()
    assertTrue(games.isNotEmpty())
    assertTrue(games.any { it.gameName == "PUBG Mobile" })
  }

  @Test
  fun testCompatibilityEvaluation() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = AppContainer(context)

    val result = container.compatibilityRepository.evaluateCompatibility(144)
    assertNotNull(result)
    assertTrue(result.items.isNotEmpty())
  }

  @Test
  fun testQuickSettingsTileResources() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tileLabel = context.getString(R.string.tile_high_performance_label)
    assertEquals("High Performance", tileLabel)
    val toastEnabled = context.getString(R.string.tile_toast_enabled)
    assertTrue(toastEnabled.contains("144 FPS"))
  }

  @Test
  fun testQuickSettingsToggleLogic() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = AppContainer(context)

    // Simulate toggle ON
    container.userPreferencesRepository.saveFpsConfig(
      targetFps = 144,
      refreshRate = 144f,
      isUnlocked = true
    )
    var config = container.userPreferencesRepository.fpsConfigFlow.value
    assertTrue(config.isUnlocked)
    assertEquals(144, config.targetFps)

    // Simulate toggle OFF (revert to 60 FPS)
    container.userPreferencesRepository.saveFpsConfig(
      targetFps = 60,
      refreshRate = 60f,
      isUnlocked = false
    )
    config = container.userPreferencesRepository.fpsConfigFlow.value
    assertEquals(60, config.targetFps)
    assertEquals(false, config.isUnlocked)
  }

  @Test
  fun testOverlayPreferencesPersistence() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = AppContainer(context)

    val initial = container.userPreferencesRepository.overlayPreferencesFlow.value
    assertEquals(false, initial.isOverlayEnabled)
    assertEquals(90, initial.opacityPercent)

    val updated = initial.copy(
      isOverlayEnabled = true,
      hudStyle = com.example.data.models.OverlayHudStyle.EXPANDED_CARD,
      opacityPercent = 85,
      autoLaunchOnGames = true,
      snapToEdges = true
    )
    container.userPreferencesRepository.saveOverlayPreferences(updated)

    val readBack = container.userPreferencesRepository.overlayPreferencesFlow.value
    assertTrue(readBack.isOverlayEnabled)
    assertEquals(com.example.data.models.OverlayHudStyle.EXPANDED_CARD, readBack.hudStyle)
    assertEquals(85, readBack.opacityPercent)
    assertTrue(readBack.autoLaunchOnGames)
    assertTrue(readBack.snapToEdges)
  }

  @Test
  fun testGroundedCompatibilityVerificationForOfficialModel() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = AppContainer(context)
    val specs = container.deviceDiagnosticsRepository.getDeviceSpecs()

    val report = container.groundedCompatibilityRepository.checkGroundedCompatibility(
      deviceSpecs = specs,
      customQuery = "iQOO 13"
    )

    assertNotNull(report)
    assertEquals("iQOO 13", report.deviceModelChecked)
    assertTrue(report.isOfficiallyListed)
    assertEquals(com.example.data.models.CompatibilityConfidence.OFFICIAL_VERIFIED, report.confidence)
    assertTrue(report.officialSupportedModels.isNotEmpty())
    assertTrue(report.detailedAnalysis.contains("officially recognized", ignoreCase = true) || report.detailedAnalysis.isNotBlank())
  }

  @Test
  fun testGroundedCompatibilityAlertForUnlistedDevice() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val container = AppContainer(context)
    val specs = container.deviceDiagnosticsRepository.getDeviceSpecs()

    val report = container.groundedCompatibilityRepository.checkGroundedCompatibility(
      deviceSpecs = specs,
      customQuery = "Generic Unlisted Phone ABC"
    )

    assertNotNull(report)
    assertEquals("Generic Unlisted Phone ABC", report.deviceModelChecked)
    assertEquals(false, report.isOfficiallyListed)
    assertTrue(report.confidence == com.example.data.models.CompatibilityConfidence.UNLISTED_DEVICE ||
               report.confidence == com.example.data.models.CompatibilityConfidence.NON_VIVO_GENERIC)
    assertTrue(report.detailedAnalysis.contains("not found", ignoreCase = true) ||
               report.detailedAnalysis.contains("not", ignoreCase = true))
  }
}
