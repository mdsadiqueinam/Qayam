package tech.sadique.qayam.data.preferences

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.AppThemeMode
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.LocationInfo
import tech.sadique.qayam.data.model.PrayerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

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

private val Context.salahDataStore by preferencesDataStore(
    name = "salah_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

private object Keys {
    val CALC_METHOD = stringPreferencesKey("calc_method")
    val JURISTIC = stringPreferencesKey("juristic_method")
    val HIGH_LAT = stringPreferencesKey("high_lat_rule")
    val THEME = stringPreferencesKey("theme_mode")
    val HIGH_PRIORITY = booleanPreferencesKey("high_priority_sound")
    val GPS_AUTO = booleanPreferencesKey("is_gps_auto")
    val H24 = booleanPreferencesKey("is_24h")
    val LAT = doublePreferencesKey("loc_lat")
    val LNG = doublePreferencesKey("loc_lng")
    val CITY = stringPreferencesKey("loc_city")
    val COUNTRY = stringPreferencesKey("loc_country")
    fun sound(prayer: PrayerType) = stringPreferencesKey("sound_${prayer.id}")
    fun enabled(prayer: PrayerType) = booleanPreferencesKey("enabled_${prayer.id}")
    fun offset(prayer: PrayerType) = intPreferencesKey("offset_${prayer.id}")
}

private fun defaultSound(prayer: PrayerType): AdhanSoundType = when (prayer) {
    PrayerType.SUNRISE, PrayerType.GURUB_E_AFTAB -> AdhanSoundType.SILENT
    PrayerType.ISRAQ -> AdhanSoundType.GENTLE_CHIME
    PrayerType.ASR -> AdhanSoundType.MADINAH
    PrayerType.MAGHRIB -> AdhanSoundType.AL_AQSA
    else -> AdhanSoundType.MAKKAH
}

/** Maps raw preferences to settings. Internal for unit tests (legacy-format coverage). */
internal fun Preferences.toUserSettings(): UserSettings {
    val lat = this[Keys.LAT] ?: 21.4225
    val lng = this[Keys.LNG] ?: 39.8262
    val isGps = this[Keys.GPS_AUTO] ?: true
    return UserSettings(
        calculationMethod = CalculationMethod.fromId(
            this[Keys.CALC_METHOD] ?: CalculationMethod.MUSLIM_WORLD_LEAGUE.id
        ),
        juristicMethod = JuristicMethod.fromId(
            this[Keys.JURISTIC] ?: JuristicMethod.STANDARD.id
        ),
        highLatitudeRule = HighLatitudeRule.fromId(
            this[Keys.HIGH_LAT] ?: HighLatitudeRule.ANGLE_BASED.id
        ),
        themeMode = AppThemeMode.fromId(
            this[Keys.THEME] ?: AppThemeMode.SYSTEM.id
        ),
        highPrioritySound = this[Keys.HIGH_PRIORITY] ?: true,
        isGpsAuto = isGps,
        is24HourFormat = this[Keys.H24] ?: false,
        currentLocation = LocationInfo(
            latitude = lat,
            longitude = lng,
            cityName = this[Keys.CITY] ?: "Makkah",
            countryName = this[Keys.COUNTRY] ?: "Saudi Arabia",
            isGpsBased = isGps
        ),
        prayerAlertSounds = PrayerType.dailyPrayers.associateWith { prayer ->
            AdhanSoundType.fromId(this[Keys.sound(prayer)] ?: defaultSound(prayer).id)
        },
        prayerAlertEnabled = PrayerType.dailyPrayers.associateWith { prayer ->
            this[Keys.enabled(prayer)] ?: prayer.defaultAlertEnabled
        },
        minuteOffsets = PrayerType.dailyPrayers.associateWith { prayer ->
            this[Keys.offset(prayer)] ?: 0
        }
    )
}

class AppSettings(
    private val context: Context,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    val settings: StateFlow<UserSettings> = context.salahDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { it.toUserSettings() }
        .stateIn(scope, SharingStarted.Eagerly, UserSettings())

    /** One-shot read for background callers (receivers) that cannot rely on a warm flow. */
    suspend fun snapshot(): UserSettings =
        context.salahDataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .map { it.toUserSettings() }
            .first()

    /** Clears the store back to defaults (used by tests; future Settings reset action). */
    suspend fun resetToDefaults() {
        context.salahDataStore.edit { it.clear() }
    }

    suspend fun updateCalculationMethod(method: CalculationMethod) {
        context.salahDataStore.edit { it[Keys.CALC_METHOD] = method.id }
    }

    suspend fun updateJuristicMethod(juristic: JuristicMethod) {
        context.salahDataStore.edit { it[Keys.JURISTIC] = juristic.id }
    }

    suspend fun updateHighLatitudeRule(rule: HighLatitudeRule) {
        context.salahDataStore.edit { it[Keys.HIGH_LAT] = rule.id }
    }

    suspend fun updateThemeMode(mode: AppThemeMode) {
        context.salahDataStore.edit { it[Keys.THEME] = mode.id }
    }

    suspend fun updateHighPrioritySound(enabled: Boolean) {
        context.salahDataStore.edit { it[Keys.HIGH_PRIORITY] = enabled }
    }

    suspend fun updateIs24HourFormat(is24H: Boolean) {
        context.salahDataStore.edit { it[Keys.H24] = is24H }
    }

    suspend fun updatePrayerAlertSound(prayer: PrayerType, sound: AdhanSoundType) {
        context.salahDataStore.edit { it[Keys.sound(prayer)] = sound.id }
    }

    suspend fun updatePrayerAlertEnabled(prayer: PrayerType, enabled: Boolean) {
        context.salahDataStore.edit { it[Keys.enabled(prayer)] = enabled }
    }

    suspend fun updatePrayerMinuteOffset(prayer: PrayerType, offset: Int) {
        context.salahDataStore.edit { it[Keys.offset(prayer)] = offset }
    }

    suspend fun updateLocation(location: LocationInfo) {
        context.salahDataStore.edit {
            it[Keys.LAT] = location.latitude
            it[Keys.LNG] = location.longitude
            it[Keys.CITY] = location.cityName
            it[Keys.COUNTRY] = location.countryName
            it[Keys.GPS_AUTO] = location.isGpsBased
        }
    }
}
