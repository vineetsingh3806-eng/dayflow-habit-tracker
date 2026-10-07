package com.aistudio.dayflow.app.domain.usecase

import com.aistudio.dayflow.app.domain.model.AppSettings
import com.aistudio.dayflow.app.domain.model.ThemeMode
import com.aistudio.dayflow.app.domain.model.WeekStart
import com.aistudio.dayflow.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetAppSettingsUseCase(private val settingsRepository: SettingsRepository) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.getSettings()
}

class UpdateThemeModeUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}

class UpdateWeekStartUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(weekStart: WeekStart) {
        settingsRepository.setWeekStart(weekStart)
    }
}

class UpdateNotificationsEnabledUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) {
        settingsRepository.setNotificationsEnabled(enabled)
    }
}

class SetOnboardingCompletedUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(completed: Boolean) {
        settingsRepository.setOnboardingCompleted(completed)
    }
}
