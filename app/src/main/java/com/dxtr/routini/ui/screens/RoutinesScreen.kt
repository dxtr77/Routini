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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.dxtr.routini.ui.composables.CircularProgressRing
import com.dxtr.routini.ui.composables.EmptyState
import com.dxtr.routini.ui.composables.GlassCard
import com.dxtr.routini.ui.navigation.Screen
import com.dxtr.routini.ui.theme.AppIcons
import com.dxtr.routini.utils.reorderable
import com.dxtr.routini.utils.reorderableItem
import com.dxtr.routini.utils.draggableHandle
import java.time.DayOfWeek

@Composable
fun RoutinesScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel(),
    showRoutineDialog: Boolean,
    onDismissRoutineDialog: () -> Unit,
    onEditRoutine: (Routine) -> Unit,
    onAddRoutine: () -> Unit
) {
    val routines by viewModel.filteredRoutines.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val haptic = LocalHapticFeedback.current
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

    var routineToDelete by remember { mutableStateOf<Routine?>(null) }

    var routineToExport by remember { mutableStateOf<Routine?>(null) }
    val exportRoutineLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { 
            routineToExport?.let { routine ->
                viewModel.exportRoutine(routine.id, it) 
            }
        }
    }

    if (routines.isEmpty() && searchQuery.isEmpty()) {
        EmptyState(
            message = "No routines yet. Tap '+' to create one!",
            icon = AppIcons.List,
            actionLabel = "Create Routine",
            onActionClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAddRoutine()
            }
        )
    } else {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp)
        ) { padding ->

                
                // Reorder State
                val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
                val reorderState = com.dxtr.routini.utils.rememberReorderableLazyListState(
                    lazyListState = lazyListState,
                    onMove = { from, to ->
                        if (searchQuery.isEmpty()) {
                            // Adjust index for headers if any. Here we have "My Routines" header at index 0 and maybe empty search results.
                            // The itemsIndexed starts after the header. "My Routines" is item 0.
                            // So item at index X in list corresponds to index X-1 in routines list?
                            // Let's check layout.
                            
                            // itemsIndexed is called. LazyColumn has:
                            // item { header } -> index 0
                            // itemsIndexed -> index 1..N
                            
                            // onMove gives raw indices in the LazyColumn.
                            // if from is 1, it corresponds to routine at index 0.
                            
                            val headerCount = 1 // "My Routines"
                            val realFrom = from - headerCount
                            val realTo = to - headerCount
                            
                            if (realFrom >= 0 && realTo >= 0 && realFrom < routines.size && realTo < routines.size) {
                                viewModel.reorderRoutines(realFrom, realTo)
                            }
                        }
                    }
                )

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 16.dp,
                        bottom = 100.dp
                    )
                ) {
                    item {
                        Text(
                            text = "My Routines",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (routines.isEmpty() && searchQuery.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No routines found for \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    itemsIndexed(items = routines, key = { _, it -> it.id }) { index, routine ->
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = androidx.compose.animation.fadeIn() + 
                                    androidx.compose.animation.slideInVertically(
                                        initialOffsetY = { it / 4 },
                                        animationSpec = androidx.compose.animation.core.spring(
                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                                        )
                                    ),
                             modifier = Modifier.then(
                                 if (searchQuery.isEmpty()) Modifier.reorderableItem(reorderState, routine.id) else Modifier
                             )
                        ) {
                            ModernRoutineCard(
                                routine = routine,
                                viewModel = viewModel,
                                onNavigateToDetail = {
                                    navController.navigate(Screen.RoutineDetail.createRoute(routine.id))
                                },
                                onEdit = { onEditRoutine(routine) },
                                onDelete = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    routineToDelete = routine 
                                },
                                onExport = {
                                     routineToExport = routine
                                     exportRoutineLauncher.launch("${routine.name.replace(" ", "_").lowercase()}.json")
                                },
                                onMoveUp = { viewModel.moveRoutineUp(routine) },
                                onMoveDown = { viewModel.moveRoutineDown(routine) },
                                canMoveUp = index > 0,
                                canMoveDown = index < routines.size - 1,
                                dragHandle = if (searchQuery.isEmpty()) {
                                    {
                                        Icon(
                                            painter = painterResource(id = AppIcons.DragHandle),
                                            contentDescription = "Drag to reorder",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .draggableHandle(reorderState, index + 1), // +1 for header offset
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
        }
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

    if (routineToDelete != null) {
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text("Delete Routine?") },
            text = { Text("Are you sure you want to delete '${routineToDelete?.name}'? All history for this routine will also be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        routineToDelete?.let { viewModel.deleteRoutine(it) }
                        routineToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
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
        else -> activeDays.joinToString { it -> it.name.take(3).lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
    }
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernRoutineCard(
    routine: Routine,
    viewModel: MainViewModel,
    onNavigateToDetail: () -> Unit, 
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    dragHandle: @Composable (() -> Unit)? = null
) {
    val tasks by viewModel.getTasksForRoutine(routine.id).collectAsState(initial = emptyList())
    val today = java.time.LocalDate.now()
    val currentDayOfWeek = today.dayOfWeek
    val relevantTasks = tasks.filter { task ->
        val days = task.specificDays
        if (!days.isNullOrEmpty()) {
            days.contains(currentDayOfWeek)
        } else {
            routine.recurringDays.contains(currentDayOfWeek)
        }
    }
    val completedTasks = relevantTasks.count { it.isDone }
    val totalTasks = relevantTasks.size
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    
    var showMenu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dragHandle != null) {
            dragHandle()
            Spacer(modifier = Modifier.width(8.dp))
        }

        GlassCard(
            onClick = onNavigateToDetail,
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.animateContentSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routine.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SmartDayIndicator(routine.recurringDays)
                    
                    if (totalTasks > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "$completedTasks/$totalTasks tasks completed today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                     Icon(
                         painter = painterResource(id = if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore),
                         contentDescription = if (expanded) "Collapse" else "Expand",
                         tint = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                }

                if (totalTasks > 0) {
                    CircularProgressRing(
                        progress = progress,
                        size = 50.dp,
                        strokeWidth = 5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                 }
                 
                 Box {
                     IconButton(onClick = { showMenu = true }) {
                         Icon(
                             painter = painterResource(id = AppIcons.MoreVert), 
                             contentDescription = "More Options",
                             tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                         )
                     }
                     DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (canMoveUp) {
                             DropdownMenuItem(
                                text = { Text("Move Up") },
                                onClick = { 
                                    onMoveUp()
                                    showMenu = false 
                                },
                                leadingIcon = { Icon(painterResource(id = AppIcons.KeyboardArrowUp), null) }
                            )
                        }
                        if (canMoveDown) {
                             DropdownMenuItem(
                                text = { Text("Move Down") },
                                onClick = { 
                                    onMoveDown()
                                    showMenu = false 
                                },
                                leadingIcon = { Icon(painterResource(id = AppIcons.KeyboardArrowDown), null) }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { 
                                onEdit()
                                showMenu = false 
                            },
                            leadingIcon = { Icon(painterResource(id = AppIcons.Edit), contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export") },
                            onClick = {
                                onExport()
                                showMenu = false
                            },
                            leadingIcon = { Icon(painterResource(id = AppIcons.ArrowForward), contentDescription = null) }
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
            
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (tasks.isEmpty()) {
                     Text("No tasks configured.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp))
                } else {
                     Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                         tasks.forEach { task ->
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                 Icon(painterResource(AppIcons.TaskAlt), null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                 Spacer(Modifier.width(8.dp))
                                 Text(task.title, style = MaterialTheme.typography.bodyMedium)
                                 Spacer(Modifier.weight(1f))
                                 if (task.time != null) {
                                     Text(task.time.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                 }
                             }
                         }
                     }
                }
                 }
            }
        }
    }
}
// RoutineDialog remains largely similar, just reused if possible or kept here.
// I'll keep it here as provided in previous code but with updated imports if any.
// ... (Including RoutineDialog and RoutineDialogContent)

