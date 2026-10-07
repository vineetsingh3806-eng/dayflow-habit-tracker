package com.aistudio.dayflow.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Habits : Screen("habits")
    object Goals : Screen("goals")
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")

    object CreateHabit : Screen("habit/create")
    object EditHabit : Screen("habit/edit/{habitId}") {
        fun createRoute(habitId: Long) = "habit/edit/$habitId"
    }
    object HabitDetail : Screen("habit/detail/{habitId}") {
        fun createRoute(habitId: Long) = "habit/detail/$habitId"
    }
    object ArchivedHabits : Screen("habits/archived")

    object CreateGoal : Screen("goal/create")
    object EditGoal : Screen("goal/edit/{goalId}") {
        fun createRoute(goalId: Long) = "goal/edit/$goalId"
    }
    object GoalDetail : Screen("goal/detail/{goalId}") {
        fun createRoute(goalId: Long) = "goal/detail/$goalId"
    }
    object ArchivedGoals : Screen("goals/archived")
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        route = Screen.Habits.route,
        title = "Habits",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle
    ),
    BottomNavItem(
        route = Screen.Goals.route,
        title = "Goals",
        selectedIcon = Icons.Filled.Flag,
        unselectedIcon = Icons.Outlined.Flag
    ),
    BottomNavItem(
        route = Screen.Statistics.route,
        title = "Stats",
        selectedIcon = Icons.Filled.DateRange,
        unselectedIcon = Icons.Outlined.DateRange
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)
