package com.aistudio.dayflow.app

import com.aistudio.dayflow.app.domain.model.Goal
import com.aistudio.dayflow.app.domain.model.GoalWithDetails
import com.aistudio.dayflow.app.domain.model.Milestone
import com.aistudio.dayflow.app.domain.usecase.GoalProgressCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalProgressCalculatorTest {

    @Test
    fun testMilestoneProgressCalculation() {
        val goal = Goal(id = 1, title = "Complete Marathon")
        val milestones = listOf(
            Milestone(id = 1, goalId = 1, title = "Milestone 1", completed = true),
            Milestone(id = 2, goalId = 1, title = "Milestone 2", completed = true),
            Milestone(id = 3, goalId = 1, title = "Milestone 3", completed = false),
            Milestone(id = 4, goalId = 1, title = "Milestone 4", completed = false)
        )
        val details = GoalWithDetails(goal = goal, milestones = milestones)

        val progress = GoalProgressCalculator.calculateProgress(details)
        assertEquals(0.5f, progress, 0.001f)
    }

    @Test
    fun testEmptyMilestonesPreservesExistingGoalProgress() {
        val goal = Goal(id = 1, title = "Read Books", progress = 0.75f)
        val details = GoalWithDetails(goal = goal, milestones = emptyList())

        val progress = GoalProgressCalculator.calculateProgress(details)
        assertEquals(0.75f, progress, 0.001f)
    }

    @Test
    fun testDeadlineDescriptionsNonShaming() {
        val today = LocalDate.of(2026, 10, 2)
        val futureGoal = Goal(id = 1, title = "Goal", targetDate = today.plusDays(5))
        val overdueGoal = Goal(id = 2, title = "Goal", targetDate = today.minusDays(3))
        val todayGoal = Goal(id = 3, title = "Goal", targetDate = today)

        assertEquals("5 days remaining", GoalProgressCalculator.getDeadlineDescription(futureGoal, today))
        assertEquals("3 days past target date", GoalProgressCalculator.getDeadlineDescription(overdueGoal, today))
        assertEquals("Due today", GoalProgressCalculator.getDeadlineDescription(todayGoal, today))

        assertTrue(GoalProgressCalculator.isGoalOverdue(overdueGoal, today))
        assertFalse(GoalProgressCalculator.isGoalOverdue(futureGoal, today))
    }
}
