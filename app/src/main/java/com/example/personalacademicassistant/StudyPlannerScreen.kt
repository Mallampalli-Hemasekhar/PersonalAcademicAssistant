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
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    val studyPlans = remember {
        mutableStateListOf<StudyPlan>()
    }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()

    // Load study plans from Firestore
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
                        errorMessage = error.message ?: "Failed to load study plans."
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {

                        studyPlans.clear()

                        for (document in snapshot.documents) {

                            val plan = StudyPlan(
                                id = document.id,
                                subject = document.getString("subject") ?: "",
                                topic = document.getString("topic") ?: "",
                                studyDate = document.getString("studyDate") ?: "",
                                startTime = document.getString("startTime") ?: "",
                                endTime = document.getString("endTime") ?: "",
                                completed = document.getBoolean("completed") ?: false
                            )

                            studyPlans.add(plan)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = "📚 Smart Study Planner",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Organize your study sessions and achieve your goals.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Summary cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Plans")
                    Text(
                        studyPlans.size.toString(),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Completed")
                    Text(
                        completedCount.toString(),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pending")
                    Text(
                        pendingCount.toString(),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Add study plan
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Create Study Plan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172554)
                )

                Spacer(modifier = Modifier.height(15.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic to Study") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = studyDate,
                    onValueChange = { studyDate = it },
                    label = { Text("Study Date (DD-MM-YYYY)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start Time (e.g. 09:00 AM)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("End Time (e.g. 10:00 AM)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {

                        if (
                            subject.isBlank() ||
                            topic.isBlank() ||
                            studyDate.isBlank() ||
                            startTime.isBlank() ||
                            endTime.isBlank()
                        ) {
                            errorMessage = "Please fill in all fields."
                            successMessage = ""
                        } else if (currentUser == null) {
                            errorMessage = "Please login first."
                            successMessage = ""
                        } else {

                            errorMessage = ""
                            successMessage = ""

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

                                    successMessage = "Study plan added successfully!"
                                }
                                .addOnFailureListener { exception ->
                                    errorMessage = exception.message
                                        ?: "Failed to save study plan."
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
                        "Add Study Plan",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        errorMessage,
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                }

                if (successMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        successMessage,
                        color = Color(0xFF2E7D32),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "My Study Sessions",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {

            CircularProgressIndicator()

        } else if (studyPlans.isEmpty()) {

            Text(
                text = "No study plans added yet.",
                color = Color.Gray
            )

        } else {

            studyPlans.forEach { plan ->

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
                            text = plan.subject,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3F51B5)
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text("Topic: ${plan.topic}")

                        Text("Date: ${plan.studyDate}")

                        Text(
                            "Time: ${plan.startTime} - ${plan.endTime}"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Checkbox(
                                checked = plan.completed,
                                onCheckedChange = { checked ->

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
                                }
                            )

                            Text(
                                text = if (plan.completed) {
                                    "Completed"
                                } else {
                                    "Mark as Completed"
                                },
                                color = if (plan.completed) {
                                    Color(0xFF2E7D32)
                                } else {
                                    Color.DarkGray
                                }
                            )
                        }

                        Button(
                            onClick = {

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
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}
