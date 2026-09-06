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

private const val SYNTH_SAMPLE_RATE = 22050
private const val MILLIS_PER_SECOND = 1000.0
private const val MIN_ATTACK_DECAY_SAMPLES = 1
private const val VIBRATO_FREQUENCY = 5.0
private const val VIBRATO_DEPTH = 0.015
private const val HARMONIC_2_GAIN = 0.45
private const val HARMONIC_3_GAIN = 0.20
private const val HARMONIC_4_GAIN = 0.08
private const val HARMONIC_NORMALIZATION = 1.73
private const val MAX_PCM_AMPLITUDE = 28000.0
private const val MIN_PCM_VALUE = -32768
private const val MAX_PCM_VALUE = 32767
private const val MIN_VOLUME = 0f
private const val MAX_VOLUME = 1f

private fun computeEnvelope(index: Int, numSamples: Int, attackSamples: Int, decaySamples: Int): Double = when {
    index < attackSamples -> index.toDouble() / attackSamples
    index > numSamples - decaySamples -> (numSamples - index).toDouble() / decaySamples
    else -> 1.0
}

private fun computeInstantFrequency(note: Note, timeSeconds: Double): Double {
    val vibratoFreq = if (note.vibrato) VIBRATO_FREQUENCY else 0.0
    val vibratoDepth = if (note.vibrato) VIBRATO_DEPTH else 0.0
    val twoPi = 2.0 * PI
    return note.freq * (1.0 + vibratoDepth * sin(twoPi * vibratoFreq * timeSeconds))
}

private fun mixHarmonics(instantFreq: Double, timeSeconds: Double): Double {
    val twoPi = 2.0 * PI
    val fundamental = sin(twoPi * instantFreq * timeSeconds)
    val harmonic2 = HARMONIC_2_GAIN * sin(twoPi * (instantFreq * 2.0) * timeSeconds)
    val harmonic3 = HARMONIC_3_GAIN * sin(twoPi * (instantFreq * 3.0) * timeSeconds)
    val harmonic4 = HARMONIC_4_GAIN * sin(twoPi * (instantFreq * 4.0) * timeSeconds)
    return (fundamental + harmonic2 + harmonic3 + harmonic4) / HARMONIC_NORMALIZATION
}

private fun synthesizeSample(note: Note, index: Int, numSamples: Int, attackSamples: Int, decaySamples: Int): Short {
    val timeSeconds = index.toDouble() / SYNTH_SAMPLE_RATE
    val envelope = computeEnvelope(index, numSamples, attackSamples, decaySamples)
    val instantFreq = computeInstantFrequency(note, timeSeconds)
    val rawSignal = mixHarmonics(instantFreq, timeSeconds)
    val sampleValue =
        (rawSignal * envelope * MAX_PCM_AMPLITUDE).toInt().coerceIn(MIN_PCM_VALUE, MAX_PCM_VALUE)
    return sampleValue.toShort()
}

@Singleton
class SynthPlayer @Inject constructor() {

    private companion object {
        const val TAG = "SynthPlayer"
    }

    private var activeAudioTrack: AudioTrack? = null

    val isPlaying: Boolean
        get() = activeAudioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING

    fun play(notes: List<Note>, highPriorityAlarm: Boolean, volumeMultiplier: Float, scope: CoroutineScope) {
        val track = buildAudioTrack(highPriorityAlarm, volumeMultiplier)
        activeAudioTrack = track
        track.play()

        try {
            renderSequence(track, notes, scope)
        } finally {
            releaseTrack(track)
        }
    }

    private fun buildAudioTrack(highPriorityAlarm: Boolean, volumeMultiplier: Float): AudioTrack {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SYNTH_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = maxOf(minBufferSize, SYNTH_SAMPLE_RATE * 2)
        val track = AudioTrack.Builder()
            .setAudioAttributes(buildAudioAttributes(highPriorityAlarm))
            .setAudioFormat(buildAudioFormat())
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.setVolume(volumeMultiplier.coerceIn(MIN_VOLUME, MAX_VOLUME))
        return track
    }

    private fun buildAudioAttributes(highPriorityAlarm: Boolean): AudioAttributes = AudioAttributes.Builder()
        .apply {
            if (highPriorityAlarm) {
                setUsage(AudioAttributes.USAGE_ALARM)
            } else {
                setUsage(AudioAttributes.USAGE_NOTIFICATION)
            }
            setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            if (highPriorityAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE)
            }
        }.build()

    private fun buildAudioFormat(): AudioFormat = AudioFormat.Builder()
        .setSampleRate(SYNTH_SAMPLE_RATE)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .build()

    private fun renderSequence(track: AudioTrack, notes: List<Note>, scope: CoroutineScope) {
        for (note in notes) {
            val shouldContinue = renderSingleNote(track, note, scope)
            if (!shouldContinue) break
        }
    }

    private fun renderSingleNote(track: AudioTrack, note: Note, scope: CoroutineScope): Boolean {
        val result = when {
            !scope.isActive -> NoteRenderResult.Cancelled

            note.freq <= 0.0 -> {
                renderSilence(track, note)
                NoteRenderResult.Handled
            }

            else -> renderTonalNote(track, note, scope)
        }
        return result == NoteRenderResult.Handled
    }

    private fun renderTonalNote(track: AudioTrack, note: Note, scope: CoroutineScope): NoteRenderResult {
        val samples = synthesizeSamples(note, scope)
        if (samples == null || !scope.isActive) return NoteRenderResult.Cancelled
        track.write(samples, 0, samples.size)
        return NoteRenderResult.Handled
    }

    private fun renderSilence(track: AudioTrack, note: Note) {
        val silenceSamples = (SYNTH_SAMPLE_RATE * (note.durationMs / MILLIS_PER_SECOND)).toInt()
        val silenceBuffer = ShortArray(silenceSamples)
        track.write(silenceBuffer, 0, silenceBuffer.size)
    }

    private fun synthesizeSamples(note: Note, scope: CoroutineScope): ShortArray? {
        val numSamples = (SYNTH_SAMPLE_RATE * (note.durationMs / MILLIS_PER_SECOND)).toInt()
        val samples = ShortArray(numSamples)
        val attackSamples =
            (SYNTH_SAMPLE_RATE * (note.attackMs / MILLIS_PER_SECOND)).toInt().coerceAtLeast(MIN_ATTACK_DECAY_SAMPLES)
        val decaySamples =
            (SYNTH_SAMPLE_RATE * (note.decayMs / MILLIS_PER_SECOND)).toInt().coerceAtLeast(MIN_ATTACK_DECAY_SAMPLES)
        for (i in 0 until numSamples) {
            if (!scope.isActive) return null
            samples[i] = synthesizeSample(note, i, numSamples, attackSamples, decaySamples)
        }
        return samples
    }

    private fun releaseTrack(track: AudioTrack) {
        try {
            track.stop()
            track.release()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "AudioTrack cleanup error", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "AudioTrack cleanup error", e)
        } finally {
            if (activeAudioTrack === track) {
                activeAudioTrack = null
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
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        } finally {
            activeAudioTrack = null
        }
    }
}

private enum class NoteRenderResult {
    Handled,
    Cancelled,
}
