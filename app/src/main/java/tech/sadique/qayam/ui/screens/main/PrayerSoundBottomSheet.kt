
package tech.sadique.qayam.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.ui.components.PrayerSoundPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerSoundBottomSheet(
    prayer: PrayerType,
    currentSound: AdhanSoundType,
    isEnabled: Boolean,
    isPlayingSound: Boolean,
    playingSoundType: AdhanSoundType?,
    sheetState: SheetState,
    onToggleAlertEnabled: (Boolean) -> Unit,
    onSelectSound: (AdhanSoundType) -> Unit,
    onPlayPreview: (AdhanSoundType) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .testTag("sound_selection_bottom_sheet"),
        ) {
            // Header with Prayer Name
            Text(
                text = "${prayer.displayName} Alert Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Control notification and alarm sound for this prayer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show Notification Toggle Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Notification",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Text(
                            text = if (isEnabled) "Notifications are turned ON" else "Notifications are turned OFF",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggleAlertEnabled,
                        modifier = Modifier.testTag("switch_prayer_enabled_${prayer.id}"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isEnabled) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No notification or alarm will be triggered for ${prayer.displayName}." +
                            " Enable the switch above to receive alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                Text(
                    text = "Alert Type & Sound:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(8.dp))

                PrayerSoundPicker(
                    currentSound = currentSound,
                    isPlayingSound = isPlayingSound,
                    playingSoundType = playingSoundType,
                    onSelectSound = onSelectSound,
                    onPlayPreview = onPlayPreview,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}
