package com.dxtr.routini.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dxtr.routini.data.AppDatabase
import com.dxtr.routini.utils.AlarmScheduler
import kotlinx.coroutines.flow.firstOrNull
import java.time.DayOfWeek
import java.time.LocalDate

class MidnightResetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val routineDao = database.routineDao()

        val routinesWithTasks = routineDao.getRoutinesWithTasks().firstOrNull() ?: emptyList()
        val today = LocalDate.now()
        val todayDayOfWeek = try { DayOfWeek.valueOf(today.dayOfWeek.name) } catch (e: Exception) { null }

        routinesWithTasks.forEach { routineWithTasks ->
            val routine = routineWithTasks.routine
            val tasks = routineWithTasks.tasks
            val routineDays = routine.recurringDays

            tasks.forEach { task ->
                // LOGIC FIX: Determine effective days
                val effectiveDays = if (!task.specificDays.isNullOrEmpty()) task.specificDays else routineDays

                if (task.isDone) {
                    val isTodayScheduled = effectiveDays.contains(todayDayOfWeek)
                    val isDaily = effectiveDays.isEmpty()
                    
                    if (isTodayScheduled || isDaily) {
                        routineDao.resetTask(task.id)
                    }
                }

                if (task.time != null) {
                    // Pass the effective days to scheduler
                    AlarmScheduler.scheduleRoutineTaskAlarm(applicationContext, task, effectiveDays)
                }
            }
        }
        return Result.success()
    }
}