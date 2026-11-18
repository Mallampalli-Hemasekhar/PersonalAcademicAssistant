
package com.example.personalacademicassistant

import androidx.compose.animation.animateContentSize
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Assignment(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val dueDate: String = "",
    val priority: String = "",
    val completed: Boolean = false
)

private val AssignmentBlue = Color(0xFF5278E8)
private val AssignmentGreen = Color(0xFF26A69A)
private val AssignmentOrange = Color(0xFFFFA726)
private val AssignmentRed = Color(0xFFE65B5B)

private fun isAssignmentOverdue(assignment: Assignment): Boolean {
    if (assignment.completed) return false

    val formats = listOf(
        "dd MMM yyyy",
        "d MMM yyyy",
        "dd MMMM yyyy",
        "d MMMM yyyy",
        "dd-MM-yyyy",
        "dd/MM/yyyy",
        "yyyy-MM-dd"
    )

    val dueDate = formats.firstNotNullOfOrNull { pattern ->
        try {
            SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                isLenient = false
            }.parse(assignment.dueDate)
        } catch (e: Exception) {
            null
        }
    } ?: return false

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return dueDate.before(today.time)
}

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
    val overdueCount = assignments.count { isAssignmentOverdue(it) }

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
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = "My Assignments",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "Stay organized. Meet every deadline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(15.dp),
                color = AssignmentBlue.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "📚",
                    fontSize = 27.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {

            AssignmentStatCard(
                title = "Total",
                count = assignments.size,
                color = AssignmentBlue,
                modifier = Modifier.weight(1f)
            )

            AssignmentStatCard(
                title = "Pending",
                count = pendingCount,
                color = AssignmentOrange,
                modifier = Modifier.weight(1f)
            )

            AssignmentStatCard(
                title = "Completed",
                count = completedCount,
                color = AssignmentGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (overdueCount > 0) {

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = AssignmentRed.copy(alpha = 0.10f)
            ) {

                Text(
                    text = "⚠ $overdueCount assignment(s) overdue. Check your deadlines!",
                    modifier = Modifier.padding(14.dp),
                    color = AssignmentRed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        Button(
            onClick = {
                errorMessage = ""
                showAddAssignment = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {

            Text(
                text = "+ Add New Assignment",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
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
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = "Your Tasks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

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

                    Text(
                        text = "📝",
                        fontSize = 52.sp
                    )

                    Spacer(modifier = Modifier.height(15.dp))

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
                    contentPadding = PaddingValues(bottom = 20.dp)
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
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 16.dp
            )
        ) {

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = count.toString(),
                fontSize = 25.sp,
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
        "high" -> AssignmentRed
        "medium" -> AssignmentOrange
        else -> AssignmentGreen
    }

    val overdue = isAssignmentOverdue(assignment)

    val statusColor = when {
        assignment.completed -> AssignmentGreen
        overdue -> AssignmentRed
        else -> AssignmentOrange
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(19.dp),
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
                verticalAlignment = Alignment.Top
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

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "📚 ${assignment.subject}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = priorityColor.copy(alpha = 0.13f)
                ) {

                    Text(
                        text = assignment.priority.ifBlank { "Normal" },
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        color = priorityColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(13.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "📅",
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Due: ${assignment.dueDate}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                if (overdue) {
                    Text(
                        text = "OVERDUE",
                        color = AssignmentRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(9.dp),
                color = statusColor.copy(alpha = 0.12f)
            ) {

                Text(
                    text = when {
                        assignment.completed -> "✓ Completed"
                        overdue -> "⚠ Overdue"
                        else -> "◷ Pending"
                    },
                    modifier = Modifier.padding(
                        horizontal = 11.dp,
                        vertical = 7.dp
                    ),
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

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
    var priority by remember { mutableStateOf("Medium") }
    var expanded by remember { mutableStateOf(false) }

    val priorities = listOf("Low", "Medium", "High")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),

        title = {
            Column {

                Text(
                    text = "New Assignment",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Add your academic task details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                    shape = RoundedCornerShape(13.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject") },
                    placeholder = { Text("Enter subject name") },
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Due Date") },
                    placeholder = { Text("Example: 20 Oct 2026") },
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box {

                    OutlinedTextField(
                        value = priority,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Priority") },
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(13.dp)
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(top = 8.dp)
                    ) {

                        TextButton(
                            onClick = {
                                expanded = true
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
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
                        dueDate.isNotBlank()
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
                        dueDate.isNotBlank()
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
