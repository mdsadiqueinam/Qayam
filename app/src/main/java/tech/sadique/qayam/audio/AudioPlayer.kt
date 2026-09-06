package tech.sadique.qayam.audio

import kotlinx.coroutines.flow.StateFlow
import tech.sadique.qayam.data.model.AdhanSoundType

interface AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val currentlyPlayingSound: StateFlow<AdhanSoundType?>
    fun playSound(
        soundType: AdhanSoundType,
        highPriority: Boolean = true,
        volume: Float = 1.0f,
        onComplete: (() -> Unit)? = null,
    )
    fun stopSound()
}
