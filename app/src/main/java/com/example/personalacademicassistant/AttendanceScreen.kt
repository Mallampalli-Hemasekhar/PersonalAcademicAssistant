
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

data class AttendanceRecord(
    val id: String = "",
    val subject: String = "",
    val totalClasses: Int = 0,
    val attendedClasses: Int = 0
)

private const val REQUIRED_ATTENDANCE = 75.0

private fun attendancePercentage(record: AttendanceRecord): Double {
    val total = record.totalClasses.coerceAtLeast(0)
    val attended = record.attendedClasses.coerceIn(0, total)

    return if (total == 0) 0.0 else attended.toDouble() / total * 100
}

private fun formatAttendance(value: Double): String {
    return String.format(Locale.US, "%.1f%%", value)
}

@Composable
fun AttendanceScreen() {

    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val userId = auth.currentUser?.uid

    val colors = MaterialTheme.colorScheme

    var attendanceRecords by remember {
        mutableStateOf<List<AttendanceRecord>>(emptyList())
    }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember {
        mutableStateOf<AttendanceRecord?>(null)
    }

    DisposableEffect(userId) {

        if (userId == null) {
            errorMessage = "Please log in to view attendance."
            isLoading = false

            onDispose { }
        } else {

            isLoading = true
            errorMessage = ""

            val listener = firestore.collection("users")
                .document(userId)
                .collection("attendance")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        errorMessage = error.message
                            ?: "Unable to load attendance."
                        isLoading = false
                        return@addSnapshotListener
                    }

                    attendanceRecords = snapshot?.documents?.map { document ->

                        AttendanceRecord(
                            id = document.id,
                            subject = document.getString("subject") ?: "",
                            totalClasses = (
                                    document.getLong("totalClasses") ?: 0L
                                    ).toInt().coerceAtLeast(0),
                            attendedClasses = (
                                    document.getLong("attendedClasses") ?: 0L
                                    ).toInt().coerceAtLeast(0)
                        )

                    } ?: emptyList()

                    isLoading = false
                }

            onDispose {
                listener.remove()
            }
        }
    }

    val totalClasses = attendanceRecords.sumOf {
        it.totalClasses.coerceAtLeast(0)
    }

    val attendedClasses = attendanceRecords.sumOf {
        it.attendedClasses.coerceIn(0, it.totalClasses.coerceAtLeast(0))
    }

    val missedClasses = (totalClasses - attendedClasses).coerceAtLeast(0)

    val overallPercentage = if (totalClasses > 0) {
        attendedClasses.toDouble() / totalClasses * 100
    } else {
        0.0
    }

    val safePercentage = overallPercentage.coerceIn(0.0, 100.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 22.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {

            Column {

                Text(
                    text = "📊 My Attendance",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "Track and improve your subject-wise attendance.",
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.primaryContainer
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "OVERALL ATTENDANCE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = formatAttendance(overallPercentage),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(
                                colors.surface.copy(alpha = 0.55f),
                                RoundedCornerShape(20.dp)
                            )
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(
                                    (safePercentage / 100).toFloat()
                                )
                                .fillMaxHeight()
                                .background(
                                    colors.primary,
                                    RoundedCornerShape(20.dp)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = when {
                            totalClasses == 0 ->
                                "Add your attendance records to see your progress."

                            overallPercentage < REQUIRED_ATTENDANCE ->
                                "⚠ Your overall attendance is below 75%. Try to attend more classes."

                            else ->
                                "✓ Your overall attendance is satisfactory!"
                        },
                        fontSize = 13.sp,
                        color = colors.onPrimaryContainer
                    )
                }
            }
        }

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                AttendanceStatCard(
                    title = "Total Classes",
                    value = totalClasses.toString(),
                    modifier = Modifier.weight(1f),
                    icon = "📚"
                )

                AttendanceStatCard(
                    title = "Attended",
                    value = attendedClasses.toString(),
                    modifier = Modifier.weight(1f),
                    icon = "✓"
                )

                AttendanceStatCard(
                    title = "Missed",
                    value = missedClasses.toString(),
                    modifier = Modifier.weight(1f),
                    icon = "✕"
                )
            }
        }

        item {

            Button(
                onClick = {
                    errorMessage = ""
                    showAddDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(15.dp)
            ) {

                Text(
                    text = "+ Add Subject Attendance",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {

            Column {

                Text(
                    text = "Subject-wise Attendance",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Monitor each subject individually.",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }

        if (isLoading) {

            item {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 35.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        CircularProgressIndicator()

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Loading attendance...",
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

        } else if (attendanceRecords.isEmpty()) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surfaceVariant
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(25.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "📊",
                            fontSize = 42.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "No attendance records yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = "Add your first subject to start tracking attendance.",
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

        } else {

            items(
                items = attendanceRecords,
                key = { it.id }
            ) { record ->

                AttendanceSubjectCard(
                    record = record,

                    onPresent = {

                        updateAttendance(
                            firestore = firestore,
                            userId = userId,
                            record = record,
                            present = true,
                            onError = { errorMessage = it }
                        )
                    },

                    onAbsent = {

                        updateAttendance(
                            firestore = firestore,
                            userId = userId,
                            record = record,
                            present = false,
                            onError = { errorMessage = it }
                        )
                    },

                    onDelete = {
                        recordToDelete = record
                    }
                )
            }
        }

        if (errorMessage.isNotBlank()) {

            item {

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colors.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(14.dp),
                        color = colors.onErrorContainer,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    if (showAddDialog) {

        AddAttendanceDialog(
            onDismiss = {
                showAddDialog = false
            },

            onAddAttendance = { record ->

                if (userId == null) {

                    errorMessage = "Please log in first."

                } else {

                    val data = hashMapOf(
                        "subject" to record.subject,
                        "totalClasses" to record.totalClasses,
                        "attendedClasses" to record.attendedClasses
                    )

                    firestore.collection("users")
                        .document(userId)
                        .collection("attendance")
                        .add(data)
                        .addOnSuccessListener {
                            showAddDialog = false
                            errorMessage = ""
                        }
                        .addOnFailureListener { exception ->
                            errorMessage = exception.message
                                ?: "Unable to add attendance."
                        }
                }
            }
        )
    }

    recordToDelete?.let { record ->

        AlertDialog(
            onDismissRequest = {
                recordToDelete = null
            },

            title = {
                Text("Delete Attendance?")
            },

            text = {
                Text(
                    "Are you sure you want to delete the attendance record for ${record.subject}? This cannot be undone."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        if (userId == null) {

                            errorMessage = "Please log in first."

                        } else {

                            firestore.collection("users")
                                .document(userId)
                                .collection("attendance")
                                .document(record.id)
                                .delete()
                                .addOnFailureListener { exception ->
                                    errorMessage = exception.message
                                        ?: "Failed to delete attendance."
                                }
                        }

                        recordToDelete = null
                    }
                ) {

                    Text(
                        "Delete",
                        color = colors.error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        recordToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AttendanceStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: String
) {

    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            Text(
                text = icon,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }
    }
}

private fun updateAttendance(
    firestore: FirebaseFirestore,
    userId: String?,
    record: AttendanceRecord,
    present: Boolean,
    onError: (String) -> Unit
) {

    if (userId == null) {
        onError("Please log in first.")
        return
    }

    val currentTotal = record.totalClasses.coerceAtLeast(0)

    val currentAttended = record.attendedClasses.coerceIn(
        0,
        currentTotal
    )

    val newTotal = currentTotal + 1

    val newAttended = if (present) {
        currentAttended + 1
    } else {
        currentAttended
    }

    firestore.collection("users")
        .document(userId)
        .collection("attendance")
        .document(record.id)
        .update(
            mapOf(
                "totalClasses" to newTotal,
                "attendedClasses" to newAttended
            )
        )
        .addOnFailureListener { exception ->

            onError(
                exception.message ?: "Failed to update attendance."
            )
        }
}

@Composable
fun AttendanceSubjectCard(
    record: AttendanceRecord,
    onPresent: () -> Unit,
    onAbsent: () -> Unit,
    onDelete: () -> Unit
) {

    val colors = MaterialTheme.colorScheme

    val percentage = attendancePercentage(record)

    val total = record.totalClasses.coerceAtLeast(0)

    val attended = record.attendedClasses.coerceIn(0, total)

    val missed = (total - attended).coerceAtLeast(0)

    val isSatisfactory = percentage >= REQUIRED_ATTENDANCE

    val statusColor = if (isSatisfactory) {
        Color(0xFF249B68)
    } else {
        colors.error
    }

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = record.subject,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isSatisfactory) {
                            "Attendance on track"
                        } else {
                            "Attendance needs improvement"
                        },
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = formatAttendance(percentage),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .background(
                        colors.surfaceVariant,
                        RoundedCornerShape(20.dp)
                    )
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            (percentage / 100).toFloat()
                                .coerceIn(0f, 1f)
                        )
                        .fillMaxHeight()
                        .background(
                            statusColor,
                            RoundedCornerShape(20.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                AttendanceMiniStat(
                    label = "Total Classes",
                    value = total.toString()
                )

                AttendanceMiniStat(
                    label = "Attended",
                    value = attended.toString(),
                    valueColor = Color(0xFF249B68)
                )

                AttendanceMiniStat(
                    label = "Missed",
                    value = missed.toString(),
                    valueColor = colors.error
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            if (total > 0 && percentage < REQUIRED_ATTENDANCE) {

                val requiredClasses = kotlin.math.ceil(
                    (REQUIRED_ATTENDANCE * total - 100.0 * attended) /
                            (100.0 - REQUIRED_ATTENDANCE)
                ).toInt().coerceAtLeast(0)

                Text(
                    text = "You need to attend the next $requiredClasses consecutive classes to reach 75%, assuming no more absences.",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    onClick = onPresent,
                    modifier = Modifier
                        .weight(1f)
                        .height(43.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Text(
                        "+ Present",
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onAbsent,
                    modifier = Modifier
                        .weight(1f)
                        .height(43.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Text(
                        "+ Absent",
                        fontSize = 13.sp,
                        color = colors.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                TextButton(
                    onClick = onDelete
                ) {

                    Text(
                        "Delete record",
                        color = colors.error,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceMiniStat(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {

    Column {

        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
fun AddAttendanceDialog(
    onDismiss: () -> Unit,
    onAddAttendance: (AttendanceRecord) -> Unit
) {

    var subject by remember { mutableStateOf("") }
    var totalClasses by remember { mutableStateOf("") }
    var attendedClasses by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = "Add Subject Attendance",
                fontWeight = FontWeight.Bold
            )
        },

        text = {

            Column {

                Text(
                    text = "Enter the current attendance details for this subject.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        validationError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject Name") },
                    placeholder = { Text("Example: Data Structures") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = totalClasses,
                    onValueChange = {
                        if (it.all(Char::isDigit)) {
                            totalClasses = it
                            validationError = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Total Classes Held") },
                    placeholder = { Text("Example: 25") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = attendedClasses,
                    onValueChange = {
                        if (it.all(Char::isDigit)) {
                            attendedClasses = it
                            validationError = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Classes Attended") },
                    placeholder = { Text("Example: 20") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
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

                    val total = totalClasses.toIntOrNull()
                    val attended = attendedClasses.toIntOrNull()

                    when {

                        subject.isBlank() -> {
                            validationError = "Please enter a subject name."
                        }

                        total == null || attended == null -> {
                            validationError = "Please enter valid class numbers."
                        }

                        total <= 0 -> {
                            validationError = "Total classes must be greater than zero."
                        }

                        attended < 0 || attended > total -> {
                            validationError =
                                "Attended classes cannot exceed total classes."
                        }

                        else -> {

                            onAddAttendance(
                                AttendanceRecord(
                                    subject = subject.trim(),
                                    totalClasses = total,
                                    attendedClasses = attended
                                )
                            )
                        }
                    }
                }
            ) {

                Text("Save Attendance")
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
