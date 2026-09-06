package tech.sadique.qayam.data.preferences

import androidx.datastore.preferences.core.Preferences
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.AppThemeMode
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.LocationInfo
import tech.sadique.qayam.data.model.PrayerType

internal fun defaultSound(prayer: PrayerType): AdhanSoundType = when (prayer) {
    PrayerType.SUNRISE, PrayerType.GURUB_E_AFTAB -> AdhanSoundType.SILENT
    PrayerType.ISRAQ -> AdhanSoundType.GENTLE_CHIME
    PrayerType.ASR -> AdhanSoundType.MADINAH
    PrayerType.MAGHRIB -> AdhanSoundType.AL_AQSA
    else -> AdhanSoundType.MAKKAH
}

/** Maps raw preferences to settings. Internal for unit tests (legacy-format coverage). */
internal fun Preferences.toUserSettings(): UserSettings {
    val lat = this[SettingsKeys.LAT] ?: 21.4225
    val lng = this[SettingsKeys.LNG] ?: 39.8262
    val isGps = this[SettingsKeys.GPS_AUTO] ?: true
    return UserSettings(
        calculationMethod = CalculationMethod.fromId(
            this[SettingsKeys.CALC_METHOD] ?: CalculationMethod.MUSLIM_WORLD_LEAGUE.id
        ),
        juristicMethod = JuristicMethod.fromId(
            this[SettingsKeys.JURISTIC] ?: JuristicMethod.STANDARD.id
        ),
        highLatitudeRule = HighLatitudeRule.fromId(
            this[SettingsKeys.HIGH_LAT] ?: HighLatitudeRule.ANGLE_BASED.id
        ),
        themeMode = AppThemeMode.fromId(
            this[SettingsKeys.THEME] ?: AppThemeMode.SYSTEM.id
        ),
        highPrioritySound = this[SettingsKeys.HIGH_PRIORITY] ?: true,
        isGpsAuto = isGps,
        is24HourFormat = this[SettingsKeys.H24] ?: false,
        currentLocation = LocationInfo(
            latitude = lat,
            longitude = lng,
            cityName = this[SettingsKeys.CITY] ?: "Makkah",
            countryName = this[SettingsKeys.COUNTRY] ?: "Saudi Arabia",
            isGpsBased = isGps
        ),
        prayerAlertSounds = PrayerType.dailyPrayers.associateWith { prayer ->
            AdhanSoundType.fromId(this[SettingsKeys.sound(prayer)] ?: defaultSound(prayer).id)
        },
        prayerAlertEnabled = PrayerType.dailyPrayers.associateWith { prayer ->
            this[SettingsKeys.enabled(prayer)] ?: prayer.defaultAlertEnabled
        },
        minuteOffsets = PrayerType.dailyPrayers.associateWith { prayer ->
            this[SettingsKeys.offset(prayer)] ?: 0
        }
    )
}
