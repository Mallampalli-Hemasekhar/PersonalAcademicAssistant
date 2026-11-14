
package com.example.personalacademicassistant

import androidx.compose.foundation.layout.Arrangement
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun AIMockInterviewScreen() {

    val scope = rememberCoroutineScope()

    val model = remember {
        Firebase.ai(
            backend = GenerativeBackend.googleAI(),
            useLimitedUseAppCheckTokens = true
        ).generativeModel("gemini-3.8-flash")
    }

    var jobRole by remember { mutableStateOf("") }
    var interviewType by remember { mutableStateOf("Technical") }
    var difficulty by remember { mutableStateOf("Intermediate") }

    var questions by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var evaluation by remember { mutableStateOf("") }

    var isGenerating by remember { mutableStateOf(false) }
    var isEvaluating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf("") }

    val blue = Color(0xFF3949AB)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "🎤 AI Mock Interview",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = blue
        )

        Text(
            text = "Practice interviews with Gemini AI and improve your confidence.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // INTERVIEW SETUP

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
                    text = "Interview Setup",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = jobRole,
                    onValueChange = {
                        jobRole = it
                        errorMessage = ""
                    },
                    label = {
                        Text("Job Role / Subject")
                    },
                    placeholder = {
                        Text("Example: Android Developer")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                InterviewDropdown(
                    label = "Interview Type",
                    selected = interviewType,
                    options = listOf(
                        "Technical",
                        "HR",
                        "Mixed"
                    ),
                    onSelected = {
                        interviewType = it
                    }
                )

                InterviewDropdown(
                    label = "Difficulty Level",
                    selected = difficulty,
                    options = listOf(
                        "Beginner",
                        "Intermediate",
                        "Advanced"
                    ),
                    onSelected = {
                        difficulty = it
                    }
                )

                Button(
                    onClick = {

                        if (jobRole.isBlank()) {
                            errorMessage =
                                "Please enter a job role or subject."
                            return@Button
                        }

                        scope.launch {

                            isGenerating = true
                            questions = ""
                            answer = ""
                            evaluation = ""
                            errorMessage = ""
                            saveMessage = ""

                            try {

                                val prompt = """
                                    Act as a professional interviewer.

                                    Generate exactly 5 realistic interview questions.

                                    Job Role or Subject: $jobRole
                                    Interview Type: $interviewType
                                    Difficulty: $difficulty

                                    Requirements:
                                    1. Generate exactly 5 questions.
                                    2. Make questions relevant to the selected role.
                                    3. Use clear and understandable language.
                                    4. Include practical and conceptual questions
                                       when appropriate.
                                    5. Number every question.
                                    6. Do not provide answers yet.

                                    Return only the interview questions.
                                """.trimIndent()

                                val response = model.generateContent(prompt)

                                questions = response.text
                                    ?: "No questions were generated. Please try again."

                                if (questions.isBlank()) {
                                    questions =
                                        "The AI returned an empty response. Please try again."
                                }

                            } catch (e: Exception) {

                                errorMessage =
                                    "Question generation failed: ${
                                        e.localizedMessage
                                            ?: "Unknown error"
                                    }"

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

                        Text("Generating Questions...")

                    } else {
                        Text("✨ Generate Interview Questions")
                    }
                }
            }
        }

        // GENERATED QUESTIONS AND ANSWER

        if (questions.isNotBlank()) {

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
                        text = "📋 Interview Questions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = blue
                    )

                    Text(
                        text = questions,
                        fontSize = 15.sp,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your Answer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = answer,
                        onValueChange = {
                            answer = it
                            evaluation = ""
                            saveMessage = ""
                            errorMessage = ""
                        },
                        label = {
                            Text("Write your interview answer")
                        },
                        placeholder = {
                            Text("Type your answer here...")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        minLines = 4
                    )

                    Button(
                        onClick = {

                            if (answer.isBlank()) {
                                errorMessage =
                                    "Please enter your answer first."
                                return@Button
                            }

                            scope.launch {

                                isEvaluating = true
                                evaluation = ""
                                errorMessage = ""
                                saveMessage = ""

                                try {

                                    val prompt = """
                                        Act as a professional interview evaluator.

                                        Job Role: $jobRole
                                        Interview Type: $interviewType
                                        Difficulty: $difficulty

                                        Interview Questions:
                                        $questions

                                        Candidate Answer:
                                        $answer

                                        Evaluate the candidate's answer.

                                        Include:
                                        1. Overall feedback.
                                        2. Accuracy and relevance.
                                        3. Strengths.
                                        4. Areas for improvement.
                                        5. A better sample answer.
                                        6. A score out of 10.

                                        Be constructive and encouraging.
                                        Explain the reason for the score.
                                        Do not give an arbitrary high score.
                                    """.trimIndent()

                                    val response =
                                        model.generateContent(prompt)

                                    evaluation = response.text
                                        ?: "No evaluation returned. Please try again."

                                    if (evaluation.isBlank()) {
                                        evaluation =
                                            "The AI returned an empty evaluation. Please try again."
                                    }

                                } catch (e: Exception) {

                                    errorMessage =
                                        "Answer evaluation failed: ${
                                            e.localizedMessage
                                                ?: "Unknown error"
                                        }"

                                } finally {
                                    isEvaluating = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isEvaluating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00897B)
                        )
                    ) {

                        if (isEvaluating) {

                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )

                            Spacer(modifier = Modifier.padding(4.dp))

                            Text("Evaluating...")

                        } else {
                            Text("🎯 Evaluate My Answer")
                        }
                    }
                }
            }
        }

        // AI EVALUATION

        if (evaluation.isNotBlank()) {

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
                        text = "📊 AI Interview Feedback",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = blue
                    )

                    Text(
                        text = evaluation,
                        fontSize = 14.sp,
                        lineHeight = 23.sp
                    )

                    // SAVE INTERVIEW SESSION

                    Button(
                        onClick = {

                            val uid = FirebaseAuth.getInstance()
                                .currentUser?.uid

                            if (uid == null) {
                                errorMessage =
                                    "Please log in to save your interview."
                                return@Button
                            }

                            scope.launch {

                                isSaving = true
                                errorMessage = ""
                                saveMessage = ""

                                try {

                                    FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(uid)
                                        .collection("mockInterviews")
                                        .add(
                                            hashMapOf(
                                                "jobRole" to jobRole,
                                                "interviewType" to interviewType,
                                                "difficulty" to difficulty,
                                                "questions" to questions,
                                                "answer" to answer,
                                                "evaluation" to evaluation,
                                                "createdAt" to
                                                        FieldValue.serverTimestamp()
                                            )
                                        )

                                    saveMessage =
                                        "Interview session saved successfully!"

                                } catch (e: Exception) {

                                    errorMessage =
                                        "Saving failed: ${
                                            e.localizedMessage
                                                ?: "Unknown error"
                                        }"

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
                            Text("💾 Save Interview Session")
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

        // ERROR MESSAGE

        if (errorMessage.isNotBlank()) {

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

// REUSABLE DROPDOWN

@Composable
fun InterviewDropdown(
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
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

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
