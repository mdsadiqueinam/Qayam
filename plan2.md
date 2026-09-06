# Complete SOLID Architecture Refactoring Plan (Dagger Hilt & Clean Architecture)

## Goal Description
Refactor the entire Qayam Android application to strictly follow the **SOLID principles** using **Dagger Hilt** and **Clean Architecture**.
Because backward compatibility with legacy facades is **not needed**, all monolithic God-objects and singletons (`PrayerTimeCalculator`, `AdhanAudioSynthesizer`, `AppSettings`, `AdhanNotificationManager`, `LocationService`) will be completely replaced by single-responsibility domain interfaces, pure Kotlin engines, and specialized data implementations.

---

## The 5 SOLID Principles in Target Architecture

1. **Single Responsibility Principle (SRP)**:
    - Every class has one reason to change.
    - Astronomical mathematics (`AstronomicalEngine`) is isolated from schedule construction (`AstronomicalPrayerCalculator`).
    - PCM waveform generation (`PcmSynthesizer`) is isolated from Android hardware management (`AdhanAudioPlayerImpl`).
    - Alarm scheduling (`AndroidAlarmScheduler`) is completely decoupled from notification presentation (`AndroidPrayerNotificationNotifier`) and channel setup (`NotificationChannelManager`).
    - GPS coordinate retrieval (`FusedLocationProviderImpl`) is isolated from geocoding (`AndroidGeocoderServiceImpl`).
    - ViewModel (`PrayerViewModel`) handles only UI state and user event routing, delegating all business logic to Use Cases.

2. **Open/Closed Principle (OCP)**:
    - Domain logic and UI depend only on interfaces (`PrayerCalculator`, `AudioPlayer`, `AlarmScheduler`, `PrayerNotificationNotifier`, `SettingsRepository`, `LocationProvider`).
    - Swapping an engine (e.g. replacing synthesized audio with Media3/ExoPlayer, or adding a new astronomical calculation algorithm) requires adding a new implementation and changing a single `@Binds` line in Hilt's `RepositoryModule`. No consumer code is touched.

3. **Liskov Substitution Principle (LSP)**:
    - All implementations adhere strictly to the behavioral contracts defined by domain interfaces.
    - Any implementation can be replaced with a test double (Fake/Mock) without breaking caller assumptions or throwing unexpected runtime exceptions.

4. **Interface Segregation Principle (ISP)**:
    - Large interfaces are split into role-specific contracts (e.g. `AlarmScheduler` vs. `PrayerNotificationNotifier`).
    - In Jetpack Compose, monolithic screens are separated into stateful wrappers and pure stateless composables (`MainPrayerContent`, `SettingsContent`) that accept only explicit state objects and event lambdas rather than depending on a concrete ViewModel.

5. **Dependency Inversion Principle (DIP)**:
    - High-level modules (ViewModels, BroadcastReceivers, Services) do not instantiate low-level Android frameworks (`AlarmManager`, `NotificationManager`, `AudioTrack`, `DataStore`, `FusedLocationProviderClient`).
    - Dagger Hilt with KSP provides compile-time dependency injection and graph validation.

---

## User Review Required

> [!IMPORTANT]
> **Complete Replacement of Legacy Classes**:
> The following files will be deleted and replaced with clean SOLID interfaces & implementations:
> - `app/.../data/calculator/PrayerTimeCalculator.kt` -> Deleted.
> - `app/.../data/preferences/AppSettings.kt` -> Deleted.
> - `app/.../audio/AdhanAudioSynthesizer.kt` -> Deleted.
> - `app/.../notification/AdhanNotificationManager.kt` -> Deleted.
> - `app/.../data/location/LocationService.kt` -> Deleted.

> [!TIP]
> **Test Suite Modernization**:
> Existing unit tests (`PrayerCalculatorTest`, `DataStoreRoundtripTest`, `PreferencesMappingTest`, `SettingsDefaultsTest`, `PrayerTickerTest`) will be updated to test the new interfaces and classes directly without using legacy facades.

---

## Proposed Changes

