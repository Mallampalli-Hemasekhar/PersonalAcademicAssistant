
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val model = remember {
        Firebase.ai(
            backend = GenerativeBackend.googleAI(),
            useLimitedUseAppCheckTokens = true
        ).generativeModel("gemini-3.5-flash")
    }

    // GENERATE QUIZ

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

    // SAVE QUIZ RESULT TO FIRESTORE

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

    // MAIN SCREEN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = "📝 AI Quiz Generator",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF172554)
        )

        Text(
            text = "Powered by Gemini AI",
            color = Color(0xFF3F51B5),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // INPUT SCREEN

        if (questions.isEmpty()) {

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Enter Subject") },
                placeholder = { Text("Example: Data Structures") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Enter Topic") },
                placeholder = { Text("Example: Linked Lists") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                "Select Difficulty",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                listOf("Easy", "Medium", "Hard").forEach { level ->

                    Button(
                        onClick = { difficulty = level },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (difficulty == level) {
                                Color(0xFF3F51B5)
                            } else {
                                Color(0xFF9E9E9E)
                            }
                        )
                    ) {
                        Text(level)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { generateQuiz() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Generate Quiz with AI")
            }

            if (isLoading) {

                Spacer(modifier = Modifier.height(20.dp))

                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    "Gemini is preparing your questions...",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // RESULT SCREEN

        else if (showResult) {

            Text(
                "🎉 Quiz Completed!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF172554)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8EAF6)
                )
            ) {

                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text("Your Score", fontSize = 20.sp)

                    Text(
                        "$score / ${questions.size}",
                        fontSize = 38.sp,
                        color = Color(0xFF3F51B5)
                    )

                    Text(
                        "Percentage: ${score * 100 / questions.size}%",
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Another Quiz")
            }
        }

        // QUESTION SCREEN

        else {

            val question = questions[currentQuestion]

            Text(
                "Question ${currentQuestion + 1} of ${questions.size}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = {
                    (currentQuestion + 1).toFloat() / questions.size
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                question.question,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ANSWER OPTIONS

            question.options.forEachIndexed { index, option ->

                val isCorrect = showExplanation &&
                        index == question.answerIndex

                val isWrong = showExplanation &&
                        index == selectedAnswer &&
                        selectedAnswer != question.answerIndex

                val cardColor = when {
                    isCorrect -> Color(0xFFC8E6C9)
                    isWrong -> Color(0xFFFFCDD2)
                    selectedAnswer == index -> Color(0xFFDDE3FF)
                    else -> Color.White
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable(enabled = !showExplanation) {
                            selectedAnswer = index
                            errorMessage = ""
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = cardColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        RadioButton(
                            selected = selectedAnswer == index,
                            onClick = {
                                if (!showExplanation) {
                                    selectedAnswer = index
                                    errorMessage = ""
                                }
                            },
                            enabled = !showExplanation
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(option)
                    }
                }
            }

            // ANSWER FEEDBACK

            if (showExplanation) {

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    if (selectedAnswer == question.answerIndex) {
                        "Correct answer! ✓"
                    } else {
                        "Incorrect answer."
                    },
                    color = Color(0xFF172554),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Explanation: ${question.explanation}"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CHECK ANSWER BUTTON

            if (!showExplanation) {

                Button(
                    onClick = {

                        if (selectedAnswer == -1) {

                            errorMessage = "Please select an answer first."

                        } else {

                            errorMessage = ""

                            if (selectedAnswer == question.answerIndex) {
                                score++
                            }

                            showExplanation = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check Answer")
                }

            } else {

                // NEXT QUESTION / VIEW RESULTS BUTTON

                Button(
                    onClick = {

                        errorMessage = ""

                        if (currentQuestion < questions.size - 1) {

                            currentQuestion++
                            selectedAnswer = -1
                            showExplanation = false

                        } else {

                            showResult = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3F51B5)
                    )
                ) {

                    Text(
                        if (currentQuestion < questions.size - 1) {
                            "Next Question →"
                        } else {
                            "View Results"
                        },
                        fontSize = 16.sp
                    )
                }
            }
        }

        // ERROR MESSAGE

        if (errorMessage.isNotEmpty()) {

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
