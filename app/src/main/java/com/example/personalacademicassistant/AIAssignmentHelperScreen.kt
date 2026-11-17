
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch

@Composable
fun AIAssignmentHelperScreen() {

    val scope = rememberCoroutineScope()

    val model = remember {
        Firebase.ai(
            backend = GenerativeBackend.googleAI(),
            useLimitedUseAppCheckTokens = true
        ).generativeModel("gemini-3.5-flash")
    }

    var subject by remember { mutableStateOf("") }
    var assignmentTitle by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }

    var assignmentType by remember {
        mutableStateOf("Programming Assignment")
    }

    var academicLevel by remember {
        mutableStateOf("B.Tech")
    }

    var wordCount by remember {
        mutableStateOf("1000 words")
    }

    var generatedContent by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf("") }

    val assignmentTypes = listOf(
        "Programming Assignment",
        "Theory Assignment",
        "Research Assignment",
        "Case Study",
        "Project Report",
        "Lab Assignment"
    )

    val academicLevels = listOf(
        "B.Tech",
        "BCA",
        "B.Sc",
        "M.Tech",
        "MCA",
        "Other"
    )

    val wordCounts = listOf(
        "500 words",
        "1000 words",
        "1500 words",
        "2000 words",
        "2500 words"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🤖",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "AI Assignment Helper",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Plan, understand and prepare better academic assignments with Gemini AI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiary,
                                    CircleShape
                                )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "Gemini AI Ready",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Assignment details heading

        Column {

            Text(
                text = "Create Your Assignment",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Enter the details below to generate a structured academic guide.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Details form

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                Text(
                    text = "📋 Assignment Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        errorMessage = ""
                    },
                    label = {
                        Text("Subject Name")
                    },
                    placeholder = {
                        Text("Example: Data Structures")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isGenerating
                )

                OutlinedTextField(
                    value = assignmentTitle,
                    onValueChange = {
                        assignmentTitle = it
                        errorMessage = ""
                    },
                    label = {
                        Text("Assignment Title")
                    },
                    placeholder = {
                        Text("Example: Binary Search Trees")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isGenerating
                )

                AssignmentDropdown(
                    label = "Assignment Type",
                    selected = assignmentType,
                    options = assignmentTypes,
                    onSelected = {
                        assignmentType = it
                    }
                )

                AssignmentDropdown(
                    label = "Academic Level",
                    selected = academicLevel,
                    options = academicLevels,
                    onSelected = {
                        academicLevel = it
                    }
                )

                AssignmentDropdown(
                    label = "Expected Length",
                    selected = wordCount,
                    options = wordCounts,
                    onSelected = {
                        wordCount = it
                    }
                )

                OutlinedTextField(
                    value = instructions,
                    onValueChange = {
                        instructions = it
                        errorMessage = ""
                    },
                    label = {
                        Text("Assignment Questions / Instructions")
                    },
                    placeholder = {
                        Text(
                            "Enter your assignment questions, requirements or topics..."
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp),
                    minLines = 4,
                    maxLines = 6,
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isGenerating
                )

                // Generate button

                Button(
                    onClick = {

                        if (
                            subject.isBlank() ||
                            assignmentTitle.isBlank() ||
                            instructions.isBlank()
                        ) {
                            errorMessage =
                                "Please enter the subject, assignment title and instructions."
                            return@Button
                        }

                        if (isGenerating) return@Button

                        scope.launch {

                            isGenerating = true
                            generatedContent = ""
                            errorMessage = ""
                            saveMessage = ""

                            try {

                                val prompt = """
                                    You are an experienced academic assignment assistant.

                                    Help a student understand and prepare this assignment.

                                    Subject: $subject
                                    Assignment Title: $assignmentTitle
                                    Assignment Type: $assignmentType
                                    Academic Level: $academicLevel
                                    Expected Length: $wordCount

                                    Assignment Instructions:
                                    $instructions

                                    Generate a detailed and well-organized response containing:

                                    1. Introduction to the topic.
                                    2. Explanation of the main concepts.
                                    3. A clear assignment structure with headings.
                                    4. Important points under each heading.
                                    5. Relevant examples.
                                    6. Suggested diagrams, tables or code examples.
                                    7. Conclusion.
                                    8. Suggestions for further reading and research.

                                    Use simple academic language suitable for the
                                    specified academic level.

                                    For programming assignments, include explanations
                                    and illustrative code where appropriate.

                                    Do not invent references, authors, books or URLs.
                                    Clearly distinguish suggested research topics from
                                    verified sources.

                                    Help the student learn and prepare their own work.
                                    Do not claim that the response is a verified submission.
                                """.trimIndent()

                                val response = model.generateContent(prompt)

                                generatedContent = response.text
                                    ?: "The AI did not return any text. Please try again."

                            } catch (e: Exception) {

                                errorMessage = e.localizedMessage
                                    ?: "Unable to generate assignment."

                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(15.dp)
                ) {

                    if (isGenerating) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text("Generating Assignment...")

                    } else {

                        Text(
                            text = "✨ Generate Assignment",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        // Loading message

        if (isGenerating) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(25.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {

                        Text(
                            text = "Gemini is preparing your assignment...",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Text(
                            text = "This may take a few moments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Error message

        if (errorMessage.isNotBlank()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {

                Text(
                    text = "⚠️ $errorMessage",
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Generated assignment

        if (generatedContent.isNotBlank()) {

            Column {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "📘 Generated Assignment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {

                        Text(
                            text = "AI Generated",
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = assignmentTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "$subject • $assignmentType",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider()

                        Text(
                            text = generatedContent,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Save assignment

                        Button(
                            onClick = {

                                val uid = FirebaseAuth.getInstance()
                                    .currentUser?.uid

                                if (uid == null) {
                                    errorMessage =
                                        "Please log in to save your assignment."
                                    return@Button
                                }

                                if (isSaving) return@Button

                                scope.launch {

                                    isSaving = true
                                    saveMessage = ""
                                    errorMessage = ""

                                    try {

                                        FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(uid)
                                            .collection("assignmentHelpers")
                                            .add(
                                                hashMapOf(
                                                    "subject" to subject,
                                                    "assignmentTitle" to assignmentTitle,
                                                    "assignmentType" to assignmentType,
                                                    "academicLevel" to academicLevel,
                                                    "wordCount" to wordCount,
                                                    "instructions" to instructions,
                                                    "generatedContent" to generatedContent,
                                                    "createdAt" to FieldValue.serverTimestamp()
                                                )
                                            )

                                        saveMessage =
                                            "Assignment saved successfully to Firebase!"

                                    } catch (e: Exception) {

                                        errorMessage = e.localizedMessage
                                            ?: "Unable to save assignment."

                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !isSaving,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {

                            if (isSaving) {

                                CircularProgressIndicator(
                                    modifier = Modifier.size(21.dp),
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    strokeWidth = 2.dp
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Text("Saving...")

                            } else {

                                Text(
                                    text = "💾 Save Assignment",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (saveMessage.isNotBlank()) {

                            Text(
                                text = "✓ $saveMessage",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun AssignmentDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedButton(
                onClick = {
                    expanded = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(
                    horizontal = 14.dp,
                    vertical = 12.dp
                )
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = selected,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "▼",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {

                options.forEach { option ->

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontWeight = if (selected == option) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                        trailingIcon = {
                            if (selected == option) {
                                Text(
                                    text = "✓",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
