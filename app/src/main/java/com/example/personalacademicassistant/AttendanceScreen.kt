
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.round

data class AttendanceRecord(
    val id: String = "",
    val subject: String = "",
    val totalClasses: Int = 0,
    val attendedClasses: Int = 0
)

@Composable
fun AttendanceScreen() {

    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val userId = auth.currentUser?.uid

    var showAddAttendance by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    var attendanceRecords by remember {
        mutableStateOf<List<AttendanceRecord>>(emptyList())
    }

    val colors = MaterialTheme.colorScheme

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
                .addSnapshotListener { result, error ->

                    if (error != null) {
                        errorMessage = error.message
                            ?: "Unable to load attendance."
                        isLoading = false
                        return@addSnapshotListener
                    }

                    attendanceRecords = result?.documents?.map { document ->
                        AttendanceRecord(
                            id = document.id,
                            subject = document.getString("subject") ?: "",
                            totalClasses = document.getLong("totalClasses")?.toInt() ?: 0,
                            attendedClasses = document.getLong("attendedClasses")?.toInt() ?: 0
                        )
                    } ?: emptyList()

                    isLoading = false
                }

            onDispose {
                listener.remove()
            }
        }
    }

    val totalClasses = attendanceRecords.sumOf { it.totalClasses }
    val attendedClasses = attendanceRecords.sumOf { it.attendedClasses }

    val overallPercentage = if (totalClasses > 0) {
        attendedClasses.toDouble() / totalClasses * 100
    } else {
        0.0
    }

    val roundedOverall = round(overallPercentage * 100) / 100

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(20.dp)
    ) {

        Text(
            text = "📊 Attendance",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Track your subject-wise attendance",
            fontSize = 14.sp,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Overall Attendance",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$roundedOverall%",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .background(
                            colors.surface.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                (overallPercentage / 100.0)
                                    .toFloat()
                                    .coerceIn(0f, 1f)
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
                    text = if (totalClasses == 0) {
                        "Add attendance to see your progress."
                    } else if (overallPercentage < 75) {
                        "⚠️ Your overall attendance is below 75%."
                    } else {
                        "✅ Your overall attendance is satisfactory."
                    },
                    fontSize = 13.sp,
                    color = colors.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            AttendanceSummaryCard(
                title = "Total Classes",
                value = totalClasses.toString(),
                modifier = Modifier.weight(1f)
            )

            AttendanceSummaryCard(
                title = "Attended",
                value = attendedClasses.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                errorMessage = ""
                showAddAttendance = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "+ Add Attendance",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Subject-wise Attendance",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {

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
                    text = "Loading attendance...",
                    color = colors.onSurfaceVariant
                )
            }

        } else if (attendanceRecords.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "📊",
                    fontSize = 48.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "No attendance records yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )

                Text(
                    text = "Add your first subject's attendance.",
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = colors.error,
                        fontSize = 13.sp
                    )
                }
            }

        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = attendanceRecords,
                    key = { it.id }
                ) { record ->

                    AttendanceCard(
                        record = record,

                        onPresent = {
                            updateAttendance(
                                firestore = firestore,
                                userId = userId,
                                record = record,
                                newTotal = record.totalClasses + 1,
                                newAttended = record.attendedClasses + 1,
                                onError = { errorMessage = it }
                            )
                        },

                        onAbsent = {
                            updateAttendance(
                                firestore = firestore,
                                userId = userId,
                                record = record,
                                newTotal = record.totalClasses + 1,
                                newAttended = record.attendedClasses,
                                onError = { errorMessage = it }
                            )
                        },

                        onDelete = {
                            if (userId != null) {
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
                        }
                    )
                }

                if (errorMessage.isNotBlank()) {
                    item {
                        Text(
                            text = errorMessage,
                            color = colors.error,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showAddAttendance) {
        AddAttendanceDialog(
            onDismiss = {
                showAddAttendance = false
            },
            onAddAttendance = { record ->

                if (userId == null) {
                    errorMessage = "Please log in first."
                } else {

                    val attendanceData = hashMapOf(
                        "subject" to record.subject,
                        "totalClasses" to record.totalClasses,
                        "attendedClasses" to record.attendedClasses
                    )

                    firestore.collection("users")
                        .document(userId)
                        .collection("attendance")
                        .add(attendanceData)
                        .addOnSuccessListener {
                            showAddAttendance = false
                            errorMessage = ""
                        }
                        .addOnFailureListener { exception ->
                            errorMessage = exception.message
                                ?: "Failed to add attendance."
                        }
                }
            }
        )
    }
}

@Composable
fun AttendanceSummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 25.sp,
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
    newTotal: Int,
    newAttended: Int,
    onError: (String) -> Unit
) {

    if (userId == null) {
        onError("Please log in first.")
        return
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
fun AttendanceCard(
    record: AttendanceRecord,
    onPresent: () -> Unit,
    onAbsent: () -> Unit,
    onDelete: () -> Unit
) {

    val colors = MaterialTheme.colorScheme

    val percentage = if (record.totalClasses > 0) {
        record.attendedClasses.toDouble() / record.totalClasses * 100
    } else {
        0.0
    }

    val roundedPercentage = round(percentage * 100) / 100

    val statusColor = if (percentage < 75) {
        colors.error
    } else {
        Color(0xFF219653)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = record.subject,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Classes Held",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant
                )

                Text(
                    text = record.totalClasses.toString(),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Classes Attended",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant
                )

                Text(
                    text = record.attendedClasses.toString(),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Attendance",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface
                )

                Text(
                    text = "$roundedPercentage%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                            (percentage / 100.0)
                                .toFloat()
                                .coerceIn(0f, 1f)
                        )
                        .fillMaxHeight()
                        .background(
                            statusColor,
                            RoundedCornerShape(20.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (percentage < 75) {
                    "⚠️ Attendance is below 75%"
                } else {
                    "✅ Attendance is satisfactory"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Button(
                    onClick = onPresent,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Present")
                }

                Button(
                    onClick = onAbsent,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error
                    )
                ) {
                    Text("Absent")
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
                        text = "Delete",
                        color = colors.error
                    )
                }
            }
        }
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
                text = "Add Attendance",
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
                    placeholder = { Text("Example: DBMS") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = totalClasses,
                    onValueChange = {
                        totalClasses = it
                        validationError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Total Classes") },
                    placeholder = { Text("Example: 20") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = attendedClasses,
                    onValueChange = {
                        attendedClasses = it
                        validationError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Classes Attended") },
                    placeholder = { Text("Example: 17") },
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
                            validationError = "Enter a subject name."
                        }

                        total == null || attended == null -> {
                            validationError = "Enter valid class numbers."
                        }

                        total <= 0 -> {
                            validationError = "Total classes must be greater than zero."
                        }

                        attended < 0 || attended > total -> {
                            validationError =
                                "Attended classes must be between 0 and total classes."
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
                Text("Add")
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
