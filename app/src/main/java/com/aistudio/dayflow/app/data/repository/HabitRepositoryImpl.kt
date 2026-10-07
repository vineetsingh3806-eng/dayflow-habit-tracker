package com.aistudio.dayflow.app.data.repository

import com.aistudio.dayflow.app.data.local.dao.HabitCompletionDao
import com.aistudio.dayflow.app.data.local.dao.HabitDao
import com.aistudio.dayflow.app.data.local.entity.HabitCompletionEntity
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.HabitCompletion
import com.aistudio.dayflow.app.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val habitCompletionDao: HabitCompletionDao
) : HabitRepository {

    override fun getActiveHabits(): Flow<List<Habit>> {
        return habitDao.getActiveHabits().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getArchivedHabits(): Flow<List<Habit>> {
        return habitDao.getArchivedHabits().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getHabitById(id: Long): Flow<Habit?> {
        return habitDao.getHabitById(id).map { it?.toDomain() }
    }

    override suspend fun getHabitByIdOnce(id: Long): Habit? {
        return habitDao.getHabitByIdOnce(id)?.toDomain()
    }

    override suspend fun insertHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit.toEntity())
    }

    override suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit.toEntity())
    }

    override suspend fun archiveHabit(id: Long) {
        habitDao.setArchived(id, true)
    }

    override suspend fun restoreHabit(id: Long) {
        habitDao.setArchived(id, false)
    }

    override suspend fun deleteHabit(id: Long) {
        habitDao.deleteHabitById(id)
    }

    override fun getCompletionsForDate(date: LocalDate): Flow<List<HabitCompletion>> {
        return habitCompletionDao.getCompletionsForDate(date).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getCompletionsForDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HabitCompletion>> {
        return habitCompletionDao.getCompletionsForDateRange(startDate, endDate).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getCompletionsForDateRangeOnce(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitCompletion> {
        return habitCompletionDao.getCompletionsForDateRangeOnce(startDate, endDate).map { it.toDomain() }
    }

    override fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>> {
        return habitCompletionDao.getCompletionsForHabit(habitId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllCompletions(): Flow<List<HabitCompletion>> {
        return habitCompletionDao.getAllCompletions().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun recordCompletion(
        habitId: Long,
        date: LocalDate,
        status: CompletionStatus,
        notes: String
    ): Long {
        val entity = HabitCompletionEntity(
            habitId = habitId,
            date = date,
            completedAt = Instant.now(),
            status = status,
            notes = notes
        )
        return habitCompletionDao.insertCompletion(entity)
    }

    override suspend fun undoCompletion(habitId: Long, date: LocalDate) {
        habitCompletionDao.deleteCompletion(habitId, date)
    }

    override fun getTotalCompletionsCount(): Flow<Int> {
        return habitCompletionDao.getTotalCompletedCount()
    }
}
