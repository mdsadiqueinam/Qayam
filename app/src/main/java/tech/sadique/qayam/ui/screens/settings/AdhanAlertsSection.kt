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
    modifier: Modifier = Modifier,
) {
    SettingsSectionCard(
        title = "Adhan Alerts & Sounds",
        icon = Icons.Default.NotificationsActive,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AlertHeader(highPrioritySound = highPrioritySound, onUpdate = onUpdateHighPrioritySound)
            AlertIntro()
            PrayerType.dailyPrayers.forEach { prayer ->
                val sound = prayerAlertSounds[prayer] ?: AdhanSoundType.MAKKAH
                AlertRow(
                    prayer = prayer,
                    isEnabled = prayerAlertEnabled[prayer] ?: prayer.defaultAlertEnabled,
                    sound = sound,
                    isPlaying = isPlayingSound && playingSoundType == sound,
                    onSelectPrayerForSound = onSelectPrayerForSound,
                    onTogglePrayerEnabled = onTogglePrayerEnabled,
                    onPlayPreview = onPlayPreview,
                )
            }
        }
    }
}

@Composable
private fun AlertHeader(highPrioritySound: Boolean, onUpdate: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Top Priority (Play in Silent Mode)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Uses alarm audio stream to sound Adhan even when phone is on silent/vibrate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = highPrioritySound,
            onCheckedChange = onUpdate,
            modifier = Modifier.testTag("switch_high_priority_sound"),
        )
    }
}

@Composable
private fun AlertIntro() {
    Text(
        text = "Prayer Notifications & Sounds:",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = "Turn notifications on or off for each prayer time, or tap to customize the alert sound" +
            " / vibration.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AlertRow(
    prayer: PrayerType,
    isEnabled: Boolean,
    sound: AdhanSoundType,
    isPlaying: Boolean,
    onSelectPrayerForSound: (PrayerType) -> Unit,
    onTogglePrayerEnabled: (PrayerType, Boolean) -> Unit,
    onPlayPreview: (AdhanSoundType) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = alertRowColor(isEnabled),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isEnabled) { onSelectPrayerForSound(prayer) }
            .testTag("prayer_settings_card_${prayer.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AlertTitleRow(prayer = prayer, isEnabled = isEnabled)
                AlertSubtitle(isEnabled = isEnabled, soundTitle = sound.title)
            }
            SoundRow(
                prayer = prayer,
                sound = sound,
                isEnabled = isEnabled,
                isPlaying = isPlaying,
                onToggle = { checked -> onTogglePrayerEnabled(prayer, checked) },
                onPlayPreview = onPlayPreview,
            )
        }
    }
}

@Composable
private fun alertRowColor(isEnabled: Boolean) = if (isEnabled) {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
} else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
}

@Composable
private fun AlertTitleRow(prayer: PrayerType, isEnabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = prayer.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isEnabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            },
        )
        if (prayer.subtitle != null) {
            Text(
                text = "(${prayer.subtitle})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun AlertSubtitle(isEnabled: Boolean, soundTitle: String) {
    Text(
        text = if (isEnabled) soundTitle else "Notification Disabled",
        style = MaterialTheme.typography.bodySmall,
        color = if (isEnabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        },
    )
}

@Composable
private fun SoundRow(
    prayer: PrayerType,
    sound: AdhanSoundType,
    isEnabled: Boolean,
    isPlaying: Boolean,
    onToggle: (Boolean) -> Unit,
    onPlayPreview: (AdhanSoundType) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isPreviewableAlert(isEnabled, sound)) {
            IconButton(
                onClick = { onPlayPreview(sound) },
                modifier = Modifier.testTag("preview_btn_${prayer.id}"),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.GraphicEq,
                    contentDescription = "Preview ${sound.title} for ${prayer.displayName}" +
                        if (isPlaying) ", playing, tap to stop" else "",
                    tint = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            modifier = Modifier.testTag("switch_prayer_alert_${prayer.id}"),
        )
    }
}

private fun isPreviewableAlert(isEnabled: Boolean, sound: AdhanSoundType) =
    isEnabled && sound != AdhanSoundType.SILENT && sound != AdhanSoundType.VIBRATE_ONLY
