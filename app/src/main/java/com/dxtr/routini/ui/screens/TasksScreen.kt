package com.dxtr.routini.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val tasks by viewModel.standaloneTasks.collectAsState()
    val haptic = LocalHapticFeedback.current
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<StandaloneTask?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showAddTaskDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task_action))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = tasks, key = { it.id }) { task ->
                StandaloneTaskItem(
                    task = task,
                    onToggle = {
                        viewModel.updateStandaloneTask(task.copy(isDone = !task.isDone))
                    },
                    onEdit = {
                        taskToEdit = task
                    },
                    onDelete = {
                        viewModel.deleteStandaloneTask(task)
                    }
                )
            }
        }

        if (showAddTaskDialog) {
            StandaloneTaskDialog(
                dialogTitle = stringResource(R.string.new_task_title),
                onDismiss = { showAddTaskDialog = false },
                onConfirm = { title, description, date, time, soundUri, shouldPlaySound ->
                    val newTask = StandaloneTask(
                        title = title,
                        description = description,
                        date = date,
                        time = time,
                        customSoundUri = soundUri,
                        shouldPlaySound = shouldPlaySound
                    )
                    viewModel.addStandaloneTask(newTask)
                    showAddTaskDialog = false
                }
            )
        }

        if (taskToEdit != null) {
            val task = taskToEdit!!
            StandaloneTaskDialog(
                dialogTitle = stringResource(R.string.edit_task_title),
                initialTitle = task.title,
                initialDescription = task.description,
                initialDate = task.date,
                initialTime = task.time,
                initialSoundUri = task.customSoundUri,
                initialPlaySound = task.shouldPlaySound,
                onDismiss = { taskToEdit = null },
                onConfirm = { title, description, date, time, soundUri, shouldPlaySound ->
                    viewModel.updateStandaloneTask(task.copy(
                        title = title,
                        description = description,
                        date = date,
                        time = time,
                        customSoundUri = soundUri,
                        shouldPlaySound = shouldPlaySound
                    ))
                    taskToEdit = null
                },
                onDelete = {
                    viewModel.deleteStandaloneTask(task)
                    taskToEdit = null
                }
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
    val haptic = LocalHapticFeedback.current
    var showDescription by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showDescription = !showDescription
                    },
                    onLongPress = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEdit() 
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (task.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (task.isDone) stringResource(R.string.mark_not_done_desc) else stringResource(R.string.mark_done_desc),
                    tint = if (task.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = if (task.isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )

                AnimatedVisibility(visible = showDescription && !task.description.isNullOrBlank()) {
                    Text(
                        text = task.description ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.date != null) {
                        Text(
                            text = task.date.format(DateTimeFormatter.ofPattern("MMM dd")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    if (task.time != null) {
                        Text(
                            text = task.time.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        if (task.shouldPlaySound) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = stringResource(R.string.alarm_option),
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.notify_option),
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else if (task.date == null) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = stringResource(R.string.any_time_label),
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.any_time_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_task_desc),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_task_desc),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
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
    onDismiss: () -> Unit,
    onConfirm: (String, String?, LocalDate?, LocalTime?, String?, Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription ?: "") }
    
    // Separate toggles for Date and Time
    var selectedDate by remember { mutableStateOf(initialDate) }
    var isTimeEnabled by remember { mutableStateOf(initialTime != null) }
    
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime?.hour ?: LocalTime.now().hour,
        initialMinute = initialTime?.minute ?: LocalTime.now().minute
    )
    
    var selectedSoundUri by remember { mutableStateOf(initialSoundUri?.let { Uri.parse(it) }) }
    var alertMode by remember { mutableStateOf(if (initialPlaySound) 1 else 0) }
    
    var showAlarmConfig by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Date picker state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )
    
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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_desc_label)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Date Selection Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.date_label),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    Button(
                        onClick = { showDatePicker = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedDate != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedDate != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: stringResource(R.string.set_date_action)
                        )
                    }
                    
                    if (selectedDate != null) {
                        IconButton(onClick = { selectedDate = null }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_date_desc))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time/Alarm Selection Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.alert_label),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    IconButton(
                        onClick = { showAlarmConfig = true },
                        modifier = Modifier.background(
                            color = if (isTimeEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = if (isTimeEnabled) {
                                if (alertMode == 1) Icons.Default.Alarm else Icons.Default.Notifications
                            } else {
                                Icons.Default.Add 
                            },
                            contentDescription = stringResource(R.string.set_alert_desc),
                            tint = if (isTimeEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (isTimeEnabled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (alertMode == 1) stringResource(R.string.alarm_option) else stringResource(R.string.notify_option),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { isTimeEnabled = false }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_alert_desc))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.none_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank()) {
                        val time = if (isTimeEnabled) LocalTime.of(timePickerState.hour, timePickerState.minute) else null
                        onConfirm(
                            title,
                            description.ifBlank { null },
                            selectedDate,
                            time,
                            selectedSoundUri?.toString(),
                            isTimeEnabled && alertMode == 1
                        ) 
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (dialogTitle.startsWith("Edit")) stringResource(R.string.save_action) else stringResource(R.string.add_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action))
            }
        }
    )
    
    if (showAlarmConfig) {
        AlarmConfigDialog(
            initialHour = if (isTimeEnabled) timePickerState.hour else LocalTime.now().hour,
            initialMinute = if (isTimeEnabled) timePickerState.minute else LocalTime.now().minute,
            initialAlertMode = alertMode,
            initialSoundUri = selectedSoundUri,
            onDismiss = { showAlarmConfig = false },
            onConfirm = { hour, minute, mode, uri ->
                timePickerState.hour = hour
                timePickerState.minute = minute
                alertMode = mode
                selectedSoundUri = uri
                isTimeEnabled = true
                showAlarmConfig = false
            }
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
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}