package tech.sadique.qayam.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.ui.theme.SalahTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun PrayerCard(
    prayer: PrayerType,
    time: Calendar,
    isCurrent: Boolean,
    isNext: Boolean,
    is24Hour: Boolean,
    soundType: AdhanSoundType,
    isEnabled: Boolean,
    isPlayingThisSound: Boolean,
    onToggleAlert: () -> Unit,
    onSoundClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember(is24Hour) {
        if (is24Hour) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("h:mm a", Locale.getDefault())
        }
    }
    val formattedTime = remember(time.timeInMillis, is24Hour) {
        timeFormatter.format(time.time)
    }

    val icon: ImageVector = when (prayer) {
        PrayerType.FAJR -> Icons.Default.WbTwilight
        PrayerType.SUNRISE -> Icons.Default.Brightness5
        PrayerType.ISRAQ -> Icons.Default.Brightness5
        PrayerType.DHUHR -> Icons.Default.Brightness7
        PrayerType.ASR -> Icons.Default.Brightness6
        PrayerType.GURUB_E_AFTAB -> Icons.Default.WbTwilight
        PrayerType.MAGHRIB -> Icons.Default.WbTwilight
        PrayerType.ISHA -> Icons.Default.Brightness2
    }

    val cardBgColor by animateColorAsState(
        targetValue = when {
            isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            isNext -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "CardBgColor"
    )

    val borderStroke = when {
        isCurrent -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        isNext -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("prayer_card_${prayer.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrent) 6.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon + Names
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = prayer.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isCurrent || isNext) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )

                        if (isCurrent) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "NOW",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isNext) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "NEXT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = prayer.arabicName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        prayer.subtitle?.let { sub ->
                            Text(
                                text = "• $sub",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Right: Time + Alert Audio Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("prayer_time_${prayer.id}")
                )

                // Alert Button
                val alertIcon = when {
                    !isEnabled -> Icons.Default.NotificationsOff
                    soundType == AdhanSoundType.SILENT -> Icons.Default.NotificationsNone
                    soundType == AdhanSoundType.VIBRATE_ONLY -> Icons.Default.Vibration
                    isPlayingThisSound -> Icons.Default.GraphicEq
                    else -> Icons.Default.NotificationsActive
                }

                val alertDescription = when {
                    !isEnabled -> "${prayer.displayName} alert: Muted (No notification)"
                    soundType == AdhanSoundType.SILENT -> "${prayer.displayName} alert: Visual only (Silent notification)"
                    soundType == AdhanSoundType.VIBRATE_ONLY -> "${prayer.displayName} alert: Vibrate only"
                    else -> "${prayer.displayName} alert: ${soundType.title}"
                }

                Surface(
                    shape = CircleShape,
                    color = if (isPlayingThisSound) MaterialTheme.colorScheme.secondary
                    else if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(38.dp)
                ) {
                    IconButton(
                        onClick = onSoundClick,
                        modifier = Modifier.testTag("sound_btn_${prayer.id}")
                    ) {
                        Icon(
                            imageVector = alertIcon,
                            contentDescription = alertDescription,
                            tint = if (isPlayingThisSound) MaterialTheme.colorScheme.onSecondary
                            else if (isEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Prayer card current", showBackground = true)
@Preview(
    name = "Prayer card current dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PrayerCardPreview() {
    SalahTheme {
        PrayerCard(
            prayer = PrayerType.FAJR,
            time = Calendar.getInstance(),
            isCurrent = true,
            isNext = false,
            is24Hour = false,
            soundType = AdhanSoundType.MAKKAH,
            isEnabled = true,
            isPlayingThisSound = false,
            onToggleAlert = {},
            onSoundClick = {}
        )
    }
}

@Preview(name = "Prayer card muted", showBackground = true)
@Composable
private fun PrayerCardMutedPreview() {
    SalahTheme {
        PrayerCard(
            prayer = PrayerType.SUNRISE,
            time = Calendar.getInstance(),
            isCurrent = false,
            isNext = false,
            is24Hour = true,
            soundType = AdhanSoundType.SILENT,
            isEnabled = false,
            isPlayingThisSound = false,
            onToggleAlert = {},
            onSoundClick = {}
        )
    }
}
