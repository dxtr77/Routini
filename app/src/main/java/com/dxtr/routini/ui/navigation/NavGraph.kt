package com.dxtr.routini.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dxtr.routini.ui.screens.HomeScreen
import com.dxtr.routini.ui.screens.RoutineDetailScreen
import com.dxtr.routini.ui.screens.RoutinesScreen
import com.dxtr.routini.ui.screens.TasksScreen

@Composable
fun RoutiniNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Routines.route) {
            RoutinesScreen(navController = navController)
        }
        composable(Screen.Tasks.route) {
            TasksScreen()
        }
        composable(
            route = Screen.RoutineDetail.route,
            arguments = listOf(navArgument("routineId") { type = NavType.IntType })
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getInt("routineId")
            if (routineId != null) {
                RoutineDetailScreen(
                    routineId = routineId,
                    navController = navController
                )
            }
        }
    }
}