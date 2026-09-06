package tech.sadique.qayam.audio

import kotlinx.coroutines.flow.StateFlow
import tech.sadique.qayam.data.model.AdhanSoundType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPreviewController @Inject constructor(
    private val audioPlayer: AudioPlayer
) {
    val isPlayingSound: StateFlow<Boolean> = audioPlayer.isPlaying
    val playingSoundType: StateFlow<AdhanSoundType?> = audioPlayer.currentlyPlayingSound

    fun playPreviewSound(soundType: AdhanSoundType, highPriority: Boolean) {
        if (isPlayingSound.value && playingSoundType.value == soundType) {
            audioPlayer.stopSound()
        } else {
            audioPlayer.playSound(
                soundType = soundType,
                highPriority = highPriority,
                volume = 1.0f
            )
        }
    }

    fun stopPreviewSound() {
        audioPlayer.stopSound()
    }
}
