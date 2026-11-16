
package com.example.personalacademicassistant

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
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(currentUser?.uid) {

        if (currentUser == null) {
            errorMessage = "Please login to view your performance."
            isLoading = false
        } else {

            isLoading = true
            errorMessage = ""

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
            .padding(horizontal = 16.dp)
    ) {

        Text(
            text = "📈 Academic Performance",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
            modifier = Modifier.padding(
                top = 18.dp,
                bottom = 6.dp
            )
        )

        Text(
            text = "Understand your academic progress",
            fontSize = 14.sp,
            color = colors.onSurfaceVariant
        )

        when {

            isLoading -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Loading performance data...",
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            errorMessage.isNotEmpty() -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage,
                        color = colors.error
                    )
                }
            }

            subjects.isEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "📊",
                            fontSize = 48.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "No GPA records found",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Add subjects in the GPA Calculator first.",
                            fontSize = 14.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 18.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    item {

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colors.primary
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
                                    color = colors.onPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%.2f",
                                        cgpa
                                    ),
                                    color = colors.onPrimary,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Out of 10.00",
                                    color = colors.onPrimary.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                LinearProgressIndicator(
                                    progress = {
                                        (cgpa / 10.0)
                                            .toFloat()
                                            .coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(9.dp),
                                    color = colors.onPrimary,
                                    trackColor = colors.onPrimary.copy(
                                        alpha = 0.25f
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Calculated from $totalCredits total credits",
                                    color = colors.onPrimary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    item {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            PerformanceSummaryCard(
                                title = "Total Subjects",
                                value = subjects.size.toString(),
                                modifier = Modifier.weight(1f)
                            )

                            PerformanceSummaryCard(
                                title = "Total Credits",
                                value = totalCredits.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Performance Highlights",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                    }

                    item {
                        PerformanceInfoCard(
                            title = "🏆 Strongest Subject",
                            subject = strongestSubject?.subject ?: "",
                            grade = strongestSubject?.grade ?: "",
                            gradePoint = strongestSubject?.gradePoint ?: 0,
                            positive = true
                        )
                    }

                    item {
                        PerformanceInfoCard(
                            title = "📚 Subject to Improve",
                            subject = weakestSubject?.subject ?: "",
                            grade = weakestSubject?.grade ?: "",
                            gradePoint = weakestSubject?.gradePoint ?: 0,
                            positive = false
                        )
                    }

                    item {
                        Text(
                            text = "Subject-wise Performance",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                    }

                    items(
                        items = subjects,
                        key = { it.id }
                    ) { subject ->

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
                                modifier = Modifier.padding(17.dp)
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Text(
                                        text = subject.subject,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = colors.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = "${subject.gradePoint}/10",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = colors.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                LinearProgressIndicator(
                                    progress = {
                                        (subject.gradePoint / 10f)
                                            .coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(9.dp),
                                    color = colors.primary,
                                    trackColor = colors.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "${subject.semester} • ${subject.credits} credits • Grade ${subject.grade}",
                                    fontSize = 12.sp,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Semester-wise Performance",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                    }

                    items(semesterResults) { semester ->

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
                                modifier = Modifier.padding(17.dp)
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Text(
                                        text = semester.first,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurface
                                    )

                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%.2f",
                                            semester.second
                                        ),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.secondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                LinearProgressIndicator(
                                    progress = {
                                        (semester.second / 10.0)
                                            .toFloat()
                                            .coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(9.dp),
                                    color = colors.secondary,
                                    trackColor = colors.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "${semester.third} total credits",
                                    fontSize = 12.sp,
                                    color = colors.onSurfaceVariant
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
fun PerformanceSummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
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
            modifier = Modifier.padding(17.dp)
        ) {

            Text(
                text = title,
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }
    }
}

@Composable
fun PerformanceInfoCard(
    title: String,
    subject: String,
    grade: String,
    gradePoint: Int,
    positive: Boolean
) {

    val colors = MaterialTheme.colorScheme

    val containerColor = if (positive) {
        Color(0xFF1B8E61).copy(alpha = 0.12f)
    } else {
        colors.errorContainer
    }

    val contentColor = if (positive) {
        Color(0xFF219653)
    } else {
        colors.onErrorContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {

        Column(
            modifier = Modifier.padding(17.dp)
        ) {

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = subject,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Grade: $grade  |  Grade Points: $gradePoint",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )
        }
    }
}
