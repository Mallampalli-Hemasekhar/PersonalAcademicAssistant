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

data class Exam(
    val id: String = "",
    val subject: String = "",
    val examType: String = "",
    val examDate: String = "",
    val examTime: String = "",
    val location: String = ""
)

@Composable
fun ExamScreen() {

    var showAddExam by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val exams = remember {
        mutableStateListOf<Exam>()
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
                .collection("exams")
                .get()
                .addOnSuccessListener { result ->

                    exams.clear()

                    for (document in result.documents) {

                        val exam = Exam(
                            id = document.id,
                            subject = document.getString("subject") ?: "",
                            examType = document.getString("examType") ?: "",
                            examDate = document.getString("examDate") ?: "",
                            examTime = document.getString("examTime") ?: "",
                            location = document.getString("location") ?: ""
                        )

                        exams.add(exam)
                    }

                    isLoading = false
                }
                .addOnFailureListener { exception ->

                    errorMessage =
                        exception.message ?: "Failed to load exams."

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
            text = "📝 Exams",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Manage your upcoming examinations",
            fontSize = 15.sp,
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {
                showAddExam = true
                errorMessage = ""
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3F51B5)
            )
        ) {

            Text(
                text = "+ Add Exam",
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
                    text = "Loading exams...",
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

        } else if (exams.isEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "📝",
                    fontSize = 50.sp
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "No exams added yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172554)
                )

                Text(
                    text = "Add your first exam to get started.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

        } else {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = exams,
                    key = { it.id }
                ) { exam ->

                    ExamCard(
                        exam = exam,
                        onDelete = {

                            currentUser?.let { user ->

                                firestore
                                    .collection("users")
                                    .document(user.uid)
                                    .collection("exams")
                                    .document(exam.id)
                                    .delete()
                                    .addOnSuccessListener {

                                        exams.remove(exam)
                                    }
                                    .addOnFailureListener { exception ->

                                        errorMessage =
                                            exception.message
                                                ?: "Failed to delete exam."
                                    }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddExam) {

        AddExamDialog(

            onDismiss = {
                showAddExam = false
            },

            onAddExam = { exam ->

                if (currentUser == null) {

                    errorMessage = "User is not logged in."
                    showAddExam = false

                } else {

                    val examData = hashMapOf(

                        "subject" to exam.subject,

                        "examType" to exam.examType,

                        "examDate" to exam.examDate,

                        "examTime" to exam.examTime,

                        "location" to exam.location
                    )

                    firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .collection("exams")
                        .add(examData)
                        .addOnSuccessListener { documentReference ->

                            exams.add(
                                exam.copy(
                                    id = documentReference.id
                                )
                            )

                            showAddExam = false
                            errorMessage = ""
                        }
                        .addOnFailureListener { exception ->

                            errorMessage =
                                exception.message
                                    ?: "Failed to add exam."
                        }
                }
            }
        )
    }
}

@Composable
fun ExamCard(
    exam: Exam,
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
                text = exam.subject,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172554)
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "📌 Type: ${exam.examType}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = "📅 Date: ${exam.examDate}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = "⏰ Time: ${exam.examTime}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = "🏫 Location: ${exam.location}",
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
fun AddExamDialog(
    onDismiss: () -> Unit,
    onAddExam: (Exam) -> Unit
) {

    var subject by remember {
        mutableStateOf("")
    }

    var examType by remember {
        mutableStateOf("")
    }

    var examDate by remember {
        mutableStateOf("")
    }

    var examTime by remember {
        mutableStateOf("")
    }

    var location by remember {
        mutableStateOf("")
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    val examTypes = listOf(
        "Internal",
        "Midterm",
        "Semester",
        "Practical",
        "Viva",
        "Other"
    )

    AlertDialog(

        onDismissRequest = {
            onDismiss()
        },

        title = {
            Text(
                text = "Add Exam",
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

                Column {

                    OutlinedTextField(
                        value = examType,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Exam Type")
                        },
                        readOnly = true,
                        singleLine = true
                    )

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
                            text = if (examType.isEmpty()) {
                                "Select Exam Type"
                            } else {
                                examType
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {

                        examTypes.forEach { selectedType ->

                            DropdownMenuItem(

                                text = {
                                    Text(selectedType)
                                },

                                onClick = {

                                    examType = selectedType
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = examDate,
                    onValueChange = {
                        examDate = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Exam Date")
                    },
                    placeholder = {
                        Text("Example: 25 Oct 2026")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = examTime,
                    onValueChange = {
                        examTime = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Exam Time")
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
                    value = location,
                    onValueChange = {
                        location = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Exam Location")
                    },
                    placeholder = {
                        Text("Example: Block A - Hall 204")
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
                        examType.isNotBlank() &&
                        examDate.isNotBlank() &&
                        examTime.isNotBlank() &&
                        location.isNotBlank()
                    ) {

                        onAddExam(

                            Exam(
                                subject = subject,
                                examType = examType,
                                examDate = examDate,
                                examTime = examTime,
                                location = location
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