```
app/src/main/java/tech/sadique/qayam/
├── SalahApp.kt                                  [MODIFY: @HiltAndroidApp]
├── MainActivity.kt                              [MODIFY: @AndroidEntryPoint]
├── core/
│   └── domain/
│       ├── model/
│       │   └── PrayerModels.kt                  [EXISTING: Core data classes]
│       ├── calculator/
│       │   ├── PrayerCalculator.kt              [NEW: Domain Interface]
│       │   └── AstronomicalEngine.kt            [NEW: Pure Astronomical Math]
│       ├── repository/
│       │   └── SettingsRepository.kt            [NEW: Domain Interface]
│       ├── audio/
│       │   └── AudioPlayer.kt                   [NEW: Domain Interface]
│       ├── alarm/
│       │   └── AlarmScheduler.kt                [NEW: Domain Interface]
│       ├── notification/
│       │   └── PrayerNotificationNotifier.kt    [NEW: Domain Interface]
│       ├── location/
│       │   ├── LocationProvider.kt              [NEW: Domain Interface]
│       │   └── GeocoderService.kt               [NEW: Domain Interface]
│       └── usecase/
│           ├── GetPrayerScheduleUseCase.kt      [NEW]
│           ├── GetCurrentPrayerStateUseCase.kt  [NEW]
│           ├── SchedulePrayerAlarmsUseCase.kt   [NEW]
│           ├── RefreshLocationUseCase.kt        [NEW]
│           └── AudioPlaybackUseCase.kt          [NEW]
├── data/
│   ├── calculator/
│   │   ├── PrayerTimeCalculator.kt              [DELETE]
│   │   └── AstronomicalPrayerCalculator.kt      [NEW: Implements PrayerCalculator]
│   ├── preferences/
│   │   ├── AppSettings.kt                       [DELETE]
│   │   └── DataStoreSettingsRepository.kt       [NEW: Implements SettingsRepository]
│   ├── audio/
│   │   ├── PcmSynthesizer.kt                    [NEW: Waveform & ADSR Synthesis]
│   │   ├── MelodyProvider.kt                    [NEW: Melodic Note Data]
│   │   └── AdhanAudioPlayerImpl.kt              [NEW: Implements AudioPlayer]
│   ├── alarm/
│   │   └── AndroidAlarmScheduler.kt             [NEW: Implements AlarmScheduler]
│   ├── notification/
│   │   ├── AdhanNotificationManager.kt          [DELETE]
│   │   ├── NotificationChannelManager.kt        [NEW: Notification Channels]
│   │   └── AndroidPrayerNotificationNotifier.kt [NEW: Implements PrayerNotificationNotifier]
│   └── location/
│       ├── LocationService.kt                   [DELETE]
│       ├── CityPresets.kt                       [NEW: Presets Data]
│       ├── FusedLocationProviderImpl.kt         [NEW: Implements LocationProvider]
│       └── AndroidGeocoderServiceImpl.kt        [NEW: Implements GeocoderService]
├── di/
│   ├── RepositoryModule.kt                      [NEW: Hilt @Binds Module]
│   ├── CoroutinesModule.kt                      [NEW: Hilt @Provides Coroutines]
│   └── ReceiverEntryPoint.kt                    [NEW: Hilt @EntryPoint for Receivers]
├── receiver/
│   ├── AdhanAlarmReceiver.kt                    [MODIFY: Uses ReceiverEntryPoint]
│   └── BootReceiver.kt                          [MODIFY: Uses ReceiverEntryPoint]
├── service/
│   └── AdhanPlaybackService.kt                  [MODIFY: @AndroidEntryPoint]
└── ui/
    ├── screens/
    │   ├── MainPrayerScreen.kt                  [MODIFY: State Hoisting]
    │   └── SettingsScreen.kt                    [MODIFY: State Hoisting]
    └── viewmodel/
        └── PrayerViewModel.kt                   [MODIFY: @HiltViewModel + Injected Use Cases]
```

---

### Component 1: Build & Gradle Configuration

#### [MODIFY] `gradle/libs.versions.toml`
Add Hilt 2.60.1 and KSP 2.3.11:
```toml
[versions]
...
hilt = "2.60.1"
ksp = "2.3.11"

[libraries]
...
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }

[plugins]
...
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

#### [MODIFY] `build.gradle.kts` (Root)
```kotlin
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.hilt.android) apply false
}
```

#### [MODIFY] `app/build.gradle.kts`
```kotlin
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt.android)
}

dependencies {
  ...
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  testImplementation(libs.hilt.android.testing)
  kspTest(libs.hilt.compiler)
}
```

---

### Component 2: Domain Layer (`core/domain`)

#### [NEW] `core/domain/calculator/PrayerCalculator.kt`
```kotlin
package tech.sadique.qayam.core.domain.calculator

import tech.sadique.qayam.data.model.*
import java.util.Calendar

interface PrayerCalculator {
    fun calculateSchedule(
        date: Calendar,
        latitude: Double,
        longitude: Double,
        timezoneOffsetHours: Double,
        method: CalculationMethod,
        juristic: JuristicMethod,
        highLatitudeRule: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED,
        minuteOffsets: Map<PrayerType, Int> = emptyMap()
    ): PrayerSchedule

    fun calculateCurrentState(
        currentTime: Calendar,
        schedule: PrayerSchedule,
        latitude: Double,
        longitude: Double
    ): CurrentPrayerState
}
```

#### [NEW] `core/domain/calculator/AstronomicalEngine.kt`
Extracts all pure math and trigonometric calculations:
```kotlin
package tech.sadique.qayam.core.domain.calculator

import kotlin.math.*

object AstronomicalEngine {
    private const val RAD = Math.PI / 180.0
    private const val DEG = 180.0 / Math.PI

