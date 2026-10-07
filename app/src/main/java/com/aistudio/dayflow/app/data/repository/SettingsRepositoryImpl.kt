package com.aistudio.dayflow.app.data.repository

import com.aistudio.dayflow.app.data.local.preferences.DayFlowDataStore
import com.aistudio.dayflow.app.domain.model.AppSettings
import com.aistudio.dayflow.app.domain.model.ThemeMode
import com.aistudio.dayflow.app.domain.model.WeekStart
import com.aistudio.dayflow.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val dataStore: DayFlowDataStore
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> {
        return dataStore.appSettingsFlow
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.setThemeMode(mode)
    }

    override suspend fun setWeekStart(weekStart: WeekStart) {
        dataStore.setWeekStart(weekStart)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.setNotificationsEnabled(enabled)
    }

    override suspend fun setDefaultReminderTime(hour: Int, minute: Int) {
        dataStore.setDefaultReminderTime(hour, minute)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.setOnboardingCompleted(completed)
    }
}
