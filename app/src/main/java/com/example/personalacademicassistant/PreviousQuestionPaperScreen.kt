
package com.example.personalacademicassistant

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.window.Dialog
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

    var papers by remember {
        mutableStateOf<List<QuestionPaper>>(emptyList())
    }

    var examName by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    var searchQuery by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    fun clearForm() {
        examName = ""
        subject = ""
        semester = ""
        year = ""
        link = ""
        editingId = null
        showForm = false
    }

    DisposableEffect(collection) {
        if (collection == null) {
            loading = false
            message = "Please log in to manage your question papers."
            onDispose { }
        } else {
            loading = true

            val listener = collection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    message = error.localizedMessage
                        ?: "Unable to load question papers."
                    loading = false
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

                loading = false
            }

            onDispose {
                listener.remove()
            }
        }
    }

    val filteredPapers = papers.filter { paper ->
        paper.examName.contains(searchQuery, ignoreCase = true) ||
                paper.subject.contains(searchQuery, ignoreCase = true) ||
                paper.semester.contains(searchQuery, ignoreCase = true) ||
                paper.year.contains(searchQuery, ignoreCase = true)
    }

    val uniqueSubjects = papers.map { it.subject }
        .distinct()
        .size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Previous Question Papers",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Your exam preparation library, all in one place.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Preparation Library",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Keep previous papers organized and accessible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = "📚",
                    style = MaterialTheme.typography.displayMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PaperStatCard(
                title = "Total Papers",
                value = papers.size.toString(),
                emoji = "📄",
                modifier = Modifier.weight(1f)
            )

            PaperStatCard(
                title = "Subjects",
                value = uniqueSubjects.toString(),
                emoji = "📚",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search exam, subject or year") },
            leadingIcon = { Text("🔍") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                clearForm()
                message = ""
                showForm = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = userId != null
        ) {
            Text(
                text = "＋  Add Question Paper",
                fontWeight = FontWeight.SemiBold
            )
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message,
                color = if (
                    message.contains("successfully", ignoreCase = true) ||
                    message.contains("deleted", ignoreCase = true)
                ) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saved Question Papers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${filteredPapers.size} papers",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

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

            filteredPapers.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📂",
                                style = MaterialTheme.typography.displayMedium
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (papers.isEmpty()) {
                                    "No question papers yet"
                                } else {
                                    "No matching papers found"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = if (papers.isEmpty()) {
                                    "Add your first previous question paper to get started."
                                } else {
                                    "Try searching with a different keyword."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredPapers,
                        key = { it.id }
                    ) { paper ->

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ) {
                                        Text(
                                            text = "📄",
                                            modifier = Modifier.padding(12.dp),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = paper.examName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = paper.subject,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PaperTag("Semester ${paper.semester}")
                                    PaperTag("Year ${paper.year}")
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(paper.link)
                                                )
                                                context.startActivity(intent)
                                            } catch (exception: Exception) {
                                                message = "Unable to open this paper."
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("↗ Open")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            editingId = paper.id
                                            examName = paper.examName
                                            subject = paper.subject
                                            semester = paper.semester
                                            year = paper.year
                                            link = paper.link
                                            message = ""
                                            showForm = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Edit")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            collection?.document(paper.id)
                                                ?.delete()
                                                ?.addOnSuccessListener {
                                                    message = "Question paper deleted."
                                                }
                                                ?.addOnFailureListener {
                                                    message = "Unable to delete paper."
                                                }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
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
                }
            }
        }
    }

    if (showForm) {

        Dialog(
            onDismissRequest = {
                if (!isSaving) clearForm()
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 650.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = if (editingId == null) {
                            "Add Question Paper"
                        } else {
                            "Edit Question Paper"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Enter the details of your previous examination paper.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = examName,
                        onValueChange = { examName = it },
                        label = { Text("Exam Name") },
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

                    OutlinedTextField(
                        value = semester,
                        onValueChange = { semester = it },
                        label = { Text("Semester") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Year") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("PDF or Document URL") },
                        placeholder = { Text("https://example.com/paper.pdf") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { clearForm() },
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {

                                val currentCollection = collection

                                when {
                                    currentCollection == null -> {
                                        message = "Please log in first."
                                    }

                                    examName.isBlank() ||
                                            subject.isBlank() ||
                                            semester.isBlank() ||
                                            year.isBlank() ||
                                            link.isBlank() -> {
                                        message = "Please fill in all fields."
                                    }

                                    !link.trim().startsWith("https://") &&
                                            !link.trim().startsWith("http://") -> {
                                        message = "Enter a valid HTTP or HTTPS URL."
                                    }

                                    else -> {

                                        val paperData = hashMapOf<String, Any>(
                                            "examName" to examName.trim(),
                                            "subject" to subject.trim(),
                                            "semester" to semester.trim(),
                                            "year" to year.trim(),
                                            "link" to link.trim()
                                        )

                                        isSaving = true
                                        message = ""

                                        val operation = if (editingId == null) {
                                            currentCollection.add(paperData)
                                        } else {
                                            currentCollection
                                                .document(editingId!!)
                                                .update(paperData)
                                        }

                                        operation
                                            .addOnSuccessListener {
                                                message = "Question paper saved successfully."
                                                isSaving = false
                                                clearForm()
                                            }
                                            .addOnFailureListener { exception ->
                                                message = exception.localizedMessage
                                                    ?: "Unable to save question paper."
                                                isSaving = false
                                            }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    if (editingId == null) "Save Paper"
                                    else "Update Paper"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaperStatCard(
    title: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaperTag(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 6.dp
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
