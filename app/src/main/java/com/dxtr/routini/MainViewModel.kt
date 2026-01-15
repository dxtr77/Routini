package com.dxtr.routini

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dxtr.routini.data.AppDatabase
import com.dxtr.routini.data.Routine
import com.dxtr.routini.data.RoutineHistory
import com.dxtr.routini.data.RoutineTask
import com.dxtr.routini.data.StandaloneTask
import com.dxtr.routini.data.Task
import com.dxtr.routini.utils.AlarmScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val routineDao = AppDatabase.getDatabase(application).routineDao()
    private val standaloneTaskDao = AppDatabase.getDatabase(application).standaloneTaskDao()
    private val routineHistoryDao = AppDatabase.getDatabase(application).routineHistoryDao()

    private val _isSavingTask = MutableStateFlow(false)
    val isSavingTask: StateFlow<Boolean> = _isSavingTask.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val routines: StateFlow<List<Routine>> = routineDao.getAllRoutines()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val allStandaloneTasks: Flow<List<StandaloneTask>> = standaloneTaskDao.getAllStandaloneTasks()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = selectedDate.flatMapLatest { date ->
        val historyFlow = routineHistoryDao.getHistoryForDate(date)
        val standaloneTasksFlow = standaloneTaskDao.getStandaloneTasksForDate(date)
        val routinesForDayFlow = routineDao.getRoutinesForDay(date.dayOfWeek.name)

        routinesForDayFlow.flatMapLatest { routines ->
            val routineTaskFlows = routines.map { getTasksForRoutine(it.id) }
            val combinedRoutineTasksFlow = if (routineTaskFlows.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(routineTaskFlows) { tasksArray ->
                    tasksArray.toList().flatten().filter { task ->
                        task.specificDays.isNullOrEmpty() || task.specificDays.contains(date.dayOfWeek)
                    }
                }
            }

            combine(combinedRoutineTasksFlow, standaloneTasksFlow, historyFlow) { routineTasks, standaloneTasks, history ->
                val allTasks = mutableListOf<Task>()
                val today = LocalDate.now()

                allTasks.addAll(routineTasks.map { task ->
                    val isDone = if (date == today) task.isDone else history.any { it.taskId == task.id }
                    task.copy(isDone = isDone)
                })

                allTasks.addAll(standaloneTasks.map { task ->
                    val isDone = if (date == today) task.isDone else history.any { it.taskId == task.id }
                    task.copy(isDone = isDone)
                })

                allTasks.sortedWith(compareBy(nullsLast()) { it.time })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTasks: StateFlow<List<StandaloneTask>> = allStandaloneTasks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    fun onNextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun onPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

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
        _isSavingTask.value = true
        val id = routineDao.insertRoutineTask(task)
        if (task.time != null) {
            val routine = routineDao.getRoutineById(task.routineId)
            val newTask = task.copy(id = id.toInt())
            AlarmScheduler.scheduleRoutineTaskAlarm(getApplication(), newTask, routine?.recurringDays)
        }
        _isSavingTask.value = false
    }

    fun updateRoutineTask(task: RoutineTask) = viewModelScope.launch {
        _isSavingTask.value = true
        routineDao.updateRoutineTask(task)

        if (task.time != null) {
            val routine = routineDao.getRoutineById(task.routineId)
            AlarmScheduler.scheduleRoutineTaskAlarm(getApplication(), task, routine?.recurringDays)
        } else {
            AlarmScheduler.cancelRoutineTaskAlarm(getApplication(), task)
        }
        _isSavingTask.value = false
    }

    fun deleteRoutineTask(task: RoutineTask) = viewModelScope.launch {
        routineDao.deleteRoutineTask(task)
        AlarmScheduler.cancelRoutineTaskAlarm(getApplication(), task)
    }

    // Standalone Tasks
    fun addStandaloneTask(task: StandaloneTask) = viewModelScope.launch {
        _isSavingTask.value = true
        val id = standaloneTaskDao.insertStandaloneTask(task)
        if (task.time != null) {
            val newTask = task.copy(id = id.toInt())
            AlarmScheduler.scheduleStandaloneTaskAlarm(getApplication(), newTask)
        }
        _isSavingTask.value = false
    }

    fun updateStandaloneTask(task: StandaloneTask) = viewModelScope.launch {
        _isSavingTask.value = true
        standaloneTaskDao.update(task)
        if (task.time != null) {
            AlarmScheduler.scheduleStandaloneTaskAlarm(getApplication(), task)
        } else {
            AlarmScheduler.cancelStandaloneTaskAlarm(getApplication(), task)
        }
        _isSavingTask.value = false
    }

    fun deleteStandaloneTask(task: StandaloneTask) = viewModelScope.launch {
        standaloneTaskDao.deleteStandaloneTask(task)
        AlarmScheduler.cancelStandaloneTaskAlarm(getApplication(), task)
    }

    fun updateTaskStatus(task: Task, isDone: Boolean, date: LocalDate?) {
        viewModelScope.launch {
            when (task) {
                is RoutineTask -> {
                    if (date == null) return@launch // Should not happen for a routine task
                    val today = LocalDate.now()
                    if (date == today) {
                        updateRoutineTask(task.copy(isDone = isDone))
                    }
                    if (isDone) {
                        routineHistoryDao.insert(RoutineHistory(taskId = task.id, completionDate = date))
                    } else {
                        routineHistoryDao.delete(task.id, date)
                    }
                }
                is StandaloneTask -> {
                    updateStandaloneTask(task.copy(isDone = isDone))
                    if (date != null) {
                        if (isDone) {
                            routineHistoryDao.insert(RoutineHistory(taskId = task.id, completionDate = date))
                        } else {
                            routineHistoryDao.delete(task.id, date)
                        }
                    }
                }
            }
        }
    }
}
