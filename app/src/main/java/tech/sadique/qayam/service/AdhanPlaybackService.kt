package tech.sadique.qayam.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import tech.sadique.qayam.audio.AudioPlayer
import tech.sadique.qayam.data.model.AdhanSoundType
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.notification.PrayerNotificationNotifier
import javax.inject.Inject

@AndroidEntryPoint
class AdhanPlaybackService : Service() {

    @Inject
    lateinit var audioPlayer: AudioPlayer

    @Inject
    lateinit var notifier: PrayerNotificationNotifier

    companion object {
        private const val TAG = "AdhanPlaybackService"
        const val ACTION_START_PLAYBACK = "tech.sadique.qayam.service.ACTION_START_PLAYBACK"
        const val ACTION_STOP_PLAYBACK = "tech.sadique.qayam.service.ACTION_STOP_PLAYBACK"
        const val EXTRA_PRAYER_ID = "extra_prayer_id"
        const val EXTRA_SOUND_TYPE = "extra_sound_type"
        const val EXTRA_HIGH_PRIORITY = "extra_high_priority"

        private const val WAKELOCK_TIMEOUT_MS = 180_000L

        fun start(context: Context, prayerType: PrayerType, soundType: AdhanSoundType, highPriority: Boolean) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_START_PLAYBACK
                putExtra(EXTRA_PRAYER_ID, prayerType.id)
                putExtra(EXTRA_SOUND_TYPE, soundType.id)
                putExtra(EXTRA_HIGH_PRIORITY, highPriority)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdhanPlaybackService::class.java).apply {
                action = ACTION_STOP_PLAYBACK
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Could not start stop-service from background", e)
            } catch (e: SecurityException) {
                Log.w(TAG, "Could not start stop-service from background", e)
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        Log.d(TAG, "onStartCommand with action: $action")

        when (action) {
            ACTION_STOP_PLAYBACK,
            PrayerNotificationNotifier.ACTION_STOP_ADHAN,
            -> {
                stopPlaybackAndFinish()
            }

            ACTION_START_PLAYBACK -> {
                val prayerId = intent.getStringExtra(EXTRA_PRAYER_ID) ?: PrayerType.FAJR.id
                val prayerType = PrayerType.fromId(prayerId)
                val soundId = intent.getStringExtra(EXTRA_SOUND_TYPE) ?: AdhanSoundType.MAKKAH.id
                val soundType = AdhanSoundType.fromId(soundId)
                val highPriority = intent.getBooleanExtra(EXTRA_HIGH_PRIORITY, true)

                // Acquire WakeLock (max 3 minutes) to keep CPU awake during audio synthesis
                acquireWakeLock()

                // Start as Foreground Service with prayer alert notification
                val notification = notifier.buildPrayerNotification(prayerType, soundType, highPriority)
                val notificationId = PrayerNotificationNotifier.NOTIFICATION_ID_BASE + prayerType.ordinal

                val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                } else {
                    0
                }

                ServiceCompat.startForeground(
                    this,
                    notificationId,
                    notification,
                    foregroundServiceType,
                )

                // Play Audio
                audioPlayer.playSound(
                    soundType = soundType,
                    highPriority = highPriority,
                    volume = 1.0f,
                ) {
                    Log.d(TAG, "Audio synthesis complete, stopping playback service")
                    stopPlaybackAndFinish()
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "qayam:AdhanPlaybackWakeLock",
            )?.apply {
                setReferenceCounted(false)
                acquire(WAKELOCK_TIMEOUT_MS)
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Error releasing wake lock", e)
        } catch (e: SecurityException) {
            Log.w(TAG, "Error releasing wake lock", e)
        } finally {
            wakeLock = null
        }
    }

    private fun stopPlaybackAndFinish() {
        audioPlayer.stopSound()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlaybackAndFinish()
    }
}
