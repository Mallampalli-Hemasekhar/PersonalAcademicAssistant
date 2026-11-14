
package com.example.personalacademicassistant

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .background(Color(0xFFF5F7FB))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = "AI Code Explainer",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Understand programming code with the help of AI.",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Programming Language",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        CodeOptionDropdown(
            selectedOption = programmingLanguage,
            options = languages,
            onOptionSelected = {
                programmingLanguage = it
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "What do you want AI to do?",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        CodeOptionDropdown(
            selectedOption = explanationType,
            options = explanationTypes,
            onOptionSelected = {
                explanationType = it
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Paste Your Code",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = codeInput,
            onValueChange = {
                codeInput = it
                errorMessage = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            placeholder = {
                Text("Paste your programming code here...")
            },
            minLines = 8,
            maxLines = 12
        )

        Spacer(modifier = Modifier.height(18.dp))

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
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5E35B1)
            )
        ) {
            Text(
                text = if (isLoading) {
                    "Analyzing Code..."
                } else {
                    "✨ Explain My Code"
                },
                fontSize = 16.sp
            )
        }

        if (isLoading) {

            Spacer(modifier = Modifier.height(20.dp))

            CircularProgressIndicator(
                modifier = Modifier.padding(8.dp),
                color = Color(0xFF5E35B1)
            )

            Text(
                text = "Gemini is analyzing your code...",
                color = Color.Gray
            )
        }

        if (errorMessage.isNotBlank()) {

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFC62828),
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        if (explanation.isNotBlank()) {

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AI Explanation",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172554)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 3.dp
                )
            ) {
                Text(
                    text = explanation,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    color = Color(0xFF263238),
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (saveMessage.isNotBlank()) {

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = saveMessage,
                    color = if (saveMessage.startsWith("Explanation saved")) {
                        Color(0xFF2E7D32)
                    } else {
                        Color(0xFFEF6C00)
                    },
                    fontSize = 13.sp
                )
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

        Button(
            onClick = {
                expanded = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF263238)
            )
        ) {
            Text(
                text = "$selectedOption  ▼",
                modifier = Modifier.fillMaxWidth()
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier.background(Color.White)
        ) {

            options.forEach { option ->

                DropdownMenuItem(
                    text = {
                        Text(option)
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
