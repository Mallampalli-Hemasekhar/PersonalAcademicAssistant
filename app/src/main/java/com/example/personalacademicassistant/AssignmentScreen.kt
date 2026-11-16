
package com.example.personalacademicassistant

import androidx.compose.animation.animateContentSize
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

    var showAddAssignment by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val assignments = remember {
        mutableStateListOf<Assignment>()
    }

    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser

    val pendingCount = assignments.count { !it.completed }
    val completedCount = assignments.count { it.completed }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            errorMessage = "User is not logged in."
            isLoading = false
        } else {
            firestore.collection("users")
                .document(currentUser.uid)
                .collection("assignments")
                .get()
                .addOnSuccessListener { result ->

                    assignments.clear()

                    for (document in result.documents) {
                        assignments.add(
                            Assignment(
                                id = document.id,
                                title = document.getString("title") ?: "",
                                subject = document.getString("subject") ?: "",
                                dueDate = document.getString("dueDate") ?: "",
                                priority = document.getString("priority") ?: "",
                                completed = document.getBoolean("completed") ?: false
                            )
                        )
                    }

                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    errorMessage = exception.message
                        ?: "Failed to load assignments."
                    isLoading = false
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {

        Text(
            text = "My Assignments 📚",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Organize your tasks and stay ahead of deadlines.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssignmentStatCard(
                title = "Pending",
                count = pendingCount,
                color = Color(0xFFFFA726),
                modifier = Modifier.weight(1f)
            )

            AssignmentStatCard(
                title = "Completed",
                count = completedCount,
                color = Color(0xFF26A69A),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                showAddAssignment = true
                errorMessage = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "+ Add New Assignment",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (errorMessage.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading your assignments...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            assignments.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "📝", fontSize = 54.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "No assignments yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Add your first assignment to get started.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = assignments,
                        key = { it.id }
                    ) { assignment ->

                        AssignmentCard(
                            assignment = assignment,

                            onToggleComplete = {
                                val updatedStatus = !assignment.completed

                                currentUser?.let { user ->
                                    firestore.collection("users")
                                        .document(user.uid)
                                        .collection("assignments")
                                        .document(assignment.id)
                                        .update("completed", updatedStatus)
                                        .addOnSuccessListener {
                                            val index = assignments.indexOf(assignment)

                                            if (index != -1) {
                                                assignments[index] =
                                                    assignment.copy(
                                                        completed = updatedStatus
                                                    )
                                            }
                                        }
                                        .addOnFailureListener { exception ->
                                            errorMessage = exception.message
                                                ?: "Failed to update assignment."
                                        }
                                }
                            },

                            onDelete = {
                                currentUser?.let { user ->
                                    firestore.collection("users")
                                        .document(user.uid)
                                        .collection("assignments")
                                        .document(assignment.id)
                                        .delete()
                                        .addOnSuccessListener {
                                            assignments.remove(assignment)
                                        }
                                        .addOnFailureListener { exception ->
                                            errorMessage = exception.message
                                                ?: "Failed to delete assignment."
                                        }
                                }
                            }
                        )
                    }
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

                    firestore.collection("users")
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
                            errorMessage = exception.message
                                ?: "Failed to add assignment."
                        }
                }
            }
        )
    }
}

@Composable
fun AssignmentStatCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = count.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun AssignmentCard(
    assignment: Assignment,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (assignment.priority.lowercase()) {
        "high" -> Color(0xFFE53935)
        "medium" -> Color(0xFFFFA000)
        else -> Color(0xFF43A047)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                        text = assignment.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "📚 ${assignment.subject}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = priorityColor.copy(alpha = 0.13f)
                ) {
                    Text(
                        text = assignment.priority,
                        modifier = Modifier.padding(
                            horizontal = 11.dp,
                            vertical = 6.dp
                        ),
                        color = priorityColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "📅 Due date: ${assignment.dueDate}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (assignment.completed) {
                    Color(0xFF26A69A).copy(alpha = 0.13f)
                } else {
                    Color(0xFFFFA726).copy(alpha = 0.13f)
                }
            ) {
                Text(
                    text = if (assignment.completed) {
                        "✓ Completed"
                    } else {
                        "◷ Pending"
                    },
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 7.dp
                    ),
                    color = if (assignment.completed) {
                        Color(0xFF26A69A)
                    } else {
                        Color(0xFFE58A00)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onToggleComplete
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
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
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
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val priorities = listOf("Low", "Medium", "High")

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = "Add New Assignment",
                fontWeight = FontWeight.Bold
            )
        },

        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Assignment Title") },
                    placeholder = { Text("Enter assignment name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject") },
                    placeholder = { Text("Enter subject name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Due Date") },
                    placeholder = { Text("Example: 20 Oct 2026") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box {
                    OutlinedTextField(
                        value = priority,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Priority") },
                        placeholder = { Text("Select priority") },
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(top = 8.dp)
                    ) {
                        TextButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        priorities.forEach { selectedPriority ->
                            DropdownMenuItem(
                                text = { Text(selectedPriority) },
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
                                title = title.trim(),
                                subject = subject.trim(),
                                dueDate = dueDate.trim(),
                                priority = priority
                            )
                        )
                    }
                },
                enabled = title.isNotBlank() &&
                        subject.isNotBlank() &&
                        dueDate.isNotBlank() &&
                        priority.isNotBlank()
            ) {
                Text("Add Assignment")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
