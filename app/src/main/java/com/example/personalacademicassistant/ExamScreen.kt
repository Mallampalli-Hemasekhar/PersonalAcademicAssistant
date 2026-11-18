
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    var showAddExam by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val exams = remember { mutableStateListOf<Exam>() }

    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser
    val context = LocalContext.current

    LaunchedEffect(Unit) {

        if (currentUser == null) {
            errorMessage = "User is not logged in."
            isLoading = false
        } else {

            firestore.collection("users")
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

                        // Schedule reminders for existing exams.
                        ExamReminderScheduler.scheduleExam(
                            context = context,
                            examId = exam.id,
                            subject = exam.subject,
                            examDate = exam.examDate,
                            examTime = exam.examTime,
                            location = exam.location
                        )
                    }

                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    errorMessage = exception.message
                        ?: "Failed to load exams."
                    isLoading = false
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {

        Text(
            text = "Exam Management",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Organize and track your examinations",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ExamStatCard(
                title = "Total Exams",
                value = exams.size.toString(),
                modifier = Modifier.weight(1f)
            )

            ExamStatCard(
                title = "Exam Types",
                value = exams.map { it.examType }.distinct().size.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                errorMessage = ""
                showAddExam = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "+ Add New Exam",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (errorMessage.isNotEmpty()) {

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        when {

            isLoading -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            exams.isEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "📝",
                            fontSize = 55.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "No Exams Yet",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Add your examinations to keep track of them.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {

                Text(
                    text = "Your Examinations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
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

                                    firestore.collection("users")
                                        .document(user.uid)
                                        .collection("exams")
                                        .document(exam.id)
                                        .delete()
                                        .addOnSuccessListener {

                                            // Cancel all reminders for this exam.
                                            ExamReminderScheduler.cancelExam(
                                                context = context,
                                                examId = exam.id
                                            )

                                            exams.remove(exam)
                                        }
                                        .addOnFailureListener { exception ->

                                            errorMessage = exception.message
                                                ?: "Failed to delete exam."
                                        }
                                }
                            }
                        )
                    }
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

                    firestore.collection("users")
                        .document(currentUser.uid)
                        .collection("exams")
                        .add(examData)
                        .addOnSuccessListener { documentReference ->

                            val savedExam = exam.copy(
                                id = documentReference.id
                            )

                            exams.add(savedExam)

                            // Schedule 3 advance reminders.
                            ExamReminderScheduler.scheduleExam(
                                context = context,
                                examId = savedExam.id,
                                subject = savedExam.subject,
                                examDate = savedExam.examDate,
                                examTime = savedExam.examTime,
                                location = savedExam.location
                            )

                            showAddExam = false
                            errorMessage = ""
                        }
                        .addOnFailureListener { exception ->

                            errorMessage = exception.message
                                ?: "Failed to add exam."
                        }
                }
            }
        )
    }
}

@Composable
fun ExamStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ExamCard(
    exam: Exam,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        text = exam.subject,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {

                        Text(
                            text = exam.examType,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExamInfoRow("📅", "Date", exam.examDate)
            ExamInfoRow("⏰", "Time", exam.examTime)
            ExamInfoRow("📍", "Location", exam.location)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                TextButton(onClick = onDelete) {

                    Text(
                        text = "Delete Exam",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ExamInfoRow(
    icon: String,
    label: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = icon,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "$label:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(70.dp)
        )

        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AddExamDialog(
    onDismiss: () -> Unit,
    onAddExam: (Exam) -> Unit
) {

    var subject by remember { mutableStateOf("") }
    var examType by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf("") }
    var examTime by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }

    val examTypes = listOf(
        "Internal",
        "Midterm",
        "Semester",
        "Practical",
        "Viva",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Add New Exam",
                fontWeight = FontWeight.Bold
            )
        },
        text = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        validationMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject") },
                    placeholder = { Text("Example: DBMS") },
                    singleLine = true
                )

                Box {

                    OutlinedTextField(
                        value = examType,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Exam Type") },
                        placeholder = { Text("Select exam type") },
                        readOnly = true,
                        singleLine = true
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(top = 8.dp)
                    ) {

                        TextButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxSize()
                        ) {}
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {

                        examTypes.forEach { type ->

                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    examType = type
                                    expanded = false
                                    validationMessage = ""
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = examDate,
                    onValueChange = {
                        examDate = it
                        validationMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Exam Date") },
                    placeholder = { Text("Example: 25 Oct 2026") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = examTime,
                    onValueChange = {
                        examTime = it
                        validationMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Exam Time") },
                    placeholder = { Text("Example: 10:00 AM") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = {
                        location = it
                        validationMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Exam Location") },
                    placeholder = { Text("Example: Block A - Hall 204") },
                    singleLine = true
                )

                if (validationMessage.isNotEmpty()) {

                    Text(
                        text = validationMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
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
                                subject = subject.trim(),
                                examType = examType,
                                examDate = examDate.trim(),
                                examTime = examTime.trim(),
                                location = location.trim()
                            )
                        )

                    } else {

                        validationMessage = "Please fill in all fields."
                    }
                }
            ) {
                Text("Add Exam")
            }
        },
        dismissButton = {

            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
