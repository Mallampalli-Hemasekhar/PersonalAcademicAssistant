
package com.example.personalacademicassistant

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {

        val title = inputData.getString("title") ?: "Study Reminder"
        val message = inputData.getString("message") ?: "Time to study!"

        val channelId = "academic_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "Academic Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        return try {

            NotificationManagerCompat
                .from(applicationContext)
                .notify(
                    System.currentTimeMillis().toInt(),
                    notification
                )

            Result.success()

        } catch (e: SecurityException) {
            Result.success()
        }
    }
}
