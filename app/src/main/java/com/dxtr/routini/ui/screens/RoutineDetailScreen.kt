package com.dxtr.routini.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dxtr.routini.MainViewModel
import com.dxtr.routini.R
import com.dxtr.routini.data.Routine
import com.dxtr.routini.data.RoutineTask
import com.dxtr.routini.ui.composables.QuickTaskItem
import com.dxtr.routini.ui.composables.TaskDialog
import com.dxtr.routini.ui.theme.AppIcons
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    routineId: Int,
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val routines by viewModel.routines.collectAsState()
    val routine = routines.find { it.id == routineId }
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allTasks by viewModel.getTasksForRoutine(routineId).collectAsState(initial = emptyList())
    val isSaving by viewModel.isSavingTask.collectAsState()

    if (routine == null) {
        return
    }

    var selectedTab by remember { mutableStateOf(0) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<RoutineTask?>(null) }
    var showRoutineDialog by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val isTodayInActiveDays = routine.recurringDays.contains(today.dayOfWeek)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routine.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painter = painterResource(id = AppIcons.ArrowBack), contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showRoutineDialog = true }) {
                        Icon(painter = painterResource(id = AppIcons.Edit), contentDescription = "Edit Routine")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                taskToEdit = null
                showTaskDialog = true
            }) {
                Icon(painter = painterResource(id = AppIcons.Add), contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Today") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("All Tasks") })
            }

            when (selectedTab) {
                0 -> {
                    if (isTodayInActiveDays) {
                        val tasksForToday = allTasks.filter { it.specificDays.isNullOrEmpty() || it.specificDays.contains(today.dayOfWeek) }
                        if (tasksForToday.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No tasks for today", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(tasksForToday, key = { it.id }) { task ->
                                    QuickTaskItem(
                                        task = task,
                                        onToggle = { viewModel.updateTaskStatus(task, !task.isDone, selectedDate) },
                                        onEdit = {
                                            taskToEdit = task
                                            showTaskDialog = true
                                        },
                                        onDelete = { viewModel.deleteRoutineTask(task) }
                                    )
                                }
                            }
                        }
                    } else {
                        RestDayCard(routine = routine) {
                            val daysToAdd = it.value - today.dayOfWeek.value
                            val finalDaysToAdd = if (daysToAdd < 0) daysToAdd + 7 else daysToAdd
                            viewModel.onDateSelected(today.plusDays(finalDaysToAdd.toLong()))
                        }
                    }
                }
                1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allTasks, key = { "all_${it.id}" }) { task ->
                            QuickTaskItem(
                                task = task,
                                onToggle = { viewModel.updateTaskStatus(task, !task.isDone, selectedDate) },
                                onEdit = {
                                    taskToEdit = task
                                    showTaskDialog = true
                                },
                                onDelete = { viewModel.deleteRoutineTask(task) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTaskDialog) {
        val currentTask = taskToEdit
        val dialogTitle = if (currentTask == null) stringResource(R.string.new_task_title) else stringResource(R.string.edit_task_title)

        TaskDialog(
            dialogTitle = dialogTitle,
            initialTitle = currentTask?.title ?: "",
            initialDescription = currentTask?.description,
            initialTime = currentTask?.time,
            initialSoundUri = currentTask?.customSoundUri,
            initialPlaySound = currentTask?.shouldPlaySound ?: false,
            initialShouldVibrate = currentTask?.shouldVibrate ?: false,
            initialSpecificDays = currentTask?.specificDays ?: emptyList(),
            availableDays = routine.recurringDays,
            isSaving = isSaving,
            onDismiss = { showTaskDialog = false },
            onConfirm = { title, desc, time, sound, playSound, shouldVibrate, specificDays ->
                val taskData = currentTask?.copy(
                    title = title,
                    description = desc,
                    time = time,
                    customSoundUri = sound,
                    shouldPlaySound = playSound,
                    shouldVibrate = shouldVibrate,
                    specificDays = specificDays
                ) ?: RoutineTask(
                    routineId = routine.id,
                    title = title,
                    description = desc,
                    time = time,
                    customSoundUri = sound,
                    shouldPlaySound = playSound,
                    shouldVibrate = shouldVibrate,
                    specificDays = specificDays
                )

                if (currentTask == null) {
                    viewModel.addRoutineTask(taskData)
                } else {
                    viewModel.updateRoutineTask(taskData)
                }
                showTaskDialog = false
            },
            onDelete = if (currentTask != null) {
                {
                    viewModel.deleteRoutineTask(currentTask)
                    showTaskDialog = false
                }
            } else null
        )
    }

    if (showRoutineDialog) {
        RoutineDialog(
            routine = routine,
            onConfirm = { name, days ->
                viewModel.updateRoutine(routine.copy(name = name, recurringDays = days))
                showRoutineDialog = false
            },
            onDelete = {
                viewModel.deleteRoutine(routine)
                showRoutineDialog = false
                navController.popBackStack()
            },
            onDismiss = { showRoutineDialog = false }
        )
    }
}

@Composable
fun RestDayCard(routine: Routine, onStartEarly: (DayOfWeek) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Rest Day", style = MaterialTheme.typography.titleLarge)
            Text("No tasks scheduled for today.", style = MaterialTheme.typography.bodyMedium)
            val nextRunDay = routine.recurringDays.firstOrNull { it > LocalDate.now().dayOfWeek } ?: routine.recurringDays.firstOrNull()
            if (nextRunDay != null) {
                Text("Next session: ${nextRunDay.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { onStartEarly(nextRunDay) }) {
                    Text("Start ${nextRunDay.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}'s Routine Now")
                }
            }
        }
    }
}