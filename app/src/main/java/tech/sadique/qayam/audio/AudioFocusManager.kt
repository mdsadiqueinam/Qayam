package tech.sadique.qayam.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioFocusManager @Inject constructor(@ApplicationContext private val context: Context) {
    private companion object {
        const val TAG = "AudioFocusManager"
    }

    private var focusRequest: Any? = null
    private var focusListener: AudioManager.OnAudioFocusChangeListener? = null

    fun requestAudioFocus(highPriorityAlarm: Boolean, onFocusLoss: () -> Unit): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return false
            val usage = if (highPriorityAlarm) {
                AudioAttributes.USAGE_ALARM
            } else {
                AudioAttributes.USAGE_NOTIFICATION
            }
            val attrs = AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val listener = AudioManager.OnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    -> onFocusLoss()
                }
            }
            focusListener = listener

            val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(listener)
                    .build()
                focusRequest = req
                audioManager.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    listener,
                    if (highPriorityAlarm) AudioManager.STREAM_ALARM else AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
                )
            }
            res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Audio focus request failed", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Audio focus request failed", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Audio focus request failed", e)
            false
        }
    }

    fun abandonAudioFocus() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (focusRequest as? AudioFocusRequest)?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                focusListener?.let { audioManager.abandonAudioFocus(it) }
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Audio focus abandon failed", e)
        } catch (e: SecurityException) {
            Log.w(TAG, "Audio focus abandon failed", e)
        } finally {
            focusRequest = null
            focusListener = null
        }
    }
}
