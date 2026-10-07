package com.aistudio.dayflow.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.aistudio.dayflow.app.DayFlowApplication
import com.aistudio.dayflow.app.data.local.entity.HabitEntity
import com.aistudio.dayflow.app.di.AppContainer
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.FrequencyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId <= 0L) return

        val fallbackHabitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "Habit"
        val targetDesc = intent.getStringExtra(EXTRA_TARGET_DESC) ?: ""

        val app = context.applicationContext as? DayFlowApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Verify habit still exists in the local database
                val habit = app.container.database.habitDao().getHabitByIdOnce(habitId)
                if (habit == null) {
                    HabitReminderScheduler.cancelReminder(context, habitId, reason = "habit_not_found")
                    return@launch
                }

                // 2. Verify habit is active and reminder is still configured
                if (habit.isArchived || habit.reminderTime == null) {
                    HabitReminderScheduler.cancelReminder(
                        context,
                        habitId,
                        reason = if (habit.isArchived) "habit_archived" else "reminder_disabled"
                    )
                    return@launch
                }

                // 3. Verify notifications are enabled globally
                val settings = app.container.settingsRepository.getSettings().first()
                val notificationsEnabled = settings.notificationsEnabled

                // 4. Verify today is actually a scheduled day for this habit
                val today = LocalDate.now(ZoneId.systemDefault())
                val isScheduledToday = HabitReminderScheduler.isHabitScheduledOn(
                    frequencyType = habit.frequencyType,
                    selectedDays = habit.selectedDays,
                    dayOfWeek = today.dayOfWeek
                )

                // 5. Verify today's occurrence is not already completed
                val completion = app.container.database.habitCompletionDao().getCompletion(habitId, today)
                val isCompletedToday = completion != null && completion.status == CompletionStatus.COMPLETED

                // 5b. For TIMES_PER_WEEK habits, verify weekly target has not already been achieved this week
                val isWeeklyTargetMet = if (habit.frequencyType == FrequencyType.TIMES_PER_WEEK) {
                    val weekStartDay = if (settings.weekStart == com.aistudio.dayflow.app.domain.model.WeekStart.SUNDAY) DayOfWeek.SUNDAY else DayOfWeek.MONDAY
                    val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(weekStartDay))
                    val endOfWeek = startOfWeek.plusDays(6)
                    val countThisWeek = app.container.database.habitCompletionDao().getCompletedCountForHabitInRange(
                        habitId = habitId,
                        startDate = startOfWeek,
                        endDate = endOfWeek
                    )
                    countThisWeek >= habit.targetTimesPerWeek
                } else {
                    false
                }

                // 6. Show notification only if ALL checks pass
                if (notificationsEnabled && isScheduledToday && !isCompletedToday && !isWeeklyTargetMet) {
                    val dayName = today.dayOfWeek.getDisplayName(
                        java.time.format.TextStyle.FULL,
                        java.util.Locale.getDefault()
                    )
                    val bodyText = when (habit.frequencyType) {
                        FrequencyType.SELECTED_DAYS -> "Your $dayName habit is ready."
                        FrequencyType.DAILY -> "Your daily habit is ready."
                        FrequencyType.WEEKDAYS -> "Your $dayName habit is ready."
                        FrequencyType.TIMES_PER_WEEK -> "Your $dayName habit is ready."
                    }
                    NotificationHelper.showHabitReminderNotification(
                        context = context,
                        habitId = habitId,
                        habitName = habit.name.ifBlank { fallbackHabitName },
                        bodyText = bodyText,
                        targetDesc = targetDesc.ifBlank { "${habit.targetCount} ${habit.targetUnit}" }
                    )
                }

                // 7. Immediately schedule the NEXT occurrence automatically
                // For daily habits: tomorrow. For selected days: next matching weekday.
                HabitReminderScheduler.scheduleNextOccurrence(
                    context = context,
                    habit = habit,
                    fromDateTime = LocalDateTime.of(today, habit.reminderTime),
                    includeTodayIfFuture = false
                )
            } catch (e: Throwable) {
                // Defensive: ensure background work never crashes receiver process
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "EXTRA_HABIT_ID"
        const val EXTRA_HABIT_NAME = "EXTRA_HABIT_NAME"
        const val EXTRA_TARGET_DESC = "EXTRA_TARGET_DESC"
    }
}

object HabitReminderScheduler {

    const val ACTION_HABIT_REMINDER = "com.aistudio.dayflow.app.action.HABIT_REMINDER"

    /**
     * Determines whether a habit is scheduled on a given day of the week.
     */
    fun isHabitScheduledOn(
        frequencyType: FrequencyType,
        selectedDays: Set<DayOfWeek>,
        dayOfWeek: DayOfWeek
    ): Boolean {
        return when (frequencyType) {
            FrequencyType.DAILY -> true
            FrequencyType.WEEKDAYS -> dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
            FrequencyType.SELECTED_DAYS -> selectedDays.contains(dayOfWeek)
            FrequencyType.TIMES_PER_WEEK -> true
        }
    }

