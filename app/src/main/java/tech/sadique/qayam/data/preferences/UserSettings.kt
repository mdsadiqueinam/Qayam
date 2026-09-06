package tech.sadique.qayam.data.preferences

import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.AppThemeMode
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.LocationInfo
import tech.sadique.qayam.data.model.PrayerType

data class UserSettings(
    val calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
    val juristicMethod: JuristicMethod = JuristicMethod.STANDARD,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val highPrioritySound: Boolean = true, // Play sound even in silent/DND mode
    val isGpsAuto: Boolean = true,
    val is24HourFormat: Boolean = false,
    val currentLocation: LocationInfo = LocationInfo(
        latitude = 21.4225,
        longitude = 39.8262,
        cityName = "Makkah",
        countryName = "Saudi Arabia",
        isGpsBased = false
    ),
    val prayerAlertSounds: Map<PrayerType, AdhanSoundType> = mapOf(
        PrayerType.FAJR to AdhanSoundType.MAKKAH,
        PrayerType.SUNRISE to AdhanSoundType.SILENT,
        PrayerType.ISRAQ to AdhanSoundType.GENTLE_CHIME,
        PrayerType.DHUHR to AdhanSoundType.MAKKAH,
        PrayerType.ASR to AdhanSoundType.MADINAH,
        PrayerType.GURUB_E_AFTAB to AdhanSoundType.SILENT,
        PrayerType.MAGHRIB to AdhanSoundType.AL_AQSA,
        PrayerType.ISHA to AdhanSoundType.MAKKAH
    ),
    val prayerAlertEnabled: Map<PrayerType, Boolean> = PrayerType.dailyPrayers.associateWith { it.defaultAlertEnabled },
    val minuteOffsets: Map<PrayerType, Int> = mapOf(
        PrayerType.FAJR to 0,
        PrayerType.SUNRISE to 0,
        PrayerType.ISRAQ to 0,
        PrayerType.DHUHR to 0,
        PrayerType.ASR to 0,
        PrayerType.GURUB_E_AFTAB to 0,
        PrayerType.MAGHRIB to 0,
        PrayerType.ISHA to 0
    )
)
