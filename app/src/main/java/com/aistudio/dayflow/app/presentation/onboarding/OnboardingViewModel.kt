package com.aistudio.dayflow.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.usecase.CreateHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.SetOnboardingCompletedUseCase
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val setOnboardingCompletedUseCase: SetOnboardingCompletedUseCase,
    private val createHabitUseCase: CreateHabitUseCase? = null
) : ViewModel() {

    fun completeOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            setOnboardingCompletedUseCase(true)
            onFinished()
        }
    }

    fun completeWithHabits(selectedHabits: List<Habit>, onFinished: () -> Unit) {
        viewModelScope.launch {
            if (createHabitUseCase != null) {
                selectedHabits.forEach { habit ->
                    createHabitUseCase(habit)
                }
            }
            setOnboardingCompletedUseCase(true)
            onFinished()
        }
    }
}

