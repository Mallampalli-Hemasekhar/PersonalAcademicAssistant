
package com.example.personalacademicassistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ExamReminderScheduler {

    private val dateFormats = listOf(
        "dd MMM yyyy",
        "d MMM yyyy",
        "dd-MM-yyyy",
        "d-M-yyyy",
        "dd/MM/yyyy",
        "d/M/yyyy",
        "yyyy-MM-dd"
    )

    private val timeFormats = listOf(
        "h:mm a",
        "hh:mm a",
        "HH:mm"
    )

    private val reminderOffsets = listOf(
        1440L to "1 day",
        180L to "3 hours",
        30L to "30 minutes"
    )

    fun scheduleExam(
        context: Context,
        examId: String,
        subject: String,
        examDate: String,
        examTime: String,
        location: String
    ) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (examId.isBlank()) return

        val examCalendar = parseExamDateTime(
            examDate,
            examTime
        ) ?: return

        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        val now = System.currentTimeMillis()

        reminderOffsets.forEachIndexed { index, reminder ->

            val reminderTime = Calendar.getInstance().apply {
                timeInMillis = examCalendar.timeInMillis
                add(Calendar.MINUTE, -reminder.first.toInt())
            }

            if (reminderTime.timeInMillis <= now) {
                return@forEachIndexed
            }

            val intent = Intent(
                context,
                ExamReminderReceiver::class.java
            ).apply {
                action = "EXAM_REMINDER_$index"

                putExtra("userId", userId)
                putExtra("examId", examId)
                putExtra("subject", subject)
                putExtra("location", location)
                putExtra("reminderLabel", reminder.second)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getRequestCode(examId, index),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelExam(
        context: Context,
        examId: String
    ) {

        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        reminderOffsets.indices.forEach { index ->

            val intent = Intent(
                context,
                ExamReminderReceiver::class.java
            ).apply {
                action = "EXAM_REMINDER_$index"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getRequestCode(examId, index),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun getRequestCode(
        examId: String,
        index: Int
    ): Int {
        return 100000 + (examId.hashCode() * 3) + index
    }

    private fun parseExamDateTime(
        examDate: String,
        examTime: String
    ): Calendar? {

        var parsedDate: java.util.Date? = null

        for (pattern in dateFormats) {
            try {
                val formatter = SimpleDateFormat(
                    pattern,
                    Locale.ENGLISH
                )

                formatter.isLenient = false
                parsedDate = formatter.parse(examDate.trim())

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
                parsedTime = formatter.parse(examTime.trim())

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
