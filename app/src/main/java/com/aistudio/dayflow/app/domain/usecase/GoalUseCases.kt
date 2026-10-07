package com.aistudio.dayflow.app.domain.usecase

import com.aistudio.dayflow.app.domain.model.Goal
import com.aistudio.dayflow.app.domain.model.GoalStatus
import com.aistudio.dayflow.app.domain.model.GoalWithDetails
import com.aistudio.dayflow.app.domain.model.Milestone
import com.aistudio.dayflow.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow

class CreateGoalUseCase(private val goalRepository: GoalRepository) {
    suspend operator fun invoke(goal: Goal): Result<Long> {
        val trimmed = goal.title.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Goal title cannot be empty"))
        }
        return try {
            val id = goalRepository.insertGoal(goal.copy(title = trimmed))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class UpdateGoalUseCase(private val goalRepository: GoalRepository) {
    suspend operator fun invoke(goal: Goal): Result<Unit> {
        val trimmed = goal.title.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Goal title cannot be empty"))
        }
        return try {
            goalRepository.updateGoal(goal.copy(title = trimmed))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteGoalUseCase(private val goalRepository: GoalRepository) {
    suspend operator fun invoke(goalId: Long): Result<Unit> {
        return try {
            goalRepository.deleteGoal(goalId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ArchiveGoalUseCase(private val goalRepository: GoalRepository) {
    suspend operator fun invoke(goalId: Long): Result<Unit> {
        return try {
            goalRepository.updateGoalStatus(goalId, GoalStatus.ARCHIVED)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetActiveGoalsUseCase(private val goalRepository: GoalRepository) {
    operator fun invoke(): Flow<List<Goal>> = goalRepository.getActiveGoals()
}

class GetGoalWithDetailsUseCase(private val goalRepository: GoalRepository) {
    operator fun invoke(goalId: Long): Flow<GoalWithDetails?> = goalRepository.getGoalWithDetails(goalId)
}

class CreateMilestoneUseCase(private val goalRepository: GoalRepository) {
    suspend operator fun invoke(milestone: Milestone): Result<Long> {
        val trimmed = milestone.title.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Milestone title cannot be empty"))
        }
        return try {
            val id = goalRepository.insertMilestone(milestone.copy(title = trimmed))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ToggleMilestoneCompletionUseCase(private val goalRepository: GoalRepository) {
    suspend operator fun invoke(milestoneId: Long, currentCompleted: Boolean): Result<Unit> {
        return try {
            goalRepository.setMilestoneCompleted(milestoneId, !currentCompleted)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteMilestoneUseCase(private val goalRepository: GoalRepository) {
    suspend operator fun invoke(milestoneId: Long): Result<Unit> {
        return try {
            goalRepository.deleteMilestone(milestoneId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class LinkHabitToMilestoneUseCase(private val goalRepository: GoalRepository) {
    suspend operator fun invoke(habitId: Long, milestoneId: Long, goalId: Long): Result<Unit> {
        return try {
            goalRepository.linkHabitToMilestone(habitId, milestoneId, goalId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class UnlinkHabitFromMilestoneUseCase(private val goalRepository: GoalRepository) {
    suspend operator fun invoke(habitId: Long, milestoneId: Long): Result<Unit> {
        return try {
            goalRepository.unlinkHabitFromMilestone(habitId, milestoneId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
