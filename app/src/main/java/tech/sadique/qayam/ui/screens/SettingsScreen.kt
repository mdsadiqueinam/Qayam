
package tech.sadique.qayam.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.ui.components.PrayerSoundPicker
import tech.sadique.qayam.ui.screens.settings.AdhanAlertsSection
import tech.sadique.qayam.ui.screens.settings.AppearanceSection
import tech.sadique.qayam.ui.screens.settings.BackgroundReliabilitySection
import tech.sadique.qayam.ui.screens.settings.CalculationSection
import tech.sadique.qayam.ui.screens.settings.JuristicSection
import tech.sadique.qayam.ui.screens.settings.LocationSection
import tech.sadique.qayam.ui.screens.settings.OffsetsSection
import tech.sadique.qayam.ui.viewmodel.PrayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: PrayerViewModel, onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    var selectedPrayerId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPrayerForSound = selectedPrayerId?.let { PrayerType.fromId(it) }

    // Hoisted above LazyColumn: permission status refreshes on return from Settings.
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var canExactAlarms by rememberSaveable { mutableStateOf(viewModel.canScheduleExactAlarms()) }
    var isBatteryIgnored by rememberSaveable { mutableStateOf(viewModel.isIgnoringBatteryOptimizations()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canExactAlarms = viewModel.canScheduleExactAlarms()
                isBatteryIgnored = viewModel.isIgnoringBatteryOptimizations()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_settings_back"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Main Screen",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 1. Theme & Appearance
            item {
                AppearanceSection(
                    themeMode = settings.themeMode,
                    is24HourFormat = settings.is24HourFormat,
                    onThemeChange = { viewModel.updateThemeMode(it) },
                    on24HourChange = { viewModel.updateIs24HourFormat(it) },
                )
            }

            // 2. Prayer Calculation Method
            item {
                CalculationSection(
                    selectedMethod = settings.calculationMethod,
                    onMethodSelect = { viewModel.updateCalculationMethod(it) },
                )
            }

            // 3. School of Thought (Juristic Asr Method)
            item {
                JuristicSection(
                    selectedJuristic = settings.juristicMethod,
                    onJuristicSelect = { viewModel.updateJuristicMethod(it) },
                )
            }

            // 4. Adhan Alerts & High Priority Audio
            item {
                AdhanAlertsSection(
                    highPrioritySound = settings.highPrioritySound,
                    prayerAlertEnabled = settings.prayerAlertEnabled,
                    prayerAlertSounds = settings.prayerAlertSounds,
                    isPlayingSound = uiState.isPlayingSound,
                    playingSoundType = uiState.playingSoundType,
                    onUpdateHighPrioritySound = { viewModel.updateHighPrioritySound(it) },
                    onSelectPrayerForSound = { selectedPrayerId = it.id },
                    onTogglePrayerEnabled = { prayer, enabled ->
                        viewModel.updatePrayerAlertEnabled(prayer, enabled)
                    },
                    onPlayPreview = { viewModel.playPreviewSound(it) },
                )
            }

            // 5. Background Reliability & Exact Alarms
            item {
                BackgroundReliabilitySection(
                    canExactAlarms = canExactAlarms,
                    isBatteryIgnored = isBatteryIgnored,
                    onScheduleTestAlarm = { viewModel.scheduleTestAlarm(it) },
                    onOpenExactAlarmSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                            context.startActivity(intent)
                        }
                    },
                    onOpenBatterySettings = {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    },
                )
            }

            // 6. Offline & Location Settings
            item {
                LocationSection(
                    currentLocation = settings.currentLocation,
                    onRefreshGps = { viewModel.refreshGpsLocation() },
                    onSelectPreset = { viewModel.selectPresetLocation(it) },
                )
            }

            // 7. Manual Minute Fine-Tuning Offsets
            item {
                OffsetsSection(
                    minuteOffsets = settings.minuteOffsets,
                    onUpdateOffset = { prayer, offset ->
                        viewModel.updatePrayerMinuteOffset(prayer, offset)
                    },
                )
            }
        }
    }

    // Modal Alert Dialog for selecting Prayer Alert Sound
    selectedPrayerForSound?.let { prayer ->
        val currentSound = settings.prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH
        AlertDialog(
            onDismissRequest = {
                if (uiState.isPlayingSound) viewModel.stopPreviewSound()
                selectedPrayerId = null
            },
            title = {
                Column {
                    Text(
                        text = "${prayer.displayName} Alert Sound",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Choose notification sound or vibration mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            text = {
                PrayerSoundPicker(
                    currentSound = currentSound,
                    isPlayingSound = uiState.isPlayingSound,
                    playingSoundType = uiState.playingSoundType,
                    onSelectSound = { sound ->
                        if (uiState.isPlayingSound) viewModel.stopPreviewSound()
                        viewModel.updatePrayerAlertSound(prayer, sound)
                        selectedPrayerId = null
                    },
                    onPlayPreview = { sound ->
                        viewModel.playPreviewSound(sound)
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (uiState.isPlayingSound) viewModel.stopPreviewSound()
                        selectedPrayerId = null
                    },
                ) {
                    Text("Done")
                }
            },
        )
    }
}
