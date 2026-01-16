package com.dxtr.routini.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dxtr.routini.data.RoutineTask
import com.dxtr.routini.data.StandaloneTask
import com.dxtr.routini.receiver.AlarmReceiver
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object AlarmScheduler {

    private const val STANDALONE_TASK_OFFSET = 1000000

    fun scheduleRoutineTaskAlarm(context: Context, task: RoutineTask, recurringDays: List<DayOfWeek>?) {
        val time = task.time ?: return

        val targetDays = if (!task.specificDays.isNullOrEmpty()) {
            task.specificDays
        } else {
            recurringDays
        }

        val alarmTime = calculateNextAlarmTime(time, targetDays, task.isDone) ?: return

        scheduleAlarm(context, task.id, task.title, task.customSoundUri, task.shouldPlaySound, alarmTime, "ROUTINE")
    }

    fun scheduleStandaloneTaskAlarm(context: Context, task: StandaloneTask) {
        val time = task.time ?: return

        val triggerAtMillis: Long = if (task.date != null) {
            val taskDateTime = LocalDateTime.of(task.date, time)
            if (taskDateTime.isBefore(LocalDateTime.now())) return
            taskDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else {
            calculateNextAlarmTime(time, null, false) ?: return
        }

        scheduleAlarm(context, task.id + STANDALONE_TASK_OFFSET, task.title, task.customSoundUri, task.shouldPlaySound, triggerAtMillis, "STANDALONE")
    }

    fun cancelRoutineTaskAlarm(context: Context, task: RoutineTask) { cancelAlarm(context, task.id) }
    fun cancelStandaloneTaskAlarm(context: Context, task: StandaloneTask) { cancelAlarm(context, task.id + STANDALONE_TASK_OFFSET) }

    private fun scheduleAlarm(context: Context, taskId: Int, title: String, soundUri: String?, playSound: Boolean, triggerAtMillis: Long, taskType: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TRIGGER
            putExtra("TITLE", title); putExtra("SOUND_URI", soundUri); putExtra("PLAY_SOUND", playSound); putExtra("TASK_ID", taskId); putExtra("TASK_TYPE", taskType)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, taskId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    private fun cancelAlarm(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, taskId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    private fun calculateNextAlarmTime(time: LocalTime, recurringDays: List<DayOfWeek>?, isDoneForToday: Boolean): Long? {
        val now = LocalDateTime.now()
        var nextAlarmDateTime = now.with(time)

        if (nextAlarmDateTime.isBefore(now) || isDoneForToday) {
            nextAlarmDateTime = nextAlarmDateTime.plusDays(1)
        }

        if (recurringDays.isNullOrEmpty()) {
            return nextAlarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        for (i in 0 until 60) {
            val currentDayOfWeek = nextAlarmDateTime.dayOfWeek
            if (recurringDays.contains(currentDayOfWeek)) {
                return nextAlarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            nextAlarmDateTime = nextAlarmDateTime.plusDays(1)
        }

        return null
    }
}