package com.dxtr.routini.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.dxtr.routini.data.RoutineTask
import com.dxtr.routini.ui.composables.QuickTaskItem
import com.dxtr.routini.ui.composables.TaskDialog
import com.dxtr.routini.ui.theme.AppIcons
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    routineId: Int,
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    // 1. Fetch Routine & Tasks
    val routines by viewModel.routines.collectAsState()
    val routine = routines.find { it.id == routineId }
    val tasks by viewModel.getTasksForRoutine(routineId).collectAsState(initial = emptyList())

    // Handle loading or deleted routine
    if (routine == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // 2. Setup Tabs (Only show days the routine is active on)
    val activeDays = routine.recurringDays.sorted() // Ensure order (Mon -> Sun)
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    
    // Safety check if activeDays is empty (edge case)
    val currentDay = if (activeDays.isNotEmpty()) activeDays.getOrElse(selectedDayIndex) { activeDays[0] } else null

    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<RoutineTask?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routine.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(painter = painterResource(id = AppIcons.ArrowBack), contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                taskToEdit = null // Ensure we are adding a new task
                showTaskDialog = true 
            }) {
                Icon(painter = painterResource(id = AppIcons.Add), contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            
            // 3. The Tab Row
            if (activeDays.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedDayIndex,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedDayIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    activeDays.forEachIndexed { index, day ->
                        Tab(
                            selected = selectedDayIndex == index,
                            onClick = { selectedDayIndex = index },
                            text = { Text(day.name.take(3)) } // "MON", "TUE"
                        )
                    }
                }
            }

            // 4. Filter Tasks for the selected Tab
            val filteredTasks = tasks.filter { task ->
                // Show task if it is assigned SPECIFICALLY to this day
                task.specificDays?.contains(currentDay) == true
            }

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tasks for this day", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        // Re-use your existing QuickTaskItem
                        QuickTaskItem(
                            task = task,
                            onToggle = { viewModel.updateRoutineTask(task.copy(isDone = !task.isDone)) },
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

    // 5. Add/Edit Dialog
    if (showTaskDialog) {
        val currentTask = taskToEdit
        val dialogTitle = if (currentTask == null) stringResource(R.string.new_task_title) else stringResource(R.string.edit_task_title)
        val initialSpecificDays = if (currentTask == null) {
            if (currentDay != null) listOf(currentDay) else emptyList()
        } else {
            currentTask.specificDays ?: emptyList()
        }

        TaskDialog(
            dialogTitle = dialogTitle,
            initialTitle = currentTask?.title ?: "",
            initialDescription = currentTask?.description ?: "",
            initialTime = currentTask?.time?.toString() ?: "",
            initialSoundUri = currentTask?.customSoundUri ?: "",
            initialPlaySound = currentTask?.shouldPlaySound ?: true,
            initialSpecificDays = initialSpecificDays,
            availableDays = routine.recurringDays,
            onDismiss = { showTaskDialog = false },
            onConfirm = { title, desc, time, sound, playSound, specificDays ->
                if (specificDays.isNotEmpty()) { // Ensure at least one day is selected
                    val taskData = currentTask?.copy(
                        title = title,
                        description = desc,
                        time = if (time.isNotBlank()) LocalTime.parse(time) else null,
                        customSoundUri = sound,
                        shouldPlaySound = playSound,
                        specificDays = specificDays
                    ) ?: RoutineTask(
                        routineId = routine.id,
                        title = title,
                        description = desc,
                        time = if (time.isNotBlank()) LocalTime.parse(time) else null,
                        customSoundUri = sound,
                        shouldPlaySound = playSound,
                        specificDays = specificDays
                    )

                    if (currentTask == null) {
                        viewModel.addRoutineTask(taskData)
                    } else {
                        viewModel.updateRoutineTask(taskData)
                    }
                    showTaskDialog = false
                }
            },
            onDelete = if (currentTask != null) {
                { 
                    viewModel.deleteRoutineTask(currentTask)
                    showTaskDialog = false 
                }
            } else null
        )
    }
}
