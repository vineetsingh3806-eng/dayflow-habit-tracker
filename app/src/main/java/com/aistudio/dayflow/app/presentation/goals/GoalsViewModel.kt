package com.aistudio.dayflow.app.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.dayflow.app.domain.model.Goal
import com.aistudio.dayflow.app.domain.model.Milestone
import com.aistudio.dayflow.app.domain.repository.GoalRepository
import com.aistudio.dayflow.app.domain.usecase.ArchiveGoalUseCase
import com.aistudio.dayflow.app.domain.usecase.CreateGoalUseCase
import com.aistudio.dayflow.app.domain.usecase.DeleteGoalUseCase
import com.aistudio.dayflow.app.domain.usecase.GetActiveGoalsUseCase
import com.aistudio.dayflow.app.domain.usecase.GoalProgressCalculator
import com.aistudio.dayflow.app.domain.usecase.UpdateGoalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GoalItemUi(
    val goal: Goal,
    val milestoneCount: Int,
    val completedMilestoneCount: Int,
    val deadlineText: String
)

data class GoalsUiState(
    val isLoading: Boolean = true,
    val goals: List<GoalItemUi> = emptyList()
)

class GoalsViewModel(
    private val getActiveGoalsUseCase: GetActiveGoalsUseCase,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState: StateFlow<GoalsUiState> = getActiveGoalsUseCase().combine(
        MutableStateFlow(Unit)
    ) { activeGoals, _ ->
        val goalItems = activeGoals.map { goal ->
            val milestones = goalRepository.getMilestonesForGoal(goal.id)
            GoalItemUi(
                goal = goal,
                milestoneCount = 0, // updated reactively in GoalDetail
                completedMilestoneCount = 0,
                deadlineText = GoalProgressCalculator.getDeadlineDescription(goal, today)
            )
        }
        GoalsUiState(
            isLoading = false,
            goals = goalItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GoalsUiState()
    )
}
