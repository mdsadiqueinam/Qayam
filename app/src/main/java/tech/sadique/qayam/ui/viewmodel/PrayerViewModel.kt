package tech.sadique.qayam.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.sadique.qayam.audio.AudioPreviewController
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.CurrentPrayerState
import tech.sadique.qayam.data.model.PrayerSchedule
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.SettingsRepository
import tech.sadique.qayam.data.preferences.UserSettings
import tech.sadique.qayam.domain.RecalculateScheduleUseCase
import tech.sadique.qayam.domain.RefreshLocationUseCase
import tech.sadique.qayam.notification.AlarmScheduler
import tech.sadique.qayam.notification.SchedulePrayerAlarmsUseCase
import java.util.Calendar
import javax.inject.Inject

data class PrayerUiState(
    val settings: UserSettings = UserSettings(),
    val schedule: PrayerSchedule? = null,
    val isLocationLoading: Boolean = false,
    val locationErrorMessage: String? = null,
    val isPlayingSound: Boolean = false,
    val playingSoundType: AdhanSoundType? = null,
)

/**
 * Per-second clock state, collected only by the composables that render
 * live time (clock, countdown, horizon). Kept separate from [PrayerUiState]
 * so the rest of the UI does not recompose every second.
 */
data class PrayerTickerState(
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val currentState: CurrentPrayerState? = null,
)

@HiltViewModel
class PrayerViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val schedulePrayerAlarmsUseCase: SchedulePrayerAlarmsUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val recalculateScheduleUseCase: RecalculateScheduleUseCase,
    private val refreshLocationUseCase: RefreshLocationUseCase,
    private val audioPreviewController: AudioPreviewController,
    private val tickerManager: TickerManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    val tickerState: StateFlow<PrayerTickerState> = tickerManager.tickerState

    val settingsUpdater = SettingsUpdateFacade(settingsRepository, viewModelScope)

    init {
        var firstSettings = true
        viewModelScope.launch {
            settingsRepository.settings.collect { newSettings ->
                _uiState.value = _uiState.value.copy(settings = newSettings)
                recalculateSchedule()
                schedulePrayerAlarmsUseCase(newSettings)
                if (firstSettings) {
                    firstSettings = false
                    if (newSettings.isGpsAuto) refreshGpsLocation()
                }
            }
        }

        viewModelScope.launch {
            audioPreviewController.isPlayingSound.collect { isPlaying ->
                _uiState.value = _uiState.value.copy(isPlayingSound = isPlaying)
            }
        }
        viewModelScope.launch {
            audioPreviewController.playingSoundType.collect { sound ->
                _uiState.value = _uiState.value.copy(playingSoundType = sound)
            }
        }

        tickerManager.start(viewModelScope) {
            _uiState.value.schedule to _uiState.value.settings.currentLocation
        }
    }

    fun recalculateSchedule() {
        val settings = _uiState.value.settings
        val now = Calendar.getInstance()
        val schedule = recalculateScheduleUseCase(settings, now)
        _uiState.value = _uiState.value.copy(schedule = schedule)
        tickerManager.refreshTicker(schedule, settings.currentLocation, now)
    }

    fun refreshGpsLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLocationLoading = true, locationErrorMessage = null)
            val result = refreshLocationUseCase()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLocationLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLocationLoading = false,
                    locationErrorMessage = "GPS unavailable. Using cached coordinates.",
                )
            }
        }
    }

    fun playPreviewSound(soundType: AdhanSoundType) {
        audioPreviewController.playPreviewSound(soundType, _uiState.value.settings.highPrioritySound)
    }

    fun stopPreviewSound() {
        audioPreviewController.stopPreviewSound()
    }

    fun canScheduleExactAlarms(): Boolean = alarmScheduler.canScheduleExactAlarms()

    fun isIgnoringBatteryOptimizations(): Boolean = alarmScheduler.isIgnoringBatteryOptimizations()

    fun scheduleTestAlarm(delaySeconds: Int = 10) {
        val nextPrayer = tickerState.value.currentState?.nextPrayer ?: PrayerType.FAJR
        val soundType = _uiState.value.settings.prayerAlertSounds[nextPrayer] ?: AdhanSoundType.MAKKAH
        alarmScheduler.scheduleTestAlarm(
            delaySeconds = delaySeconds,
            prayerType = nextPrayer,
            soundType = soundType,
        )
    }

    public override fun onCleared() {
        super.onCleared()
        tickerManager.stop()
        audioPreviewController.stopPreviewSound()
    }
}
