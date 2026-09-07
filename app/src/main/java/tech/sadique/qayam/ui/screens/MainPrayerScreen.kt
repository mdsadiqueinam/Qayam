package tech.sadique.qayam.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.ui.screens.main.AudioPlayingBanner
import tech.sadique.qayam.ui.screens.main.CountdownItem
import tech.sadique.qayam.ui.screens.main.DateSubtitleItem
import tech.sadique.qayam.ui.screens.main.HeroItem
import tech.sadique.qayam.ui.screens.main.LocationTopBar
import tech.sadique.qayam.ui.screens.main.PrayerSoundBottomSheet
import tech.sadique.qayam.ui.screens.main.prayerScheduleItems
import tech.sadique.qayam.ui.viewmodel.PrayerTickerState
import tech.sadique.qayam.ui.viewmodel.PrayerUiState
import tech.sadique.qayam.ui.viewmodel.PrayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPrayerScreen(viewModel: PrayerViewModel, onNavigateToSettings: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tickerState by viewModel.tickerState.collectAsStateWithLifecycle()
    var selectedPrayerId by rememberSaveable { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val locationLauncher = rememberLocationLauncher(onGranted = { viewModel.refreshGpsLocation() })
    val notificationLauncher = rememberNotificationLauncher()
    RequestPermissionsEffect(
        context = context,
        locationLauncher = locationLauncher,
        notificationLauncher = notificationLauncher,
    )
    Scaffold(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AudioPlayingBanner(
                isPlayingSound = uiState.isPlayingSound,
                playingSoundType = uiState.playingSoundType,
                onStopSound = { viewModel.stopPreviewSound() },
            )
        },
    ) { innerPadding ->
        MainPrayerListContent(
            uiState = uiState,
            tickerState = tickerState,
            onRefreshLocation = { handleLocationRefresh(context, viewModel, locationLauncher) },
            onNavigateToSettings = onNavigateToSettings,
            onSoundClick = { prayer -> selectedPrayerId = prayer.id },
            modifier = Modifier.padding(innerPadding),
        )
    }
    SoundBottomSheetHost(
        selectedPrayerId = selectedPrayerId,
        uiState = uiState,
        sheetState = sheetState,
        onDismiss = { selectedPrayerId = null },
        onToggle = { prayer, checked -> viewModel.settingsUpdater.updatePrayerAlertEnabled(prayer, checked) },
        onSelectSound = { prayer, sound -> viewModel.settingsUpdater.updatePrayerAlertSound(prayer, sound) },
        onPreview = { sound -> viewModel.playPreviewSound(sound) },
    )
}

@Composable
private fun rememberLocationLauncher(onGranted: () -> Unit) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) {
        onGranted()
    }
}

@Composable
private fun rememberNotificationLauncher() = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
) { }

@Composable
private fun RequestPermissionsEffect(
    context: Context,
    locationLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    notificationLauncher: ManagedActivityResultLauncher<String, Boolean>,
) {
    LaunchedEffect(Unit) {
        if (needsNotificationPermission(context)) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!hasLocationPermission(context)) {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }
}

private fun needsNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return false
    }
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) != PackageManager.PERMISSION_GRANTED
}

private fun hasLocationPermission(context: Context): Boolean = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_FINE_LOCATION,
) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

private fun handleLocationRefresh(
    context: Context,
    viewModel: PrayerViewModel,
    locationLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
) {
    if (hasLocationPermission(context)) {
        viewModel.refreshGpsLocation()
    } else {
        locationLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }
}

@Composable
private fun MainPrayerListContent(
    uiState: PrayerUiState,
    tickerState: PrayerTickerState,
    onRefreshLocation: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSoundClick: (PrayerType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("main_prayer_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LocationTopBar(
                location = uiState.settings.currentLocation,
                isLoading = uiState.isLocationLoading,
                onRefreshLocation = onRefreshLocation,
                onNavigateToSettings = onNavigateToSettings,
            )
        }
        item {
            DateSubtitleItem(
                currentTimeMillis = tickerState.currentTimeMillis,
                calculationTitle = uiState.settings.calculationMethod.title.substringBefore('('),
            )
        }
        item {
            HeroItem(tickerState = tickerState, is24Hour = uiState.settings.is24HourFormat)
        }
        item {
            CountdownItem(currentState = tickerState.currentState, is24Hour = uiState.settings.is24HourFormat)
        }
        item {
            ScheduleHeader(juristicTitle = uiState.settings.juristicMethod.title.substringBefore('(').trim())
        }
        val schedule = uiState.schedule
        if (schedule != null) {
            prayerScheduleItems(
                schedule = schedule,
                tickerState = tickerState,
                settings = uiState.settings,
                isPlayingSound = uiState.isPlayingSound,
                playingSoundType = uiState.playingSoundType,
                onSoundClick = onSoundClick,
            )
        }
    }
}

@Composable
private fun ScheduleHeader(juristicTitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Today's Prayers",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = juristicTitle,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundBottomSheetHost(
    selectedPrayerId: String?,
    uiState: PrayerUiState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onToggle: (PrayerType, Boolean) -> Unit,
    onSelectSound: (PrayerType, AdhanSoundType) -> Unit,
    onPreview: (AdhanSoundType) -> Unit,
) {
    val prayer = selectedPrayerId?.let { PrayerType.fromId(it) }
    if (prayer == null) {
        return
    }
    PrayerSoundBottomSheet(
        prayer = prayer,
        currentSound = uiState.settings.prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH,
        isEnabled = uiState.settings.prayerAlertEnabled[prayer] ?: prayer.defaultAlertEnabled,
        isPlayingSound = uiState.isPlayingSound,
        playingSoundType = uiState.playingSoundType,
        sheetState = sheetState,
        onToggleAlertEnabled = { checked -> onToggle(prayer, checked) },
        onSelectSound = { sound -> onSelectSound(prayer, sound) },
        onPlayPreview = onPreview,
        onDismiss = onDismiss,
    )
}
