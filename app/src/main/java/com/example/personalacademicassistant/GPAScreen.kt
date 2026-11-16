
package com.example.personalacademicassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

data class GPASubject(
    val id: String = "",
    val semester: String = "",
    val subject: String = "",
    val credits: Int = 0,
    val grade: String = "",
    val gradePoint: Int = 0
)

private val gradePoints = mapOf(
    "O" to 10,
    "A+" to 9,
    "A" to 8,
    "B+" to 7,
    "B" to 6,
    "C" to 5,
    "F" to 0
)

@Composable
fun GPAScreen() {

    var semester by remember { mutableStateOf("Semester 1") }
    var subject by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf("O") }

    var showGradeMenu by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val subjects = remember {
        mutableStateListOf<GPASubject>()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val currentUser = auth.currentUser

    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(currentUser?.uid) {

        if (currentUser == null) {
            errorMessage = "Please login to use the GPA Calculator."
            isLoading = false
        } else {

            firestore
                .collection("users")
                .document(currentUser.uid)
                .collection("gpaSubjects")
                .get()
                .addOnSuccessListener { result ->

                    subjects.clear()

                    for (document in result.documents) {

                        subjects.add(
                            GPASubject(
                                id = document.id,
                                semester = document.getString("semester") ?: "",
                                subject = document.getString("subject") ?: "",
                                credits = (document.getLong("credits") ?: 0L).toInt(),
                                grade = document.getString("grade") ?: "",
                                gradePoint = (document.getLong("gradePoint") ?: 0L).toInt()
                            )
                        )
                    }

                    isLoading = false
                }
                .addOnFailureListener { exception ->

                    errorMessage = exception.message
                        ?: "Failed to load GPA records."

                    isLoading = false
                }
        }
    }

    val semesterSubjects = subjects.filter {
        it.semester.equals(semester.trim(), ignoreCase = true)
    }

    val totalCredits = semesterSubjects.sumOf {
        it.credits
    }

    val totalGradePoints = semesterSubjects.sumOf {
        it.credits * it.gradePoint
    }

    val semesterGPA = if (totalCredits > 0) {
        totalGradePoints.toDouble() / totalCredits
    } else {
        0.0
    }

    val overallCredits = subjects.sumOf {
        it.credits
    }

    val overallGradePoints = subjects.sumOf {
        it.credits * it.gradePoint
    }

    val cgpa = if (overallCredits > 0) {
        overallGradePoints.toDouble() / overallCredits
    } else {
        0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            text = "🎓 GPA Calculator",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Track your academic performance and calculate your GPA.",
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(22.dp))

        // GPA and CGPA cards

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            GPAScoreCard(
                title = "Semester GPA",
                value = String.format("%.2f", semesterGPA),
                modifier = Modifier.weight(1f),
                containerColor = Color(0xFF4F46B5)
            )

            GPAScoreCard(
                title = "Overall CGPA",
                value = String.format("%.2f", cgpa),
                modifier = Modifier.weight(1f),
                containerColor = Color(0xFF008F83)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary cards

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            GPASummaryCard(
                title = "Semester Credits",
                value = totalCredits.toString(),
                modifier = Modifier.weight(1f)
            )

            GPASummaryCard(
                title = "Total Subjects",
                value = semesterSubjects.size.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Semester selection

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Select Semester",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = semester,
                    onValueChange = {
                        semester = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Semester Name")
                    },
                    placeholder = {
                        Text("Example: Semester 1")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Add subject form

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "➕ Add Subject Grade",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Subject Name")
                    },
                    placeholder = {
                        Text("Example: Data Structures")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = credits,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            credits = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Subject Credits")
                    },
                    placeholder = {
                        Text("Example: 4")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Grade",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box {

                    OutlinedButton(
                        onClick = {
                            showGradeMenu = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Text(
                            text = "$selectedGrade - ${gradePoints[selectedGrade]} Grade Points"
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text("▼")
                    }

                    DropdownMenu(
                        expanded = showGradeMenu,
                        onDismissRequest = {
                            showGradeMenu = false
                        }
                    ) {

                        gradePoints.forEach { (grade, point) ->

                            DropdownMenuItem(
                                text = {
                                    Text("$grade - $point Grade Points")
                                },
                                onClick = {
                                    selectedGrade = grade
                                    showGradeMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {

                        val creditValue = credits.toIntOrNull()

                        if (currentUser == null) {

                            errorMessage = "Please login first."

                        } else if (semester.isBlank()) {

                            errorMessage = "Please enter a semester name."

                        } else if (subject.isBlank()) {

                            errorMessage = "Please enter a subject name."

                        } else if (creditValue == null || creditValue <= 0) {

                            errorMessage = "Please enter valid subject credits."

                        } else {

                            errorMessage = ""
                            isSaving = true

                            val gradePoint = gradePoints[selectedGrade] ?: 0

                            val subjectData = hashMapOf(
                                "semester" to semester.trim(),
                                "subject" to subject.trim(),
                                "credits" to creditValue,
                                "grade" to selectedGrade,
                                "gradePoint" to gradePoint
                            )

                            firestore
                                .collection("users")
                                .document(currentUser.uid)
                                .collection("gpaSubjects")
                                .add(subjectData)
                                .addOnSuccessListener { document ->

                                    subjects.add(
                                        GPASubject(
                                            id = document.id,
                                            semester = semester.trim(),
                                            subject = subject.trim(),
                                            credits = creditValue,
                                            grade = selectedGrade,
                                            gradePoint = gradePoint
                                        )
                                    )

                                    subject = ""
                                    credits = ""
                                    selectedGrade = "O"
                                    isSaving = false
                                }
                                .addOnFailureListener { exception ->

                                    errorMessage = exception.message
                                        ?: "Failed to save subject."

                                    isSaving = false
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Text(
                        text = if (isSaving) "Saving..." else "+ Add Subject",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Error message

        if (errorMessage.isNotEmpty()) {

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = errorMessage,
                color = colorScheme.error,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        // Subject records

        Text(
            text = "📚 Subject Records",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(25.dp),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }

            semesterSubjects.isEmpty() -> {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surfaceVariant
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "📖",
                            fontSize = 35.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "No Subjects Yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )

                        Text(
                            text = "Add your subject grades to calculate your GPA.",
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {

                semesterSubjects.forEach { record ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surface
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
                                        text = record.subject,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )

                                    Text(
                                        text = record.semester,
                                        fontSize = 12.sp,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colorScheme.primaryContainer
                                ) {

                                    Text(
                                        text = "Grade ${record.grade}",
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 7.dp
                                        ),
                                        color = colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            HorizontalDivider(
                                color = colorScheme.outlineVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                GPARecordDetail(
                                    title = "Credits",
                                    value = record.credits.toString()
                                )

                                GPARecordDetail(
                                    title = "Grade Points",
                                    value = record.gradePoint.toString()
                                )

                                GPARecordDetail(
                                    title = "Weighted Points",
                                    value = (record.credits * record.gradePoint).toString()
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {

                                TextButton(
                                    onClick = {

                                        currentUser?.let { user ->

                                            firestore
                                                .collection("users")
                                                .document(user.uid)
                                                .collection("gpaSubjects")
                                                .document(record.id)
                                                .delete()
                                                .addOnSuccessListener {

                                                    subjects.remove(record)
                                                }
                                                .addOnFailureListener { exception ->

                                                    errorMessage = exception.message
                                                        ?: "Failed to delete record."
                                                }
                                        }
                                    }
                                ) {

                                    Text(
                                        text = "Delete",
                                        color = colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Credit summary

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.secondaryContainer
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Credit Summary",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Semester Total Credits: $totalCredits",
                    color = colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "Overall Total Credits: $overallCredits",
                    color = colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun GPAScoreCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = title,
                fontSize = 13.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun GPASummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun GPARecordDetail(
    title: String,
    value: String
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = title,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
