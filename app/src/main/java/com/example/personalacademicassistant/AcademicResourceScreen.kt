
package com.example.personalacademicassistant

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var selectedFilter by remember { mutableStateOf("All") }

    var editingId by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    val types = listOf("PDF", "Website", "Video", "Book", "Notes", "Other")
    val filters = listOf("All") + types

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

        val matchesSearch =
            resource.title.contains(searchQuery, ignoreCase = true) ||
                    resource.subject.contains(searchQuery, ignoreCase = true) ||
                    resource.type.contains(searchQuery, ignoreCase = true) ||
                    resource.description.contains(searchQuery, ignoreCase = true)

        val matchesType =
            selectedFilter == "All" || resource.type == selectedFilter

        matchesSearch && matchesType
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Column {
                    Text(
                        text = "Academic Resources 📚",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Your personal library for smarter learning.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {

                        Text(
                            text = "My Learning Library",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = "Keep all your important study materials organized in one place.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${resources.size}",
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Total Resources",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "📖",
                                fontSize = 46.sp
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ResourceStatCard(
                        value = "${resources.count { it.type == "PDF" }}",
                        label = "PDFs",
                        emoji = "📄",
                        modifier = Modifier.weight(1f)
                    )

                    ResourceStatCard(
                        value = "${resources.count { it.type == "Video" }}",
                        label = "Videos",
                        emoji = "🎬",
                        modifier = Modifier.weight(1f)
                    )

                    ResourceStatCard(
                        value = "${resources.count { it.type == "Book" }}",
                        label = "Books",
                        emoji = "📚",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search title, subject or type...") },
                    leadingIcon = {
                        Text("🔍", fontSize = 19.sp)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            TextButton(onClick = { searchQuery = "" }) {
                                Text("Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            item {
                Column {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(
                            rememberScrollState()
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filters.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter) }
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your Resources",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "${filteredResources.size} resources found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            clearForm()
                            message = ""
                            showForm = true
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Add")
                    }
                }
            }

            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(35.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredResources.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📚", fontSize = 42.sp)

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (resources.isEmpty()) {
                                    "Your library is empty"
                                } else {
                                    "No matching resources found"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = if (resources.isEmpty()) {
                                    "Add your first study resource to get started."
                                } else {
                                    "Try another search or category."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (resources.isEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        clearForm()
                                        showForm = true
                                    }
                                ) {
                                    Text("Add First Resource")
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredResources,
                    key = { it.id }
                ) { resource ->

                    AcademicResourceCard(
                        resource = resource,
                        onOpen = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(resource.link)
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                message = "Unable to open this resource link."
                            }
                        },
                        onEdit = {
                            title = resource.title
                            subject = resource.subject
                            description = resource.description
                            link = resource.link
                            selectedType = resource.type
                            editingId = resource.id
                            message = ""
                            showForm = true
                        },
                        onDelete = {
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
                    )
                }
            }

            item {
                if (message.isNotBlank()) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showForm) {

        var expanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { clearForm() },

            shape = RoundedCornerShape(24.dp),

            title = {
                Text(
                    text = if (editingId == null) {
                        "Add New Resource 📚"
                    } else {
                        "Edit Resource ✏️"
                    },
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Resource Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "Resource Type",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
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
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            types.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
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
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("Resource Link (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },

            confirmButton = {
                Button(
                    onClick = {

                        if (saving) return@Button

                        if (title.isBlank() || subject.isBlank()) {
                            message = "Please enter the resource title and subject."
                            return@Button
                        }

                        if (
                            link.isNotBlank() &&
                            !link.startsWith("https://", ignoreCase = true) &&
                            !link.startsWith("http://", ignoreCase = true)
                        ) {
                            message = "Enter a valid link starting with http:// or https://."
                            return@Button
                        }

                        val uid = userId

                        if (uid == null) {
                            message = "Please log in first."
                            return@Button
                        }

                        saving = true

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
                                    saving = false
                                    message = "Resource saved successfully!"
                                    clearForm()
                                }
                                .addOnFailureListener { error ->
                                    saving = false
                                    message = error.localizedMessage
                                        ?: "Failed to save resource."
                                }

                        } else {

                            val documentId = editingId

                            if (documentId == null) {
                                saving = false
                                return@Button
                            }

                            collection.document(documentId)
                                .update(resourceData)
                                .addOnSuccessListener {
                                    saving = false
                                    message = "Resource updated successfully!"
                                    clearForm()
                                }
                                .addOnFailureListener { error ->
                                    saving = false
                                    message = error.localizedMessage
                                        ?: "Failed to update resource."
                                }
                        }
                    },
                    enabled = !saving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(if (saving) "Saving..." else "Save Resource")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = { clearForm() },
                    enabled = !saving
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ResourceStatCard(
    value: String,
    label: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp, horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AcademicResourceCard(
    resource: AcademicResource,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val emoji = when (resource.type) {
        "PDF" -> "📄"
        "Website" -> "🌐"
        "Video" -> "🎬"
        "Book" -> "📚"
        "Notes" -> "📝"
        else -> "📁"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = emoji,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 25.sp
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = resource.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = resource.subject,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = resource.type,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (resource.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = resource.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (resource.link.isNotBlank()) {
                    TextButton(onClick = onOpen) {
                        Text("Open ↗")
                    }
                }

                TextButton(onClick = onEdit) {
                    Text("Edit")
                }

                TextButton(onClick = onDelete) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
