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

private const val DEFAULT_LATITUDE = 21.4225
private const val DEFAULT_LONGITUDE = 39.8262
private const val DEFAULT_CITY = "Makkah"
private const val DEFAULT_COUNTRY = "Saudi Arabia"

/** Maps raw preferences to settings. Internal for unit tests (legacy-format coverage). */
internal fun Preferences.toUserSettings(): UserSettings {
    val isGps = this[SettingsKeys.GPS_AUTO] ?: true
    return UserSettings(
        calculationMethod = readCalculationMethod(),
        juristicMethod = readJuristicMethod(),
        highLatitudeRule = readHighLatitudeRule(),
        themeMode = readThemeMode(),
        highPrioritySound = this[SettingsKeys.HIGH_PRIORITY] ?: true,
        isGpsAuto = isGps,
        is24HourFormat = this[SettingsKeys.H24] ?: false,
        currentLocation = readLocation(isGps),
        prayerAlertSounds = readPrayerSounds(),
        prayerAlertEnabled = readPrayerEnabled(),
        minuteOffsets = readMinuteOffsets(),
    )
}

private fun Preferences.readCalculationMethod(): CalculationMethod = CalculationMethod.fromId(
    this[SettingsKeys.CALC_METHOD] ?: CalculationMethod.MUSLIM_WORLD_LEAGUE.id,
)

private fun Preferences.readJuristicMethod(): JuristicMethod = JuristicMethod.fromId(
    this[SettingsKeys.JURISTIC] ?: JuristicMethod.STANDARD.id,
)

private fun Preferences.readHighLatitudeRule(): HighLatitudeRule = HighLatitudeRule.fromId(
    this[SettingsKeys.HIGH_LAT] ?: HighLatitudeRule.ANGLE_BASED.id,
)

private fun Preferences.readThemeMode(): AppThemeMode = AppThemeMode.fromId(
    this[SettingsKeys.THEME] ?: AppThemeMode.SYSTEM.id,
)

private fun Preferences.readLocation(isGps: Boolean): LocationInfo = LocationInfo(
    latitude = this[SettingsKeys.LAT] ?: DEFAULT_LATITUDE,
    longitude = this[SettingsKeys.LNG] ?: DEFAULT_LONGITUDE,
    cityName = this[SettingsKeys.CITY] ?: DEFAULT_CITY,
    countryName = this[SettingsKeys.COUNTRY] ?: DEFAULT_COUNTRY,
    isGpsBased = isGps,
)

private fun Preferences.readPrayerSounds(): Map<PrayerType, AdhanSoundType> = PrayerType.dailyPrayers
    .associateWith { prayer ->
        AdhanSoundType.fromId(this[SettingsKeys.sound(prayer)] ?: defaultSound(prayer).id)
    }

private fun Preferences.readPrayerEnabled(): Map<PrayerType, Boolean> = PrayerType.dailyPrayers
    .associateWith { prayer ->
        this[SettingsKeys.enabled(prayer)] ?: prayer.defaultAlertEnabled
    }

private fun Preferences.readMinuteOffsets(): Map<PrayerType, Int> = PrayerType.dailyPrayers
    .associateWith { prayer ->
        this[SettingsKeys.offset(prayer)] ?: 0
    }
