package tech.sadique.qayam.data.model

import java.util.Calendar

enum class PrayerType(
    val id: String,
    val displayName: String,
    val arabicName: String,
    val isMainPrayer: Boolean = true,
    val subtitle: String? = null,
) {
    FAJR("fajr", "Fajr", "الفجر", true),
    SUNRISE("sunrise", "Sunrise", "شُرُوق الشَّمْس", false),
    ISRAQ("israq", "Israq", "الإشراق", false),
    DHUHR("dhuhr", "Dhuhr", "الظهر", true),
    ASR("asr", "Asr", "العصر", true),
    GURUB_E_AFTAB("sunset", "Sunset", "غُروب الشَّمْس", false),
    MAGHRIB("maghrib", "Maghrib", "المغرب", true),
    ISHA("isha", "Isha", "العشاء", true),
    ;

    companion object {
        fun fromId(id: String): PrayerType = entries.find { it.id.equals(id, ignoreCase = true) } ?: FAJR

        val dailyPrayers: List<PrayerType>
            get() = listOf(FAJR, SUNRISE, ISRAQ, DHUHR, ASR, GURUB_E_AFTAB, MAGHRIB, ISHA)

        val obligatories: List<PrayerType>
            get() = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    }

    /** Single source of truth: sunrise/sunset markers never alert by default. */
    val defaultAlertEnabled: Boolean
        get() = this != SUNRISE && this != GURUB_E_AFTAB
}

enum class AdhanSoundType(val id: String, val title: String, val description: String) {
    MAKKAH("makkah", "Makkah Al-Mukarramah", "Adhan from Masjid Al-Haram"),
    MADINAH("madinah", "Madinah Al-Munawwarah", "Adhan from Al-Masjid An-Nabawi"),
    AL_AQSA("al_aqsa", "Al-Aqsa Al-Quds", "Adhan from Al-Aqsa"),
    SYSTEM_ALARM("system", "Device Alarm Ringtone", "Default system alarm audio"),
    VIBRATE_ONLY("vibrate", "Vibrate Only", "Haptic notification without sound"),
    SILENT("silent", "Silent", "Visual notification only"),
    ;

    companion object {
        fun fromId(id: String): AdhanSoundType = entries.find { it.id.equals(id, ignoreCase = true) } ?: MAKKAH
    }
}

enum class CalculationMethod(
    val id: String,
    val title: String,
    val subtitle: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutesAfterMaghrib: Double? = null,
    val maghribAngle: Double? = null,
) {
    MUSLIM_WORLD_LEAGUE(
        id = "mwl",
        title = "Muslim World League (MWL)",
        subtitle = "Europe, Far East, parts of USA (Fajr: 18°, Isha: 17°)",
        fajrAngle = 18.0,
        ishaAngle = 17.0,
    ),
    ISNA(
        id = "isna",
        title = "Islamic Society of North America (ISNA)",
        subtitle = "North America standard (Fajr: 15°, Isha: 15°)",
        fajrAngle = 15.0,
        ishaAngle = 15.0,
    ),
    EGYPT(
        id = "egypt",
        title = "Egyptian General Authority of Survey",
        subtitle = "Egypt, Africa, Middle East, Syria (Fajr: 19.5°, Isha: 17.5°)",
        fajrAngle = 19.5,
        ishaAngle = 17.5,
    ),
    UMM_AL_QURA(
        id = "umm_al_qura",
        title = "Umm al-Qura University, Makkah",
        subtitle = "Arabian Peninsula standard (Fajr: 18.5°, Isha: 90 min)",
        fajrAngle = 18.5,
        ishaAngle = 0.0,
        ishaMinutesAfterMaghrib = 90.0,
    ),
    KARACHI(
        id = "karachi",
        title = "Univ. of Islamic Sciences, Karachi",
        subtitle = "Pakistan, India, Bangladesh, Afghanistan (Fajr: 18°, Isha: 18°)",
        fajrAngle = 18.0,
        ishaAngle = 18.0,
    ),
    DUBAI(
        id = "dubai",
        title = "Gulf / Dubai (UAE)",
        subtitle = "United Arab Emirates (Fajr: 18.2°, Isha: 18.2°)",
        fajrAngle = 18.2,
        ishaAngle = 18.2,
    ),
    TEHRAN(
        id = "tehran",
        title = "Institute of Geophysics, Tehran",
        subtitle = "Iran & Shia communities (Fajr: 17.7°, Maghrib: 4.5°, Isha: 14°)",
        fajrAngle = 17.7,
        ishaAngle = 14.0,
        maghribAngle = 4.5,
    ),
    FRANCE_UOIF(
        id = "france",
        title = "Union des Organisations Islamiques (France)",
        subtitle = "France & Western Europe (Fajr: 12°, Isha: 12°)",
        fajrAngle = 12.0,
        ishaAngle = 12.0,
    ),
    ;

    companion object {
        fun fromId(id: String): CalculationMethod = entries.find {
            it.id.equals(id, ignoreCase = true)
        } ?: MUSLIM_WORLD_LEAGUE
    }
}