    fun dSin(d: Double): Double = sin(d * RAD)
    fun dCos(d: Double): Double = cos(d * RAD)
    fun dTan(d: Double): Double = tan(d * RAD)
    fun dAsin(x: Double): Double = asin(x.coerceIn(-1.0, 1.0)) * DEG
    fun dAcos(x: Double): Double = acos(x.coerceIn(-1.0, 1.0)) * DEG
    fun dAtan2(y: Double, x: Double): Double = atan2(y, x) * DEG

    fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    fun fixHour(h: Double): Double {
        var res = h - 24.0 * floor(h / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    data class SunPosition(val declination: Double, val equationOfTime: Double)

    fun sunPosition(jd: Double): SunPosition {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dSin(g) + 0.020 * dSin(2 * g))

        val e = 23.439 - 0.00000036 * d
        val ra = fixAngle(dAtan2(dCos(e) * dSin(l), dCos(l))) / 15.0

        val declination = dAsin(dSin(e) * dSin(l))
        val equationOfTime = q / 15.0 - ra
        return SunPosition(declination, equationOfTime)
    }

    fun hourAngle(altitude: Double, latitude: Double, declination: Double): Double {
        val cosH = (dSin(altitude) - dSin(latitude) * dSin(declination)) / (dCos(latitude) * dCos(declination))
        return if (cosH < -1.0) 180.0 else if (cosH > 1.0) 0.0 else dAcos(cosH)
    }
}
```

#### [NEW] `core/domain/repository/SettingsRepository.kt`
```kotlin
package tech.sadique.qayam.core.domain.repository

import kotlinx.coroutines.flow.StateFlow
import tech.sadique.qayam.data.model.*
import tech.sadique.qayam.data.preferences.UserSettings

interface SettingsRepository {
    val settings: StateFlow<UserSettings>
    suspend fun snapshot(): UserSettings
    suspend fun resetToDefaults()
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
```

#### [NEW] `core/domain/audio/AudioPlayer.kt`
```kotlin
package tech.sadique.qayam.core.domain.audio

import kotlinx.coroutines.flow.StateFlow
import tech.sadique.qayam.data.model.AdhanSoundType

interface AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val currentlyPlayingSound: StateFlow<AdhanSoundType?>

    fun playSound(
        soundType: AdhanSoundType,
        highPriorityAlarm: Boolean = true,
        volume: Float = 1.0f,
        onComplete: (() -> Unit)? = null
    )
    fun stopSound()
}
```

#### [NEW] `core/domain/alarm/AlarmScheduler.kt`
```kotlin
package tech.sadique.qayam.core.domain.alarm

import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType

interface AlarmScheduler {
    fun setExactAlarm(
        prayer: PrayerType,
        triggerTimeMillis: Long,
        soundType: AdhanSoundType,
        highPriority: Boolean
    )
    fun cancelAlarm(prayer: PrayerType)
    fun canScheduleExactAlarms(): Boolean
    fun isIgnoringBatteryOptimizations(): Boolean
    fun scheduleTestAlarm(delaySeconds: Int, prayerType: PrayerType, soundType: AdhanSoundType)
}
```

#### [NEW] `core/domain/notification/PrayerNotificationNotifier.kt`
```kotlin
package tech.sadique.qayam.core.domain.notification

import android.app.Notification
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType

interface PrayerNotificationNotifier {
    fun buildPrayerNotification(
        prayerType: PrayerType,
        soundType: AdhanSoundType,
        highPriority: Boolean
    ): Notification

    fun showPrayerNotification(
        prayerType: PrayerType,
        soundType: AdhanSoundType,
        highPriority: Boolean
    )

