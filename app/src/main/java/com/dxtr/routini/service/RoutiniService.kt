package com.dxtr.routini.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.dxtr.routini.MainActivity
import com.dxtr.routini.R
import com.dxtr.routini.receiver.AlarmReceiver
import com.dxtr.routini.ui.AlarmActivity

class RoutiniService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        createNotificationChannel()

        // FIX: Add foregroundServiceType for Android 14+ compliance
        startForeground(
            NOTIFICATION_ID,
            createNotification("Routini is Active", "Monitoring your routines and tasks"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_PLAY) {
            val soundUriString = intent.getStringExtra(EXTRA_SOUND_URI)
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "Alarm"
            val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
            val taskType = intent.getStringExtra(EXTRA_TASK_TYPE)

            playSound(soundUriString)
            startVibration()

            val notification = createAlarmNotification(title, taskId, taskType)
            startForeground(NOTIFICATION_ID, notification)
        } else if (action == ACTION_STOP) {
            stopSound()
            stopSelf()
        }

        return START_STICKY
    }

    private fun playSound(soundUriString: String?) {
        stopSound() // Stop any previous playback and abandon focus

        // FIX 1: Use AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE to force Spotify/others to PAUSE, not duck.
        // We use AUDIOFOCUS_GAIN as a fallback for older SDKs to ensure interruption.
        val focusType = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val request = AudioFocusRequest.Builder(focusType)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener {
                // Optional: Handle if WE lose focus (e.g. phone call comes in)
                // if (it == AudioManager.AUDIOFOCUS_LOSS) stopSound()
            }
            .build()
        focusRequest = request
        val focusResult = audioManager?.requestAudioFocus(request)


        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w("RoutiniService", "Could not gain audio focus, but playing alarm anyway.")
        }

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            try {
                // FIX 2: Better Fallback logic for Default Sound
                var soundUri: Uri? = null

                // 1. Try custom sound
                if (!soundUriString.isNullOrEmpty()) {
                    soundUri = soundUriString.toUri()
                }

                // 2. Try Default Alarm
                if (soundUri == null) {
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                }

                // 3. Try Default Notification (if Alarm is silent/null)
                if (soundUri == null) {
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }

                // 4. Try Default Ringtone (Last resort)
                if (soundUri == null) {
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }

                if (soundUri != null) {
                    setDataSource(this@RoutiniService, soundUri)
                    prepareAsync()
                    setOnPreparedListener { it.start() }
                    setOnCompletionListener { stopSelf() }
                } else {
                    Log.e("RoutiniService", "No valid sound URI found to play.")
                }
            } catch (e: Exception) {
                Log.e("RoutiniService", "Error setting data source", e)
                stopSelf()
            }
        }
    }

    private fun startVibration() {
        stopVibration() // Stop any existing vibration
        val pattern = longArrayOf(0, 500, 500) // Vibrate 500ms, Pause 500ms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 means repeat
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun stopSound() {
        try {
            stopVibration()
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null

            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            focusRequest = null
        } catch (e: Exception) {
            Log.e("RoutiniService", "Error stopping sound or abandoning focus", e)
        }
    }

    // ... [Rest of your code (onDestroy, createNotificationChannel, createNotification, Companion) remains the same]

    override fun onDestroy() {
        super.onDestroy()
        stopSound()
    }

    private fun createNotificationChannel() {
        val name = "Routini Alarms"
        val descriptionText = "Shows notifications for active alarms"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500)
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(title: String, text: String): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createAlarmNotification(title: String, taskId: Int, taskType: String?): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TITLE", title)
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(AlarmReceiver.EXTRA_TASK_TYPE, taskType)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            taskId * 1000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_MARK_DONE
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(AlarmReceiver.EXTRA_TASK_TYPE, taskType)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            this,
            taskId * 100,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_STOP
            putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            taskId,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Alarm is ringing!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500))
            .setDeleteIntent(stopPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Mark as Done", donePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    companion object {
        const val ACTION_PLAY = "com.dxtr.routini.service.PLAY"
        const val ACTION_STOP = "com.dxtr.routini.service.STOP"
        const val EXTRA_SOUND_URI = "EXTRA_SOUND_URI"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TYPE = "EXTRA_TASK_TYPE"

        private const val CHANNEL_ID = "RoutiniAlarmChannel"
        private const val NOTIFICATION_ID = 1
    }
}
