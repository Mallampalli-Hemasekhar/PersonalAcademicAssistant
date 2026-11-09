
package com.example.personalacademicassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

    fun scheduleReminder() {

        val delayMinutes = minutes.toLongOrNull()

        if (title.isBlank() || message.isBlank()) {
            status = "Please enter the title and message."
            return
        }

        if (delayMinutes == null || delayMinutes <= 0) {
            status = "Please enter a valid number of minutes."
            return
        }

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    "title" to title,
                    "message" to message
                )
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)

        status = "Your reminder has been scheduled!"

        title = ""
        message = ""
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->

        if (granted) {
            scheduleReminder()
        } else {
            status = "Please allow notifications in app settings."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "🔔 Notifications & Reminders",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Never miss an assignment, exam, or study session."
        )

        Spacer(modifier = Modifier.height(5.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Reminder Title") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Reminder Message") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 2
        )

        OutlinedTextField(
            value = minutes,
            onValueChange = { minutes = it },
            label = { Text("Remind me after (minutes)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Button(
            onClick = {

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
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Schedule Reminder")
        }

        if (status.isNotBlank()) {
            Text(
                text = status,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Reminder Tips",
                    fontWeight = FontWeight.Bold
                )

                Text("• Set reminders before assignment deadlines.")
                Text("• Revise important topics before exams.")
                Text("• Schedule regular study sessions.")
            }
        }
    }
}
