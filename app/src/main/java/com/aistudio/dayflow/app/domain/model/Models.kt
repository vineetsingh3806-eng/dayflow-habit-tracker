package com.aistudio.dayflow.app.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class FrequencyType {
    DAILY,
    WEEKDAYS,
    SELECTED_DAYS,
    TIMES_PER_WEEK
}

enum class CompletionStatus {
    COMPLETED,
    REST_DAY,
    SKIPPED
}

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class WeekStart {
    MONDAY,
    SUNDAY
}

data class Habit(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val icon: String = "check_circle", // Name of Material Icon or identifier
    val colorHex: String = "#4F46E5",  // Hex color representation
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val selectedDays: Set<DayOfWeek> = emptySet(), // For SELECTED_DAYS
    val targetTimesPerWeek: Int = 7,               // For TIMES_PER_WEEK
    val targetCount: Int = 1,                      // e.g. 1 (checkbox), 8 (glasses of water), 30 (minutes)
    val targetUnit: String = "times",              // e.g. "times", "mins", "pages"
    val reminderTime: LocalTime? = null,           // Optional daily reminder time
    val createdAt: Instant = Instant.now(),
    val isArchived: Boolean = false
)

data class HabitCompletion(
    val id: Long = 0,
    val habitId: Long,
    val date: LocalDate,
    val completedAt: Instant = Instant.now(),
    val status: CompletionStatus = CompletionStatus.COMPLETED,
    val notes: String = ""
)

data class Goal(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val targetDate: LocalDate? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val progress: Float = 0.0f,
    val createdAt: Instant = Instant.now()
)

data class Milestone(
    val id: Long = 0,
    val goalId: Long,
    val title: String,
    val order: Int = 0,
    val completed: Boolean = false,
    val completedAt: Instant? = null
)

data class HabitGoalLink(
    val habitId: Long,
    val milestoneId: Long,
    val goalId: Long,
    val createdAt: Instant = Instant.now()
)

data class GoalWithDetails(
    val goal: Goal,
    val milestones: List<Milestone> = emptyList(),
    val linkedHabitIds: List<Long> = emptyList()
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val weekStart: WeekStart = WeekStart.MONDAY,
    val notificationsEnabled: Boolean = true,
    val defaultReminderHour: Int = 20, // 8:00 PM default
    val defaultReminderMinute: Int = 0,
    val onboardingCompleted: Boolean = false
)

enum class AchievementCategory {
    STREAK,
    COMPLETION,
    GENERAL
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val target: Int,
    val currentProgress: Int,
    val isUnlocked: Boolean,
    val category: AchievementCategory = AchievementCategory.GENERAL
)

enum class WeeklyDayStatus {
    COMPLETED,
    MISSED,
    REST_DAY,
    UPCOMING
}

data class WeeklyDayItem(
    val dayName: String,     // "Mon", "Tue"
    val date: LocalDate,
    val status: WeeklyDayStatus,
    val completedCount: Int,
    val scheduledCount: Int,
    val isToday: Boolean
)

data class DayHeatmapCell(
    val date: LocalDate,
    val completionCount: Int,
    val intensity: Float // 0f to 1f
)
