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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

    var showAddTimetable by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val timetableEntries = remember {
        mutableStateListOf<TimetableEntry>()
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
                    }

                    isLoading = false
                }
                .addOnFailureListener { exception ->

                    errorMessage =
                        exception.message ?: "Failed to load timetable."

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
            text = "📅 Timetable",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Manage your weekly class schedule",
            fontSize = 15.sp,
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {
                showAddTimetable = true
                errorMessage = ""
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3F51B5)
            )
        ) {

            Text(
                text = "+ Add Class",
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
                    text = "Loading timetable...",
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

        } else if (timetableEntries.isEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "📅",
                    fontSize = 50.sp
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "No classes added yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172554)
                )

                Text(
                    text = "Add your first class to build your timetable.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

        } else {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = timetableEntries,
                    key = { it.id }
                ) { entry ->

                    TimetableCard(
                        entry = entry,
                        onDelete = {

                            currentUser?.let { user ->

                                firestore
                                    .collection("users")
                                    .document(user.uid)
                                    .collection("timetable")
                                    .document(entry.id)
                                    .delete()
                                    .addOnSuccessListener {

                                        timetableEntries.remove(entry)
                                    }
                                    .addOnFailureListener { exception ->

                                        errorMessage =
                                            exception.message
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

                if (currentUser == null) {

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

                    firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .collection("timetable")
                        .add(timetableData)
                        .addOnSuccessListener { documentReference ->

                            timetableEntries.add(
                                entry.copy(
                                    id = documentReference.id
                                )
                            )

                            showAddTimetable = false
                            errorMessage = ""
                        }
                        .addOnFailureListener { exception ->

                            errorMessage =
                                exception.message
                                    ?: "Failed to add class."
                        }
                }
            }
        )
    }
}

@Composable
fun TimetableCard(
    entry: TimetableEntry,
    onDelete: () -> Unit
) {

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
                text = entry.subject,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172554)
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "📅 ${entry.day}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = "⏰ ${entry.startTime} - ${entry.endTime}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = "🏫 ${entry.classroom}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(
                modifier = Modifier.height(8.dp)
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
fun AddTimetableDialog(
    onDismiss: () -> Unit,
    onAddTimetable: (TimetableEntry) -> Unit
) {

    var subject by remember {
        mutableStateOf("")
    }

    var day by remember {
        mutableStateOf("")
    }

    var startTime by remember {
        mutableStateOf("")
    }

    var endTime by remember {
        mutableStateOf("")
    }

    var classroom by remember {
        mutableStateOf("")
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    val days = listOf(
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday"
    )

    AlertDialog(

        onDismissRequest = {
            onDismiss()
        },

        title = {
            Text(
                text = "Add Class",
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
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Column {

                    OutlinedTextField(
                        value = day,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Day")
                        },
                        readOnly = true,
                        singleLine = true
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {

                        days.forEach { selectedDay ->

                            DropdownMenuItem(

                                text = {
                                    Text(selectedDay)
                                },

                                onClick = {

                                    day = selectedDay
                                    expanded = false
                                }
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Button(
                        onClick = {
                            expanded = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = if (day.isEmpty()) {
                                "Select Day"
                            } else {
                                day
                            }
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = startTime,
                    onValueChange = {
                        startTime = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Start Time")
                    },
                    placeholder = {
                        Text("Example: 09:00 AM")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = {
                        endTime = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("End Time")
                    },
                    placeholder = {
                        Text("Example: 10:00 AM")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = classroom,
                    onValueChange = {
                        classroom = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Classroom")
                    },
                    placeholder = {
                        Text("Example: C-204")
                    },
                    singleLine = true
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

                                subject = subject,

                                day = day,

                                startTime = startTime,

                                endTime = endTime,

                                classroom = classroom
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