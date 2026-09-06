package tech.sadique.qayam.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tech.sadique.qayam.di.ApplicationScope
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class RingtonePlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope
) {
    private companion object {
        const val TAG = "RingtonePlayer"
    }

    private var activeRingtone: Ringtone? = null
    private var autoStopJob: Job? = null

    val isPlaying: Boolean
        get() = activeRingtone?.isPlaying == true

    fun playSystemAlarm(onComplete: (() -> Unit)?) {
        stop()

        val once = AtomicBoolean(false)
        val done: () -> Unit = {
            if (once.compareAndSet(false, true)) {
                onComplete?.invoke()
            }
        }

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
                done()
                return
            }

            val ringtone = RingtoneManager.getRingtone(context, alertUri)
            if (ringtone == null) {
                Log.w(TAG, "System ringtone unavailable; skipping system alarm")
                done()
                return
            }
            activeRingtone = ringtone

            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            ringtone.play()

            autoStopJob = externalScope.launch {
                delay(15.seconds)
                stop()
                done()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing system alarm", e)
            stop()
            done()
        }
    }

    fun stop() {
        autoStopJob?.cancel()
        autoStopJob = null

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
    }
}
