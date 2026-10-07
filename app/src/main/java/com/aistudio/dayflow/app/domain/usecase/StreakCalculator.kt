package com.aistudio.dayflow.app.domain.usecase

import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.FrequencyType
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.HabitCompletion
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class StreakInfo(
    val currentStreak: Int,
    val bestStreak: Int,
    val totalCompletions: Int,
    val isCompletedToday: Boolean,
    val isDueToday: Boolean
)

object StreakCalculator {

    /**
     * Determines whether a habit is scheduled / expected on a given [date].
     */
    fun isHabitScheduledOnDate(habit: Habit, date: LocalDate): Boolean {
        // If date is before habit creation date, it wasn't scheduled yet
        val createdDate = habit.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        if (date.isBefore(createdDate)) {
            return false
        }

        return when (habit.frequencyType) {
            FrequencyType.DAILY -> true
            FrequencyType.WEEKDAYS -> {
                val dow = date.dayOfWeek
                dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY
            }
            FrequencyType.SELECTED_DAYS -> {
                habit.selectedDays.contains(date.dayOfWeek)
            }
            FrequencyType.TIMES_PER_WEEK -> {
                // For times per week, any day can potentially be a scheduled day up to the weekly target
                true
            }
        }
    }

    /**
     * Calculates the deterministic streak metrics for a [habit] given its [completions]
     * up to a [referenceDate] (defaults to today).
     */
    fun calculateStreak(
        habit: Habit,
        completions: List<HabitCompletion>,
        referenceDate: LocalDate = LocalDate.now()
    ): StreakInfo {
        val completedCompletions = completions.filter { it.status == CompletionStatus.COMPLETED }
        val completionDates = completedCompletions.map { it.date }.toSet()
        val restDates = completions.filter { it.status == CompletionStatus.REST_DAY }.map { it.date }.toSet()

        val isCompletedToday = completionDates.contains(referenceDate)
        val isDueToday = isHabitScheduledOnDate(habit, referenceDate)
        val totalCompletions = completedCompletions.size

        if (completedCompletions.isEmpty()) {
            return StreakInfo(
                currentStreak = 0,
                bestStreak = 0,
                totalCompletions = 0,
                isCompletedToday = isCompletedToday,
                isDueToday = isDueToday
            )
        }

        val createdDate = habit.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        // Handle TIMES_PER_WEEK separately to respect weekly targets
        if (habit.frequencyType == FrequencyType.TIMES_PER_WEEK) {
            val (current, best) = calculateTimesPerWeekStreak(habit, completionDates, referenceDate, createdDate)
            return StreakInfo(
                currentStreak = current,
                bestStreak = best,
                totalCompletions = totalCompletions,
                isCompletedToday = isCompletedToday,
                isDueToday = isDueToday
            )
        }

        // Sort all unique dates in ascending order
        val sortedDates = completionDates.sorted()
        val earliestDate = sortedDates.first()

        // 1. Calculate best streak across entire history
        var bestStreak = 0
        var currentSequence = 0
        var checkDate = earliestDate

        while (!checkDate.isAfter(referenceDate)) {
            val isScheduled = isHabitScheduledOnDate(habit, checkDate)
            val isCompleted = completionDates.contains(checkDate)
            val isRest = restDates.contains(checkDate)

            if (isScheduled) {
                if (isCompleted) {
                    currentSequence++
                    if (currentSequence > bestStreak) {
                        bestStreak = currentSequence
                    }
                } else if (isRest) {
                    // Rest day preserves sequence without incrementing
                } else {
                    // Missed scheduled day resets sequence
                    currentSequence = 0
                }
            }
            checkDate = checkDate.plusDays(1)
        }

        // 2. Calculate current active streak ending today or yesterday
        var currentStreak = 0
        var walkBackDate = referenceDate

        // If today is scheduled and NOT completed, streak can still be alive from yesterday
        if (isHabitScheduledOnDate(habit, referenceDate) && !isCompletedToday && !restDates.contains(referenceDate)) {
            walkBackDate = referenceDate.minusDays(1)
        }

        // Walk backwards through scheduled days
        while (!walkBackDate.isBefore(earliestDate)) {
            val isScheduled = isHabitScheduledOnDate(habit, walkBackDate)
            val isCompleted = completionDates.contains(walkBackDate)
            val isRest = restDates.contains(walkBackDate)

            if (isScheduled) {
                if (isCompleted) {
                    currentStreak++
                } else if (isRest) {
                    // Rest day preserves streak
                } else {
                    // Break on first missed scheduled day
                    break
                }
            }
            walkBackDate = walkBackDate.minusDays(1)
        }

        // Ensure bestStreak is at least currentStreak
        if (currentStreak > bestStreak) {
            bestStreak = currentStreak
        }

        return StreakInfo(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            totalCompletions = totalCompletions,
            isCompletedToday = isCompletedToday,
            isDueToday = isDueToday
        )
    }

