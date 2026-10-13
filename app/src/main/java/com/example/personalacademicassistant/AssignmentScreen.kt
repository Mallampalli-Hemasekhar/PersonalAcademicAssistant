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

data class Assignment(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val dueDate: String = "",
    val priority: String = "",
    val completed: Boolean = false
)

@Composable
fun AssignmentScreen() {

    var showAddAssignment by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val assignments = remember {
        mutableStateListOf<Assignment>()
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
                .collection("assignments")
                .get()
                .addOnSuccessListener { result ->

                    assignments.clear()

                    for (document in result.documents) {

                        val assignment = Assignment(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            subject = document.getString("subject") ?: "",
                            dueDate = document.getString("dueDate") ?: "",
                            priority = document.getString("priority") ?: "",
                            completed = document.getBoolean("completed") ?: false
                        )

                        assignments.add(assignment)
                    }

                    isLoading = false
                }
                .addOnFailureListener { exception ->

                    errorMessage =
                        exception.message ?: "Failed to load assignments."

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
            text = "📋 Assignments",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Manage your assignments and deadlines",
            fontSize = 15.sp,
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {
                showAddAssignment = true
                errorMessage = ""
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3F51B5)
            )
        ) {

            Text(
                text = "+ Add Assignment",
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
                    text = "Loading assignments...",
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

        } else if (assignments.isEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "📋",
                    fontSize = 50.sp
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "No assignments yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172554)
                )

                Text(
                    text = "Add your first assignment to get started.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

        } else {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = assignments,
                    key = { it.id }
                ) { assignment ->

                    AssignmentCard(
                        assignment = assignment,

                        onToggleComplete = {

                            val updatedStatus =
                                !assignment.completed

                            currentUser?.let { user ->

                                firestore
                                    .collection("users")
                                    .document(user.uid)
                                    .collection("assignments")
                                    .document(assignment.id)
                                    .update(
                                        "completed",
                                        updatedStatus
                                    )
                                    .addOnSuccessListener {

                                        val index =
                                            assignments.indexOf(assignment)

                                        if (index != -1) {

                                            assignments[index] =
                                                assignment.copy(
                                                    completed = updatedStatus
                                                )
                                        }
                                    }
                                    .addOnFailureListener { exception ->

                                        errorMessage =
                                            exception.message
                                                ?: "Failed to update assignment."
                                    }
                            }
                        },

                        onDelete = {

                            currentUser?.let { user ->

                                firestore
                                    .collection("users")
                                    .document(user.uid)
                                    .collection("assignments")
                                    .document(assignment.id)
                                    .delete()
                                    .addOnSuccessListener {

                                        assignments.remove(assignment)
                                    }
                                    .addOnFailureListener { exception ->

                                        errorMessage =
                                            exception.message
                                                ?: "Failed to delete assignment."
                                    }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddAssignment) {

        AddAssignmentDialog(

            onDismiss = {
                showAddAssignment = false
            },

            onAddAssignment = { assignment ->

                if (currentUser == null) {

                    errorMessage = "User is not logged in."
                    showAddAssignment = false

                } else {

                    val assignmentData = hashMapOf(

                        "title" to assignment.title,

                        "subject" to assignment.subject,

                        "dueDate" to assignment.dueDate,

                        "priority" to assignment.priority,

                        "completed" to false
                    )

                    firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .collection("assignments")
                        .add(assignmentData)
                        .addOnSuccessListener { documentReference ->

                            assignments.add(
                                assignment.copy(
                                    id = documentReference.id
                                )
                            )

                            showAddAssignment = false
                            errorMessage = ""
                        }
                        .addOnFailureListener { exception ->

                            errorMessage =
                                exception.message
                                    ?: "Failed to add assignment."
                        }
                }
            }
        )
    }
}

@Composable
fun AssignmentCard(
    assignment: Assignment,
    onToggleComplete: () -> Unit,
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
                text = assignment.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (assignment.completed) {
                    Color.Gray
                } else {
                    Color(0xFF172554)
                }
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "📚 Subject: ${assignment.subject}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = "📅 Due Date: ${assignment.dueDate}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = "⚡ Priority: ${assignment.priority}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = if (assignment.completed) {
                    "✅ Completed"
                } else {
                    "⏳ Pending"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (assignment.completed) {
                    Color(0xFF2E7D32)
                } else {
                    Color(0xFFE65100)
                }
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
                        onToggleComplete()
                    }
                ) {

                    Text(
                        text = if (assignment.completed) {
                            "Mark Pending"
                        } else {
                            "Complete"
                        }
                    )
                }

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
fun AddAssignmentDialog(
    onDismiss: () -> Unit,
    onAddAssignment: (Assignment) -> Unit
) {

    var title by remember {
        mutableStateOf("")
    }

    var subject by remember {
        mutableStateOf("")
    }

    var dueDate by remember {
        mutableStateOf("")
    }

    var priority by remember {
        mutableStateOf("")
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    val priorities = listOf(
        "Low",
        "Medium",
        "High"
    )

    AlertDialog(

        onDismissRequest = {
            onDismiss()
        },

        title = {
            Text(
                text = "Add Assignment",
                fontWeight = FontWeight.Bold
            )
        },

        text = {

            Column {

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Assignment Title")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Subject")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = {
                        dueDate = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Due Date")
                    },
                    placeholder = {
                        Text("Example: 20 Oct 2026")
                    },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Column {

                    OutlinedTextField(
                        value = priority,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Priority")
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
                            text = if (priority.isEmpty()) {
                                "Select Priority"
                            } else {
                                priority
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {

                        priorities.forEach { selectedPriority ->

                            DropdownMenuItem(

                                text = {
                                    Text(selectedPriority)
                                },

                                onClick = {

                                    priority = selectedPriority
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    if (
                        title.isNotBlank() &&
                        subject.isNotBlank() &&
                        dueDate.isNotBlank() &&
                        priority.isNotBlank()
                    ) {

                        onAddAssignment(

                            Assignment(
                                title = title,
                                subject = subject,
                                dueDate = dueDate,
                                priority = priority
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