package tech.sadique.qayam.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import tech.sadique.qayam.data.model.AdhanSoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds
import kotlin.math.PI
import kotlin.math.sin

object AdhanAudioSynthesizer {

    private const val TAG = "AdhanAudio"
    private const val SAMPLE_RATE = 22050

    private val synthScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeAudioTrack: AudioTrack? = null
    private var activeRingtone: Ringtone? = null
    private var playJob: Job? = null
    private var focusRequest: Any? = null
    private var focusListener: AudioManager.OnAudioFocusChangeListener? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentlyPlayingSound = MutableStateFlow<AdhanSoundType?>(null)
    val currentlyPlayingSound: StateFlow<AdhanSoundType?> = _currentlyPlayingSound.asStateFlow()

    fun playSound(
        context: Context,
        soundType: AdhanSoundType,
        highPriorityAlarm: Boolean = true,
        volume: Float = 1.0f,
        onComplete: (() -> Unit)? = null
    ) {
        stopSound()

        if (soundType == AdhanSoundType.SILENT || soundType == AdhanSoundType.VIBRATE_ONLY) {
            onComplete?.invoke()
            return
        }

        val once = AtomicBoolean(false)
        val done: () -> Unit = {
            if (once.compareAndSet(false, true)) onComplete?.invoke()
        }

        _isPlaying.value = true
        _currentlyPlayingSound.value = soundType

        if (!requestAudioFocus(context.applicationContext, highPriorityAlarm)) {
            Log.w(TAG, "Audio focus denied; playing anyway at requested volume")
        }

        if (soundType == AdhanSoundType.SYSTEM_ALARM) {
            playSystemAlarm(context, done)
            return
        }

        playJob = synthScope.launch {
            try {
                val notes = getMelodySequence(soundType)
                playSynthesizedSequence(notes, highPriorityAlarm, volume, this)
            } catch (e: Exception) {
                Log.e(TAG, "Audio synthesis error", e)
            } finally {
                abandonAudioFocus(context.applicationContext)
                _isPlaying.value = false
                _currentlyPlayingSound.value = null
                done()
            }
        }
    }

    fun stopSound() {
        playJob?.cancel()
        playJob = null

        try {
            activeAudioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        } finally {
            activeAudioTrack = null
        }

        try {
            activeRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Ringtone", e)
        } finally {
            activeRingtone = null
        }

        _isPlaying.value = false
        _currentlyPlayingSound.value = null
    }

