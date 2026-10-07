package com.aistudio.dayflow.app.presentation.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.FrequencyType
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.HabitCompletion
import com.aistudio.dayflow.app.domain.repository.HabitRepository
import com.aistudio.dayflow.app.domain.usecase.ArchiveHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.CreateHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.DeleteHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.GetActiveHabitsUseCase
import com.aistudio.dayflow.app.domain.usecase.GetArchivedHabitsUseCase
import com.aistudio.dayflow.app.domain.usecase.RestoreHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.StreakCalculator
import com.aistudio.dayflow.app.domain.usecase.StreakInfo
import com.aistudio.dayflow.app.domain.usecase.ToggleHabitCompletionUseCase
import com.aistudio.dayflow.app.domain.usecase.UpdateHabitUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class HabitFilter {
    ALL,
    DAILY,
    WEEKDAYS,
    CUSTOM
}

data class HabitItemUi(
    val habit: Habit,
    val streakInfo: StreakInfo,
    val isCompletedToday: Boolean
)

data class HabitsUiState(
    val isLoading: Boolean = true,
    val selectedFilter: HabitFilter = HabitFilter.ALL,
    val habits: List<HabitItemUi> = emptyList(),
    val archivedCount: Int = 0
)

class HabitsViewModel(
    private val getActiveHabitsUseCase: GetActiveHabitsUseCase,
    private val getArchivedHabitsUseCase: GetArchivedHabitsUseCase,
    private val createHabitUseCase: CreateHabitUseCase,
    private val updateHabitUseCase: UpdateHabitUseCase,
    private val deleteHabitUseCase: DeleteHabitUseCase,
    private val archiveHabitUseCase: ArchiveHabitUseCase,
    private val restoreHabitUseCase: RestoreHabitUseCase,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val filterFlow = MutableStateFlow(HabitFilter.ALL)

    val uiState: StateFlow<HabitsUiState> = combine(
        getActiveHabitsUseCase(),
        getArchivedHabitsUseCase(),
        habitRepository.getAllCompletions(),
        filterFlow
    ) { activeHabits, archivedHabits, allCompletions, filter ->
        val currentToday = LocalDate.now()
        val completionsByHabit = allCompletions.groupBy { it.habitId }

        val filteredHabits = when (filter) {
            HabitFilter.ALL -> activeHabits
            HabitFilter.DAILY -> activeHabits.filter { it.frequencyType == FrequencyType.DAILY }
            HabitFilter.WEEKDAYS -> activeHabits.filter { it.frequencyType == FrequencyType.WEEKDAYS }
            HabitFilter.CUSTOM -> activeHabits.filter {
                it.frequencyType == FrequencyType.SELECTED_DAYS || it.frequencyType == FrequencyType.TIMES_PER_WEEK
            }
        }

        val habitItems = filteredHabits.map { habit ->
            val history = completionsByHabit[habit.id] ?: emptyList()
            val streak = StreakCalculator.calculateStreak(habit, history, currentToday)
            HabitItemUi(
                habit = habit,
                streakInfo = streak,
                isCompletedToday = history.any { it.date == currentToday && it.status == CompletionStatus.COMPLETED }
            )
        }

        HabitsUiState(
            isLoading = false,
            selectedFilter = filter,
            habits = habitItems,
            archivedCount = archivedHabits.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsUiState()
    )

    fun setFilter(filter: HabitFilter) {
        filterFlow.value = filter
    }

    fun toggleHabitToday(habitId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            toggleHabitCompletionUseCase(
                habitId = habitId,
                date = LocalDate.now(),
                isCurrentlyCompleted = isCompleted
            )
        }
    }

    fun archiveHabit(habitId: Long) {
        viewModelScope.launch {
            archiveHabitUseCase(habitId)
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            deleteHabitUseCase(habitId)
        }
    }
}
