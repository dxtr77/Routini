package com.dxtr.routini.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dxtr.routini.MainViewModel
import com.dxtr.routini.R
import com.dxtr.routini.data.Routine
import com.dxtr.routini.ui.composables.EmptyState
import com.dxtr.routini.ui.navigation.Screen
import com.dxtr.routini.ui.theme.AppIcons
import java.time.DayOfWeek

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
    ) { }

    LaunchedEffect(Unit) {
        val needsNotification = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        val needsOverlay = !Settings.canDrawOverlays(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val needsExactAlarm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()

        if (needsNotification || needsOverlay || needsExactAlarm) {
            showPermissionDialog = true
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                routineToEdit = null
                showRoutineDialog = true
            }) {
                Icon(painter = painterResource(id = AppIcons.Add), contentDescription = stringResource(R.string.add_routine_desc))
            }
        }
    ) { innerPadding ->
        if (routines.isEmpty()) {
            EmptyState(
                message = "No routines yet. Tap '+' to create one!",
                icon = AppIcons.List,
                actionLabel = "Create Routine",
                onActionClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    routineToEdit = null
                    showRoutineDialog = true
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = routines, key = { it.id }) { routine ->
                    ModernRoutineCard(
                        routine = routine,
                        viewModel = viewModel,
                        onNavigateToDetail = {
                            navController.navigate(Screen.RoutineDetail.createRoute(routine.id))
                        },
                        onEdit = { 
                            routineToEdit = routine
                            showRoutineDialog = true
                        },
                        onDelete = { viewModel.deleteRoutine(routine) }
                    )
                }
            }
        }

        if (showRoutineDialog) {
            RoutineDialog(
                routine = routineToEdit,
                onConfirm = { name, days ->
                    if (routineToEdit == null) {
                        viewModel.addRoutine(Routine(name = name, themeColor = android.graphics.Color.BLUE, recurringDays = days))
                    } else {
                        viewModel.updateRoutine(routineToEdit!!.copy(name = name, recurringDays = days))
                    }
                    showRoutineDialog = false
                },
                onDelete = {
                    routineToEdit?.let { viewModel.deleteRoutine(it) }
                    showRoutineDialog = false
                },
                onDismiss = { showRoutineDialog = false }
            )
        }
        
        if (showPermissionDialog) {
             AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                icon = { Icon(painter = painterResource(id = AppIcons.Warning), contentDescription = null) },
                title = { Text(stringResource(R.string.permissions_required_title)) },
                text = { Text(stringResource(R.string.permissions_required_message)) },
                confirmButton = {
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            launcherNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        if (!Settings.canDrawOverlays(context)) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                            context.startActivity(intent)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            if (!alarmManager.canScheduleExactAlarms()) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                context.startActivity(intent)
                            }
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

@Composable
fun SmartDayIndicator(activeDays: List<DayOfWeek>) {
    val isEveryDay = activeDays.size == 7
    val isWeekdays = activeDays.size == 5 && activeDays.containsAll(
        listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    )
    val isWeekends = activeDays.size == 2 && activeDays.containsAll(
        listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    )

    val text = when {
        isEveryDay -> "Every Day"
        isWeekdays -> "Weekdays"
        isWeekends -> "Weekends"
        else -> activeDays.joinToString { it.name.take(3).lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } } 
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun DayIndicator(activeDays: List<DayOfWeek>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val days = listOf("M", "T", "W", "T", "F", "S", "S")
        val allDays = DayOfWeek.values()

        days.forEachIndexed { index, label ->
            val isActive = activeDays.contains(allDays[index])
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernRoutineCard(
    routine: Routine,
    viewModel: MainViewModel,
    onNavigateToDetail: () -> Unit, 
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tasks by viewModel.getTasksForRoutine(routine.id).collectAsState(initial = emptyList())
    val completedTasks = tasks.count { it.isDone }
    val totalTasks = tasks.size
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progressAnimation")
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToDetail() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SmartDayIndicator(routine.recurringDays)
            }

            if (progress > 0) {
                if (progress == 1f) {
                    Icon(
                        painter = painterResource(id = AppIcons.CheckCircle),
                        contentDescription = "Completed",
                        tint = Color.Green,
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                        CircularProgressIndicator(
                            progress = animatedProgress,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 6.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } 

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(painter = painterResource(id = AppIcons.MoreVert), contentDescription = "More Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { 
                            onEdit()
                            showMenu = false 
                        },
                        leadingIcon = { Icon(painterResource(id = AppIcons.Edit), contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { 
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = { Icon(painterResource(id = AppIcons.Delete), contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
fun RoutineDialog(
    routine: Routine?,
    onConfirm: (String, List<DayOfWeek>) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = routine != null
    var name by remember { mutableStateOf(routine?.name ?: "") }
    var selectedDays by remember { mutableStateOf(routine?.recurringDays ?: emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Routine" else stringResource(R.string.new_routine_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            RoutineDialogContent(
                name = name,
                onNameChange = { name = it },
                selectedDays = selectedDays,
                onSelectedDaysChange = { selectedDays = it },
                isEditing = isEditing,
                onDelete = onDelete
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && !isSaving) {
                        isSaving = true
                        onConfirm(name, selectedDays)
                    }
                },
                enabled = name.isNotBlank() && selectedDays.isNotEmpty() && !isSaving
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

@Composable
fun RoutineDialogContent(
    name: String,
    onNameChange: (String) -> Unit,
    selectedDays: List<DayOfWeek>,
    onSelectedDaysChange: (List<DayOfWeek>) -> Unit,
    isEditing: Boolean,
    onDelete: () -> Unit
) {
    Column {
        if (isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(id = AppIcons.Delete),
                        contentDescription = "Delete Routine",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.routine_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.recurring_days_label), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DayOfWeek.values().forEach { day ->
                val isSelected = selectedDays.contains(day)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            onSelectedDaysChange(
                                if (isSelected) selectedDays - day else selectedDays + day
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.name.take(1),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