    /**
     * Calculates completion rate for a habit over the last N days.
     */
    fun calculateConsistencyRate(
        habit: Habit,
        completions: List<HabitCompletion>,
        days: Int = 30,
        referenceDate: LocalDate = LocalDate.now()
    ): Float {
        val completedDates = completions.filter { it.status == CompletionStatus.COMPLETED }.map { it.date }.toSet()
        val startDate = referenceDate.minusDays(days.toLong() - 1)

        if (habit.frequencyType == FrequencyType.TIMES_PER_WEEK) {
            val weeks = days / 7f
            val expectedCompletions = (weeks * habit.targetTimesPerWeek).coerceAtLeast(1f)
            val actualCompletions = completedDates.count { !it.isBefore(startDate) && !it.isAfter(referenceDate) }
            return (actualCompletions / expectedCompletions).coerceIn(0f, 1f)
        }

        var scheduledDaysCount = 0
        var completedDaysCount = 0

        var curr = startDate
        while (!curr.isAfter(referenceDate)) {
            if (isHabitScheduledOnDate(habit, curr)) {
                scheduledDaysCount++
                if (completedDates.contains(curr)) {
                    completedDaysCount++
                }
            }
            curr = curr.plusDays(1)
        }

        return if (scheduledDaysCount == 0) 1.0f else (completedDaysCount.toFloat() / scheduledDaysCount.toFloat()).coerceIn(0f, 1f)
    }

    private fun calculateTimesPerWeekStreak(
        habit: Habit,
        completionDates: Set<LocalDate>,
        referenceDate: LocalDate,
        createdDate: LocalDate
    ): Pair<Int, Int> {
        val target = habit.targetTimesPerWeek.coerceAtLeast(1)
        val startWeekMonday = createdDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val currentWeekMonday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        var currentStreak = 0
        var bestStreak = 0
        var checkMonday = startWeekMonday

        while (!checkMonday.isAfter(currentWeekMonday)) {
            val weekSunday = checkMonday.plusDays(6)
            val weekCompletions = completionDates.count {
                !it.isBefore(checkMonday) && !it.isAfter(weekSunday) && !it.isAfter(referenceDate)
            }

            val isCurrentWeek = (checkMonday == currentWeekMonday)
            if (isCurrentWeek) {
                val isCompletedToday = completionDates.contains(referenceDate)
                val remainingDaysIncludingToday = ChronoUnit.DAYS.between(referenceDate, weekSunday).toInt() + (if (!isCompletedToday) 1 else 0)
                val canStillMeetTarget = (weekCompletions + remainingDaysIncludingToday) >= target

                if (weekCompletions >= target || canStillMeetTarget) {
                    currentStreak += weekCompletions
                    if (currentStreak > bestStreak) {
                        bestStreak = currentStreak
                    }
                } else {
                    currentStreak = 0
                }
            } else {
                if (weekCompletions >= target) {
                    currentStreak += weekCompletions
                    if (currentStreak > bestStreak) {
                        bestStreak = currentStreak
                    }
                } else {
                    currentStreak = 0
                }
            }

            checkMonday = checkMonday.plusWeeks(1)
        }

        return Pair(currentStreak, bestStreak)
    }

    /**
     * Returns an encouraging, guilt-free motivational message based on current and best streak.
     */
    fun getMotivationalStreakMessage(currentStreak: Int, bestStreak: Int): String {
        return when {
            currentStreak == 0 && bestStreak > 0 -> "It's okay ❤️ Start again today."
            currentStreak == 0 -> "Ready for a fresh start?"
            currentStreak == 1 -> "Great start! Day 1 complete."
            currentStreak in 2..6 -> "Every day is another chance to build consistency."
            currentStreak in 7..13 -> "🔥 7 days strong!"
            currentStreak >= 14 -> "Awesome consistency! Keep going."
            else -> "Every day is another chance to build consistency."
        }
    }
}
