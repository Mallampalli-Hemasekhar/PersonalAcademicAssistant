
package com.example.personalacademicassistant

import android.util.Log
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.ai
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val MOCK_INTERVIEW_TAG = "AIMockInterview"
private const val GEMINI_MODEL_NAME = "gemini-3.8-flash"

@Composable
fun AIMockInterviewScreen() {

    val scope = rememberCoroutineScope()

    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    val model = remember {
        Firebase.ai(
            backend = GenerativeBackend.googleAI(),
            useLimitedUseAppCheckTokens = true
        ).generativeModel(GEMINI_MODEL_NAME)
    }

    var jobRole by remember { mutableStateOf("Android Developer") }
    var interviewType by remember { mutableStateOf("Technical") }
    var difficulty by remember { mutableStateOf("Intermediate") }

    var questions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedQuestion by remember { mutableStateOf(0) }

    var candidateAnswer by remember { mutableStateOf("") }
    var evaluation by remember { mutableStateOf("") }

    var isGenerating by remember { mutableStateOf(false) }
    var isEvaluating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    val primaryColor = Color(0xFF3949AB)

    fun readableError(error: Exception): String {
        val message = error.message.orEmpty()
        val details = message.lowercase()

        Log.e(MOCK_INTERVIEW_TAG, "Gemini request failed", error)

        return when {
            "429" in details ||
                    "resource_exhausted" in details ||
                    "quota" in details ->
                "Gemini API quota exceeded. Please check your Firebase AI Logic usage and quota, then try again later."

            "503" in details ||
                    "unavailable" in details ||
                    "high demand" in details ->
                "Gemini is currently experiencing high demand. Please try again later."

            "403" in details ||
                    "permission_denied" in details ->
                "Permission denied. Please check Firebase AI Logic and App Check configuration."

            "404" in details ||
                    "not found" in details ->
                "The selected Gemini model or API endpoint was not found. Please verify your model configuration."

            "401" in details ||
                    "unauthenticated" in details ->
                "Authentication failed. Please check your Firebase configuration and login."

            "timeout" in details ->
                "The AI request took too long. Please try again."

            else ->
                "Something went wrong: ${message.ifBlank { "Unknown error" }}"
        }
    }

    fun generateQuestions() {

        if (isGenerating) return

        errorMessage = ""
        successMessage = ""
        evaluation = ""
        candidateAnswer = ""
        questions = emptyList()

        isGenerating = true

        scope.launch {
            try {

                val prompt = """
                    You are an expert technical interviewer.

                    Create exactly FIVE realistic interview questions.

                    Job Role: $jobRole
                    Interview Type: $interviewType
                    Difficulty: $difficulty

                    Requirements:
                    1. Questions must match the selected job role.
                    2. Questions must match the selected interview type.
                    3. Follow the selected difficulty level.
                    4. Avoid duplicate questions.
                    5. Questions must be useful for a real interview.
                    6. Keep each question clear and understandable.

                    IMPORTANT:
                    Return exactly five questions.
                    Put each question on a separate line.
                    Number them from 1 to 5.
                    Do not include introductions, answers or explanations.
                """.trimIndent()

                val response = withTimeout(90_000L) {
                    model.generateContent(prompt)
                }

                val generatedText = response.text
                    ?: throw IllegalStateException(
                        "Gemini returned an empty response."
                    )

                val parsedQuestions = generatedText
                    .lines()
                    .map { line ->
                        line.trim()
                            .replace(Regex("^\\s*\\d+[.)\\-:]\\s*"), "")
                            .trim()
                    }
                    .filter { it.isNotBlank() }
                    .take(5)

                if (parsedQuestions.size != 5) {
                    throw IllegalStateException(
                        "Gemini did not return exactly five questions. Please generate again."
                    )
                }

                questions = parsedQuestions
                selectedQuestion = 0

                successMessage = "Five interview questions generated successfully!"

                Log.d(
                    MOCK_INTERVIEW_TAG,
                    "Successfully generated five questions."
                )

            } catch (e: TimeoutCancellationException) {

                Log.e(
                    MOCK_INTERVIEW_TAG,
                    "Question generation timed out",
                    e
                )

                errorMessage =
                    "The AI request took longer than 90 seconds. Please try again."

            } catch (e: Exception) {

                errorMessage =
                    "Question generation failed: ${readableError(e)}"

            } finally {
                isGenerating = false
            }
        }
    }

    fun evaluateAnswer() {

        if (isEvaluating) return

        if (questions.isEmpty()) {
            errorMessage = "Generate interview questions first."
            return
        }

        if (candidateAnswer.isBlank()) {
            errorMessage = "Please enter your answer before evaluation."
            return
        }

        errorMessage = ""
        successMessage = ""
        evaluation = ""
        isEvaluating = true

        scope.launch {
            try {

                val question = questions[selectedQuestion]

                val prompt = """
                    You are an experienced professional interviewer.

                    Evaluate the following candidate answer.

                    Job Role: $jobRole
                    Interview Type: $interviewType
                    Difficulty: $difficulty

                    Interview Question:
                    $question

                    Candidate Answer:
                    $candidateAnswer

                    Evaluate the answer based on:
                    1. Technical or subject accuracy.
                    2. Understanding of the concept.
                    3. Completeness of the answer.
                    4. Communication and clarity.

                    Return your feedback using this exact structure:

                    SCORE: X/10

                    OVERALL FEEDBACK:
                    Explain the quality of the answer.

                    STRENGTHS:
                    Mention what the candidate did well.

                    AREAS TO IMPROVE:
                    Explain what is missing or incorrect.

                    IDEAL ANSWER:
                    Give a clear example of a strong answer.

                    Be fair, constructive and beginner-friendly.
                    Do not give a perfect score unless the answer deserves it.
                """.trimIndent()

                val response = withTimeout(90_000L) {
                    model.generateContent(prompt)
                }

                evaluation = response.text
                    ?: throw IllegalStateException(
                        "Gemini returned an empty evaluation."
                    )

                Log.d(
                    MOCK_INTERVIEW_TAG,
                    "Answer evaluation completed."
                )

                successMessage = "Your answer has been evaluated!"

            } catch (e: TimeoutCancellationException) {

                Log.e(
                    MOCK_INTERVIEW_TAG,
                    "Answer evaluation timed out",
                    e
                )

                errorMessage =
                    "Evaluation took longer than 90 seconds. Please try again."

            } catch (e: Exception) {

                errorMessage =
                    "Answer evaluation failed: ${readableError(e)}"

            } finally {
                isEvaluating = false
            }
        }
    }

    fun saveInterview() {

        if (isSaving) return

        val user = auth.currentUser

        if (user == null) {
            errorMessage = "Please log in before saving your interview."
            return
        }

        if (questions.isEmpty() || evaluation.isBlank()) {
            errorMessage =
                "Generate questions and evaluate an answer before saving."
            return
        }

        errorMessage = ""
        successMessage = ""
        isSaving = true

        val interviewData = hashMapOf(
            "jobRole" to jobRole,
            "interviewType" to interviewType,
            "difficulty" to difficulty,
            "questions" to questions,
            "selectedQuestion" to questions[selectedQuestion],
            "candidateAnswer" to candidateAnswer,
            "evaluation" to evaluation,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(user.uid)
            .collection("mockInterviews")
            .add(interviewData)
            .addOnSuccessListener {

                Log.d(
                    MOCK_INTERVIEW_TAG,
                    "Interview saved: ${it.id}"
                )

                successMessage = "Interview saved successfully!"

                isSaving = false
            }
            .addOnFailureListener { exception ->

                Log.e(
                    MOCK_INTERVIEW_TAG,
                    "Failed to save interview",
                    exception
                )

                errorMessage =
                    "Could not save interview: ${exception.message}"

                isSaving = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = "🎤 AI Mock Interview",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Practice interviews with Gemini AI and improve your confidence.",
            fontSize = 14.sp,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE4E5F0)
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Interview Setup",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = jobRole,
                    onValueChange = { jobRole = it },
                    label = { Text("Job Role / Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                InterviewDropdown(
                    label = "Interview Type",
                    selectedValue = interviewType,
                    options = listOf(
                        "Technical",
                        "HR",
                        "Behavioral",
                        "Mixed"
                    ),
                    onSelect = { interviewType = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                InterviewDropdown(
                    label = "Difficulty Level",
                    selectedValue = difficulty,
                    options = listOf(
                        "Beginner",
                        "Intermediate",
                        "Advanced"
                    ),
                    onSelect = { difficulty = it }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { generateQuestions() },
                    enabled = !isGenerating &&
                            !isEvaluating &&
                            !isSaving &&
                            jobRole.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    )
                ) {

                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("✨ Generate Interview Questions")
                    }
                }
            }
        }

        if (isGenerating) {
            LoadingMessage("Generating your five interview questions...")
        }

        if (questions.isNotEmpty()) {

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Your Interview Questions",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            questions.forEachIndexed { index, question ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedQuestion == index)
                            Color(0xFFE0E5FF)
                        else
                            Color.White
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Question ${index + 1}",
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(text = question)
                        }

                        TextButton(
                            onClick = {
                                selectedQuestion = index
                                candidateAnswer = ""
                                evaluation = ""
                                errorMessage = ""
                                successMessage = ""
                            }
                        ) {
                            Text(
                                if (selectedQuestion == index)
                                    "Selected"
                                else
                                    "Answer"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "Your Answer",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = questions[selectedQuestion],
                fontSize = 14.sp,
                color = primaryColor,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = candidateAnswer,
                onValueChange = {
                    candidateAnswer = it
                    evaluation = ""
                    successMessage = ""
                    errorMessage = ""
                },
                label = { Text("Write your answer here") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { evaluateAnswer() },
                enabled = !isEvaluating &&
                        !isGenerating &&
                        !isSaving &&
                        candidateAnswer.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                )
            ) {

                if (isEvaluating) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Evaluate My Answer")
                }
            }

            if (isEvaluating) {
                LoadingMessage("Gemini is evaluating your answer...")
            }

            if (evaluation.isNotBlank()) {

                Spacer(modifier = Modifier.height(18.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "📊 Interview Evaluation",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = evaluation,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { saveInterview() },
                            enabled = !isSaving &&
                                    !isGenerating &&
                                    !isEvaluating,
                            modifier = Modifier.fillMaxWidth(),
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
                            } else {
                                Text("Save Interview to Firebase")
                            }
                        }
                    }
                }
            }
        }

        if (errorMessage.isNotBlank()) {

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = errorMessage,
                color = Color(0xFFD32F2F),
                fontSize = 14.sp
            )
        }

        if (successMessage.isNotBlank()) {

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = successMessage,
                color = Color(0xFF2E7D32),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(25.dp))
    }
}

@Composable
private fun LoadingMessage(message: String) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        CircularProgressIndicator(
            modifier = Modifier.height(22.dp),
            strokeWidth = 2.dp
        )

        Spacer(modifier = Modifier.padding(6.dp))

        Text(
            text = message,
            fontSize = 13.sp,
            color = Color.DarkGray
        )
    }
}

@Composable
private fun InterviewDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    Column {

        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box {

            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {

                Text(
                    text = selectedValue,
                    modifier = Modifier.weight(1f)
                )

                Text("▼")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {

                options.forEach { option ->

                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
