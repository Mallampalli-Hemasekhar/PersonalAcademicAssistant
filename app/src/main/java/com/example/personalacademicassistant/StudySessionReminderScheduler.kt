
package com.example.personalacademicassistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object StudySessionReminderScheduler {

    private const val REMINDER_MINUTES = 10

    private val dateFormats = listOf(
        "dd-MM-yyyy",
        "d-M-yyyy"
    )

    private val timeFormats = listOf(
        "hh:mm a",
        "h:mm a",
        "HH:mm"
    )

    fun scheduleSession(
        context: Context,
        userId: String,
        planId: String,
        subject: String,
        topic: String,
        studyDate: String,
        startTime: String
    ) {

        if (userId.isBlank() || planId.isBlank()) return

        val sessionTime = parseSessionDateTime(
            studyDate,
            startTime
        ) ?: return

        val reminderTime = Calendar.getInstance().apply {
            timeInMillis = sessionTime.timeInMillis
            add(Calendar.MINUTE, -REMINDER_MINUTES)
        }

        if (reminderTime.timeInMillis <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(
            context,
            StudySessionReminderReceiver::class.java
        ).apply {
            action = "STUDY_SESSION_REMINDER"
            putExtra("userId", userId)
            putExtra("planId", planId)
            putExtra("subject", subject)
            putExtra("topic", topic)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(planId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime.timeInMillis,
            pendingIntent
        )
    }

    fun cancelSession(
        context: Context,
        planId: String
    ) {

        if (planId.isBlank()) return

        val intent = Intent(
            context,
            StudySessionReminderReceiver::class.java
        ).apply {
            action = "STUDY_SESSION_REMINDER"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(planId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun getRequestCode(planId: String): Int {
        return 200000 + planId.hashCode()
    }

    private fun parseSessionDateTime(
        studyDate: String,
        startTime: String
    ): Calendar? {

        var parsedDate: java.util.Date? = null

        for (pattern in dateFormats) {
            try {
                val formatter = SimpleDateFormat(
                    pattern,
                    Locale.ENGLISH
                )

                formatter.isLenient = false
                parsedDate = formatter.parse(studyDate.trim())

                if (parsedDate != null) break

            } catch (_: Exception) {
                // Try the next date format.
            }
        }

        var parsedTime: java.util.Date? = null

        for (pattern in timeFormats) {
            try {
                val formatter = SimpleDateFormat(
                    pattern,
                    Locale.ENGLISH
                )

                formatter.isLenient = false
                parsedTime = formatter.parse(startTime.trim())

                if (parsedTime != null) break

            } catch (_: Exception) {
                // Try the next time format.
            }
        }

        if (parsedDate == null || parsedTime == null) {
            return null
        }

        val dateCalendar = Calendar.getInstance().apply {
            time = parsedDate
        }

        val timeCalendar = Calendar.getInstance().apply {
            time = parsedTime
        }

        return Calendar.getInstance().apply {

            set(
                Calendar.YEAR,
                dateCalendar.get(Calendar.YEAR)
            )

            set(
                Calendar.MONTH,
                dateCalendar.get(Calendar.MONTH)
            )

            set(
                Calendar.DAY_OF_MONTH,
                dateCalendar.get(Calendar.DAY_OF_MONTH)
            )

            set(
                Calendar.HOUR_OF_DAY,
                timeCalendar.get(Calendar.HOUR_OF_DAY)
            )

            set(
                Calendar.MINUTE,
                timeCalendar.get(Calendar.MINUTE)
            )

            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
