package tech.sadique.qayam.data.preferences

import android.content.Context
import android.content.SharedPreferences
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.AppThemeMode
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.LocationInfo
import tech.sadique.qayam.data.model.PrayerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        PrayerType.DHUHR to AdhanSoundType.MAKKAH,
        PrayerType.ASR to AdhanSoundType.MADINAH,
        PrayerType.MAGHRIB to AdhanSoundType.AL_AQSA,
        PrayerType.ISHA to AdhanSoundType.MAKKAH
    ),
    val prayerAlertEnabled: Map<PrayerType, Boolean> = mapOf(
        PrayerType.FAJR to true,
        PrayerType.SUNRISE to false,
        PrayerType.DHUHR to true,
        PrayerType.ASR to true,
        PrayerType.MAGHRIB to true,
        PrayerType.ISHA to true
    ),
    val minuteOffsets: Map<PrayerType, Int> = mapOf(
        PrayerType.FAJR to 0,
        PrayerType.SUNRISE to 0,
        PrayerType.DHUHR to 0,
        PrayerType.ASR to 0,
        PrayerType.MAGHRIB to 0,
        PrayerType.ISHA to 0
    )
)

class AppSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val methodId = prefs.getString("calc_method", CalculationMethod.MUSLIM_WORLD_LEAGUE.id) ?: CalculationMethod.MUSLIM_WORLD_LEAGUE.id
        val juristicId = prefs.getString("juristic_method", JuristicMethod.STANDARD.id) ?: JuristicMethod.STANDARD.id
        val highLatId = prefs.getString("high_lat_rule", HighLatitudeRule.ANGLE_BASED.id) ?: HighLatitudeRule.ANGLE_BASED.id
        val themeId = prefs.getString("theme_mode", AppThemeMode.SYSTEM.id) ?: AppThemeMode.SYSTEM.id
        val highPriority = prefs.getBoolean("high_priority_sound", true)
        val isGps = prefs.getBoolean("is_gps_auto", true)
        val is24H = prefs.getBoolean("is_24h", false)

        val lat = prefs.getFloat("loc_lat", 21.4225f).toDouble()
        val lng = prefs.getFloat("loc_lng", 39.8262f).toDouble()
        val city = prefs.getString("loc_city", "Makkah") ?: "Makkah"
        val country = prefs.getString("loc_country", "Saudi Arabia") ?: "Saudi Arabia"

        val alertSounds = PrayerType.dailyPrayers.associateWith { prayer ->
            val defaultSound = when (prayer) {
                PrayerType.SUNRISE -> AdhanSoundType.SILENT
                PrayerType.ASR -> AdhanSoundType.MADINAH
                PrayerType.MAGHRIB -> AdhanSoundType.AL_AQSA
                else -> AdhanSoundType.MAKKAH
            }
            val soundId = prefs.getString("sound_${prayer.id}", defaultSound.id) ?: defaultSound.id
            AdhanSoundType.fromId(soundId)
        }

        val alertEnabled = PrayerType.dailyPrayers.associateWith { prayer ->
            val def = prayer != PrayerType.SUNRISE
            prefs.getBoolean("enabled_${prayer.id}", def)
        }

        val offsets = PrayerType.dailyPrayers.associateWith { prayer ->
            prefs.getInt("offset_${prayer.id}", 0)
        }

        return UserSettings(
            calculationMethod = CalculationMethod.fromId(methodId),
            juristicMethod = JuristicMethod.fromId(juristicId),
            highLatitudeRule = HighLatitudeRule.fromId(highLatId),
            themeMode = AppThemeMode.fromId(themeId),
            highPrioritySound = highPriority,
            isGpsAuto = isGps,
            is24HourFormat = is24H,
            currentLocation = LocationInfo(
                latitude = lat,
                longitude = lng,
                cityName = city,
                countryName = country,
                isGpsBased = isGps
            ),
            prayerAlertSounds = alertSounds,
            prayerAlertEnabled = alertEnabled,
            minuteOffsets = offsets
        )
    }

    fun updateCalculationMethod(method: CalculationMethod) {
        prefs.edit().putString("calc_method", method.id).apply()
        _settings.value = _settings.value.copy(calculationMethod = method)
    }

    fun updateJuristicMethod(juristic: JuristicMethod) {
        prefs.edit().putString("juristic_method", juristic.id).apply()
        _settings.value = _settings.value.copy(juristicMethod = juristic)
    }

    fun updateHighLatitudeRule(rule: HighLatitudeRule) {
        prefs.edit().putString("high_lat_rule", rule.id).apply()
        _settings.value = _settings.value.copy(highLatitudeRule = rule)
    }

    fun updateThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.id).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun updateHighPrioritySound(enabled: Boolean) {
        prefs.edit().putBoolean("high_priority_sound", enabled).apply()
        _settings.value = _settings.value.copy(highPrioritySound = enabled)
    }

    fun updateIs24HourFormat(is24H: Boolean) {
        prefs.edit().putBoolean("is_24h", is24H).apply()
        _settings.value = _settings.value.copy(is24HourFormat = is24H)
    }

    fun updatePrayerAlertSound(prayer: PrayerType, sound: AdhanSoundType) {
        prefs.edit().putString("sound_${prayer.id}", sound.id).apply()
        val newMap = _settings.value.prayerAlertSounds.toMutableMap()
        newMap[prayer] = sound
        _settings.value = _settings.value.copy(prayerAlertSounds = newMap)
    }

    fun updatePrayerAlertEnabled(prayer: PrayerType, enabled: Boolean) {
        prefs.edit().putBoolean("enabled_${prayer.id}", enabled).apply()
        val newMap = _settings.value.prayerAlertEnabled.toMutableMap()
        newMap[prayer] = enabled
        _settings.value = _settings.value.copy(prayerAlertEnabled = newMap)
    }

    fun updatePrayerMinuteOffset(prayer: PrayerType, offset: Int) {
        prefs.edit().putInt("offset_${prayer.id}", offset).apply()
        val newMap = _settings.value.minuteOffsets.toMutableMap()
        newMap[prayer] = offset
        _settings.value = _settings.value.copy(minuteOffsets = newMap)
    }

    fun updateLocation(location: LocationInfo) {
        prefs.edit()
            .putFloat("loc_lat", location.latitude.toFloat())
            .putFloat("loc_lng", location.longitude.toFloat())
            .putString("loc_city", location.cityName)
            .putString("loc_country", location.countryName)
            .putBoolean("is_gps_auto", location.isGpsBased)
            .apply()
        _settings.value = _settings.value.copy(
            currentLocation = location,
            isGpsAuto = location.isGpsBased
        )
    }
}
