package com.aistudio.dayflow.app

import com.aistudio.dayflow.app.domain.model.AchievementCategory
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.FrequencyType
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.HabitCompletion
import com.aistudio.dayflow.app.domain.model.WeeklyDayStatus
import com.aistudio.dayflow.app.domain.usecase.AchievementCalculator
import com.aistudio.dayflow.app.domain.usecase.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class DayFlowEngagementFeaturesTest {

    private fun createHabit(
        id: Long = 1,
        name: String = "Reading",
        frequencyType: FrequencyType = FrequencyType.DAILY,
        selectedDays: Set<DayOfWeek> = emptySet(),
        createdDate: LocalDate = LocalDate.of(2026, 9, 1)
    ): Habit {
        return Habit(
            id = id,
            name = name,
            frequencyType = frequencyType,
            selectedDays = selectedDays,
            createdAt = createdDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        )
    }

    // --- 1. LOCAL STREAKS TESTS ---

    @Test
    fun testConsecutiveCompletionsIncreaseCurrentAndBestStreak() {
        val habit = createHabit()
        val today = LocalDate.of(2026, 10, 5)
        val completions = listOf(
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 1), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 2), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 3), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 4), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = today, status = CompletionStatus.COMPLETED)
        )

        val streakInfo = StreakCalculator.calculateStreak(habit, completions, today)
        assertEquals(5, streakInfo.currentStreak)
        assertEquals(5, streakInfo.bestStreak)
        assertTrue(streakInfo.isCompletedToday)
        assertEquals(5, streakInfo.totalCompletions)
    }

    @Test
    fun testMultipleCompletionsOnSameDayDoNotInflateStreak() {
        val habit = createHabit()
        val today = LocalDate.of(2026, 10, 2)
        val yesterday = LocalDate.of(2026, 10, 1)

        // Multiple completions logged on the same day (e.g. drinking glasses of water)
        val completions = listOf(
            HabitCompletion(id = 101, habitId = 1, date = yesterday, status = CompletionStatus.COMPLETED),
            HabitCompletion(id = 102, habitId = 1, date = yesterday, status = CompletionStatus.COMPLETED),
            HabitCompletion(id = 103, habitId = 1, date = today, status = CompletionStatus.COMPLETED),
            HabitCompletion(id = 104, habitId = 1, date = today, status = CompletionStatus.COMPLETED),
            HabitCompletion(id = 105, habitId = 1, date = today, status = CompletionStatus.COMPLETED)
        )

        val streakInfo = StreakCalculator.calculateStreak(habit, completions, today)
        // Only 2 calendar days were completed consecutively
        assertEquals(2, streakInfo.currentStreak)
        assertEquals(2, streakInfo.bestStreak)
    }

    @Test
    fun testMissingRequiredDayBreaksStreakWhilePreservingBestStreak() {
        val habit = createHabit()
        val today = LocalDate.of(2026, 10, 6)

        val completions = listOf(
            // Streak of 3 days
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 1), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 2), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 3), status = CompletionStatus.COMPLETED),
            // Missed 10-04 and 10-05
            // New streak starts today
            HabitCompletion(habitId = 1, date = today, status = CompletionStatus.COMPLETED)
        )

        val streakInfo = StreakCalculator.calculateStreak(habit, completions, today)
        assertEquals(1, streakInfo.currentStreak)
        assertEquals(3, streakInfo.bestStreak)
    }

    @Test
    fun testWeekdayHabitDoesNotBreakStreakOnWeekends() {
        val habit = createHabit(frequencyType = FrequencyType.WEEKDAYS)

        // Friday 2026-10-02, Sat 10-03, Sun 10-04, Mon 2026-10-05
        val friday = LocalDate.of(2026, 10, 2)
        val monday = LocalDate.of(2026, 10, 5)

        val completions = listOf(
            HabitCompletion(habitId = 1, date = friday, status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = monday, status = CompletionStatus.COMPLETED)
        )

        val streakOnMonday = StreakCalculator.calculateStreak(habit, completions, monday)
        assertEquals(2, streakOnMonday.currentStreak)
        assertEquals(2, streakOnMonday.bestStreak)
    }

    @Test
    fun testTimesPerWeekHabitRespectsWeeklyTargetWithoutRequiringEveryDay() {
        // Target: 3 times per week, created on Monday 2026-09-28
        val habit = Habit(
            id = 1,
            name = "Workouts",
            frequencyType = FrequencyType.TIMES_PER_WEEK,
            targetTimesPerWeek = 3,
            createdAt = LocalDate.of(2026, 9, 28).atStartOfDay(ZoneId.systemDefault()).toInstant()
        )

        // Week 1 (2026-09-28 to 2026-10-04): completed Mon (09-28), Wed (09-30), Fri (10-02) -> 3 times (target met!)
        // Week 2 (2026-10-05 to 2026-10-11): today is Wednesday 10-07. Completed Mon 10-05 and Wed 10-07 -> 2 times.
        val completions = listOf(
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 9, 28), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 9, 30), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 2), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 5), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 7), status = CompletionStatus.COMPLETED)
        )

        val wednesday = LocalDate.of(2026, 10, 7)
        val streak = StreakCalculator.calculateStreak(habit, completions, wednesday)

        // Streak is alive and consecutive: 3 from week 1 + 2 from week 2 = 5!
        // Tuesday was NOT treated as a broken day!
        assertEquals(5, streak.currentStreak)
        assertEquals(5, streak.bestStreak)
    }

    // --- 2. ACHIEVEMENTS & MILESTONES TESTS ---

    @Test
    fun testAchievementsUnlockBasedOnActualActivity() {
        val habit = createHabit()
        val completions = (1..7).map { day ->
            HabitCompletion(
                habitId = 1,
                date = LocalDate.of(2026, 10, day),
                status = CompletionStatus.COMPLETED
            )
        }

        val achievements = AchievementCalculator.calculateAchievements(
            habits = listOf(habit),
            allCompletions = completions,
            unlockedAchievementIds = emptySet(),
            referenceDate = LocalDate.of(2026, 10, 7)
        )

        // First step (1 completion) -> Unlocked
        val firstStep = achievements.first { it.id == "first_habit_completed" }
        assertTrue(firstStep.isUnlocked)
        assertEquals(1, firstStep.currentProgress)

        // 3-day streak -> Unlocked
        val streak3 = achievements.first { it.id == "streak_3" }
        assertTrue(streak3.isUnlocked)
        assertEquals(3, streak3.currentProgress)

        // 7-day streak -> Unlocked
        val streak7 = achievements.first { it.id == "streak_7" }
        assertTrue(streak7.isUnlocked)
        assertEquals(7, streak7.currentProgress)

        // 14-day streak -> Locked with progress 7/14
        val streak14 = achievements.first { it.id == "streak_14" }
        assertFalse(streak14.isUnlocked)
        assertEquals(7, streak14.currentProgress)
        assertEquals(14, streak14.target)

        // 50 completions -> Locked with progress 7/50
        val comp50 = achievements.first { it.id == "completions_50" }
        assertFalse(comp50.isUnlocked)
        assertEquals(7, comp50.currentProgress)
        assertEquals(50, comp50.target)
    }

    @Test
    fun testPreviouslyUnlockedAchievementsRemainUnlocked() {
        val habit = createHabit()
        // No current completions (e.g. newly installed or data reset)
        val completions = emptyList<HabitCompletion>()

        val previouslyUnlocked = setOf("first_habit_completed", "streak_7")

        val achievements = AchievementCalculator.calculateAchievements(
            habits = listOf(habit),
            allCompletions = completions,
            unlockedAchievementIds = previouslyUnlocked,
            referenceDate = LocalDate.of(2026, 10, 1)
        )

        val firstStep = achievements.first { it.id == "first_habit_completed" }
        assertTrue(firstStep.isUnlocked)
        assertEquals(firstStep.target, firstStep.currentProgress)

        val streak7 = achievements.first { it.id == "streak_7" }
        assertTrue(streak7.isUnlocked)
        assertEquals(streak7.target, streak7.currentProgress)

        val streak30 = achievements.first { it.id == "streak_30" }
        assertFalse(streak30.isUnlocked)
        assertEquals(0, streak30.currentProgress)
    }

    // --- 3. PROGRESS VISUALIZATION TESTS ---

    @Test
    fun testConsistencyRateCalculation() {
        val habit = createHabit()
        val referenceDate = LocalDate.of(2026, 10, 10)

        // 5 completions in last 10 days
        val completions = listOf(
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 1), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 2), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 3), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 4), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 5), status = CompletionStatus.COMPLETED)
        )

        val rate10Days = StreakCalculator.calculateConsistencyRate(habit, completions, days = 10, referenceDate = referenceDate)
        assertEquals(0.5f, rate10Days, 0.01f)
    }
}
