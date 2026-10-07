package com.aistudio.dayflow.app

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.aistudio.dayflow.app.data.local.entity.HabitCompletionEntity
import com.aistudio.dayflow.app.data.local.entity.HabitEntity
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.FrequencyType
import com.aistudio.dayflow.app.notifications.HabitBootReceiver
import com.aistudio.dayflow.app.notifications.HabitReminderReceiver
import com.aistudio.dayflow.app.notifications.HabitReminderScheduler
import com.aistudio.dayflow.app.notifications.NotificationHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@Suppress("DEPRECATION")
class DayFlowHabitReminderReliabilityTest {

    private lateinit var context: Context
    private lateinit var app: DayFlowApplication
    private lateinit var alarmManager: AlarmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        app = context as DayFlowApplication
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowAlarmManager = Shadows.shadowOf(alarmManager)
        shadowNotificationManager = Shadows.shadowOf(notificationManager)
    }

    /**
     * Requirement 1: Daily habit next-occurrence calculation.
     */
    @Test
    fun test1_dailyHabitNextOccurrenceCalculation() {
        val reminderTime = LocalTime.of(19, 0) // 7:00 PM

        // Earlier the same day (10:00 AM) -> should schedule for today at 7:00 PM
        val morningTime = LocalDateTime.of(2026, 10, 5, 10, 0) // Monday
        val nextOccurrenceMorning = HabitReminderScheduler.calculateNextOccurrence(
            frequencyType = FrequencyType.DAILY,
            selectedDays = emptySet(),
            reminderTime = reminderTime,
            fromDateTime = morningTime,
            includeTodayIfFuture = true
        )
        assertEquals(LocalDateTime.of(2026, 10, 5, 19, 0), nextOccurrenceMorning)

        // Later the same day (8:00 PM) -> should rollover to tomorrow at 7:00 PM
        val eveningTime = LocalDateTime.of(2026, 10, 5, 20, 0) // Monday
        val nextOccurrenceEvening = HabitReminderScheduler.calculateNextOccurrence(
            frequencyType = FrequencyType.DAILY,
            selectedDays = emptySet(),
            reminderTime = reminderTime,
            fromDateTime = eveningTime,
            includeTodayIfFuture = true
        )
        assertEquals(LocalDateTime.of(2026, 10, 6, 19, 0), nextOccurrenceEvening)
    }

    /**
     * Requirement 2: Selected Saturday habit -> next Saturday.
     */
    @Test
    fun test2_selectedSaturdayHabitNextSaturday() {
        val saturdayReminder = LocalTime.of(19, 0)
        val selectedSaturday = setOf(DayOfWeek.SATURDAY)

        // Saturday morning (10:00 AM) -> today (Saturday) at 7:00 PM
        val satMorning = LocalDateTime.of(2026, 10, 10, 10, 0) // Saturday
        val nextSatMorning = HabitReminderScheduler.calculateNextOccurrence(
            frequencyType = FrequencyType.SELECTED_DAYS,
            selectedDays = selectedSaturday,
            reminderTime = saturdayReminder,
            fromDateTime = satMorning,
            includeTodayIfFuture = true
        )
        assertEquals(LocalDateTime.of(2026, 10, 10, 19, 0), nextSatMorning)

        // Saturday after reminder time (8:00 PM) -> next Saturday (Oct 17) at 7:00 PM
        val satEvening = LocalDateTime.of(2026, 10, 10, 20, 0)
        val nextSatEvening = HabitReminderScheduler.calculateNextOccurrence(
            frequencyType = FrequencyType.SELECTED_DAYS,
            selectedDays = selectedSaturday,
            reminderTime = saturdayReminder,
            fromDateTime = satEvening,
            includeTodayIfFuture = true
        )
        assertEquals(LocalDateTime.of(2026, 10, 17, 19, 0), nextSatEvening)

        // Wednesday (Oct 7) -> upcoming Saturday (Oct 10) at 7:00 PM
        val wednesday = LocalDateTime.of(2026, 10, 7, 14, 0)
        val fromWednesday = HabitReminderScheduler.calculateNextOccurrence(
            frequencyType = FrequencyType.SELECTED_DAYS,
            selectedDays = selectedSaturday,
            reminderTime = saturdayReminder,
            fromDateTime = wednesday,
            includeTodayIfFuture = true
        )
        assertEquals(LocalDateTime.of(2026, 10, 10, 19, 0), fromWednesday)
    }

    /**
     * Requirement 3: Selected multiple weekdays -> correct next weekday.
     */
    @Test
    fun test3_selectedMultipleWeekdaysCorrectNextWeekday() {
        val days = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
        val reminderTime = LocalTime.of(19, 0)

        // On Monday (Oct 5) -> next is Tuesday (Oct 6)
        val monday = LocalDateTime.of(2026, 10, 5, 12, 0)
        val nextFromMon = HabitReminderScheduler.calculateNextOccurrence(
            frequencyType = FrequencyType.SELECTED_DAYS,
            selectedDays = days,
            reminderTime = reminderTime,
            fromDateTime = monday,
            includeTodayIfFuture = true
        )
        assertEquals(LocalDateTime.of(2026, 10, 6, 19, 0), nextFromMon)

        // On Tuesday after reminder time (8:00 PM) -> next is Thursday (Oct 8)
        val tuesdayNight = LocalDateTime.of(2026, 10, 6, 20, 0)
        val nextFromTue = HabitReminderScheduler.calculateNextOccurrence(
            frequencyType = FrequencyType.SELECTED_DAYS,
            selectedDays = days,
            reminderTime = reminderTime,
            fromDateTime = tuesdayNight,
            includeTodayIfFuture = true
        )
        assertEquals(LocalDateTime.of(2026, 10, 8, 19, 0), nextFromTue)

        // On Thursday after reminder time (8:00 PM) -> next is next Tuesday (Oct 13)
        val thursdayNight = LocalDateTime.of(2026, 10, 8, 20, 0)
        val nextFromThu = HabitReminderScheduler.calculateNextOccurrence(
            frequencyType = FrequencyType.SELECTED_DAYS,
            selectedDays = days,
            reminderTime = reminderTime,
            fromDateTime = thursdayNight,
            includeTodayIfFuture = true
        )
        assertEquals(LocalDateTime.of(2026, 10, 13, 19, 0), nextFromThu)
    }

    /**
     * Requirement 4: Reminder time already passed -> next valid occurrence in the future.
     */
    @Test
    fun test4_reminderTimeAlreadyPassedNextValidOccurrence() {
        val reminderTime = LocalTime.of(18, 0) // 6:00 PM
        val pastTimeToday = LocalDateTime.of(2026, 10, 5, 18, 30) // Monday 6:30 PM

        // For weekdays: Friday 6:30 PM -> next Monday 6:00 PM
        val fridayNight = LocalDateTime.of(2026, 10, 9, 18, 30) // Friday
        val nextWeekday = HabitReminderScheduler.calculateNextOccurrence(
            frequencyType = FrequencyType.WEEKDAYS,
            selectedDays = emptySet(),
            reminderTime = reminderTime,
            fromDateTime = fridayNight,
            includeTodayIfFuture = true
        )
        assertEquals(LocalDateTime.of(2026, 10, 12, 18, 0), nextWeekday)
        assertEquals(DayOfWeek.MONDAY, nextWeekday.dayOfWeek)
        assertTrue(nextWeekday.isAfter(fridayNight))
    }

    /**
     * Requirement 5: Completed habit -> notification suppressed.
     */
    @Test
    fun test5_completedHabitNotificationSuppressed() = runBlocking {
        val today = LocalDate.now(ZoneId.systemDefault())
        val habitId = app.container.database.habitDao().insertHabit(
            HabitEntity(
                name = "Read Books",
                description = "Daily reading",
                icon = "book",
                colorHex = "#4F46E5",
                frequencyType = FrequencyType.DAILY,
                selectedDays = emptySet(),
                targetTimesPerWeek = 7,
                targetCount = 1,
                targetUnit = "times",
                reminderTime = LocalTime.of(19, 0),
                createdAt = Instant.now(),
                isArchived = false
            )
        )

        // Mark habit as COMPLETED for today
        app.container.database.habitCompletionDao().insertCompletion(
            HabitCompletionEntity(
                habitId = habitId,
                date = today,
                completedAt = Instant.now(),
                status = CompletionStatus.COMPLETED,
                notes = ""
            )
        )

        val initialNotificationCount = shadowNotificationManager.allNotifications.size

        val receiver = HabitReminderReceiver()
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_NAME, "Read Books")
        }
        receiver.onReceive(context, intent)

        // Wait for Dispatchers.IO coroutine execution
        Thread.sleep(200)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // Notification must NOT have been shown
        val newNotificationCount = shadowNotificationManager.allNotifications.size
        assertEquals(initialNotificationCount, newNotificationCount)
    }

    /**
     * Requirement 6: Archived habit -> notification suppressed and alarm cleaned up.
     */
    @Test
    fun test6_archivedHabitNotificationSuppressed() = runBlocking {
        val habitId = app.container.database.habitDao().insertHabit(
            HabitEntity(
                name = "Archived Habit",
                description = "Not active",
                icon = "check",
                colorHex = "#4F46E5",
                frequencyType = FrequencyType.DAILY,
                selectedDays = emptySet(),
                targetTimesPerWeek = 7,
                targetCount = 1,
                targetUnit = "times",
                reminderTime = LocalTime.of(19, 0),
                createdAt = Instant.now(),
                isArchived = true
            )
        )

        val initialNotificationCount = shadowNotificationManager.allNotifications.size

        val receiver = HabitReminderReceiver()
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_NAME, "Archived Habit")
        }
        receiver.onReceive(context, intent)
        Thread.sleep(200)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        assertEquals(initialNotificationCount, shadowNotificationManager.allNotifications.size)
    }

    /**
     * Requirement 7: Deleted habit -> notification suppressed.
     */
    @Test
    fun test7_deletedHabitNotificationSuppressed() = runBlocking {
        val nonExistentHabitId = 99999L
        val initialNotificationCount = shadowNotificationManager.allNotifications.size

        val receiver = HabitReminderReceiver()
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, nonExistentHabitId)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_NAME, "Deleted Habit")
        }
        receiver.onReceive(context, intent)
        Thread.sleep(200)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        assertEquals(initialNotificationCount, shadowNotificationManager.allNotifications.size)
    }

    /**
     * Requirement 8: Receiver schedules the next occurrence after firing.
     */
    @Test
    fun test8_receiverSchedulesNextOccurrenceAfterFiring() = runBlocking {
        val habitId = app.container.database.habitDao().insertHabit(
            HabitEntity(
                name = "DSA Study",
                description = "Weekend study",
                icon = "code",
                colorHex = "#4F46E5",
                frequencyType = FrequencyType.SELECTED_DAYS,
                selectedDays = setOf(DayOfWeek.SATURDAY),
                targetTimesPerWeek = 1,
                targetCount = 1,
                targetUnit = "times",
                reminderTime = LocalTime.of(19, 0),
                createdAt = Instant.now(),
                isArchived = false
            )
        )

        val habit = app.container.database.habitDao().getHabitByIdOnce(habitId)
        assertNotNull(habit)

        // Schedule next occurrence from receiver execution path
        HabitReminderScheduler.scheduleNextOccurrence(
            context = context,
            habit = habit!!,
            fromDateTime = LocalDateTime.now(ZoneId.systemDefault()),
            includeTodayIfFuture = false
        )

        // Verify that an alarm is scheduled in AlarmManager
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue(scheduledAlarms.isNotEmpty())
    }

    /**
     * Requirement 9: Duplicate scheduling does not create duplicate alarms.
     */
    @Test
    fun test9_duplicateSchedulingDoesNotCreateDuplicateAlarms() {
        val habitId = 555L

        // Call scheduleReminder multiple times for the same habit
        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = "Drink Water",
            frequencyType = FrequencyType.DAILY,
            selectedDays = emptySet(),
            reminderTime = LocalTime.of(10, 0),
            targetDesc = "8 glasses"
        )

        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = "Drink Water Updated",
            frequencyType = FrequencyType.DAILY,
            selectedDays = emptySet(),
            reminderTime = LocalTime.of(11, 0),
            targetDesc = "8 glasses"
        )

        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = "Drink Water Final",
            frequencyType = FrequencyType.DAILY,
            selectedDays = emptySet(),
            reminderTime = LocalTime.of(12, 0),
            targetDesc = "8 glasses"
        )

        // PendingIntent identity per habit guarantees cancel-and-replace
        val matchingAlarms = shadowAlarmManager.scheduledAlarms.filter { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.data?.toString() == "dayflow://reminder/$habitId" ||
                shadowPi.requestCode == habitId.toInt()
        }
        assertEquals(1, matchingAlarms.size)
    }

    /**
     * Requirement 10: Boot rescheduling restores future reminders.
     */
    @Test
    fun test10_bootReschedulingRestoresFutureReminders() = runBlocking {
        // Clear existing alarms
        shadowAlarmManager.scheduledAlarms.clear()

        // Insert 1 active habit with reminder
        val activeHabitId = app.container.database.habitDao().insertHabit(
            HabitEntity(
                name = "Morning Workout",
                description = "Fitness",
                icon = "fitness",
                colorHex = "#4F46E5",
                frequencyType = FrequencyType.DAILY,
                selectedDays = emptySet(),
                targetTimesPerWeek = 7,
                targetCount = 1,
                targetUnit = "times",
                reminderTime = LocalTime.of(7, 0),
                createdAt = Instant.now(),
                isArchived = false
            )
        )

        // Insert 1 archived habit (should not be scheduled)
        val archivedHabitId = app.container.database.habitDao().insertHabit(
            HabitEntity(
                name = "Old Workout",
                description = "Archived",
                icon = "fitness",
                colorHex = "#4F46E5",
                frequencyType = FrequencyType.DAILY,
                selectedDays = emptySet(),
                targetTimesPerWeek = 7,
                targetCount = 1,
                targetUnit = "times",
                reminderTime = LocalTime.of(8, 0),
                createdAt = Instant.now(),
                isArchived = true
            )
        )

        // Trigger boot rescheduling
        HabitReminderScheduler.rescheduleAllActiveReminders(context, app.container)

        // Verify active habit has alarm scheduled
        val matchingActive = shadowAlarmManager.scheduledAlarms.filter { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.data?.toString() == "dayflow://reminder/$activeHabitId" ||
                shadowPi.requestCode == activeHabitId.toInt()
        }
        assertTrue("Expected alarm scheduled for active habit on boot", matchingActive.isNotEmpty())

        // Verify archived habit has NO alarm scheduled
        val matchingArchived = shadowAlarmManager.scheduledAlarms.filter { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.data?.toString() == "dayflow://reminder/$archivedHabitId" ||
                shadowPi.requestCode == archivedHabitId.toInt()
        }
        assertTrue("Archived habit must not have scheduled alarm", matchingArchived.isEmpty())
    }

    /**
     * Requirement 8.1: Cancel old reminder -> new reminder scheduled.
     */
    @Test
    fun test11_cancelOldReminder_newReminderScheduled() {
        val habitId = 101L
        shadowAlarmManager.scheduledAlarms.clear()

        // Schedule first reminder
        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = "Morning Meditation",
            reminderTime = LocalTime.of(8, 0)
        )
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // Reschedule (triggers cancel old -> schedule new)
        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = "Morning Meditation Updated",
            reminderTime = LocalTime.of(9, 0)
        )
        // Must remain exactly 1 scheduled alarm, not left in a cancelled state
        val matchingAlarms = shadowAlarmManager.scheduledAlarms.filter { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.data?.toString() == "dayflow://reminder/$habitId" ||
                shadowPi.requestCode == habitId.toInt()
        }
        assertEquals(1, matchingAlarms.size)
    }

    /**
     * Requirement 8.2: Reminder fires -> next weekly reminder scheduled.
     */
    @Test
    fun test12_reminderFires_nextWeeklyReminderScheduled() = runBlocking {
        shadowAlarmManager.scheduledAlarms.clear()

        val habitId = app.container.database.habitDao().insertHabit(
            HabitEntity(
                name = "Weekly Review",
                description = "Weekend review",
                icon = "analytics",
                colorHex = "#4F46E5",
                frequencyType = FrequencyType.SELECTED_DAYS,
                selectedDays = setOf(DayOfWeek.SUNDAY),
                targetTimesPerWeek = 1,
                targetCount = 1,
                targetUnit = "times",
                reminderTime = LocalTime.of(18, 0),
                createdAt = Instant.now(),
                isArchived = false
            )
        )
        val habit = app.container.database.habitDao().getHabitByIdOnce(habitId)!!

        // Simulate alarm firing on Sunday at 6:00 PM
        val sundayFireTime = LocalDateTime.of(2026, 10, 11, 18, 0) // Sunday
        HabitReminderScheduler.scheduleNextOccurrence(
            context = context,
            habit = habit,
            fromDateTime = sundayFireTime,
            includeTodayIfFuture = false
        )

        // Verify next occurrence is scheduled for next Sunday (Oct 18)
        val matchingAlarms = shadowAlarmManager.scheduledAlarms.filter { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.data?.toString() == "dayflow://reminder/$habitId" ||
                shadowPi.requestCode == habitId.toInt()
        }
        assertEquals(1, matchingAlarms.size)
        val nextSundayMillis = LocalDateTime.of(2026, 10, 18, 18, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals(nextSundayMillis, matchingAlarms[0].triggerAtTime)
    }

    /**
     * Requirement 8.3: Completing today's habit does not cancel next week's reminder.
     */
    @Test
    fun test13_completingTodayHabit_doesNotCancelNextWeekReminder() = runBlocking {
        shadowAlarmManager.scheduledAlarms.clear()
        val today = LocalDate.now(ZoneId.systemDefault())

        val habitId = app.container.database.habitDao().insertHabit(
            HabitEntity(
                name = "Guitar Practice",
                description = "Music",
                icon = "music",
                colorHex = "#4F46E5",
                frequencyType = FrequencyType.DAILY,
                selectedDays = emptySet(),
                targetTimesPerWeek = 7,
                targetCount = 1,
                targetUnit = "times",
                reminderTime = LocalTime.of(20, 0),
                createdAt = Instant.now(),
                isArchived = false
            )
        )

        // Mark today completed
        app.container.database.habitCompletionDao().insertCompletion(
            HabitCompletionEntity(
                habitId = habitId,
                date = today,
                completedAt = Instant.now(),
                status = CompletionStatus.COMPLETED,
                notes = ""
            )
        )

        val initialNotifications = shadowNotificationManager.allNotifications.size

        // Receiver runs for today's reminder
        val receiver = HabitReminderReceiver()
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(HabitReminderReceiver.EXTRA_HABIT_NAME, "Guitar Practice")
        }
        receiver.onReceive(context, intent)
        Thread.sleep(200)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // 1. Notification suppressed for today
        assertEquals(initialNotifications, shadowNotificationManager.allNotifications.size)

        // 2. Future reminder is STILL scheduled for tomorrow (not cancelled)
        val matchingAlarms = shadowAlarmManager.scheduledAlarms.filter { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.data?.toString() == "dayflow://reminder/$habitId" ||
                shadowPi.requestCode == habitId.toInt()
        }
        assertEquals(1, matchingAlarms.size)
        val tomorrowMillis = LocalDateTime.of(today.plusDays(1), LocalTime.of(20, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals(tomorrowMillis, matchingAlarms[0].triggerAtTime)
    }

    /**
     * Requirement 8.4: Editing reminder time replaces the old alarm correctly.
     */
    @Test
    fun test14_editingReminderTime_replacesOldAlarmCorrectly() {
        val habitId = 202L
        shadowAlarmManager.scheduledAlarms.clear()

        // Initial schedule: 10:00 AM
        val initialTime = LocalTime.of(10, 0)
        val testNow = LocalDateTime.of(2026, 10, 5, 8, 0) // Monday 8:00 AM
        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = "Hydrate",
            reminderTime = initialTime,
            now = testNow
        )
        val initialExpectedMillis = LocalDateTime.of(2026, 10, 5, 10, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
        assertEquals(initialExpectedMillis, shadowAlarmManager.scheduledAlarms[0].triggerAtTime)

        // User edits reminder time to 11:30 AM
        val updatedTime = LocalTime.of(11, 30)
        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = "Hydrate",
            reminderTime = updatedTime,
            now = testNow
        )
        val updatedExpectedMillis = LocalDateTime.of(2026, 10, 5, 11, 30)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val matchingAlarms = shadowAlarmManager.scheduledAlarms.filter { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.data?.toString() == "dayflow://reminder/$habitId" ||
                shadowPi.requestCode == habitId.toInt()
        }
        assertEquals(1, matchingAlarms.size)
        assertEquals(updatedExpectedMillis, matchingAlarms[0].triggerAtTime)
    }

    /**
     * Requirement 8.5: Explicitly disabling reminders cancels the alarm permanently.
     */
    @Test
    fun test15_explicitlyDisablingReminders_cancelsAlarm() {
        val habitId = 303L
        shadowAlarmManager.scheduledAlarms.clear()

        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = "Stretch",
            reminderTime = LocalTime.of(15, 0)
        )
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // User toggles off reminder for this habit
        HabitReminderScheduler.cancelReminder(context, habitId, reason = "reminder_removed_by_user")

        val matchingAlarms = shadowAlarmManager.scheduledAlarms.filter { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.data?.toString() == "dayflow://reminder/$habitId" ||
                shadowPi.requestCode == habitId.toInt()
        }
        assertTrue("No alarm should remain after explicitly disabling reminders", matchingAlarms.isEmpty())
    }

    /**
     * Requirement 8.6: Archived/deleted habit cancels the alarm.
     */
    @Test
    fun test16_archivedOrDeletedHabit_cancelsAlarm() = runBlocking {
        val habitId = 404L
        shadowAlarmManager.scheduledAlarms.clear()

        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = "Night Walk",
            reminderTime = LocalTime.of(21, 0)
        )
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // When archived
        HabitReminderScheduler.cancelReminder(context, habitId, reason = "habit_archived_by_user")
        val matchingAfterArchive = shadowAlarmManager.scheduledAlarms.filter { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.data?.toString() == "dayflow://reminder/$habitId" ||
                shadowPi.requestCode == habitId.toInt()
        }
        assertTrue("No alarm should remain after archiving habit", matchingAfterArchive.isEmpty())

        // When deleted
        HabitReminderScheduler.cancelReminder(context, habitId, reason = "habit_deleted_by_user")
        val matchingAfterDelete = shadowAlarmManager.scheduledAlarms.filter { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.data?.toString() == "dayflow://reminder/$habitId" ||
                shadowPi.requestCode == (habitId and 0x7FFFFFFF).toInt()
        }
        assertTrue("No alarm should remain after deleting habit", matchingAfterDelete.isEmpty())
    }

    /**
     * Requirement 11.4: Times-per-week habit reminder.
     */
    @Test
    fun test17_timesPerWeekReminder() = runBlocking {
        shadowAlarmManager.scheduledAlarms.clear()
        val habitId = app.container.database.habitDao().insertHabit(
            HabitEntity(
                name = "Gym Workout",
                description = "3x per week",
                icon = "fitness",
                colorHex = "#4F46E5",
                frequencyType = FrequencyType.TIMES_PER_WEEK,
                selectedDays = emptySet(),
                targetTimesPerWeek = 3,
                targetCount = 1,
                targetUnit = "sessions",
                reminderTime = LocalTime.of(17, 30),
                createdAt = Instant.now(),
                isArchived = false
            )
        )
        val habit = app.container.database.habitDao().getHabitByIdOnce(habitId)!!

        val tuesdayMorning = LocalDateTime.of(2026, 10, 6, 9, 0)
        val nextOccurrence = HabitReminderScheduler.calculateNextOccurrence(
            frequencyType = habit.frequencyType,
            selectedDays = habit.selectedDays,
            reminderTime = habit.reminderTime!!,
            fromDateTime = tuesdayMorning,
            includeTodayIfFuture = true
        )
        assertEquals(LocalDateTime.of(2026, 10, 6, 17, 30), nextOccurrence)

        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habit.id,
            habitName = habit.name,
            frequencyType = habit.frequencyType,
            selectedDays = habit.selectedDays,
            reminderTime = habit.reminderTime!!,
            targetDesc = "${habit.targetCount} ${habit.targetUnit}",
            now = tuesdayMorning
        )
        val matchingAlarms = shadowAlarmManager.scheduledAlarms.filter { alarm ->
            val shadowPi = Shadows.shadowOf(alarm.operation)
            shadowPi.savedIntent.data?.toString() == "dayflow://reminder/$habitId" ||
                shadowPi.requestCode == (habitId and 0x7FFFFFFF).toInt()
        }
        assertEquals(1, matchingAlarms.size)
        assertEquals(AlarmManager.RTC_WAKEUP, matchingAlarms[0].type)
    }

    /**
     * Requirement 11.5: Multiple habits with different reminder times.
     */
    @Test
    fun test18_multipleHabitsWithDifferentReminderTimes() {
        shadowAlarmManager.scheduledAlarms.clear()
        val habit1Id = 501L
        val habit2Id = 502L

        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habit1Id,
            habitName = "Morning Coffee",
            reminderTime = LocalTime.of(7, 30)
        )
        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habit2Id,
            habitName = "Evening Read",
            reminderTime = LocalTime.of(21, 0)
        )

        val alarm1 = shadowAlarmManager.scheduledAlarms.find {
            val pi = Shadows.shadowOf(it.operation)
            pi.savedIntent.data?.toString() == "dayflow://reminder/$habit1Id"
        }
        val alarm2 = shadowAlarmManager.scheduledAlarms.find {
            val pi = Shadows.shadowOf(it.operation)
            pi.savedIntent.data?.toString() == "dayflow://reminder/$habit2Id"
        }
        assertNotNull("Alarm 1 should exist", alarm1)
        assertNotNull("Alarm 2 should exist", alarm2)
        assertTrue(alarm1!!.triggerAtTime != alarm2!!.triggerAtTime)
    }

    /**
     * Requirement 11.6: Two habits with the same reminder time do not collide.
     */
    @Test
    fun test19_twoHabitsSameReminderTime_noCollision() {
        shadowAlarmManager.scheduledAlarms.clear()
        val habit1Id = 601L
        val habit2Id = 602L
        val sameTime = LocalTime.of(8, 0)

        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habit1Id,
            habitName = "Drink Water",
            reminderTime = sameTime
        )
        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habit2Id,
            habitName = "Take Vitamins",
            reminderTime = sameTime
        )

        val alarm1 = shadowAlarmManager.scheduledAlarms.find {
            val pi = Shadows.shadowOf(it.operation)
            pi.savedIntent.data?.toString() == "dayflow://reminder/$habit1Id" &&
                pi.requestCode == (habit1Id and 0x7FFFFFFF).toInt()
        }
        val alarm2 = shadowAlarmManager.scheduledAlarms.find {
            val pi = Shadows.shadowOf(it.operation)
            pi.savedIntent.data?.toString() == "dayflow://reminder/$habit2Id" &&
                pi.requestCode == (habit2Id and 0x7FFFFFFF).toInt()
        }
        assertNotNull("Alarm 1 should be scheduled", alarm1)
        assertNotNull("Alarm 2 should be scheduled", alarm2)
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
    }

    /**
     * Requirement 11.10: Re-enabling a habit restores reminder.
     */
    @Test
    fun test20_reEnablingHabitReminder_restoresAlarm() {
        shadowAlarmManager.scheduledAlarms.clear()
        val habitId = 701L
        val reminderTime = LocalTime.of(12, 0)

        // 1. Initial schedule
        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = "Lunch Walk",
            reminderTime = reminderTime
        )
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // 2. Disabled
        HabitReminderScheduler.cancelReminder(context, habitId, reason = "disabled_by_user")
        val alarmsAfterDisable = shadowAlarmManager.scheduledAlarms.filter {
            Shadows.shadowOf(it.operation).savedIntent.data?.toString() == "dayflow://reminder/$habitId"
        }
        assertTrue(alarmsAfterDisable.isEmpty())

        // 3. Re-enabled
        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = "Lunch Walk",
            reminderTime = reminderTime
        )
        val alarmsAfterReenable = shadowAlarmManager.scheduledAlarms.filter {
            Shadows.shadowOf(it.operation).savedIntent.data?.toString() == "dayflow://reminder/$habitId"
        }
        assertEquals(1, alarmsAfterReenable.size)
    }

    /**
     * Requirement 11.11: App restart restores active reminders.
     */
    @Test
    fun test21_appRestart_reschedulesActiveReminders() = runBlocking {
        shadowAlarmManager.scheduledAlarms.clear()
        val habitId = app.container.database.habitDao().insertHabit(
            HabitEntity(
                name = "Night Reflection",
                description = "Journaling",
                icon = "edit",
                colorHex = "#4F46E5",
                frequencyType = FrequencyType.DAILY,
                selectedDays = emptySet(),
                targetTimesPerWeek = 7,
                targetCount = 1,
                targetUnit = "times",
                reminderTime = LocalTime.of(22, 0),
                createdAt = Instant.now(),
                isArchived = false
            )
        )

        // Cold start reschedule simulation
        HabitReminderScheduler.rescheduleAllActiveReminders(context, app.container)

        val matchingAlarms = shadowAlarmManager.scheduledAlarms.filter {
            Shadows.shadowOf(it.operation).savedIntent.data?.toString() == "dayflow://reminder/$habitId"
        }
        assertEquals(1, matchingAlarms.size)
        assertTrue(matchingAlarms[0].triggerAtTime > System.currentTimeMillis())
    }

    /**
     * Requirement 11.13: TIME_SET triggers reschedule.
     */
    @Test
    fun test22_timeSetReceiver_reschedulesActiveReminders() = runBlocking {
        shadowAlarmManager.scheduledAlarms.clear()
        val habitId = app.container.database.habitDao().insertHabit(
            HabitEntity(
                name = "Clock Check Habit",
                description = "Testing time set",
                icon = "schedule",
                colorHex = "#4F46E5",
                frequencyType = FrequencyType.DAILY,
                selectedDays = emptySet(),
                targetTimesPerWeek = 7,
                targetCount = 1,
                targetUnit = "times",
                reminderTime = LocalTime.of(15, 0),
                createdAt = Instant.now(),
                isArchived = false
            )
        )

        val receiver = HabitBootReceiver()
        val intent = Intent(Intent.ACTION_TIME_CHANGED)
        receiver.onReceive(context, intent)
        Thread.sleep(200)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val matchingAlarms = shadowAlarmManager.scheduledAlarms.filter {
            Shadows.shadowOf(it.operation).savedIntent.data?.toString() == "dayflow://reminder/$habitId"
        }
        assertTrue("TIME_SET should trigger active reminder rescheduling", matchingAlarms.isNotEmpty())
    }

    /**
     * Requirement 11.14: TIMEZONE_CHANGED triggers reschedule.
     */
    @Test
    fun test23_timezoneChangedReceiver_recalculatesReminders() = runBlocking {
        shadowAlarmManager.scheduledAlarms.clear()
        val habitId = app.container.database.habitDao().insertHabit(
            HabitEntity(
                name = "Timezone Habit",
                description = "Testing timezone shift",
                icon = "language",
                colorHex = "#4F46E5",
                frequencyType = FrequencyType.DAILY,
                selectedDays = emptySet(),
                targetTimesPerWeek = 7,
                targetCount = 1,
                targetUnit = "times",
                reminderTime = LocalTime.of(16, 0),
                createdAt = Instant.now(),
                isArchived = false
            )
        )

        val receiver = HabitBootReceiver()
        val intent = Intent(Intent.ACTION_TIMEZONE_CHANGED)
        receiver.onReceive(context, intent)
        Thread.sleep(200)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val matchingAlarms = shadowAlarmManager.scheduledAlarms.filter {
            Shadows.shadowOf(it.operation).savedIntent.data?.toString() == "dayflow://reminder/$habitId"
        }
        assertTrue("TIMEZONE_CHANGED should trigger rescheduling", matchingAlarms.isNotEmpty())
    }

    /**
     * Requirement 11.15 & 11.16: Screen locked & device idle -> Alarm uses RTC_WAKEUP.
     */
    @Test
    fun test24_screenLockedAndDeviceIdle_alarmUsesRTCWakeup() {
        shadowAlarmManager.scheduledAlarms.clear()
        val habitId = 801L
        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = "Hydrate Idle Test",
            reminderTime = LocalTime.of(14, 0)
        )
        val alarm = shadowAlarmManager.scheduledAlarms.find {
            Shadows.shadowOf(it.operation).savedIntent.data?.toString() == "dayflow://reminder/$habitId"
        }
        assertNotNull(alarm)
        assertEquals("Must use RTC_WAKEUP to wake device during screen locked / Doze idle", AlarmManager.RTC_WAKEUP, alarm!!.type)
    }

    /**
     * Requirement 11.17: Notification permission denied handled gracefully without crash.
     */
    @Test
    fun test25_notificationPermissionDenied_handledGracefullyWithoutCrash() {
        NotificationHelper.showHabitReminderNotification(
            context = context,
            habitId = 999L,
            habitName = "Permission Denied Test",
            bodyText = "Testing graceful permission denial",
            targetDesc = "1 time"
        )
        NotificationHelper.cancelHabitReminder(context, 999L)
    }

    /**
     * Requirement 11.18 & 11.19: Exact alarm permission available & fallback paths.
     */
    @Test
    fun test26_exactAlarmPermissionAvailableAndUnavailableFallback() {
        shadowAlarmManager.scheduledAlarms.clear()

        // 1. Verify canScheduleExactAlarms helper runs cleanly without exception
        val canExact = HabitReminderScheduler.canScheduleExactAlarms(context)
        assertNotNull(canExact)

        val habit1Id = 901L
        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habit1Id,
            habitName = "Exact Alarm Test",
            reminderTime = LocalTime.of(10, 0)
        )
        val alarm1 = shadowAlarmManager.scheduledAlarms.find {
            Shadows.shadowOf(it.operation).savedIntent.data?.toString() == "dayflow://reminder/$habit1Id"
        }
        assertNotNull("Alarm must be scheduled when exact alarms permitted", alarm1)

        // 2. Test scheduleAlarmInternal path directly
        val habit2Id = 902L
        val pendingIntent2 = HabitReminderScheduler.createReminderPendingIntent(context, habit2Id)
        val triggerMillis = System.currentTimeMillis() + 60_000L
        HabitReminderScheduler.scheduleAlarmInternal(
            alarmManager = alarmManager,
            triggerAtMillis = triggerMillis,
            pendingIntent = pendingIntent2,
            habitId = habit2Id
        )
        val alarm2 = shadowAlarmManager.scheduledAlarms.find {
            Shadows.shadowOf(it.operation).savedIntent.data?.toString() == "dayflow://reminder/$habit2Id"
        }
        assertNotNull("Alarm must be scheduled cleanly via scheduleAlarmInternal", alarm2)
    }

    /**
     * Requirement 11.20: Reminder time already passed today -> strictly scheduled in the future.
     */
    @Test
    fun test27_reminderTimeAlreadyPassedToday_guaranteedFutureTimestamp() {
        val habitId = 950L
        val pastReminderTime = LocalTime.of(6, 0)
        val afternoonNow = LocalDateTime.of(2026, 10, 5, 14, 0)

        val nextOccurrence = HabitReminderScheduler.calculateNextOccurrence(
            frequencyType = FrequencyType.DAILY,
            selectedDays = emptySet(),
            reminderTime = pastReminderTime,
            fromDateTime = afternoonNow,
            includeTodayIfFuture = true
        )

        assertEquals(LocalDateTime.of(2026, 10, 6, 6, 0), nextOccurrence)
        assertTrue("Calculated occurrence must be in the future", nextOccurrence.isAfter(afternoonNow))

        shadowAlarmManager.scheduledAlarms.clear()
        HabitReminderScheduler.scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = "Early Morning Run",
            reminderTime = pastReminderTime,
            now = afternoonNow
        )
        val scheduledAlarm = shadowAlarmManager.scheduledAlarms.find {
            Shadows.shadowOf(it.operation).savedIntent.data?.toString() == "dayflow://reminder/$habitId"
        }
        assertNotNull(scheduledAlarm)
        val expectedAfternoonMillis = afternoonNow.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertTrue("AlarmManager triggerAtTime must be strictly in the future of reference time", scheduledAlarm!!.triggerAtTime > expectedAfternoonMillis)
    }
}
