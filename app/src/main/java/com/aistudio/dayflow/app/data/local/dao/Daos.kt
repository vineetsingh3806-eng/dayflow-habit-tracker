package com.aistudio.dayflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aistudio.dayflow.app.data.local.entity.GoalEntity
import com.aistudio.dayflow.app.data.local.entity.HabitCompletionEntity
import com.aistudio.dayflow.app.data.local.entity.HabitEntity
import com.aistudio.dayflow.app.data.local.entity.HabitGoalLinkEntity
import com.aistudio.dayflow.app.data.local.entity.MilestoneEntity
import com.aistudio.dayflow.app.domain.model.GoalStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun getArchivedHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitById(id: Long): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitByIdOnce(id: Long): HabitEntity?

    @Query("SELECT * FROM habits")
    suspend fun getAllHabitsOnce(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE isArchived = 0 AND reminderTime IS NOT NULL")
    suspend fun getActiveHabitsWithRemindersOnce(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)
}

@Dao
interface HabitCompletionDao {
    @Query("SELECT * FROM habit_completions WHERE date = :date")
    fun getCompletionsForDate(date: LocalDate): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getCompletionsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getCompletionsForDateRangeOnce(startDate: LocalDate, endDate: LocalDate): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date DESC")
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getCompletionsForHabitOnce(habitId: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getCompletion(habitId: Long, date: LocalDate): HabitCompletionEntity?

    @Query("SELECT * FROM habit_completions ORDER BY date DESC")
    fun getAllCompletions(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions ORDER BY date DESC")
    suspend fun getAllCompletionsOnce(): List<HabitCompletionEntity>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE status = 'COMPLETED'")
    fun getTotalCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND status = 'COMPLETED' AND date BETWEEN :startDate AND :endDate")
    suspend fun getCompletedCountForHabitInRange(habitId: Long, startDate: LocalDate, endDate: LocalDate): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletionEntity): Long

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCompletion(habitId: Long, date: LocalDate)

    @Query("DELETE FROM habit_completions WHERE id = :id")
    suspend fun deleteCompletionById(id: Long)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE status = 'ARCHIVED' ORDER BY createdAt DESC")
    fun getArchivedGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun getAllGoalsOnce(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun getGoalById(id: Long): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalByIdOnce(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET progress = :progress WHERE id = :id")
    suspend fun updateGoalProgress(id: Long, progress: Float)

    @Query("UPDATE goals SET status = :status WHERE id = :id")
    suspend fun updateGoalStatus(id: Long, status: GoalStatus)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)
}

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones WHERE goalId = :goalId ORDER BY `order` ASC, id ASC")
    fun getMilestonesForGoal(goalId: Long): Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM milestones WHERE goalId = :goalId ORDER BY `order` ASC, id ASC")
    suspend fun getMilestonesForGoalOnce(goalId: Long): List<MilestoneEntity>

    @Query("SELECT * FROM milestones")
    suspend fun getAllMilestonesOnce(): List<MilestoneEntity>

    @Query("SELECT * FROM milestones WHERE id = :id")
    suspend fun getMilestoneById(id: Long): MilestoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: MilestoneEntity): Long

    @Update
    suspend fun updateMilestone(milestone: MilestoneEntity)

    @Query("UPDATE milestones SET completed = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun setMilestoneCompleted(id: Long, completed: Boolean, completedAt: java.time.Instant?)

    @Query("DELETE FROM milestones WHERE id = :id")
    suspend fun deleteMilestoneById(id: Long)
}

@Dao
interface HabitGoalLinkDao {
    @Query("SELECT * FROM habit_goal_links WHERE goalId = :goalId")
    fun getLinksForGoal(goalId: Long): Flow<List<HabitGoalLinkEntity>>

    @Query("SELECT * FROM habit_goal_links WHERE goalId = :goalId")
    suspend fun getLinksForGoalOnce(goalId: Long): List<HabitGoalLinkEntity>

    @Query("SELECT * FROM habit_goal_links WHERE habitId = :habitId")
    fun getLinksForHabit(habitId: Long): Flow<List<HabitGoalLinkEntity>>

    @Query("SELECT * FROM habit_goal_links")
    suspend fun getAllLinksOnce(): List<HabitGoalLinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: HabitGoalLinkEntity)

    @Query("DELETE FROM habit_goal_links WHERE habitId = :habitId AND milestoneId = :milestoneId")
    suspend fun deleteLink(habitId: Long, milestoneId: Long)

    @Query("DELETE FROM habit_goal_links WHERE goalId = :goalId")
    suspend fun deleteLinksForGoal(goalId: Long)
}