    fun dismissPrayerNotification(prayerType: PrayerType)
    fun areNotificationsEnabled(): Boolean
}
```

#### [NEW] `core/domain/location/LocationProvider.kt` & `GeocoderService.kt`
```kotlin
package tech.sadique.qayam.core.domain.location

import tech.sadique.qayam.data.model.LocationInfo

interface LocationProvider {
    suspend fun getCurrentLocation(): LocationInfo?
}

interface GeocoderService {
    suspend fun getCityAndCountry(latitude: Double, longitude: Double): Pair<String, String>
}
```

#### [NEW] Domain Use Cases (`core/domain/usecase/`)

1. **`GetPrayerScheduleUseCase.kt`**:
```kotlin
package tech.sadique.qayam.core.domain.usecase

import tech.sadique.qayam.core.domain.calculator.PrayerCalculator
import tech.sadique.qayam.data.model.PrayerSchedule
import tech.sadique.qayam.data.preferences.UserSettings
import java.util.Calendar
import javax.inject.Inject

class GetPrayerScheduleUseCase @Inject constructor(
    private val calculator: PrayerCalculator
) {
    operator fun invoke(date: Calendar, settings: UserSettings): PrayerSchedule {
        val loc = settings.currentLocation
        val tzOffset = date.timeZone.getOffset(date.timeInMillis) / 3600000.0
        return calculator.calculateSchedule(
            date = date,
            latitude = loc.latitude,
            longitude = loc.longitude,
            timezoneOffsetHours = tzOffset,
            method = settings.calculationMethod,
            juristic = settings.juristicMethod,
            highLatitudeRule = settings.highLatitudeRule,
            minuteOffsets = settings.minuteOffsets
        )
    }
}
```

2. **`GetCurrentPrayerStateUseCase.kt`**:
```kotlin
package tech.sadique.qayam.core.domain.usecase

import tech.sadique.qayam.core.domain.calculator.PrayerCalculator
import tech.sadique.qayam.data.model.CurrentPrayerState
import tech.sadique.qayam.data.model.LocationInfo
import tech.sadique.qayam.data.model.PrayerSchedule
import java.util.Calendar
import javax.inject.Inject

class GetCurrentPrayerStateUseCase @Inject constructor(
    private val calculator: PrayerCalculator
) {
    operator fun invoke(
        currentTime: Calendar,
        schedule: PrayerSchedule,
        location: LocationInfo
    ): CurrentPrayerState {
        return calculator.calculateCurrentState(
            currentTime = currentTime,
            schedule = schedule,
            latitude = location.latitude,
            longitude = location.longitude
        )
    }
}
```

3. **`SchedulePrayerAlarmsUseCase.kt`**:
```kotlin
package tech.sadique.qayam.core.domain.usecase

import tech.sadique.qayam.core.domain.alarm.AlarmScheduler
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.UserSettings
import java.util.Calendar
import javax.inject.Inject

class SchedulePrayerAlarmsUseCase @Inject constructor(
    private val alarmScheduler: AlarmScheduler,
    private val getPrayerScheduleUseCase: GetPrayerScheduleUseCase
) {
    operator fun invoke(settings: UserSettings) {
        val now = Calendar.getInstance()
        val todaySchedule = getPrayerScheduleUseCase(now, settings)

        val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowSchedule = getPrayerScheduleUseCase(tomorrow, settings)

        val nowMillis = now.timeInMillis

        for (prayer in PrayerType.dailyPrayers) {
            val isEnabled = settings.prayerAlertEnabled[prayer] ?: prayer.defaultAlertEnabled
            if (!isEnabled) {
                alarmScheduler.cancelAlarm(prayer)
                continue
            }

            val todayTime = todaySchedule.getTime(prayer).timeInMillis
            val triggerMillis = if (todayTime > nowMillis) todayTime else tomorrowSchedule.getTime(prayer).timeInMillis
            val soundType = settings.prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH

            alarmScheduler.setExactAlarm(
                prayer = prayer,
                triggerTimeMillis = triggerMillis,
                soundType = soundType,
                highPriority = settings.highPrioritySound
            )
        }
    }
}
```

4. **`RefreshLocationUseCase.kt`**:
```kotlin
package tech.sadique.qayam.core.domain.usecase

import tech.sadique.qayam.core.domain.location.LocationProvider
import tech.sadique.qayam.core.domain.repository.SettingsRepository
import tech.sadique.qayam.data.model.LocationInfo
import javax.inject.Inject

class RefreshLocationUseCase @Inject constructor(
    private val locationProvider: LocationProvider,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): LocationInfo? {
        val location = locationProvider.getCurrentLocation()
        if (location != null) {
            settingsRepository.updateLocation(location)
        }
        return location
    }
}
```

5. **`AudioPlaybackUseCase.kt`**:
```kotlin
package tech.sadique.qayam.core.domain.usecase

import tech.sadique.qayam.core.domain.audio.AudioPlayer
import tech.sadique.qayam.data.model.AdhanSoundType
import javax.inject.Inject

class AudioPlaybackUseCase @Inject constructor(
    private val audioPlayer: AudioPlayer
) {
    val isPlaying = audioPlayer.isPlaying
    val currentlyPlayingSound = audioPlayer.currentlyPlayingSound

    fun playPreview(soundType: AdhanSoundType, highPriority: Boolean) {
        if (isPlaying.value && currentlyPlayingSound.value == soundType) {
            audioPlayer.stopSound()
        } else {
            audioPlayer.playSound(soundType, highPriorityAlarm = highPriority, volume = 1.0f)
        }
    }

    fun stopAudio() {
        audioPlayer.stopSound()
    }
}
```

---

### Component 3: Data Layer Implementations (`data/`)

#### [DELETE] `data/calculator/PrayerTimeCalculator.kt`
#### [NEW] `data/calculator/AstronomicalPrayerCalculator.kt`
Implements `PrayerCalculator` by calling `AstronomicalEngine`:
```kotlin
package tech.sadique.qayam.data.calculator

import tech.sadique.qayam.core.domain.calculator.AstronomicalEngine
import tech.sadique.qayam.core.domain.calculator.PrayerCalculator
import tech.sadique.qayam.data.model.*
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max

