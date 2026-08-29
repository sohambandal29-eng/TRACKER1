package com.example.tracker.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object Tasks : Screen("tasks")
    object Timer : Screen("timer")
    object Analytics : Screen("analytics")
    object Roadmap : Screen("roadmap")
    object Timetable : Screen("timetable")
    object Groups : Screen("groups")
    object Blocks : Screen("blocks")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object Admin : Screen("admin")
}
