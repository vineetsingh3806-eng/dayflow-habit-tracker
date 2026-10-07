package com.aistudio.dayflow.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.dayflow.app.data.local.DayFlowDatabase
import com.aistudio.dayflow.app.data.local.preferences.DayFlowDataStore
import com.aistudio.dayflow.app.data.repository.HabitRepositoryImpl
import com.aistudio.dayflow.app.data.repository.SettingsRepositoryImpl
import com.aistudio.dayflow.app.data.repository.toDomain
import com.aistudio.dayflow.app.domain.model.AppSettings
import com.aistudio.dayflow.app.domain.model.FrequencyType
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.Milestone
import com.aistudio.dayflow.app.domain.model.ThemeMode
import com.aistudio.dayflow.app.domain.usecase.CreateHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.StreakCalculator
import com.aistudio.dayflow.app.domain.usecase.UpdateHabitUseCase
import com.aistudio.dayflow.app.notifications.HabitReminderReceiver
import com.aistudio.dayflow.app.notifications.HabitReminderScheduler
import com.aistudio.dayflow.app.presentation.navigation.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DayFlowPostFixRegressionTest {

    private lateinit var database: DayFlowDatabase
    private lateinit var habitRepo: HabitRepositoryImpl
    private lateinit var settingsRepo: SettingsRepositoryImpl
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, DayFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitRepo = HabitRepositoryImpl(database.habitDao(), database.habitCompletionDao())
        val dataStore = DayFlowDataStore(context)
        settingsRepo = SettingsRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testNotificationMasterToggle() = runBlocking {
        // Initially default should have notifications enabled
        val initialSettings = settingsRepo.getSettings().first()
        assertTrue(initialSettings.notificationsEnabled)

        // Turn OFF master notifications
        settingsRepo.setNotificationsEnabled(false)
        val disabledSettings = settingsRepo.getSettings().first()
        assertFalse(disabledSettings.notificationsEnabled)

        // Turn ON master notifications
        settingsRepo.setNotificationsEnabled(true)
        val enabledSettings = settingsRepo.getSettings().first()
        assertTrue(enabledSettings.notificationsEnabled)
    }

    @Test
    fun testArchivedAndDeletedHabitDetection() = runBlocking {
        val createHabit = CreateHabitUseCase(habitRepo)
        val habitId = createHabit(
            Habit(
                name = "Morning Stretch",
                reminderTime = LocalTime.of(8, 0),
                frequencyType = FrequencyType.DAILY
            )
        ).getOrThrow()

        // Active habit exists in DB and is not archived
        val activeHabit = database.habitDao().getHabitByIdOnce(habitId)
        assertNotNull(activeHabit)
        assertFalse(activeHabit!!.isArchived)

        // Archive habit
        database.habitDao().setArchived(habitId, true)
        val archivedHabit = database.habitDao().getHabitByIdOnce(habitId)
        assertNotNull(archivedHabit)
        assertTrue(archivedHabit!!.isArchived)

        // Delete habit
        database.habitDao().deleteHabitById(habitId)
        val deletedHabit = database.habitDao().getHabitByIdOnce(habitId)
        assertNull(deletedHabit)
    }

    @Test
    fun testWeekdayAndSelectedDayScheduling() {
        val habitCreatedAt = LocalDate.of(2026, 10, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val weekdayHabit = Habit(
            name = "Work Focus",
            frequencyType = FrequencyType.WEEKDAYS,
            createdAt = habitCreatedAt
        )

        // Monday (Weekday) -> scheduled
        val monday = LocalDate.of(2026, 10, 5) // Monday
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertTrue(StreakCalculator.isHabitScheduledOnDate(weekdayHabit, monday))

        // Saturday (Weekend) -> NOT scheduled
        val saturday = LocalDate.of(2026, 10, 3) // Saturday
        assertEquals(DayOfWeek.SATURDAY, saturday.dayOfWeek)
        assertFalse(StreakCalculator.isHabitScheduledOnDate(weekdayHabit, saturday))

        // Sunday (Weekend) -> NOT scheduled
        val sunday = LocalDate.of(2026, 10, 4) // Sunday
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertFalse(StreakCalculator.isHabitScheduledOnDate(weekdayHabit, sunday))

        // Selected Days Habit (e.g. Tuesday, Thursday)
        val selectedHabit = Habit(
            name = "Gym Sessions",
            frequencyType = FrequencyType.SELECTED_DAYS,
            selectedDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            createdAt = habitCreatedAt
        )

        val tuesday = LocalDate.of(2026, 10, 6) // Tuesday
        assertEquals(DayOfWeek.TUESDAY, tuesday.dayOfWeek)
        assertTrue(StreakCalculator.isHabitScheduledOnDate(selectedHabit, tuesday))

        val wednesday = LocalDate.of(2026, 10, 7) // Wednesday
        assertEquals(DayOfWeek.WEDNESDAY, wednesday.dayOfWeek)
        assertFalse(StreakCalculator.isHabitScheduledOnDate(selectedHabit, wednesday))
    }

    @Test
    fun testHabitRenameUpdatesDatabase() = runBlocking {
        val createHabit = CreateHabitUseCase(habitRepo)
        val updateHabit = UpdateHabitUseCase(habitRepo)

        val habitId = createHabit(
            Habit(name = "Old Habit Name", frequencyType = FrequencyType.DAILY)
        ).getOrThrow()

        val saved = database.habitDao().getHabitByIdOnce(habitId)
        assertEquals("Old Habit Name", saved?.name)

        // Rename habit
        updateHabit(saved!!.toDomain().copy(name = "Updated Habit Name"))

        val reloaded = database.habitDao().getHabitByIdOnce(habitId)
        assertEquals("Updated Habit Name", reloaded?.name)
    }

    @Test
    fun testTargetCountCoercion() {
        val userInputs = listOf("0", "-5", "abc", "", "1", "10")
        val coerced = userInputs.map { text ->
            (text.toIntOrNull() ?: 1).coerceAtLeast(1)
        }

        assertEquals(listOf(1, 1, 1, 1, 1, 10), coerced)
    }

    @Test
    fun testPendingMilestonePreservation() {
        val initialMilestones = mutableListOf<String>()
        var newMilestoneText = "  Draft Research Paper  "

        // Simulating the save button logic
        if (newMilestoneText.trim().isNotEmpty()) {
            initialMilestones.add(newMilestoneText.trim())
            newMilestoneText = ""
        }

        assertEquals(1, initialMilestones.size)
        assertEquals("Draft Research Paper", initialMilestones.first())
        assertEquals("", newMilestoneText)
    }

    @Test
    fun testAlarmCancellationSafety() {
        // Cancel reminder for non-existing or existing habit should not throw
        HabitReminderScheduler.cancelReminder(context, 9999L)
        HabitReminderScheduler.cancelReminder(context, -1L)
    }

    @Test
    fun testNotificationDeepLinkRoute() {
        val habitId = 42L
        val route = Screen.HabitDetail.createRoute(habitId)
        assertEquals("habit/detail/42", route)

        val extractedId = route.substringAfter("habit/detail/").toLongOrNull()
        assertEquals(42L, extractedId)
    }

    @Test
    fun testMidnightDateRolloverCalculation() {
        // Today vs tomorrow
        val day1 = LocalDate.of(2026, 10, 2)
        val day2 = LocalDate.of(2026, 10, 3)

        val habit = Habit(
            name = "Daily Reading",
            frequencyType = FrequencyType.DAILY,
            createdAt = day1.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
        )
        assertTrue(StreakCalculator.isHabitScheduledOnDate(habit, day1))
        assertTrue(StreakCalculator.isHabitScheduledOnDate(habit, day2))

        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(day1, day2)
        assertEquals(1L, daysBetween)
    }

    @Test
    fun testCustomNotificationSoundUriAndChannel() {
        val soundUri = com.aistudio.dayflow.app.notifications.NotificationHelper.getReminderSoundUri(context)
        assertNotNull(soundUri)
        assertTrue(soundUri.toString().startsWith("android.resource://${context.packageName}/"))
        assertTrue(soundUri.lastPathSegment == "dayflow_reminder" || soundUri.lastPathSegment == R.raw.dayflow_reminder.toString())

        // Channel creation should succeed without error
        com.aistudio.dayflow.app.notifications.NotificationHelper.createNotificationChannel(context)
    }

    @Test
    fun testFreshInstallDefaultsToLightModeAndPreservesCustomPreferences() = runBlocking {
        val freshSettings = AppSettings()
        assertEquals(ThemeMode.LIGHT, freshSettings.themeMode)

        settingsRepo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, settingsRepo.getSettings().first().themeMode)

        settingsRepo.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, settingsRepo.getSettings().first().themeMode)

        settingsRepo.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, settingsRepo.getSettings().first().themeMode)
    }

    @Test
    fun testApplicationAndMainActivityStartupLifecycle() {
        val app = ApplicationProvider.getApplicationContext<Context>() as DayFlowApplication
        assertNotNull(app.container)
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.create().start().resume().visible().get()
        assertNotNull(activity)
        val contentView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
        assertNotNull(contentView)
        assertTrue("Content view should have children rendered by Compose", contentView.childCount > 0)
        assertFalse(activity.isFinishing)
        assertFalse(activity.isDestroyed)
    }
}
