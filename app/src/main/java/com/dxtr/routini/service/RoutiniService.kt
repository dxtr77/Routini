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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dxtr.routini.MainActivity
import com.dxtr.routini.R

class RoutiniService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()

        // FIX: Add foregroundServiceType for Android 14+ compliance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                createNotification("Routini is Active", "Monitoring your routines and tasks"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification("Routini is Active", "Monitoring your routines and tasks"))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_PLAY) {
            val soundUriString = intent.getStringExtra(EXTRA_SOUND_URI)
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "Alarm"
            
            playSound(soundUriString)
            
            val notification = createNotification("Alarm: $title", "Playing alarm sound. Tap to open.")
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
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
        val focusType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
        } else {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        }

        val focusResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            audioManager?.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(null, AudioManager.STREAM_ALARM, focusType)
        }

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
                    soundUri = Uri.parse(soundUriString)
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

    private fun stopSound() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Routini Alarms"
            val descriptionText = "Shows notifications for active alarms"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
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

    companion object {
        const val ACTION_PLAY = "com.dxtr.routini.service.PLAY"
        const val ACTION_STOP = "com.dxtr.routini.service.STOP"
        const val EXTRA_SOUND_URI = "EXTRA_SOUND_URI"
        const val EXTRA_TITLE = "EXTRA_TITLE"

        private const val CHANNEL_ID = "RoutiniAlarmChannel"
        private const val NOTIFICATION_ID = 1
    }
}
