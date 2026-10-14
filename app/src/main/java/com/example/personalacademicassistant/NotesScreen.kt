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

data class AcademicNote(
    val id: String = "",
    val subject: String = "",
    val title: String = "",
    val content: String = ""
)

@Composable
fun NotesScreen() {

    var showAddNote by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    val notes = remember {
        mutableStateListOf<AcademicNote>()
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
                .collection("notes")
                .get()
                .addOnSuccessListener { result ->

                    notes.clear()

                    for (document in result.documents) {

                        val note = AcademicNote(
                            id = document.id,
                            subject = document.getString("subject") ?: "",
                            title = document.getString("title") ?: "",
                            content = document.getString("content") ?: ""
                        )

                        notes.add(note)
                    }

                    isLoading = false
                }
                .addOnFailureListener { exception ->

                    errorMessage =
                        exception.message ?: "Failed to load notes."

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
            text = "📒 Notes",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Create and manage your academic notes",
            fontSize = 15.sp,
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(

            onClick = {

                showAddNote = true
                errorMessage = ""
            },

            modifier = Modifier.fillMaxWidth(),

            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3F51B5)
            )

        ) {

            Text(
                text = "+ Add Note",
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
                    text = "Loading notes...",
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

        } else if (notes.isEmpty()) {

            Column(

                modifier = Modifier.fillMaxSize(),

                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement = Arrangement.Center

            ) {

                Text(
                    text = "📒",
                    fontSize = 50.sp
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "No notes yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172554)
                )

                Text(
                    text = "Add your first academic note.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

        } else {

            LazyColumn(

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)

            ) {

                items(

                    items = notes,

                    key = {
                        it.id
                    }

                ) { note ->

                    NoteCard(

                        note = note,

                        onDelete = {

                            currentUser?.let { user ->

                                firestore
                                    .collection("users")
                                    .document(user.uid)
                                    .collection("notes")
                                    .document(note.id)
                                    .delete()
                                    .addOnSuccessListener {

                                        notes.remove(note)
                                    }
                                    .addOnFailureListener { exception ->

                                        errorMessage =
                                            exception.message
                                                ?: "Failed to delete note."
                                    }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddNote) {

        AddNoteDialog(

            onDismiss = {

                showAddNote = false
            },

            onAddNote = { note ->

                if (currentUser == null) {

                    errorMessage =
                        "User is not logged in."

                    showAddNote = false

                } else {

                    val noteData = hashMapOf(

                        "subject" to note.subject,

                        "title" to note.title,

                        "content" to note.content
                    )

                    firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .collection("notes")
                        .add(noteData)
                        .addOnSuccessListener { documentReference ->

                            notes.add(

                                note.copy(
                                    id = documentReference.id
                                )
                            )

                            showAddNote = false
                            errorMessage = ""
                        }
                        .addOnFailureListener { exception ->

                            errorMessage =
                                exception.message
                                    ?: "Failed to add note."
                        }
                }
            }
        )
    }
}


@Composable
fun NoteCard(
    note: AcademicNote,
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

                text = note.title,

                fontSize = 20.sp,

                fontWeight = FontWeight.Bold,

                color = Color(0xFF172554)
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(

                text = "📚 Subject: ${note.subject}",

                fontSize = 14.sp,

                color = Color.Gray
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(

                text = note.content,

                fontSize = 15.sp,

                color = Color.DarkGray
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(

                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.End

            ) {

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
fun AddNoteDialog(

    onDismiss: () -> Unit,

    onAddNote: (AcademicNote) -> Unit

) {

    var subject by remember {
        mutableStateOf("")
    }

    var title by remember {
        mutableStateOf("")
    }

    var content by remember {
        mutableStateOf("")
    }

    AlertDialog(

        onDismissRequest = {

            onDismiss()
        },

        title = {

            Text(

                text = "Add Note",

                fontWeight = FontWeight.Bold
            )
        },

        text = {

            Column {

                OutlinedTextField(

                    value = subject,

                    onValueChange = {

                        subject = it
                    },

                    modifier = Modifier.fillMaxWidth(),

                    label = {

                        Text("Subject")
                    },

                    placeholder = {

                        Text("Example: Data Structures")
                    },

                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(

                    value = title,

                    onValueChange = {

                        title = it
                    },

                    modifier = Modifier.fillMaxWidth(),

                    label = {

                        Text("Note Title")
                    },

                    placeholder = {

                        Text("Example: Stack Operations")
                    },

                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(

                    value = content,

                    onValueChange = {

                        content = it
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),

                    label = {

                        Text("Note Content")
                    },

                    placeholder = {

                        Text("Write your academic notes here...")
                    }
                )
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

                                subject = subject,

                                title = title,

                                content = content
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