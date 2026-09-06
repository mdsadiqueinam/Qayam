package tech.sadique.qayam.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.sadique.qayam.data.location.CityPresets
import tech.sadique.qayam.data.model.LocationInfo
import java.util.Locale

@Composable
fun LocationSection(
    currentLocation: LocationInfo,
    onRefreshGps: () -> Unit,
    onSelectPreset: (LocationInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSectionCard(
        title = "Location & Offline Presets",
        icon = Icons.Default.LocationCity,
        modifier = modifier
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
                        text = "${currentLocation.cityName} (${String.format(Locale.US, "%.2f", currentLocation.latitude)}°, ${String.format(Locale.US, "%.2f", currentLocation.longitude)}°)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onRefreshGps,
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
                items(CityPresets.LIST) { preset ->
                    val isCurrentCity = currentLocation.cityName.equals(preset.cityName, ignoreCase = true)
                    FilterChip(
                        selected = isCurrentCity,
                        onClick = { onSelectPreset(preset) },
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
