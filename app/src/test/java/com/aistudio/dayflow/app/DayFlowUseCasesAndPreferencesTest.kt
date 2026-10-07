package com.aistudio.dayflow.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.dayflow.app.data.local.DayFlowDatabase
import com.aistudio.dayflow.app.data.local.preferences.DayFlowDataStore
import com.aistudio.dayflow.app.data.repository.GoalRepositoryImpl
import com.aistudio.dayflow.app.data.repository.HabitRepositoryImpl
import com.aistudio.dayflow.app.data.repository.SettingsRepositoryImpl
import com.aistudio.dayflow.app.domain.model.AppSettings
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.FrequencyType
import com.aistudio.dayflow.app.domain.model.Goal
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.ThemeMode
import com.aistudio.dayflow.app.domain.model.WeekStart
import com.aistudio.dayflow.app.domain.usecase.ArchiveHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.CreateGoalUseCase
import com.aistudio.dayflow.app.domain.usecase.CreateHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.DeleteHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.GetActiveHabitsUseCase
import com.aistudio.dayflow.app.domain.usecase.RecordHabitCompletionUseCase
import com.aistudio.dayflow.app.domain.usecase.RestoreHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.ToggleHabitCompletionUseCase
import com.aistudio.dayflow.app.domain.usecase.UndoHabitCompletionUseCase
import com.aistudio.dayflow.app.domain.usecase.UpdateHabitUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DayFlowUseCasesAndPreferencesTest {

    private lateinit var database: DayFlowDatabase
    private lateinit var habitRepository: HabitRepositoryImpl
    private lateinit var goalRepository: GoalRepositoryImpl
    private lateinit var settingsRepository: SettingsRepositoryImpl
    private lateinit var dataStore: DayFlowDataStore

    // Use cases
    private lateinit var createHabitUseCase: CreateHabitUseCase
    private lateinit var updateHabitUseCase: UpdateHabitUseCase
    private lateinit var deleteHabitUseCase: DeleteHabitUseCase
    private lateinit var archiveHabitUseCase: ArchiveHabitUseCase
    private lateinit var restoreHabitUseCase: RestoreHabitUseCase
    private lateinit var getActiveHabitsUseCase: GetActiveHabitsUseCase
    private lateinit var recordHabitCompletionUseCase: RecordHabitCompletionUseCase
    private lateinit var undoHabitCompletionUseCase: UndoHabitCompletionUseCase
    private lateinit var toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase
    private lateinit var createGoalUseCase: CreateGoalUseCase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DayFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        habitRepository = HabitRepositoryImpl(
            database.habitDao(),
            database.habitCompletionDao()
        )

        goalRepository = GoalRepositoryImpl(
            database.goalDao(),
            database.milestoneDao(),
            database.habitGoalLinkDao()
        )

        dataStore = DayFlowDataStore(context)
        settingsRepository = SettingsRepositoryImpl(dataStore)

        createHabitUseCase = CreateHabitUseCase(habitRepository)
        updateHabitUseCase = UpdateHabitUseCase(habitRepository)
        deleteHabitUseCase = DeleteHabitUseCase(habitRepository)
        archiveHabitUseCase = ArchiveHabitUseCase(habitRepository)
        restoreHabitUseCase = RestoreHabitUseCase(habitRepository)
        getActiveHabitsUseCase = GetActiveHabitsUseCase(habitRepository)
        recordHabitCompletionUseCase = RecordHabitCompletionUseCase(habitRepository)
        undoHabitCompletionUseCase = UndoHabitCompletionUseCase(habitRepository)
        toggleHabitCompletionUseCase = ToggleHabitCompletionUseCase(
            recordHabitCompletionUseCase,
            undoHabitCompletionUseCase
        )
        createGoalUseCase = CreateGoalUseCase(goalRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testCreateHabitValidation() = runBlocking {
        // Empty habit name should fail
        val invalidResult = createHabitUseCase(
            Habit(name = "   ")
        )
        assertTrue(invalidResult.isFailure)

        // Valid habit name should succeed
        val validResult = createHabitUseCase(
            Habit(
                name = "  Morning Run  ",
                frequencyType = FrequencyType.WEEKDAYS,
                targetTimesPerWeek = 5
            )
        )
        assertTrue(validResult.isSuccess)

        val active = getActiveHabitsUseCase().first()
        assertEquals(1, active.size)
        assertEquals("Morning Run", active[0].name)
    }

    @Test
    fun testToggleCompletionUseCase() = runBlocking {
        val habitId = createHabitUseCase(
            Habit(name = "Daily Walk")
        ).getOrThrow()

        val date = LocalDate.of(2026, 10, 2)

        // 1. Toggle to complete
        val res1 = toggleHabitCompletionUseCase(
            habitId = habitId,
            date = date,
            isCurrentlyCompleted = false,
            status = CompletionStatus.COMPLETED
        )
        assertTrue(res1.isSuccess)

        val completionsAfterFirstToggle = habitRepository.getCompletionsForDate(date).first()
        assertEquals(1, completionsAfterFirstToggle.size)
        assertEquals(habitId, completionsAfterFirstToggle[0].habitId)

        // 2. Toggle to undo
        val res2 = toggleHabitCompletionUseCase(
            habitId = habitId,
            date = date,
            isCurrentlyCompleted = true
        )
        assertTrue(res2.isSuccess)

        val completionsAfterSecondToggle = habitRepository.getCompletionsForDate(date).first()
        assertEquals(0, completionsAfterSecondToggle.size)
    }

    @Test
    fun testDefaultAppearanceIsLightMode() = runBlocking {
        val defaultSettings = AppSettings()
        assertEquals(ThemeMode.LIGHT, defaultSettings.themeMode)
    }

    @Test
    fun testDataStorePreferencesPersistence() = runBlocking {
        // Reset to default settings
        settingsRepository.setThemeMode(ThemeMode.SYSTEM)
        settingsRepository.setWeekStart(WeekStart.MONDAY)
        settingsRepository.setNotificationsEnabled(true)
        settingsRepository.setOnboardingCompleted(false)

        val initialSettings = settingsRepository.getSettings().first()
        assertEquals(ThemeMode.SYSTEM, initialSettings.themeMode)
        assertEquals(WeekStart.MONDAY, initialSettings.weekStart)
        assertTrue(initialSettings.notificationsEnabled)
        assertFalse(initialSettings.onboardingCompleted)

        // Update settings
        settingsRepository.setThemeMode(ThemeMode.DARK)
        settingsRepository.setWeekStart(WeekStart.SUNDAY)
        settingsRepository.setNotificationsEnabled(false)
        settingsRepository.setOnboardingCompleted(true)

        val updatedSettings = settingsRepository.getSettings().first()
        assertEquals(ThemeMode.DARK, updatedSettings.themeMode)
        assertEquals(WeekStart.SUNDAY, updatedSettings.weekStart)
        assertFalse(updatedSettings.notificationsEnabled)
        assertTrue(updatedSettings.onboardingCompleted)
    }

    @Test
    fun testGoalCreationValidation() = runBlocking {
        // Blank title fails
        val invalid = createGoalUseCase(
            Goal(title = "   ")
        )
        assertTrue(invalid.isFailure)

        // Valid title succeeds
        val valid = createGoalUseCase(
            Goal(title = "Save $10,000 for emergency fund")
        )
        assertTrue(valid.isSuccess)

        val goals = goalRepository.getActiveGoals().first()
        assertEquals(1, goals.size)
        assertEquals("Save $10,000 for emergency fund", goals[0].title)
    }
}
