package com.dxtr.routini

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dxtr.routini.data.AppDatabase
import com.dxtr.routini.data.Routine
import com.dxtr.routini.data.RoutineTask
import com.dxtr.routini.data.StandaloneTask
import com.dxtr.routini.utils.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val routineDao = AppDatabase.getDatabase(application).routineDao()
    private val standaloneTaskDao = AppDatabase.getDatabase(application).standaloneTaskDao()

    val routines: StateFlow<List<Routine>> = routineDao.getAllRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val standaloneTasks: StateFlow<List<StandaloneTask>> = standaloneTaskDao.getAllStandaloneTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Routines
    fun addRoutine(routine: Routine) = viewModelScope.launch {
        routineDao.insertRoutine(routine)
    }

    fun updateRoutine(routine: Routine) = viewModelScope.launch {
        routineDao.updateRoutine(routine)
    }

    fun deleteRoutine(routine: Routine) = viewModelScope.launch {
        routineDao.deleteRoutine(routine)
    }

    fun getTasksForRoutine(routineId: Int): Flow<List<RoutineTask>> {
        return routineDao.getTasksForRoutine(routineId)
    }

    // Routine Tasks
    fun addRoutineTask(task: RoutineTask) = viewModelScope.launch {
        val id = routineDao.insertRoutineTask(task)
        if (task.time != null) {
            val routine = routineDao.getRoutineById(task.routineId)
            val newTask = task.copy(id = id.toInt())
            AlarmScheduler.scheduleRoutineTaskAlarm(getApplication(), newTask, routine?.recurringDays)
        }
    }

    fun updateRoutineTask(task: RoutineTask) = viewModelScope.launch {
        routineDao.updateRoutineTask(task)
        
        if (task.time != null) {
             // Logic fix: Always reschedule the alarm when updated, regardless of isDone status.
             val routine = routineDao.getRoutineById(task.routineId)
             AlarmScheduler.scheduleRoutineTaskAlarm(getApplication(), task, routine?.recurringDays)
        } else {
            // Only cancel if time was removed completely
            AlarmScheduler.cancelRoutineTaskAlarm(getApplication(), task)
        }
    }

    fun deleteRoutineTask(task: RoutineTask) = viewModelScope.launch {
        routineDao.deleteRoutineTask(task)
        AlarmScheduler.cancelRoutineTaskAlarm(getApplication(), task)
    }

    // Standalone Tasks
    fun addStandaloneTask(task: StandaloneTask) = viewModelScope.launch {
        val id = standaloneTaskDao.insertStandaloneTask(task)
        if (task.time != null) {
            val newTask = task.copy(id = id.toInt())
            AlarmScheduler.scheduleStandaloneTaskAlarm(getApplication(), newTask)
        }
    }

    fun updateStandaloneTask(task: StandaloneTask) = viewModelScope.launch {
        standaloneTaskDao.update(task)
        if (task.time != null) {
            if (task.isDone) {
                AlarmScheduler.cancelStandaloneTaskAlarm(getApplication(), task)
            } else {
                AlarmScheduler.scheduleStandaloneTaskAlarm(getApplication(), task)
            }
        } else {
            AlarmScheduler.cancelStandaloneTaskAlarm(getApplication(), task)
        }
    }

    fun deleteStandaloneTask(task: StandaloneTask) = viewModelScope.launch {
        standaloneTaskDao.deleteStandaloneTask(task)
        AlarmScheduler.cancelStandaloneTaskAlarm(getApplication(), task)
    }
}
