package com.dxtr.routini.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dxtr.routini.data.AppDatabase
import com.dxtr.routini.utils.AlarmScheduler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val database = AppDatabase.getDatabase(context)
            val routineDao = database.routineDao()
            val standaloneTaskDao = database.standaloneTaskDao()

            GlobalScope.launch {
                val allRoutineTasks = routineDao.getAllRoutineTasks()
                allRoutineTasks.forEach { task ->
                    // Assuming you have a way to get the recurring days for a routine task
                    // For now, passing null, which means it will be treated as a daily alarm
                    AlarmScheduler.scheduleRoutineTaskAlarm(context, task, null)
                }

                val allStandaloneTasks = standaloneTaskDao.getAllStandaloneTasksSync()
                allStandaloneTasks.forEach { task ->
                    AlarmScheduler.scheduleStandaloneTaskAlarm(context, task)
                }
            }
        }
    }
}
