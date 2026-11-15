
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    // Real-time Firestore attendance listener
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .padding(20.dp)
    ) {

        Text(
            text = "📊 Attendance",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your attendance overview",
            fontSize = 15.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                showAddAttendance = true
                errorMessage = ""
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3F51B5)
            )
        ) {
            Text(
                text = "+ Add Attendance",
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF3F51B5)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Loading attendance...")
            }

        } else if (errorMessage.isNotBlank() && attendanceRecords.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("⚠️", fontSize = 40.sp)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = errorMessage,
                    color = Color.Red
                )
            }

        } else if (attendanceRecords.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📊", fontSize = 50.sp)

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "No attendance records yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172554)
                )

                Text(
                    text = "Add attendance for your first subject.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
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
            }
        }

        if (errorMessage.isNotBlank() && attendanceRecords.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 13.sp
            )
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

    val percentage = if (record.totalClasses > 0) {
        record.attendedClasses.toDouble() / record.totalClasses * 100
    } else {
        0.0
    }

    val roundedPercentage = round(percentage * 100) / 100

    val statusColor = if (percentage < 75) {
        Color(0xFFD32F2F)
    } else {
        Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172554)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Classes Held: ${record.totalClasses}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = "Classes Attended: ${record.attendedClasses}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Attendance: $roundedPercentage%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (percentage < 75) {
                Text(
                    text = "⚠️ Attendance is below 75%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            } else {
                Text(
                    text = "✅ Attendance is satisfactory",
                    fontSize = 14.sp,
                    color = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Button(
                    onClick = onPresent
                ) {
                    Text("Present")
                }

                Button(
                    onClick = onAbsent,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
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
                        color = Color.Red
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
                        color = Color.Red,
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
                        subject.isBlank() ->
                            validationError = "Enter a subject name."

                        total == null || attended == null ->
                            validationError = "Enter valid class numbers."

                        total <= 0 ->
                            validationError = "Total classes must be greater than zero."

                        attended < 0 || attended > total ->
                            validationError = "Attended classes must be between 0 and total classes."

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
