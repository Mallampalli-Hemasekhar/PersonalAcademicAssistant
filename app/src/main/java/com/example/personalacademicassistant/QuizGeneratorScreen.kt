
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.BorderStroke
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import org.json.JSONObject

private data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String
)

@Composable
fun QuizGeneratorScreen() {

    var subject by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Medium") }

    var questions by remember {
        mutableStateOf<List<QuizQuestion>>(emptyList())
    }

    var currentQuestion by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }

    var isLoading by remember { mutableStateOf(false) }
    var showExplanation by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var hasSavedResult by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    val model = remember {
        Firebase.ai(
            backend = GenerativeBackend.googleAI(),
            useLimitedUseAppCheckTokens = true
        ).generativeModel("gemini-3.5-flash")
    }

    fun generateQuiz() {

        if (subject.isBlank() || topic.isBlank()) {
            errorMessage = "Please enter both subject and topic."
            return
        }

        if (isLoading) return

        isLoading = true
        errorMessage = ""
        questions = emptyList()
        currentQuestion = 0
        selectedAnswer = -1
        score = 0
        showExplanation = false
        showResult = false
        hasSavedResult = false

        scope.launch {
            try {

                val prompt = """
                    Generate exactly 5 multiple-choice questions.

                    Subject: $subject
                    Topic: $topic
                    Difficulty: $difficulty

                    Return ONLY valid JSON in this format:

                    {
                      "questions": [
                        {
                          "question": "Question text",
                          "options": [
                            "Option A",
                            "Option B",
                            "Option C",
                            "Option D"
                          ],
                          "answerIndex": 0,
                          "explanation": "Explanation"
                        }
                      ]
                    }

                    Rules:
                    1. Generate exactly 5 questions.
                    2. Every question must have exactly 4 options.
                    3. answerIndex must be between 0 and 3.
                    4. Explanations must be educational.
                    5. Do not include markdown outside JSON.
                """.trimIndent()

                val response = model.generateContent(prompt)

                val responseText = response.text
                    ?: throw Exception("Gemini returned an empty response.")

                val cleanResponse = responseText
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val json = JSONObject(cleanResponse)
                val array = json.getJSONArray("questions")

                val generatedQuestions = mutableListOf<QuizQuestion>()

                for (i in 0 until array.length()) {

                    val item = array.getJSONObject(i)
                    val optionsJson = item.getJSONArray("options")

                    val options = (0 until optionsJson.length()).map {
                        optionsJson.getString(it)
                    }

                    val answerIndex = item.getInt("answerIndex")

                    if (options.size != 4 || answerIndex !in 0..3) {
                        throw Exception("Invalid question received from Gemini.")
                    }

                    generatedQuestions.add(
                        QuizQuestion(
                            question = item.getString("question"),
                            options = options,
                            answerIndex = answerIndex,
                            explanation = item.getString("explanation")
                        )
                    )
                }

                if (generatedQuestions.size != 5) {
                    throw Exception("Gemini did not generate exactly 5 questions.")
                }

                questions = generatedQuestions

            } catch (e: Exception) {

                errorMessage =
                    "Quiz generation failed: ${e.localizedMessage ?: "Unknown error"}"

            } finally {
                isLoading = false
            }
        }
    }

    // Save quiz result to Firestore

    LaunchedEffect(showResult) {

        if (showResult && !hasSavedResult) {

            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null && questions.isNotEmpty()) {

                hasSavedResult = true

                val quizData = hashMapOf(
                    "subject" to subject,
                    "topic" to topic,
                    "difficulty" to difficulty,
                    "totalQuestions" to questions.size,
                    "score" to score,
                    "percentage" to (score * 100 / questions.size),
                    "timestamp" to System.currentTimeMillis()
                )

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("quizHistory")
                    .add(quizData)
                    .addOnFailureListener {
                        errorMessage =
                            "Quiz completed, but history could not be saved: ${it.localizedMessage}"
                        hasSavedResult = false
                    }

            } else if (userId == null) {
                errorMessage = "Please log in to save your quiz result."
            }
        }
    }

    val percentage = if (questions.isNotEmpty()) {
        score * 100 / questions.size
    } else {
        0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // HEADER

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("📝", fontSize = 27.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = "AI Quiz Generator",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                )

                Text(
                    text = "Test your knowledge with Gemini AI",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = colorScheme.secondaryContainer
            ) {
                Text(
                    text = "AI",
                    modifier = Modifier.padding(
                        horizontal = 13.dp,
                        vertical = 8.dp
                    ),
                    color = colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // ERROR MESSAGE

        if (errorMessage.isNotEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(14.dp),
                    color = colorScheme.onErrorContainer,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // INPUT SCREEN

        if (questions.isEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "Create Your Quiz ✨",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Choose a subject and topic. Gemini will prepare five questions for you.",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Subject",
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Example: Data Structures")
                        },
                        leadingIcon = {
                            Text("📚")
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Topic",
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Example: Linked Lists")
                        },
                        leadingIcon = {
                            Text("📖")
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Select Difficulty",
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        listOf("Easy", "Medium", "Hard").forEach { level ->

                            val selected = difficulty == level

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = !isLoading) {
                                        difficulty = level
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    colorScheme.primaryContainer
                                } else {
                                    colorScheme.surfaceVariant
                                },
                                border = if (selected) {
                                    BorderStroke(
                                        1.dp,
                                        colorScheme.primary
                                    )
                                } else {
                                    null
                                }
                            ) {

                                Text(
                                    text = level,
                                    modifier = Modifier.padding(
                                        vertical = 13.dp
                                    ),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) {
                                        colorScheme.onPrimaryContainer
                                    } else {
                                        colorScheme.onSurfaceVariant
                                    },
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { generateQuiz() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(15.dp)
                    ) {

                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(21.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text("Generating Quiz...")
                        } else {
                            Text(
                                text = "✨  Generate Quiz with AI",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (isLoading) {

                Spacer(modifier = Modifier.height(22.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        CircularProgressIndicator()

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Gemini is preparing your questions...",
                            color = colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = "Please wait a moment.",
                            color = colorScheme.onPrimaryContainer,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // QUIZ INFORMATION

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(18.dp)
            ) {

                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text = "Your Quiz Overview",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        QuizInfoItem("5", "Questions")
                        QuizInfoItem("4", "Options")
                        QuizInfoItem("AI", "Generated")
                    }
                }
            }
        }

        // RESULT SCREEN

        else if (showResult) {

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.primaryContainer
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = if (percentage >= 80) "🏆" else if (percentage >= 50) "🎉" else "💪",
                        fontSize = 55.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Quiz Completed!",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$subject • $topic",
                        fontSize = 13.sp,
                        color = colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surface,
                        modifier = Modifier.size(155.dp)
                    ) {

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            Text(
                                text = "$percentage%",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorScheme.primary
                            )

                            Text(
                                text = "Your Score",
                                fontSize = 13.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = "$score / ${questions.size} Correct Answers",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            percentage >= 80 -> "Excellent work! Keep it up."
                            percentage >= 50 -> "Good effort! Keep practicing."
                            else -> "Don't give up. Learn and try again!"
                        },
                        fontSize = 13.sp,
                        color = colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp)
            ) {

                Column(modifier = Modifier.padding(18.dp)) {

                    Text(
                        text = "Quiz Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text("Correct", color = colorScheme.onSurfaceVariant)
                        Text(
                            "$score",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E9D68)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text("Incorrect", color = colorScheme.onSurfaceVariant)
                        Text(
                            "${questions.size - score}",
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text("Difficulty", color = colorScheme.onSurfaceVariant)
                        Text(
                            difficulty,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    questions = emptyList()
                    showResult = false
                    showExplanation = false
                    selectedAnswer = -1
                    currentQuestion = 0
                    score = 0
                    errorMessage = ""
                    hasSavedResult = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(
                    "🔄  Generate Another Quiz",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // QUESTION SCREEN

        else {

            val question = questions[currentQuestion]

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Question ${currentQuestion + 1} of ${questions.size}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                )

                Surface(
                    shape = CircleShape,
                    color = colorScheme.secondaryContainer
                ) {
                    Text(
                        text = difficulty,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(13.dp))

            LinearProgressIndicator(
                progress = {
                    (currentQuestion + 1).toFloat() / questions.size
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {

                Column(modifier = Modifier.padding(20.dp)) {

                    Text(
                        text = "QUESTION ${currentQuestion + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = question.question,
                        fontSize = 19.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Choose your answer",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            question.options.forEachIndexed { index, option ->

                val isCorrect = showExplanation &&
                        index == question.answerIndex

                val isWrong = showExplanation &&
                        index == selectedAnswer &&
                        selectedAnswer != question.answerIndex

                val isSelected = selectedAnswer == index

                val optionColor = when {
                    isCorrect -> Color(0xFF2E9D68)
                    isWrong -> colorScheme.error
                    isSelected -> colorScheme.primary
                    else -> colorScheme.outlineVariant
                }

                val backgroundColor = when {
                    isCorrect -> Color(0xFF2E9D68).copy(alpha = 0.12f)
                    isWrong -> colorScheme.errorContainer
                    isSelected -> colorScheme.primaryContainer
                    else -> colorScheme.surface
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable(enabled = !showExplanation) {
                            selectedAnswer = index
                            errorMessage = ""
                        },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = if (isSelected || isCorrect) 1.5.dp else 1.dp,
                        color = optionColor
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = backgroundColor
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Surface(
                            shape = CircleShape,
                            color = if (isSelected || isCorrect || isWrong) {
                                optionColor
                            } else {
                                colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(35.dp)
                        ) {

                            Box(contentAlignment = Alignment.Center) {

                                Text(
                                    text = if (isCorrect) "✓"
                                    else if (isWrong) "✕"
                                    else listOf("A", "B", "C", "D")[index],
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected || isCorrect || isWrong) {
                                        Color.White
                                    } else {
                                        colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = option,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            color = colorScheme.onSurface
                        )

                        if (isCorrect) {
                            Text("✓", color = optionColor, fontSize = 20.sp)
                        }
                    }
                }
            }

            // ANSWER FEEDBACK

            if (showExplanation) {

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedAnswer == question.answerIndex) {
                            Color(0xFF2E9D68).copy(alpha = 0.12f)
                        } else {
                            colorScheme.errorContainer
                        }
                    )
                ) {

                    Column(modifier = Modifier.padding(16.dp)) {

                        Text(
                            text = if (selectedAnswer == question.answerIndex) {
                                "✓ Correct Answer!"
                            } else {
                                "✕ Incorrect Answer"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (selectedAnswer == question.answerIndex) {
                                Color(0xFF2E9D68)
                            } else {
                                colorScheme.error
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Explanation",
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = question.explanation,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ACTION BUTTON

            Button(
                onClick = {

                    if (!showExplanation) {

                        if (selectedAnswer == -1) {
                            errorMessage = "Please select an answer first."
                        } else {

                            errorMessage = ""

                            if (selectedAnswer == question.answerIndex) {
                                score++
                            }

                            showExplanation = true
                        }

                    } else {

                        errorMessage = ""

                        if (currentQuestion < questions.size - 1) {

                            currentQuestion++
                            selectedAnswer = -1
                            showExplanation = false

                        } else {
                            showResult = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(15.dp)
            ) {

                Text(
                    text = if (!showExplanation) {
                        "Check Answer"
                    } else if (currentQuestion < questions.size - 1) {
                        "Next Question →"
                    } else {
                        "View Results 🎉"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun QuizInfoItem(
    value: String,
    label: String
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