@Singleton
class AstronomicalPrayerCalculator @Inject constructor() : PrayerCalculator {
    override fun calculateSchedule(
        date: Calendar,
        latitude: Double,
        longitude: Double,
        timezoneOffsetHours: Double,
        method: CalculationMethod,
        juristic: JuristicMethod,
        highLatitudeRule: HighLatitudeRule,
        minuteOffsets: Map<PrayerType, Int>
    ): PrayerSchedule {
        // [Existing verified astronomical algorithm from PrayerTimeCalculator using AstronomicalEngine]
        ...
    }

    override fun calculateCurrentState(
        currentTime: Calendar,
        schedule: PrayerSchedule,
        latitude: Double,
        longitude: Double
    ): CurrentPrayerState {
        // [Existing verified current state algorithm using AstronomicalEngine]
        ...
    }
}
```

#### [DELETE] `data/preferences/AppSettings.kt`
#### [NEW] `data/preferences/DataStoreSettingsRepository.kt`
Implements `SettingsRepository` using DataStore Preferences:
```kotlin
package tech.sadique.qayam.data.preferences

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import tech.sadique.qayam.core.domain.repository.SettingsRepository
import tech.sadique.qayam.data.model.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.salahDataStore by preferencesDataStore(
    name = "salah_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val settings: StateFlow<UserSettings> = context.salahDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toUserSettings() }
        .stateIn(scope, SharingStarted.Eagerly, UserSettings())

    override suspend fun snapshot(): UserSettings =
        context.salahDataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it.toUserSettings() }
            .first()

    override suspend fun resetToDefaults() {
        context.salahDataStore.edit { it.clear() }
    }

    override suspend fun updateCalculationMethod(method: CalculationMethod) {
        context.salahDataStore.edit { it[Keys.CALC_METHOD] = method.id }
    }

    override suspend fun updateJuristicMethod(juristic: JuristicMethod) {
        context.salahDataStore.edit { it[Keys.JURISTIC] = juristic.id }
    }

    override suspend fun updateHighLatitudeRule(rule: HighLatitudeRule) {
        context.salahDataStore.edit { it[Keys.HIGH_LAT] = rule.id }
    }

    override suspend fun updateThemeMode(mode: AppThemeMode) {
        context.salahDataStore.edit { it[Keys.THEME] = mode.id }
    }

    override suspend fun updateHighPrioritySound(enabled: Boolean) {
        context.salahDataStore.edit { it[Keys.HIGH_PRIORITY] = enabled }
    }

    override suspend fun updateIs24HourFormat(is24H: Boolean) {
        context.salahDataStore.edit { it[Keys.H24] = is24H }
    }

    override suspend fun updatePrayerAlertSound(prayer: PrayerType, sound: AdhanSoundType) {
        context.salahDataStore.edit { it[Keys.sound(prayer)] = sound.id }
    }

    override suspend fun updatePrayerAlertEnabled(prayer: PrayerType, enabled: Boolean) {
        context.salahDataStore.edit { it[Keys.enabled(prayer)] = enabled }
    }

    override suspend fun updatePrayerMinuteOffset(prayer: PrayerType, offset: Int) {
        context.salahDataStore.edit { it[Keys.offset(prayer)] = offset }
    }

    override suspend fun updateLocation(location: LocationInfo) {
        context.salahDataStore.edit {
            it[Keys.LAT] = location.latitude
            it[Keys.LNG] = location.longitude
            it[Keys.CITY] = location.cityName
            it[Keys.COUNTRY] = location.countryName
            it[Keys.GPS_AUTO] = location.isGpsBased
        }
    }
}
```

#### [DELETE] `audio/AdhanAudioSynthesizer.kt`
#### [NEW] `data/audio/PcmSynthesizer.kt` & `MelodyProvider.kt`
- `MelodyProvider`: maps `AdhanSoundType` into note sequences (pitch frequency, duration, envelope parameters).
- `PcmSynthesizer`: converts note sequences into 16-bit PCM ShortBuffers with ADSR envelope, vibrato, and harmonic acoustics.

#### [NEW] `data/audio/AdhanAudioPlayerImpl.kt`
Implements `AudioPlayer`. Manages `AudioTrack`, audio focus requests via `AudioManager`, and system ringtones:
```kotlin
package tech.sadique.qayam.data.audio

