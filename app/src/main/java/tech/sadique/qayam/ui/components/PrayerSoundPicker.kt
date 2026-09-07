package tech.sadique.qayam.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.sadique.qayam.data.model.AdhanSoundType

@Composable
fun PrayerSoundPicker(
    currentSound: AdhanSoundType,
    isPlayingSound: Boolean,
    playingSoundType: AdhanSoundType?,
    onSelectSound: (AdhanSoundType) -> Unit,
    onPlayPreview: (AdhanSoundType) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
    ) {
        items(AdhanSoundType.entries) { sound ->
            SoundOptionRow(
                sound = sound,
                isSelected = sound == currentSound,
                isPlaying = isPlayingSound && playingSoundType == sound,
                onSelectSound = onSelectSound,
                onPlayPreview = onPlayPreview,
            )
        }
    }
}

@Composable
private fun SoundOptionRow(
    sound: AdhanSoundType,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelectSound: (AdhanSoundType) -> Unit,
    onPlayPreview: (AdhanSoundType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = soundRowColor(isSelected),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelectSound(sound) }
            .testTag("sound_dialog_option_${sound.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(selected = isSelected, onClick = { onSelectSound(sound) })
                SoundOptionLabel(sound = sound, isSelected = isSelected)
            }
            if (isPreviewable(sound)) {
                SoundPreviewButton(sound = sound, isPlaying = isPlaying, onPlayPreview = onPlayPreview)
            }
        }
    }
}

@Composable
private fun soundRowColor(isSelected: Boolean) = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
} else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
}

private fun isPreviewable(sound: AdhanSoundType) =
    sound != AdhanSoundType.SILENT && sound != AdhanSoundType.VIBRATE_ONLY

@Composable
private fun SoundOptionLabel(sound: AdhanSoundType, isSelected: Boolean) {
    Column {
        Text(
            text = sound.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = sound.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SoundPreviewButton(sound: AdhanSoundType, isPlaying: Boolean, onPlayPreview: (AdhanSoundType) -> Unit) {
    IconButton(
        onClick = { onPlayPreview(sound) },
        modifier = Modifier.testTag("preview_dialog_${sound.id}"),
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.GraphicEq,
            contentDescription = "Preview ${sound.title}" + if (isPlaying) ", playing, tap to stop" else "",
            tint = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}
