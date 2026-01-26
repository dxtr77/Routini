package com.dxtr.routini.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dxtr.routini.MainViewModel
import com.dxtr.routini.data.StandaloneTask
import com.dxtr.routini.ui.composables.AnimatedCheckbox
import com.dxtr.routini.ui.composables.CalendarView
import com.dxtr.routini.ui.composables.EmptyState
import com.dxtr.routini.ui.composables.GlassCard
import com.dxtr.routini.ui.composables.StandaloneTaskDialog
import com.dxtr.routini.ui.theme.AppIcons
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TasksScreen(
    viewModel: MainViewModel = viewModel(),
    onAddTask: () -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val taskCountsPerDate by viewModel.taskCountsPerDate.collectAsState()

    val haptic = LocalHapticFeedback.current
    var taskToEdit by remember { mutableStateOf<StandaloneTask?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var filterType by rememberSaveable { mutableStateOf("ALL") }
    var viewMode by rememberSaveable { mutableStateOf("LIST") } // LIST or CALENDAR
    var isDateFiltered by rememberSaveable { mutableStateOf(false) }
    var localSelectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) } // Set of User IDs
    var taskToDelete by remember { mutableStateOf<StandaloneTask?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    
    // Auto-enable filter if coming from dashboard with a specific date? 
    // Actually better to keep it explicit for the "Tasks" screen.

    val snackbarHostState = remember { SnackbarHostState() }
    rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Only show EmptyState if there are no tasks AND no search query
            if (tasks.isEmpty() && searchQuery.isEmpty()) {
                EmptyState(
                    message = "No tasks or alarms. Tap '+' to add one.",
                    icon = AppIcons.TaskAlt,
                    actionLabel = "Add Task",
                    onActionClick = onAddTask
                )
            } else {

                if (selectedIds.isNotEmpty()) {
                    GlassCard(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${selectedIds.size} Selected",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row {
                                IconButton(onClick = {
                                    val tasksToUpdate = tasks.filter { it.id in selectedIds }
                                    tasksToUpdate.forEach {
                                        viewModel.updateTaskStatus(
                                            it,
                                            true,
                                            it.date
                                        )
                                    }
                                    selectedIds = emptySet()
                                }) {
                                    Icon(
                                        painter = painterResource(id = AppIcons.Check),
                                        contentDescription = "Mark items done",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
                                    showBulkDeleteConfirm = true
                                }) {
                                    Icon(
                                        painter = painterResource(id = AppIcons.Delete),
                                        contentDescription = "Delete items",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                IconButton(onClick = { selectedIds = emptySet() }) {
                                    Icon(
                                        painter = painterResource(id = AppIcons.Close),
                                        contentDescription = "Clear Selection",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                } else {
                    GlassCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        contentPadding = 8.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SlidingFilterToggle(
                                    selectedFilter = filterType,
                                    onFilterSelected = { filterType = it }
                                )
                                
                                SlidingViewToggle(
                                    viewMode = viewMode,
                                    onViewChange = { viewMode = it }
                                )
                            }

                            if (viewMode == "CALENDAR") {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                                CalendarView(
                                    selectedDate = localSelectedDate,
                                    onDateSelected = { date -> 
                                        localSelectedDate = date
                                        isDateFiltered = true
                                        viewMode = "LIST"
                                    },
                                    tasksPerDate = taskCountsPerDate
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                // Date Filter Active Chip
                if (isDateFiltered && viewMode == "LIST") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { isDateFiltered = false },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = AppIcons.CalendarMonth),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Date: ${localSelectedDate.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    painter = painterResource(id = AppIcons.Close),
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        TextButton(onClick = { isDateFiltered = false }) {
                            Text("Show All Tasks", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Calendar Task List or Main List
                if (viewMode == "CALENDAR") {
                    val tasksForSelectedDate = tasks.filter { it.date == localSelectedDate }
                    if (tasksForSelectedDate.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tasks on ${localSelectedDate.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp)
                        ) {
                            items(tasksForSelectedDate, key = { it.id }) { task ->
                                StandaloneTaskItem(
                                    task = task,
                                    taskToDelete = taskToDelete,
                                    onEdit = { taskToEdit = task; showEditDialog = true },
                                    onToggle = { viewModel.updateTaskStatus(task, !task.isDone, task.date) },
                                    onDelete = { taskToDelete = task },
                                    isSelected = selectedIds.contains(task.id),
                                    isInSelectionMode = selectedIds.isNotEmpty(),
                                    onSelectionToggle = {
                                        selectedIds = if (selectedIds.contains(task.id)) selectedIds - task.id else selectedIds + task.id
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Normal List View
                    val filteredTasks = tasks.filter { task ->
                        (searchQuery.isEmpty() || task.title.contains(searchQuery, ignoreCase = true)) &&
                        (if (isDateFiltered) task.date == localSelectedDate else true) &&
                        when (filterType) {
                            "PENDING" -> !task.isDone
                            "DONE" -> task.isDone
                            else -> true
                        }
                    }

                    if (filteredTasks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No tasks for \"$searchQuery\"" 
                                       else if (isDateFiltered) "No tasks on ${localSelectedDate.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))}"
                                       else "No matching tasks",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {

                    val groupedTasks =
                        filteredTasks.groupBy { it.date ?: LocalDate.MIN }.toSortedMap()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                    ) {
                        groupedTasks.forEach { (date, tasksForDate) ->
                            item {
                                val headerText = when (date) {
                                    LocalDate.MIN -> "No Date"
                                    LocalDate.now() -> "Today"
                                    LocalDate.now().plusDays(1) -> "Tomorrow"
                                    else -> date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL))
                                }
                                Text(
                                    text = headerText,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                )
                            }
                            items(tasksForDate, key = { task -> "standalone_${task.id}" }) { task ->
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = true,
                                    enter = androidx.compose.animation.fadeIn() +
                                            androidx.compose.animation.slideInVertically(
                                                initialOffsetY = { it / 4 }
                                            )
                                ) {
                                    StandaloneTaskItem(
                                        task = task,
                                        taskToDelete = taskToDelete,
                                        onToggle = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.updateTaskStatus(
                                                task,
                                                !task.isDone,
                                                if (date == LocalDate.MIN) null else date
                                            )
                                        },
                                        onEdit = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            taskToEdit = task
                                            showEditDialog = true
                                        },
                                        onDelete = {
                                            taskToDelete = task
                                        },
                                        isSelected = task.id in selectedIds,
                                        isInSelectionMode = selectedIds.isNotEmpty(),
                                        onSelectionToggle = {
                                            selectedIds = if (task.id in selectedIds) {
                                                selectedIds - task.id
                                            } else {
                                                selectedIds + task.id
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
        }

        if (showEditDialog && taskToEdit != null) {
            val currentTask = taskToEdit!!
            StandaloneTaskDialog(
                dialogTitle = "Edit Task",
                onDismiss = { showEditDialog = false },
                onConfirm = { title, description, date, time, soundUri, shouldPlaySound, shouldVibrate ->
                    viewModel.updateStandaloneTask(
                        currentTask.copy(
                            title = title,
                            description = description,
                            date = date,
                            time = time,
                            customSoundUri = soundUri,
                            shouldPlaySound = shouldPlaySound,
                            shouldVibrate = shouldVibrate
                        )
                    )
                    showEditDialog = false
                },
                onDelete = {
                    taskToDelete = currentTask
                    showEditDialog = false
                },
                initialTitle = currentTask.title,
                initialDescription = currentTask.description,
                initialDate = currentTask.date,
                initialTime = currentTask.time,
                initialSoundUri = currentTask.customSoundUri,
                initialPlaySound = currentTask.shouldPlaySound,
                initialShouldVibrate = currentTask.shouldVibrate,
                isSaving = false,
                isNewTask = false
            )
        }

        if (taskToDelete != null) {
            AlertDialog(
                onDismissRequest = { taskToDelete = null },
                title = { Text("Delete Task?") },
                text = { Text("Are you sure you want to delete this task?") },
                confirmButton = {
                    Button(
                        onClick = {
                            taskToDelete?.let { viewModel.deleteStandaloneTask(it) }
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

        if (showBulkDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showBulkDeleteConfirm = false },
                title = { Text("Delete Selected Tasks?") },
                text = { Text("Are you sure you want to delete ${selectedIds.size} tasks?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val tasksToDelete = tasks.filter { it.id in selectedIds }
                            tasksToDelete.forEach { viewModel.deleteStandaloneTask(it) }
                            selectedIds = emptySet()
                            showBulkDeleteConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBulkDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandaloneTaskItem(
        task: StandaloneTask,
        taskToDelete: StandaloneTask?,
        onToggle: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        isSelected: Boolean = false,
        isInSelectionMode: Boolean = false,
        onSelectionToggle: () -> Unit = {}
    ) {
        val haptic = LocalHapticFeedback.current
        var showDescription by remember { mutableStateOf(false) }

        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = {
                when (it) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        onDelete()
                        false // Don't dismiss immediately, wait for dialog
                    }

                    SwipeToDismissBoxValue.StartToEnd -> {
                        onToggle()
                        false
                    }

                    else -> false
                }
            }
        )

        // Reset swipe if deletion cancelled
        LaunchedEffect(taskToDelete) {
            if (taskToDelete == null && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
        }

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                    }, label = "bgColor"
                )
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
                        .clip(RoundedCornerShape(20.dp))
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
                            onTap = {
                                if (isInSelectionMode) onSelectionToggle()
                                else showDescription = !showDescription
                            },
                            onLongPress = {
                                if (!isInSelectionMode) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelectionToggle()
                                }
                            }
                        )
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        task.isDone -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .alpha(if (task.isDone) 0.6f else 1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Use AnimatedCheckbox, but we need to handle click manually if it's just visual,
                        // or let Checkbox handle it.
                        // Since the row swipe also toggles, and tap toggles description,
                        // we want the checkbox to be clickable individually.
                        AnimatedCheckbox(
                            checked = task.isDone,
                            onCheckedChange = { onToggle() }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = if (task.isDone) FontWeight.Normal else FontWeight.Medium,
                                    textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (task.isDone) MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.8f
                                ) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (task.date != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = AppIcons.CalendarMonth),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = task.date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                
                                if (task.time != null) {
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    val timeText = com.dxtr.routini.utils.TimeUtils.formatTime(context, task.time)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = timeText,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showDescription) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp, end = 16.dp, bottom = 8.dp)
                        ) {
                            if (!task.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = onEdit,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 0.dp, horizontal = 16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = AppIcons.Edit),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit Task")
                            }
                        }
                    }
                }
            }
        }
    }
@Composable
private fun SlidingFilterToggle(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("ALL", "PENDING", "DONE")
    val labels = listOf("All", "Pending", "Done")
    val selectedIndex = filters.indexOf(selectedFilter)
    
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(4.dp)
    ) {
        // Sliding Background Pill
        val offset by animateDpAsState(
            targetValue = when (selectedIndex) {
                0 -> 0.dp
                1 -> 54.dp // Fine-tuned based on label width
                else -> 126.dp
            },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "pill_offset"
        )

        val pillWidth = when (selectedIndex) {
            0 -> 50.dp
            1 -> 68.dp
            else -> 58.dp
        }

        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(pillWidth)
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            filters.forEachIndexed { index, filter ->
                val isSelected = selectedFilter == filter
                
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onFilterSelected(filter) 
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labels[index],
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SlidingViewToggle(
    viewMode: String,
    onViewChange: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isList = viewMode == "LIST"
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(4.dp)
    ) {
        // Sliding Background Pill
        val offset by animateDpAsState(
            targetValue = if (isList) 0.dp else 40.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "view_pill_offset"
        )

        Box(
            modifier = Modifier
                .offset(x = offset)
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // List View Icon
            IconButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onViewChange("LIST") 
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = AppIcons.List),
                    contentDescription = "List View",
                    modifier = Modifier.size(16.dp),
                    tint = if (isList) MaterialTheme.colorScheme.onPrimary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Calendar View Icon
            IconButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onViewChange("CALENDAR") 
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(id = AppIcons.CalendarMonth),
                    contentDescription = "Calendar View",
                    modifier = Modifier.size(16.dp),
                    tint = if (!isList) MaterialTheme.colorScheme.onPrimary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
