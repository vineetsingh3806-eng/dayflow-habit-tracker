package com.aistudio.dayflow.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.Goal
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.HabitCompletion
import com.aistudio.dayflow.app.domain.model.WeeklyDayItem
import com.aistudio.dayflow.app.domain.model.WeeklyDayStatus
import com.aistudio.dayflow.app.domain.usecase.GetActiveGoalsUseCase
import com.aistudio.dayflow.app.domain.usecase.GetActiveHabitsUseCase
import com.aistudio.dayflow.app.domain.usecase.GetCompletionsForDateUseCase
import com.aistudio.dayflow.app.domain.usecase.GoalProgressCalculator
import com.aistudio.dayflow.app.domain.usecase.StreakCalculator
import com.aistudio.dayflow.app.domain.usecase.StreakInfo
import com.aistudio.dayflow.app.domain.usecase.ToggleHabitCompletionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class HabitHomeItem(
    val habit: Habit,
    val isCompleted: Boolean,
    val streakInfo: StreakInfo
)

data class GoalAttentionItem(
    val goal: Goal,
    val deadlineDescription: String,
    val progressPercent: Int
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val currentDate: LocalDate = LocalDate.now(),
    val habits: List<HabitHomeItem> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val completionPercentage: Int = 0,
    val maxStreak: Int = 0,
    val bestOverallStreak: Int = 0,
    val weeklyDayItems: List<WeeklyDayItem> = emptyList(),
    val weeklyCompletionSummary: String = "",
    val goalAttentions: List<GoalAttentionItem> = emptyList()
)

class HomeViewModel(
    private val getActiveHabitsUseCase: GetActiveHabitsUseCase,
    private val getCompletionsForDateUseCase: GetCompletionsForDateUseCase,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    private val getActiveGoalsUseCase: GetActiveGoalsUseCase,
    private val habitRepository: com.aistudio.dayflow.app.domain.repository.HabitRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        getActiveHabitsUseCase(),
        getActiveGoalsUseCase(),
        habitRepository.getAllCompletions()
    ) { activeHabits, activeGoals, allCompletions ->
        val currentToday = LocalDate.now()
        val completionsByHabit = allCompletions.groupBy { it.habitId }
        val todayCompletedIds = allCompletions
            .filter { it.date == currentToday && it.status == CompletionStatus.COMPLETED }
            .map { it.habitId }
            .toSet()

        // Filter habits due today
        val habitsDueToday = activeHabits.filter { habit ->
            StreakCalculator.isHabitScheduledOnDate(habit, currentToday)
        }

        val habitItems = habitsDueToday.map { habit ->
            val habitHistory = completionsByHabit[habit.id] ?: emptyList()
            val streak = StreakCalculator.calculateStreak(habit, habitHistory, currentToday)
            HabitHomeItem(
                habit = habit,
                isCompleted = todayCompletedIds.contains(habit.id),
                streakInfo = streak
            )
        }

        val completedCount = habitItems.count { it.isCompleted }
        val totalCount = habitItems.size
        val percentage = if (totalCount > 0) ((completedCount.toFloat() / totalCount.toFloat()) * 100).toInt() else 0
        val maxStreak = habitItems.maxOfOrNull { it.streakInfo.currentStreak } ?: 0
        val bestOverallStreak = activeHabits.maxOfOrNull { habit ->
            val habitHistory = completionsByHabit[habit.id] ?: emptyList()
            StreakCalculator.calculateStreak(habit, habitHistory, currentToday).bestStreak
        } ?: 0

        // Weekly activity indicators for current week (Monday to Sunday)
        val currentWeekStart = currentToday.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDates = (0..6).map { currentWeekStart.plusDays(it.toLong()) }
        var weeklyScheduledDays = 0
        var weeklyCompletedDays = 0

        val weeklyDayItems = weekDates.map { date ->
            val isToday = date == currentToday
            val isFuture = date.isAfter(currentToday)
            val scheduledCount = activeHabits.count { StreakCalculator.isHabitScheduledOnDate(it, date) }
            val completedCountOnDay = allCompletions.count { it.date == date && it.status == CompletionStatus.COMPLETED }

            if (!isFuture && scheduledCount > 0) {
                weeklyScheduledDays++
                if (completedCountOnDay > 0) {
                    weeklyCompletedDays++
                }
            }

            val status = when {
                isFuture -> WeeklyDayStatus.UPCOMING
                scheduledCount == 0 -> WeeklyDayStatus.REST_DAY
                completedCountOnDay >= scheduledCount -> WeeklyDayStatus.COMPLETED
                else -> WeeklyDayStatus.MISSED
            }

            WeeklyDayItem(
                dayName = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                date = date,
                status = status,
                completedCount = completedCountOnDay,
                scheduledCount = scheduledCount,
                isToday = isToday
            )
        }

        val weeklyRate = if (weeklyScheduledDays > 0) ((weeklyCompletedDays.toFloat() / weeklyScheduledDays.toFloat()) * 100).toInt() else 0
        val weeklySummary = "Completion: $weeklyCompletedDays/$weeklyScheduledDays — $weeklyRate%"

        // Goal attention: Goals that have targetDate within 14 days or overdue
        val goalAttentions = activeGoals
            .filter { it.targetDate != null && it.progress < 1.0f }
            .sortedBy { it.targetDate }
            .take(2)
            .map { goal ->
                GoalAttentionItem(
                    goal = goal,
                    deadlineDescription = GoalProgressCalculator.getDeadlineDescription(goal, currentToday),
                    progressPercent = (goal.progress * 100).toInt()
                )
            }

        HomeUiState(
            isLoading = false,
            currentDate = currentToday,
            habits = habitItems,
            completedCount = completedCount,
            totalCount = totalCount,
            completionPercentage = percentage,
            maxStreak = maxStreak,
            bestOverallStreak = bestOverallStreak,
            weeklyDayItems = weeklyDayItems,
            weeklyCompletionSummary = weeklySummary,
            goalAttentions = goalAttentions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun toggleHabit(habitId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            toggleHabitCompletionUseCase(
                habitId = habitId,
                date = LocalDate.now(),
                isCurrentlyCompleted = isCompleted
            )
        }
    }
}
