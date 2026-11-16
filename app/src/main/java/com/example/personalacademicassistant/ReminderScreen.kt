
package com.example.personalacademicassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

@Composable
fun ReminderScreen() {

    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("1") }
    var status by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    fun scheduleReminder() {

        val delayMinutes = minutes.toLongOrNull()

        if (title.isBlank() || message.isBlank()) {
            status = "Please enter the reminder title and message."
            isSuccess = false
            return
        }

        if (delayMinutes == null || delayMinutes <= 0) {
            status = "Please enter a valid number of minutes."
            isSuccess = false
            return
        }

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    "title" to title.trim(),
                    "message" to message.trim()
                )
            )
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueue(request)

        status = "Your reminder has been scheduled successfully!"
        isSuccess = true

        title = ""
        message = ""
        minutes = "1"
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->

        if (granted) {
            scheduleReminder()
        } else {
            status = "Please allow notifications in app settings."
            isSuccess = false
        }
    }

    fun requestAndSchedule() {

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            scheduleReminder()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        // Header

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = "Reminders & Notifications 🔔",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Stay organized and never miss an important task.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Reminder Introduction Card

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text = "⏰",
                    fontSize = 32.sp
                )

                Text(
                    text = "Plan it. Schedule it. Remember it.",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "Create personal reminders for assignments, exams, study sessions and important academic activities.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Create Reminder Form

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {

                Text(
                    text = "Create New Reminder",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Title

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Title") },
                    placeholder = {
                        Text("Example: Complete assignment")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Message

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Reminder Message") },
                    placeholder = {
                        Text("Enter reminder details...")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    minLines = 3
                )

                // Time

                Text(
                    text = "Remind me after",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )

                OutlinedTextField(
                    value = minutes,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            minutes = it
                        }
                    },
                    label = { Text("Time in minutes") },
                    placeholder = { Text("Enter minutes") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    trailingIcon = {
                        Text(
                            text = "MIN",
                            modifier = Modifier.padding(end = 12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                )

                // Quick Time Selection

                Text(
                    text = "Quick Select",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {

                    listOf("5", "15", "30", "60").forEach { value ->

                        val selected = minutes == value

                        OutlinedButton(
                            onClick = {
                                minutes = value
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                horizontal = 2.dp,
                                vertical = 9.dp
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {

                            Text(
                                text = "${value}m",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Schedule Button

                Button(
                    onClick = {
                        requestAndSchedule()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {

                    Text(
                        text = "🔔  Schedule Reminder",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status Message

                if (status.isNotBlank()) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {

                        Text(
                            text = status,
                            modifier = Modifier.padding(14.dp),
                            color = if (isSuccess) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Reminder Tips

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {

                Text(
                    text = "💡 Smart Reminder Tips",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                ReminderTip(
                    emoji = "📚",
                    title = "Assignment Deadlines",
                    description = "Set reminders before your assignment submission dates."
                )

                ReminderTip(
                    emoji = "📝",
                    title = "Exam Preparation",
                    description = "Schedule revision sessions for important subjects."
                )

                ReminderTip(
                    emoji = "🎯",
                    title = "Study Sessions",
                    description = "Create regular reminders to maintain your study routine."
                )

                ReminderTip(
                    emoji = "⏰",
                    title = "Stay Consistent",
                    description = "Use reminders to build productive daily habits."
                )
            }
        }

        // Information Card

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "How it works",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "1. Enter your reminder title and message."
                )

                Text(
                    text = "2. Choose how many minutes to wait."
                )

                Text(
                    text = "3. Tap Schedule Reminder."
                )

                Text(
                    text = "4. Receive a notification when the scheduled work runs."
                )

                Text(
                    text = "Note: WorkManager schedules background work. Actual notification delivery can be delayed by Android battery restrictions or system scheduling."
                )
            }
        }
    }
}

@Composable
fun ReminderTip(
    emoji: String,
    title: String,
    description: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = emoji,
            fontSize = 25.sp
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {

            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
