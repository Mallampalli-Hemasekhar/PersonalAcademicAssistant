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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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

    var showAddAttendance by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val attendanceRecords = remember {
        mutableStateListOf<AttendanceRecord>()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val currentUser = auth.currentUser

    LaunchedEffect(Unit) {

        if (currentUser == null) {

            errorMessage = "User is not logged in."
            isLoading = false

        } else {

            firestore
                .collection("users")
                .document(currentUser.uid)
                .collection("attendance")
                .get()
                .addOnSuccessListener { result ->

                    attendanceRecords.clear()

                    for (document in result.documents) {

                        val record = AttendanceRecord(
                            id = document.id,
                            subject = document.getString("subject") ?: "",
                            totalClasses =
                                document.getLong("totalClasses")?.toInt() ?: 0,
                            attendedClasses =
                                document.getLong("attendedClasses")?.toInt() ?: 0
                        )

                        attendanceRecords.add(record)
                    }

                    isLoading = false
                }
                .addOnFailureListener { exception ->

                    errorMessage =
                        exception.message ?: "Failed to load attendance."

                    isLoading = false
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

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Track your subject-wise attendance",
            fontSize = 15.sp,
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

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

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        if (isLoading) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "Loading attendance...",
                    color = Color.Gray
                )
            }

        } else if (errorMessage.isNotEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "⚠️",
                    fontSize = 45.sp
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = errorMessage,
                    color = Color.Red
                )
            }

        } else if (attendanceRecords.isEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "📊",
                    fontSize = 50.sp
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

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
                                userId = currentUser?.uid,
                                record = record,
                                newTotal = record.totalClasses + 1,
                                newAttended = record.attendedClasses + 1,
                                attendanceRecords = attendanceRecords,
                                onError = { message ->
                                    errorMessage = message
                                }
                            )
                        },

                        onAbsent = {

                            updateAttendance(
                                firestore = firestore,
                                userId = currentUser?.uid,
                                record = record,
                                newTotal = record.totalClasses + 1,
                                newAttended = record.attendedClasses,
                                attendanceRecords = attendanceRecords,
                                onError = { message ->
                                    errorMessage = message
                                }
                            )
                        },

                        onDelete = {

                            currentUser?.let { user ->

                                firestore
                                    .collection("users")
                                    .document(user.uid)
                                    .collection("attendance")
                                    .document(record.id)
                                    .delete()
                                    .addOnSuccessListener {

                                        attendanceRecords.remove(record)
                                    }
                                    .addOnFailureListener { exception ->

                                        errorMessage =
                                            exception.message
                                                ?: "Failed to delete attendance."
                                    }
                            }
                        }
                    )
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

                if (currentUser == null) {

                    errorMessage = "User is not logged in."
                    showAddAttendance = false

                } else {

                    val attendanceData = hashMapOf(

                        "subject" to record.subject,

                        "totalClasses" to record.totalClasses,

                        "attendedClasses" to record.attendedClasses
                    )

                    firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .collection("attendance")
                        .add(attendanceData)
                        .addOnSuccessListener { documentReference ->

                            attendanceRecords.add(
                                record.copy(
                                    id = documentReference.id
                                )
                            )

                            showAddAttendance = false
                            errorMessage = ""
                        }
                        .addOnFailureListener { exception ->

                            errorMessage =
                                exception.message
                                    ?: "Failed to add attendance."
                        }
                }
            }
        )
    }
}

fun updateAttendance(
    firestore: FirebaseFirestore,
    userId: String?,
    record: AttendanceRecord,
    newTotal: Int,
    newAttended: Int,
    attendanceRecords: MutableList<AttendanceRecord>,
    onError: (String) -> Unit
) {

    if (userId == null) {
        onError("User is not logged in.")
        return
    }

    firestore
        .collection("users")
        .document(userId)
        .collection("attendance")
        .document(record.id)
        .update(
            mapOf(
                "totalClasses" to newTotal,
                "attendedClasses" to newAttended
            )
        )
        .addOnSuccessListener {

            val index = attendanceRecords.indexOf(record)

            if (index != -1) {

                attendanceRecords[index] =
                    record.copy(
                        totalClasses = newTotal,
                        attendedClasses = newAttended
                    )
            }
        }
        .addOnFailureListener { exception ->

            onError(
                exception.message
                    ?: "Failed to update attendance."
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

    val percentage =
        if (record.totalClasses > 0) {
            (record.attendedClasses.toDouble() /
                    record.totalClasses.toDouble()) * 100
        } else {
            0.0
        }

    val roundedPercentage =
        round(percentage * 100) / 100

    val percentageText =
        "$roundedPercentage%"

    val statusColor =
        if (percentage < 75) {
            Color(0xFFD32F2F)
        } else {
            Color(0xFF2E7D32)
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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

            Spacer(
                modifier = Modifier.height(8.dp)
            )

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

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Attendance: $percentageText",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

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

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Button(
                    onClick = {
                        onPresent()
                    }
                ) {

                    Text("Present")
                }

                Button(
                    onClick = {
                        onAbsent()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    )
                ) {

                    Text("Absent")
                }
            }

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                TextButton(
                    onClick = {
                        onDelete()
                    }
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

    var subject by remember {
        mutableStateOf("")
    }

    var totalClasses by remember {
        mutableStateOf("")
    }

    var attendedClasses by remember {
        mutableStateOf("")
    }

    AlertDialog(

        onDismissRequest = {
            onDismiss()
        },

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
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Subject")
                    },
                    placeholder = {
                        Text("Example: DBMS")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = totalClasses,
                    onValueChange = {
                        totalClasses = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Total Classes")
                    },
                    placeholder = {
                        Text("Example: 20")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = attendedClasses,
                    onValueChange = {
                        attendedClasses = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Classes Attended")
                    },
                    placeholder = {
                        Text("Example: 17")
                    },
                    singleLine = true
                )
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    val total = totalClasses.toIntOrNull()
                    val attended = attendedClasses.toIntOrNull()

                    if (
                        subject.isNotBlank() &&
                        total != null &&
                        attended != null &&
                        total > 0 &&
                        attended >= 0 &&
                        attended <= total
                    ) {

                        onAddAttendance(

                            AttendanceRecord(
                                subject = subject,
                                totalClasses = total,
                                attendedClasses = attended
                            )
                        )
                    }
                }
            ) {

                Text("Add")
            }
        },

        dismissButton = {

            TextButton(
                onClick = {
                    onDismiss()
                }
            ) {

                Text("Cancel")
            }
        }
    )
}