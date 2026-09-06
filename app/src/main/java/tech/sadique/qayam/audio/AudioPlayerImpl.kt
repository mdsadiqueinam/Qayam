package tech.sadique.qayam.audio

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.sadique.qayam.data.model.AdhanSoundType
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerImpl @Inject constructor(
    private val audioFocusManager: AudioFocusManager,
    private val ringtonePlayer: RingtonePlayer,
    private val fileAdhanPlayer: FileAdhanPlayer,
) : AudioPlayer {

    private companion object {
        const val TAG = "AudioPlayerImpl"
        const val MIN_VOLUME = 0f
        const val MAX_VOLUME = 1f
    }

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentlyPlayingSound = MutableStateFlow<AdhanSoundType?>(null)
    override val currentlyPlayingSound: StateFlow<AdhanSoundType?> = _currentlyPlayingSound.asStateFlow()

    override fun playSound(
        soundType: AdhanSoundType,
        highPriority: Boolean,
        volume: Float,
        onComplete: (() -> Unit)?,
    ) {
        stopSound()

        if (soundType == AdhanSoundType.SILENT || soundType == AdhanSoundType.VIBRATE_ONLY) {
            onComplete?.invoke()
            return
        }

        val once = AtomicBoolean(false)
        val done: () -> Unit = {
            if (once.compareAndSet(false, true)) {
                onComplete?.invoke()
            }
        }

        _isPlaying.value = true
        _currentlyPlayingSound.value = soundType

        if (!audioFocusManager.requestAudioFocus(highPriority, onFocusLoss = { stopSound() })) {
            Log.w(TAG, "Audio focus denied; playing anyway at requested volume")
        }

        if (soundType == AdhanSoundType.SYSTEM_ALARM) {
            ringtonePlayer.playSystemAlarm {
                finishPlayback()
                done()
            }
            return
        }

        fileAdhanPlayer.playAdhan(soundType, volume.coerceIn(MIN_VOLUME, MAX_VOLUME)) {
            finishPlayback()
            done()
        }
    }

    override fun stopSound() {
        fileAdhanPlayer.stop()
        ringtonePlayer.stop()
        audioFocusManager.abandonAudioFocus()

        _isPlaying.value = false
        _currentlyPlayingSound.value = null
    }

    private fun finishPlayback() {
        audioFocusManager.abandonAudioFocus()
        _isPlaying.value = false
        _currentlyPlayingSound.value = null
    }
}
