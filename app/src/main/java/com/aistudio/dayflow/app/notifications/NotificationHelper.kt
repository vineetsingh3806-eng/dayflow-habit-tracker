package com.aistudio.dayflow.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aistudio.dayflow.app.MainActivity
import com.aistudio.dayflow.app.R

object NotificationHelper {

    const val CHANNEL_ID = "dayflow_habit_reminders"
    const val CHANNEL_NAME = "Habit Reminders"
    const val CHANNEL_DESC = "Gentle daily reminders for your scheduled habits"

    fun getReminderSoundUri(context: Context): Uri {
        return try {
            Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/dayflow_reminder")
        } catch (e: Throwable) {
            Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.dayflow_reminder}")
        }
    }

    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

                // Do not recreate or override if channel already exists
                val existingChannel = try {
                    notificationManager.getNotificationChannel(CHANNEL_ID)
                } catch (e: Throwable) {
                    null
                }

                if (existingChannel != null) {
                    return
                }

                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                    try {
                        val soundUri = getReminderSoundUri(context)
                        val audioAttributes = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                        setSound(soundUri, audioAttributes)
                    } catch (e: Throwable) {
                        // Fallback safely if sound cannot be attached
                    }
                }
                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Throwable) {
            // Defensive: Never crash application startup
        }
    }

    fun showHabitReminderNotification(
        context: Context,
        habitId: Long,
        habitName: String,
        bodyText: String? = null,
        targetDesc: String = ""
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_HABIT_ID", habitId)
        }

        val notificationId = (habitId and 0x7FFFFFFF).toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Clear, encouraging, noticeable title and body
        val title = if (habitName.startsWith("Time for ", ignoreCase = true)) {
            habitName
        } else {
            "Time for $habitName"
        }

        val content = if (!bodyText.isNullOrBlank()) {
            bodyText
        } else if (targetDesc.isNotEmpty()) {
            "Your $targetDesc habit is ready."
        } else {
            "Your scheduled habit is ready."
        }

        val soundUri = try {
            getReminderSoundUri(context)
        } catch (e: Throwable) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                if (soundUri != null) {
                    setSound(soundUri)
                }
            }
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Android 13+ runtime notification permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("DayFlowReminder", "POST_NOTIFICATIONS permission not granted, skipping notification")
                return
            }
        }

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.d("DayFlowReminder", "Notifications disabled at system level, skipping notification")
            return
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.w("DayFlowReminder", "SecurityException posting notification: ${e.message}")
        } catch (e: Throwable) {
            Log.w("DayFlowReminder", "Throwable posting notification: ${e.message}")
        }
    }

    fun cancelHabitReminder(context: Context, habitId: Long) {
        try {
            val notificationId = (habitId and 0x7FFFFFFF).toInt()
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (e: Throwable) {
            // Safe cancellation
        }
    }
}
