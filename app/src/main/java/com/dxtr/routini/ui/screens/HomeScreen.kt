package com.dxtr.routini.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dxtr.routini.MainViewModel
import com.dxtr.routini.R
import com.dxtr.routini.data.Routine
import com.dxtr.routini.data.StandaloneTask
import com.dxtr.routini.ui.composables.DonationDialog
import com.dxtr.routini.ui.composables.RoutineDialog
import com.dxtr.routini.ui.composables.StandaloneTaskDialog
import com.dxtr.routini.ui.navigation.Screen
import com.dxtr.routini.ui.theme.AppIcons
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    var showRoutineDialog by rememberSaveable { mutableStateOf(false) }
    var showTaskDialog by rememberSaveable { mutableStateOf(false) }
    // Routine is complex object, hard to save without custom saver or ID. 
    // For now keeping Routine as remember, minimizing risk. 
    // Ideally we save routine ID and fetch it, or make Routine Parcelable.
    var routineToEdit by remember { mutableStateOf<Routine?>(null) }
    
    val isSavingTask by viewModel.isSavingTask.collectAsState()
    val showAddTaskDialogTrigger by viewModel.showAddTaskDialog.collectAsState()
    val showDonationPrompt by viewModel.showDonationPrompt.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Handle trigger from ViewModel
    LaunchedEffect(showAddTaskDialogTrigger) {
        if (showAddTaskDialogTrigger) {
            showTaskDialog = true
            viewModel.triggerAddTaskDialog(false)
        }
    }

    // Effect to focus search
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        } else {
            viewModel.onSearchQueryChanged("")
        }
    }

    var showMenu by remember { mutableStateOf(false) }

    val exportAllLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportData(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importData(it) } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    AnimatedContent(targetState = isSearchActive, label = "TopBarTitle") { searching ->
                        if (searching) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .focusRequester(focusRequester),
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                "Search tasks...",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        } else {
                            Text("Routini", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                },
                navigationIcon = {
                     if (isSearchActive) {
                         IconButton(onClick = { isSearchActive = false }) {
                             Icon(painterResource(AppIcons.ArrowBack), contentDescription = "Back")
                         }
                     }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(painter = painterResource(id = AppIcons.Search), contentDescription = "Search")
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(painter = painterResource(id = AppIcons.MoreVert), contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export All Data") },
                                onClick = {
                                    showMenu = false
                                    exportAllLauncher.launch("routini_backup_${LocalDate.now()}.json")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Data") },
                                onClick = {
                                    showMenu = false
                                    importLauncher.launch(arrayOf("application/json"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                        }
                    } else {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(painter = painterResource(id = AppIcons.Close), contentDescription = "Clear")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                val screens = listOf("Dashboard", "Routines", "Tasks")
                val icons = listOf(AppIcons.Schedule, AppIcons.List, AppIcons.CheckCircle)
                
                screens.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        icon = { 
                            Icon(
                                painter = painterResource(id = icons[index]), 
                                contentDescription = title
                            ) 
                        },
                        label = { Text(title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            ExpandableFab(
                onAddTask = { showTaskDialog = true },
                onAddRoutine = {
                    routineToEdit = null
                    showRoutineDialog = true
                }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> DashboardScreen(viewModel = viewModel)
                1 -> RoutinesScreen(
                    navController = navController,
                    viewModel = viewModel,
                    showRoutineDialog = showRoutineDialog,
                    onDismissRoutineDialog = { showRoutineDialog = false },
                    onEditRoutine = {
                        routineToEdit = it
                        showRoutineDialog = true
                    },
                    onAddRoutine = {
                        routineToEdit = null
                        showRoutineDialog = true
                    }
                )
                2 -> TasksScreen(viewModel = viewModel, onAddTask = { showTaskDialog = true })
            }
        }

        // Dialogs
        if (showRoutineDialog) {
            RoutineDialog(
                routine = routineToEdit,
                onConfirm = { name, days ->
                    val defaultColor = 0xFF6200EE.toInt()
                    if (routineToEdit == null) {
                        viewModel.addRoutine(Routine(name = name, themeColor = defaultColor, recurringDays = days))
                    } else {
                        viewModel.updateRoutine(routineToEdit!!.copy(name = name, recurringDays = days, themeColor = defaultColor))
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

        if (showTaskDialog) {
            StandaloneTaskDialog(
                dialogTitle = stringResource(R.string.new_task_title),
                onDismiss = { showTaskDialog = false },
                onConfirm = { title, desc, date, time, sound, playSound, shouldVibrate ->
                    viewModel.addStandaloneTask(
                        StandaloneTask(
                            title = title,
                            description = desc,
                            date = date,
                            time = time,
                            customSoundUri = sound,
                            shouldPlaySound = playSound,
                            shouldVibrate = shouldVibrate
                        ))
                    showTaskDialog = false
                },
                initialTitle = "",
                initialDescription = null,
                initialDate = LocalDate.now(),
                initialTime = null,
                initialSoundUri = null,
                initialPlaySound = false,
                initialShouldVibrate = false,
                isSaving = isSavingTask,
                onDelete = null,
                isNewTask = true
            )
        }

        if (showDonationPrompt) {
            DonationDialog(onDismiss = { viewModel.dismissDonationPrompt() })
        }
    }
}

@Composable
fun ExpandableFab(
    onAddTask: () -> Unit,
    onAddRoutine: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "fab_rotation"
    )
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FabItem(text = "Add Routine", icon = AppIcons.List, onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onAddRoutine()
                    expanded = false
                })
                FabItem(text = "Add Task", icon = AppIcons.CheckCircle, onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onAddTask()
                    expanded = false
                })
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        FloatingActionButton(
            onClick = { 
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                expanded = !expanded 
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                painter = painterResource(id = AppIcons.Add),
                contentDescription = "Add",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
fun FabItem(text: String, icon: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.width(16.dp))
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(painter = painterResource(id = icon), contentDescription = null)
        }
    }
}
