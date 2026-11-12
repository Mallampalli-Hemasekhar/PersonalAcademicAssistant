
package com.example.personalacademicassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private data class SemesterSubject(
    val name: String,
    val totalTopics: Int,
    val completedTopics: Int
)

private data class SemesterItem(
    val id: String,
    val name: String,
    val subjects: List<SemesterSubject>
)

@Composable
fun SemesterProgressScreen() {

    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()

    val semesters = remember { mutableStateListOf<SemesterItem>() }

    var semesterName by remember { mutableStateOf("") }
    var subjectName by remember { mutableStateOf("") }
    var totalTopics by remember { mutableStateOf("") }
    var completedTopics by remember { mutableStateOf("") }

    var showSemesterDialog by remember { mutableStateOf(false) }
    var showSubjectDialog by remember { mutableStateOf(false) }

    var editingIndex by remember { mutableIntStateOf(-1) }
    var selectedSemester by remember { mutableStateOf<SemesterItem?>(null) }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val collection = remember(userId) {
        userId?.let {
            db.collection("users")
                .document(it)
                .collection("semesterProgress")
        }
    }

    // FIRESTORE DATA LISTENER
    DisposableEffect(collection) {

        if (collection == null) {
            loading = false
            onDispose { }
        } else {

            val registration = collection.addSnapshotListener { snapshot, error ->

                if (error != null) {
                    errorMessage = error.message ?: "Unable to load semesters."
                    loading = false
                } else if (snapshot != null) {

                    semesters.clear()

                    snapshot.documents.forEach { document ->

                        val subjectList =
                            document.get("subjects") as? List<*>

                        val parsedSubjects = subjectList?.mapNotNull { item ->

                            val map = item as? Map<*, *>
                                ?: return@mapNotNull null

                            SemesterSubject(
                                name = map["name"] as? String ?: "",
                                totalTopics =
                                    (map["totalTopics"] as? Number)?.toInt() ?: 0,
                                completedTopics =
                                    (map["completedTopics"] as? Number)?.toInt() ?: 0
                            )

                        } ?: emptyList()

                        semesters.add(
                            SemesterItem(
                                id = document.id,
                                name = document.getString("semesterName")
                                    ?: "Semester",
                                subjects = parsedSubjects
                            )
                        )
                    }

                    loading = false
                }
            }

            onDispose {
                registration.remove()
            }
        }
    }

    // SAVE SUBJECTS TO FIRESTORE
    fun saveSubjects(
        semester: SemesterItem,
        updatedSubjects: List<SemesterSubject>
    ) {

        val subjectData = updatedSubjects.map {
            hashMapOf<String, Any>(
                "name" to it.name,
                "totalTopics" to it.totalTopics,
                "completedTopics" to it.completedTopics
            )
        }

        collection?.document(semester.id)
            ?.update("subjects", subjectData)
            ?.addOnSuccessListener {
                errorMessage = ""
            }
            ?.addOnFailureListener {
                errorMessage = it.message ?: "Unable to save subject."
            }
    }

    // LOGIN CHECK
    if (userId == null) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text("Please log in to use Semester Progress Tracker.")
        }

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp)
    ) {

        Text(
            text = "Semester Progress Tracker",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Track your syllabus completion across semesters.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ADD SEMESTER BUTTON
        Button(
            onClick = {
                semesterName = ""
                errorMessage = ""
                showSemesterDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving
        ) {
            Text("＋ Add Semester")
        }

        if (errorMessage.isNotEmpty()) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SEMESTER LIST
        when {

            loading -> {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            semesters.isEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("No semesters added yet. Add your first semester!")
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = semesters,
                        key = { it.id }
                    ) { semester ->

                        val total =
                            semester.subjects.sumOf { it.totalTopics }

                        val completed =
                            semester.subjects.sumOf { it.completedTopics }

                        val progress =
                            if (total > 0) {
                                completed.toFloat() / total.toFloat()
                            } else {
                                0f
                            }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {

                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                Text(
                                    text = semester.name,
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Overall Progress: ${(progress * 100).toInt()}%"
                                )

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )

                                Text("$completed of $total topics completed")

                                Spacer(modifier = Modifier.height(12.dp))

                                HorizontalDivider()

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Subjects",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                if (semester.subjects.isEmpty()) {
                                    Text("No subjects added yet.")
                                }

                                // SUBJECT LIST
                                semester.subjects.forEachIndexed { index, subject ->

                                    val subjectProgress =
                                        if (subject.totalTopics > 0) {
                                            subject.completedTopics.toFloat() /
                                                    subject.totalTopics.toFloat()
                                        } else {
                                            0f
                                        }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor =
                                                MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {

                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {

                                            Text(
                                                text = subject.name,
                                                style = MaterialTheme.typography.titleSmall
                                            )

                                            Text(
                                                "${(subjectProgress * 100).toInt()}% completed"
                                            )

                                            LinearProgressIndicator(
                                                progress = { subjectProgress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp)
                                            )

                                            Text(
                                                "${subject.completedTopics} / ${subject.totalTopics} topics"
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {

                                                TextButton(
                                                    onClick = {
                                                        selectedSemester = semester
                                                        editingIndex = index
                                                        subjectName = subject.name
                                                        totalTopics =
                                                            subject.totalTopics.toString()
                                                        completedTopics =
                                                            subject.completedTopics.toString()
                                                        errorMessage = ""
                                                        showSubjectDialog = true
                                                    }
                                                ) {
                                                    Text("Edit")
                                                }

                                                TextButton(
                                                    onClick = {
                                                        val updated =
                                                            semester.subjects.toMutableList()

                                                        updated.removeAt(index)

                                                        saveSubjects(
                                                            semester,
                                                            updated
                                                        )
                                                    }
                                                ) {
                                                    Text(
                                                        "Delete",
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // ADD SUBJECT BUTTON
                                Button(
                                    onClick = {
                                        selectedSemester = semester
                                        editingIndex = -1
                                        subjectName = ""
                                        totalTopics = ""
                                        completedTopics = ""
                                        errorMessage = ""
                                        showSubjectDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("＋ Add Subject")
                                }

                                TextButton(
                                    onClick = {
                                        collection?.document(semester.id)
                                            ?.delete()
                                            ?.addOnFailureListener {
                                                errorMessage =
                                                    it.message ?: "Delete failed."
                                            }
                                    }
                                ) {
                                    Text(
                                        "Delete Semester",
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

    // ADD SEMESTER DIALOG
    if (showSemesterDialog) {

        AlertDialog(
            onDismissRequest = {
                if (!saving) {
                    showSemesterDialog = false
                }
            },

            title = {
                Text("Add Semester")
            },

            text = {
                Column {

                    OutlinedTextField(
                        value = semesterName,
                        onValueChange = {
                            semesterName = it
                        },
                        label = {
                            Text("Semester Name")
                        },
                        placeholder = {
                            Text("Example: Semester 1")
                        },
                        singleLine = true,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(
                    enabled = !saving,
                    onClick = {

                        if (semesterName.isBlank()) {
                            errorMessage = "Please enter a semester name."
                        } else if (collection == null) {
                            errorMessage = "Unable to connect to your account."
                        } else {

                            saving = true
                            errorMessage = ""

                            val semesterData = hashMapOf<String, Any>(
                                "semesterName" to semesterName.trim(),
                                "subjects" to emptyList<Map<String, Any>>()
                            )

                            collection.add(semesterData)
                                .addOnSuccessListener {
                                    saving = false
                                    showSemesterDialog = false
                                    semesterName = ""
                                }
                                .addOnFailureListener {
                                    saving = false
                                    errorMessage =
                                        it.message ?: "Unable to add semester."
                                }
                        }
                    }
                ) {

                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save")
                    }
                }
            },

            dismissButton = {

                TextButton(
                    enabled = !saving,
                    onClick = {
                        showSemesterDialog = false
                        errorMessage = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // ADD / EDIT SUBJECT DIALOG
    if (showSubjectDialog) {

        AlertDialog(
            onDismissRequest = {
                showSubjectDialog = false
            },

            title = {
                Text(
                    if (editingIndex == -1) {
                        "Add Subject"
                    } else {
                        "Edit Subject"
                    }
                )
            },

            text = {

                Column {

                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = {
                            subjectName = it
                        },
                        label = {
                            Text("Subject Name")
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = totalTopics,
                        onValueChange = {
                            totalTopics = it
                        },
                        label = {
                            Text("Total Topics")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = completedTopics,
                        onValueChange = {
                            completedTopics = it
                        },
                        label = {
                            Text("Completed Topics")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true
                    )

                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        val total = totalTopics.toIntOrNull()
                        val completed = completedTopics.toIntOrNull()

                        if (
                            subjectName.isNotBlank() &&
                            total != null &&
                            completed != null &&
                            total > 0 &&
                            completed >= 0 &&
                            completed <= total
                        ) {

                            val semester = selectedSemester

                            if (semester != null) {

                                val updated =
                                    semester.subjects.toMutableList()

                                val newSubject = SemesterSubject(
                                    name = subjectName.trim(),
                                    totalTopics = total,
                                    completedTopics = completed
                                )

                                if (editingIndex == -1) {
                                    updated.add(newSubject)
                                } else if (editingIndex in updated.indices) {
                                    updated[editingIndex] = newSubject
                                }

                                saveSubjects(semester, updated)

                                showSubjectDialog = false
                                errorMessage = ""
                            }

                        } else {

                            errorMessage =
                                "Enter valid values. Completed topics cannot exceed total topics."
                        }
                    }
                ) {
                    Text("Save")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showSubjectDialog = false
                        errorMessage = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
