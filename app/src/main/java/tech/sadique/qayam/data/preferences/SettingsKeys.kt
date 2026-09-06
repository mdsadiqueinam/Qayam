package tech.sadique.qayam.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import tech.sadique.qayam.data.model.PrayerType

internal object SettingsKeys {
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
