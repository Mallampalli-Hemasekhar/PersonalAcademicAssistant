
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class AcademicNote(
    val id: String = "",
    val subject: String = "",
    val title: String = "",
    val content: String = ""
)

@Composable
fun NotesScreen() {

    var showNoteDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<AcademicNote?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val notes = remember { mutableStateListOf<AcademicNote>() }

    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUser = auth.currentUser

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            errorMessage = "User is not logged in."
            isLoading = false
        } else {
            firestore.collection("users")
                .document(currentUser.uid)
                .collection("notes")
                .get()
                .addOnSuccessListener { result ->

                    notes.clear()

                    result.documents.forEach { document ->
                        notes.add(
                            AcademicNote(
                                id = document.id,
                                subject = document.getString("subject") ?: "",
                                title = document.getString("title") ?: "",
                                content = document.getString("content") ?: ""
                            )
                        )
                    }

                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    errorMessage = exception.message ?: "Failed to load notes."
                    isLoading = false
                }
        }
    }

    val filteredNotes = notes.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.subject.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
    }

    val groupedNotes = filteredNotes.groupBy { it.subject.ifBlank { "Other" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {

        Text(
            text = "My Academic Notes",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Create, organize and manage your study notes",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Notes",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = notes.size.toString(),
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(text = "📚", fontSize = 35.sp)
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search Notes") },
            placeholder = { Text("Search by title, subject or content") },
            leadingIcon = {
                Text("🔍")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    TextButton(onClick = { searchQuery = "" }) {
                        Text("Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                editingNote = null
                showNoteDialog = true
                errorMessage = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "+ Add New Note",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            filteredNotes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "🔍" else "📒",
                            fontSize = 50.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "No Matching Notes"
                            } else {
                                "No Notes Yet"
                            },
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "Try another search term."
                            } else {
                                "Add your first academic note."
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedNotes.forEach { (subject, subjectNotes) ->

                        item(key = "subject_$subject") {
                            Text(
                                text = "📚 $subject",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(
                                    top = 8.dp,
                                    bottom = 2.dp
                                )
                            )
                        }

                        items(
                            items = subjectNotes,
                            key = { it.id }
                        ) { note ->

                            NoteCard(
                                note = note,
                                onEdit = {
                                    editingNote = note
                                    showNoteDialog = true
                                },
                                onDelete = {

                                    currentUser?.let { user ->
                                        firestore.collection("users")
                                            .document(user.uid)
                                            .collection("notes")
                                            .document(note.id)
                                            .delete()
                                            .addOnSuccessListener {
                                                notes.remove(note)
                                                errorMessage = ""
                                            }
                                            .addOnFailureListener { exception ->
                                                errorMessage = exception.message
                                                    ?: "Failed to delete note."
                                            }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNoteDialog) {
        AddNoteDialog(
            initialNote = editingNote,
            onDismiss = {
                showNoteDialog = false
                editingNote = null
            },
            onAddNote = { note ->

                if (currentUser == null) {
                    errorMessage = "User is not logged in."
                    showNoteDialog = false
                } else {

                    val noteData = hashMapOf(
                        "subject" to note.subject,
                        "title" to note.title,
                        "content" to note.content
                    )

                    val notesCollection = firestore.collection("users")
                        .document(currentUser.uid)
                        .collection("notes")

                    if (note.id.isEmpty()) {

                        notesCollection.add(noteData)
                            .addOnSuccessListener { documentReference ->

                                notes.add(
                                    note.copy(id = documentReference.id)
                                )

                                showNoteDialog = false
                                editingNote = null
                                errorMessage = ""
                            }
                            .addOnFailureListener { exception ->
                                errorMessage = exception.message
                                    ?: "Failed to add note."
                            }

                    } else {

                        notesCollection.document(note.id)
                            .update(noteData as Map<String, Any>)
                            .addOnSuccessListener {

                                val index = notes.indexOfFirst {
                                    it.id == note.id
                                }

                                if (index != -1) {
                                    notes[index] = note
                                }

                                showNoteDialog = false
                                editingNote = null
                                errorMessage = ""
                            }
                            .addOnFailureListener { exception ->
                                errorMessage = exception.message
                                    ?: "Failed to update note."
                            }
                    }
                }
            }
        )
    }
}

@Composable
fun NoteCard(
    note: AcademicNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(17.dp)
        ) {

            Text(
                text = note.title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = "📚 ${note.subject}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = note.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Text(
                        text = "Edit",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(onClick = onDelete) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun AddNoteDialog(
    initialNote: AcademicNote? = null,
    onDismiss: () -> Unit,
    onAddNote: (AcademicNote) -> Unit
) {

    var subject by remember(initialNote) {
        mutableStateOf(initialNote?.subject ?: "")
    }

    var title by remember(initialNote) {
        mutableStateOf(initialNote?.title ?: "")
    }

    var content by remember(initialNote) {
        mutableStateOf(initialNote?.content ?: "")
    }

    var validationMessage by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (initialNote == null) "Add New Note" else "Edit Note",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        validationMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject") },
                    placeholder = { Text("Example: Data Structures") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note Title") },
                    placeholder = { Text("Example: Stack Operations") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        validationMessage = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    label = { Text("Note Content") },
                    placeholder = {
                        Text("Write your academic notes here...")
                    }
                )

                if (validationMessage.isNotEmpty()) {
                    Text(
                        text = validationMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (
                        subject.isNotBlank() &&
                        title.isNotBlank() &&
                        content.isNotBlank()
                    ) {
                        onAddNote(
                            AcademicNote(
                                id = initialNote?.id ?: "",
                                subject = subject.trim(),
                                title = title.trim(),
                                content = content.trim()
                            )
                        )
                    } else {
                        validationMessage = "Please fill in all fields."
                    }
                }
            ) {
                Text(
                    if (initialNote == null) "Add Note" else "Save Changes"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
