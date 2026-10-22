package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.models.FpsConfig
import com.example.data.models.ThemePreferences
import com.example.data.models.UnlockStatus
import com.example.ui.components.FpsCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun fps_card_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme(themePreferences = ThemePreferences.DEFAULT) {
        FpsCard(
          fpsConfig = FpsConfig(targetFps = 144, refreshRateHz = 144, currentFps = 60, isUnlocked = false),
          unlockStatus = UnlockStatus.Ready
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
