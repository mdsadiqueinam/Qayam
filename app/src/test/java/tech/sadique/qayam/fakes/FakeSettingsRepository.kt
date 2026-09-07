package tech.sadique.qayam.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.AppThemeMode
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.LocationInfo
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.SettingsRepository
import tech.sadique.qayam.data.preferences.UserSettings

class FakeSettingsRepository(initialSettings: UserSettings = UserSettings()) : SettingsRepository {

    private val _settings = MutableStateFlow(initialSettings)
    override val settings: Flow<UserSettings> = _settings.asStateFlow()

    override suspend fun snapshot(): UserSettings = _settings.value

    override suspend fun updateThemeMode(mode: AppThemeMode) {
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    override suspend fun updateIs24HourFormat(is24Hour: Boolean) {
        _settings.value = _settings.value.copy(is24HourFormat = is24Hour)
    }

    override suspend fun updateCalculationMethod(method: CalculationMethod) {
        _settings.value = _settings.value.copy(calculationMethod = method)
    }

    override suspend fun updateJuristicMethod(method: JuristicMethod) {
        _settings.value = _settings.value.copy(juristicMethod = method)
    }

    override suspend fun updateHighLatitudeRule(rule: HighLatitudeRule) {
        _settings.value = _settings.value.copy(highLatitudeRule = rule)
    }

    override suspend fun updateHighPrioritySound(enabled: Boolean) {
        _settings.value = _settings.value.copy(highPrioritySound = enabled)
    }

    override suspend fun updateLocation(location: LocationInfo) {
        _settings.value = _settings.value.copy(currentLocation = location)
    }

    override suspend fun updatePrayerAlertSound(prayer: PrayerType, sound: AdhanSoundType) {
        val updated = _settings.value.prayerAlertSounds.toMutableMap().apply { put(prayer, sound) }
        _settings.value = _settings.value.copy(prayerAlertSounds = updated)
    }

    override suspend fun updatePrayerAlertEnabled(prayer: PrayerType, enabled: Boolean) {
        val updated = _settings.value.prayerAlertEnabled.toMutableMap().apply { put(prayer, enabled) }
        _settings.value = _settings.value.copy(prayerAlertEnabled = updated)
    }

    override suspend fun updatePrayerMinuteOffset(prayer: PrayerType, offsetMinutes: Int) {
        val updated = _settings.value.minuteOffsets.toMutableMap().apply { put(prayer, offsetMinutes) }
        _settings.value = _settings.value.copy(minuteOffsets = updated)
    }
}