import android.content.Context
import android.media.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import tech.sadique.qayam.core.domain.audio.AudioPlayer
import tech.sadique.qayam.data.model.AdhanSoundType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdhanAudioPlayerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentlyPlayingSound = MutableStateFlow<AdhanSoundType?>(null)
    override val currentlyPlayingSound: StateFlow<AdhanSoundType?> = _currentlyPlayingSound.asStateFlow()

    override fun playSound(
        soundType: AdhanSoundType,
        highPriorityAlarm: Boolean,
        volume: Float,
        onComplete: (() -> Unit)?
    ) {
        ...
    }

    override fun stopSound() {
        ...
    }
}
```

#### [DELETE] `notification/AdhanNotificationManager.kt`
#### [NEW] `data/notification/NotificationChannelManager.kt`
Encapsulates creating and registering the 3 notification channels (`ADHAN_CHANNEL_ID`, `ADHAN_VIBRATE_CHANNEL_ID`, `ADHAN_SILENT_CHANNEL_ID`).

#### [NEW] `data/notification/AndroidPrayerNotificationNotifier.kt`
Implements `PrayerNotificationNotifier`. Builds notifications with `NotificationCompat.Builder` and posts them to `NotificationManager`.

#### [NEW] `data/alarm/AndroidAlarmScheduler.kt`
Implements `AlarmScheduler`. Encapsulates `AlarmManager.setAlarmClock` / `setAndAllowWhileIdle`, checking exact alarm permission and battery optimizations.

#### [DELETE] `data/location/LocationService.kt`
#### [NEW] `data/location/CityPresets.kt`
Contains `CITY_PRESETS` list of global cities.
#### [NEW] `data/location/FusedLocationProviderImpl.kt`
Implements `LocationProvider` via `FusedLocationProviderClient`.
#### [NEW] `data/location/AndroidGeocoderServiceImpl.kt`
Implements `GeocoderService` via Android `Geocoder`.

---

### Component 4: Dependency Injection via Hilt (`di/`)

#### [NEW] `di/RepositoryModule.kt`
```kotlin
package tech.sadique.qayam.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.sadique.qayam.core.domain.alarm.AlarmScheduler
import tech.sadique.qayam.core.domain.audio.AudioPlayer
import tech.sadique.qayam.core.domain.calculator.PrayerCalculator
import tech.sadique.qayam.core.domain.location.GeocoderService
import tech.sadique.qayam.core.domain.location.LocationProvider
import tech.sadique.qayam.core.domain.notification.PrayerNotificationNotifier
import tech.sadique.qayam.core.domain.repository.SettingsRepository
import tech.sadique.qayam.data.alarm.AndroidAlarmScheduler
import tech.sadique.qayam.data.audio.AdhanAudioPlayerImpl
import tech.sadique.qayam.data.calculator.AstronomicalPrayerCalculator
import tech.sadique.qayam.data.location.AndroidGeocoderServiceImpl
import tech.sadique.qayam.data.location.FusedLocationProviderImpl
import tech.sadique.qayam.data.notification.AndroidPrayerNotificationNotifier
import tech.sadique.qayam.data.preferences.DataStoreSettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
    @Binds @Singleton abstract fun bindPrayerCalculator(impl: AstronomicalPrayerCalculator): PrayerCalculator
    @Binds @Singleton abstract fun bindAudioPlayer(impl: AdhanAudioPlayerImpl): AudioPlayer
    @Binds @Singleton abstract fun bindAlarmScheduler(impl: AndroidAlarmScheduler): AlarmScheduler
    @Binds @Singleton abstract fun bindPrayerNotificationNotifier(impl: AndroidPrayerNotificationNotifier): PrayerNotificationNotifier
    @Binds @Singleton abstract fun bindLocationProvider(impl: FusedLocationProviderImpl): LocationProvider
    @Binds @Singleton abstract fun bindGeocoderService(impl: AndroidGeocoderServiceImpl): GeocoderService
}
```

#### [NEW] `di/CoroutinesModule.kt`
Provides `@ApplicationScope`, `@IoDispatcher`, `@DefaultDispatcher`.

#### [NEW] `di/ReceiverEntryPoint.kt`
Provides access to dependencies in `BroadcastReceiver`s:
```kotlin
package tech.sadique.qayam.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tech.sadique.qayam.core.domain.audio.AudioPlayer
import tech.sadique.qayam.core.domain.notification.PrayerNotificationNotifier
import tech.sadique.qayam.core.domain.repository.SettingsRepository
import tech.sadique.qayam.core.domain.usecase.SchedulePrayerAlarmsUseCase

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverEntryPoint {
    fun schedulePrayerAlarmsUseCase(): SchedulePrayerAlarmsUseCase
    fun audioPlayer(): AudioPlayer
    fun notificationNotifier(): PrayerNotificationNotifier
    fun settingsRepository(): SettingsRepository
}
```

---

### Component 5: Android System Components

#### [MODIFY] `SalahApp.kt`
```kotlin
package tech.sadique.qayam

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.sadique.qayam.core.domain.repository.SettingsRepository
import tech.sadique.qayam.core.domain.usecase.SchedulePrayerAlarmsUseCase
import tech.sadique.qayam.data.notification.NotificationChannelManager
import javax.inject.Inject

@HiltAndroidApp
class SalahApp : Application() {

