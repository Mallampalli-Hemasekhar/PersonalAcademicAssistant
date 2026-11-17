
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
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

@Composable
fun AICodeExplainerScreen() {

    val scope = rememberCoroutineScope()

    val model = remember {
        Firebase.ai(
            backend = GenerativeBackend.googleAI(),
            useLimitedUseAppCheckTokens = true
        ).generativeModel("gemini-3.5-flash")
    }

    var programmingLanguage by remember {
        mutableStateOf("Python")
    }

    var explanationType by remember {
        mutableStateOf("Explain line by line")
    }

    var codeInput by remember {
        mutableStateOf("")
    }

    var explanation by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var saveMessage by remember {
        mutableStateOf("")
    }

    val languages = listOf(
        "Python",
        "Java",
        "Kotlin",
        "C",
        "C++",
        "JavaScript",
        "Other"
    )

    val explanationTypes = listOf(
        "Explain line by line",
        "Summarize code",
        "Find and fix errors",
        "Explain time and space complexity",
        "Optimize the code"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
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
                            text = "💻",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "AI Code Explainer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Understand code, fix errors and improve your programming skills.",
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

        Spacer(modifier = Modifier.height(22.dp))

        // Programming language

        Text(
            text = "Programming Language",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        CodeOptionDropdown(
            selectedOption = programmingLanguage,
            options = languages,
            onOptionSelected = {
                programmingLanguage = it
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Explanation type

        Text(
            text = "What do you want AI to do?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        CodeOptionDropdown(
            selectedOption = explanationType,
            options = explanationTypes,
            onOptionSelected = {
                explanationType = it
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Code input

        Text(
            text = "Paste Your Code",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Add your code below and let AI help you understand it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = codeInput,
            onValueChange = {
                codeInput = it
                errorMessage = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            placeholder = {
                Text("Paste your programming code here...")
            },
            minLines = 8,
            maxLines = 12,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Analyze button

        Button(
            onClick = {

                if (codeInput.isBlank()) {
                    errorMessage = "Please enter some code first."
                    return@Button
                }

                if (isLoading) {
                    return@Button
                }

                isLoading = true
                errorMessage = ""
                explanation = ""
                saveMessage = ""

                val prompt = """
                    You are an expert programming teacher.

                    Programming language: $programmingLanguage

                    Student request: $explanationType

                    Explain the following code in simple language
                    suitable for a beginner student.

                    Follow these instructions:
                    1. Be accurate and easy to understand.
                    2. Use clear headings and examples where useful.
                    3. Explain technical terms simply.
                    4. If the code contains errors, identify them clearly.
                    5. If explaining complexity, discuss time and
                       space complexity separately.
                    6. If suggesting optimized code, explain the
                       improvements.
                    7. Do not invent code that is not present.

                    Student's code:

                    $codeInput
                """.trimIndent()

                scope.launch {

                    try {

                        val response = model.generateContent(prompt)
                        val answer = response.text

                        if (answer.isNullOrBlank()) {

                            errorMessage =
                                "The AI returned an empty response. Please try again."

                        } else {

                            explanation = answer

                            val uid = Firebase.auth.currentUser?.uid

                            if (uid != null) {

                                val doubtData = hashMapOf(
                                    "language" to programmingLanguage,
                                    "requestType" to explanationType,
                                    "code" to codeInput,
                                    "explanation" to answer,
                                    "createdAt" to FieldValue.serverTimestamp()
                                )

                                Firebase.firestore
                                    .collection("users")
                                    .document(uid)
                                    .collection("codeExplanations")
                                    .add(doubtData)
                                    .addOnSuccessListener {
                                        saveMessage =
                                            "Explanation saved successfully."
                                    }
                                    .addOnFailureListener {
                                        saveMessage =
                                            "AI response received, but saving failed: ${it.message}"
                                    }

                            } else {

                                saveMessage =
                                    "Sign in to save your explanations."
                            }
                        }

                    } catch (e: Exception) {

                        errorMessage =
                            e.localizedMessage
                                ?: "Something went wrong. Please try again."

                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = !isLoading && codeInput.isNotBlank(),
            shape = RoundedCornerShape(16.dp)
        ) {

            Text(
                text = if (isLoading) {
                    "Analyzing Code..."
                } else {
                    "✨ Explain My Code"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Loading indicator

        if (isLoading) {

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {

                        Text(
                            text = "Gemini is analyzing your code...",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Please wait while your explanation is prepared.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Error message

        if (errorMessage.isNotBlank()) {

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {

                Text(
                    text = "⚠️ $errorMessage",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // AI explanation

        if (explanation.isNotBlank()) {

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "✨ AI Explanation",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {

                    Text(
                        text = "Generated",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Language: $programmingLanguage",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Task: $explanationType",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp)
                    )

                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (saveMessage.isNotBlank()) {

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {

                    Text(
                        text = "✓ $saveMessage",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CodeOptionDropdown(
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {

        OutlinedButton(
            onClick = {
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = selectedOption,
                    modifier = Modifier.weight(1f),
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
                            fontWeight = if (selectedOption == option) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    trailingIcon = {
                        if (selectedOption == option) {
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
