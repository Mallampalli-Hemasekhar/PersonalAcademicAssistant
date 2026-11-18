
package com.example.personalacademicassistant

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.google.firebase.auth.FirebaseAuth

class ClassReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "class_reminder_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val userId = intent.getStringExtra("userId") ?: return
        val classId = intent.getStringExtra("classId") ?: return
        val subject = intent.getStringExtra("subject") ?: "Upcoming Class"
        val day = intent.getStringExtra("day") ?: return
        val startTime = intent.getStringExtra("startTime") ?: return
        val classroom = intent.getStringExtra("classroom") ?: ""

        // Prevent notifications belonging to another account.
        if (FirebaseAuth.getInstance().currentUser?.uid != userId) {
            return
        }

        createNotificationChannel(context)

        val permissionGranted =
            Build.VERSION.SDK_INT < 33 ||
                    context.checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {

            val message = if (classroom.isNotBlank()) {
                "$subject starts in 5 minutes. Classroom: $classroom"
            } else {
                "$subject starts in 5 minutes."
            }

            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }

            val notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Upcoming Class")
                .setContentText(message)
                .setAutoCancel(true)
                .build()

            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            try {
                manager.notify(classId.hashCode(), notification)
            } catch (_: SecurityException) {
                // Notification permission is unavailable.
            }
        }

        // Schedule the next weekly reminder for the same account.
        ClassReminderScheduler.scheduleNext(
            context,
            classId,
            subject,
            day,
            startTime,
            classroom
        )
    }

    private fun createNotificationChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Class Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Reminders 5 minutes before scheduled classes"
            }

            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }
}
