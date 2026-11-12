
package com.example.personalacademicassistant

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
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

    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("users")
                .document(userId)
                .collection("studyBuddyMemberships")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    val groupIds = snapshot.documents.map { it.id }

                    if (groupIds.isEmpty()) {
                        groups.clear()
                        return@addSnapshotListener
                    }

                    db.collection("studyGroups")
                        .whereIn(
                            com.google.firebase.firestore.FieldPath.documentId(),
                            groupIds.take(10)
                        )
                        .get()
                        .addOnSuccessListener { result ->
                            groups.clear()
                            groups.addAll(
                                result.documents.mapNotNull { document ->
                                    document.toObject(StudyGroup::class.java)
                                        ?.copy(id = document.id)
                                }
                            )
                        }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Study Buddy & Collaboration",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Create study groups and learn together.")

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    showCreateDialog = true
                    message = ""
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Create Group")
            }

            Button(
                onClick = {
                    showJoinDialog = true
                    message = ""
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Join Group")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "My Study Groups",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (groups.isEmpty()) {
            Text("No study groups yet. Create or join a group!")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(groups, key = { it.id }) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                group.name,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text("Subject: ${group.subject}")
                            Text("Description: ${group.description}")
                            Text("Group Code: ${group.groupCode}")
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Study Group") },
            text = {
                Column {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") }
                    )

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") }
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") }
                    )

                    if (message.isNotEmpty()) {
                        Text(message)
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

                            val group = hashMapOf(
                                "id" to groupId,
                                "name" to groupName.trim(),
                                "subject" to subject.trim(),
                                "description" to description.trim(),
                                "groupCode" to code,
                                "createdBy" to userId
                            )

                            db.collection("studyGroups")
                                .document(groupId)
                                .set(group)
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
                                            showCreateDialog = false
                                        }
                                        .addOnFailureListener {
                                            message = it.localizedMessage
                                                ?: "Membership could not be saved."
                                        }
                                }
                                .addOnFailureListener {
                                    message = it.localizedMessage
                                        ?: "Could not create group."
                                }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Study Group") },
            text = {
                Column {
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it.uppercase() },
                        label = { Text("Enter Group Code") }
                    )

                    if (message.isNotEmpty()) {
                        Text(message)
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
                                                showJoinDialog = false
                                            }
                                            .addOnFailureListener {
                                                message = it.localizedMessage
                                                    ?: "Could not join group."
                                            }
                                    }
                                }
                                .addOnFailureListener {
                                    message = it.localizedMessage
                                        ?: "Could not find group."
                                }
                        }
                    }
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                Button(onClick = { showJoinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
