package com.dxtr.routini.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dxtr.routini.data.AppDatabase
import com.dxtr.routini.utils.AlarmScheduler
import kotlinx.coroutines.flow.first

class ResetTasksWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val routineDao = db.routineDao()
            val standaloneTaskDao = db.standaloneTaskDao()

            routineDao.resetAllTasks()
            // Standalone tasks are one-off, so we do not reset them.

            val routines = routineDao.getAllRoutines().first()
            routines.forEach { routine ->
                val tasks = routineDao.getTasksForRoutine(routine.id).first()
                tasks.forEach { task ->
                    if (task.time != null) {
                        // Reschedule alarm for the new day
                        AlarmScheduler.scheduleRoutineTaskAlarm(appContext, task, routine.recurringDays)
                    }
                }
            }
            
            // Standalone tasks are not rescheduled as they are date-specific or manual one-offs.


            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
