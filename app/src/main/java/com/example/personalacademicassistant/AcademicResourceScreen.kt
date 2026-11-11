
package com.example.personalacademicassistant

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

data class AcademicResource(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val type: String = "PDF",
    val description: String = "",
    val link: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicResourceScreen() {

    val context = LocalContext.current
    val userId = Firebase.auth.currentUser?.uid
    val db = Firebase.firestore

    var resources by remember {
        mutableStateOf<List<AcademicResource>>(emptyList())
    }

    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("PDF") }
    var searchQuery by remember { mutableStateOf("") }

    var editingId by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    val types = listOf("PDF", "Website", "Video", "Book", "Notes", "Other")

    fun clearForm() {
        title = ""
        subject = ""
        description = ""
        link = ""
        selectedType = "PDF"
        editingId = null
        showForm = false
    }

    DisposableEffect(userId) {

        if (userId == null) {
            loading = false
            message = "Please log in to manage your resources."
            onDispose { }
        } else {

            loading = true

            val registration = db.collection("users")
                .document(userId)
                .collection("academicResources")
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        message = error.localizedMessage
                            ?: "Unable to load resources."
                        loading = false
                        return@addSnapshotListener
                    }

                    resources = snapshot?.documents?.map { document ->

                        AcademicResource(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            subject = document.getString("subject") ?: "",
                            type = document.getString("type") ?: "PDF",
                            description = document.getString("description") ?: "",
                            link = document.getString("link") ?: ""
                        )

                    } ?: emptyList()

                    loading = false
                }

            onDispose {
                registration.remove()
            }
        }
    }

    val filteredResources = resources.filter { resource ->

        resource.title.contains(searchQuery, ignoreCase = true) ||
                resource.subject.contains(searchQuery, ignoreCase = true) ||
                resource.type.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        Text(
            text = "📚 Academic Resource Manager",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Organize all your study materials in one place.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search resources") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                clearForm()
                message = ""
                showForm = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("＋ Add New Resource")
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (loading) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

        } else if (filteredResources.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (resources.isEmpty()) {
                        "No resources added yet.\nTap Add New Resource to get started."
                    } else {
                        "No matching resources found."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = filteredResources,
                    key = { it.id }
                ) { resource ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = resource.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Subject: ${resource.subject}")
                            Text("Type: ${resource.type}")

                            if (resource.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(resource.description)
                            }

                            if (resource.link.isNotBlank()) {

                                Text(
                                    text = "Open Resource ↗",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable {
                                            try {
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(resource.link)
                                                )

                                                context.startActivity(intent)

                                            } catch (e: Exception) {
                                                message = "Unable to open this link."
                                            }
                                        }
                                        .padding(vertical = 8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {

                                TextButton(
                                    onClick = {

                                        title = resource.title
                                        subject = resource.subject
                                        description = resource.description
                                        link = resource.link
                                        selectedType = resource.type
                                        editingId = resource.id

                                        message = ""
                                        showForm = true
                                    }
                                ) {
                                    Text("Edit")
                                }

                                TextButton(
                                    onClick = {

                                        if (userId != null) {

                                            db.collection("users")
                                                .document(userId)
                                                .collection("academicResources")
                                                .document(resource.id)
                                                .delete()
                                                .addOnSuccessListener {
                                                    message = "Resource deleted successfully!"
                                                }
                                                .addOnFailureListener { error ->
                                                    message = error.localizedMessage
                                                        ?: "Failed to delete resource."
                                                }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {

        var expanded by remember { mutableStateOf(false) }

        AlertDialog(

            onDismissRequest = {
                clearForm()
            },

            title = {
                Text(
                    if (editingId == null) {
                        "Add Resource"
                    } else {
                        "Edit Resource"
                    }
                )
            },

            text = {

                Column(
                    modifier = Modifier.heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Resource Title") },
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

                    Text(
                        text = "Resource Type",
                        fontWeight = FontWeight.Bold
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = {
                            expanded = !expanded
                        }
                    ) {

                        OutlinedTextField(
                            value = selectedType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Type") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expanded
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = {
                                expanded = false
                            }
                        ) {

                            types.forEach { type ->

                                DropdownMenuItem(
                                    text = {
                                        Text(type)
                                    },
                                    onClick = {
                                        selectedType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("Resource Link (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri
                        ),
                        singleLine = true
                    )
                }
            },

            confirmButton = {

                Button(
                    onClick = {

                        if (title.isBlank() || subject.isBlank()) {
                            message = "Please enter title and subject."
                            return@Button
                        }

                        if (
                            link.isNotBlank() &&
                            !link.startsWith("https://") &&
                            !link.startsWith("http://")
                        ) {
                            message = "Enter a valid link starting with http:// or https://."
                            return@Button
                        }

                        val uid = userId

                        if (uid == null) {
                            message = "Please log in first."
                            return@Button
                        }

                        val resourceData = hashMapOf<String, Any>(
                            "title" to title.trim(),
                            "subject" to subject.trim(),
                            "type" to selectedType,
                            "description" to description.trim(),
                            "link" to link.trim(),
                            "updatedAt" to System.currentTimeMillis()
                        )

                        val collection = db.collection("users")
                            .document(uid)
                            .collection("academicResources")

                        if (editingId == null) {

                            resourceData["createdAt"] =
                                System.currentTimeMillis()

                            collection.add(resourceData)
                                .addOnSuccessListener {
                                    message = "Resource saved successfully!"
                                    clearForm()
                                }
                                .addOnFailureListener { error ->
                                    message = error.localizedMessage
                                        ?: "Failed to save resource."
                                }

                        } else {

                            collection.document(editingId!!)
                                .update(resourceData)
                                .addOnSuccessListener {
                                    message = "Resource updated successfully!"
                                    clearForm()
                                }
                                .addOnFailureListener { error ->
                                    message = error.localizedMessage
                                        ?: "Failed to update resource."
                                }
                        }
                    }
                ) {
                    Text("Save Resource")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        clearForm()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
