
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

data class StudyGroup(
    val id: String = "",
    val name: String = "",
    val subject: String = "",
    val description: String = "",
    val groupCode: String = "",
    val createdBy: String = ""
)

@Composable
fun StudyBuddyScreen() {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    val groups = remember { mutableStateListOf<StudyGroup>() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    var groupName by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(userId) {

        if (userId == null) {
            isLoading = false
            message = "Please log in to access study groups."
            onDispose { }
        } else {

            isLoading = true

            val listener = db.collection("users")
                .document(userId)
                .collection("studyBuddyMemberships")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        message = error.localizedMessage
                            ?: "Unable to load your study groups."
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    val groupIds = snapshot.documents.map { it.id }

                    if (groupIds.isEmpty()) {
                        groups.clear()
                        isLoading = false
                        return@addSnapshotListener
                    }

                    val batches = groupIds.chunked(10)
                    val loadedGroups = mutableListOf<StudyGroup>()
                    var completedBatches = 0

                    batches.forEach { batch ->

                        db.collection("studyGroups")
                            .whereIn(FieldPath.documentId(), batch)
                            .get()
                            .addOnSuccessListener { result ->

                                loadedGroups.addAll(
                                    result.documents.mapNotNull { document ->
                                        document.toObject(StudyGroup::class.java)
                                            ?.copy(id = document.id)
                                    }
                                )

                                completedBatches++

                                if (completedBatches == batches.size) {
                                    groups.clear()
                                    groups.addAll(
                                        loadedGroups.sortedBy {
                                            it.name.lowercase()
                                        }
                                    )
                                    isLoading = false
                                }
                            }
                            .addOnFailureListener { exception ->

                                completedBatches++

                                message = exception.localizedMessage
                                    ?: "Unable to load some study groups."

                                if (completedBatches == batches.size) {
                                    groups.clear()
                                    groups.addAll(
                                        loadedGroups.sortedBy {
                                            it.name.lowercase()
                                        }
                                    )
                                    isLoading = false
                                }
                            }
                    }
                }

            onDispose {
                listener.remove()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        Text(
            text = "Study Buddy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Learn together, share knowledge and stay motivated.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "Your Learning Community",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Create a group for your subject or join an existing group with an invitation code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "${groups.size} Study Groups",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    groupName = ""
                    subject = ""
                    description = ""
                    message = ""
                    showCreateDialog = true
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("＋ Create Group")
            }

            OutlinedButton(
                onClick = {
                    joinCode = ""
                    message = ""
                    showJoinDialog = true
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Join Group")
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "My Study Groups",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${groups.size} groups",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

        } else if (groups.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📚",
                            style = MaterialTheme.typography.displaySmall
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "No study groups yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Create your first group or join one using a group code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

        } else {

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = groups,
                    key = { it.id }
                ) { group ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = group.subject,
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 5.dp
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            if (group.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = group.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "GROUP INVITATION CODE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = group.groupCode,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    // Create Study Group Dialog

    if (showCreateDialog) {

        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
            },
            title = {
                Text(
                    text = "Create Study Group",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Set up a group and invite your classmates using its unique code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )

                    if (message.isNotBlank()) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {

                        if (userId == null) {
                            message = "Please log in first."
                        } else if (
                            groupName.isBlank() ||
                            subject.isBlank()
                        ) {
                            message = "Enter group name and subject."
                        } else {

                            val groupId = UUID.randomUUID().toString()
                            val code = UUID.randomUUID()
                                .toString()
                                .take(6)
                                .uppercase()

                            val groupData = hashMapOf(
                                "id" to groupId,
                                "name" to groupName.trim(),
                                "subject" to subject.trim(),
                                "description" to description.trim(),
                                "groupCode" to code,
                                "createdBy" to userId
                            )

                            db.collection("studyGroups")
                                .document(groupId)
                                .set(groupData)
                                .addOnSuccessListener {

                                    db.collection("users")
                                        .document(userId)
                                        .collection("studyBuddyMemberships")
                                        .document(groupId)
                                        .set(
                                            mapOf(
                                                "joinedAt" to FieldValue.serverTimestamp()
                                            )
                                        )
                                        .addOnSuccessListener {

                                            groupName = ""
                                            subject = ""
                                            description = ""
                                            message = ""
                                            showCreateDialog = false
                                        }
                                        .addOnFailureListener { exception ->
                                            message = exception.localizedMessage
                                                ?: "Membership could not be saved."
                                        }
                                }
                                .addOnFailureListener { exception ->
                                    message = exception.localizedMessage
                                        ?: "Could not create group."
                                }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Create Group")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        message = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Join Study Group Dialog

    if (showJoinDialog) {

        AlertDialog(
            onDismissRequest = {
                showJoinDialog = false
            },
            title = {
                Text(
                    text = "Join Study Group",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enter the six-character invitation code shared by your study group.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = {
                            joinCode = it.uppercase().take(6)
                        },
                        label = { Text("Group Invitation Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (message.isNotBlank()) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {

                        if (userId == null) {
                            message = "Please log in first."
                        } else if (joinCode.isBlank()) {
                            message = "Enter a group code."
                        } else {

                            db.collection("studyGroups")
                                .whereEqualTo("groupCode", joinCode.trim())
                                .get()
                                .addOnSuccessListener { result ->

                                    val document = result.documents.firstOrNull()

                                    if (document == null) {

                                        message = "Invalid group code."

                                    } else {

                                        db.collection("users")
                                            .document(userId)
                                            .collection("studyBuddyMemberships")
                                            .document(document.id)
                                            .set(
                                                mapOf(
                                                    "joinedAt" to FieldValue.serverTimestamp()
                                                )
                                            )
                                            .addOnSuccessListener {

                                                joinCode = ""
                                                message = ""
                                                showJoinDialog = false
                                            }
                                            .addOnFailureListener { exception ->
                                                message = exception.localizedMessage
                                                    ?: "Could not join group."
                                            }
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    message = exception.localizedMessage
                                        ?: "Could not find group."
                                }
                        }
                    }
                ) {
                    Text("Join Group")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showJoinDialog = false
                        message = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
