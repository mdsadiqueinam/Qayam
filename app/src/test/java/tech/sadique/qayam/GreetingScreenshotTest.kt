package tech.sadique.qayam

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.AppThemeMode
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.ui.components.PrayerCard
import tech.sadique.qayam.ui.theme.SalahTheme
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

    @Test
    fun prayer_card_night_mosque_screenshot() {
        val testCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 45)
        }
        composeTestRule.setContent {
            SalahTheme(themeMode = AppThemeMode.NIGHT_MOSQUE) {
                PrayerCard(
                    prayer = PrayerType.ISHA,
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

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/card_night_mosque.png")
    }

    @Test
    fun prayer_card_muted_screenshot() {
        val testCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 5)
        }
        composeTestRule.setContent {
            SalahTheme(themeMode = AppThemeMode.DARK) {
                PrayerCard(
                    prayer = PrayerType.SUNRISE,
                    time = testCal,
                    isCurrent = false,
                    isNext = false,
                    is24Hour = true,
                    soundType = AdhanSoundType.SILENT,
                    isEnabled = false,
                    isPlayingThisSound = false,
                    onToggleAlert = {},
                    onSoundClick = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/card_muted_dark.png")
    }
}
