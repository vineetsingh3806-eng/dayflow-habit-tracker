package com.aistudio.dayflow.app.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aistudio.dayflow.app.di.AppContainer
import com.aistudio.dayflow.app.domain.model.AppSettings
import com.aistudio.dayflow.app.presentation.goals.CreateEditGoalScreen
import com.aistudio.dayflow.app.presentation.goals.GoalDetailScreen
import com.aistudio.dayflow.app.presentation.goals.GoalsScreen
import com.aistudio.dayflow.app.presentation.goals.GoalsViewModel
import com.aistudio.dayflow.app.presentation.habits.ArchivedHabitsScreen
import com.aistudio.dayflow.app.presentation.habits.CreateEditHabitScreen
import com.aistudio.dayflow.app.presentation.habits.HabitDetailScreen
import com.aistudio.dayflow.app.presentation.habits.HabitsScreen
import com.aistudio.dayflow.app.presentation.habits.HabitsViewModel
import com.aistudio.dayflow.app.presentation.home.HomeScreen
import com.aistudio.dayflow.app.presentation.home.HomeViewModel
import com.aistudio.dayflow.app.presentation.onboarding.OnboardingScreen
import com.aistudio.dayflow.app.presentation.onboarding.OnboardingViewModel
import com.aistudio.dayflow.app.presentation.settings.SettingsScreen
import com.aistudio.dayflow.app.presentation.settings.SettingsViewModel
import com.aistudio.dayflow.app.presentation.statistics.StatisticsScreen
import com.aistudio.dayflow.app.presentation.statistics.StatisticsViewModel

