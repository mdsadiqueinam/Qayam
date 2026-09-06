package tech.sadique.qayam.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType

@Composable
fun AdhanAlertsSection(
    highPrioritySound: Boolean,
    prayerAlertEnabled: Map<PrayerType, Boolean>,
    prayerAlertSounds: Map<PrayerType, AdhanSoundType>,
    isPlayingSound: Boolean,
    playingSoundType: AdhanSoundType?,
    onUpdateHighPrioritySound: (Boolean) -> Unit,
    onSelectPrayerForSound: (PrayerType) -> Unit,
    onTogglePrayerEnabled: (PrayerType, Boolean) -> Unit,
    onPlayPreview: (AdhanSoundType) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSectionCard(
        title = "Adhan Alerts & Sounds",
        icon = Icons.Default.NotificationsActive,
        modifier = modifier
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
                    checked = highPrioritySound,
                    onCheckedChange = onUpdateHighPrioritySound,
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
                val isEnabled = prayerAlertEnabled[prayer] ?: prayer.defaultAlertEnabled
                val sound = prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH
                val isPlayingThis = isPlayingSound && playingSoundType == sound

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = isEnabled) { onSelectPrayerForSound(prayer) }
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
                                    onClick = { onPlayPreview(sound) },
                                    modifier = Modifier.testTag("preview_btn_${prayer.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.GraphicEq,
                                        contentDescription = "Preview ${sound.title} for ${prayer.displayName}" + if (isPlayingThis) ", playing, tap to stop" else "",
                                        tint = if (isPlayingThis) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked -> onTogglePrayerEnabled(prayer, checked) },
                                modifier = Modifier.testTag("switch_prayer_alert_${prayer.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}
