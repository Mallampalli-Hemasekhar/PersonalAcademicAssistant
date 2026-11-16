
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

data class DailyGoal(
    val id: String = "",
    val title: String = "",
    val targetHours: Double = 0.0,
    val completedHours: Double = 0.0,
    val date: String = "",
    val completed: Boolean = false
)

@Composable
fun DailyGoalsScreen() {

    var title by remember { mutableStateOf("") }
    var targetHours by remember { mutableStateOf("") }
    var completedHours by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val goals = remember {
        mutableStateListOf<DailyGoal>()
    }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()
    val colors = MaterialTheme.colorScheme

    // Load goals from Firestore

    DisposableEffect(currentUser?.uid) {

        if (currentUser == null) {

            errorMessage = "Please login first."
            isLoading = false

            onDispose { }

        } else {

            val listener = firestore
                .collection("users")
                .document(currentUser.uid)
                .collection("dailyGoals")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        errorMessage = error.message
                            ?: "Failed to load daily goals."
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {

                        goals.clear()

                        for (document in snapshot.documents) {

                            goals.add(
                                DailyGoal(
                                    id = document.id,
                                    title = document.getString("title") ?: "",
                                    targetHours = document.getDouble("targetHours") ?: 0.0,
                                    completedHours = document.getDouble("completedHours") ?: 0.0,
                                    date = document.getString("date") ?: "",
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

    // Statistics

    val totalGoals = goals.size
    val completedGoals = goals.count { it.completed }
    val pendingGoals = totalGoals - completedGoals

    val totalTargetHours = goals.sumOf { it.targetHours }
    val totalCompletedHours = goals.sumOf { it.completedHours }

    val progress = if (totalTargetHours > 0) {
        (totalCompletedHours / totalTargetHours)
            .toFloat()
            .coerceIn(0f, 1f)
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

        // Header

        Text(
            text = "🎯 Daily Goals",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Set your daily study targets and track your progress.",
            fontSize = 14.sp,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Summary cards

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {

            GoalSummaryCard(
                title = "Total",
                value = totalGoals.toString(),
                modifier = Modifier.weight(1f),
                color = colors.surface
            )

            GoalSummaryCard(
                title = "Completed",
                value = completedGoals.toString(),
                modifier = Modifier.weight(1f),
                color = colors.surface
            )

            GoalSummaryCard(
                title = "Pending",
                value = pendingGoals.toString(),
                modifier = Modifier.weight(1f),
                color = colors.surface
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Overall progress

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = colors.primaryContainer
            ),
            shape = RoundedCornerShape(18.dp)
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
                        text = "Overall Study Progress",
                        fontSize = 17.sp,
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

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp),
                    color = colors.primary,
                    trackColor = colors.surface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${"%.1f".format(totalCompletedHours)} hours completed out of ${"%.1f".format(totalTargetHours)} target hours",
                    fontSize = 12.sp,
                    color = colors.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Create goal form

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            ),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "➕ Create Daily Goal",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Define what you want to achieve today.",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = ""
                    },
                    label = { Text("Goal Title") },
                    placeholder = {
                        Text("Example: Study Data Structures")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = {
                        date = it
                        errorMessage = ""
                    },
                    label = { Text("Date (DD-MM-YYYY)") },
                    placeholder = { Text("17-11-2026") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetHours,
                    onValueChange = {
                        targetHours = it
                        errorMessage = ""
                    },
                    label = { Text("Target Study Hours") },
                    placeholder = { Text("Example: 3") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = completedHours,
                    onValueChange = {
                        completedHours = it
                        errorMessage = ""
                    },
                    label = { Text("Already Completed Hours") },
                    placeholder = { Text("Example: 1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {

                        val target = targetHours.toDoubleOrNull()
                        val completed = completedHours.toDoubleOrNull()

                        when {

                            title.isBlank() ||
                                    date.isBlank() ||
                                    targetHours.isBlank() ||
                                    completedHours.isBlank() -> {

                                errorMessage = "Please fill in all fields."
                                successMessage = ""
                            }

                            !date.matches(
                                Regex("\\d{2}-\\d{2}-\\d{4}")
                            ) -> {

                                errorMessage = "Enter date in DD-MM-YYYY format."
                                successMessage = ""
                            }

                            target == null ||
                                    completed == null ||
                                    !target.isFinite() ||
                                    !completed.isFinite() ||
                                    target <= 0 ||
                                    completed < 0 ||
                                    completed > target -> {

                                errorMessage =
                                    "Enter valid hours. Completed hours cannot exceed target hours."
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

                                val goal = hashMapOf(
                                    "title" to title.trim(),
                                    "date" to date.trim(),
                                    "targetHours" to target,
                                    "completedHours" to completed,
                                    "completed" to (completed >= target)
                                )

                                firestore
                                    .collection("users")
                                    .document(currentUser.uid)
                                    .collection("dailyGoals")
                                    .add(goal)
                                    .addOnSuccessListener {

                                        title = ""
                                        date = ""
                                        targetHours = ""
                                        completedHours = ""

                                        successMessage =
                                            "Daily goal added successfully!"

                                        isSaving = false
                                    }
                                    .addOnFailureListener { exception ->

                                        errorMessage = exception.message
                                            ?: "Failed to save daily goal."

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
                            text = "Add Daily Goal",
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

        // Saved goals

        Text(
            text = "My Daily Goals",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Keep track of your daily achievements.",
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

        } else if (goals.isEmpty()) {

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
                        text = "🎯",
                        fontSize = 38.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "No Daily Goals Yet",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Create your first goal above and start tracking your progress.",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }

        } else {

            val sortedGoals = goals.sortedWith(
                compareBy<DailyGoal> { it.completed }
                    .thenByDescending { it.date }
            )

            sortedGoals.forEach { goal ->

                DailyGoalCard(
                    goal = goal,
                    onAddHours = {

                        if (currentUser != null && !goal.completed) {

                            val newCompletedHours =
                                (goal.completedHours + 0.5)
                                    .coerceAtMost(goal.targetHours)

                            firestore
                                .collection("users")
                                .document(currentUser.uid)
                                .collection("dailyGoals")
                                .document(goal.id)
                                .update(
                                    mapOf(
                                        "completedHours" to newCompletedHours,
                                        "completed" to (
                                                newCompletedHours >= goal.targetHours
                                                )
                                    )
                                )
                                .addOnFailureListener { exception ->

                                    errorMessage = exception.message
                                        ?: "Failed to update goal."
                                }
                        }
                    },
                    onDelete = {

                        if (currentUser != null) {

                            firestore
                                .collection("users")
                                .document(currentUser.uid)
                                .collection("dailyGoals")
                                .document(goal.id)
                                .delete()
                                .addOnFailureListener { exception ->

                                    errorMessage = exception.message
                                        ?: "Failed to delete goal."
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

// Summary card

@Composable
fun GoalSummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color
) {

    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
        shape = RoundedCornerShape(15.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = value,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
    }
}

// Individual daily goal card

@Composable
private fun DailyGoalCard(
    goal: DailyGoal,
    onAddHours: () -> Unit,
    onDelete: () -> Unit
) {

    val colors = MaterialTheme.colorScheme

    val goalProgress = if (goal.targetHours > 0) {
        (goal.completedHours / goal.targetHours)
            .toFloat()
            .coerceIn(0f, 1f)
    } else {
        0f
    }

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
                        text = goal.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "📅 ${goal.date}",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (goal.completed) {
                        colors.primaryContainer
                    } else {
                        colors.tertiaryContainer
                    }
                ) {

                    Text(
                        text = if (goal.completed) {
                            "Completed"
                        } else {
                            "In Progress"
                        },
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (goal.completed) {
                            colors.onPrimaryContainer
                        } else {
                            colors.onTertiaryContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Study Progress",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )

                Text(
                    text = "${"%.1f".format(goal.completedHours)} / ${"%.1f".format(goal.targetHours)} hrs",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            LinearProgressIndicator(
                progress = { goalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp),
                color = colors.primary,
                trackColor = colors.surfaceVariant
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = "${(goalProgress * 100).toInt()}% completed",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.primary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Button(
                    onClick = onAddHours,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    enabled = !goal.completed,
                    shape = RoundedCornerShape(10.dp)
                ) {

                    Text(
                        text = "+ 0.5 Hour",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.error
                    )
                ) {

                    Text(
                        text = "Delete",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
