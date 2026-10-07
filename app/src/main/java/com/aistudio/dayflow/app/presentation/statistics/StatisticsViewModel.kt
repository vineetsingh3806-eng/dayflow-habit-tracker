package com.aistudio.dayflow.app.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.dayflow.app.domain.model.Achievement
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.DayHeatmapCell
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.WeeklyDayItem
import com.aistudio.dayflow.app.domain.model.WeeklyDayStatus
import com.aistudio.dayflow.app.domain.repository.HabitRepository
import com.aistudio.dayflow.app.domain.usecase.GetAchievementsUseCase
import com.aistudio.dayflow.app.domain.usecase.GetActiveHabitsUseCase
import com.aistudio.dayflow.app.domain.usecase.StreakCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class StatisticsTab {
    PROGRESS,
    ACHIEVEMENTS
}

data class HabitConsistencyItem(
    val habit: Habit,
    val consistencyRate: Float, // 0f to 1f
    val currentStreak: Int,
    val totalCompletions: Int
)

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val selectedTab: StatisticsTab = StatisticsTab.PROGRESS,
    // Today
    val todayScheduledCount: Int = 0,
    val todayCompletedCount: Int = 0,
    val todayCompletionRate: Int = 0,
    // Weekly
    val weeklyCompletionRate: Int = 0,
    val weeklyCompletedDays: Int = 0,
    val weeklyScheduledDays: Int = 0,
    val weeklyDayItems: List<WeeklyDayItem> = emptyList(),
    val weeklySummaryText: String = "",
    // Monthly
    val monthlyCompletionRate: Int = 0,
    val monthlyCompletedCount: Int = 0,
    // Overall Stats
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val overallConsistencyRate: Int = 0,
    val bestDay: String? = null,
    val actionableInsights: List<String> = emptyList(),
    val trendSummary: String = "",
    val heatmapDays: List<DayHeatmapCell> = emptyList(),
    val habitConsistencies: List<HabitConsistencyItem> = emptyList(),
    // Achievements
    val achievements: List<Achievement> = emptyList(),
    val unlockedCount: Int = 0,
    val totalAchievementsCount: Int = 0
)

