package com.dxtr.routini.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dxtr.routini.MainViewModel
import com.dxtr.routini.R
import com.dxtr.routini.data.RoutineTask
import com.dxtr.routini.data.StandaloneTask
import com.dxtr.routini.data.Task
import com.dxtr.routini.ui.composables.StandaloneTaskDialog
import com.dxtr.routini.ui.theme.AppIcons
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel = viewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    val routines by viewModel.routines.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isSaving by viewModel.isSavingTask.collectAsState()

    val completedTasks = tasks.count { it.isDone }
    val progress = if (tasks.isNotEmpty()) completedTasks.toFloat() / tasks.size else 0f

    var showTaskDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showTaskDialog = true }) {
                Icon(painter = painterResource(id = AppIcons.Add), contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Text(
                text = getGreeting(),
                style = MaterialTheme.typography.headlineMedium
            )
            DateNavigationBar(
                selectedDate = selectedDate,
                onDateClicked = { showDatePicker = true },
                onPreviousDay = { viewModel.onPreviousDay() },
                onNextDay = { viewModel.onNextDay() }
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                val standaloneTasks = tasks.filterIsInstance<StandaloneTask>().filter { it.date != null }
                if (standaloneTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Tasks",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(standaloneTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onTaskStatusChanged = { changedTask, isDone ->
                                viewModel.updateTaskStatus(changedTask, isDone, selectedDate)
                            },
                            onDelete = { viewModel.deleteStandaloneTask(task) }
                        )
                    }
                }

                val routineTasks = tasks.filterIsInstance<RoutineTask>()
                val groupedRoutineTasks = routineTasks.groupBy { it.routineId }

                groupedRoutineTasks.forEach { (routineId, tasksInRoutine) ->
                    val routine = routines.find { it.id == routineId }
                    if (routine != null) {
                        item {
                            Text(
                                text = routine.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(tasksInRoutine, key = { "routine_${it.id}" }) { task ->
                            TaskCard(
                                task = task,
                                onTaskStatusChanged = { changedTask, isDone ->
                                    viewModel.updateTaskStatus(changedTask, isDone, selectedDate)
                                },
                                onDelete = { viewModel.deleteRoutineTask(task) }
                            )
                        }
                    }
                }

                if (tasks.isEmpty()) {
                    item {
                        val emptyText = if (selectedDate == LocalDate.now()) {
                            "No tasks for today!"
                        } else {
                            "No tasks for this day."
                        }
                        Text(
                            text = emptyText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 32.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTaskDialog) {
        StandaloneTaskDialog(
            dialogTitle = stringResource(R.string.new_task_title),
            onDismiss = { showTaskDialog = false },
            onConfirm = { title, desc, date, time, sound, playSound, shouldVibrate ->
                viewModel.addStandaloneTask(
                    StandaloneTask(
                        title = title,
                        description = desc,
                        date = date,
                        time = time,
                        customSoundUri = sound,
                        shouldPlaySound = playSound,
                        shouldVibrate = shouldVibrate
                    )
                )
                showTaskDialog = false
            },
            initialTitle = "",
            initialDescription = null,
            initialDate = LocalDate.now(),
            initialTime = null,
            initialSoundUri = null,
            initialPlaySound = false,
            initialShouldVibrate = false,
            isSaving = isSaving,
            onDelete = {}
        )
    }
}

@Composable
fun DateNavigationBar(
    selectedDate: LocalDate,
    onDateClicked: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onPreviousDay() }) {
            Icon(painter = painterResource(id = AppIcons.ArrowBack), contentDescription = "Previous Day")
        }
        Row(modifier = Modifier.clickable { onDateClicked() }, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (selectedDate) {
                    LocalDate.now() -> "Today"
                    LocalDate.now().plusDays(1) -> "Tomorrow"
                    LocalDate.now().minusDays(1) -> "Yesterday"
                    else -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd"))
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Icon(painter = painterResource(id = AppIcons.ArrowDropDown), contentDescription = null)
        }
        IconButton(onClick = { onNextDay() }) {
            Icon(painter = painterResource(id = AppIcons.ArrowForward), contentDescription = "Next Day")
        }
    }
}

@Composable
fun TaskCard(task: Task, onTaskStatusChanged: (Task, Boolean) -> Unit, onDelete: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onTaskStatusChanged(task, it) }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium
                )
                AnimatedVisibility(visible = isExpanded && !task.description.isNullOrBlank()) {
                    Text(
                        text = task.description!!,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            task.time?.let { time ->
                Text(time.format(DateTimeFormatter.ofPattern("HH:mm")))
            }
        }
    }
}

private fun getGreeting(): String {
    return when (LocalTime.now().hour) {
        in 0..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}
