package com.dxtr.routini.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dxtr.routini.MainViewModel
import com.dxtr.routini.data.Task
import com.dxtr.routini.ui.composables.AnimatedCheckbox
import com.dxtr.routini.ui.composables.CircularProgressRing
import com.dxtr.routini.ui.composables.GlassCard
import com.dxtr.routini.ui.theme.AppIcons
import com.dxtr.routini.ui.theme.ErrorLight
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel = viewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    // EXCLUDE Anytime tasks (standalone tasks with null date) from progress
    val progressTasks = remember(tasks) {
        tasks.filter { task ->
            !(task is com.dxtr.routini.data.StandaloneTask && task.date == null)
        }
    }
    
    val completedTasks = progressTasks.count { it.isDone }
    val progress = if (progressTasks.isNotEmpty()) completedTasks.toFloat() / progressTasks.size else 0f

    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    var taskToDelete by remember { mutableStateOf<com.dxtr.routini.data.StandaloneTask?>(null) }

    // Glassmorphism Background Gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp
                )
            ) {
                item {
                    // Header Section
                    Column {
                        Text(
                            text = getGreeting(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Overview for ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd"))}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        DateNavigationBar(
                            selectedDate = selectedDate,
                            onDateClicked = { showDatePicker = true },
                            onPreviousDay = { viewModel.onPreviousDay() },
                            onNextDay = { viewModel.onNextDay() },
                            onResetToday = { viewModel.onDateSelected(LocalDate.now()) }
                        )
                    }
                }


                if (tasks.isNotEmpty()) {
                    item {
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Daily Progress",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "$completedTasks of ${tasks.size} tasks completed",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                CircularProgressRing(
                                    progress = progress,
                                    size = 80.dp,
                                    strokeWidth = 8.dp
                                )
                            }
                        }
                    }
                }

                item {
                    MotivationalQuoteCard()
                }

                // Group Tasks
                val groupedTasks = groupTasksByTime(tasks)
                
                if (groupedTasks.isEmpty() && tasks.isEmpty()) {
                    item {
                        com.dxtr.routini.ui.composables.EmptyState(
                            message = "No tasks for today!",
                            icon = AppIcons.CheckCircle, // Using CheckCircle instead of EventAvailable if strict on icons
                            showTip = true
                        )
                    }
                } else {
                    groupedTasks.forEach { (header, tasksInGroup) ->
                        if (tasksInGroup.isNotEmpty()) {
                            item {
                                Text(
                                    text = header,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                                )
                            }
                            items(tasksInGroup, key = { it.id }) { task ->
                                    SwipeableTaskItem(
                                        task = task,
                                        taskToDelete = taskToDelete,
                                        onStatusChange = { isDone -> 
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            viewModel.updateTaskStatus(task, isDone, selectedDate)
                                        },
                                        onDelete = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            taskToDelete = task as com.dxtr.routini.data.StandaloneTask
                                        },
                                        isDeletable = task is com.dxtr.routini.data.StandaloneTask
                                    )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onDateSelected(
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task?") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                Button(
                    onClick = {
                        taskToDelete?.let { task ->
                            viewModel.deleteStandaloneTask(task)
                            scope.launch {
                                snackbarHostState.showSnackbar("Task deleted")
                            }
                        }
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskItem(
    task: Task,
    taskToDelete: com.dxtr.routini.data.StandaloneTask?,
    onStatusChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    isDeletable: Boolean
) {
    if (!isDeletable) {
         TaskCard(task = task, onTaskStatusChanged = { _, isDone -> onStatusChange(isDone) })
         return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false // Don't stick, wait for confirmation dialog
            } else {
                false
            }
        }
    )

    // Robust snap-back: if deletion is cancelled (taskToDelete becomes null), ensure we are Settled
    LaunchedEffect(taskToDelete) {
        if (taskToDelete == null && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) ErrorLight else Color.Transparent,
                label = "bgColor"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    painter = painterResource(id = AppIcons.Delete),
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        },
        content = {
            TaskCard(task = task, onTaskStatusChanged = { _, isDone -> onStatusChange(isDone) })
        }
    )
}

@Composable
fun TaskCard(task: Task, onTaskStatusChanged: (Task, Boolean) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { it / 4 }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { isExpanded = !isExpanded },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (task.isDone)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedCheckbox(
                    checked = task.isDone,
                    onCheckedChange = { onTaskStatusChanged(task, it) }
                )
                
                Spacer(modifier = Modifier.size(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (task.isDone) FontWeight.Normal else FontWeight.SemiBold,
                        textDecoration = if (task.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                        color = if (task.isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isExpanded && !task.description.isNullOrBlank()) {
                         Spacer(modifier = Modifier.height(4.dp))
                         Text(
                             text = task.description!!,
                             style = MaterialTheme.typography.bodyMedium,
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                    }
                    
                    task.time?.let { time ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = AppIcons.Schedule),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            val context = androidx.compose.ui.platform.LocalContext.current
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = com.dxtr.routini.utils.TimeUtils.formatTime(context, time),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DateNavigationBar(
    selectedDate: LocalDate,
    onDateClicked: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onResetToday: () -> Unit
) {
    val today = LocalDate.now()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousDay,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    painter = painterResource(id = AppIcons.ArrowBack),
                    contentDescription = "Previous"
                )
            }

            Row(
                modifier = Modifier.clickable { onDateClicked() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedDate) {
                        today -> "Today"
                        today.plusDays(1) -> "Tomorrow"
                        today.minusDays(1) -> "Yesterday"
                        else -> selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM dd"))
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    painter = painterResource(id = AppIcons.ArrowDropDown),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onNextDay,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    painter = painterResource(id = AppIcons.ArrowForward),
                    contentDescription = "Next"
                )
            }
        }

        if (selectedDate != today) {
            TextButton(
                onClick = onResetToday,
                modifier = Modifier.padding(top = 0.dp)
            ) {
                Text(
                    "Back to Today",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@Composable
fun MotivationalQuoteCard() {
    val quotes = listOf(
        "Small steps every day.",
        "Consistency is key.",
        "You got this!",
        "Focus on progress, not perfection."
    )
    val quote = remember { quotes.random() }

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = AppIcons.Star),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = quote,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun getGreeting(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

fun groupTasksByTime(tasks: List<Task>): Map<String, List<Task>> {
    val morning = mutableListOf<Task>()
    val afternoon = mutableListOf<Task>()
    val evening = mutableListOf<Task>()
    val anytime = mutableListOf<Task>()

    tasks.forEach { task ->
        val time = task.time
        if (time == null) {
            anytime.add(task)
        } else {
            when (time.hour) {
                in 5..11 -> morning.add(task)
                in 12..17 -> afternoon.add(task)
                else -> evening.add(task)
            }
        }
    }

    val result = linkedMapOf<String, List<Task>>()
    if (morning.isNotEmpty()) result["Morning"] = morning
    if (afternoon.isNotEmpty()) result["Afternoon"] = afternoon
    if (evening.isNotEmpty()) result["Evening"] = evening
    if (anytime.isNotEmpty()) result["Anytime"] = anytime
    return result
}
