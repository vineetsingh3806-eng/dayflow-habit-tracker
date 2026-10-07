package com.aistudio.dayflow.app.domain.repository

import com.aistudio.dayflow.app.domain.model.AppSettings
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.Goal
import com.aistudio.dayflow.app.domain.model.GoalStatus
import com.aistudio.dayflow.app.domain.model.GoalWithDetails
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.HabitCompletion
import com.aistudio.dayflow.app.domain.model.Milestone
import com.aistudio.dayflow.app.domain.model.ThemeMode
import com.aistudio.dayflow.app.domain.model.WeekStart
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    fun getActiveHabits(): Flow<List<Habit>>
    fun getArchivedHabits(): Flow<List<Habit>>
    fun getHabitById(id: Long): Flow<Habit?>
    suspend fun getHabitByIdOnce(id: Long): Habit?
    suspend fun insertHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun archiveHabit(id: Long)
    suspend fun restoreHabit(id: Long)
    suspend fun deleteHabit(id: Long)

    fun getCompletionsForDate(date: LocalDate): Flow<List<HabitCompletion>>
    fun getCompletionsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitCompletion>>
    suspend fun getCompletionsForDateRangeOnce(startDate: LocalDate, endDate: LocalDate): List<HabitCompletion>
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>>
    fun getAllCompletions(): Flow<List<HabitCompletion>>
    suspend fun recordCompletion(habitId: Long, date: LocalDate, status: CompletionStatus = CompletionStatus.COMPLETED, notes: String = ""): Long
    suspend fun undoCompletion(habitId: Long, date: LocalDate)
    fun getTotalCompletionsCount(): Flow<Int>
}

interface GoalRepository {
    fun getActiveGoals(): Flow<List<Goal>>
    fun getArchivedGoals(): Flow<List<Goal>>
    fun getAllGoals(): Flow<List<Goal>>
    fun getGoalById(id: Long): Flow<Goal?>
    fun getGoalWithDetails(id: Long): Flow<GoalWithDetails?>
    suspend fun getGoalByIdOnce(id: Long): Goal?
    suspend fun insertGoal(goal: Goal): Long
    suspend fun updateGoal(goal: Goal)
    suspend fun updateGoalProgress(id: Long, progress: Float)
    suspend fun updateGoalStatus(id: Long, status: GoalStatus)
    suspend fun deleteGoal(id: Long)

    fun getMilestonesForGoal(goalId: Long): Flow<List<Milestone>>
    suspend fun insertMilestone(milestone: Milestone): Long
    suspend fun updateMilestone(milestone: Milestone)
    suspend fun setMilestoneCompleted(id: Long, completed: Boolean)
    suspend fun deleteMilestone(id: Long)

    fun getLinkedHabitIdsForGoal(goalId: Long): Flow<List<Long>>
    suspend fun linkHabitToMilestone(habitId: Long, milestoneId: Long, goalId: Long)
    suspend fun unlinkHabitFromMilestone(habitId: Long, milestoneId: Long)
}

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setWeekStart(weekStart: WeekStart)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setDefaultReminderTime(hour: Int, minute: Int)
    suspend fun setOnboardingCompleted(completed: Boolean)
}
