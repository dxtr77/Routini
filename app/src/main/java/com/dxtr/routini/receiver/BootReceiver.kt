package com.dxtr.routini.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dxtr.routini.data.AppDatabase
import com.dxtr.routini.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val routineDao = db.routineDao()
                    val standaloneTaskDao = db.standaloneTaskDao()

                    // Reschedule Routine Tasks
                    val routines = routineDao.getAllRoutinesSuspend()
                    routines.forEach { routine ->
                        val tasks = routineDao.getTasksForRoutineSuspend(routine.id)
                        tasks.forEach { task ->
                            if (task.time != null) {
                                AlarmScheduler.scheduleRoutineTaskAlarm(context, task, routine.recurringDays)
                            }
                        }
                    }

                    // Reschedule Standalone Tasks
                    val standaloneTasks = standaloneTaskDao.getAllStandaloneTasksSuspend()
                    standaloneTasks.forEach { task ->
                        if (task.time != null && !task.isDone) {
                            AlarmScheduler.scheduleStandaloneTaskAlarm(context, task)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
