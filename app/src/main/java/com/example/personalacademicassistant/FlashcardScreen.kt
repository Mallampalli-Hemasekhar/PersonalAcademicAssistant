
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import org.json.JSONObject

private data class StudyFlashcard(
    val question: String,
    val answer: String
)

@Composable
fun FlashcardScreen() {

    val colorScheme = MaterialTheme.colorScheme

    var subject by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }

    var flashcards by remember {
        mutableStateOf<List<StudyFlashcard>>(emptyList())
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    val model = remember {
        Firebase.ai(
            backend = GenerativeBackend.googleAI(),
            useLimitedUseAppCheckTokens = true
        ).generativeModel("gemini-3.5-flash")
    }

    fun generateFlashcards() {

        if (subject.isBlank() || topic.isBlank()) {
            errorMessage = "Please enter both subject and topic."
            return
        }

        if (isLoading) return

        isLoading = true
        errorMessage = ""
        successMessage = ""
        flashcards = emptyList()
        currentIndex = 0
        isFlipped = false

        scope.launch {
            try {

                val prompt = """
                    Generate exactly 10 educational study flashcards.

                    Subject: $subject
                    Topic: $topic

                    Return ONLY valid JSON in this format:

                    {
                      "flashcards": [
                        {
                          "question": "Question here",
                          "answer": "Answer here"
                        }
                      ]
                    }

                    Rules:
                    1. Generate exactly 10 flashcards.
                    2. Questions must be clear and useful for students.
                    3. Answers must be accurate and easy to understand.
                    4. Cover important concepts of the topic.
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
                val array = json.getJSONArray("flashcards")

                val generatedCards = mutableListOf<StudyFlashcard>()

                for (i in 0 until array.length()) {

                    val item = array.getJSONObject(i)

                    generatedCards.add(
                        StudyFlashcard(
                            question = item.getString("question"),
                            answer = item.getString("answer")
                        )
                    )
                }

                if (generatedCards.size != 10) {
                    throw Exception(
                        "Gemini did not generate exactly 10 flashcards."
                    )
                }

                flashcards = generatedCards

            } catch (e: Exception) {

                errorMessage =
                    "Generation failed: ${e.localizedMessage ?: "Unknown error"}"

            } finally {
                isLoading = false
            }
        }
    }

    fun saveFlashcards() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            errorMessage = "Please log in to save flashcards."
            return
        }

        if (flashcards.isEmpty() || isSaving) return

        isSaving = true
        errorMessage = ""
        successMessage = ""

        val cardList = flashcards.map {
            mapOf(
                "question" to it.question,
                "answer" to it.answer
            )
        }

        val flashcardData = hashMapOf(
            "subject" to subject,
            "topic" to topic,
            "totalCards" to flashcards.size,
            "flashcards" to cardList,
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("flashcards")
            .add(flashcardData)
            .addOnSuccessListener {
                successMessage = "Flashcards saved successfully!"
                isSaving = false
            }
            .addOnFailureListener { exception ->
                errorMessage =
                    "Could not save flashcards: ${exception.localizedMessage}"
                isSaving = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // HEADER

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {

                Text(
                    text = "📚 AI Flashcards",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Learn smarter with AI-powered study cards.",
                    fontSize = 14.sp,
                    color = colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "✦ Powered by Gemini AI",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // INPUT SECTION

        if (flashcards.isEmpty()) {

            Text(
                text = "Create Your Flashcards",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter a subject and topic to generate 10 study cards.",
                color = colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                placeholder = { Text("Example: Data Structures") },
                leadingIcon = {
                    Text("📘")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Topic") },
                placeholder = { Text("Example: Linked Lists") },
                leadingIcon = {
                    Text("📝")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = { generateFlashcards() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isLoading) {
                        "Generating..."
                    } else {
                        "✨ Generate Flashcards"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isLoading) {

                Spacer(modifier = Modifier.height(24.dp))

                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Gemini is creating your flashcards...",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // FEATURE INFORMATION

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "How it works",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("1. Enter your subject and topic.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("2. Generate 10 AI-powered flashcards.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("3. Tap a card to reveal its answer.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("4. Save your cards to Firebase.")
                }
            }
        }

        // FLASHCARD DISPLAY

        else {

            Text(
                text = subject,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = topic,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // PROGRESS

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Your Learning Progress",
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "${currentIndex + 1} / ${flashcards.size}",
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = {
                    (currentIndex + 1).toFloat() / flashcards.size
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // FLIP CARD

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp)
                    .clickable {
                        isFlipped = !isFlipped
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFlipped) {
                        colorScheme.secondaryContainer
                    } else {
                        colorScheme.primary
                    }
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = if (isFlipped) {
                            "ANSWER"
                        } else {
                            "QUESTION"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFlipped) {
                            colorScheme.onSecondaryContainer
                        } else {
                            colorScheme.onPrimary
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (isFlipped) {
                            flashcards[currentIndex].answer
                        } else {
                            flashcards[currentIndex].question
                        },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFlipped) {
                            colorScheme.onSecondaryContainer
                        } else {
                            colorScheme.onPrimary
                        }
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = if (isFlipped) {
                            "Tap to see the question ↩"
                        } else {
                            "Tap to reveal the answer ↻"
                        },
                        fontSize = 13.sp,
                        color = if (isFlipped) {
                            colorScheme.onSecondaryContainer
                        } else {
                            colorScheme.onPrimary
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // NAVIGATION

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedButton(
                    onClick = {
                        if (currentIndex > 0) {
                            currentIndex--
                            isFlipped = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = currentIndex > 0,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("← Previous")
                }

                Button(
                    onClick = {
                        if (currentIndex < flashcards.size - 1) {
                            currentIndex++
                            isFlipped = false
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = currentIndex < flashcards.size - 1,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Next →")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SAVE FLASHCARDS

            Button(
                onClick = { saveFlashcards() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isSaving,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isSaving) {
                        "Saving..."
                    } else {
                        "💾 Save Flashcards"
                    },
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    flashcards = emptyList()
                    currentIndex = 0
                    isFlipped = false
                    errorMessage = ""
                    successMessage = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Generate New Flashcards")
            }
        }

        // SUCCESS MESSAGE

        if (successMessage.isNotEmpty()) {

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "✓ $successMessage",
                    modifier = Modifier.padding(14.dp),
                    color = colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ERROR MESSAGE

        if (errorMessage.isNotEmpty()) {

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = errorMessage,
                color = colorScheme.error,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
