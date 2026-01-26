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
import com.dxtr.routini.widget.TodayTasksWidgetProvider
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
        const val EXTRA_IS_MISSED = "IS_MISSED"
        const val TAG = "AlarmReceiver"

        fun showNotification(
            context: Context, 
            title: String, 
            playSound: Boolean, 
            taskId: Int,
            taskType: String?,
            vibrationEnabled: Boolean,
            isMissed: Boolean = false
        ) {
            val receiver = AlarmReceiver()
            receiver.showNotificationInternal(context, title, playSound, taskId, taskType, vibrationEnabled, isMissed)
        }
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
                            // Insert into History
                            db.routineHistoryDao().insert(
                                com.dxtr.routini.data.RoutineHistory(
                                    taskId = taskId,
                                    taskType = taskType ?: "STANDALONE",
                                    completionDate = java.time.LocalDate.now()
                                )
                            )

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
                            // Refresh Widget to reflect completed state
                            TodayTasksWidgetProvider.refreshWidget(context)
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
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val isDone = when (taskType) {
                            "ROUTINE" -> {
                                db.routineHistoryDao().getHistoryForTaskOnDate(taskId, "ROUTINE", java.time.LocalDate.now()) != null
                            }
                            "STANDALONE" -> {
                                val standaloneTaskId = taskId - 1000000
                                db.standaloneTaskDao().getTaskById(standaloneTaskId)?.isDone == true
                            }
                            else -> false
                        }

                        if (!isDone) {
                            val prefs = context.getSharedPreferences("routini_prefs", Context.MODE_PRIVATE)
                            val taskAlarmsEnabled = prefs.getBoolean("task_alarms", true)
                            val routineAlarmsEnabled = prefs.getBoolean("routine_alarms", true)
                            val vibrationEnabled = prefs.getBoolean("vibration", true)

                            val soundUriString = intent.getStringExtra("SOUND_URI")
                            val title = intent.getStringExtra("TITLE") ?: "Routini Alarm"
                            var playSound = intent.getBooleanExtra("PLAY_SOUND", false)

                            if (playSound) {
                                if (taskType == "ROUTINE" && !routineAlarmsEnabled) playSound = false
                                if ((taskType == "STANDALONE" || taskType == null) && !taskAlarmsEnabled) playSound = false
                            }

                            val isMissed = intent.getBooleanExtra(EXTRA_IS_MISSED, false)
                            if (playSound) {
                                val serviceIntent = Intent(context, RoutiniService::class.java).apply {
                                    this.action = RoutiniService.ACTION_PLAY
                                    putExtra(RoutiniService.EXTRA_SOUND_URI, soundUriString)
                                    putExtra(RoutiniService.EXTRA_TITLE, title)
                                    putExtra(RoutiniService.EXTRA_TASK_ID, taskId)
                                    putExtra(RoutiniService.EXTRA_TASK_TYPE, taskType)
                                    putExtra("VIBRATE", vibrationEnabled)
                                }
                                context.startForegroundService(serviceIntent)
                            } else {
                                showNotificationInternal(context, title, false, taskId, taskType, vibrationEnabled, isMissed)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking task status in trigger", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun showNotificationInternal(
        context: Context, 
        title: String, 
        playSound: Boolean, 
        taskId: Int,
        taskType: String?,
        vibrationEnabled: Boolean,
        isMissed: Boolean
    ) {
        val channelId = if (playSound) "RoutiniAlarmChannel" else "RoutiniTaskChannel"
        val channelName = if (playSound) "Routini Alarms" else "Routini Tasks"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val vbSuffix = if (vibrationEnabled) "_vib" else "_novib"
        val finalChannelId = channelId + vbSuffix

        val channel = NotificationChannel(finalChannelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
            description = if (playSound) "Active alarms" else "Task reminders"
            enableVibration(vibrationEnabled)
            if (vibrationEnabled) vibrationPattern = longArrayOf(0, 500)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
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
            taskId * 1000, 
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TYPE, taskType)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
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

        val finalTitle = when {
            isMissed -> "Missed: $title"
            !playSound -> "Reminder: $title"
            else -> title
        }

        val builder = NotificationCompat.Builder(context, finalChannelId)
            .setSmallIcon(R.mipmap.routini_icon)
            .setContentTitle(finalTitle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (playSound) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.baseline_check_24, "Mark as Done", donePendingIntent)
        
        if (vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 500))
        }

        if (playSound) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true) 
        }
            
        if (playSound && stopPendingIntent != null) {
            builder
                .setContentText("Alarm is ringing!")
                .setDeleteIntent(stopPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .setOngoing(true)
        } else {
            val contentText = if (isMissed) "Missed Alarm" else "Task Reminder"
            builder.setContentText(contentText)
                .setAutoCancel(true)
                .setOngoing(false)
        }
        
        if (taskId != -1) {
            notificationManager.notify(taskId, builder.build())
        }
    }
}
