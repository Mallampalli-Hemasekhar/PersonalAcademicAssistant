
package com.example.personalacademicassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

private val timetableDays = listOf(
    "Monday",
    "Tuesday",
    "Wednesday",
    "Thursday",
    "Friday",
    "Saturday"
)

@Composable
fun TimetableScreen() {

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    var showAddTimetable by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val timetableEntries = remember {
        mutableStateListOf<TimetableEntry>()
    }

    val currentUser = auth.currentUser

    val activeDays = timetableEntries
        .map { it.day }
        .distinct()
        .size

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                errorMessage =
                    "Notifications are disabled. You can enable them in Android settings."
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

    LaunchedEffect(currentUser?.uid) {

        if (currentUser == null) {
            errorMessage = "User is not logged in."
            isLoading = false
        } else {

            isLoading = true

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

    val sortedEntries = timetableEntries.sortedWith(
        compareBy<TimetableEntry> {
            val index = timetableDays.indexOf(it.day)
            if (index == -1) Int.MAX_VALUE else index
        }.thenBy {
            it.startTime
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 22.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {

        item {

            Column {

                Text(
                    text = "My Schedule",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "Your classes, organized in one place.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                TimetableStatCard(
                    title = "Total Classes",
                    count = timetableEntries.size,
                    icon = "📚",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                TimetableStatCard(
                    title = "Active Days",
                    count = activeDays,
                    icon = "📅",
                    color = Color(0xFF20A58B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {

            Button(
                onClick = {
                    errorMessage = ""
                    showAddTimetable = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(17.dp)
            ) {

                Text(
                    text = "＋  Add New Class",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        text = "Weekly Schedule",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Your classes arranged by day",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {

                    Text(
                        text = "${timetableEntries.size} Classes",
                        modifier = Modifier.padding(
                            horizontal = 11.dp,
                            vertical = 8.dp
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (errorMessage.isNotBlank()) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(13.dp)
                ) {

                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(13.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (isLoading) {

            item {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        CircularProgressIndicator()

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Loading your schedule...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

        } else if (sortedEntries.isEmpty()) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "📅",
                            fontSize = 48.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Your schedule is empty",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Add your first class to organize your week.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(15.dp))

                        TextButton(
                            onClick = {
                                showAddTimetable = true
                            }
                        ) {
                            Text("Add your first class")
                        }
                    }
                }
            }

        } else {

            timetableDays.forEach { day ->

                val dayEntries = sortedEntries.filter {
                    it.day.equals(day, ignoreCase = true)
                }

                if (dayEntries.isNotEmpty()) {

                    item(key = "day_$day") {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {

                                Text(
                                    text = day.take(3).uppercase(),
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = day,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "${dayEntries.size} ${if (dayEntries.size == 1) "class" else "classes"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(
                        items = dayEntries,
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

            val unknownDayEntries = sortedEntries.filter {
                !timetableDays.contains(it.day)
            }

            if (unknownDayEntries.isNotEmpty()) {

                item(key = "other_days") {

                    Text(
                        text = "Other Schedule Entries",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(
                    items = unknownDayEntries,
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
    modifier: Modifier = Modifier,
    icon: String = "📚"
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp)
        ) {

            Text(
                text = icon,
                fontSize = 23.sp
            )

            Spacer(modifier = Modifier.height(11.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = count.toString(),
                fontSize = 29.sp,
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

    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(17.dp)
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "📚  ${entry.day}",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primaryContainer
                ) {

                    Text(
                        text = entry.day.take(3).uppercase(),
                        modifier = Modifier.padding(
                            horizontal = 13.dp,
                            vertical = 10.dp
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                color = colors.outlineVariant
            )

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = colors.primaryContainer.copy(alpha = 0.7f)
                ) {

                    Text(
                        text = "⏰",
                        modifier = Modifier.padding(10.dp),
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {

                    Text(
                        text = "CLASS TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "${entry.startTime} - ${entry.endTime}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(13.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = colors.surfaceVariant
                ) {

                    Text(
                        text = "🏫",
                        modifier = Modifier.padding(10.dp),
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {

                    Text(
                        text = "CLASSROOM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = entry.classroom,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.error
                    )
                ) {

                    Text(
                        text = "Delete Class",
                        fontWeight = FontWeight.SemiBold
                    )
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

    var validationError by remember { mutableStateOf("") }

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
                    onValueChange = {
                        subject = it
                        validationError = ""
                    },
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
                        onClick = {
                            expanded = true
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                    ) {
                        Text("Select")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {

                        timetableDays.forEach { selectedDay ->

                            DropdownMenuItem(
                                text = {
                                    Text(selectedDay)
                                },
                                onClick = {
                                    day = selectedDay
                                    expanded = false
                                    validationError = ""
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = startTime,
                    onValueChange = {
                        startTime = it
                        validationError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Start Time") },
                    placeholder = { Text("Example: 09:00 AM") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = endTime,
                    onValueChange = {
                        endTime = it
                        validationError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("End Time") },
                    placeholder = { Text("Example: 10:00 AM") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = classroom,
                    onValueChange = {
                        classroom = it
                        validationError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Classroom") },
                    placeholder = { Text("Example: C-204") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (validationError.isNotBlank()) {

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = validationError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    when {

                        subject.isBlank() ->
                            validationError = "Enter a subject name."

                        day.isBlank() ->
                            validationError = "Select a class day."

                        startTime.isBlank() ->
                            validationError = "Enter the start time."

                        endTime.isBlank() ->
                            validationError = "Enter the end time."

                        classroom.isBlank() ->
                            validationError = "Enter the classroom."

                        else -> {

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

            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}
