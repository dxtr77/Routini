package com.dxtr.routini.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dxtr.routini.MainActivity
import com.dxtr.routini.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)

        if (intent.action == "STOP_ALARM") {
            stopSound()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (taskId != -1) {
                notificationManager.cancel(taskId)
            }
            return
        }

        val soundUriString = intent.getStringExtra("SOUND_URI")
        val title = intent.getStringExtra("TITLE") ?: "Routini Alarm"
        val playSound = intent.getBooleanExtra("PLAY_SOUND", false)

        Log.d("AlarmReceiver", "Alarm received: $title, Play Sound: $playSound, Task ID: $taskId")

        showNotification(context, title, playSound, taskId)

        if (playSound) {
            playSound(context, soundUriString)
        }
    }

    private fun showNotification(context: Context, title: String, playSound: Boolean, taskId: Int) {
        val channelId = "RoutiniAlarmChannel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Routini Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications for scheduled tasks"
                enableVibration(true)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        if (playSound) {
            val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "STOP_ALARM"
                putExtra("TASK_ID", taskId)
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                taskId, 
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder
                .setContentText("Alarm is ringing! Tap to stop.")
                .setContentIntent(stopPendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent) // Placeholder icon
                .setOngoing(true)
                .setAutoCancel(false) // User must explicitly stop it
        } else {
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context, taskId, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder
                .setContentText("Your scheduled task is due.")
                .setContentIntent(openAppPendingIntent)
                .setOngoing(false)
                .setAutoCancel(true) // Dismiss on tap
        }
        
        if (taskId != -1) {
            notificationManager.notify(taskId, builder.build())
        }
    }

    private fun playSound(context: Context, soundUriString: String?) {
        try {
            stopSound()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
            }
            if (!soundUriString.isNullOrEmpty()) {
                mediaPlayer?.setDataSource(context, Uri.parse(soundUriString))
            } else {
                val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                mediaPlayer?.setDataSource(context, defaultSoundUri)
            }
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopSound() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private var mediaPlayer: MediaPlayer? = null
    }
}