@Composable
fun MainScaffold(
    container: AppContainer,
    initialHabitId: Long? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val appSettings by container.settingsRepository.getSettings().collectAsStateWithLifecycle(initialValue = AppSettings())

    LaunchedEffect(initialHabitId) {
        if (initialHabitId != null && initialHabitId > 0L) {
            navController.navigate(Screen.HabitDetail.createRoute(initialHabitId))
        }
    }

    val topLevelRoutes = listOf(
        Screen.Home.route,
        Screen.Habits.route,
        Screen.Goals.route,
        Screen.Statistics.route,
        Screen.Settings.route
    )
    val shouldShowBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(text = item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val startDestination = if (appSettings.onboardingCompleted) Screen.Home.route else Screen.Onboarding.route

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (shouldShowBottomBar) padding.calculateBottomPadding() else padding.calculateBottomPadding())
        ) {
            // Onboarding
            composable(Screen.Onboarding.route) {
                val vm = remember {
                    OnboardingViewModel(
                        setOnboardingCompletedUseCase = container.setOnboardingCompletedUseCase,
                        createHabitUseCase = container.createHabitUseCase
                    )
                }
                OnboardingScreen(
                    viewModel = vm,
                    onFinish = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // Home
            composable(Screen.Home.route) {
                val vm = remember {
                    HomeViewModel(
                        container.getActiveHabitsUseCase,
                        container.getCompletionsForDateUseCase,
                        container.toggleHabitCompletionUseCase,
                        container.getActiveGoalsUseCase,
                        container.habitRepository
                    )
                }
                HomeScreen(
                    viewModel = vm,
                    onNavigateToCreateHabit = { navController.navigate(Screen.CreateHabit.route) },
                    onNavigateToHabitDetail = { habitId ->
                        navController.navigate(Screen.HabitDetail.createRoute(habitId))
                    },
                    onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                    onNavigateToGoalDetail = { goalId ->
                        navController.navigate(Screen.GoalDetail.createRoute(goalId))
                    }
                )
            }

            // Habits Tab
            composable(Screen.Habits.route) {
                val vm = remember {
                    HabitsViewModel(
                        container.getActiveHabitsUseCase,
                        container.getArchivedHabitsUseCase,
                        container.createHabitUseCase,
                        container.updateHabitUseCase,
                        container.deleteHabitUseCase,
                        container.archiveHabitUseCase,
                        container.restoreHabitUseCase,
                        container.toggleHabitCompletionUseCase,
                        container.habitRepository
                    )
                }
                HabitsScreen(
                    viewModel = vm,
                    onNavigateToCreateHabit = { navController.navigate(Screen.CreateHabit.route) },
                    onNavigateToHabitDetail = { habitId ->
                        navController.navigate(Screen.HabitDetail.createRoute(habitId))
                    },
                    onNavigateToArchivedHabits = { navController.navigate(Screen.ArchivedHabits.route) }
                )
            }

            // Create Habit
            composable(Screen.CreateHabit.route) {
                CreateEditHabitScreen(
                    habitId = null,
                    getHabitByIdUseCase = container.getHabitByIdUseCase,
                    createHabitUseCase = container.createHabitUseCase,
                    updateHabitUseCase = container.updateHabitUseCase,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Edit Habit
            composable(
                route = Screen.EditHabit.route,
                arguments = listOf(navArgument("habitId") { type = NavType.LongType })
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getLong("habitId") ?: 0L
                CreateEditHabitScreen(
                    habitId = habitId,
                    getHabitByIdUseCase = container.getHabitByIdUseCase,
                    createHabitUseCase = container.createHabitUseCase,
                    updateHabitUseCase = container.updateHabitUseCase,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Habit Detail
            composable(
                route = Screen.HabitDetail.route,
                arguments = listOf(navArgument("habitId") { type = NavType.LongType })
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getLong("habitId") ?: 0L
                HabitDetailScreen(
                    habitId = habitId,
                    getHabitByIdUseCase = container.getHabitByIdUseCase,
                    getHabitCompletionsUseCase = container.getHabitCompletionsUseCase,
                    toggleHabitCompletionUseCase = container.toggleHabitCompletionUseCase,
                    archiveHabitUseCase = container.archiveHabitUseCase,
                    restoreHabitUseCase = container.restoreHabitUseCase,
                    deleteHabitUseCase = container.deleteHabitUseCase,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(Screen.EditHabit.createRoute(id)) }
                )
            }

            // Archived Habits
            composable(Screen.ArchivedHabits.route) {
                ArchivedHabitsScreen(
                    getArchivedHabitsUseCase = container.getArchivedHabitsUseCase,
                    restoreHabitUseCase = container.restoreHabitUseCase,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Goals Tab
            composable(Screen.Goals.route) {
                val vm = remember {
                    GoalsViewModel(
                        container.getActiveGoalsUseCase,
                        container.goalRepository
                    )
                }
                GoalsScreen(
                    viewModel = vm,
                    onNavigateToCreateGoal = { navController.navigate(Screen.CreateGoal.route) },
                    onNavigateToGoalDetail = { goalId ->
                        navController.navigate(Screen.GoalDetail.createRoute(goalId))
                    }
                )
            }

            // Create Goal
            composable(Screen.CreateGoal.route) {
                CreateEditGoalScreen(
                    goalId = null,
                    goalRepository = container.goalRepository,
                    createGoalUseCase = container.createGoalUseCase,
                    updateGoalUseCase = container.updateGoalUseCase,
                    createMilestoneUseCase = container.createMilestoneUseCase,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Edit Goal
            composable(
                route = Screen.EditGoal.route,
                arguments = listOf(navArgument("goalId") { type = NavType.LongType })
            ) { backStackEntry ->
                val goalId = backStackEntry.arguments?.getLong("goalId") ?: 0L
                CreateEditGoalScreen(
                    goalId = goalId,
                    goalRepository = container.goalRepository,
                    createGoalUseCase = container.createGoalUseCase,
                    updateGoalUseCase = container.updateGoalUseCase,
                    createMilestoneUseCase = container.createMilestoneUseCase,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Goal Detail
            composable(
                route = Screen.GoalDetail.route,
                arguments = listOf(navArgument("goalId") { type = NavType.LongType })
            ) { backStackEntry ->
                val goalId = backStackEntry.arguments?.getLong("goalId") ?: 0L
                GoalDetailScreen(
                    goalId = goalId,
                    getGoalWithDetailsUseCase = container.getGoalWithDetailsUseCase,
                    toggleMilestoneCompletionUseCase = container.toggleMilestoneCompletionUseCase,
                    createMilestoneUseCase = container.createMilestoneUseCase,
                    deleteMilestoneUseCase = container.deleteMilestoneUseCase,
                    linkHabitToMilestoneUseCase = container.linkHabitToMilestoneUseCase,
                    unlinkHabitFromMilestoneUseCase = container.unlinkHabitFromMilestoneUseCase,
                    deleteGoalUseCase = container.deleteGoalUseCase,
                    archiveGoalUseCase = container.archiveGoalUseCase,
                    goalRepository = container.goalRepository,
                    habitRepository = container.habitRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(Screen.EditGoal.createRoute(id)) }
                )
            }

            // Statistics Tab
            composable(Screen.Statistics.route) {
                val vm = remember {
                    StatisticsViewModel(
                        container.getActiveHabitsUseCase,
                        container.habitRepository,
                        container.getAchievementsUseCase
                    )
                }
                StatisticsScreen(viewModel = vm)
            }

            // Settings Tab
            composable(Screen.Settings.route) {
                val vm = remember {
                    SettingsViewModel(
                        container.settingsRepository
                    )
                }
                SettingsScreen(viewModel = vm)
            }
        }
    }
}
