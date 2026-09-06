package tech.sadique.qayam.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyListScope
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
import tech.sadique.qayam.ui.viewmodel.PrayerUiState
import tech.sadique.qayam.ui.viewmodel.PrayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: PrayerViewModel, onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPrayerId by rememberSaveable { mutableStateOf<String?>(null) }
    val reliability = rememberReliabilityPermissions(viewModel = viewModel)
    Scaffold(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SettingsTopBar(onNavigateBack = onNavigateBack) },
    ) { innerPadding ->
        SettingsContent(
            viewModel = viewModel,
            uiState = uiState,
            canExactAlarms = reliability.first,
            isBatteryIgnored = reliability.second,
            onSelectPrayer = { selectedPrayerId = it.id },
            modifier = Modifier.padding(innerPadding),
        )
    }
    PrayerSoundDialogHost(
        selectedPrayerId = selectedPrayerId,
        uiState = uiState,
        onDismiss = {
            if (uiState.isPlayingSound) viewModel.stopPreviewSound()
            selectedPrayerId = null
        },
        onSelectSound = { prayer, sound ->
            if (uiState.isPlayingSound) viewModel.stopPreviewSound()
            viewModel.settingsUpdater.updatePrayerAlertSound(prayer, sound)
            selectedPrayerId = null
        },
        onPreview = { viewModel.playPreviewSound(it) },
    )
}

@Composable
private fun rememberReliabilityPermissions(viewModel: PrayerViewModel): Pair<Boolean, Boolean> {
    var canExactAlarms by rememberSaveable { mutableStateOf(viewModel.canScheduleExactAlarms()) }
    var isBatteryIgnored by rememberSaveable { mutableStateOf(viewModel.isIgnoringBatteryOptimizations()) }
    val lifecycleOwner = LocalLifecycleOwner.current
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
    return canExactAlarms to isBatteryIgnored
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = { Text(text = "Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_settings_back")) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Main Screen")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun SettingsContent(
    viewModel: PrayerViewModel,
    uiState: PrayerUiState,
    canExactAlarms: Boolean,
    isBatteryIgnored: Boolean,
    onSelectPrayer: (PrayerType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("settings_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        generalSettingsItems(viewModel = viewModel, uiState = uiState)
        alertSettingsItems(viewModel = viewModel, uiState = uiState, onSelectPrayer = onSelectPrayer)
        reliabilitySettingsItems(
            viewModel = viewModel,
            context = context,
            canExactAlarms = canExactAlarms,
            isBatteryIgnored = isBatteryIgnored,
        )
        locationSettingsItems(viewModel = viewModel, uiState = uiState)
    }
}

private fun LazyListScope.generalSettingsItems(viewModel: PrayerViewModel, uiState: PrayerUiState) {
    item {
        AppearanceSection(
            themeMode = uiState.settings.themeMode,
            is24HourFormat = uiState.settings.is24HourFormat,
            onThemeChange = { viewModel.settingsUpdater.updateThemeMode(it) },
            on24HourChange = { viewModel.settingsUpdater.updateIs24HourFormat(it) },
        )
    }
    item {
        CalculationSection(
            selectedMethod = uiState.settings.calculationMethod,
            onMethodSelect = { viewModel.settingsUpdater.updateCalculationMethod(it) },
        )
    }
    item {
        JuristicSection(
            selectedJuristic = uiState.settings.juristicMethod,
            onJuristicSelect = { viewModel.settingsUpdater.updateJuristicMethod(it) },
        )
    }
}

private fun LazyListScope.alertSettingsItems(
    viewModel: PrayerViewModel,
    uiState: PrayerUiState,
    onSelectPrayer: (PrayerType) -> Unit,
) {
    item {
        AdhanAlertsSection(
            highPrioritySound = uiState.settings.highPrioritySound,
            prayerAlertEnabled = uiState.settings.prayerAlertEnabled,
            prayerAlertSounds = uiState.settings.prayerAlertSounds,
            isPlayingSound = uiState.isPlayingSound,
            playingSoundType = uiState.playingSoundType,
            onUpdateHighPrioritySound = { viewModel.settingsUpdater.updateHighPrioritySound(it) },
            onSelectPrayerForSound = onSelectPrayer,
            onTogglePrayerEnabled = { prayer, enabled ->
                viewModel.settingsUpdater.updatePrayerAlertEnabled(prayer, enabled)
            },
            onPlayPreview = { viewModel.playPreviewSound(it) },
        )
    }
}

private fun LazyListScope.reliabilitySettingsItems(
    viewModel: PrayerViewModel,
    context: Context,
    canExactAlarms: Boolean,
    isBatteryIgnored: Boolean,
) {
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
}

private fun LazyListScope.locationSettingsItems(viewModel: PrayerViewModel, uiState: PrayerUiState) {
    item {
        LocationSection(
            currentLocation = uiState.settings.currentLocation,
            onRefreshGps = { viewModel.refreshGpsLocation() },
            onSelectPreset = { viewModel.settingsUpdater.selectPresetLocation(it) },
        )
    }
    item {
        OffsetsSection(
            minuteOffsets = uiState.settings.minuteOffsets,
            onUpdateOffset = { prayer, offset -> viewModel.settingsUpdater.updatePrayerMinuteOffset(prayer, offset) },
        )
    }
}

@Composable
private fun PrayerSoundDialogHost(
    selectedPrayerId: String?,
    uiState: PrayerUiState,
    onDismiss: () -> Unit,
    onSelectSound: (PrayerType, AdhanSoundType) -> Unit,
    onPreview: (AdhanSoundType) -> Unit,
) {
    val prayer = selectedPrayerId?.let { PrayerType.fromId(it) } ?: return
    val currentSound = uiState.settings.prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { PrayerSoundDialogTitle(prayer = prayer) },
        text = {
            PrayerSoundPicker(
                currentSound = currentSound,
                isPlayingSound = uiState.isPlayingSound,
                playingSoundType = uiState.playingSoundType,
                onSelectSound = { sound -> onSelectSound(prayer, sound) },
                onPlayPreview = onPreview,
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun PrayerSoundDialogTitle(prayer: PrayerType) {
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
}
