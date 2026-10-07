package com.aistudio.dayflow.app

import android.app.Application
import com.aistudio.dayflow.app.di.AppContainer
import com.aistudio.dayflow.app.notifications.HabitReminderScheduler
import com.aistudio.dayflow.app.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DayFlowApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Ensure notification channel creation and reminder restoration do not block cold startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationHelper.createNotificationChannel(this@DayFlowApplication)
                HabitReminderScheduler.rescheduleAllActiveReminders(this@DayFlowApplication, container)
            } catch (e: Throwable) {
                // Defensive: Never allow notification channel creation or scheduling to crash app startup
            }
        }
    }
}

