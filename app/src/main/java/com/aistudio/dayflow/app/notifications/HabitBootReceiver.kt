package com.aistudio.dayflow.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistudio.dayflow.app.DayFlowApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == "android.intent.action.TIME_SET" ||
            action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            val app = context.applicationContext as? DayFlowApplication
            if (app == null) {
                pendingResult.finish()
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    HabitReminderScheduler.rescheduleAllActiveReminders(context, app.container)
                } catch (e: Throwable) {
                    // Defensive: never crash boot/time receiver
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
