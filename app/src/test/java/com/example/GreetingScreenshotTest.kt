package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.AdhanSoundType
import com.example.data.model.PrayerType
import com.example.ui.components.PrayerCard
import com.example.ui.theme.SalahTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun prayer_card_screenshot() {
        val testCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 30)
        }

        composeTestRule.setContent {
            SalahTheme {
                PrayerCard(
                    prayer = PrayerType.DHUHR,
                    time = testCal,
                    isCurrent = true,
                    isNext = false,
                    is24Hour = false,
                    soundType = AdhanSoundType.MAKKAH,
                    isEnabled = true,
                    isPlayingThisSound = false,
                    onToggleAlert = {},
                    onSoundClick = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
