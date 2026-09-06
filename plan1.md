# SOLID Refactor Plan — Qayam (Hilt-targeted)

Status: PLAN ONLY — no code changed yet.
Target: refactor the whole codebase along SOLID lines, using Hilt for
Dependency Inversion. Ordering is friction-aware: build config first,
then seams, then splits, then UI, then tests.

Guiding constraint: **no backward compatibility is needed — the app is
unreleased.** Stored data, preference keys/schemas, screenshot goldens,
and public API shapes may change freely. No migration code
(`SharedPreferencesMigration`, legacy key fallbacks, dual-read paths)
shall be written; goldens are re-recorded whenever UI moves instead of
being treated as frozen proof.

**Pre-P0 Mechanical Renames (Standalone Step)**:
Before P0 begins, execute a zero-logic mechanical rename:
- `SalahApp` → `QayamApp` (class + file name, `AndroidManifest.xml` `android:name=".QayamApp"`, static channel ID references).
- DataStore name `"salah_prefs"` → `"qayam_prefs"` (allowed under guiding constraint as app is unreleased; wipes any local test device preferences, eliminating legacy naming debt).

Conventions used below: `S` = Single-Responsibility, `O` = Open/Closed,
`L` = Liskov, `I` = Interface Segregation, `D` = Dependency Inversion.

---

## 0. Baseline (where the code stands)

- 24 Kotlin files, no interfaces, no DI framework (verified by grep:
  zero `interface`, `@Inject`, `@Module`, Hilt/Koin/Dagger references in
  `app/src`; only KSP-era catalog leftovers, since removed).
- Storage architecture: The app uses Jetpack DataStore Preferences
  (`Context.qayamDataStore by preferencesDataStore(name = "qayam_prefs")`
  with coroutine `edit {}` blocks, tested in `DataStoreRoundtripTest`).
  The pure mapping function `Preferences.toUserSettings()` transforms
  DataStore `Preferences` (not SharedPreferences). P2's split will
  preserve this DataStore flow/edit pattern directly.
- Only DIP-respecting boundary today:
  `AdhanNotificationManager.scheduleUpcomingAlarms(settings: UserSettings)`
  takes an immutable snapshot DTO, and all callers (`PrayerViewModel`,
  both receivers, `QayamApp`) pass `snapshot()` first. Preserve this
  pattern everywhere new code is cut.
- Pure, already-testable units (do not break): `PrayerTimeCalculator`
  (`object`, no Android imports), `Preferences.toUserSettings()`
  (`internal`, covered by `PreferencesMappingTest`).
