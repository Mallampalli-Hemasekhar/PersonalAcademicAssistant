
package com.example.personalacademicassistant

import androidx.compose.foundation.background
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

                        val record = GPASubject(
                            id = document.id,
                            semester = document.getString("semester") ?: "",
                            subject = document.getString("subject") ?: "",
                            credits = (document.getLong("credits") ?: 0L).toInt(),
                            grade = document.getString("grade") ?: "",
                            gradePoint = (document.getLong("gradePoint") ?: 0L).toInt()
                        )

                        subjects.add(record)
                    }

                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    errorMessage =
                        exception.message ?: "Failed to load GPA records."
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
            .background(Color(0xFFF5F7FB))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            text = "🎓 GPA Calculator",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Calculate your semester GPA and overall CGPA.",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        // GPA and CGPA result cards

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3F51B5)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Semester GPA",
                        color = Color.White,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = String.format("%.2f", semesterGPA),
                        color = Color.White,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF00897B)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Overall CGPA",
                        color = Color.White,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = String.format("%.2f", cgpa),
                        color = Color.White,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        // Semester input

        Text(
            text = "Semester",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = semester,
            onValueChange = { semester = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Semester Name") },
            placeholder = { Text("Example: Semester 1") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Add subject form

        Text(
            text = "Add Subject Grade",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Subject Name") },
            placeholder = { Text("Example: Data Structures") },
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
            label = { Text("Subject Credits") },
            placeholder = { Text("Example: 4") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Grade dropdown

        Text(
            text = "Select Grade",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF172554)
        )

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

        Spacer(modifier = Modifier.height(18.dp))

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

                            errorMessage =
                                exception.message ?: "Failed to save subject."

                            isSaving = false
                        }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3F51B5)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isSaving) "Saving..." else "+ Add Subject",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        // Subject records

        Text(
            text = "Subject Records",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {

            Text(
                text = "Loading GPA records...",
                color = Color.Gray
            )

        } else if (semesterSubjects.isEmpty()) {

            Text(
                text = "No subjects added for this semester yet.",
                color = Color.Gray
            )

        } else {

            semesterSubjects.forEach { record ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = record.subject,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF172554)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Credits: ${record.credits}",
                            color = Color.Gray
                        )

                        Text(
                            text = "Grade: ${record.grade}",
                            color = Color.Gray
                        )

                        Text(
                            text = "Grade Points: ${record.gradePoint}",
                            color = Color.Gray
                        )

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
                                                errorMessage =
                                                    exception.message
                                                        ?: "Failed to delete record."
                                            }
                                    }
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
        }

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "Semester Total Credits: $totalCredits",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Text(
            text = "Overall Total Credits: $overallCredits",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )
    }
}
