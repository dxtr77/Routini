package com.dxtr.routini.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dxtr.routini.MainViewModel
import com.dxtr.routini.R
import com.dxtr.routini.data.Routine
import com.dxtr.routini.data.StandaloneTask
import com.dxtr.routini.ui.composables.StandaloneTaskDialog
import com.dxtr.routini.ui.theme.AppIcons
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    var showRoutineDialog by remember { mutableStateOf(false) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<Routine?>(null) }
    val isSavingTask by viewModel.isSavingTask.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    icon = { Icon(painter = painterResource(id = AppIcons.Schedule), contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    icon = { Icon(painter = painterResource(id = AppIcons.List), contentDescription = "Routines") },
                    label = { Text("Routines") }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
                    icon = { Icon(painter = painterResource(id = AppIcons.CheckCircle), contentDescription = "Tasks") },
                    label = { Text("Tasks") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                when (pagerState.currentPage) {
                    0 -> showTaskDialog = true
                    1 -> {
                        routineToEdit = null
                        showRoutineDialog = true
                    }
                    2 -> showTaskDialog = true
                }
            }) {
                Icon(painter = painterResource(id = AppIcons.Add), contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding)
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
                        )
                    )
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
    }
}
