package com.dxtr.routini.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dxtr.routini.MainViewModel
import com.dxtr.routini.R
import com.dxtr.routini.data.StandaloneTask
import com.dxtr.routini.ui.composables.EmptyState
import com.dxtr.routini.ui.composables.StandaloneTaskDialog
import com.dxtr.routini.ui.composables.TaskChip
import com.dxtr.routini.ui.theme.AppIcons
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TasksScreen(
    viewModel: MainViewModel = viewModel()
) {
    val tasks by viewModel.allTasks.collectAsState()

    val haptic = LocalHapticFeedback.current
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<StandaloneTask?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                taskToEdit = null
                showTaskDialog = true
            }) {
                Icon(painter = painterResource(id = AppIcons.Add), contentDescription = stringResource(R.string.add_task_action))
            }
        }
    ) { innerPadding ->
        if (tasks.isEmpty()) {
            EmptyState(
                message = "No tasks or alarms. Tap '+' to add one.",
                icon = AppIcons.TaskAlt,
                actionLabel = "Add Task",
                onActionClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    taskToEdit = null
                    showTaskDialog = true
                }
            )
        } else {
            val groupedTasks = tasks.groupBy { it.date ?: LocalDate.MIN }.toSortedMap() // Null dates go to the top

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupedTasks.forEach { (date, tasksForDate) ->
                    item {
                        val headerText = when (date) {
                            LocalDate.MIN -> "No Date"
                            LocalDate.now() -> "Today"
                            LocalDate.now().plusDays(1) -> "Tomorrow"
                            else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM dd"))
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                    items(tasksForDate, key = { task -> "standalone_${task.id}" }) { task ->
                        StandaloneTaskItem(
                            task = task,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateTaskStatus(task, !task.isDone, if (date == LocalDate.MIN) null else date)
                             },
                            onEdit = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                taskToEdit = task
                                showTaskDialog = true
                             },
                            onDelete = { viewModel.deleteStandaloneTask(task) }
                        )
                    }
                }
            }
        }

        if (showTaskDialog) {
            val currentTask = taskToEdit
            StandaloneTaskDialog(
                dialogTitle = if (currentTask == null) "New Task" else "Edit Task",
                onDismiss = { showTaskDialog = false },
                onConfirm = { title, description, date, time, soundUri, shouldPlaySound, shouldVibrate ->
                    isSaving = true
                    if (currentTask == null) {
                        viewModel.addStandaloneTask(StandaloneTask(
                            title = title,
                            description = description,
                            date = date,
                            time = time,
                            customSoundUri = soundUri,
                            shouldPlaySound = shouldPlaySound,
                            shouldVibrate = shouldVibrate
                        ))
                    } else {
                        viewModel.updateStandaloneTask(currentTask.copy(
                            title = title,
                            description = description,
                            date = date,
                            time = time,
                            customSoundUri = soundUri,
                            shouldPlaySound = shouldPlaySound,
                            shouldVibrate = shouldVibrate
                        ))
                    }
                    isSaving = false
                    showTaskDialog = false
                },
                onDelete = if (currentTask != null) {
                    {
                        viewModel.deleteStandaloneTask(currentTask)
                        showTaskDialog = false
                    }
                } else null,
                initialTitle = currentTask?.title ?: "",
                initialDescription = currentTask?.description,
                initialDate = currentTask?.date,
                initialTime = currentTask?.time,
                initialSoundUri = currentTask?.customSoundUri,
                initialPlaySound = currentTask?.shouldPlaySound ?: false,
                initialShouldVibrate = currentTask?.shouldVibrate ?: false,
                isSaving = isSaving
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandaloneTaskItem(
    task: StandaloneTask,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val alpha = if (task.isDone) 0.5f else 1f
    var showDescription by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggle()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> Color.Red
                SwipeToDismissBoxValue.StartToEnd -> Color.Green
                else -> Color.Transparent
            }
            val icon = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> AppIcons.Delete
                SwipeToDismissBoxValue.StartToEnd -> AppIcons.Check
                else -> null
            }
            val alignment = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.Center
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (icon != null) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(alpha)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showDescription = !showDescription },
                        onLongPress = { onEdit() }
                    )
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = if (task.isDone) 0.dp else 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggle) {
                        Icon(
                            painter = painterResource(id = if (task.isDone) AppIcons.CheckCircle else AppIcons.RadioButtonUnchecked),
                            contentDescription = null,
                            tint = if (task.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (task.date != null) {
                                TaskChip(
                                    icon = AppIcons.CalendarMonth,
                                    text = task.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                                )
                            }
                            if (task.time != null) {
                                TaskChip(
                                    icon = AppIcons.Schedule,
                                    text = task.time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
                                )
                            }
                        }
                    }
                }
                if (showDescription && !task.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 44.dp) // align with title
                    )
                }
            }
        }
    }
}