    private fun requestAudioFocus(context: Context, highPriorityAlarm: Boolean): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return false
            val usage = if (highPriorityAlarm) AudioAttributes.USAGE_ALARM
                else AudioAttributes.USAGE_NOTIFICATION
            val attrs = AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            focusListener = AudioManager.OnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> stopSound()
                }
            }
            val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(focusListener!!)
                    .build()
                focusRequest = req
                audioManager.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    focusListener,
                    if (highPriorityAlarm) AudioManager.STREAM_ALARM else AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
            }
            res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus request failed", e)
            false
        }
    }

    private fun abandonAudioFocus(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (focusRequest as? android.media.AudioFocusRequest)?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                focusListener?.let { audioManager.abandonAudioFocus(it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus abandon failed", e)
        } finally {
            focusRequest = null
            focusListener = null
        }
    }

    private fun playSystemAlarm(context: Context, onComplete: (() -> Unit)?) {
        val appContext = context.applicationContext
        try {
            var alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            if (alertUri == null) {
                Log.w(TAG, "No system ringtone URI available; skipping system alarm")
                abandonAudioFocus(appContext)
                _isPlaying.value = false
                _currentlyPlayingSound.value = null
                onComplete?.invoke()
                return
            }

            val ringtone = RingtoneManager.getRingtone(appContext, alertUri)
            if (ringtone == null) {
                Log.w(TAG, "System ringtone unavailable; skipping system alarm")
                abandonAudioFocus(appContext)
                _isPlaying.value = false
                _currentlyPlayingSound.value = null
                onComplete?.invoke()
                return
            }
            activeRingtone = ringtone

            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            ringtone.play()

            // Auto-stop system ringtone after 15 seconds (once-guarded by caller)
            playJob = synthScope.launch {
                kotlinx.coroutines.delay(15.seconds)
                stopSound()
                abandonAudioFocus(appContext)
                _isPlaying.value = false
                _currentlyPlayingSound.value = null
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing system alarm", e)
            abandonAudioFocus(appContext)
            _isPlaying.value = false
            _currentlyPlayingSound.value = null
            onComplete?.invoke()
        }
    }

    private data class Note(
        val freq: Double,
        val durationMs: Int,
        val attackMs: Int = 100,
        val decayMs: Int = 150,
        val vibrato: Boolean = true
    )

    private fun getMelodySequence(soundType: AdhanSoundType): List<Note> {
        return when (soundType) {
            AdhanSoundType.TAKBEER_ONLY -> listOf(
                // Al-laa-hu Ak-bar (Takbeer 1)
                Note(220.0, 450, 100, 100),
                Note(261.63, 600, 120, 150),
                Note(293.66, 750, 150, 200),
                Note(261.63, 500, 100, 150),
                Note(220.0, 900, 150, 350),
                Note(0.0, 400),
                // Al-laa-hu Ak-bar (Takbeer 2)
                Note(220.0, 450, 100, 100),
                Note(261.63, 600, 120, 150),
                Note(329.63, 850, 180, 250),
                Note(293.66, 500, 100, 150),
                Note(261.63, 1100, 200, 400)
            )

            AdhanSoundType.GENTLE_CHIME -> listOf(
                Note(523.25, 600, 30, 500, vibrato = false), // C5
                Note(659.25, 600, 30, 500, vibrato = false), // E5
                Note(783.99, 800, 30, 700, vibrato = false), // G5
                Note(1046.50, 1200, 30, 1000, vibrato = false) // C6
            )

            AdhanSoundType.MAKKAH -> listOf(
                // Allahu Akbar (Bayati / Rast Maqam style)
                Note(220.00, 500, 100, 120),  // A3
                Note(246.94, 550, 100, 150),  // B3
                Note(293.66, 900, 180, 300),  // D4
                Note(261.63, 650, 120, 200),  // C4
                Note(220.00, 1200, 200, 450), // A3
                Note(0.0, 350),

                // Allahu Akbar (Rising Call)
                Note(220.00, 450, 100, 100),  // A3
                Note(293.66, 650, 140, 200),  // D4
                Note(349.23, 1100, 220, 400), // F4
                Note(329.63, 700, 150, 250),  // E4
                Note(293.66, 1300, 220, 500), // D4
                Note(0.0, 400),

                // Ash-hadu alla ilaha illallah
                Note(293.66, 600, 120, 150),  // D4
                Note(349.23, 750, 150, 200),  // F4
                Note(392.00, 1200, 250, 450), // G4
                Note(349.23, 600, 120, 200),  // F4
                Note(293.66, 1400, 250, 550), // D4
                Note(0.0, 400),

                // Hayya 'alas-Salah
                Note(329.63, 600, 120, 180),  // E4
                Note(392.00, 900, 180, 300),  // G4
                Note(440.00, 1300, 250, 500), // A4
                Note(392.00, 600, 120, 200),  // G4
                Note(349.23, 1500, 250, 600)  // F4
            )

            AdhanSoundType.MADINAH -> listOf(
                // Hijaz/Saba Maqam tone
                Note(196.00, 600, 120, 180),  // G3
                Note(233.08, 700, 140, 220),  // Bb3
                Note(293.66, 1100, 200, 400), // D4
                Note(246.94, 600, 120, 200),  // B3
                Note(196.00, 1400, 250, 550), // G3
                Note(0.0, 400),

                // Allahu Akbar Part 2
                Note(233.08, 550, 120, 180),  // Bb3
                Note(293.66, 800, 160, 250),  // D4
                Note(369.99, 1300, 250, 450), // F#4
                Note(293.66, 700, 140, 250),  // D4
                Note(233.08, 1500, 250, 600)  // Bb3
            )

            AdhanSoundType.AL_AQSA -> listOf(
                // Majestic resonant melody
                Note(220.00, 600, 120, 200),  // A3
                Note(277.18, 750, 150, 250),  // C#4
                Note(329.63, 1100, 220, 400), // E4
                Note(277.18, 650, 140, 220),  // C#4
                Note(220.00, 1350, 250, 500), // A3
                Note(0.0, 350),

                Note(277.18, 550, 120, 180),  // C#4
                Note(329.63, 750, 150, 250),  // E4
                Note(415.30, 1250, 240, 450), // G#4
                Note(329.63, 700, 140, 220),  // E4
                Note(277.18, 1450, 250, 600)  // C#4
            )

            else -> listOf(
                Note(440.0, 500),
                Note(554.37, 500),
                Note(659.25, 800)
            )
        }
    }

    private fun playSynthesizedSequence(
        notes: List<Note>,
        highPriorityAlarm: Boolean,
        volumeMultiplier: Float,
        scope: CoroutineScope
    ) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, SAMPLE_RATE * 2)

        val audioAttributes = AudioAttributes.Builder().apply {
            if (highPriorityAlarm) {
                setUsage(AudioAttributes.USAGE_ALARM)
                setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE)
                }
            } else {
                setUsage(AudioAttributes.USAGE_NOTIFICATION)
                setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            }
        }.build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        val track = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        activeAudioTrack = track

        track.setVolume(volumeMultiplier.coerceIn(0f, 1f))

        track.play()

        for (note in notes) {
            if (!scope.isActive) break

            if (note.freq <= 0.0) {
                // Pause / rest
                val silenceSamples = (SAMPLE_RATE * (note.durationMs / 1000.0)).toInt()
                val silenceBuffer = ShortArray(silenceSamples)
                track.write(silenceBuffer, 0, silenceBuffer.size)
                continue
            }

            val numSamples = (SAMPLE_RATE * (note.durationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)

            val attackSamples = (SAMPLE_RATE * (note.attackMs / 1000.0)).toInt().coerceAtLeast(1)
            val decaySamples = (SAMPLE_RATE * (note.decayMs / 1000.0)).toInt().coerceAtLeast(1)

            val f = note.freq
            val twoPi = 2.0 * PI

            for (i in 0 until numSamples) {
                if (!scope.isActive) break
                val t = i.toDouble() / SAMPLE_RATE

                // ADSR Envelope
                val envelope = when {
                    i < attackSamples -> i.toDouble() / attackSamples
                    i > numSamples - decaySamples -> (numSamples - i).toDouble() / decaySamples
                    else -> 1.0
                }

                // Subtle natural vocal vibrato
                val vibratoFreq = if (note.vibrato) 5.0 else 0.0
                val vibratoDepth = if (note.vibrato) 0.015 else 0.0
                val instantFreq = f * (1.0 + vibratoDepth * sin(twoPi * vibratoFreq * t))

                // Rich harmonic acoustics (fundamental + warm 2nd & 3rd harmonics + warm resonant undertone)
                val fundamental = sin(twoPi * instantFreq * t)
                val harmonic2 = 0.45 * sin(twoPi * (instantFreq * 2.0) * t)
                val harmonic3 = 0.20 * sin(twoPi * (instantFreq * 3.0) * t)
                val harmonic4 = 0.08 * sin(twoPi * (instantFreq * 4.0) * t)

                val rawSignal = (fundamental + harmonic2 + harmonic3 + harmonic4) / 1.73
                val sampleValue = (rawSignal * envelope * 28000.0).toInt().coerceIn(-32768, 32767)

                samples[i] = sampleValue.toShort()
            }

            if (!scope.isActive) break
            track.write(samples, 0, samples.size)
        }

        // Flush and release
        try {
            track.stop()
            track.release()
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack cleanup error", e)
        } finally {
            if (activeAudioTrack === track) {
                activeAudioTrack = null
            }
        }
    }
}
