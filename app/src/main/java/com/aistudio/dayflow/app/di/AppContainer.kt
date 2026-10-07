package com.aistudio.dayflow.app.di

import android.content.Context
import com.aistudio.dayflow.app.data.local.DayFlowDatabase
import com.aistudio.dayflow.app.data.local.preferences.DayFlowDataStore
import com.aistudio.dayflow.app.data.repository.GoalRepositoryImpl
import com.aistudio.dayflow.app.data.repository.HabitRepositoryImpl
import com.aistudio.dayflow.app.data.repository.SettingsRepositoryImpl
import com.aistudio.dayflow.app.domain.repository.GoalRepository
import com.aistudio.dayflow.app.domain.repository.HabitRepository
import com.aistudio.dayflow.app.domain.repository.SettingsRepository
import com.aistudio.dayflow.app.domain.usecase.ArchiveGoalUseCase
import com.aistudio.dayflow.app.domain.usecase.ArchiveHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.CreateGoalUseCase
import com.aistudio.dayflow.app.domain.usecase.CreateHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.CreateMilestoneUseCase
import com.aistudio.dayflow.app.domain.usecase.DeleteGoalUseCase
import com.aistudio.dayflow.app.domain.usecase.DeleteHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.DeleteMilestoneUseCase
import com.aistudio.dayflow.app.domain.usecase.GetActiveGoalsUseCase
import com.aistudio.dayflow.app.domain.usecase.GetActiveHabitsUseCase
import com.aistudio.dayflow.app.domain.usecase.GetAchievementsUseCase
import com.aistudio.dayflow.app.domain.usecase.GetAppSettingsUseCase
import com.aistudio.dayflow.app.domain.usecase.GetArchivedHabitsUseCase
import com.aistudio.dayflow.app.domain.usecase.GetCompletionsForDateUseCase
import com.aistudio.dayflow.app.domain.usecase.GetGoalWithDetailsUseCase
import com.aistudio.dayflow.app.domain.usecase.GetHabitByIdUseCase
import com.aistudio.dayflow.app.domain.usecase.GetHabitCompletionsUseCase
import com.aistudio.dayflow.app.domain.usecase.LinkHabitToMilestoneUseCase
import com.aistudio.dayflow.app.domain.usecase.RecordHabitCompletionUseCase
import com.aistudio.dayflow.app.domain.usecase.RestoreHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.SetOnboardingCompletedUseCase
import com.aistudio.dayflow.app.domain.usecase.ToggleHabitCompletionUseCase
import com.aistudio.dayflow.app.domain.usecase.ToggleMilestoneCompletionUseCase
import com.aistudio.dayflow.app.domain.usecase.UndoHabitCompletionUseCase
import com.aistudio.dayflow.app.domain.usecase.UnlinkHabitFromMilestoneUseCase
import com.aistudio.dayflow.app.domain.usecase.UpdateGoalUseCase
import com.aistudio.dayflow.app.domain.usecase.UpdateHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.UpdateNotificationsEnabledUseCase
import com.aistudio.dayflow.app.domain.usecase.UpdateThemeModeUseCase
import com.aistudio.dayflow.app.domain.usecase.UpdateWeekStartUseCase

class AppContainer(private val context: Context) {

    val database: DayFlowDatabase by lazy {
        DayFlowDatabase.getInstance(context)
    }

    val dataStore: DayFlowDataStore by lazy {
        DayFlowDataStore(context)
    }

    val habitRepository: HabitRepository by lazy {
        HabitRepositoryImpl(
            habitDao = database.habitDao(),
            habitCompletionDao = database.habitCompletionDao()
        )
    }

    val goalRepository: GoalRepository by lazy {
        GoalRepositoryImpl(
            goalDao = database.goalDao(),
            milestoneDao = database.milestoneDao(),
            habitGoalLinkDao = database.habitGoalLinkDao()
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(dataStore)
    }

    // Habit Use Cases
    val createHabitUseCase by lazy { CreateHabitUseCase(habitRepository) }
    val updateHabitUseCase by lazy { UpdateHabitUseCase(habitRepository) }
    val deleteHabitUseCase by lazy { DeleteHabitUseCase(habitRepository) }
    val archiveHabitUseCase by lazy { ArchiveHabitUseCase(habitRepository) }
    val restoreHabitUseCase by lazy { RestoreHabitUseCase(habitRepository) }
    val getActiveHabitsUseCase by lazy { GetActiveHabitsUseCase(habitRepository) }
    val getArchivedHabitsUseCase by lazy { GetArchivedHabitsUseCase(habitRepository) }
    val getHabitByIdUseCase by lazy { GetHabitByIdUseCase(habitRepository) }

    // Completion Use Cases
    val recordHabitCompletionUseCase by lazy { RecordHabitCompletionUseCase(habitRepository) }
    val undoHabitCompletionUseCase by lazy { UndoHabitCompletionUseCase(habitRepository) }
    val toggleHabitCompletionUseCase by lazy {
        ToggleHabitCompletionUseCase(recordHabitCompletionUseCase, undoHabitCompletionUseCase)
    }
    val getCompletionsForDateUseCase by lazy { GetCompletionsForDateUseCase(habitRepository) }
    val getHabitCompletionsUseCase by lazy { GetHabitCompletionsUseCase(habitRepository) }
    val getAchievementsUseCase by lazy { GetAchievementsUseCase(habitRepository, dataStore) }

    // Goal & Milestone Use Cases
    val createGoalUseCase by lazy { CreateGoalUseCase(goalRepository) }
    val updateGoalUseCase by lazy { UpdateGoalUseCase(goalRepository) }
    val deleteGoalUseCase by lazy { DeleteGoalUseCase(goalRepository) }
    val archiveGoalUseCase by lazy { ArchiveGoalUseCase(goalRepository) }
    val getActiveGoalsUseCase by lazy { GetActiveGoalsUseCase(goalRepository) }
    val getGoalWithDetailsUseCase by lazy { GetGoalWithDetailsUseCase(goalRepository) }
    val createMilestoneUseCase by lazy { CreateMilestoneUseCase(goalRepository) }
    val toggleMilestoneCompletionUseCase by lazy { ToggleMilestoneCompletionUseCase(goalRepository) }
    val deleteMilestoneUseCase by lazy { DeleteMilestoneUseCase(goalRepository) }
    val linkHabitToMilestoneUseCase by lazy { LinkHabitToMilestoneUseCase(goalRepository) }
    val unlinkHabitFromMilestoneUseCase by lazy { UnlinkHabitFromMilestoneUseCase(goalRepository) }

    // Settings Use Cases
    val getAppSettingsUseCase by lazy { GetAppSettingsUseCase(settingsRepository) }
    val updateThemeModeUseCase by lazy { UpdateThemeModeUseCase(settingsRepository) }
    val updateWeekStartUseCase by lazy { UpdateWeekStartUseCase(settingsRepository) }
    val updateNotificationsEnabledUseCase by lazy { UpdateNotificationsEnabledUseCase(settingsRepository) }
    val setOnboardingCompletedUseCase by lazy { SetOnboardingCompletedUseCase(settingsRepository) }
}
