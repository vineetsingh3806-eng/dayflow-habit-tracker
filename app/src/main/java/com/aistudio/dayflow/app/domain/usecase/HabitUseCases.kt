package com.aistudio.dayflow.app.domain.usecase

import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow

class CreateHabitUseCase(private val habitRepository: HabitRepository) {
    suspend operator fun invoke(habit: Habit): Result<Long> {
        val trimmedName = habit.name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Habit name cannot be empty"))
        }
        val cleanHabit = habit.copy(
            name = trimmedName,
            description = habit.description.trim()
        )
        return try {
            val id = habitRepository.insertHabit(cleanHabit)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class UpdateHabitUseCase(private val habitRepository: HabitRepository) {
    suspend operator fun invoke(habit: Habit): Result<Unit> {
        val trimmedName = habit.name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Habit name cannot be empty"))
        }
        return try {
            habitRepository.updateHabit(habit.copy(name = trimmedName, description = habit.description.trim()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class DeleteHabitUseCase(private val habitRepository: HabitRepository) {
    suspend operator fun invoke(habitId: Long): Result<Unit> {
        return try {
            habitRepository.deleteHabit(habitId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ArchiveHabitUseCase(private val habitRepository: HabitRepository) {
    suspend operator fun invoke(habitId: Long): Result<Unit> {
        return try {
            habitRepository.archiveHabit(habitId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RestoreHabitUseCase(private val habitRepository: HabitRepository) {
    suspend operator fun invoke(habitId: Long): Result<Unit> {
        return try {
            habitRepository.restoreHabit(habitId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetActiveHabitsUseCase(private val habitRepository: HabitRepository) {
    operator fun invoke(): Flow<List<Habit>> = habitRepository.getActiveHabits()
}

class GetArchivedHabitsUseCase(private val habitRepository: HabitRepository) {
    operator fun invoke(): Flow<List<Habit>> = habitRepository.getArchivedHabits()
}

class GetHabitByIdUseCase(private val habitRepository: HabitRepository) {
    operator fun invoke(id: Long): Flow<Habit?> = habitRepository.getHabitById(id)
    suspend fun getOnce(id: Long): Habit? = habitRepository.getHabitByIdOnce(id)
}
