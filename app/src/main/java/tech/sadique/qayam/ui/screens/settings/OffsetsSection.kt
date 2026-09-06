package tech.sadique.qayam.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.sadique.qayam.data.model.PrayerType

@Composable
fun OffsetsSection(
    minuteOffsets: Map<PrayerType, Int>,
    onUpdateOffset: (PrayerType, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSectionCard(
        title = "Prayer Time Adjustments",
        icon = Icons.Default.Tune,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Fine-tune prayer times in minutes (+/-) if your local mosque timetable differs slightly",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PrayerType.dailyPrayers.forEach { prayer ->
                val offset = minuteOffsets[prayer] ?: 0
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
                                onClick = { onUpdateOffset(prayer, offset - 1) }
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Decrease ${prayer.displayName} offset by one minute",
                                    modifier = Modifier.size(16.dp)
                                )
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
                                onClick = { onUpdateOffset(prayer, offset + 1) }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Increase ${prayer.displayName} offset by one minute",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
