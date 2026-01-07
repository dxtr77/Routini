package com.dxtr.routini.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dxtr.routini.ui.screens.HomeScreen
import com.dxtr.routini.ui.screens.RoutinesScreen
import com.dxtr.routini.ui.screens.TasksScreen

@Composable
fun RoutiniNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Routines.route) {
            RoutinesScreen(navController = navController)
        }
        composable(Screen.Tasks.route) {
            TasksScreen(navController = navController)
        }
        composable(Screen.RoutineDetail.route) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString("routineId")?.toIntOrNull()
            // Placeholder for Routine Detail
        }
    }
}