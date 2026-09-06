@file:Suppress(
    "CyclomaticComplexMethod",
    "LongMethod",
    "LoopWithTooManyJumpStatements",
    "TooGenericExceptionCaught",
    "MagicNumber",
)

package tech.sadique.qayam.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

@Singleton
class SynthPlayer @Inject constructor() {

    private companion object {
        const val TAG = "SynthPlayer"
        const val SAMPLE_RATE = 22050
    }

    private var activeAudioTrack: AudioTrack? = null

    val isPlaying: Boolean
        get() = activeAudioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING

    fun play(notes: List<Note>, highPriorityAlarm: Boolean, volumeMultiplier: Float, scope: CoroutineScope) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
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

        try {
            for (note in notes) {
                if (!scope.isActive) break

                if (note.freq <= 0.0) {
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

                    // Harmonic acoustics
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
        } finally {
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

    fun stop() {
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
    }
}
