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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dxtr.routini.MainViewModel
import com.dxtr.routini.data.StandaloneTask
import com.dxtr.routini.ui.composables.EmptyState
import com.dxtr.routini.ui.composables.StandaloneTaskDialog
import com.dxtr.routini.ui.composables.TaskChip
import com.dxtr.routini.ui.theme.AppIcons
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(
    viewModel: MainViewModel = viewModel(),
    onAddTask: () -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val haptic = LocalHapticFeedback.current
    var taskToEdit by remember { mutableStateOf<StandaloneTask?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Only show EmptyState if there are no tasks AND no search query
            if (tasks.isEmpty() && searchQuery.isEmpty()) {
                EmptyState(
                    message = "No tasks or alarms. Tap '+' to add one.",
                    icon = AppIcons.TaskAlt,
                    actionLabel = "Add Task",
                    onActionClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddTask()
                    }
                )
            } else {
                // Search Bar - Always visible when there are tasks or a search is active
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search tasks...") },
                    leadingIcon = { Icon(painterResource(id = AppIcons.Search), contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(painter = painterResource(id = AppIcons.Close), contentDescription = "Clear")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                if (tasks.isEmpty()) {
                    // Search produced no results
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tasks found for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val groupedTasks = tasks.groupBy { it.date ?: LocalDate.MIN }.toSortedMap() // Null dates go to the top

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
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
                                        showEditDialog = true
                                    },
                                    onDelete = {
                                        viewModel.deleteStandaloneTask(task)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Task deleted")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && taskToEdit != null) {
        val currentTask = taskToEdit!!
        StandaloneTaskDialog(
            dialogTitle = "Edit Task",
            onDismiss = { showEditDialog = false },
            onConfirm = { title, description, date, time, soundUri, shouldPlaySound, shouldVibrate ->
                viewModel.updateStandaloneTask(currentTask.copy(
                    title = title,
                    description = description,
                    date = date,
                    time = time,
                    customSoundUri = soundUri,
                    shouldPlaySound = shouldPlaySound,
                    shouldVibrate = shouldVibrate
                ))
                showEditDialog = false
            },
            onDelete = {
                viewModel.deleteStandaloneTask(currentTask)
                showEditDialog = false
            },
            initialTitle = currentTask.title,
            initialDescription = currentTask.description,
            initialDate = currentTask.date,
            initialTime = currentTask.time,
            initialSoundUri = currentTask.customSoundUri,
            initialPlaySound = currentTask.shouldPlaySound,
            initialShouldVibrate = currentTask.shouldVibrate,
            isSaving = false, // Edit is usually fast, simplified for now
            isNewTask = false
        )
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
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
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
                    .padding(vertical = 4.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (icon != null) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showDescription = !showDescription },
                        onLongPress = { onEdit() }
                    )
                },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (task.isDone)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = if (task.isDone) androidx.compose.ui.text.font.FontWeight.Normal else androidx.compose.ui.text.font.FontWeight.Medium
                            ),
                            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                            color = if (task.isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (task.date != null) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(painter = painterResource(id = AppIcons.CalendarMonth), contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Text(
                                            text = task.date.format(DateTimeFormatter.ofPattern("MMM dd")),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            if (task.time != null) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                     val timeText = task.time.format(DateTimeFormatter.ofPattern("HH:mm"))
                                     Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Optional: Icon could go here, but usually just text is fine for time if date has icon
                                        Text(
                                            text = timeText,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                        )
                                     }
                                }
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
                        modifier = Modifier.padding(start = 52.dp, end = 16.dp)
                    )
                }
            }
        }
    }
}