enum class JuristicMethod(val id: String, val title: String, val description: String, val shadowFactor: Double) {
    STANDARD("standard", "Standard (Shafi'i, Maliki, Hanbali)", "Asr shadow length is 1x object height", 1.0),
    HANAFI("hanafi", "Hanafi", "Asr shadow length is 2x object height", 2.0),
    ;

    companion object {
        fun fromId(id: String): JuristicMethod = entries.find { it.id.equals(id, ignoreCase = true) } ?: STANDARD
    }
}

enum class HighLatitudeRule(val id: String, val title: String, val description: String) {
    ANGLE_BASED("angle_based", "Angle Based", "Proportional portion of night"),
    MID_NIGHT("mid_night", "Middle of the Night", "Limit to first/second half of night"),
    ONE_SEVENTH("one_seventh", "One Seventh of Night", "Limit to 1/7th of night"),
    NONE("none", "None", "No high latitude adjustments"),
    ;

    companion object {
        fun fromId(id: String): HighLatitudeRule = entries.find { it.id.equals(id, ignoreCase = true) } ?: ANGLE_BASED
    }
}

enum class AppThemeMode(val id: String, val title: String) {
    SYSTEM("system", "System Default"),
    LIGHT("light", "Light Desert Dawn"),
    DARK("dark", "Dark Sapphire"),
    NIGHT_MOSQUE("night_mosque", "Midnight Mosque (Emerald & Gold)"),
    ;

    companion object {
        fun fromId(id: String): AppThemeMode = entries.find { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
    }
}

data class PrayerSchedule(
    val date: Calendar,
    val fajr: Calendar,
    val sunrise: Calendar,
    val israq: Calendar,
    val dhuhr: Calendar,
    val asr: Calendar,
    val gurubAftab: Calendar,
    val maghrib: Calendar,
    val isha: Calendar,
    val midnight: Calendar,
) {
    fun getTime(type: PrayerType): Calendar = when (type) {
        PrayerType.FAJR -> fajr
        PrayerType.SUNRISE -> sunrise
        PrayerType.ISRAQ -> israq
        PrayerType.DHUHR -> dhuhr
        PrayerType.ASR -> asr
        PrayerType.GURUB_E_AFTAB -> gurubAftab
        PrayerType.MAGHRIB -> maghrib
        PrayerType.ISHA -> isha
    }

    fun getAllTimes(): List<Pair<PrayerType, Calendar>> = listOf(
        PrayerType.FAJR to fajr,
        PrayerType.SUNRISE to sunrise,
        PrayerType.ISRAQ to israq,
        PrayerType.DHUHR to dhuhr,
        PrayerType.ASR to asr,
        PrayerType.GURUB_E_AFTAB to gurubAftab,
        PrayerType.MAGHRIB to maghrib,
        PrayerType.ISHA to isha,
    )
}

data class CurrentPrayerState(
    val currentPrayer: PrayerType,
    val nextPrayer: PrayerType,
    val nextPrayerTime: Calendar,
    val timeRemainingMillis: Long,
    val totalWindowDurationMillis: Long,
    val progressInWindow: Float,
    val sunAltitudeDegrees: Double,
    val sunProgressPercent: Float, // 0.0 at sunrise, 0.5 at dhuhr noon, 1.0 at sunset, or night arc
    val isDaytime: Boolean,
)

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val cityName: String,
    val countryName: String,
    val isGpsBased: Boolean = true,
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
)
