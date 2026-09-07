package tech.sadique.qayam.data.preferences

import kotlinx.coroutines.flow.Flow
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.AppThemeMode
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.LocationInfo
import tech.sadique.qayam.data.model.PrayerType

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun snapshot(): UserSettings
    suspend fun updateCalculationMethod(method: CalculationMethod)
    suspend fun updateJuristicMethod(juristic: JuristicMethod)
    suspend fun updateHighLatitudeRule(rule: HighLatitudeRule)
    suspend fun updateThemeMode(mode: AppThemeMode)
    suspend fun updateHighPrioritySound(enabled: Boolean)
    suspend fun updateIs24HourFormat(is24H: Boolean)
    suspend fun updatePrayerAlertSound(prayer: PrayerType, sound: AdhanSoundType)
    suspend fun updatePrayerAlertEnabled(prayer: PrayerType, enabled: Boolean)
    suspend fun updatePrayerMinuteOffset(prayer: PrayerType, offset: Int)
    suspend fun updateLocation(location: LocationInfo)
}
