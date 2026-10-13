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
import androidx.compose.material3.CircularProgressIndicator
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

data class Subject(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val faculty: String = "",
    val credits: String = ""
)

@Composable
fun SubjectScreen() {

    var showAddSubject by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val subjects = remember {
        mutableStateListOf<Subject>()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val currentUser = auth.currentUser

    // Load subjects from Firestore
    LaunchedEffect(Unit) {

        if (currentUser == null) {

            errorMessage = "User is not logged in."
            isLoading = false

        } else {

            firestore
                .collection("users")
                .document(currentUser.uid)
                .collection("subjects")
                .get()
                .addOnSuccessListener { result ->

                    subjects.clear()

                    for (document in result.documents) {

                        val subject = Subject(
                            id = document.id,
                            name = document.getString("name") ?: "",
                            code = document.getString("code") ?: "",
                            faculty = document.getString("faculty") ?: "",
                            credits = document.getString("credits") ?: ""
                        )

                        subjects.add(subject)
                    }

                    isLoading = false
                }
                .addOnFailureListener { exception ->

                    errorMessage =
                        exception.message ?: "Failed to load subjects."

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
            text = "📚 Subjects",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Manage your academic subjects",
            fontSize = 15.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                showAddSubject = true
                errorMessage = ""
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3F51B5)
            )
        ) {
            Text(
                text = "+ Add Subject",
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                CircularProgressIndicator()

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Loading subjects...",
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

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = errorMessage,
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }

        } else if (subjects.isEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "📚",
                    fontSize = 50.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "No subjects added yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172554)
                )

                Text(
                    text = "Add your first subject to get started.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

        } else {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = subjects,
                    key = { it.id }
                ) { subject ->

                    SubjectCard(
                        subject = subject,
                        onDelete = {

                            currentUser?.let { user ->

                                firestore
                                    .collection("users")
                                    .document(user.uid)
                                    .collection("subjects")
                                    .document(subject.id)
                                    .delete()
                                    .addOnSuccessListener {

                                        subjects.remove(subject)
                                    }
                                    .addOnFailureListener { exception ->

                                        errorMessage =
                                            exception.message
                                                ?: "Failed to delete subject."
                                    }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddSubject) {

        AddSubjectDialog(
            onDismiss = {
                showAddSubject = false
            },

            onAddSubject = { subject ->

                if (currentUser == null) {

                    errorMessage = "User is not logged in."
                    showAddSubject = false

                } else {

                    val subjectData = hashMapOf(
                        "name" to subject.name,
                        "code" to subject.code,
                        "faculty" to subject.faculty,
                        "credits" to subject.credits
                    )

                    firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .collection("subjects")
                        .add(subjectData)
                        .addOnSuccessListener { documentReference ->

                            subjects.add(
                                subject.copy(
                                    id = documentReference.id
                                )
                            )

                            showAddSubject = false
                            errorMessage = ""
                        }
                        .addOnFailureListener { exception ->

                            errorMessage =
                                exception.message
                                    ?: "Failed to add subject."
                        }
                }
            }
        )
    }
}

@Composable
fun SubjectCard(
    subject: Subject,
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
                text = subject.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172554)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Code: ${subject.code}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = "Faculty: ${subject.faculty}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Text(
                text = "Credits: ${subject.credits}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
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
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onAddSubject: (Subject) -> Unit
) {

    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("") }

    AlertDialog(

        onDismissRequest = {
            onDismiss()
        },

        title = {
            Text(
                text = "Add Subject",
                fontWeight = FontWeight.Bold
            )
        },

        text = {

            Column {

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Subject Name")
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Subject Code")
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = faculty,
                    onValueChange = {
                        faculty = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Faculty Name")
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = credits,
                    onValueChange = {
                        credits = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Credits")
                    },
                    singleLine = true
                )
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    if (
                        name.isNotBlank() &&
                        code.isNotBlank() &&
                        faculty.isNotBlank() &&
                        credits.isNotBlank()
                    ) {

                        onAddSubject(
                            Subject(
                                name = name,
                                code = code,
                                faculty = faculty,
                                credits = credits
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