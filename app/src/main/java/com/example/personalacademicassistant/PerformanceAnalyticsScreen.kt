
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
import java.util.Locale

@Composable
fun PerformanceAnalyticsScreen() {

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val subjects = remember {
        mutableStateListOf<GPASubject>()
    }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()

    // Load GPA records from Firestore
    LaunchedEffect(currentUser?.uid) {

        if (currentUser == null) {

            errorMessage = "Please login to view your performance."
            isLoading = false

        } else {

            firestore.collection("users")
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
                        ?: "Unable to load performance data."

                    isLoading = false
                }
        }
    }

    val totalCredits = subjects.sumOf { it.credits }

    val totalWeightedPoints = subjects.sumOf {
        it.credits * it.gradePoint
    }

    val cgpa = if (totalCredits > 0) {
        totalWeightedPoints.toDouble() / totalCredits
    } else {
        0.0
    }

    val strongestSubject = subjects.maxByOrNull {
        it.gradePoint
    }

    val weakestSubject = subjects.minByOrNull {
        it.gradePoint
    }

    val semesterResults = subjects
        .groupBy { it.semester }
        .map { (semesterName, semesterSubjects) ->

            val credits = semesterSubjects.sumOf {
                it.credits
            }

            val weightedPoints = semesterSubjects.sumOf {
                it.credits * it.gradePoint
            }

            val gpa = if (credits > 0) {
                weightedPoints.toDouble() / credits
            } else {
                0.0
            }

            Triple(semesterName, gpa, credits)
        }
        .sortedBy { it.first }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
    ) {

        Text(
            text = "Academic Performance",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554),
            modifier = Modifier.padding(
                start = 16.dp,
                top = 16.dp,
                bottom = 10.dp
            )
        )

        when {

            isLoading -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF3F51B5)
                    )
                }
            }

            errorMessage.isNotEmpty() -> {

                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }

            subjects.isEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "No GPA records found.\nAdd subjects in the GPA Calculator first.",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // Overall CGPA
                    item {

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF3F51B5)
                            )
                        ) {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                Text(
                                    text = "Overall CGPA",
                                    color = Color.White,
                                    fontSize = 17.sp
                                )

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%.2f",
                                        cgpa
                                    ),
                                    color = Color.White,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Based on $totalCredits total credits",
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Strongest subject
                    item {

                        PerformanceInfoCard(
                            title = "Strongest Subject",
                            subject = strongestSubject?.subject ?: "",
                            grade = strongestSubject?.grade ?: "",
                            gradePoint = strongestSubject?.gradePoint ?: 0,
                            cardColor = Color(0xFFE8F5E9)
                        )
                    }

                    // Weakest subject
                    item {

                        PerformanceInfoCard(
                            title = "Subject to Improve",
                            subject = weakestSubject?.subject ?: "",
                            grade = weakestSubject?.grade ?: "",
                            gradePoint = weakestSubject?.gradePoint ?: 0,
                            cardColor = Color(0xFFFFF3E0)
                        )
                    }

                    // Subject-wise performance
                    item {

                        Text(
                            text = "Subject-wise Performance",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF172554)
                        )
                    }

                    items(subjects) { subject ->

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {

                                    Text(
                                        text = subject.subject,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF172554),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = "${subject.gradePoint}/10",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3F51B5)
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                LinearProgressIndicator(
                                    progress = {
                                        (subject.gradePoint / 10f)
                                            .coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(9.dp),
                                    color = Color(0xFF3F51B5),
                                    trackColor = Color(0xFFE0E0E0)
                                )

                                Spacer(
                                    modifier = Modifier.height(6.dp)
                                )

                                Text(
                                    text = "${subject.semester} • ${subject.credits} credits • Grade ${subject.grade}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Semester-wise performance
                    item {

                        Text(
                            text = "Semester-wise Performance",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF172554)
                        )
                    }

                    items(semesterResults) { semester ->

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {

                                    Text(
                                        text = semester.first,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF172554)
                                    )

                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%.2f",
                                            semester.second
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00897B)
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                LinearProgressIndicator(
                                    progress = {
                                        (semester.second / 10.0)
                                            .toFloat()
                                            .coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(9.dp),
                                    color = Color(0xFF00897B),
                                    trackColor = Color(0xFFE0E0E0)
                                )

                                Spacer(
                                    modifier = Modifier.height(6.dp)
                                )

                                Text(
                                    text = "${semester.third} total credits",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PerformanceInfoCard(
    title: String,
    subject: String,
    grade: String,
    gradePoint: Int,
    cardColor: Color
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172554)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = subject,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Grade: $grade  |  Grade Points: $gradePoint",
                color = Color.DarkGray
            )
        }
    }
}
