package tech.sadique.qayam.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import tech.sadique.qayam.audio.AdhanAudioSynthesizer
import tech.sadique.qayam.data.calculator.PrayerTimeCalculator
import tech.sadique.qayam.data.location.LocationService
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.AppThemeMode
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.CurrentPrayerState
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.LocationInfo
import tech.sadique.qayam.data.model.PrayerSchedule
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.AppSettings
import tech.sadique.qayam.data.preferences.UserSettings
import tech.sadique.qayam.notification.AdhanNotificationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.time.Duration.Companion.seconds

data class PrayerUiState(
    val settings: UserSettings = UserSettings(),
    val schedule: PrayerSchedule? = null,
    val isLocationLoading: Boolean = false,
    val locationErrorMessage: String? = null,
    val isPlayingSound: Boolean = false,
    val playingSoundType: AdhanSoundType? = null
)

/**
 * Per-second clock state, collected only by the composables that render
 * live time (clock, countdown, horizon). Kept separate from [PrayerUiState]
 * so the rest of the UI does not recompose every second.
 */
data class PrayerTickerState(
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val currentState: CurrentPrayerState? = null
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val appSettings = AppSettings(application.applicationContext)
    private val locationService = LocationService(application.applicationContext)
    private val notificationManager = AdhanNotificationManager(application.applicationContext)

    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private val _tickerState = MutableStateFlow(PrayerTickerState())
    val tickerState: StateFlow<PrayerTickerState> = _tickerState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        // Observe settings changes
        viewModelScope.launch {
            appSettings.settings.collect { newSettings ->
                _uiState.value = _uiState.value.copy(settings = newSettings)
                recalculateSchedule()
                notificationManager.scheduleUpcomingAlarms(appSettings)
            }
        }
        // Observe audio state
        viewModelScope.launch {
            AdhanAudioSynthesizer.isPlaying.collect { isPlaying ->
                _uiState.value = _uiState.value.copy(isPlayingSound = isPlaying)
            }
        }
        viewModelScope.launch {
            AdhanAudioSynthesizer.currentlyPlayingSound.collect { sound ->
                _uiState.value = _uiState.value.copy(playingSoundType = sound)
            }
        }

        // Start real-time 1-second clock ticker
        startClockTicker()

        // Auto fetch GPS location if enabled (read persisted value, not the
        // default _uiState which is isGpsAuto=true before the flow emits).
        if (appSettings.settings.value.isGpsAuto) {
            refreshGpsLocation()
        }
    }

    private fun startClockTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                refreshTicker(Calendar.getInstance())
                // Align to the next wall-clock second to avoid drift/jitter.
                val now = System.currentTimeMillis()
                delay(1000 - (now % 1000))
            }
        }
    }

    private fun refreshTicker(now: Calendar) {
        val schedule = _uiState.value.schedule
        val loc = _uiState.value.settings.currentLocation
        val currentState = if (schedule != null) {
            PrayerTimeCalculator.calculateCurrentState(
                currentTime = now,
                schedule = schedule,
                latitude = loc.latitude,
                longitude = loc.longitude
            )
        } else null
        _tickerState.value = PrayerTickerState(
            currentTimeMillis = now.timeInMillis,
            currentState = currentState
        )
    }

    fun recalculateSchedule() {
        val settings = _uiState.value.settings
        val loc = settings.currentLocation
        val now = Calendar.getInstance()
        val tzOffset = now.timeZone.getOffset(now.timeInMillis) / 3600000.0

        val schedule = PrayerTimeCalculator.calculateSchedule(
            date = now,
            latitude = loc.latitude,
            longitude = loc.longitude,
            timezoneOffsetHours = tzOffset,
            method = settings.calculationMethod,
            juristic = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            minuteOffsets = settings.minuteOffsets
        )

        _uiState.value = _uiState.value.copy(schedule = schedule)
        refreshTicker(now)
    }

    fun refreshGpsLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLocationLoading = true, locationErrorMessage = null)
            val gpsLoc = locationService.getCurrentGpsLocation()
            if (gpsLoc != null) {
                appSettings.updateLocation(gpsLoc)
                _uiState.value = _uiState.value.copy(isLocationLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLocationLoading = false,
                    locationErrorMessage = "GPS unavailable. Using cached coordinates."
                )
            }
        }
    }

    fun selectPresetLocation(location: LocationInfo) {
        appSettings.updateLocation(location)
    }

    fun updateCalculationMethod(method: CalculationMethod) {
        appSettings.updateCalculationMethod(method)
    }

    fun updateJuristicMethod(juristic: JuristicMethod) {
        appSettings.updateJuristicMethod(juristic)
    }

    fun updateHighLatitudeRule(rule: HighLatitudeRule) {
        appSettings.updateHighLatitudeRule(rule)
    }

    fun updateThemeMode(mode: AppThemeMode) {
        appSettings.updateThemeMode(mode)
    }

    fun updateHighPrioritySound(enabled: Boolean) {
        appSettings.updateHighPrioritySound(enabled)
    }

    fun updateIs24HourFormat(is24H: Boolean) {
        appSettings.updateIs24HourFormat(is24H)
    }

    fun updatePrayerAlertSound(prayer: PrayerType, sound: AdhanSoundType) {
        appSettings.updatePrayerAlertSound(prayer, sound)
    }

    fun updatePrayerAlertEnabled(prayer: PrayerType, enabled: Boolean) {
        appSettings.updatePrayerAlertEnabled(prayer, enabled)
    }

    fun updatePrayerMinuteOffset(prayer: PrayerType, offset: Int) {
        appSettings.updatePrayerMinuteOffset(prayer, offset)
    }

    fun playPreviewSound(soundType: AdhanSoundType) {
        if (_uiState.value.isPlayingSound && _uiState.value.playingSoundType == soundType) {
            AdhanAudioSynthesizer.stopSound()
        } else {
            AdhanAudioSynthesizer.playSound(
                context = getApplication(),
                soundType = soundType,
                highPriorityAlarm = _uiState.value.settings.highPrioritySound,
                volume = 1.0f
            )
        }
    }

    fun stopPreviewSound() {
        AdhanAudioSynthesizer.stopSound()
    }

    fun canScheduleExactAlarms(): Boolean {
        return notificationManager.canScheduleExactAlarms()
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return notificationManager.isIgnoringBatteryOptimizations()
    }

    fun scheduleTestAlarm(delaySeconds: Int = 10) {
        val nextPrayer = _tickerState.value.currentState?.nextPrayer ?: PrayerType.FAJR
        val soundType = _uiState.value.settings.prayerAlertSounds[nextPrayer] ?: AdhanSoundType.TAKBEER_ONLY
        notificationManager.scheduleTestAlarm(
            delaySeconds = delaySeconds,
            prayerType = nextPrayer,
            soundType = soundType
        )
    }

    override fun onCleared() {
        tickerJob?.cancel()
        AdhanAudioSynthesizer.stopSound()
    }
}
