package tech.sadique.qayam.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.sadique.qayam.R
import tech.sadique.qayam.data.model.AdhanSoundType
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileAdhanPlayer @Inject constructor(@ApplicationContext private val context: Context) {

    private companion object {
        const val TAG = "FileAdhanPlayer"
    }

    private var activePlayer: MediaPlayer? = null

    val isPlaying: Boolean
        get() = try {
            activePlayer?.isPlaying == true
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Playback state check failed", e)
            false
        }

    fun playAdhan(soundType: AdhanSoundType, volume: Float, onComplete: (() -> Unit)?) {
        stop()
        val resId = resIdFor(soundType) ?: return
        val once = AtomicBoolean(false)
        val done: () -> Unit = {
            if (once.compareAndSet(false, true)) {
                onComplete?.invoke()
            }
        }
        try {
            val descriptor = context.resources.openRawResourceFd(resId)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                setVolume(volume, volume)
                setOnCompletionListener {
                    releasePlayer(this)
                    done()
                }
                setOnErrorListener { failedPlayer, _, _ ->
                    Log.e(TAG, "Error playing adhan file for $soundType")
                    releasePlayer(failedPlayer)
                    done()
                    true
                }
            }
            descriptor.close()
            activePlayer = player
            player.prepare()
            player.start()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error playing adhan file for $soundType", e)
            stop()
            done()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Error playing adhan file for $soundType", e)
            stop()
            done()
        } catch (e: SecurityException) {
            Log.e(TAG, "Error playing adhan file for $soundType", e)
            stop()
            done()
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Error playing adhan file for $soundType", e)
            stop()
            done()
        }
    }

    fun stop() {
        val player = activePlayer
        activePlayer = null
        if (player == null) {
            return
        }
        releasePlayer(player)
    }

    @RawRes
    private fun resIdFor(soundType: AdhanSoundType): Int? = when (soundType) {
        AdhanSoundType.MAKKAH -> R.raw.adhan_makkah
        AdhanSoundType.MADINAH -> R.raw.adhan_madinah
        AdhanSoundType.AL_AQSA -> R.raw.adhan_alaqsa
        else -> null
    }

    private fun releasePlayer(player: MediaPlayer) {
        if (activePlayer === player) {
            activePlayer = null
        }
        try {
            if (player.isPlaying) {
                player.stop()
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Error stopping adhan playback", e)
        } finally {
            try {
                player.release()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Error releasing adhan player", e)
            }
        }
    }
}
