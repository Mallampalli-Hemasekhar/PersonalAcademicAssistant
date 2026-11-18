
package com.example.personalacademicassistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ClassReminderScheduler {

    private const val REMINDER_MINUTES = 5

    fun scheduleNext(
        context: Context,
        classId: String,
        subject: String,
        day: String,
        startTime: String,
        classroom: String
    ) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (classId.isBlank()) return

        val formats = listOf("h:mm a", "hh:mm a", "HH:mm")
        var parsedTime: Calendar? = null

        for (pattern in formats) {
            try {
                val formatter = SimpleDateFormat(pattern, Locale.ENGLISH)
                formatter.isLenient = false

                val date = formatter.parse(startTime.trim())

                if (date != null) {
                    parsedTime = Calendar.getInstance().apply {
                        time = date
                    }
                    break
                }
            } catch (_: Exception) {
                // Try another time format.
            }
        }

        val classTime = parsedTime ?: return

        val targetDay = when (day.trim().lowercase(Locale.ENGLISH)) {
            "sunday" -> Calendar.SUNDAY
            "monday" -> Calendar.MONDAY
            "tuesday" -> Calendar.TUESDAY
            "wednesday" -> Calendar.WEDNESDAY
            "thursday" -> Calendar.THURSDAY
            "friday" -> Calendar.FRIDAY
            "saturday" -> Calendar.SATURDAY
            else -> return
        }

        val now = Calendar.getInstance()

        val reminderTime = Calendar.getInstance().apply {

            set(
                Calendar.HOUR_OF_DAY,
                classTime.get(Calendar.HOUR_OF_DAY)
            )

            set(
                Calendar.MINUTE,
                classTime.get(Calendar.MINUTE)
            )

            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            val difference =
                (targetDay - get(Calendar.DAY_OF_WEEK) + 7) % 7

            add(Calendar.DAY_OF_YEAR, difference)
            add(Calendar.MINUTE, -REMINDER_MINUTES)

            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 7)
            }
        }

        val intent = Intent(
            context,
            ClassReminderReceiver::class.java
        ).apply {
            action = "CLASS_REMINDER"

            putExtra("userId", userId)
            putExtra("classId", classId)
            putExtra("subject", subject)
            putExtra("day", day)
            putExtra("startTime", startTime)
            putExtra("classroom", classroom)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            classId.hashCode(),
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

    fun cancel(context: Context, classId: String) {

        val intent = Intent(
            context,
            ClassReminderReceiver::class.java
        ).apply {
            action = "CLASS_REMINDER"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            classId.hashCode(),
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
}
