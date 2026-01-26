package com.dxtr.routini.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Routines : Screen("routines")
    object Tasks : Screen("tasks")
    object RoutineDetail : Screen("routine_detail/{routineId}") {
        fun createRoute(routineId: Int) = "routine_detail/$routineId"
    }
    object Permissions : Screen("permissions")
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Settings : Screen("settings")
}