
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

    val goals = remember {
        mutableStateListOf<DailyGoal>()
    }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()

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

                            val goal = DailyGoal(
                                id = document.id,
                                title = document.getString("title") ?: "",
                                targetHours = document.getDouble("targetHours") ?: 0.0,
                                completedHours = document.getDouble("completedHours") ?: 0.0,
                                date = document.getString("date") ?: "",
                                completed = document.getBoolean("completed") ?: false
                            )

                            goals.add(goal)
                        }
                    }

                    isLoading = false
                }

            onDispose {
                listener.remove()
            }
        }
    }

    val totalGoals = goals.size
    val completedGoals = goals.count { it.completed }
    val pendingGoals = totalGoals - completedGoals

    val totalTargetHours = goals.sumOf { it.targetHours }
    val totalCompletedHours = goals.sumOf { it.completedHours }

    val progress = if (totalTargetHours > 0) {
        (totalCompletedHours / totalTargetHours * 100)
            .toFloat()
            .coerceIn(0f, 100f)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = "🎯 Daily Goals",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Set your daily study targets and track your progress.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Summary cards

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            GoalSummaryCard(
                title = "Total",
                value = totalGoals.toString(),
                modifier = Modifier.weight(1f),
                color = Color(0xFFE3F2FD)
            )

            GoalSummaryCard(
                title = "Completed",
                value = completedGoals.toString(),
                modifier = Modifier.weight(1f),
                color = Color(0xFFE8F5E9)
            )

            GoalSummaryCard(
                title = "Pending",
                value = pendingGoals.toString(),
                modifier = Modifier.weight(1f),
                color = Color(0xFFFFF3E0)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Overall progress

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Overall Study Progress",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = Color(0xFF3F51B5),
                    trackColor = Color.LightGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${"%.1f".format(progress)}% completed",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3F51B5)
                )

                Text(
                    text = "$totalCompletedHours hours completed out of $totalTargetHours target hours",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Create goal form

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Create Daily Goal",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172554)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title") },
                    placeholder = { Text("Example: Study Data Structures") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (DD-MM-YYYY)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetHours,
                    onValueChange = { targetHours = it },
                    label = { Text("Target Study Hours") },
                    placeholder = { Text("Example: 3") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = completedHours,
                    onValueChange = { completedHours = it },
                    label = { Text("Already Completed Hours") },
                    placeholder = { Text("Example: 1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

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

                            target == null ||
                                    completed == null ||
                                    target <= 0 ||
                                    completed < 0 ||
                                    completed > target -> {
                                errorMessage = "Enter valid hours. Completed hours cannot exceed target hours."
                                successMessage = ""
                            }

                            currentUser == null -> {
                                errorMessage = "Please login first."
                                successMessage = ""
                            }

                            else -> {

                                errorMessage = ""
                                successMessage = ""

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
                                    }
                                    .addOnFailureListener { exception ->
                                        errorMessage = exception.message
                                            ?: "Failed to save daily goal."
                                    }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3F51B5)
                    )
                ) {
                    Text(
                        text = "Add Daily Goal",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                }

                if (successMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = successMessage,
                        color = Color(0xFF2E7D32),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Saved goals

        Text(
            text = "My Daily Goals",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {

            CircularProgressIndicator()

        } else if (goals.isEmpty()) {

            Text(
                text = "No daily goals added yet.",
                color = Color.Gray
            )

        } else {

            goals.forEach { goal ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = goal.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3F51B5)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Date: ${goal.date}")

                        Text(
                            "Study time: ${goal.completedHours} / ${goal.targetHours} hours"
                        )

                        val goalProgress =
                            if (goal.targetHours > 0) {
                                (goal.completedHours / goal.targetHours)
                                    .toFloat()
                                    .coerceIn(0f, 1f)
                            } else {
                                0f
                            }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { goalProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Color(0xFF00897B),
                            trackColor = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (goal.completed) {
                                "Goal Completed"
                            } else {
                                "In Progress"
                            },
                            color = if (goal.completed) {
                                Color(0xFF2E7D32)
                            } else {
                                Color(0xFFEF6C00)
                            },
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Button(
                                onClick = {

                                    if (currentUser != null) {

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
                                                    "completed" to (newCompletedHours >= goal.targetHours)
                                                )
                                            )
                                            .addOnFailureListener { exception ->
                                                errorMessage = exception.message
                                                    ?: "Failed to update goal."
                                            }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00897B)
                                ),
                                enabled = !goal.completed
                            ) {
                                Text("+ 0.5 Hour")
                            }

                            Button(
                                onClick = {

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
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD32F2F)
                                )
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun GoalSummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color
) {

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
        shape = RoundedCornerShape(12.dp)
    ) {

        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.DarkGray
            )

            Text(
                text = value,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172554)
            )
        }
    }
}
