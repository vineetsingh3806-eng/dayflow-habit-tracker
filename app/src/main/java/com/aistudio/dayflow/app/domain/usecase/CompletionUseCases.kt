package com.aistudio.dayflow.app.domain.usecase

import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.HabitCompletion
import com.aistudio.dayflow.app.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class RecordHabitCompletionUseCase(private val habitRepository: HabitRepository) {
    suspend operator fun invoke(
        habitId: Long,
        date: LocalDate = LocalDate.now(),
        status: CompletionStatus = CompletionStatus.COMPLETED,
        notes: String = ""
    ): Result<Long> {
        return try {
            val id = habitRepository.recordCompletion(habitId, date, status, notes)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class UndoHabitCompletionUseCase(private val habitRepository: HabitRepository) {
    suspend operator fun invoke(habitId: Long, date: LocalDate = LocalDate.now()): Result<Unit> {
        return try {
            habitRepository.undoCompletion(habitId, date)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ToggleHabitCompletionUseCase(
    private val recordCompletionUseCase: RecordHabitCompletionUseCase,
    private val undoCompletionUseCase: UndoHabitCompletionUseCase
) {
    suspend operator fun invoke(
        habitId: Long,
        date: LocalDate,
        isCurrentlyCompleted: Boolean,
        status: CompletionStatus = CompletionStatus.COMPLETED
    ): Result<Unit> {
        return if (isCurrentlyCompleted) {
            undoCompletionUseCase(habitId, date)
        } else {
            recordCompletionUseCase(habitId, date, status).map { }
        }
    }
}

class GetCompletionsForDateUseCase(private val habitRepository: HabitRepository) {
    operator fun invoke(date: LocalDate): Flow<List<HabitCompletion>> =
        habitRepository.getCompletionsForDate(date)
}

class GetHabitCompletionsUseCase(private val habitRepository: HabitRepository) {
    operator fun invoke(habitId: Long): Flow<List<HabitCompletion>> =
        habitRepository.getCompletionsForHabit(habitId)
}
