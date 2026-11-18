
package com.example.personalacademicassistant

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object SmartNotificationManager {

    private const val PERIODIC_WORK_NAME =
        "academic_activity_reminders"

    private const val IMMEDIATE_WORK_NAME =
        "academic_activity_reminders_immediate"

    private fun getConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    fun start(context: Context, userId: String) {

        if (userId.isBlank()) return

        val workManager = WorkManager.getInstance(
            context.applicationContext
        )

        val inputData = workDataOf(
            "userId" to userId
        )

        // Check pending academic activities immediately after login.
        val immediateRequest =
            OneTimeWorkRequestBuilder<SmartReminderWorker>()
                .setInputData(inputData)
                .setConstraints(getConstraints())
                .build()

        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )

        // Repeat the pending activity check every 4 hours.
        val periodicRequest =
            PeriodicWorkRequestBuilder<SmartReminderWorker>(
                4,
                TimeUnit.HOURS
            )
                .setInputData(inputData)
                .setConstraints(getConstraints())
                .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun cancel(context: Context) {

        val workManager = WorkManager.getInstance(
            context.applicationContext
        )

        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
    }
}
