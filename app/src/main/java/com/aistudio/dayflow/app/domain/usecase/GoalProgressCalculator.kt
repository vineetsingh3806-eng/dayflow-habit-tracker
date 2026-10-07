package com.aistudio.dayflow.app.domain.usecase

import com.aistudio.dayflow.app.domain.model.Goal
import com.aistudio.dayflow.app.domain.model.GoalWithDetails
import com.aistudio.dayflow.app.domain.model.Milestone

object GoalProgressCalculator {

    /**
     * Calculates the deterministic progress percentage (0.0f to 1.0f) for a goal.
     */
    fun calculateProgress(goalWithDetails: GoalWithDetails): Float {
        val milestones = goalWithDetails.milestones
        if (milestones.isEmpty()) {
            return goalWithDetails.goal.progress.coerceIn(0f, 1f)
        }

        val completedCount = milestones.count { it.completed }
        return (completedCount.toFloat() / milestones.size.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Checks if a goal is overdue without shaming language.
     */
    fun isGoalOverdue(goal: Goal, currentDate: java.time.LocalDate = java.time.LocalDate.now()): Boolean {
        val target = goal.targetDate ?: return false
        return currentDate.isAfter(target) && goal.progress < 1.0f
    }

    /**
     * Returns friendly, human days remaining or overdue string.
     */
    fun getDeadlineDescription(goal: Goal, currentDate: java.time.LocalDate = java.time.LocalDate.now()): String {
        val target = goal.targetDate ?: return "No target date set"
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(currentDate, target)
        return when {
            daysDiff > 1 -> "$daysDiff days remaining"
            daysDiff == 1L -> "Due tomorrow"
            daysDiff == 0L -> "Due today"
            daysDiff == -1L -> "1 day past target date"
            else -> "${-daysDiff} days past target date"
        }
    }
}
