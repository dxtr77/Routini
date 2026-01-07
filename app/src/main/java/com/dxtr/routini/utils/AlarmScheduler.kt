package com.dxtr.routini.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dxtr.routini.data.DayOfWeek
import com.dxtr.routini.data.RoutineTask
import com.dxtr.routini.data.StandaloneTask
import com.dxtr.routini.receiver.AlarmReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object AlarmScheduler {

    // Offset to prevent ID collisions between routine tasks and standalone tasks
    private const val STANDALONE_TASK_OFFSET = 1000000

    fun scheduleRoutineTaskAlarm(context: Context, task: RoutineTask, recurringDays: List<DayOfWeek>?) {
        val time = task.time ?: return
        
        // Don't schedule if sound/alarm is not requested, unless we want a silent notification
        // But for now, we schedule everything that has a time.
        // We will pass the 'shouldPlaySound' flag.

        val alarmTime = calculateNextAlarmTime(time, recurringDays) ?: return

        scheduleAlarm(
            context,
            taskId = task.id,
            title = task.title,
            soundUri = task.customSoundUri,
            playSound = task.shouldPlaySound,
            triggerAtMillis = alarmTime
        )
    }

    fun scheduleStandaloneTaskAlarm(context: Context, task: StandaloneTask) {
        val time = task.time ?: return
        
        val triggerAtMillis: Long = if (task.date != null) {
            // Case 1: Specific Date and Time
            val taskDateTime = LocalDateTime.of(task.date, time)
            val now = LocalDateTime.now()
            
            // If the date is in the past, don't schedule
            if (taskDateTime.isBefore(now)) return 
            
            taskDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else {
            // Case 2: Time only (implying "Next occurrence" logic)
            calculateNextAlarmTime(time, null) ?: return
        }

        scheduleAlarm(
            context,
            taskId = task.id + STANDALONE_TASK_OFFSET,
            title = task.title,
            soundUri = task.customSoundUri,
            playSound = task.shouldPlaySound, // Use the task's property!
            triggerAtMillis = triggerAtMillis
        )
    }

    fun cancelRoutineTaskAlarm(context: Context, task: RoutineTask) {
        cancelAlarm(context, task.id)
    }

    fun cancelStandaloneTaskAlarm(context: Context, task: StandaloneTask) {
        cancelAlarm(context, task.id + STANDALONE_TASK_OFFSET)
    }

    private fun scheduleAlarm(
        context: Context,
        taskId: Int,
        title: String,
        soundUri: String?,
        playSound: Boolean,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("TITLE", title)
            putExtra("SOUND_URI", soundUri)
            putExtra("PLAY_SOUND", playSound)
            putExtra("TASK_ID", taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun calculateNextAlarmTime(time: LocalTime, recurringDays: List<DayOfWeek>?): Long? {
        val now = LocalDateTime.now()
        var nextAlarm = now.with(time)

        // If time has passed for today, start checking from tomorrow
        if (nextAlarm.isBefore(now)) {
            nextAlarm = nextAlarm.plusDays(1)
        }

        if (recurringDays.isNullOrEmpty()) {
            // If no specific days, it's just daily (or once next occurrence)
            return nextAlarm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        // Find the next matching day
        for (i in 0..7) {
            val checkDay = nextAlarm.dayOfWeek // java.time.DayOfWeek
            // Convert to our DayOfWeek
            val ourDay = try {
                DayOfWeek.valueOf(checkDay.name)
            } catch (e: Exception) {
                null
            }

            if (ourDay != null && recurringDays.contains(ourDay)) {
                return nextAlarm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            nextAlarm = nextAlarm.plusDays(1)
        }
        
        return null // Should not happen if list is not empty
    }
}