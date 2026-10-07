package com.aistudio.dayflow.app.domain.usecase

import com.aistudio.dayflow.app.data.local.preferences.DayFlowDataStore
import com.aistudio.dayflow.app.domain.model.Achievement
import com.aistudio.dayflow.app.domain.repository.HabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

class GetAchievementsUseCase(
    private val habitRepository: HabitRepository,
    private val dataStore: DayFlowDataStore,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    operator fun invoke(referenceDate: LocalDate = LocalDate.now()): Flow<List<Achievement>> {
        return combine(
            habitRepository.getActiveHabits(),
            habitRepository.getAllCompletions(),
            dataStore.unlockedAchievementsFlow
        ) { habits: List<com.aistudio.dayflow.app.domain.model.Habit>, completions: List<com.aistudio.dayflow.app.domain.model.HabitCompletion>, savedUnlockedIds: Set<String> ->
            val achievements = AchievementCalculator.calculateAchievements(
                habits = habits,
                allCompletions = completions,
                unlockedAchievementIds = savedUnlockedIds,
                referenceDate = referenceDate
            )

            // Auto-persist newly unlocked achievements so they stay unlocked forever
            val newlyUnlocked = achievements.filter { it.isUnlocked && !savedUnlockedIds.contains(it.id) }.map { it.id }.toSet()
            if (newlyUnlocked.isNotEmpty()) {
                externalScope.launch {
                    dataStore.saveUnlockedAchievements(newlyUnlocked)
                }
            }

            achievements
        }
    }
}
