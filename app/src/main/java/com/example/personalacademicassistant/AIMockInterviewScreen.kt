
package com.example.personalacademicassistant

import android.util.Log
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
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
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
    var selectedQuestion by remember { mutableIntStateOf(0) }

    var candidateAnswer by remember { mutableStateOf("") }
    var evaluation by remember { mutableStateOf("") }

    var isGenerating by remember { mutableStateOf(false) }
    var isEvaluating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    val colors = MaterialTheme.colorScheme

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

        if (isGenerating || isEvaluating || isSaving) return

        errorMessage = ""
        successMessage = ""
        evaluation = ""
        candidateAnswer = ""
        questions = emptyList()
        selectedQuestion = 0

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

                successMessage =
                    "Five interview questions generated successfully!"

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

        if (isEvaluating || isGenerating || isSaving) return

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

        if (isSaving || isGenerating || isEvaluating) return

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
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = colors.primary
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎤",
                            fontSize = 29.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "AI Mock Interview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Practice real interview questions and improve your confidence with Gemini AI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(9.dp))

                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = colors.primary.copy(alpha = 0.15f)
                    ) {

                        Text(
                            text = "✨ AI Powered Practice",
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Interview progress summary

        if (questions.isNotEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.secondaryContainer
                )
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Your Interview Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSecondaryContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        InterviewStat(
                            value = "${questions.size}",
                            label = "Questions"
                        )

                        InterviewStat(
                            value = "${selectedQuestion + 1}/5",
                            label = "Current"
                        )

                        InterviewStat(
                            value = if (evaluation.isNotBlank()) "Done" else "Pending",
                            label = "Evaluation"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = {
                            (selectedQuestion + 1) / 5f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp),
                        color = colors.primary,
                        trackColor = colors.surfaceVariant
                    )
                }
            }
        }

        // Setup heading

        Column {

            Text(
                text = "Prepare Your Interview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Choose your role, interview type and difficulty.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        // Setup card

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            ),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                Text(
                    text = "⚙️ Interview Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

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
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isGenerating && !isEvaluating && !isSaving
                )

                InterviewDropdown(
                    label = "Interview Type",
                    selectedValue = interviewType,
                    options = listOf(
                        "Technical",
                        "HR",
                        "Behavioral",
                        "Mixed"
                    ),
                    enabled = !isGenerating && !isEvaluating && !isSaving,
                    onSelect = {
                        interviewType = it
                    }
                )

                InterviewDropdown(
                    label = "Difficulty Level",
                    selectedValue = difficulty,
                    options = listOf(
                        "Beginner",
                        "Intermediate",
                        "Advanced"
                    ),
                    enabled = !isGenerating && !isEvaluating && !isSaving,
                    onSelect = {
                        difficulty = it
                    }
                )

                Button(
                    onClick = {
                        generateQuestions()
                    },
                    enabled = !isGenerating &&
                            !isEvaluating &&
                            !isSaving &&
                            jobRole.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {

                    if (isGenerating) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = colors.onPrimary,
                            strokeWidth = 2.dp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text("Generating Questions...")

                    } else {

                        Text(
                            text = "✨ Generate Interview Questions",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (isGenerating) {
            InterviewLoadingCard(
                message = "Gemini is preparing your five interview questions..."
            )
        }

        // Generated questions

        if (questions.isNotEmpty()) {

            Column {

                Text(
                    text = "Your Interview Questions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Select a question to write and evaluate your answer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            questions.forEachIndexed { index, question ->

                val isSelected = selectedQuestion == index

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            colors.primaryContainer
                        } else {
                            colors.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) 3.dp else 1.dp
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = if (isSelected) {
                                colors.primary
                            } else {
                                colors.surfaceVariant
                            }
                        ) {

                            Box(
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    text = "${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        colors.onPrimary
                                    } else {
                                        colors.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Question ${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = question,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isSelected) {

                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = colors.primary
                            ) {

                                Text(
                                    text = "Selected",
                                    modifier = Modifier.padding(
                                        horizontal = 9.dp,
                                        vertical = 6.dp
                                    ),
                                    color = colors.onPrimary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                        } else {

                            TextButton(
                                onClick = {
                                    selectedQuestion = index
                                    candidateAnswer = ""
                                    evaluation = ""
                                    errorMessage = ""
                                    successMessage = ""
                                },
                                enabled = !isEvaluating &&
                                        !isSaving &&
                                        !isGenerating
                            ) {
                                Text("Answer")
                            }
                        }
                    }
                }
            }

            // Answer section

            Column {

                Text(
                    text = "Write Your Answer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Take your time and answer as if you were in a real interview.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "Question ${selectedQuestion + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )

                    Text(
                        text = questions[selectedQuestion],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )

                    HorizontalDivider()

                    OutlinedTextField(
                        value = candidateAnswer,
                        onValueChange = {
                            candidateAnswer = it
                            evaluation = ""
                            successMessage = ""
                            errorMessage = ""
                        },
                        label = {
                            Text("Your Answer")
                        },
                        placeholder = {
                            Text("Write your answer here...")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(165.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isEvaluating &&
                                !isGenerating &&
                                !isSaving
                    )

                    Text(
                        text = "Tip: Explain your reasoning and include examples where possible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            evaluateAnswer()
                        },
                        enabled = !isEvaluating &&
                                !isGenerating &&
                                !isSaving &&
                                candidateAnswer.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {

                        if (isEvaluating) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(21.dp),
                                color = colors.onPrimary,
                                strokeWidth = 2.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text("Evaluating...")

                        } else {

                            Text(
                                text = "✨ Evaluate My Answer",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (isEvaluating) {

                InterviewLoadingCard(
                    message = "Gemini is evaluating your answer..."
                )
            }

            // Evaluation result

            if (evaluation.isNotBlank()) {

                Column {

                    Text(
                        text = "Your Interview Evaluation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Review your feedback and identify areas to improve.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface
                    ),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(17.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = RoundedCornerShape(13.dp),
                                color = colors.secondaryContainer
                            ) {

                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "📊",
                                        fontSize = 22.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(11.dp))

                            Column {

                                Text(
                                    text = "AI Feedback Report",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Powered by Gemini",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = evaluation,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp,
                            color = colors.onSurface
                        )

                        Button(
                            onClick = {
                                saveInterview()
                            },
                            enabled = !isSaving &&
                                    !isGenerating &&
                                    !isEvaluating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.tertiary
                            )
                        ) {

                            if (isSaving) {

                                CircularProgressIndicator(
                                    modifier = Modifier.size(21.dp),
                                    color = colors.onTertiary,
                                    strokeWidth = 2.dp
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Text("Saving Interview...")

                            } else {

                                Text(
                                    text = "💾 Save Interview to Firebase",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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
                    containerColor = colors.errorContainer
                )
            ) {

                Text(
                    text = "⚠️ $errorMessage",
                    modifier = Modifier.padding(14.dp),
                    color = colors.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Success message

        if (successMessage.isNotBlank()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.secondaryContainer
                )
            ) {

                Text(
                    text = "✓ $successMessage",
                    modifier = Modifier.padding(14.dp),
                    color = colors.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun InterviewStat(
    value: String,
    label: String
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun InterviewLoadingCard(
    message: String
) {

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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            CircularProgressIndicator(
                modifier = Modifier.size(25.dp),
                strokeWidth = 2.dp
            )

            Spacer(modifier = Modifier.width(13.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InterviewDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    enabled: Boolean = true,
    onSelect: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Column(
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
                enabled = enabled,
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
                        text = selectedValue,
                        color = MaterialTheme.colorScheme.onSurface
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
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.surface
                )
            ) {

                options.forEach { option ->

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontWeight = if (selectedValue == option) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                        trailingIcon = {
                            if (selectedValue == option) {
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
