package tech.sadique.qayam.data.preferences

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.AppThemeMode
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.LocationInfo
import tech.sadique.qayam.data.model.PrayerType
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.qayamDataStore by preferencesDataStore(
    name = "qayam_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

@Singleton
open class DataStoreSettingsRepository @Inject constructor(@ApplicationContext internal val appContext: Context) :
    SettingsRepository {
    private val context: Context get() = appContext

    override val settings: Flow<UserSettings> = context.qayamDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { it.toUserSettings() }

    override suspend fun snapshot(): UserSettings = context.qayamDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { it.toUserSettings() }
        .first()

    override suspend fun updateCalculationMethod(method: CalculationMethod) {
        context.qayamDataStore.edit { it[SettingsKeys.CALC_METHOD] = method.id }
    }

    override suspend fun updateJuristicMethod(juristic: JuristicMethod) {
        context.qayamDataStore.edit { it[SettingsKeys.JURISTIC] = juristic.id }
    }

    override suspend fun updateHighLatitudeRule(rule: HighLatitudeRule) {
        context.qayamDataStore.edit { it[SettingsKeys.HIGH_LAT] = rule.id }
    }

    override suspend fun updateThemeMode(mode: AppThemeMode) {
        context.qayamDataStore.edit { it[SettingsKeys.THEME] = mode.id }
    }

    override suspend fun updateHighPrioritySound(enabled: Boolean) {
        context.qayamDataStore.edit { it[SettingsKeys.HIGH_PRIORITY] = enabled }
    }

    override suspend fun updateIs24HourFormat(is24H: Boolean) {
        context.qayamDataStore.edit { it[SettingsKeys.H24] = is24H }
    }

    override suspend fun updatePrayerAlertSound(prayer: PrayerType, sound: AdhanSoundType) {
        context.qayamDataStore.edit { it[SettingsKeys.sound(prayer)] = sound.id }
    }

    override suspend fun updatePrayerAlertEnabled(prayer: PrayerType, enabled: Boolean) {
        context.qayamDataStore.edit { it[SettingsKeys.enabled(prayer)] = enabled }
    }

    override suspend fun updatePrayerMinuteOffset(prayer: PrayerType, offset: Int) {
        context.qayamDataStore.edit { it[SettingsKeys.offset(prayer)] = offset }
    }

    override suspend fun updateLocation(location: LocationInfo) {
        context.qayamDataStore.edit {
            it[SettingsKeys.LAT] = location.latitude
            it[SettingsKeys.LNG] = location.longitude
            it[SettingsKeys.CITY] = location.cityName
            it[SettingsKeys.COUNTRY] = location.countryName
            it[SettingsKeys.GPS_AUTO] = location.isGpsBased
        }
    }
}

/** Clears the store back to defaults (used by tests; future Settings reset action). */
internal suspend fun DataStoreSettingsRepository.resetToDefaults() {
    appContext.qayamDataStore.edit { it.clear() }
}
