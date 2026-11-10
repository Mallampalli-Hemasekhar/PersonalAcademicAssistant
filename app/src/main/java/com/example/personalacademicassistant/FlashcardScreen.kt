
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

    // GENERATE FLASHCARDS

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
                    throw Exception("Gemini did not generate exactly 10 flashcards.")
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

    // SAVE FLASHCARDS TO FIRESTORE

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

    // SCREEN UI

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        Text(
            text = "📚 AI Flashcard Generator",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create smart study cards using Gemini AI.",
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        // INPUT SECTION

        if (flashcards.isEmpty()) {

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

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { generateFlashcards() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Generate Flashcards with AI")
            }

            if (isLoading) {

                Spacer(modifier = Modifier.height(20.dp))

                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    "Gemini is creating your flashcards...",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // FLASHCARD DISPLAY

        else {

            Text(
                text = "Subject: $subject",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3F51B5)
            )

            Text(
                text = "Topic: $topic",
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Card ${currentIndex + 1} of ${flashcards.size}",
                fontWeight = FontWeight.SemiBold
            )

            LinearProgressIndicator(
                progress = {
                    (currentIndex + 1).toFloat() / flashcards.size
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            // CLICKABLE FLASHCARD

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 230.dp)
                    .clickable {
                        isFlipped = !isFlipped
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFlipped) {
                        Color(0xFFE8EAF6)
                    } else {
                        Color(0xFF3F51B5)
                    }
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = if (isFlipped) "ANSWER" else "QUESTION",
                        color = if (isFlipped) {
                            Color(0xFF3F51B5)
                        } else {
                            Color.White
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (isFlipped) {
                            flashcards[currentIndex].answer
                        } else {
                            flashcards[currentIndex].question
                        },
                        color = if (isFlipped) {
                            Color(0xFF172554)
                        } else {
                            Color.White
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Tap card to ${if (isFlipped) "see question" else "reveal answer"}",
                        color = if (isFlipped) {
                            Color.Gray
                        } else {
                            Color.White
                        },
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // PREVIOUS AND NEXT BUTTONS

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
                    modifier = Modifier.weight(1f),
                    enabled = currentIndex > 0
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
                    modifier = Modifier.weight(1f),
                    enabled = currentIndex < flashcards.size - 1
                ) {
                    Text("Next →")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SAVE BUTTON

            Button(
                onClick = { saveFlashcards() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {

                Text(
                    if (isSaving) {
                        "Saving..."
                    } else {
                        "Save Flashcards"
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    flashcards = emptyList()
                    currentIndex = 0
                    isFlipped = false
                    errorMessage = ""
                    successMessage = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate New Flashcards")
            }
        }

        // SUCCESS MESSAGE

        if (successMessage.isNotEmpty()) {

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = successMessage,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold
            )
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
