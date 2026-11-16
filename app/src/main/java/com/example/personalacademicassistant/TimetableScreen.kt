
package com.example.personalacademicassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class TimetableEntry(
    val id: String = "",
    val subject: String = "",
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val classroom: String = ""
)

@Composable
fun TimetableScreen() {

    val context = LocalContext.current

    var showAddTimetable by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val timetableEntries = remember {
        mutableStateListOf<TimetableEntry>()
    }

    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser

    val days = listOf(
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday"
    )

    val activeDays = timetableEntries.map { it.day }.distinct().size

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                errorMessage =
                    "Please allow notifications in Android settings to receive class reminders."
            }
        }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    LaunchedEffect(Unit) {

        if (currentUser == null) {
            errorMessage = "User is not logged in."
            isLoading = false
        } else {

            firestore.collection("users")
                .document(currentUser.uid)
                .collection("timetable")
                .get()
                .addOnSuccessListener { result ->

                    timetableEntries.clear()

                    for (document in result.documents) {

                        val entry = TimetableEntry(
                            id = document.id,
                            subject = document.getString("subject") ?: "",
                            day = document.getString("day") ?: "",
                            startTime = document.getString("startTime") ?: "",
                            endTime = document.getString("endTime") ?: "",
                            classroom = document.getString("classroom") ?: ""
                        )

                        timetableEntries.add(entry)

                        ClassReminderScheduler.scheduleNext(
                            context.applicationContext,
                            entry.id,
                            entry.subject,
                            entry.day,
                            entry.startTime,
                            entry.classroom
                        )
                    }

                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    errorMessage = exception.message
                        ?: "Failed to load timetable."
                    isLoading = false
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {

        Text(
            text = "My Timetable 📅",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Manage your weekly class schedule.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            TimetableStatCard(
                title = "Total Classes",
                count = timetableEntries.size,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            TimetableStatCard(
                title = "Active Days",
                count = activeDays,
                color = Color(0xFF26A69A),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                showAddTimetable = true
                errorMessage = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "+ Add New Class",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔔",
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Class reminders are scheduled 10 minutes before each class.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        if (errorMessage.isNotEmpty()) {

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when {

            isLoading -> {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Loading your timetable...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            timetableEntries.isEmpty() -> {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📅",
                        fontSize = 54.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "No classes added yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Add your first class to build your timetable.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {

                    val sortedEntries = timetableEntries.sortedWith(
                        compareBy<TimetableEntry> {
                            days.indexOf(it.day).let { index ->
                                if (index == -1) Int.MAX_VALUE else index
                            }
                        }.thenBy { it.startTime }
                    )

                    items(
                        items = sortedEntries,
                        key = { it.id }
                    ) { entry ->

                        TimetableCard(
                            entry = entry,
                            onDelete = {

                                val user = auth.currentUser

                                if (user != null) {

                                    firestore.collection("users")
                                        .document(user.uid)
                                        .collection("timetable")
                                        .document(entry.id)
                                        .delete()
                                        .addOnSuccessListener {

                                            ClassReminderScheduler.cancel(
                                                context.applicationContext,
                                                entry.id
                                            )

                                            timetableEntries.remove(entry)
                                            errorMessage = ""
                                        }
                                        .addOnFailureListener { exception ->

                                            errorMessage = exception.message
                                                ?: "Failed to delete class."
                                        }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddTimetable) {

        AddTimetableDialog(
            onDismiss = {
                showAddTimetable = false
            },

            onAddTimetable = { entry ->

                val user = auth.currentUser

                if (user == null) {

                    errorMessage = "User is not logged in."
                    showAddTimetable = false

                } else {

                    val timetableData = hashMapOf(
                        "subject" to entry.subject,
                        "day" to entry.day,
                        "startTime" to entry.startTime,
                        "endTime" to entry.endTime,
                        "classroom" to entry.classroom
                    )

                    firestore.collection("users")
                        .document(user.uid)
                        .collection("timetable")
                        .add(timetableData)
                        .addOnSuccessListener { documentReference ->

                            val savedEntry = entry.copy(
                                id = documentReference.id
                            )

                            timetableEntries.add(savedEntry)

                            ClassReminderScheduler.scheduleNext(
                                context.applicationContext,
                                savedEntry.id,
                                savedEntry.subject,
                                savedEntry.day,
                                savedEntry.startTime,
                                savedEntry.classroom
                            )

                            showAddTimetable = false
                            errorMessage = ""
                        }
                        .addOnFailureListener { exception ->

                            errorMessage = exception.message
                                ?: "Failed to add class."
                        }
                }
            }
        )
    }
}

@Composable
fun TimetableStatCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = count.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun TimetableCard(
    entry: TimetableEntry,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = entry.subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "📅 ${entry.day}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = entry.day.take(3).uppercase(),
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "⏰ ${entry.startTime} - ${entry.endTime}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "🏫 Classroom: ${entry.classroom}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Class")
                }
            }
        }
    }
}

@Composable
fun AddTimetableDialog(
    onDismiss: () -> Unit,
    onAddTimetable: (TimetableEntry) -> Unit
) {

    var subject by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val days = listOf(
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday"
    )

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = "Add New Class 📚",
                fontWeight = FontWeight.Bold
            )
        },

        text = {

            Column {

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject") },
                    placeholder = { Text("Enter subject name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box {

                    OutlinedTextField(
                        value = day,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Day") },
                        placeholder = { Text("Select class day") },
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    TextButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                    ) {
                        Text("Select")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        days.forEach { selectedDay ->

                            DropdownMenuItem(
                                text = { Text(selectedDay) },
                                onClick = {
                                    day = selectedDay
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Start Time") },
                    placeholder = { Text("Example: 09:00 AM") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("End Time") },
                    placeholder = { Text("Example: 10:00 AM") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = classroom,
                    onValueChange = { classroom = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Classroom") },
                    placeholder = { Text("Example: C-204") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    if (
                        subject.isNotBlank() &&
                        day.isNotBlank() &&
                        startTime.isNotBlank() &&
                        endTime.isNotBlank() &&
                        classroom.isNotBlank()
                    ) {

                        onAddTimetable(
                            TimetableEntry(
                                subject = subject.trim(),
                                day = day,
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                classroom = classroom.trim()
                            )
                        )
                    }
                },
                enabled = subject.isNotBlank() &&
                        day.isNotBlank() &&
                        startTime.isNotBlank() &&
                        endTime.isNotBlank() &&
                        classroom.isNotBlank()
            ) {
                Text("Add Class")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
