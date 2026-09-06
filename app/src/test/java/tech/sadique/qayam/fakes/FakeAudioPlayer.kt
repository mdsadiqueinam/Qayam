package tech.sadique.qayam.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.sadique.qayam.audio.AudioPlayer
import tech.sadique.qayam.data.model.AdhanSoundType

class FakeAudioPlayer : AudioPlayer {

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentlyPlayingSound = MutableStateFlow<AdhanSoundType?>(null)
    override val currentlyPlayingSound: StateFlow<AdhanSoundType?> = _currentlyPlayingSound.asStateFlow()

    var playCount = 0
    var lastPlayedSound: AdhanSoundType? = null

    override fun playSound(
        soundType: AdhanSoundType,
        highPriority: Boolean,
        volume: Float,
        onComplete: (() -> Unit)?
    ) {
        if (soundType == AdhanSoundType.SILENT || soundType == AdhanSoundType.VIBRATE_ONLY) {
            onComplete?.invoke()
            return
        }
        playCount++
        lastPlayedSound = soundType
        _isPlaying.value = true
        _currentlyPlayingSound.value = soundType
    }

    override fun stopSound() {
        _isPlaying.value = false
        _currentlyPlayingSound.value = null
    }
}
