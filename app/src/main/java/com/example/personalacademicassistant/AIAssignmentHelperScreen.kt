
package com.example.personalacademicassistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val blue = Color(0xFF1565C0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "🤖 AI Assignment Helper",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = blue
        )

        Text(
            text = "Create structured assignment plans and understand academic topics with Gemini AI.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "Assignment Details",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        errorMessage = ""
                    },
                    label = { Text("Subject Name") },
                    placeholder = {
                        Text("Example: Data Structures")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = assignmentTitle,
                    onValueChange = {
                        assignmentTitle = it
                        errorMessage = ""
                    },
                    label = { Text("Assignment Title") },
                    placeholder = {
                        Text("Example: Binary Search Trees")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                AssignmentDropdown(
                    label = "Assignment Type",
                    selected = assignmentType,
                    options = listOf(
                        "Programming Assignment",
                        "Theory Assignment",
                        "Research Assignment",
                        "Case Study",
                        "Project Report",
                        "Lab Assignment"
                    ),
                    onSelected = {
                        assignmentType = it
                    }
                )

                AssignmentDropdown(
                    label = "Academic Level",
                    selected = academicLevel,
                    options = listOf(
                        "B.Tech",
                        "BCA",
                        "B.Sc",
                        "M.Tech",
                        "MCA",
                        "Other"
                    ),
                    onSelected = {
                        academicLevel = it
                    }
                )

                AssignmentDropdown(
                    label = "Expected Length",
                    selected = wordCount,
                    options = listOf(
                        "500 words",
                        "1000 words",
                        "1500 words",
                        "2000 words",
                        "2500 words"
                    ),
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
                        .height(150.dp),
                    minLines = 4
                )

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
                        .height(52.dp),
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = blue
                    )
                ) {

                    if (isGenerating) {

                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )

                        Spacer(modifier = Modifier.padding(4.dp))

                        Text("Generating...")

                    } else {

                        Text(
                            text = "✨ Generate Assignment",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        if (errorMessage.isNotBlank()) {

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }

        if (generatedContent.isNotBlank()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "📘 Generated Assignment",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = blue
                    )

                    Text(
                        text = generatedContent,
                        fontSize = 14.sp,
                        lineHeight = 23.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = {

                            val uid = FirebaseAuth.getInstance()
                                .currentUser?.uid

                            if (uid == null) {
                                errorMessage =
                                    "Please log in to save your assignment."
                                return@Button
                            }

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
                            .height(50.dp),
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        )
                    ) {

                        if (isSaving) {

                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )

                            Spacer(modifier = Modifier.padding(4.dp))

                            Text("Saving...")

                        } else {

                            Text("💾 Save Assignment")
                        }
                    }

                    if (saveMessage.isNotBlank()) {

                        Text(
                            text = saveMessage,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
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
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedButton(
                onClick = {
                    expanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(selected)
                    Text("▼")
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {

                options.forEach { option ->

                    DropdownMenuItem(
                        text = {
                            Text(option)
                        },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