class StatisticsViewModel(
    private val getActiveHabitsUseCase: GetActiveHabitsUseCase,
    private val habitRepository: HabitRepository,
    private val getAchievementsUseCase: GetAchievementsUseCase? = null
) : ViewModel() {

    private val today = LocalDate.now()
    private val _selectedTab = MutableStateFlow(StatisticsTab.PROGRESS)

    fun selectTab(tab: StatisticsTab) {
        _selectedTab.value = tab
    }

    val uiState: StateFlow<StatisticsUiState> = combine(
        getActiveHabitsUseCase(),
        habitRepository.getAllCompletions(),
        _selectedTab,
        getAchievementsUseCase?.invoke(today) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    ) { habits, allCompletions, tab, streamAchievements ->
        val completedCompletions = allCompletions.filter { it.status == CompletionStatus.COMPLETED }
        val completionsByDate = completedCompletions.groupBy { it.date }
        val completionsByHabit = completedCompletions.groupBy { it.habitId }

        // 1. Today's Progress
        val todayScheduled = habits.count { StreakCalculator.isHabitScheduledOnDate(it, today) }
        val todayCompleted = completionsByDate[today]?.size ?: 0
        val todayRate = if (todayScheduled > 0) {
            ((todayCompleted.toFloat() / todayScheduled.toFloat()) * 100).toInt().coerceAtMost(100)
        } else {
            if (todayCompleted > 0) 100 else 0
        }

        // 2. Weekly Progress (Monday to Sunday of current week)
        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDates = (0..6).map { currentWeekStart.plusDays(it.toLong()) }
        var weeklyScheduledDays = 0
        var weeklyCompletedDays = 0

        val weeklyDayItems = weekDates.map { date ->
            val isToday = (date == today)
            val isFuture = date.isAfter(today)
            val scheduledCount = habits.count { StreakCalculator.isHabitScheduledOnDate(it, date) }
            val completedCount = completionsByDate[date]?.size ?: 0

            if (!isFuture && scheduledCount > 0) {
                weeklyScheduledDays++
                if (completedCount > 0) {
                    weeklyCompletedDays++
                }
            }

            val status = when {
                isFuture -> WeeklyDayStatus.UPCOMING
                scheduledCount == 0 -> WeeklyDayStatus.REST_DAY
                completedCount >= scheduledCount -> WeeklyDayStatus.COMPLETED
                else -> WeeklyDayStatus.MISSED
            }

            WeeklyDayItem(
                dayName = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                date = date,
                status = status,
                completedCount = completedCount,
                scheduledCount = scheduledCount,
                isToday = isToday
            )
        }

        val weeklyRate = if (weeklyScheduledDays > 0) {
            ((weeklyCompletedDays.toFloat() / weeklyScheduledDays.toFloat()) * 100).toInt()
        } else {
            0
        }
        val weeklySummary = "Completion: $weeklyCompletedDays/$weeklyScheduledDays — $weeklyRate%"

        // Prior 7 days for trend calculation
        val prior7Days = (7..13).map { today.minusDays(it.toLong()) }
        var scheduledDaysPrior = 0
        var completedDaysPrior = 0
        for (day in prior7Days) {
            for (habit in habits) {
                if (StreakCalculator.isHabitScheduledOnDate(habit, day)) {
                    scheduledDaysPrior++
                    if (completionsByHabit[habit.id]?.any { it.date == day } == true) {
                        completedDaysPrior++
                    }
                }
            }
        }
        val priorRate = if (scheduledDaysPrior > 0) ((completedDaysPrior.toFloat() / scheduledDaysPrior) * 100).toInt() else 0

        val trendSummary = when {
            habits.isEmpty() -> "Create habits to start tracking consistency trends."
            weeklyCompletedDays == 0 && completedDaysPrior == 0 -> "Complete today's habits to start a streak."
            weeklyRate > priorRate -> "Consistency is up by ${weeklyRate - priorRate}% compared to last week."
            weeklyRate < priorRate -> "${weeklyRate}% completed this week. Keep showing up."
            else -> "Steady pace matching last week's consistency."
        }

        // 3. Monthly Rate (last 30 days)
        val last30Days = (0..29).map { today.minusDays(it.toLong()) }
        var scheduledDays30 = 0
        var completedDays30 = 0
        for (day in last30Days) {
            for (habit in habits) {
                if (StreakCalculator.isHabitScheduledOnDate(habit, day)) {
                    scheduledDays30++
                    if (completionsByHabit[habit.id]?.any { it.date == day } == true) {
                        completedDays30++
                    }
                }
            }
        }
        val monthlyRate = if (scheduledDays30 > 0) ((completedDays30.toFloat() / scheduledDays30) * 100).toInt() else 0

        // 4. Overall Streaks and Metrics
        val streaks = habits.map {
            StreakCalculator.calculateStreak(it, completionsByHabit[it.id] ?: emptyList(), today)
        }
        val currentMaxStreak = streaks.maxOfOrNull { it.currentStreak } ?: 0
        val bestOverallStreak = streaks.maxOfOrNull { it.bestStreak } ?: 0

        // 5. Heatmap Cells: last 70 days (10 weeks of 7 days)
        val maxPerDay = habits.size.coerceAtLeast(1)
        val heatmapDays = (69 downTo 0).map { dayOffset ->
            val date = today.minusDays(dayOffset.toLong())
            val count = completionsByDate[date]?.size ?: 0
            DayHeatmapCell(
                date = date,
                completionCount = count,
                intensity = (count.toFloat() / maxPerDay.toFloat()).coerceIn(0f, 1f)
            )
        }

        // 6. Per-habit consistency
        val habitConsistencies = habits.map { habit ->
            val habitHistory = completionsByHabit[habit.id] ?: emptyList()
            val consistency = StreakCalculator.calculateConsistencyRate(habit, habitHistory, 30, today)
            val streak = StreakCalculator.calculateStreak(habit, habitHistory, today)
            HabitConsistencyItem(
                habit = habit,
                consistencyRate = consistency,
                currentStreak = streak.currentStreak,
                totalCompletions = habitHistory.size
            )
        }.sortedByDescending { it.consistencyRate }

        val overallConsistency = if (habitConsistencies.isNotEmpty()) {
            (habitConsistencies.map { it.consistencyRate }.average() * 100).toInt()
        } else {
            0
        }

        // Best day of week
        val bestDayDow = completedCompletions
            .groupBy { it.date.dayOfWeek }
            .maxByOrNull { it.value.size }?.key
        val bestDayName = bestDayDow?.name?.lowercase()?.replaceFirstChar { it.uppercase() }

        val actionableInsights = mutableListOf<String>()
        if (weeklyRate > 0) {
            actionableInsights.add("You completed $weeklyRate% of your scheduled days this week.")
        }
        if (weeklyRate > priorRate && priorRate > 0) {
            actionableInsights.add("You're more consistent this week than last week.")
        }
        if (bestDayName != null && completedCompletions.isNotEmpty()) {
            actionableInsights.add("$bestDayName was your strongest day.")
        }

        // 7. Calculate Achievements locally or use stream
        val achievements = if (streamAchievements.isNotEmpty()) {
            streamAchievements
        } else {
            com.aistudio.dayflow.app.domain.usecase.AchievementCalculator.calculateAchievements(
                habits = habits,
                allCompletions = allCompletions,
                unlockedAchievementIds = emptySet(),
                referenceDate = today
            )
        }
        val unlockedCount = achievements.count { it.isUnlocked }

        StatisticsUiState(
            isLoading = false,
            selectedTab = tab,
            todayScheduledCount = todayScheduled,
            todayCompletedCount = todayCompleted,
            todayCompletionRate = todayRate,
            weeklyCompletionRate = weeklyRate,
            weeklyCompletedDays = weeklyCompletedDays,
            weeklyScheduledDays = weeklyScheduledDays,
            weeklyDayItems = weeklyDayItems,
            weeklySummaryText = weeklySummary,
            monthlyCompletionRate = monthlyRate,
            monthlyCompletedCount = completedDays30,
            currentStreak = currentMaxStreak,
            bestStreak = bestOverallStreak,
            totalCompletions = completedCompletions.size,
            overallConsistencyRate = overallConsistency,
            bestDay = bestDayName,
            actionableInsights = actionableInsights,
            trendSummary = trendSummary,
            heatmapDays = heatmapDays,
            habitConsistencies = habitConsistencies,
            achievements = achievements,
            unlockedCount = unlockedCount,
            totalAchievementsCount = achievements.size
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState()
    )
}
