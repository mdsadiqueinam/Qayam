@file:Suppress("FunctionNaming", "LongMethod")

package tech.sadique.qayam.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import tech.sadique.qayam.ui.viewmodel.PrayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPrayerScreen(viewModel: PrayerViewModel, onNavigateToSettings: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tickerState by viewModel.tickerState.collectAsStateWithLifecycle()

    var selectedPrayerId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPrayerForSoundModal = selectedPrayerId?.let { PrayerType.fromId(it) }
    val sheetState = rememberModalBottomSheetState()

    // Permission launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.refreshGpsLocation()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val hasLocPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (!hasLocPerm) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AudioPlayingBanner(
                isPlayingSound = uiState.isPlayingSound,
                playingSoundType = uiState.playingSoundType,
                onStopSound = { viewModel.stopPreviewSound() },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("main_prayer_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Top Bar with Location & Settings Button
            item {
                LocationTopBar(
                    location = uiState.settings.currentLocation,
                    isLoading = uiState.isLocationLoading,
                    onRefreshLocation = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            viewModel.refreshGpsLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        }
                    },
                    onNavigateToSettings = onNavigateToSettings,
                )
            }

            // 2. Date Subtitle
            item {
                DateSubtitleItem(
                    currentTimeMillis = tickerState.currentTimeMillis,
                    calculationTitle = uiState.settings.calculationMethod.title.substringBefore('('),
                )
            }

            // 3. Hero Animated Sun / Horizon Canvas with Mosque & Active Prayer
            item {
                HeroItem(
                    tickerState = tickerState,
                    is24Hour = uiState.settings.is24HourFormat,
                )
            }

            // 4. Upcoming Prayer Countdown Timer Card
            item {
                CountdownItem(
                    currentState = tickerState.currentState,
                    is24Hour = uiState.settings.is24HourFormat,
                )
            }

            // 5. Daily Salah Schedule Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
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
                        text = uiState.settings.juristicMethod.title.substringBefore('(').trim(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // 6. Prayer Cards
            val schedule = uiState.schedule
            if (schedule != null) {
                prayerScheduleItems(
                    schedule = schedule,
                    tickerState = tickerState,
                    settings = uiState.settings,
                    isPlayingSound = uiState.isPlayingSound,
                    playingSoundType = uiState.playingSoundType,
                    onToggleAlert = { prayer, enabled ->
                        viewModel.updatePrayerAlertEnabled(prayer, enabled)
                    },
                    onSoundClick = { prayer -> selectedPrayerId = prayer.id },
                )
            }
        }
    }

    selectedPrayerForSoundModal?.let { prayer ->
        val currentSound = uiState.settings.prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH
        val isEnabled = uiState.settings.prayerAlertEnabled[prayer] ?: prayer.defaultAlertEnabled

        PrayerSoundBottomSheet(
            prayer = prayer,
            currentSound = currentSound,
            isEnabled = isEnabled,
            isPlayingSound = uiState.isPlayingSound,
            playingSoundType = uiState.playingSoundType,
            sheetState = sheetState,
            onToggleAlertEnabled = { checked ->
                viewModel.updatePrayerAlertEnabled(prayer, checked)
            },
            onSelectSound = { sound ->
                viewModel.updatePrayerAlertSound(prayer, sound)
            },
            onPlayPreview = { sound ->
                viewModel.playPreviewSound(sound)
            },
            onDismiss = { selectedPrayerId = null },
        )
    }
}
