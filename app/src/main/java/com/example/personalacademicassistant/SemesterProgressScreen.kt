
package com.example.personalacademicassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val semesters = remember {
        mutableStateListOf<SemesterItem>()
    }

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

    val colors = MaterialTheme.colorScheme

    // Firestore listener

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

                        val subjectList = document.get("subjects") as? List<*>

                        val parsedSubjects = subjectList?.mapNotNull { item ->

                            val map = item as? Map<*, *>
                                ?: return@mapNotNull null

                            SemesterSubject(
                                name = map["name"] as? String ?: "",
                                totalTopics = (map["totalTopics"] as? Number)?.toInt() ?: 0,
                                completedTopics = (map["completedTopics"] as? Number)?.toInt() ?: 0
                            )
                        } ?: emptyList()

                        semesters.add(
                            SemesterItem(
                                id = document.id,
                                name = document.getString("semesterName") ?: "Semester",
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

    // Save subjects

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

    // Login check

    if (userId == null) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "Please log in to use Semester Progress Tracker.",
                color = colors.onBackground
            )
        }

        return
    }

    val totalTopicsOverall = semesters.sumOf { semester ->
        semester.subjects.sumOf { it.totalTopics }
    }

    val completedTopicsOverall = semesters.sumOf { semester ->
        semester.subjects.sumOf { it.completedTopics }
    }

    val overallProgress = if (totalTopicsOverall > 0) {
        completedTopicsOverall.toFloat() / totalTopicsOverall.toFloat()
    } else {
        0f
    }

    val remainingTopics = (totalTopicsOverall - completedTopicsOverall).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(18.dp)
    ) {

        Text(
            text = "📊 Semester Progress",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Monitor your syllabus completion across semesters.",
            fontSize = 14.sp,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Overall progress card

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.primaryContainer
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Overall Syllabus Progress",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer
                    )

                    Text(
                        text = "${(overallProgress * 100).toInt()}%",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp),
                    color = colors.primary,
                    trackColor = colors.surface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$completedTopicsOverall of $totalTopicsOverall topics completed",
                    fontSize = 13.sp,
                    color = colors.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Statistics

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            SemesterStatCard(
                title = "Semesters",
                value = semesters.size.toString(),
                modifier = Modifier.weight(1f)
            )

            SemesterStatCard(
                title = "Completed",
                value = completedTopicsOverall.toString(),
                modifier = Modifier.weight(1f)
            )

            SemesterStatCard(
                title = "Remaining",
                value = remainingTopics.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Add semester button

        Button(
            onClick = {
                semesterName = ""
                errorMessage = ""
                showSemesterDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !saving,
            shape = RoundedCornerShape(13.dp)
        ) {

            Text(
                text = "＋ Add New Semester",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (errorMessage.isNotEmpty() && !showSemesterDialog && !showSubjectDialog) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage,
                color = colors.error,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Semesters",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Semester list

        when {

            loading -> {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }

            semesters.isEmpty() -> {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surfaceVariant
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(25.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Text(
                            text = "📚",
                            fontSize = 40.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "No Semesters Yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )

                        Text(
                            text = "Add your first semester to start tracking your syllabus.",
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    items(
                        items = semesters,
                        key = { it.id }
                    ) { semester ->

                        val total = semester.subjects.sumOf {
                            it.totalTopics
                        }

                        val completed = semester.subjects.sumOf {
                            it.completedTopics
                        }

                        val progress = if (total > 0) {
                            completed.toFloat() / total.toFloat()
                        } else {
                            0f
                        }

                        SemesterProgressCard(
                            semester = semester,
                            progress = progress,
                            total = total,
                            completed = completed,
                            onAddSubject = {

                                selectedSemester = semester
                                editingIndex = -1
                                subjectName = ""
                                totalTopics = ""
                                completedTopics = ""
                                errorMessage = ""
                                showSubjectDialog = true
                            },
                            onEditSubject = { index, subject ->

                                selectedSemester = semester
                                editingIndex = index
                                subjectName = subject.name
                                totalTopics = subject.totalTopics.toString()
                                completedTopics = subject.completedTopics.toString()
                                errorMessage = ""
                                showSubjectDialog = true
                            },
                            onDeleteSubject = { index ->

                                val updated = semester.subjects.toMutableList()
                                updated.removeAt(index)

                                saveSubjects(semester, updated)
                            },
                            onDeleteSemester = {

                                collection?.document(semester.id)
                                    ?.delete()
                                    ?.addOnFailureListener {
                                        errorMessage = it.message ?: "Delete failed."
                                    }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add semester dialog

    if (showSemesterDialog) {

        AlertDialog(
            onDismissRequest = {
                if (!saving) {
                    showSemesterDialog = false
                    errorMessage = ""
                }
            },
            title = {
                Text(
                    "Add Semester",
                    fontWeight = FontWeight.Bold
                )
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMessage.isNotEmpty()) {

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errorMessage,
                            color = colors.error,
                            fontSize = 13.sp
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
                                    errorMessage = it.message
                                        ?: "Unable to add semester."
                                }
                        }
                    }
                ) {

                    if (saving) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
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
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Add / edit subject dialog

    if (showSubjectDialog) {

        AlertDialog(
            onDismissRequest = {
                showSubjectDialog = false
                errorMessage = ""
            },
            title = {

                Text(
                    text = if (editingIndex == -1) {
                        "Add Subject"
                    } else {
                        "Edit Subject"
                    },
                    fontWeight = FontWeight.Bold
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
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = totalTopics,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                totalTopics = it
                            }
                        },
                        label = {
                            Text("Total Topics")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = completedTopics,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                completedTopics = it
                            }
                        },
                        label = {
                            Text("Completed Topics")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMessage.isNotEmpty()) {

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errorMessage,
                            color = colors.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        val total = totalTopics.toIntOrNull()
                        val completed = completedTopics.toIntOrNull()

                        if (subjectName.isBlank()) {

                            errorMessage = "Please enter a subject name."

                        } else if (total == null || total <= 0) {

                            errorMessage = "Enter a valid total topic count."

                        } else if (
                            completed == null ||
                            completed < 0 ||
                            completed > total
                        ) {

                            errorMessage = "Completed topics cannot exceed total topics."

                        } else {

                            val semester = selectedSemester

                            if (semester != null) {

                                val updated = semester.subjects.toMutableList()

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
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// Semester progress card

@Composable
private fun SemesterProgressCard(
    semester: SemesterItem,
    progress: Float,
    total: Int,
    completed: Int,
    onAddSubject: () -> Unit,
    onEditSubject: (Int, SemesterSubject) -> Unit,
    onDeleteSubject: (Int) -> Unit,
    onDeleteSemester: () -> Unit
) {

    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = semester.name,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Text(
                        text = "${semester.subjects.size} subjects",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.primaryContainer
                ) {

                    Text(
                        text = "${(progress * 100).toInt()}%",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = colors.primary,
                trackColor = colors.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "$completed / $total topics",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )

                Text(
                    text = "${(total - completed).coerceAtLeast(0)} remaining",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            HorizontalDivider(
                color = colors.outlineVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Subject Breakdown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            if (semester.subjects.isEmpty()) {

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "No subjects added yet.",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant
                )
            }

            semester.subjects.forEachIndexed { index, subject ->

                val subjectProgress = if (subject.totalTopics > 0) {
                    subject.completedTopics.toFloat() / subject.totalTopics.toFloat()
                } else {
                    0f
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surfaceVariant
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = subject.name,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = colors.onSurface
                            )

                            Text(
                                text = "${(subjectProgress * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = colors.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { subjectProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = colors.secondary,
                            trackColor = colors.surface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${subject.completedTopics} / ${subject.totalTopics} topics completed",
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {

                            TextButton(
                                onClick = {
                                    onEditSubject(index, subject)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {

                                Text("Edit")
                            }

                            TextButton(
                                onClick = {
                                    onDeleteSubject(index)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {

                                Text(
                                    text = "Delete",
                                    color = colors.error
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onAddSubject,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {

                Text("＋ Add Subject")
            }

            TextButton(
                onClick = onDeleteSemester,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "Delete Semester",
                    color = colors.error
                )
            }
        }
    }
}

// Statistics card

@Composable
private fun SemesterStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = value,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
