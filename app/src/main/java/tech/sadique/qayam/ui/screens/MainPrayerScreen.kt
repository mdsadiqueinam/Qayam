package tech.sadique.qayam.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.CurrentPrayerState
import tech.sadique.qayam.data.model.PrayerSchedule
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.data.preferences.UserSettings
import tech.sadique.qayam.ui.components.CountdownTimerView
import tech.sadique.qayam.ui.components.MasjidHorizonCanvas
import tech.sadique.qayam.ui.components.PrayerCard
import tech.sadique.qayam.ui.theme.DarkPrimary
import tech.sadique.qayam.ui.theme.GoldLight
import tech.sadique.qayam.ui.viewmodel.PrayerTickerState
import tech.sadique.qayam.ui.viewmodel.PrayerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPrayerScreen(
    viewModel: PrayerViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tickerFlow: StateFlow<PrayerTickerState> = viewModel.tickerState

    var selectedPrayerId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPrayerForSoundModal = selectedPrayerId?.let { PrayerType.fromId(it) }
    val sheetState = rememberModalBottomSheetState()

    // Permission launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.refreshGpsLocation()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* handled */ }

    LaunchedEffect(Unit) {
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Check location permission (accept FINE or COARSE)
        val hasLocPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        if (!hasLocPerm) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Floating Live Adhan Audio Bar when audio is currently playing
            AnimatedVisibility(
                visible = uiState.isPlayingSound,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag("audio_playing_banner"),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Adhan Audio Playing",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = uiState.playingSoundType?.title ?: "Adhan Voice",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.stopPreviewSound() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("stop_audio_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("main_prayer_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Bar with Location & Settings Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location Chip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Refresh GPS location"
                            ) {
                                // Refresh happens in the permission callback on grant;
                                // if already granted, refresh immediately.
                                val granted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    viewModel.refreshGpsLocation()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                            .testTag("location_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.settings.currentLocation.isGpsBased) Icons.Default.MyLocation else Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = uiState.settings.currentLocation.cityName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (uiState.settings.currentLocation.isGpsBased) "GPS Location" else uiState.settings.currentLocation.countryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            if (uiState.isLocationLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(start = 4.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Location",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Settings Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("btn_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // 2. Date Subtitle (collects only the clock flow)
            item {
                DateSubtitleItem(
                    tickerFlow = tickerFlow,
                    calculationTitle = uiState.settings.calculationMethod.title.substringBefore('(')
                )
            }

            // 3. Hero Animated Sun / Horizon Canvas with Mosque & Active Prayer
            // (collects only the clock flow; static chrome above does not recompose per second)
            item {
                HeroItem(
                    tickerFlow = tickerFlow,
                    is24Hour = uiState.settings.is24HourFormat
                )
            }

            // 4. Upcoming Prayer Countdown Timer Card
            item {
                CountdownItem(
                    tickerFlow = tickerFlow,
                    is24Hour = uiState.settings.is24HourFormat
                )
            }

            // 5. Daily Salah Schedule Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Prayers",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = uiState.settings.juristicMethod.title.substringBefore('(').trim(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 6. Prayer Cards (highlight state comes from the clock flow only)
            val schedule = uiState.schedule
            if (schedule != null) {
                prayerScheduleItems(
                    schedule = schedule,
                    tickerFlow = tickerFlow,
                    settings = uiState.settings,
                    isPlayingSound = uiState.isPlayingSound,
                    playingSoundType = uiState.playingSoundType,
                    onToggleAlert = { prayer, enabled ->
                        viewModel.updatePrayerAlertEnabled(prayer, enabled)
                    },
                    onSoundClick = { prayer -> selectedPrayerId = prayer.id }
                )
            }
        }
    }

    // Modal Bottom Sheet for selecting Prayer Alert Sound
    selectedPrayerForSoundModal?.let { prayer ->
        val currentSound = uiState.settings.prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH
        val isEnabled = uiState.settings.prayerAlertEnabled[prayer] ?: prayer.defaultAlertEnabled

        ModalBottomSheet(
            onDismissRequest = { selectedPrayerId = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("sound_selection_bottom_sheet")
            ) {
                // Header with Prayer Name
                Text(
                    text = "${prayer.displayName} Alert Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Control notification and alarm sound for this prayer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Show Notification Toggle Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Notification",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEnabled) "Notifications are turned ON" else "Notifications are turned OFF",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                viewModel.updatePrayerAlertEnabled(prayer, checked)
                            },
                            modifier = Modifier.testTag("switch_prayer_enabled_${prayer.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isEnabled) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No notification or alarm will be triggered for ${prayer.displayName}. Enable the switch above to receive alerts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Alert Type & Sound:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(AdhanSoundType.entries) { sound ->
                            val isSelected = sound == currentSound
                            val isPlaying = uiState.isPlayingSound && uiState.playingSoundType == sound

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.updatePrayerAlertSound(prayer, sound)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.updatePrayerAlertSound(prayer, sound)
                                            }
                                        )
                                        Column {
                                            Text(
                                                text = sound.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = sound.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Preview Button
                                    if (sound != AdhanSoundType.SILENT && sound != AdhanSoundType.VIBRATE_ONLY) {
                                        IconButton(
                                            onClick = { viewModel.playPreviewSound(sound) }
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.GraphicEq,
                                                contentDescription = "Preview ${sound.title}" + if (isPlaying) ", playing, tap to stop" else "",
                                                tint = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { selectedPrayerId = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Date subtitle. Collects only the per-second clock flow so the rest of
 * [MainPrayerScreen] is not recomposed by the ticker.
 */
@Composable
private fun DateSubtitleItem(
    tickerFlow: StateFlow<PrayerTickerState>,
    calculationTitle: String,
    modifier: Modifier = Modifier
) {
    val ticker by tickerFlow.collectAsStateWithLifecycle()
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Text(
            text = dateFormatter.format(Date(ticker.currentTimeMillis)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Calculation: $calculationTitle",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
    }
}

/**
 * Hero horizon card (canvas + prayer pill + live clock + sun tag).
 * Collects only the per-second clock flow.
 */
@Composable
private fun HeroItem(
    tickerFlow: StateFlow<PrayerTickerState>,
    is24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    val ticker by tickerFlow.collectAsStateWithLifecycle()
    val state = ticker.currentState
    val timeFormatter = remember(is24Hour) {
        if (is24Hour) SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        else SimpleDateFormat("h:mm:ss a", Locale.getDefault())
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 230.dp)
            .testTag("hero_horizon_card"),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MasjidHorizonCanvas(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .semantics {
                        contentDescription =
                            "Animated sky for ${state?.currentPrayer?.displayName ?: "loading"} prayer"
                    }
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Current Active Prayer Pill (placeholder until the first tick resolves)
                val currentPrayer = state?.currentPrayer
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier.testTag("current_prayer_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DarkPrimary)
                        )
                        Text(
                            text = currentPrayer?.let { "${it.displayName.uppercase()} TIME" }
                                ?: "LOADING PRAYER TIMES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color.White
                        )
                        currentPrayer?.let {
                            Text(
                                text = it.arabicName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = timeFormatter.format(Date(ticker.currentTimeMillis)),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.testTag("live_clock_text")
                )

                val alt = state?.sunAltitudeDegrees ?: 0.0
                val sunStatus = remember(alt) {
                    if (alt > 0) String.format(Locale.US, "Sun Altitude: +%.1f° (Day)", alt)
                    else String.format(Locale.US, "Sun Altitude: %.1f° (Night)", alt)
                }
                Text(
                    text = sunStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * Countdown card. Collects only the per-second clock flow.
 */
@Composable
private fun CountdownItem(
    tickerFlow: StateFlow<PrayerTickerState>,
    is24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    val ticker by tickerFlow.collectAsStateWithLifecycle()
    CountdownTimerView(
        state = ticker.currentState,
        is24Hour = is24Hour,
        modifier = modifier
    )
}

/**
 * Daily prayer cards. Highlight state comes from the per-second clock flow;
 * static rows do not read it at the [MainPrayerScreen] root.
 */
private fun LazyListScope.prayerScheduleItems(
    schedule: PrayerSchedule,
    tickerFlow: StateFlow<PrayerTickerState>,
    settings: UserSettings,
    isPlayingSound: Boolean,
    playingSoundType: AdhanSoundType?,
    onToggleAlert: (PrayerType, Boolean) -> Unit,
    onSoundClick: (PrayerType) -> Unit
) {
    items(PrayerType.dailyPrayers) { prayer ->
        PrayerCardRow(
            prayer = prayer,
            schedule = schedule,
            tickerFlow = tickerFlow,
            settings = settings,
            isPlayingSound = isPlayingSound,
            playingSoundType = playingSoundType,
            onToggleAlert = onToggleAlert,
            onSoundClick = onSoundClick
        )
    }
}

@Composable
private fun PrayerCardRow(
    prayer: PrayerType,
    schedule: PrayerSchedule,
    tickerFlow: StateFlow<PrayerTickerState>,
    settings: UserSettings,
    isPlayingSound: Boolean,
    playingSoundType: AdhanSoundType?,
    onToggleAlert: (PrayerType, Boolean) -> Unit,
    onSoundClick: (PrayerType) -> Unit
) {
    val ticker by tickerFlow.collectAsStateWithLifecycle()
    val currentState: CurrentPrayerState? = ticker.currentState
    val isEnabled = settings.prayerAlertEnabled[prayer] ?: prayer.defaultAlertEnabled
    val soundType = settings.prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH
    PrayerCard(
        prayer = prayer,
        time = schedule.getTime(prayer),
        isCurrent = currentState?.currentPrayer == prayer,
        isNext = currentState?.nextPrayer == prayer,
        is24Hour = settings.is24HourFormat,
        soundType = soundType,
        isEnabled = isEnabled,
        isPlayingThisSound = isPlayingSound && playingSoundType == soundType,
        onToggleAlert = { onToggleAlert(prayer, !isEnabled) },
        onSoundClick = { onSoundClick(prayer) }
    )
}
