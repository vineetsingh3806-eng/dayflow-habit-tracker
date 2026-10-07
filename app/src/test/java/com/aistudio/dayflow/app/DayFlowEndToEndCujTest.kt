package com.aistudio.dayflow.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.dayflow.app.data.local.DayFlowDatabase
import com.aistudio.dayflow.app.data.local.preferences.DayFlowDataStore
import com.aistudio.dayflow.app.data.repository.GoalRepositoryImpl
import com.aistudio.dayflow.app.data.repository.HabitRepositoryImpl
import com.aistudio.dayflow.app.data.repository.SettingsRepositoryImpl
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.FrequencyType
import com.aistudio.dayflow.app.domain.model.Goal
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.Milestone
import com.aistudio.dayflow.app.domain.model.ThemeMode
import com.aistudio.dayflow.app.domain.usecase.CreateGoalUseCase
import com.aistudio.dayflow.app.domain.usecase.CreateHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.CreateMilestoneUseCase
import com.aistudio.dayflow.app.domain.usecase.GetActiveGoalsUseCase
import com.aistudio.dayflow.app.domain.usecase.GetActiveHabitsUseCase
import com.aistudio.dayflow.app.domain.usecase.GetGoalWithDetailsUseCase
import com.aistudio.dayflow.app.domain.usecase.GoalProgressCalculator
import com.aistudio.dayflow.app.domain.usecase.LinkHabitToMilestoneUseCase
import com.aistudio.dayflow.app.domain.usecase.RecordHabitCompletionUseCase
import com.aistudio.dayflow.app.domain.usecase.SetOnboardingCompletedUseCase
import com.aistudio.dayflow.app.domain.usecase.StreakCalculator
import com.aistudio.dayflow.app.domain.usecase.ToggleHabitCompletionUseCase
import com.aistudio.dayflow.app.domain.usecase.ToggleMilestoneCompletionUseCase
import com.aistudio.dayflow.app.domain.usecase.UndoHabitCompletionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DayFlowEndToEndCujTest {

    private lateinit var database: DayFlowDatabase
    private lateinit var habitRepo: HabitRepositoryImpl
    private lateinit var goalRepo: GoalRepositoryImpl
    private lateinit var settingsRepo: SettingsRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DayFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitRepo = HabitRepositoryImpl(database.habitDao(), database.habitCompletionDao())
        goalRepo = GoalRepositoryImpl(database.goalDao(), database.milestoneDao(), database.habitGoalLinkDao())
        settingsRepo = SettingsRepositoryImpl(DayFlowDataStore(context))
        runBlocking {
            settingsRepo.setOnboardingCompleted(false)
            settingsRepo.setThemeMode(ThemeMode.SYSTEM)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testCompleteUserJourney() = runBlocking {
        // 1. Fresh Install & Onboarding
        val initialSettings = settingsRepo.getSettings().first()
        assertFalse(initialSettings.onboardingCompleted)

        val setOnboardingUseCase = SetOnboardingCompletedUseCase(settingsRepo)
        setOnboardingUseCase(true)
        assertTrue(settingsRepo.getSettings().first().onboardingCompleted)

        // 2. Create Goal
        val createGoalUseCase = CreateGoalUseCase(goalRepo)
        val goalId = createGoalUseCase(
            Goal(
                title = "Learn Android Architecture",
                description = "Master Compose and Clean Architecture",
                targetDate = LocalDate.now().plusMonths(2)
            )
        ).getOrThrow()
        assertTrue(goalId > 0)

        // 3. Create Milestone
        val createMilestoneUseCase = CreateMilestoneUseCase(goalRepo)
        val milestoneId = createMilestoneUseCase(
            Milestone(
                goalId = goalId,
                title = "Complete Phase 2 Database and Domain Layer",
                order = 1
            )
        ).getOrThrow()
        assertTrue(milestoneId > 0)

        // 4. Create Habit
        val createHabitUseCase = CreateHabitUseCase(habitRepo)
        val habitId = createHabitUseCase(
            Habit(
                name = "Study Kotlin 30 mins",
                targetCount = 30,
                targetUnit = "mins",
                frequencyType = FrequencyType.DAILY,
                reminderTime = LocalTime.of(8, 0)
            )
        ).getOrThrow()
        assertTrue(habitId > 0)

        // 5. Link Habit to Milestone
        val linkHabitUseCase = LinkHabitToMilestoneUseCase(goalRepo)
        linkHabitUseCase(habitId, milestoneId, goalId)

        val getGoalWithDetailsUseCase = GetGoalWithDetailsUseCase(goalRepo)
        val goalDetails = getGoalWithDetailsUseCase(goalId).first()
        assertNotNull(goalDetails)
        assertEquals(1, goalDetails!!.milestones.size)
        assertEquals(1, goalDetails.linkedHabitIds.size)
        assertEquals(habitId, goalDetails.linkedHabitIds[0])

        // 6. Complete Habit
        val today = LocalDate.now()
        val recordCompletionUseCase = RecordHabitCompletionUseCase(habitRepo)
        val undoCompletionUseCase = UndoHabitCompletionUseCase(habitRepo)
        val toggleHabitUseCase = ToggleHabitCompletionUseCase(recordCompletionUseCase, undoCompletionUseCase)

        toggleHabitUseCase(habitId, today, isCurrentlyCompleted = false)

        val todayCompletions = habitRepo.getCompletionsForDate(today).first()
        assertEquals(1, todayCompletions.size)
        assertEquals(habitId, todayCompletions[0].habitId)

        // 7. Check Streak & Progress
        val habit = habitRepo.getHabitByIdOnce(habitId)!!
        val completions = habitRepo.getCompletionsForHabit(habitId).first()
        val streak = StreakCalculator.calculateStreak(habit, completions, today)
        assertEquals(1, streak.currentStreak)
        assertTrue(streak.isCompletedToday)

        // 8. Undo Completion
        toggleHabitUseCase(habitId, today, isCurrentlyCompleted = true)
        val afterUndo = habitRepo.getCompletionsForDate(today).first()
        assertEquals(0, afterUndo.size)

        // Re-complete for stats
        toggleHabitUseCase(habitId, today, isCurrentlyCompleted = false)

        // 9. Complete Milestone
        val toggleMilestoneUseCase = ToggleMilestoneCompletionUseCase(goalRepo)
        toggleMilestoneUseCase(milestoneId, currentCompleted = false)

        val updatedGoalDetails = getGoalWithDetailsUseCase(goalId).first()!!
        assertTrue(updatedGoalDetails.milestones[0].completed)
        val progress = GoalProgressCalculator.calculateProgress(updatedGoalDetails)
        assertEquals(1.0f, progress, 0.001f)

        // 10. Configure Reminder & Settings
        settingsRepo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, settingsRepo.getSettings().first().themeMode)
    }
}
