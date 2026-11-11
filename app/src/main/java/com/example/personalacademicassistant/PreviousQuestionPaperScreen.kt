
package com.example.personalacademicassistant

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class QuestionPaper(
    val id: String = "",
    val examName: String = "",
    val subject: String = "",
    val semester: String = "",
    val year: String = "",
    val link: String = ""
)

@Composable
fun PreviousQuestionPaperScreen() {

    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    val collection = remember(userId) {
        userId?.let {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(it)
                .collection("questionPapers")
        }
    }

    var papers by remember { mutableStateOf(emptyList<QuestionPaper>()) }

    var examName by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    var searchQuery by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    DisposableEffect(collection) {

        val listener = collection?.addSnapshotListener { snapshot, error ->

            if (error != null) {
                message = error.localizedMessage ?: "Unable to load papers"
                return@addSnapshotListener
            }

            papers = snapshot?.documents?.map { document ->

                QuestionPaper(
                    id = document.id,
                    examName = document.getString("examName") ?: "",
                    subject = document.getString("subject") ?: "",
                    semester = document.getString("semester") ?: "",
                    year = document.getString("year") ?: "",
                    link = document.getString("link") ?: ""
                )

            } ?: emptyList()
        }

        onDispose {
            listener?.remove()
        }
    }

    val filteredPapers = papers.filter { paper ->

        paper.examName.contains(searchQuery, ignoreCase = true) ||
                paper.subject.contains(searchQuery, ignoreCase = true) ||
                paper.semester.contains(searchQuery, ignoreCase = true) ||
                paper.year.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Previous Question Papers",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search question papers") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                editingId = null
                examName = ""
                subject = ""
                semester = ""
                year = ""
                link = ""
                showForm = true
                message = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("＋ Add Question Paper")
        }

        if (showForm) {

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (editingId == null) "Add New Paper" else "Edit Paper",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = examName,
                onValueChange = { examName = it },
                label = { Text("Exam Name") },
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
                value = semester,
                onValueChange = { semester = it },
                label = { Text("Semester") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("Year") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("PDF or document URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = {

                        val currentCollection = collection

                        if (currentCollection == null) {
                            message = "Please log in first"
                        } else if (
                            examName.isBlank() ||
                            subject.isBlank() ||
                            semester.isBlank() ||
                            year.isBlank() ||
                            link.isBlank()
                        ) {
                            message = "Please fill in all fields"
                        } else if (
                            !link.trim().startsWith("https://") &&
                            !link.trim().startsWith("http://")
                        ) {
                            message = "Enter a valid HTTP or HTTPS URL"
                        } else {

                            val paperData = hashMapOf<String, Any>(
                                "examName" to examName.trim(),
                                "subject" to subject.trim(),
                                "semester" to semester.trim(),
                                "year" to year.trim(),
                                "link" to link.trim()
                            )

                            val operation = if (editingId == null) {
                                currentCollection.add(paperData)
                            } else {
                                currentCollection
                                    .document(editingId!!)
                                    .update(paperData)
                            }

                            operation
                                .addOnSuccessListener {

                                    message = "Question paper saved successfully!"

                                    showForm = false
                                    editingId = null

                                    examName = ""
                                    subject = ""
                                    semester = ""
                                    year = ""
                                    link = ""
                                }
                                .addOnFailureListener { exception ->

                                    message = exception.localizedMessage
                                        ?: "Unable to save question paper"
                                }
                        }
                    }
                ) {
                    Text(
                        if (editingId == null) "Save Paper"
                        else "Update Paper"
                    )
                }

                OutlinedButton(
                    onClick = {
                        showForm = false
                        editingId = null
                        message = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        }

        if (message.isNotBlank()) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Saved Papers (${filteredPapers.size})",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(
                items = filteredPapers,
                key = { it.id }
            ) { paper ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {

                        Text(
                            text = paper.examName,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Subject: ${paper.subject}")
                        Text("Semester: ${paper.semester}")
                        Text("Year: ${paper.year}")

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(paper.link)
                                        )

                                        context.startActivity(intent)

                                    } catch (exception: Exception) {
                                        message = "Unable to open this paper"
                                    }
                                }
                            ) {
                                Text("Open")
                            }

                            OutlinedButton(
                                onClick = {
                                    editingId = paper.id
                                    examName = paper.examName
                                    subject = paper.subject
                                    semester = paper.semester
                                    year = paper.year
                                    link = paper.link
                                    showForm = true
                                    message = ""
                                }
                            ) {
                                Text("Edit")
                            }

                            OutlinedButton(
                                onClick = {

                                    collection
                                        ?.document(paper.id)
                                        ?.delete()
                                        ?.addOnSuccessListener {
                                            message = "Question paper deleted"
                                        }
                                        ?.addOnFailureListener {
                                            message = "Unable to delete paper"
                                        }
                                }
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
