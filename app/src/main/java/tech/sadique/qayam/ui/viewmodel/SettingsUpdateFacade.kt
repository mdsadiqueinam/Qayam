package tech.sadique.qayam.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.AppThemeMode
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.LocationInfo
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.SettingsRepository

/**
 * Fire-and-forget settings writes. Owned by [PrayerViewModel] and launched in
 * the ViewModel scope so writes are cancelled with the ViewModel and run on
 * the same dispatcher in tests. Exposed as [PrayerViewModel.settingsUpdater]
 * so callers do not need a wrapper function per setting on the ViewModel.
 */
class SettingsUpdateFacade(private val settingsRepository: SettingsRepository, private val scope: CoroutineScope) {

    fun selectPresetLocation(location: LocationInfo) {
        scope.launch { settingsRepository.updateLocation(location) }
    }

    fun updateCalculationMethod(method: CalculationMethod) {
        scope.launch { settingsRepository.updateCalculationMethod(method) }
    }

    fun updateJuristicMethod(juristic: JuristicMethod) {
        scope.launch { settingsRepository.updateJuristicMethod(juristic) }
    }

    fun updateHighLatitudeRule(rule: HighLatitudeRule) {
        scope.launch { settingsRepository.updateHighLatitudeRule(rule) }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        scope.launch { settingsRepository.updateThemeMode(mode) }
    }

    fun updateHighPrioritySound(enabled: Boolean) {
        scope.launch { settingsRepository.updateHighPrioritySound(enabled) }
    }

    fun updateIs24HourFormat(is24H: Boolean) {
        scope.launch { settingsRepository.updateIs24HourFormat(is24H) }
    }

    fun updatePrayerAlertSound(prayer: PrayerType, sound: AdhanSoundType) {
        scope.launch { settingsRepository.updatePrayerAlertSound(prayer, sound) }
    }

    fun updatePrayerAlertEnabled(prayer: PrayerType, enabled: Boolean) {
        scope.launch { settingsRepository.updatePrayerAlertEnabled(prayer, enabled) }
    }

    fun updatePrayerMinuteOffset(prayer: PrayerType, offset: Int) {
        scope.launch { settingsRepository.updatePrayerMinuteOffset(prayer, offset) }
    }
}
