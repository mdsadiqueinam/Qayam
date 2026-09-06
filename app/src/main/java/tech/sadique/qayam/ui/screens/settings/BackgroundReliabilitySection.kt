

package tech.sadique.qayam.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val TEST_ALARM_DELAY_SECONDS = 10

@Composable
fun BackgroundReliabilitySection(
    canExactAlarms: Boolean,
    isBatteryIgnored: Boolean,
    onScheduleTestAlarm: (Int) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var testAlarmScheduled by rememberSaveable { mutableStateOf(false) }

    SettingsSectionCard(
        title = "Background Reliability",
        icon = Icons.Default.NotificationsActive,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Ensure prayer notifications and Adhan audio fire precisely on time even when the" +
                    " screen is locked.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Exact Alarm Permission
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exact Alarm Permission",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (canExactAlarms) {
                                    "Granted (Alarms trigger exactly on time)"
                                } else {
                                    "Denied (Alerts may be delayed by Android)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (canExactAlarms) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                        Icon(
                            imageVector = if (canExactAlarms) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (canExactAlarms) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }

                    if (!canExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        OutlinedButton(
                            onClick = onOpenExactAlarmSettings,
                            modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Battery Optimization",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (isBatteryIgnored) {
                                    "Unrestricted (Recommended)"
                                } else {
                                    "Optimized (OS may throttle background alarms)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBatteryIgnored) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                        Icon(
                            imageVector = if (isBatteryIgnored) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isBatteryIgnored) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }

                    if (!isBatteryIgnored) {
                        OutlinedButton(
                            onClick = onOpenBatterySettings,
                            modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Test Background Alert (10 Seconds)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Tap to schedule a test alert in 10 seconds, then lock your device or leave the" +
                            " app to test background wakeup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Button(
                        onClick = {
                            onScheduleTestAlarm(TEST_ALARM_DELAY_SECONDS)
                            testAlarmScheduled = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (testAlarmScheduled) {
                                "Test Alert Scheduled (Fires in 10s)"
                            } else {
                                "Schedule 10s Test Alert"
                            },
                        )
                    }

                    if (testAlarmScheduled) {
                        Text(
                            text = "✓ Scheduled! Lock your device now to verify.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