    @Inject lateinit var channelManager: NotificationChannelManager
    @Inject lateinit var schedulePrayerAlarmsUseCase: SchedulePrayerAlarmsUseCase
    @Inject lateinit var settingsRepository: SettingsRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        channelManager.createNotificationChannels()
        applicationScope.launch {
            try {
                val settings = settingsRepository.snapshot()
                schedulePrayerAlarmsUseCase(settings)
            } catch (e: Exception) {
                android.util.Log.e("SalahApp", "Failed to arm alarms on startup", e)
            }
        }
    }
}
```

#### [MODIFY] `MainActivity.kt`
Annotated with `@AndroidEntryPoint`. ViewModel is injected via standard `by viewModels()`.

#### [MODIFY] `service/AdhanPlaybackService.kt`
Annotated with `@AndroidEntryPoint`. Constructor/field injects `AudioPlayer` and `PrayerNotificationNotifier`.

#### [MODIFY] `receiver/AdhanAlarmReceiver.kt` & `receiver/BootReceiver.kt`
Uses `EntryPointAccessors.fromApplication(context, ReceiverEntryPoint::class.java)` to invoke `schedulePrayerAlarmsUseCase` and play audio without creating any concrete classes.

---

### Component 6: Presentation / UI Layer (`ui/`)

#### [MODIFY] `ui/viewmodel/PrayerViewModel.kt`
```kotlin
package tech.sadique.qayam.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.sadique.qayam.core.domain.alarm.AlarmScheduler
import tech.sadique.qayam.core.domain.notification.PrayerNotificationNotifier
import tech.sadique.qayam.core.domain.repository.SettingsRepository
import tech.sadique.qayam.core.domain.usecase.*
import tech.sadique.qayam.data.model.*
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class PrayerViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val getPrayerScheduleUseCase: GetPrayerScheduleUseCase,
    private val getCurrentPrayerStateUseCase: GetCurrentPrayerStateUseCase,
    private val refreshLocationUseCase: RefreshLocationUseCase,
    private val schedulePrayerAlarmsUseCase: SchedulePrayerAlarmsUseCase,
    private val audioPlaybackUseCase: AudioPlaybackUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val notificationNotifier: PrayerNotificationNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private val _tickerState = MutableStateFlow(PrayerTickerState())
    val tickerState: StateFlow<PrayerTickerState> = _tickerState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        var firstSettings = true
        viewModelScope.launch {
            settingsRepository.settings.collect { newSettings ->
                _uiState.value = _uiState.value.copy(settings = newSettings)
                recalculateSchedule()
                schedulePrayerAlarmsUseCase(newSettings)
                if (firstSettings) {
                    firstSettings = false
                    if (newSettings.isGpsAuto) refreshGpsLocation()
                }
            }
        }
        viewModelScope.launch {
            audioPlaybackUseCase.isPlaying.collect { isPlaying ->
                _uiState.value = _uiState.value.copy(isPlayingSound = isPlaying)
            }
        }
        viewModelScope.launch {
            audioPlaybackUseCase.currentlyPlayingSound.collect { sound ->
                _uiState.value = _uiState.value.copy(playingSoundType = sound)
            }
        }
        startClockTicker()
    }

    private fun startClockTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                refreshTicker(Calendar.getInstance())
                val now = System.currentTimeMillis()
                delay(1000 - (now % 1000))
            }
        }
    }

    private fun refreshTicker(now: Calendar) {
        val schedule = _uiState.value.schedule
        val loc = _uiState.value.settings.currentLocation
        val currentState = if (schedule != null) {
            getCurrentPrayerStateUseCase(currentTime = now, schedule = schedule, location = loc)
        } else null
        _tickerState.value = PrayerTickerState(currentTimeMillis = now.timeInMillis, currentState = currentState)
    }

    fun recalculateSchedule() {
        val now = Calendar.getInstance()
        val schedule = getPrayerScheduleUseCase(now, _uiState.value.settings)
        _uiState.value = _uiState.value.copy(schedule = schedule)
        refreshTicker(now)
    }

    fun refreshGpsLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLocationLoading = true, locationErrorMessage = null)
            val gpsLoc = refreshLocationUseCase()
            if (gpsLoc != null) {
                _uiState.value = _uiState.value.copy(isLocationLoading = false)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLocationLoading = false,
                    locationErrorMessage = "GPS unavailable. Using cached coordinates."
                )
            }
        }
    }

    fun selectPresetLocation(location: LocationInfo) = viewModelScope.launch { settingsRepository.updateLocation(location) }
    fun updateCalculationMethod(method: CalculationMethod) = viewModelScope.launch { settingsRepository.updateCalculationMethod(method) }
    fun updateJuristicMethod(juristic: JuristicMethod) = viewModelScope.launch { settingsRepository.updateJuristicMethod(juristic) }
    fun updateHighLatitudeRule(rule: HighLatitudeRule) = viewModelScope.launch { settingsRepository.updateHighLatitudeRule(rule) }
    fun updateThemeMode(mode: AppThemeMode) = viewModelScope.launch { settingsRepository.updateThemeMode(mode) }
    fun updateHighPrioritySound(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateHighPrioritySound(enabled) }
    fun updateIs24HourFormat(is24H: Boolean) = viewModelScope.launch { settingsRepository.updateIs24HourFormat(is24H) }
    fun updatePrayerAlertSound(prayer: PrayerType, sound: AdhanSoundType) = viewModelScope.launch { settingsRepository.updatePrayerAlertSound(prayer, sound) }
    fun updatePrayerAlertEnabled(prayer: PrayerType, enabled: Boolean) = viewModelScope.launch { settingsRepository.updatePrayerAlertEnabled(prayer, enabled) }
    fun updatePrayerMinuteOffset(prayer: PrayerType, offset: Int) = viewModelScope.launch { settingsRepository.updatePrayerMinuteOffset(prayer, offset) }

    fun playPreviewSound(soundType: AdhanSoundType) = audioPlaybackUseCase.playPreview(soundType, _uiState.value.settings.highPrioritySound)
    fun stopPreviewSound() = audioPlaybackUseCase.stopAudio()

    fun canScheduleExactAlarms(): Boolean = alarmScheduler.canScheduleExactAlarms()
    fun isIgnoringBatteryOptimizations(): Boolean = alarmScheduler.isIgnoringBatteryOptimizations()

    fun scheduleTestAlarm(delaySeconds: Int = 10) {
        val nextPrayer = _tickerState.value.currentState?.nextPrayer ?: PrayerType.FAJR
        val soundType = _uiState.value.settings.prayerAlertSounds[nextPrayer] ?: AdhanSoundType.TAKBEER_ONLY
        alarmScheduler.scheduleTestAlarm(delaySeconds, nextPrayer, soundType)
    }

    override fun onCleared() {
        tickerJob?.cancel()
        audioPlaybackUseCase.stopAudio()
    }
}
```

#### [MODIFY] `ui/screens/MainPrayerScreen.kt` & `SettingsScreen.kt`
Apply **Interface Segregation (ISP)** via state hoisting:
- The entry composables `MainPrayerScreen(viewModel)` and `SettingsScreen(viewModel)` observe state from the ViewModel and pass plain state and callbacks into pure stateless composables:
  `MainPrayerContent(uiState, tickerFlow, onRefreshLocation, onNavigateToSettings, onToggleAlert, onSoundClick, onStopAudio)`
- Screens no longer tightly bind internal components to the concrete ViewModel.

---

### Component 7: Tests Modernization (`test/`)

#### [MODIFY] `PrayerCalculatorTest.kt`
Updated to directly instantiate `AstronomicalPrayerCalculator()`:
```kotlin
val calculator = AstronomicalPrayerCalculator()
val s = calculator.calculateSchedule(...)
```

#### [MODIFY] `DataStoreRoundtripTest.kt`
Updated to directly test `DataStoreSettingsRepository(context)`:
```kotlin
val repo = DataStoreSettingsRepository(context)
repo.updateCalculationMethod(CalculationMethod.KARACHI)
val snapshot = repo.snapshot()
assertEquals(CalculationMethod.KARACHI, snapshot.calculationMethod)
```

#### [MODIFY] `PreferencesMappingTest.kt` & `SettingsDefaultsTest.kt`
Updated to test `DataStoreSettingsRepository` and `UserSettings` mappings.

#### [MODIFY] `PrayerTickerTest.kt`
Updated to instantiate `PrayerViewModel` directly by passing mocked or fake Use Cases:
- Eliminates reliance on complex Robolectric ViewModel stores.
- Validates the ticker and state flow in complete isolation.

#### [NEW] `SchedulePrayerAlarmsUseCaseTest.kt`
Pure Kotlin unit test verifying prayer alarm trigger time calculations with fake `AlarmScheduler` (0 Robolectric overhead, executes in milliseconds).

#### [NEW] `RefreshLocationUseCaseTest.kt`
Pure Kotlin unit test verifying GPS fallback and repository update with fake `LocationProvider` and fake `SettingsRepository`.

---

## Verification Plan

### Automated Tests
1. **Execute All Unit & Screenshot Tests**:
   ```bash
   JAVA_HOME="/home/sadique/.local/share/JetBrains/Toolbox/apps/android-studio/jbr" ./gradlew testDebugUnitTest
   ```
2. **Verify Roborazzi Screenshot Tests**:
    - `HeroItemScreenshotTest` and `GreetingScreenshotTest` continue to render identical pixels against goldens.

### Manual Verification
1. App builds and packages with 0 errors or warnings under KSP.
2. Launch app: verify prayer schedule appears, live canvas displays sun/stars according to local time, and countdown decrements each second.
3. Open Settings: toggle juristic method, calculation method, and theme mode. Verify changes immediately reflect in the UI and persist across cold restarts.
4. Audio: test Adhan audio preview and verify playback starts and stops on button tap.