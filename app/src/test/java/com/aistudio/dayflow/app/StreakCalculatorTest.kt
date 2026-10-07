package com.aistudio.dayflow.app

import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.FrequencyType
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.HabitCompletion
import com.aistudio.dayflow.app.domain.usecase.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StreakCalculatorTest {

    private fun createHabit(
        frequencyType: FrequencyType = FrequencyType.DAILY,
        selectedDays: Set<DayOfWeek> = emptySet(),
        targetTimesPerWeek: Int = 7,
        createdAt: LocalDate = LocalDate.of(2026, 1, 1)
    ): Habit {
        return Habit(
            id = 1,
            name = "Test Habit",
            frequencyType = frequencyType,
            selectedDays = selectedDays,
            targetTimesPerWeek = targetTimesPerWeek,
            createdAt = createdAt.atStartOfDay(ZoneId.systemDefault()).toInstant()
        )
    }

    // 1. First completion
    @Test
    fun testFirstCompletionIncreasesStreakToOne() {
        val habit = createHabit()
        val today = LocalDate.of(2026, 10, 2)
        val completions = listOf(
            HabitCompletion(habitId = 1, date = today, status = CompletionStatus.COMPLETED)
        )

        val streak = StreakCalculator.calculateStreak(habit, completions, today)
        assertEquals(1, streak.currentStreak)
        assertEquals(1, streak.bestStreak)
        assertTrue(streak.isCompletedToday)
        assertEquals(1, streak.totalCompletions)
    }

    // 2. Two consecutive qualifying completions
    @Test
    fun testTwoConsecutiveQualifyingCompletions() {
        val habit = createHabit()
        val today = LocalDate.of(2026, 10, 2)
        val yesterday = today.minusDays(1)
        val completions = listOf(
            HabitCompletion(habitId = 1, date = yesterday, status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = today, status = CompletionStatus.COMPLETED)
        )

        val streak = StreakCalculator.calculateStreak(habit, completions, today)
        assertEquals(2, streak.currentStreak)
        assertEquals(2, streak.bestStreak)
        assertTrue(streak.isCompletedToday)
    }

    // 3. Missed scheduled day
    @Test
    fun testMissedScheduledDayResetsStreak() {
        val habit = createHabit()
        val today = LocalDate.of(2026, 10, 5)
        val completions = listOf(
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 1), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 2), status = CompletionStatus.COMPLETED),
            // Missed 10-03 and 10-04
            HabitCompletion(habitId = 1, date = today, status = CompletionStatus.COMPLETED)
        )

        val streak = StreakCalculator.calculateStreak(habit, completions, today)
        assertEquals(1, streak.currentStreak)
        assertEquals(2, streak.bestStreak)
    }

    // 4. Weekly habit (not broken on non-scheduled days)
    @Test
    fun testWeeklyHabitSchedule() {
        val habit = createHabit(
            frequencyType = FrequencyType.SELECTED_DAYS,
            selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        )
        // 2026-10-02 is Friday, 2026-10-05 is Monday, 2026-10-09 is Friday
        val completions = listOf(
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 2), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 5), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 9), status = CompletionStatus.COMPLETED)
        )

        val streak = StreakCalculator.calculateStreak(habit, completions, LocalDate.of(2026, 10, 9))
        assertEquals(3, streak.currentStreak)
        assertEquals(3, streak.bestStreak)
    }

    // 5. Weekday habit
    @Test
    fun testWeekdayHabitSkipsWeekendsWithoutBreakingStreak() {
        val habit = createHabit(frequencyType = FrequencyType.WEEKDAYS)
        // 2026-10-02 is Friday, 10-03 is Saturday, 10-04 is Sunday, 10-05 is Monday
        val completions = listOf(
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 2), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 5), status = CompletionStatus.COMPLETED)
        )

        val streak = StreakCalculator.calculateStreak(habit, completions, LocalDate.of(2026, 10, 5))
        assertEquals(2, streak.currentStreak)
    }

    // 6. Selected-day habit
    @Test
    fun testSelectedDayHabit() {
        val habit = createHabit(
            frequencyType = FrequencyType.SELECTED_DAYS,
            selectedDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
        )
        // 2026-09-29 (Tue), 2026-10-01 (Thu)
        val completions = listOf(
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 9, 29), status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = LocalDate.of(2026, 10, 1), status = CompletionStatus.COMPLETED)
        )

        val streak = StreakCalculator.calculateStreak(habit, completions, LocalDate.of(2026, 10, 1))
        assertEquals(2, streak.currentStreak)
    }

    // 7. Custom schedule
    @Test
    fun testCustomScheduleExpectedDays() {
        val habit = createHabit(
            frequencyType = FrequencyType.SELECTED_DAYS,
            selectedDays = setOf(DayOfWeek.SUNDAY)
        )
        assertFalse(StreakCalculator.isHabitScheduledOnDate(habit, LocalDate.of(2026, 10, 2))) // Friday
        assertTrue(StreakCalculator.isHabitScheduledOnDate(habit, LocalDate.of(2026, 10, 4)))  // Sunday
    }

    // 8. Undo completion recalculates correctly
    @Test
    fun testUndoCompletionRecalculatesStreak() {
        val habit = createHabit()
        val today = LocalDate.of(2026, 10, 2)
        val yesterday = today.minusDays(1)

        val initialCompletions = listOf(
            HabitCompletion(habitId = 1, date = yesterday, status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = today, status = CompletionStatus.COMPLETED)
        )
        assertEquals(2, StreakCalculator.calculateStreak(habit, initialCompletions, today).currentStreak)

        // Undo today
        val undoneCompletions = listOf(
            HabitCompletion(habitId = 1, date = yesterday, status = CompletionStatus.COMPLETED)
        )
        val afterUndoStreak = StreakCalculator.calculateStreak(habit, undoneCompletions, today)
        assertEquals(1, afterUndoStreak.currentStreak)
        assertFalse(afterUndoStreak.isCompletedToday)
    }

    // 9. Backdated completion
    @Test
    fun testBackdatedCompletionRestoresBrokenStreak() {
        val habit = createHabit()
        val d1 = LocalDate.of(2026, 10, 1)
        val d2 = LocalDate.of(2026, 10, 2)
        val d3 = LocalDate.of(2026, 10, 3)

        // Only d1 and d3 completed initially
        val initial = listOf(
            HabitCompletion(habitId = 1, date = d1, status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = d3, status = CompletionStatus.COMPLETED)
        )
        assertEquals(1, StreakCalculator.calculateStreak(habit, initial, d3).currentStreak)

        // Now user backdates d2
        val withBackdated = listOf(
            HabitCompletion(habitId = 1, date = d1, status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = d2, status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = d3, status = CompletionStatus.COMPLETED)
        )
        assertEquals(3, StreakCalculator.calculateStreak(habit, withBackdated, d3).currentStreak)
    }

    // 10. Rest/skip behavior
    @Test
    fun testRestDayPreservesStreakWithoutIncrementing() {
        val habit = createHabit()
        val d1 = LocalDate.of(2026, 10, 1)
        val d2 = LocalDate.of(2026, 10, 2)
        val d3 = LocalDate.of(2026, 10, 3)

        val completions = listOf(
            HabitCompletion(habitId = 1, date = d1, status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = d2, status = CompletionStatus.REST_DAY),
            HabitCompletion(habitId = 1, date = d3, status = CompletionStatus.COMPLETED)
        )

        val streak = StreakCalculator.calculateStreak(habit, completions, d3)
        assertEquals(2, streak.currentStreak)
        assertEquals(2, streak.bestStreak)
    }

    // 11. Schedule change
    @Test
    fun testScheduleChangeAdaptsGracefully() {
        val habitDaily = createHabit(frequencyType = FrequencyType.DAILY)
        val habitWeekdays = habitDaily.copy(frequencyType = FrequencyType.WEEKDAYS)

        val saturday = LocalDate.of(2026, 10, 3)
        assertTrue(StreakCalculator.isHabitScheduledOnDate(habitDaily, saturday))
        assertFalse(StreakCalculator.isHabitScheduledOnDate(habitWeekdays, saturday))
    }

    // 12. Month boundary
    @Test
    fun testMonthBoundaryConsecutiveCompletions() {
        val habit = createHabit()
        val sep30 = LocalDate.of(2026, 9, 30)
        val oct1 = LocalDate.of(2026, 10, 1)
        val oct2 = LocalDate.of(2026, 10, 2)

        val completions = listOf(
            HabitCompletion(habitId = 1, date = sep30, status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = oct1, status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = oct2, status = CompletionStatus.COMPLETED)
        )

        val streak = StreakCalculator.calculateStreak(habit, completions, oct2)
        assertEquals(3, streak.currentStreak)
    }

    // 13. Year boundary
    @Test
    fun testYearBoundaryConsecutiveCompletions() {
        val habit = createHabit(createdAt = LocalDate.of(2025, 12, 1))
        val dec31 = LocalDate.of(2025, 12, 31)
        val jan1 = LocalDate.of(2026, 1, 1)

        val completions = listOf(
            HabitCompletion(habitId = 1, date = dec31, status = CompletionStatus.COMPLETED),
            HabitCompletion(habitId = 1, date = jan1, status = CompletionStatus.COMPLETED)
        )

        val streak = StreakCalculator.calculateStreak(habit, completions, jan1)
        assertEquals(2, streak.currentStreak)
    }

    // 14. Timezone/date boundary
    @Test
    fun testTimezoneBoundaryConsistentLocalDate() {
        val localDate = LocalDate.now(ZoneId.of("UTC"))
        val habit = createHabit()
        val completions = listOf(
            HabitCompletion(habitId = 1, date = localDate, status = CompletionStatus.COMPLETED)
        )

        val streak = StreakCalculator.calculateStreak(habit, completions, localDate)
        assertEquals(1, streak.currentStreak)
        assertTrue(streak.isCompletedToday)
    }
}
