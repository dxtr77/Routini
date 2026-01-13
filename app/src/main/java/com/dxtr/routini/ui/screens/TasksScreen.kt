package com.dxtr.routini.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dxtr.routini.MainViewModel
import com.dxtr.routini.R
import com.dxtr.routini.data.StandaloneTask
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task_action))
            }
        }
    ) { innerPadding ->
        if (tasks.isEmpty()) {
            EmptyState(
                message = "No tasks or alarms. Tap '+' to add one.",
                icon = Icons.Default.TaskAlt
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
                        imageVector = if (task.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
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
                           TaskChip(icon = Icons.Default.CalendarMonth, text = task.date.format(DateTimeFormatter.ofPattern("MMM dd")))
                        }
                        if (task.time != null) {
                           TaskChip(icon = Icons.Default.Alarm, text = task.time.format(DateTimeFormatter.ofPattern("HH:mm")))
                        }
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
fun TaskChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandaloneTaskDialog(
    dialogTitle: String,
    initialTitle: String = "",
    initialDescription: String? = null,
    initialDate: LocalDate? = null,
    initialTime: LocalTime? = null,
    initialSoundUri: String? = null,
    initialPlaySound: Boolean = false,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, LocalDate?, LocalTime?, String?, Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription ?: "") }

    var selectedDate by remember { mutableStateOf(initialDate) }
    var selectedTime by remember { mutableStateOf(initialTime) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var shouldPlaySound by remember { mutableStateOf(initialPlaySound) }
    var selectedSoundUri by remember { mutableStateOf(initialSoundUri?.let { Uri.parse(it) }) }
    var showSoundSelectionDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> selectedSoundUri = uri }
    )

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            selectedSoundUri = uri
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime?.hour ?: LocalTime.now().hour,
            initialMinute = selectedTime?.minute ?: LocalTime.now().minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel_action)) }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.ok_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel_action)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dialogTitle)
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_task_desc), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_desc_label)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: stringResource(R.string.set_date_action))
                    }
                    if (selectedDate != null) {
                        IconButton(onClick = { selectedDate = null }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_date_desc))
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: stringResource(R.string.set_time_action))
                    }
                     if (selectedTime != null) {
                        IconButton(onClick = { selectedTime = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear time")
                        }
                    }
                }

                if (selectedTime != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (shouldPlaySound) Icons.Default.Alarm else Icons.Default.Notifications,
                                contentDescription = null, tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (shouldPlaySound) stringResource(R.string.alarm_option) else stringResource(R.string.notify_option), style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(checked = shouldPlaySound, onCheckedChange = { shouldPlaySound = it })
                    }

                    if (shouldPlaySound) {
                        Button(
                            onClick = { showSoundSelectionDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (selectedSoundUri != null) stringResource(R.string.sound_selected) else stringResource(R.string.pick_custom_sound))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && !isSaving) {
                        onConfirm(title, description.ifBlank { null }, selectedDate, selectedTime, selectedSoundUri?.toString(), shouldPlaySound)
                    }
                },
                enabled = title.isNotBlank() && !isSaving
            ) {
                Text(stringResource(R.string.save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        }
    )

    if (showSoundSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showSoundSelectionDialog = false },
            title = { Text("Choose Sound Source") },
            confirmButton = {}, // No confirm button, actions are immediate
            dismissButton = { TextButton(onClick = { showSoundSelectionDialog = false }) { Text("Cancel") } },
            text = {
                Column {
                    Button(onClick = { /* Launch Ringtone Picker */ }, modifier = Modifier.fillMaxWidth()) {
                        Text("System Ringtones")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { filePickerLauncher.launch("audio/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text("Audio File")
                    }
                }
            }
        )
    }
}
