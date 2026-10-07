package com.aistudio.dayflow.app.data.repository

import com.aistudio.dayflow.app.data.local.dao.GoalDao
import com.aistudio.dayflow.app.data.local.dao.HabitGoalLinkDao
import com.aistudio.dayflow.app.data.local.dao.MilestoneDao
import com.aistudio.dayflow.app.data.local.entity.HabitGoalLinkEntity
import com.aistudio.dayflow.app.domain.model.Goal
import com.aistudio.dayflow.app.domain.model.GoalStatus
import com.aistudio.dayflow.app.domain.model.GoalWithDetails
import com.aistudio.dayflow.app.domain.model.Milestone
import com.aistudio.dayflow.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant

class GoalRepositoryImpl(
    private val goalDao: GoalDao,
    private val milestoneDao: MilestoneDao,
    private val habitGoalLinkDao: HabitGoalLinkDao
) : GoalRepository {

    override fun getActiveGoals(): Flow<List<Goal>> {
        return goalDao.getActiveGoals().map { list -> list.map { it.toDomain() } }
    }

    override fun getArchivedGoals(): Flow<List<Goal>> {
        return goalDao.getArchivedGoals().map { list -> list.map { it.toDomain() } }
    }

    override fun getAllGoals(): Flow<List<Goal>> {
        return goalDao.getAllGoals().map { list -> list.map { it.toDomain() } }
    }

    override fun getGoalById(id: Long): Flow<Goal?> {
        return goalDao.getGoalById(id).map { it?.toDomain() }
    }

    override fun getGoalWithDetails(id: Long): Flow<GoalWithDetails?> {
        val goalFlow = goalDao.getGoalById(id)
        val milestonesFlow = milestoneDao.getMilestonesForGoal(id)
        val linksFlow = habitGoalLinkDao.getLinksForGoal(id)

        return combine(goalFlow, milestonesFlow, linksFlow) { goalEntity, milestoneEntities, linkEntities ->
            goalEntity?.let {
                GoalWithDetails(
                    goal = it.toDomain(),
                    milestones = milestoneEntities.map { m -> m.toDomain() },
                    linkedHabitIds = linkEntities.map { l -> l.habitId }
                )
            }
        }
    }

    override suspend fun getGoalByIdOnce(id: Long): Goal? {
        return goalDao.getGoalByIdOnce(id)?.toDomain()
    }

    override suspend fun insertGoal(goal: Goal): Long {
        return goalDao.insertGoal(goal.toEntity())
    }

    override suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal.toEntity())
    }

    override suspend fun updateGoalProgress(id: Long, progress: Float) {
        goalDao.updateGoalProgress(id, progress)
    }

    override suspend fun updateGoalStatus(id: Long, status: GoalStatus) {
        goalDao.updateGoalStatus(id, status)
    }

    override suspend fun deleteGoal(id: Long) {
        goalDao.deleteGoalById(id)
    }

    override fun getMilestonesForGoal(goalId: Long): Flow<List<Milestone>> {
        return milestoneDao.getMilestonesForGoal(goalId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertMilestone(milestone: Milestone): Long {
        return milestoneDao.insertMilestone(milestone.toEntity())
    }

    override suspend fun updateMilestone(milestone: Milestone) {
        milestoneDao.updateMilestone(milestone.toEntity())
    }

    override suspend fun setMilestoneCompleted(id: Long, completed: Boolean) {
        val completedAt = if (completed) Instant.now() else null
        milestoneDao.setMilestoneCompleted(id, completed, completedAt)
    }

    override suspend fun deleteMilestone(id: Long) {
        milestoneDao.deleteMilestoneById(id)
    }

    override fun getLinkedHabitIdsForGoal(goalId: Long): Flow<List<Long>> {
        return habitGoalLinkDao.getLinksForGoal(goalId).map { links ->
            links.map { it.habitId }
        }
    }

    override suspend fun linkHabitToMilestone(habitId: Long, milestoneId: Long, goalId: Long) {
        habitGoalLinkDao.insertLink(
            HabitGoalLinkEntity(
                habitId = habitId,
                milestoneId = milestoneId,
                goalId = goalId,
                createdAt = Instant.now()
            )
        )
    }

    override suspend fun unlinkHabitFromMilestone(habitId: Long, milestoneId: Long) {
        habitGoalLinkDao.deleteLink(habitId, milestoneId)
    }
}