    /**
     * Calculates the next matching occurrence for a habit.
     * Handles weekdays, selected days, past-time rollover, midnight rollover, and weekly recurrence.
     */
    fun calculateNextOccurrence(
        frequencyType: FrequencyType,
        selectedDays: Set<DayOfWeek>,
        reminderTime: LocalTime,
        fromDateTime: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()),
        includeTodayIfFuture: Boolean = true
    ): LocalDateTime {
        val today = fromDateTime.toLocalDate()
        val currentTime = fromDateTime.toLocalTime()

        // If today is a valid scheduled day and reminderTime is in the future
        if (includeTodayIfFuture &&
            isHabitScheduledOn(frequencyType, selectedDays, today.dayOfWeek) &&
            reminderTime.isAfter(currentTime)
        ) {
            return LocalDateTime.of(today, reminderTime)
        }

        // Search the next 1 to 7 days for the next valid occurrence
        for (dayOffset in 1..7) {
            val candidateDate = today.plusDays(dayOffset.toLong())
            if (isHabitScheduledOn(frequencyType, selectedDays, candidateDate.dayOfWeek)) {
                return LocalDateTime.of(candidateDate, reminderTime)
            }
        }

        // Defensive fallback
        return LocalDateTime.of(today.plusDays(1), reminderTime)
    }

    /**
     * Creates a deterministic PendingIntent for this habit's alarm.
     */
    fun createReminderPendingIntent(
        context: Context,
        habitId: Long,
        habitName: String = "",
        targetDesc: String = "",
        flags: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    ): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = ACTION_HABIT_REMINDER
            data = Uri.parse("dayflow://reminder/$habitId")
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            if (habitName.isNotEmpty()) {
                putExtra(HabitReminderReceiver.EXTRA_HABIT_NAME, habitName)
            }
            if (targetDesc.isNotEmpty()) {
                putExtra(HabitReminderReceiver.EXTRA_TARGET_DESC, targetDesc)
            }
        }
        val requestCode = (habitId and 0x7FFFFFFF).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )
    }

    private const val TAG = "DayFlowReminder"

    /**
     * Checks if exact alarm scheduling is permitted on this device.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                alarmManager.canScheduleExactAlarms()
            } catch (e: Throwable) {
                false
            }
        } else {
            true
        }
    }

    /**
     * Internal helper to schedule an alarm using exact timing.
     * Uses setExactAndAllowWhileIdle for exact timing across idle/Doze modes.
     * If exact alarm permission is unavailable, no inexact alarm is scheduled;
     * reminders are scheduled/rescheduled once exact alarm access is granted.
     */
    internal fun scheduleAlarmInternal(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        habitId: Long
    ) {
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                alarmManager.canScheduleExactAlarms()
            } catch (e: Throwable) {
                false
            }
        } else {
            true
        }

        if (!canScheduleExact) {
            Log.w(TAG, "Exact alarm permission is unavailable for habitId=$habitId; reminder not scheduled until permission is granted")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Exact alarm scheduled (RTC_WAKEUP) for habitId=$habitId at millis=$triggerAtMillis")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission denied (SecurityException) for habitId=$habitId: ${e.message}")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to schedule exact alarm for habitId=$habitId: ${e.message}")
        }
    }

    /**
     * Cancels any previously scheduled AlarmManager reminder for this habit.
     */
    fun cancelReminder(context: Context, habitId: Long, reason: String = "unspecified") {
        try {
            Log.d(TAG, "CANCEL reminder habitId=$habitId reason=$reason")
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pendingIntent = createReminderPendingIntent(
                context = context,
                habitId = habitId,
                flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to cancel reminder habitId=$habitId reason=$reason: ${e.message}")
        }
    }

    /**
     * Schedules the next reminder occurrence using AlarmManager.RTC_WAKEUP.
     * Prevents duplicate alarms by cancelling previous PendingIntents first.
     * Ensures triggers are strictly in the future.
     */
    fun scheduleReminder(
        context: Context,
        habitId: Long,
        habitName: String,
        frequencyType: FrequencyType = FrequencyType.DAILY,
        selectedDays: Set<DayOfWeek> = emptySet(),
        reminderTime: LocalTime,
        targetDesc: String = "",
        now: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault())
    ) {
        // Step 1: Cancel any previous pending intent for this habit to guarantee no duplicate alarms
        cancelReminder(context, habitId, reason = "replace_before_schedule")

        // If frequency is SELECTED_DAYS but no days are selected, do not schedule any alarm
        if (frequencyType == FrequencyType.SELECTED_DAYS && selectedDays.isEmpty()) {
            Log.d(TAG, "Habit has SELECTED_DAYS with no days selected; reminder not scheduled")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Step 2: Calculate the next valid occurrence
        val nextOccurrence = calculateNextOccurrence(
            frequencyType = frequencyType,
            selectedDays = selectedDays,
            reminderTime = reminderTime,
            fromDateTime = now,
            includeTodayIfFuture = true
        )

        // Step 3: Convert to trigger epoch milliseconds using device local timezone
        var triggerAtMillis = nextOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Safety: Guarantee triggerAtMillis is strictly in the future compared to reference now
        val referenceNowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerAtMillis <= referenceNowMillis) {
            val futureNow = now.plusSeconds(2)
            val futureOccurrence = calculateNextOccurrence(
                frequencyType = frequencyType,
                selectedDays = selectedDays,
                reminderTime = reminderTime,
                fromDateTime = futureNow,
                includeTodayIfFuture = false
            )
            triggerAtMillis = futureOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        // Step 4: Create deterministic PendingIntent
        val pendingIntent = createReminderPendingIntent(
            context = context,
            habitId = habitId,
            habitName = habitName,
            targetDesc = targetDesc,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Step 5: Schedule via exact alarm with fallback
        scheduleAlarmInternal(alarmManager, triggerAtMillis, pendingIntent, habitId)
        Log.d(TAG, "SCHEDULE reminder habitId=$habitId triggerAt=$nextOccurrence")
    }

    /**
     * Backwards-compatible overload for existing 5-parameter calls.
     */
    fun scheduleReminder(
        context: Context,
        habitId: Long,
        habitName: String,
        reminderTime: LocalTime,
        targetDesc: String = ""
    ) {
        scheduleReminder(
            context = context,
            habitId = habitId,
            habitName = habitName,
            frequencyType = FrequencyType.DAILY,
            selectedDays = emptySet(),
            reminderTime = reminderTime,
            targetDesc = targetDesc
        )
    }

    /**
     * Automatically schedules the next occurrence after a reminder fires.
     */
    fun scheduleNextOccurrence(
        context: Context,
        habit: HabitEntity,
        fromDateTime: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()),
        includeTodayIfFuture: Boolean = false
    ) {
        val reminderTime = habit.reminderTime ?: return
        if (habit.isArchived) return
        if (habit.frequencyType == FrequencyType.SELECTED_DAYS && habit.selectedDays.isEmpty()) return

        cancelReminder(context, habit.id, reason = "replace_before_next_occurrence")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val nextOccurrence = calculateNextOccurrence(
            frequencyType = habit.frequencyType,
            selectedDays = habit.selectedDays,
            reminderTime = reminderTime,
            fromDateTime = fromDateTime,
            includeTodayIfFuture = includeTodayIfFuture
        )

        var triggerAtMillis = nextOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Safety: Guarantee triggerAtMillis is strictly in the future compared to reference fromDateTime
        val referenceNowMillis = fromDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerAtMillis <= referenceNowMillis) {
            val futureNow = fromDateTime.plusSeconds(2)
            val futureOccurrence = calculateNextOccurrence(
                frequencyType = habit.frequencyType,
                selectedDays = habit.selectedDays,
                reminderTime = reminderTime,
                fromDateTime = futureNow,
                includeTodayIfFuture = false
            )
            triggerAtMillis = futureOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        val pendingIntent = createReminderPendingIntent(
            context = context,
            habitId = habit.id,
            habitName = habit.name,
            targetDesc = "${habit.targetCount} ${habit.targetUnit}",
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmInternal(alarmManager, triggerAtMillis, pendingIntent, habit.id)
        Log.d(TAG, "NEXT reminder habitId=${habit.id} nextOccurrence=$nextOccurrence")
    }

    /**
     * Re-schedules reminders for all active habits with reminder times.
     * Invoked upon device boot (BOOT_COMPLETED) or timezone/time changes.
     */
    suspend fun rescheduleAllActiveReminders(context: Context, container: AppContainer) {
        try {
            val settings = container.settingsRepository.getSettings().first()
            val activeHabitsWithReminders = container.database.habitDao().getActiveHabitsWithRemindersOnce()

            if (!settings.notificationsEnabled) {
                // Notifications globally disabled: cancel all alarms
                for (habit in activeHabitsWithReminders) {
                    cancelReminder(context, habit.id, reason = "notifications_disabled_globally")
                }
                return
            }

            val now = LocalDateTime.now(ZoneId.systemDefault())
            for (habit in activeHabitsWithReminders) {
                val reminderTime = habit.reminderTime ?: continue
                scheduleReminder(
                    context = context,
                    habitId = habit.id,
                    habitName = habit.name,
                    frequencyType = habit.frequencyType,
                    selectedDays = habit.selectedDays,
                    reminderTime = reminderTime,
                    targetDesc = "${habit.targetCount} ${habit.targetUnit}",
                    now = now
                )
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to reschedule all active reminders: ${e.message}")
        }
    }
}
