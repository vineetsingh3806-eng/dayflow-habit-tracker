package com.aistudio.dayflow.app.domain.usecase

import com.aistudio.dayflow.app.domain.model.Achievement
import com.aistudio.dayflow.app.domain.model.AchievementCategory
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.HabitCompletion
import java.time.LocalDate

object AchievementCalculator {

    data class AchievementDefinition(
        val id: String,
        val title: String,
        val description: String,
        val iconEmoji: String,
        val target: Int,
        val category: AchievementCategory
    )

    val DEFINITIONS = listOf(
        AchievementDefinition(
            id = "first_habit_completed",
            title = "First Step",
            description = "Complete your first habit",
            iconEmoji = "🏆",
            target = 1,
            category = AchievementCategory.COMPLETION
        ),
        AchievementDefinition(
            id = "streak_3",
            title = "Building Momentum",
            description = "Maintain a 3-day streak",
            iconEmoji = "🔥",
            target = 3,
            category = AchievementCategory.STREAK
        ),
        AchievementDefinition(
            id = "streak_7",
            title = "7-Day Warrior",
            description = "Maintain a 7-day streak",
            iconEmoji = "🔥",
            target = 7,
            category = AchievementCategory.STREAK
        ),
        AchievementDefinition(
            id = "streak_14",
            title = "Two Weeks Strong",
            description = "Maintain a 14-day streak",
            iconEmoji = "⚡",
            target = 14,
            category = AchievementCategory.STREAK
        ),
        AchievementDefinition(
            id = "streak_30",
            title = "Monthly Mastery",
            description = "Maintain a 30-day streak",
            iconEmoji = "🌟",
            target = 30,
            category = AchievementCategory.STREAK
        ),
        AchievementDefinition(
            id = "completions_10",
            title = "Double Digits",
            description = "Complete habits 10 times",
            iconEmoji = "✨",
            target = 10,
            category = AchievementCategory.COMPLETION
        ),
        AchievementDefinition(
            id = "completions_50",
            title = "Consistency",
            description = "Complete habits 50 times",
            iconEmoji = "💪",
            target = 50,
            category = AchievementCategory.COMPLETION
        ),
        AchievementDefinition(
            id = "completions_100",
            title = "Century",
            description = "Complete habits 100 times",
            iconEmoji = "🎯",
            target = 100,
            category = AchievementCategory.COMPLETION
        )
    )

    /**
     * Evaluates current user activity and returns the list of achievements with up-to-date
     * unlocked status and progress metrics.
     */
    fun calculateAchievements(
        habits: List<Habit>,
        allCompletions: List<HabitCompletion>,
        unlockedAchievementIds: Set<String>,
        referenceDate: LocalDate = LocalDate.now()
    ): List<Achievement> {
        val totalCompletedCount = allCompletions.count { it.status == CompletionStatus.COMPLETED }
        val completionsByHabit = allCompletions.groupBy { it.habitId }

        val bestOverallStreak = habits.maxOfOrNull { habit ->
            val habitHistory = completionsByHabit[habit.id] ?: emptyList()
            StreakCalculator.calculateStreak(habit, habitHistory, referenceDate).bestStreak
        } ?: 0

        return DEFINITIONS.map { def ->
            val rawValue = when (def.category) {
                AchievementCategory.STREAK -> bestOverallStreak
                AchievementCategory.COMPLETION -> totalCompletedCount
                AchievementCategory.GENERAL -> totalCompletedCount
            }

            val isUnlocked = unlockedAchievementIds.contains(def.id) || rawValue >= def.target
            val progress = if (isUnlocked) def.target else rawValue.coerceAtMost(def.target)

            Achievement(
                id = def.id,
                title = def.title,
                description = def.description,
                iconEmoji = def.iconEmoji,
                target = def.target,
                currentProgress = progress,
                isUnlocked = isUnlocked,
                category = def.category
            )
        }
    }
}
