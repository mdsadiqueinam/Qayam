package tech.sadique.qayam

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import tech.sadique.qayam.data.calculator.PrayerTimeCalculator
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.ui.screens.HeroItem
import tech.sadique.qayam.ui.theme.SalahTheme
import tech.sadique.qayam.ui.viewmodel.PrayerTickerState
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class HeroItemScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var defaultTz: TimeZone

    @Before
    fun pinTimezone() {
        defaultTz = TimeZone.getDefault()
        // Clock text uses the device timezone; pin it so goldens are stable.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Riyadh"))
    }

    @After
    fun restoreTimezone() {
        TimeZone.setDefault(defaultTz)
    }

    private fun tickerAt(hour: Int): MutableStateFlow<PrayerTickerState> {
        val tz = TimeZone.getTimeZone("Asia/Riyadh")
        val date = Calendar.getInstance(tz).apply {
            set(2026, Calendar.MARCH, 20, hour, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val schedule = PrayerTimeCalculator.calculateSchedule(
            date = date,
            latitude = 21.4225,
            longitude = 39.8262,
            timezoneOffsetHours = 3.0,
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            juristic = JuristicMethod.STANDARD
        )
        val state = PrayerTimeCalculator.calculateCurrentState(
            currentTime = date,
            schedule = schedule,
            latitude = 21.4225,
            longitude = 39.8262
        )
        return MutableStateFlow(PrayerTickerState(date.timeInMillis, state))
    }

    @Test
    fun hero_day_screenshot() {
        val flow = tickerAt(15)
        composeTestRule.setContent {
            SalahTheme {
                HeroItem(tickerFlow = flow, is24Hour = false)
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/hero_day.png")
    }

    @Test
    fun hero_night_screenshot() {
        val flow = tickerAt(22)
        composeTestRule.setContent {
            SalahTheme {
                HeroItem(tickerFlow = flow, is24Hour = false)
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/hero_night.png")
    }
}
