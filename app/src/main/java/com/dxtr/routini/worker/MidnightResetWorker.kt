package com.dxtr.routini.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dxtr.routini.data.AppDatabase
import com.dxtr.routini.data.DayOfWeek
import com.dxtr.routini.utils.AlarmScheduler
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate

class MidnightResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val routineDao = database.routineDao()

        val routinesWithTasks = routineDao.getRoutinesWithTasks().firstOrNull() ?: emptyList()
        val today = LocalDate.now()
        val todayDayOfWeek = try {
            DayOfWeek.valueOf(today.dayOfWeek.name)
        } catch (e: Exception) {
            null
        }

        routinesWithTasks.forEach { routineWithTasks ->
            val routine = routineWithTasks.routine
            val tasks = routineWithTasks.tasks
            val recurringDays = routine.recurringDays

            tasks.forEach { task ->
                if (task.isDone) {
                    val isTodayScheduled = recurringDays.contains(todayDayOfWeek)
                    val isDaily = recurringDays.isEmpty()
                    
                    if (isTodayScheduled || isDaily) {
                        routineDao.resetTask(task.id)
                    }
                }

                if (task.time != null) {
                    AlarmScheduler.scheduleRoutineTaskAlarm(applicationContext, task, recurringDays)
                }
            }
        }

        return Result.success()
    }
}