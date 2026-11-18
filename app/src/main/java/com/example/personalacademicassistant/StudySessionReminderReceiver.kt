
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

class StudySessionReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "study_session_reminder_channel"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val userId = intent.getStringExtra("userId") ?: return
        val planId = intent.getStringExtra("planId") ?: return
        val subject = intent.getStringExtra("subject") ?: "Study Session"
        val topic = intent.getStringExtra("topic") ?: ""

        // Do not show another account's reminder after logout or account switching.
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

        val message = if (topic.isNotBlank()) {
            "$subject: $topic starts in 10 minutes. Get ready to study!"
        } else {
            "$subject study session starts in 10 minutes. Get ready!"
        }

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📚 Study Session Reminder")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(
                planId.hashCode(),
                notification
            )
    }

    private fun createNotificationChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Study Session Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications 10 minutes before planned study sessions"
            }

            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }
}
