
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class StudyPlan(
    val id: String = "",
    val subject: String = "",
    val topic: String = "",
    val studyDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val completed: Boolean = false
)

@Composable
fun StudyPlannerScreen() {

    var subject by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var studyDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    val studyPlans = remember {
        mutableStateListOf<StudyPlan>()
    }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()
    val colors = MaterialTheme.colorScheme

    DisposableEffect(currentUser?.uid) {

        if (currentUser == null) {
            errorMessage = "Please login to use the Study Planner."
            isLoading = false

            onDispose { }
        } else {

            val listener = firestore
                .collection("users")
                .document(currentUser.uid)
                .collection("studyPlans")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        errorMessage = error.message
                            ?: "Failed to load study plans."
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {

                        studyPlans.clear()

                        for (document in snapshot.documents) {

                            studyPlans.add(
                                StudyPlan(
                                    id = document.id,
                                    subject = document.getString("subject") ?: "",
                                    topic = document.getString("topic") ?: "",
                                    studyDate = document.getString("studyDate") ?: "",
                                    startTime = document.getString("startTime") ?: "",
                                    endTime = document.getString("endTime") ?: "",
                                    completed = document.getBoolean("completed") ?: false
                                )
                            )
                        }
                    }

                    isLoading = false
                }

            onDispose {
                listener.remove()
            }
        }
    }

    val completedCount = studyPlans.count { it.completed }
    val pendingCount = studyPlans.count { !it.completed }

    val progress = if (studyPlans.isNotEmpty()) {
        completedCount.toFloat() / studyPlans.size.toFloat()
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = "📚 Smart Study Planner",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Plan your study sessions and stay consistent with your learning goals.",
            fontSize = 14.sp,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Study plan statistics

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            StudyPlannerStatCard(
                title = "Total Plans",
                value = studyPlans.size.toString(),
                modifier = Modifier.weight(1f),
                icon = "📚"
            )

            StudyPlannerStatCard(
                title = "Completed",
                value = completedCount.toString(),
                modifier = Modifier.weight(1f),
                icon = "✅"
            )

            StudyPlannerStatCard(
                title = "Pending",
                value = pendingCount.toString(),
                modifier = Modifier.weight(1f),
                icon = "⏳"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Overall progress

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.primaryContainer
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Study Progress",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer
                    )

                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp),
                    color = colors.primary,
                    trackColor = colors.surface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (studyPlans.isEmpty()) {
                        "Add your first study plan to track your progress."
                    } else {
                        "$completedCount out of ${studyPlans.size} study sessions completed."
                    },
                    fontSize = 13.sp,
                    color = colors.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Create study plan

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
                    text = "➕ Create Study Plan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Add a subject, topic, date and study time.",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        errorMessage = ""
                    },
                    label = { Text("Subject Name") },
                    placeholder = { Text("e.g. Data Structures") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = topic,
                    onValueChange = {
                        topic = it
                        errorMessage = ""
                    },
                    label = { Text("Topic to Study") },
                    placeholder = { Text("e.g. Linked Lists") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = studyDate,
                    onValueChange = {
                        studyDate = it
                        errorMessage = ""
                    },
                    label = { Text("Study Date (DD-MM-YYYY)") },
                    placeholder = { Text("17-11-2026") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = startTime,
                    onValueChange = {
                        startTime = it
                        errorMessage = ""
                    },
                    label = { Text("Start Time") },
                    placeholder = { Text("09:00 AM") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = endTime,
                    onValueChange = {
                        endTime = it
                        errorMessage = ""
                    },
                    label = { Text("End Time") },
                    placeholder = { Text("10:00 AM") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {

                        when {
                            subject.isBlank() ||
                                    topic.isBlank() ||
                                    studyDate.isBlank() ||
                                    startTime.isBlank() ||
                                    endTime.isBlank() -> {

                                errorMessage = "Please fill in all fields."
                                successMessage = ""
                            }

                            currentUser == null -> {
                                errorMessage = "Please login first."
                                successMessage = ""
                            }

                            isSaving -> Unit

                            else -> {

                                errorMessage = ""
                                successMessage = ""
                                isSaving = true

                                val plan = hashMapOf(
                                    "subject" to subject.trim(),
                                    "topic" to topic.trim(),
                                    "studyDate" to studyDate.trim(),
                                    "startTime" to startTime.trim(),
                                    "endTime" to endTime.trim(),
                                    "completed" to false
                                )

                                firestore
                                    .collection("users")
                                    .document(currentUser.uid)
                                    .collection("studyPlans")
                                    .add(plan)
                                    .addOnSuccessListener {

                                        subject = ""
                                        topic = ""
                                        studyDate = ""
                                        startTime = ""
                                        endTime = ""

                                        successMessage =
                                            "Study plan added successfully!"
                                        isSaving = false
                                    }
                                    .addOnFailureListener { exception ->

                                        errorMessage = exception.message
                                            ?: "Failed to save study plan."

                                        isSaving = false
                                    }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                ) {

                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(21.dp),
                            color = colors.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Add Study Plan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                if (errorMessage.isNotEmpty()) {

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = errorMessage,
                        color = colors.error,
                        fontSize = 13.sp
                    )
                }

                if (successMessage.isNotEmpty()) {

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = successMessage,
                        color = colors.primary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Study sessions heading

        Text(
            text = "My Study Sessions",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Manage your planned learning sessions.",
            fontSize = 13.sp,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

        } else if (studyPlans.isEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "📖",
                        fontSize = 38.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "No Study Plans Yet",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Create your first study session above.",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }

        } else {

            val sortedPlans = studyPlans.sortedWith(
                compareBy<StudyPlan> { it.completed }
                    .thenBy { it.studyDate }
                    .thenBy { it.startTime }
            )

            sortedPlans.forEach { plan ->

                StudyPlanCard(
                    plan = plan,
                    onCompletionChange = { checked ->

                        if (currentUser != null) {

                            firestore
                                .collection("users")
                                .document(currentUser.uid)
                                .collection("studyPlans")
                                .document(plan.id)
                                .update("completed", checked)
                                .addOnFailureListener { exception ->

                                    errorMessage = exception.message
                                        ?: "Failed to update study plan."
                                }
                        }
                    },
                    onDelete = {

                        if (currentUser != null) {

                            firestore
                                .collection("users")
                                .document(currentUser.uid)
                                .collection("studyPlans")
                                .document(plan.id)
                                .delete()
                                .addOnFailureListener { exception ->

                                    errorMessage = exception.message
                                        ?: "Failed to delete study plan."
                                }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun StudyPlannerStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: String
) {

    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = icon,
                fontSize = 21.sp
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = value,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StudyPlanCard(
    plan: StudyPlan,
    onCompletionChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {

    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
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
                        text = plan.subject,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = plan.topic,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (plan.completed) {
                        colors.primaryContainer
                    } else {
                        colors.tertiaryContainer
                    }
                ) {

                    Text(
                        text = if (plan.completed) "Completed" else "Pending",
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (plan.completed) {
                            colors.onPrimaryContainer
                        } else {
                            colors.onTertiaryContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = colors.outlineVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "📅  ${plan.studyDate}",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = "🕒  ${plan.startTime} - ${plan.endTime}",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = plan.completed,
                    onCheckedChange = onCompletionChange
                )

                Text(
                    text = if (plan.completed) {
                        "Study session completed"
                    } else {
                        "Mark as completed"
                    },
                    fontSize = 13.sp,
                    color = colors.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.error
                )
            ) {

                Text(
                    text = "Delete Study Plan",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
