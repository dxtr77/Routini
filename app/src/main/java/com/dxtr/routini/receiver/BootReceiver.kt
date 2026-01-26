package com.dxtr.routini.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dxtr.routini.data.AppDatabase
import com.dxtr.routini.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val routineDao = db.routineDao()
                    val standaloneTaskDao = db.standaloneTaskDao()

                    val now = LocalDateTime.now()
                    val today = LocalDate.now()

                    // Reschedule Routine Tasks
                    val routines = routineDao.getAllRoutinesSuspend()
                    routines.forEach { routine ->
                        val tasks = routineDao.getTasksForRoutineSuspend(routine.id)
                        tasks.forEach { task ->
                            val time = task.time ?: return@forEach
                            
                            // Check if it should have fired today
                            val targetDays = task.specificDays ?: routine.recurringDays
                            val isScheduledForToday = targetDays.contains(now.dayOfWeek)
                            
                            if (isScheduledForToday) {
                                val taskDateTime = LocalDateTime.of(today, time)
                                val history = db.routineHistoryDao().getHistoryForTaskOnDate(task.id, "ROUTINE", today)
                                
                                // Notify as missed task if triggered within last 24h and not done
                                if (taskDateTime.isBefore(now) && taskDateTime.isAfter(now.minusHours(24)) && history == null) {
                                    showMissedTaskNotification(context, task.id, task.title, "ROUTINE")
                                }
                            }
                            AlarmScheduler.scheduleRoutineTaskAlarm(context, task, routine.recurringDays)
                        }
                    }

                    // Reschedule Standalone Tasks
                    val standaloneTasks = standaloneTaskDao.getAllStandaloneTasksSuspend()
                    standaloneTasks.forEach { task ->
                        val time = task.time ?: return@forEach
                        val taskDate = task.date ?: today
                        val taskDateTime = LocalDateTime.of(taskDate, time)
                        
                        if (!task.isDone) {
                            if (taskDateTime.isBefore(now) && taskDateTime.isAfter(now.minusHours(24))) {
                                showMissedTaskNotification(context, task.id, task.title, "STANDALONE")
                            }
                            AlarmScheduler.scheduleStandaloneTaskAlarm(context, task)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BootReceiver", "Error rescheduling alarms", e)
                } finally {
                    // Refresh widgets after boot to ensure they show current data
                    try {
                        com.dxtr.routini.widget.TodayTasksWidgetProvider.refreshWidget(context)
                    } catch (e: Exception) {
                        android.util.Log.e("BootReceiver", "Error refreshing widgets", e)
                    }
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showMissedTaskNotification(context: Context, taskId: Int, title: String, taskType: String) {
        AlarmReceiver.showNotification(
            context = context,
            title = title,
            playSound = false,
            taskId = taskId,
            taskType = taskType,
            vibrationEnabled = true, // Defaulting to true for missed notifications
            isMissed = true
        )
    }
}
