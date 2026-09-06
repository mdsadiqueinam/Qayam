package tech.sadique.qayam.audio

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.di.ApplicationScope
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerImpl @Inject constructor(
    private val audioFocusManager: AudioFocusManager,
    private val ringtonePlayer: RingtonePlayer,
    private val synthPlayer: SynthPlayer,
    private val melodyRepository: MelodyRepository,
    @ApplicationScope private val externalScope: CoroutineScope
) : AudioPlayer {

    constructor(@ApplicationContext context: Context) : this(
        audioFocusManager = AudioFocusManager(context),
        ringtonePlayer = RingtonePlayer(context, CoroutineScope(SupervisorJob() + Dispatchers.Default)),
        synthPlayer = SynthPlayer(),
        melodyRepository = MelodyRepository(),
        externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )

    private companion object {
        const val TAG = "AudioPlayerImpl"
    }

    private var playJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentlyPlayingSound = MutableStateFlow<AdhanSoundType?>(null)
    override val currentlyPlayingSound: StateFlow<AdhanSoundType?> = _currentlyPlayingSound.asStateFlow()

    override fun playSound(
        soundType: AdhanSoundType,
        highPriority: Boolean,
        volume: Float,
        onComplete: (() -> Unit)?
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
                audioFocusManager.abandonAudioFocus()
                _isPlaying.value = false
                _currentlyPlayingSound.value = null
                done()
            }
            return
        }

        playJob = externalScope.launch(Dispatchers.Default) {
            try {
                val notes = melodyRepository.getMelodySequence(soundType)
                synthPlayer.play(notes, highPriority, volume, this)
            } catch (e: Exception) {
                Log.e(TAG, "Audio synthesis error", e)
            } finally {
                audioFocusManager.abandonAudioFocus()
                _isPlaying.value = false
                _currentlyPlayingSound.value = null
                done()
            }
        }
    }

    override fun stopSound() {
        playJob?.cancel()
        playJob = null

        synthPlayer.stop()
        ringtonePlayer.stop()
        audioFocusManager.abandonAudioFocus()

        _isPlaying.value = false
        _currentlyPlayingSound.value = null
    }
}
