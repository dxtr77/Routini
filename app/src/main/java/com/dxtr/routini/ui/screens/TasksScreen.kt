package com.dxtr.routini.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dxtr.routini.MainViewModel
import com.dxtr.routini.R
import com.dxtr.routini.data.StandaloneTask
import com.dxtr.routini.ui.composables.EmptyState
import com.dxtr.routini.ui.composables.StandaloneTaskDialog
import com.dxtr.routini.ui.theme.AppIcons
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TasksScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val tasks by viewModel.standaloneTasks.collectAsState()
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
                icon = AppIcons.TaskAlt
            )
        } else {
            val groupedTasks = tasks.groupBy { it.date ?: LocalDate.MAX } // Null dates go to the end

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupedTasks.forEach { (date, tasksForDate) ->
                    item {
                        val headerText = when {
                            date == LocalDate.MAX -> "No Date"
                            date == LocalDate.now() -> "Today"
                            date == LocalDate.now().plusDays(1) -> "Tomorrow"
                            else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM dd"))
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                    items(tasksForDate, key = { it.id }) { task ->
                        StandaloneTaskItem(
                            task = task,
                            onToggle = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateStandaloneTask(task.copy(isDone = !task.isDone))
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
            val task = taskToEdit
            StandaloneTaskDialog(
                dialogTitle = if (task == null) stringResource(R.string.new_task_title) else stringResource(R.string.edit_task_title),
                initialTitle = task?.title ?: "",
                initialDescription = task?.description,
                initialDate = task?.date,
                initialTime = task?.time,
                initialSoundUri = task?.customSoundUri,
                initialPlaySound = task?.shouldPlaySound ?: false,
                isSaving = isSaving,
                onDismiss = { 
                    showTaskDialog = false
                    taskToEdit = null
                },
                onConfirm = { title, description, date, time, soundUri, shouldPlaySound ->
                    isSaving = true
                    if (task == null) {
                        viewModel.addStandaloneTask(StandaloneTask(
                            title = title,
                            description = description,
                            date = date,
                            time = time,
                            customSoundUri = soundUri,
                            shouldPlaySound = shouldPlaySound
                        ))
                    } else {
                        viewModel.updateStandaloneTask(task.copy(
                            title = title,
                            description = description,
                            date = date,
                            time = time,
                            customSoundUri = soundUri,
                            shouldPlaySound = shouldPlaySound
                        ))
                    }
                     showTaskDialog = false
                     taskToEdit = null
                     isSaving = false
                },
                onDelete = if (task != null) {
                    {
                        viewModel.deleteStandaloneTask(task)
                        showTaskDialog = false
                        taskToEdit = null
                    }
                } else null
            )
        }
    }
}

@Composable
fun StandaloneTaskItem(
    task: StandaloneTask,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val alpha = if (task.isDone) 0.5f else 1f
    var showDescription by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(vertical = 4.dp)
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (task.date != null) {
                           TaskChip(icon = AppIcons.CalendarMonth, text = task.date.format(DateTimeFormatter.ofPattern("MMM dd")))
                        }
                        if (task.time != null) {
                           TaskChip(icon = AppIcons.Alarm, text = task.time.format(DateTimeFormatter.ofPattern("HH:mm")))
                        }
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(painter = painterResource(id = AppIcons.Delete), contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
             AnimatedVisibility(visible = showDescription && !task.description.isNullOrBlank()) {
                 Text(
                    text = task.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 52.dp, top = 8.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
fun TaskChip(@DrawableRes icon: Int, text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