- God classes and SRP/ISP violations, by responsibility count:
  - `PrayerViewModel` (~240 lines, ~7 jobs: settings fan-out, ticker,
    recalculation, GPS coordination, 9 write-throughs, audio preview,
    alarm facade) + concrete `new`s at :53–55.
  - `AdhanNotificationManager` (291 lines, 6 jobs: notification channel
    creation, notification building, show/dismiss, scheduling orchestration,
    exact-alarm gateway, capability queries) + static coupling to
    `QayamApp`/`MainActivity`.
  - `AdhanAudioSynthesizer` (453-line `object`, 6 jobs: orchestration,
    teardown, audio focus, ringtone backend, melody data, PCM engine).
  - `LocationService` (142 lines, 2 distinct responsibilities behind 1 class:
    GPS location coordinate acquisition via `FusedLocationProviderClient` and
    reverse geocoding string lookup via Android `Geocoder`). This ISP/SRP
    violation must be separated into `LocationProvider` (returning raw coordinates)
    and `GeocoderService` (returning city/country).
  - `SettingsScreen` (937 lines, largest file: 7 inline sections +
    dialog duplicating Main's bottom sheet), `MainPrayerScreen`
    (~800 lines: permissions, banner, list assembly, chip, sheet).
  - `AppSettings` file (4 concerns: model, keys, mapper, DataStore repository).
- UI-layer ISP violation: both screens take whole `PrayerViewModel`;
  leaf components (`PrayerCard`, `CountdownTimerView`, section
  composables taking `StateFlow`/`UserSettings` + lambdas) are the target
  pattern.
- No custom type hierarchy exists, so no LSP violation is possible
  today — but every new interface must ship with fake contracts
  (see §5).

---

## P0 — Hilt scaffolding, zero behavior change (D, build)

Goal: the DI machinery compiles and injects nothing yet.

1. **Hard Gate (Blocking Step 0)**:
   Before editing any Gradle configuration or `libs.versions.toml`:
   - Resolve and verify the exact KSP release matching Kotlin 2.4.10 on
     Maven Central / the KSP releases page.
   - Confirm that the Hilt release in that range supports that KSP line.
   - Only after this compatibility check passes, edit `libs.versions.toml`.
2. `gradle/libs.versions.toml`:
   - Add `ksp` version and plugin `com.google.devtools.ksp`.
   - Add `hilt` version (2.6x line with verified KSP support), library
     `hilt-android` (`com.google.dagger:hilt-android`), compiler
     `hilt-compiler` (`com.google.dagger:hilt-compiler`), and plugin
     `hilt` (`com.google.dagger.hilt.android`).
   - *Guardrail*: Do **not** add `hilt-android-testing` — P4 uses plain
     fakes and direct constructor injection.
3. `build.gradle.kts` (root): declare `ksp` and `hilt` apply-false entries.
4. `app/build.gradle.kts`: apply `ksp` + `hilt` plugins; add
   `implementation(libs.hilt.android)` and `ksp(libs.hilt.compiler)`.
5. Entry points, annotations only:
   - `QayamApp` → `@HiltAndroidApp`.
   - `MainActivity`, `AdhanAlarmReceiver`, `BootReceiver`,
     `AdhanPlaybackService` → `@AndroidEntryPoint`.
6. Verify: `./gradlew :app:assembleDebug` green. No class logic moves.
   Risk note: Hilt + KSP add configure/compile time to every build.

## P1 — DIP: abstractions + bindings, no logic moves (D, I)

Goal: every high-level class depends on an abstraction; production
bindings keep today's behavior byte-identical.

1. Declare five narrow interfaces (ISP-sized):
   - `SettingsRepository` — `val settings: Flow<UserSettings>`,
     `suspend fun snapshot(): UserSettings`, and the 10 `suspend fun update*`
     write-through methods (the 9 settings updates + `updateLocation`).
     (Backed by `AppSettings`.)
     *Guardrail*: `settings` stays a cold `Flow`, not `StateFlow`, keeping
     fakes lightweight. No speculative methods like `resetToDefaults()` on
     the domain interface.
   - `LocationProvider` — `suspend fun getCurrentLocation(): Coordinates?`
     (using domain `data class Coordinates(val latitude: Double, val longitude: Double, val altitude: Double = 0.0)`).
     (Backed by `LocationService` coordinate retrieval.)
   - `GeocoderService` — `suspend fun getCityAndCountry(lat: Double, lng: Double): Pair<String, String>`.
     (Backed by `LocationService` reverse geocoding.)
   - `AlarmScheduler` — `scheduleUpcomingAlarms(settings: UserSettings)`,
     `canScheduleExactAlarms(): Boolean`, `areNotificationsEnabled(): Boolean`,
     `isIgnoringBatteryOptimizations(): Boolean`, `scheduleTestAlarm(...)`.
     (Backed by `AdhanNotificationManager`.)
   - `AudioPlayer` — `playSound(soundType: AdhanSoundType, highPriority: Boolean = true, volume: Float = 1.0f, onComplete: (() -> Unit)? = null)`,
     `stopSound()`, `val isPlaying: StateFlow<Boolean>`,
     `val currentlyPlayingSound: StateFlow<AdhanSoundType?>`.
     (Context is encapsulated internally via `@ApplicationContext` injection into the implementation,
     freeing `PrayerViewModel` from needing `getApplication()` and allowing it to extend plain `ViewModel()`.)
     (Backed by `AdhanAudioSynthesizer`.)
2. Hilt modules: one `@Module @InstallIn(SingletonComponent::class)`
   per area (preferred over a single `AppModule`, per S), binding each
   interface to its current implementation via `@Binds`.
   - Provide `@ApplicationContext Context` (already the pattern).
   - Provide a qualified `@ApplicationScope CoroutineScope`
     (`SupervisorJob() + Dispatchers.Default`) and delete the ad-hoc
     trio: `receiverScope` (both receivers), `applicationScope`
     (`QayamApp`), `synthScope` (audio object).
3. Rewire consumers (constructor/field injection only):
   - `PrayerViewModel` → `@HiltViewModel @Inject` constructor taking
     the domain interfaces (`SettingsRepository`, `LocationProvider`, `GeocoderService`, `AlarmScheduler`, `AudioPlayer`);
     change base class from `AndroidViewModel(application)` to standard `ViewModel()`.
     Delete concrete `new`s (`PrayerViewModel.kt:53–55`) and direct `object` calls
     (`AdhanAudioSynthesizer.*` at :82, :87, :204, :206, :216, :239).
     Keep **one-line ViewModel delegates** for the 9 write-through
     settings updates (`fun updateCalculationMethod(m) = viewModelScope.launch { settingsRepository.updateCalculationMethod(m) }`).
     Location writes are handled via `RefreshLocationUseCase` / preset selection.
     This keeps the VM boundary simple and avoids unneeded abstraction.
     `MainActivity` keeps `by viewModels()` — now Hilt-provided.
   - Test continuity: Update `PrayerTickerTest` in P1 to instantiate `PrayerViewModel` via constructor
     injection (passing instances/fakes) rather than `AndroidViewModelFactory.getInstance(app)`.
   - `QayamApp`: inject `@ApplicationScope lateinit var applicationScope: CoroutineScope`
     and the `AlarmScheduler` for startup alarm arming in P1 (switches to
     `SchedulePrayerAlarmsUseCase` in P2). Pinning the scope eliminates ad-hoc startup scope creation.
   - Receivers: inject interfaces via `@Inject lateinit var` (or `ReceiverEntryPoint`);
     call `super.onReceive(context, intent)` when annotated with `@AndroidEntryPoint` so Hilt can inject dependencies;
     delete `AppSettings(...)` / `AdhanNotificationManager(...)` from `onReceive`.
   - `AdhanPlaybackService`: **leave on the current singleton facade during P1**.
     Move it to injected `AudioPlayer` in P2 when the audio split lands, keeping
     P1 purely mechanical.
4. Verify: full suite (`testDebugUnitTest`, `verifyRoborazziDebug`,
   `assembleDebug`) green. Goldens may be re-recorded if DI wiring
   shifts any rendering timing; no behavior change is intended, but
   exact-pixel stability is not a gate (see guiding constraint).

## P2 — SRP: split the god classes (S, D)

Goal: each class has one reason to change. All new units are
`@Singleton`/`@ActivityRetainedScoped` injectables, safe to fake in P4.

1. `PrayerViewModel` (~240 → ~100 lines, merges state and routes events only):
   - Extract `RecalculateScheduleUseCase` (timezone + calculator call,
     today :125–144), `TickerManager` (`tickerJob`, second alignment,
     `refreshTicker`, today :96–123), `RefreshLocationUseCase` (GPS fetch via
     `LocationProvider.getCurrentLocation()` returning `Coordinates?` + reverse geocoding via
     `GeocoderService.getCityAndCountry()` + `LocationInfo` assembly and `SettingsRepository.updateLocation`
     write, today :146–164; matches P4 test), `AudioPreviewController`
     (preview toggle + audio-state observe, today :81–90, :202–217).
   - Maintain the 9 simple one-line VM delegates to `SettingsRepository`
     (resolved: no `SettingsEditor` indirection needed).
2. `AdhanNotificationManager` (291 lines, 6 jobs) → split into 5 focused components:
   - `NotificationChannelManager`: handles high-priority, vibrate, and silent
     notification channel creation. Channels are an **app-start** concern, not a
     notification-building concern. Injected into and initialized from `QayamApp.onCreate()`.
   - `PrayerNotificationNotifier`: builds notification title, body, styles, and PendingIntents,
     and handles show/dismiss (`notify()` and `cancel()`) calls (today :40–120).
   - `SchedulePrayerAlarmsUseCase`: absorbs the today/tomorrow trigger-time calculation +
     disabled-prayer cancellation logic (today :126–176). Named as a use case symmetric
     with `RecalculateScheduleUseCase`. Post-split, `AlarmScheduler` keeps
     `setExactAlarm`/`cancelAlarm`/test + capabilities; `scheduleUpcomingAlarms` moves to
     `SchedulePrayerAlarmsUseCase` and off the interface.
   - `ExactAlarmGateway`: sets and cancels alarms via `AlarmManager` (`setAlarmClock` /
     `setAndAllowWhileIdle`), PendingIntent creation, and cancel handling (today :178–241, :275–290).
   - `AlarmCapabilities`: queries exact-alarm permission, battery optimization exemptions,
     and notification permission (today :243–273).
   - Cut static `QayamApp.*_CHANNEL_ID` / `MainActivity::class.java` coupling via
     injected channel config and intent factory.
3. Audio `object` (453 lines, 6 jobs) → `@Singleton AudioPlayer` facade over:
   - `AudioFocusManager` (:124–180),
   - `RingtonePlayer` (:182–235, incl. 15s auto-stop),
   - `SynthPlayer` (PCM synthesis engine with ADSR envelope, :344–452),
   - `MelodyRepository` (Note data + 5 melodies, :237–342).
   - Rewire `AdhanPlaybackService` to inject `AudioPlayer` and `PrayerNotificationNotifier`
     now that the audio split has landed.
   - Removes global-mutable-singleton concurrency risk while retaining single runtime instance.
4. `LocationService` (142 lines, 2 jobs) → split into:
   - `FusedLocationProviderImpl` (implementing `LocationProvider`): coordinates with
     `FusedLocationProviderClient`, checks runtime location permissions, handles timeouts, returns `Coordinates?`.
   - `AndroidGeocoderServiceImpl` (implementing `GeocoderService`): reverse geocoding
     using Android `Geocoder`.
   - `CityPresets.kt`: static list of preset global locations.
5. `AppSettings` file (4 concerns) → split into:
   - `UserSettings.kt` (model),
   - `SettingsKeys.kt` (Preferences keys),
   - `SettingsMapper.kt` (`Preferences.toUserSettings()`),
   - `DataStoreSettingsRepository.kt` (implements `SettingsRepository` using `preferencesDataStore` and `edit {}`).
     Retains `internal suspend fun resetToDefaults()` for `DataStoreRoundtripTest` cleanup, but excluded from domain `SettingsRepository`.
   - In `SettingsDefaultsTest`: update read call from `.settings.value` to `snapshot()` to match the cold `Flow` contract.
   - Key names may be renamed for clarity while splitting. No migration or dual-read paths needed (guiding constraint).
   - `PrayerTimeCalculator` stays a pure facade (split into `SolarMath`/`ScheduleCalculator`/`CurrentStateResolver` only if P3 work touches it).
6. Verify after each split: unit tests + `assembleDebug`. No UI
   changes ⇒ no screenshot churn.

## P3 — ISP + OCP at the UI layer (I, O)

Goal: screens depend on narrow state + callbacks; adding UI never
means editing a 900-line file.

1. Narrow both screen signatures to the `PrayerCard` pattern
   (state + lambdas, no `PrayerViewModel`):
   - `MainPrayerScreen` root (~500 lines) keeps flow collection +
     callback wiring only; extract `AudioPlayingBanner`,
     `LocationTopBar`, `PrayerSoundBottomSheet`, and move
     `DateSubtitleItem`/`HeroItem`/`CountdownItem`/`PrayerCardRow`
     to own files; narrow `tickerFlow: StateFlow` params to plain
     `PrayerTickerState` where the collector already sits above.
   - `SettingsScreen` (937 lines) → per-section files
     (`Appearance`, `Calculation`, `Juristic`, `AdhanAlerts`,
     `BackgroundReliability`, `Location`, `Offsets`) plus a shared
     `PrayerSoundPicker` that dedupes today's Main-sheet vs
     Settings-dialog duplication, plus hoisted
     `SettingsSectionCard`.
   - Extract `AppNavHost` from `MainActivity` (which keeps only
     `setContent` + theme + VM delegate).
2. Clean extension seams (no speculative generality):
   - *Cut speculative work*: Do **not** build an "alarm strategy as injectable policy"
     seam — there is only one strategy today (AlarmClock with fallback). Document where
     the policy decision lives in `ExactAlarmGateway`.
   - Documented new-setting recipe (Keys → mapper → repository update → UI row)
     replacing today's 3-place edit.
3. Verify: `@Preview`s render, screenshot matrix **re-recorded**
   (UI files move), TalkBack pass for the M5 semantics
   (live regions, roles, headings, disambiguated labels).

## P4 — Tests against fakes (D payoff, L contracts)

Goal: the suite no longer needs Robolectric to test app logic.

1. Fakes on `TestScope`:
   - `FakeSettingsRepository` (in-memory flow + snapshot),
   - `FakeAlarmScheduler` (records calls, honors idempotence/cancel-disabled),
   - `FakeAudioPlayer` (tracks playback states),
   - `FakeLocationProvider` (provides stubbed coordinates),
   - `FakeGeocoderService` (provides stubbed city/country).
2. Fake contracts every fake must honor (Liskov guardrails, sourced
   from current behavior — these pin semantics, not stored data):
   - `snapshot() == settings.first()`.
   - Scheduling idempotent; disabled prayers cancelled, never
     scheduled (`AdhanNotificationManager.kt:160–175, cancelAlarm`).
   - `playSound(SILENT/VIBRATE_ONLY)` = no-op + immediate `onComplete`
     (`AdhanAudioSynthesizer.kt:54–57`).
3. Two new pure-JVM use case tests:
   - `SchedulePrayerAlarmsUseCaseTest`: tests trigger-time math (today vs tomorrow)
     and cancellation of disabled prayers against `FakeAlarmScheduler`.
   - `RefreshLocationUseCaseTest`: tests GPS location retrieval, fallback on failure,
     and updating `SettingsRepository`.
4. Rewrite ViewModel tests as pure JVM:
   - *Test Dispatcher Setup*: Call `Dispatchers.setMain(testDispatcher)` in setup
     and `Dispatchers.resetMain()` in teardown since `viewModelScope` uses `Dispatchers.Main`.
   - Test `PrayerViewModel` in complete isolation with fakes, eliminating Robolectric
     overhead (`shadowOf(Looper).idle()`).
   - Keep Robolectric only for receivers, notification channels, and screenshot tests.
5. Verify: `./gradlew :app:testDebugUnitTest`, lint clean, and a faster suite
   (no Robolectric on VM paths).

---

## Execution order & gates

P0 → P1 → P2 → P3 → P4. After every phase:
`testDebugUnitTest` + `verifyRoborazziDebug` + `assembleDebug` green.
P1 is the point of no return on build config (KSP/Hilt versions);
P2–P3 stay incremental per class.

All previously open inputs have been resolved and closed:
- KSP resolution is an explicit blocking gate at Step 0 of P0 (verified via Maven Central / the KSP releases page).
- Service stop/start stays on facade in P1 and moves to injected `AudioPlayer` in P2.
- 9 write-through settings methods remain one-line delegates in ViewModel (with `updateLocation` on `SettingsRepository`).
- Startup alarm arming: in P1 `QayamApp` injects `AlarmScheduler` + `@ApplicationScope`; in P2 switches to `SchedulePrayerAlarmsUseCase`.
