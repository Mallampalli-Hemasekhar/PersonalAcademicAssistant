
package com.example.personalacademicassistant

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class ExamReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "exam_reminder_channel"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val userId = intent.getStringExtra("userId") ?: return
        val examId = intent.getStringExtra("examId") ?: return
        val subject = intent.getStringExtra("subject") ?: "Your exam"
        val location = intent.getStringExtra("location") ?: ""
        val reminderLabel =
            intent.getStringExtra("reminderLabel") ?: "soon"

        // Verify the account that scheduled this reminder.
        if (FirebaseAuth.getInstance().currentUser?.uid != userId) {
            return
        }

        createNotificationChannel(context)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val message = buildString {

            append("$subject exam is in $reminderLabel.")

            if (location.isNotBlank()) {
                append(" Location: $location")
            }

            append(" Start preparing!")
        }

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📚 Upcoming Exam Reminder")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(
                    examId.hashCode() + reminderLabel.hashCode(),
                    notification
                )
        } catch (_: SecurityException) {
            // Notification permission is unavailable.
        }
    }

    private fun createNotificationChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Exam Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Advance reminders for upcoming examinations"
            }

            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }
}
