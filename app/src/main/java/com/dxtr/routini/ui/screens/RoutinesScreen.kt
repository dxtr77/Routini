package com.dxtr.routini.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dxtr.routini.MainViewModel
import com.dxtr.routini.R
import com.dxtr.routini.data.DayOfWeek
import com.dxtr.routini.data.Routine
import com.dxtr.routini.data.RoutineTask
import com.dxtr.routini.ui.navigation.Screen
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun RoutinesScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val routines by viewModel.routines.collectAsState()
    val haptic = LocalHapticFeedback.current
    var showRoutineDialog by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<Routine?>(null) }

    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    val launcherNotification = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle result
    }

    LaunchedEffect(Unit) {
        val needsNotification = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        
        val needsOverlay = !Settings.canDrawOverlays(context)
        
        if (needsNotification || needsOverlay) {
            showPermissionDialog = true
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                routineToEdit = null // Clear any routine being edited
                showRoutineDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_routine_desc))
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
            items(items = routines, key = { it.id }) { routine ->
                RoutineCard(
                    routine = routine,
                    viewModel = viewModel,
                    onEditRoutine = {
                        routineToEdit = routine
                        showRoutineDialog = true
                    }
                )
            }
        }

        if (showRoutineDialog) {
            RoutineDialog(
                routine = routineToEdit,
                onDismiss = { showRoutineDialog = false },
                onConfirm = { name, days ->
                    if (routineToEdit == null) {
                        val newRoutine = Routine(
                            name = name,
                            themeColor = android.graphics.Color.BLUE,
                            recurringDays = days
                        )
                        viewModel.addRoutine(newRoutine)
                    } else {
                        val updatedRoutine = routineToEdit!!.copy(
                            name = name,
                            recurringDays = days
                        )
                        viewModel.updateRoutine(updatedRoutine)
                    }
                    showRoutineDialog = false
                },
                onDelete = {
                    routineToEdit?.let { viewModel.deleteRoutine(it) }
                    showRoutineDialog = false
                }
            )
        }
        
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text(stringResource(R.string.permissions_required_title)) },
                text = {
                    Text(stringResource(R.string.permissions_required_message))
                },
                confirmButton = {
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                             launcherNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        if (!Settings.canDrawOverlays(context)) {
                             val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                             context.startActivity(intent)
                        }
                        showPermissionDialog = false
                    }) {
                        Text(stringResource(R.string.grant_action))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text(stringResource(R.string.later_action))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDialog(
    routine: Routine?,
    onDismiss: () -> Unit,
    onConfirm: (String, List<DayOfWeek>) -> Unit,
    onDelete: () -> Unit
) {
    val isEditing = routine != null
    var name by remember { mutableStateOf(routine?.name ?: "") }
    var selectedDays by remember { mutableStateOf(routine?.recurringDays ?: DayOfWeek.values().toList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isEditing) "Edit Routine" else stringResource(R.string.new_routine_title))
                if (isEditing) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Routine", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.routine_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.recurring_days_label), style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DayOfWeek.values().forEach { day ->
                        val isSelected = selectedDays.contains(day)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    selectedDays = if (isSelected) {
                                        selectedDays - day
                                    } else {
                                        selectedDays + day
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.name.take(1),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedDays) },
                enabled = name.isNotBlank() && selectedDays.isNotEmpty()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineCard(
    routine: Routine,
    viewModel: MainViewModel,
    onEditRoutine: () -> Unit
) {
    val tasks by viewModel.getTasksForRoutine(routine.id).collectAsState(initial = emptyList())
    val completedTasks = tasks.count { it.isDone }
    val totalTasks = tasks.size
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    
    var expanded by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<RoutineTask?>(null) }
    
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                     Text(
                        text = routine.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val daysText = if (routine.recurringDays.size == 7) "Every day" 
                                   else routine.recurringDays.joinToString(", ") { it.name.take(3) }
                    Text(
                        text = daysText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditRoutine) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_task_title), // Reusing string
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 4.dp,
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.rotate(rotationState)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand tasks"
                        )
                    }
                }
            }
            
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.tasks_tab),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (tasks.isEmpty()) {
                        Text(
                            text = "No tasks yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        tasks.forEach { task ->
                            QuickTaskItem(
                                task = task, 
                                onToggle = {
                                    viewModel.updateRoutineTask(task.copy(isDone = !task.isDone))
                                },
                                onEdit = {
                                    taskToEdit = task
                                },
                                onDelete = {
                                    viewModel.deleteRoutineTask(task)
                                }
                            )
                        }
                    }
                    
                    Button(
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_task_action))
                    }
                }
            }
        }
    }
    
    if (showAddTaskDialog) {
        TaskDialog(
            dialogTitle = stringResource(R.string.new_task_title),
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, description, time, soundUri, shouldPlaySound ->
                val newTask = RoutineTask(
                    routineId = routine.id,
                    title = title,
                    description = description,
                    time = time,
                    customSoundUri = soundUri,
                    shouldPlaySound = shouldPlaySound
                )
                viewModel.addRoutineTask(newTask)
                showAddTaskDialog = false
            }
        )
    }

    if (taskToEdit != null) {
        val task = taskToEdit!!
        TaskDialog(
            dialogTitle = stringResource(R.string.edit_task_title),
            initialTitle = task.title,
            initialDescription = task.description,
            initialTime = task.time,
            initialSoundUri = task.customSoundUri,
            initialPlaySound = task.shouldPlaySound,
            onDismiss = { taskToEdit = null },
            onConfirm = { title, description, time, soundUri, shouldPlaySound ->
                viewModel.updateRoutineTask(task.copy(
                    title = title,
                    description = description,
                    time = time,
                    customSoundUri = soundUri,
                    shouldPlaySound = shouldPlaySound
                ))
                taskToEdit = null
            },
            onDelete = {
                viewModel.deleteRoutineTask(task)
                taskToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    dialogTitle: String,
    initialTitle: String = "",
    initialDescription: String? = null,
    initialTime: LocalTime? = null,
    initialSoundUri: String? = null,
    initialPlaySound: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, LocalTime?, String?, Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription ?: "") }
    
    var selectedTime by remember { mutableStateOf(initialTime) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    var shouldPlaySound by remember { mutableStateOf(initialPlaySound) }
    var selectedSoundUri by remember { mutableStateOf(initialSoundUri?.let { Uri.parse(it) }) }
    var showSoundSelectionDialog by remember { mutableStateOf(false) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedSoundUri = uri
    }
    
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) selectedSoundUri = uri
        }
    }
    
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime?.hour ?: 8,
            initialMinute = selectedTime?.minute ?: 0
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) }
        )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
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
                
                // Time Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Set Time"
                            )
                        }
                    }
                    
                    if (selectedTime != null) {
                        IconButton(onClick = { selectedTime = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear time")
                        }
                    }
                }

                // Alert Config Row
                if (selectedTime != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (shouldPlaySound) Icons.Default.Alarm else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (shouldPlaySound) stringResource(R.string.alarm_option) else stringResource(R.string.notify_option),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (shouldPlaySound) "Sound enabled" else "Silent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = shouldPlaySound,
                            onCheckedChange = { shouldPlaySound = it }
                        )
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
                    if (title.isNotBlank()) {
                        onConfirm(
                            title,
                            description.ifBlank { null },
                            selectedTime,
                            selectedSoundUri?.toString(),
                            shouldPlaySound
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
    
    if (showSoundSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showSoundSelectionDialog = false },
            title = { Text("Choose Sound Source") },
            text = {
                Column {
                    Button(
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedSoundUri)
                            }
                            ringtonePickerLauncher.launch(intent)
                            showSoundSelectionDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("System Ringtones")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            filePickerLauncher.launch("audio/*")
                            showSoundSelectionDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Audio File")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSoundSelectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun QuickTaskItem(
    task: RoutineTask,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showDescription by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
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
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
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
            
            // Description only visible when pressed (toggled)
            AnimatedVisibility(visible = showDescription && !task.description.isNullOrBlank()) {
                Text(
                    text = task.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                }
            }
        }
        
        // Edit/Delete actions in the row
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