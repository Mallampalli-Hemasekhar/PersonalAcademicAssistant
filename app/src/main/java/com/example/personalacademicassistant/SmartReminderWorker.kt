
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
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmartReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {

        val userId = inputData.getString("userId")
            ?: return Result.failure()

        // Ensure this work belongs to the currently logged-in account.
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId != userId) {
            return Result.success()
        }

        return try {

            val firestore = FirebaseFirestore.getInstance()

            val assignments = Tasks.await(
                firestore.collection("users")
                    .document(userId)
                    .collection("assignments")
                    .whereEqualTo("completed", false)
                    .get()
            )

            val todayFormat = SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
            ).apply {
                isLenient = false
            }

            val today = todayFormat.format(Date())

            val dailyGoals = Tasks.await(
                firestore.collection("users")
                    .document(userId)
                    .collection("dailyGoals")
                    .whereEqualTo("completed", false)
                    .get()
            )

            val pendingAssignments = assignments.documents

            val incompleteGoals = dailyGoals.documents.filter { document ->

                val goalDate = document.getString("date") ?: ""

                val targetHours = (
                        document.get("targetHours") as? Number
                        )?.toDouble() ?: 0.0

                val completedHours = (
                        document.get("completedHours") as? Number
                        )?.toDouble() ?: 0.0

                goalDate == today && targetHours > completedHours
            }

            if (
                pendingAssignments.isEmpty() &&
                incompleteGoals.isEmpty()
            ) {
                return Result.success()
            }

            createNotificationChannel()

            if (
                Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return Result.success()
            }

            val assignmentCount = pendingAssignments.size
            val goalCount = incompleteGoals.size

            val message = buildString {

                if (assignmentCount > 0) {
                    append("$assignmentCount pending assignment(s)")
                }

                if (goalCount > 0) {
                    if (isNotEmpty()) append(" and ")
                    append("$goalCount incomplete daily goal(s)")
                }

                append(". Keep making progress!")
            }

            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle("Your academic work needs attention")

            pendingAssignments.take(4).forEach { document ->

                val title = document.getString("title")
                    ?: "Untitled assignment"

                inboxStyle.addLine("Assignment: $title")
            }

            incompleteGoals.take(4).forEach { document ->

                val title = document.getString("title")
                    ?: "Daily goal"

                inboxStyle.addLine("Daily goal: $title")
            }

            val notification = NotificationCompat.Builder(
                applicationContext,
                CHANNEL_ID
            )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("📚 Pending Academic Work")
                .setContentText(message)
                .setStyle(inboxStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat
                    .from(applicationContext)
                    .notify(NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                return Result.success()
            }

            Result.success()

        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Academic Activity Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Reminders for pending assignments and daily goals"
            }

            val manager = applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "academic_activity_reminders"
        const val NOTIFICATION_ID = 4101
    }
}
