package com.aistudio.dayflow.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aistudio.dayflow.app.domain.model.AppSettings
import com.aistudio.dayflow.app.domain.model.ThemeMode
import com.aistudio.dayflow.app.domain.model.WeekStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dayflow_settings")

class DayFlowDataStore(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_WEEK_START = stringPreferencesKey("week_start")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_UNLOCKED_ACHIEVEMENTS = stringSetPreferencesKey("unlocked_achievements")
    }

    val unlockedAchievementsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_UNLOCKED_ACHIEVEMENTS] ?: emptySet()
    }

    suspend fun saveUnlockedAchievements(ids: Set<String>) {
        if (ids.isEmpty()) return
        context.dataStore.edit { preferences ->
            val existing = preferences[KEY_UNLOCKED_ACHIEVEMENTS] ?: emptySet()
            preferences[KEY_UNLOCKED_ACHIEVEMENTS] = existing + ids
        }
    }

    val appSettingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val themeModeStr = preferences[KEY_THEME_MODE] ?: ThemeMode.LIGHT.name
        val weekStartStr = preferences[KEY_WEEK_START] ?: WeekStart.MONDAY.name
        val notificationsEnabled = preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
        val reminderHour = preferences[KEY_REMINDER_HOUR] ?: 20
        val reminderMinute = preferences[KEY_REMINDER_MINUTE] ?: 0
        val onboardingCompleted = preferences[KEY_ONBOARDING_COMPLETED] ?: false

        AppSettings(
            themeMode = try { ThemeMode.valueOf(themeModeStr) } catch (e: Exception) { ThemeMode.LIGHT },
            weekStart = try { WeekStart.valueOf(weekStartStr) } catch (e: Exception) { WeekStart.MONDAY },
            notificationsEnabled = notificationsEnabled,
            defaultReminderHour = reminderHour,
            defaultReminderMinute = reminderMinute,
            onboardingCompleted = onboardingCompleted
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setWeekStart(weekStart: WeekStart) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WEEK_START] = weekStart.name
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setDefaultReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REMINDER_HOUR] = hour
            preferences[KEY_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }
}
