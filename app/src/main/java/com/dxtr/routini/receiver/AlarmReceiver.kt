package com.dxtr.routini.receiver

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dxtr.routini.R
import com.dxtr.routini.data.AppDatabase
import com.dxtr.routini.service.RoutiniService
import com.dxtr.routini.ui.AlarmActivity
import com.dxtr.routini.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STOP = "STOP_ALARM"
        const val ACTION_MARK_DONE = "MARK_AS_DONE"
        const val ACTION_TRIGGER = "TRIGGER_ALARM"
        
        // keys for extras
        const val EXTRA_TASK_ID = "TASK_ID"
        const val EXTRA_TASK_TYPE = "TASK_TYPE" // "ROUTINE" or "STANDALONE"
        const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val taskType = intent.getStringExtra(EXTRA_TASK_TYPE)

        when (intent.action) {
            ACTION_STOP -> {
                val serviceIntent = Intent(context, RoutiniService::class.java).apply {
                    this.action = RoutiniService.ACTION_STOP
                }
                context.startService(serviceIntent)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (taskId != -1) notificationManager.cancel(taskId)
                return
            }

            ACTION_MARK_DONE -> {
                val serviceIntent = Intent(context, RoutiniService::class.java).apply {
                    this.action = RoutiniService.ACTION_STOP
                }
                context.startService(serviceIntent)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (taskId != -1) notificationManager.cancel(taskId)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        if (taskId != -1) {
                            when (taskType) {
                                "ROUTINE" -> {
                                    val task = db.routineDao().getTaskById(taskId)
                                    task?.let {
                                        db.routineDao().updateRoutineTask(it.copy(isDone = true))
                                        val routine = db.routineDao().getRoutineById(it.routineId)
                                        AlarmScheduler.scheduleRoutineTaskAlarm(context, it.copy(isDone = true), routine?.recurringDays)
                                    }
                                }
                                "STANDALONE" -> {
                                     val standaloneTaskId = taskId - 1000000 // Subtract offset
                                     val task = db.standaloneTaskDao().getTaskById(standaloneTaskId)
                                     task?.let {
                                         db.standaloneTaskDao().update(it.copy(isDone = true))
                                         AlarmScheduler.scheduleStandaloneTaskAlarm(context, it.copy(isDone = true))
                                     }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating task in background", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
                return
            }

            ACTION_TRIGGER, null -> {
                // Normal Alarm Trigger
                val soundUriString = intent.getStringExtra("SOUND_URI")
                val title = intent.getStringExtra("TITLE") ?: "Routini Alarm"
                val playSound = intent.getBooleanExtra("PLAY_SOUND", false)

                showNotification(context, title, playSound, taskId, taskType)

                if (playSound) {
                    val serviceIntent = Intent(context, RoutiniService::class.java).apply {
                        this.action = RoutiniService.ACTION_PLAY
                        putExtra(RoutiniService.EXTRA_SOUND_URI, soundUriString)
                        putExtra(RoutiniService.EXTRA_TITLE, title)
                    }
                    context.startForegroundService(serviceIntent)
                }
            }
        }
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun showNotification(
        context: Context, 
        title: String, 
        playSound: Boolean, 
        taskId: Int,
        taskType: String?
    ) {
        val channelId = "RoutiniAlarmChannel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "Routini Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Notifications for scheduled tasks"
            enableVibration(true)
            setBypassDnd(true)
            if (playSound) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(null, audioAttributes)
            }
        }
        notificationManager.createNotificationChannel(channel)
        
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TITLE", title)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TYPE, taskType)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            taskId * 1000, // Unique Request Code
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, AlarmActivity::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TYPE, taskType)
        }
        val donePendingIntent = PendingIntent.getActivity(
            context,
            taskId * 100, 
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopPendingIntent = if (playSound) {
            val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_TASK_ID, taskId)
            }
            PendingIntent.getBroadcast(
                context,
                taskId,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true) 
            .addAction(R.drawable.ic_launcher_foreground, "Mark as Done", donePendingIntent)
            
        if (playSound && stopPendingIntent != null) {
            builder
                .setContentText("Alarm is ringing!")
                .setDeleteIntent(stopPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .setOngoing(true)
        } else {
            builder.setContentText("Task due now.")
                .setAutoCancel(true)
        }
        
        if (taskId != -1) {
            notificationManager.notify(taskId, builder.build())
        }
    }
}
