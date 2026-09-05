package tech.sadique.qayam.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.sadique.qayam.data.location.LocationService
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.AppThemeMode
import tech.sadique.qayam.data.model.CalculationMethod
import tech.sadique.qayam.data.model.HighLatitudeRule
import tech.sadique.qayam.data.model.JuristicMethod
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.ui.viewmodel.PrayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PrayerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    var selectedPrayerForSound by remember { mutableStateOf<PrayerType?>(null) }

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
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_settings_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Main Screen"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Theme & Appearance
            item {
                SettingsSectionCard(
                    title = "Appearance & Theme",
                    icon = Icons.Default.DarkMode
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Theme Switcher",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        AppThemeMode.entries.forEach { mode ->
                            val isSelected = settings.themeMode == mode
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.updateThemeMode(mode) }
                                    .testTag("theme_option_${mode.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.updateThemeMode(mode) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 24-Hour format toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "24-Hour Time Format",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (settings.is24HourFormat) "Display e.g. 18:30" else "Display e.g. 6:30 PM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.is24HourFormat,
                                onCheckedChange = { viewModel.updateIs24HourFormat(it) },
                                modifier = Modifier.testTag("switch_24h_format")
                            )
                        }
                    }
                }
            }

            // 2. Prayer Calculation Method
            item {
                SettingsSectionCard(
                    title = "Calculation Method",
                    icon = Icons.Default.Calculate
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Standard calculation parameters for Fajr & Isha angles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        CalculationMethod.entries.forEach { method ->
                            val isSelected = settings.calculationMethod == method
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.updateCalculationMethod(method) }
                                    .testTag("calc_method_${method.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = method.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = method.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.updateCalculationMethod(method) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. School of Thought (Juristic Asr Method)
            item {
                SettingsSectionCard(
                    title = "School of Thought (Asr)",
                    icon = Icons.Default.School
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Determines Asr prayer time shadow ratio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        JuristicMethod.entries.forEach { juristic ->
                            val isSelected = settings.juristicMethod == juristic
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.updateJuristicMethod(juristic) }
                                    .testTag("juristic_${juristic.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = juristic.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = juristic.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.updateJuristicMethod(juristic) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Adhan Alerts & High Priority Audio
            item {
                SettingsSectionCard(
                    title = "Adhan Alerts & Sounds",
                    icon = Icons.Default.NotificationsActive
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // High Priority Sound Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Top Priority (Play in Silent Mode)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Uses alarm audio stream to sound Adhan even when phone is on silent/vibrate",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.highPrioritySound,
                                onCheckedChange = { viewModel.updateHighPrioritySound(it) },
                                modifier = Modifier.testTag("switch_high_priority_sound")
                            )
                        }

                        Text(
                            text = "Prayer Notifications & Sounds:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Turn notifications on or off for each prayer time, or tap to customize the alert sound / vibration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        PrayerType.dailyPrayers.forEach { prayer ->
                            val isEnabled = settings.prayerAlertEnabled[prayer] ?: (prayer != PrayerType.SUNRISE && prayer != PrayerType.GURUB_E_AFTAB)
                            val sound = settings.prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH
                            val isPlayingThis = uiState.isPlayingSound && uiState.playingSoundType == sound

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = isEnabled) {
                                        selectedPrayerForSound = prayer
                                    }
                                    .testTag("prayer_settings_card_${prayer.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = prayer.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            if (prayer.subtitle != null) {
                                                Text(
                                                    text = "(${prayer.subtitle})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isEnabled) sound.title else "Notification Disabled",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isEnabled && sound != AdhanSoundType.SILENT && sound != AdhanSoundType.VIBRATE_ONLY) {
                                            IconButton(
                                                onClick = { viewModel.playPreviewSound(sound) },
                                                modifier = Modifier.testTag("preview_btn_${prayer.id}")
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.GraphicEq,
                                                    contentDescription = "Test sound",
                                                    tint = if (isPlayingThis) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = isEnabled,
                                            onCheckedChange = { checked ->
                                                viewModel.updatePrayerAlertEnabled(prayer, checked)
                                            },
                                            modifier = Modifier.testTag("switch_prayer_alert_${prayer.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Background Reliability & Exact Alarms
            item {
                val context = LocalContext.current
                var testAlarmScheduled by remember { mutableStateOf(false) }
                val canExactAlarms = remember { viewModel.canScheduleExactAlarms() }
                val isBatteryIgnored = remember { viewModel.isIgnoringBatteryOptimizations() }

                SettingsSectionCard(
                    title = "Background Reliability",
                    icon = Icons.Default.NotificationsActive
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Ensure prayer notifications and Adhan audio fire precisely on time even when the screen is locked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Exact Alarm Permission
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Exact Alarm Permission",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (canExactAlarms) "Granted (Alarms trigger exactly on time)"
                                            else "Denied (Alerts may be delayed by Android)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (canExactAlarms) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Icon(
                                        imageVector = if (canExactAlarms) Icons.Default.Check else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (canExactAlarms) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }

                                if (!canExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Grant Exact Alarm Permission")
                                    }
                                }
                            }
                        }

                        // Battery Optimization
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Battery Optimization",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isBatteryIgnored) "Unrestricted (Recommended)"
                                            else "Optimized (OS may throttle background alarms)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isBatteryIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isBatteryIgnored) Icons.Default.Check else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isBatteryIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }

                                if (!isBatteryIgnored && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                context.startActivity(intent)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Set Battery to Unrestricted")
                                    }
                                }
                            }
                        }

                        // Test Alarm
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Test Background Alert (10 Seconds)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to schedule a test alert in 10 seconds, then lock your device or leave the app to test background wakeup.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = {
                                        viewModel.scheduleTestAlarm(10)
                                        testAlarmScheduled = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (testAlarmScheduled) "Test Alert Scheduled (Fires in 10s)" else "Schedule 10s Test Alert")
                                }

                                if (testAlarmScheduled) {
                                    Text(
                                        text = "✓ Scheduled! Lock your device now to verify.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Offline & Location Settings
            item {
                SettingsSectionCard(
                    title = "Location & Offline Presets",
                    icon = Icons.Default.LocationCity
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Current Coordinates",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${settings.currentLocation.cityName} (${String.format("%.2f", settings.currentLocation.latitude)}°, ${String.format("%.2f", settings.currentLocation.longitude)}°)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = { viewModel.refreshGpsLocation() },
                                modifier = Modifier.testTag("btn_refresh_gps_settings")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Get GPS Location",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "Quick Offline City Presets (1-tap setup):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(LocationService.CITY_PRESETS) { preset ->
                                val isCurrentCity = settings.currentLocation.cityName.equals(preset.cityName, ignoreCase = true)
                                FilterChip(
                                    selected = isCurrentCity,
                                    onClick = { viewModel.selectPresetLocation(preset) },
                                    label = { Text(preset.cityName) },
                                    leadingIcon = if (isCurrentCity) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 6. Manual Minute Fine-Tuning Offsets
            item {
                SettingsSectionCard(
                    title = "Prayer Time Adjustments",
                    icon = Icons.Default.Tune
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Fine-tune prayer times in minutes (+/-) if your local mosque timetable differs slightly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        PrayerType.dailyPrayers.forEach { prayer ->
                            val offset = settings.minuteOffsets[prayer] ?: 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = prayer.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.updatePrayerMinuteOffset(prayer, offset - 1) }
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease minute", modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Text(
                                        text = if (offset > 0) "+$offset min" else "$offset min",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(62.dp),
                                        color = if (offset != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )

                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.updatePrayerMinuteOffset(prayer, offset + 1) }
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase minute", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Alert Dialog for selecting Prayer Alert Sound
    selectedPrayerForSound?.let { prayer ->
        val currentSound = settings.prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH
        AlertDialog(
            onDismissRequest = {
                if (uiState.isPlayingSound) viewModel.stopPreviewSound()
                selectedPrayerForSound = null
            },
            title = {
                Column {
                    Text(
                        text = "${prayer.displayName} Alert Sound",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose notification sound or vibration mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AdhanSoundType.entries) { sound ->
                        val isSelected = sound == currentSound
                        val isPlaying = uiState.isPlayingSound && uiState.playingSoundType == sound

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (uiState.isPlayingSound) viewModel.stopPreviewSound()
                                    viewModel.updatePrayerAlertSound(prayer, sound)
                                    selectedPrayerForSound = null
                                }
                                .testTag("sound_dialog_option_${sound.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            if (uiState.isPlayingSound) viewModel.stopPreviewSound()
                                            viewModel.updatePrayerAlertSound(prayer, sound)
                                            selectedPrayerForSound = null
                                        }
                                    )
                                    Column {
                                        Text(
                                            text = sound.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = sound.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (sound != AdhanSoundType.SILENT && sound != AdhanSoundType.VIBRATE_ONLY) {
                                    IconButton(
                                        onClick = { viewModel.playPreviewSound(sound) },
                                        modifier = Modifier.testTag("preview_dialog_${sound.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.GraphicEq,
                                            contentDescription = "Test sound",
                                            tint = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (uiState.isPlayingSound) viewModel.stopPreviewSound()
                        selectedPrayerForSound = null
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
