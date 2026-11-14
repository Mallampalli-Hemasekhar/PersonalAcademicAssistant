
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private data class DoubtChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun AIDoubtSolverScreen() {

    val subjects = listOf(
        "General",
        "Data Structures and Algorithms",
        "DBMS",
        "Operating Systems",
        "Computer Networks",
        "Artificial Intelligence",
        "Machine Learning",
        "Mathematics",
        "Programming",
        "Other"
    )

    var selectedSubject by remember { mutableStateOf("General") }
    var subjectMenuExpanded by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            DoubtChatMessage(
                "Hello! 👋 I'm your AI Academic Doubt Solver.\n\nSelect a subject and ask me any academic question. I'll explain it step by step in simple language.",
                false
            )
        )
    }

    var userInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val model = remember {
        Firebase.ai(
            backend = GenerativeBackend.googleAI(),
            useLimitedUseAppCheckTokens = true
        ).generativeModel("gemini-3.5-flash")
    }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendDoubt() {

        val question = userInput.trim()

        if (question.isEmpty() || isLoading) return

        val conversation = messages.joinToString("\n") {
            if (it.isUser) {
                "Student: ${it.text}"
            } else {
                "AI Tutor: ${it.text}"
            }
        }

        messages.add(DoubtChatMessage(question, true))

        userInput = ""
        isLoading = true

        coroutineScope.launch {

            try {

                val prompt = """
                    You are an expert AI Academic Doubt Solver.

                    Selected subject: $selectedSubject

                    Your responsibilities:
                    1. Understand the student's academic question.
                    2. Explain concepts in simple beginner-friendly language.
                    3. Provide step-by-step explanations.
                    4. Give examples wherever useful.
                    5. Explain programming code clearly when requested.
                    6. For numerical problems, show the solution steps.
                    7. Correct misunderstandings politely.
                    8. Avoid unnecessarily complicated explanations.

                    Format your answer clearly using headings,
                    numbered steps and examples when appropriate.

                    Previous conversation:
                    $conversation

                    Student's question:
                    $question

                    Provide a helpful academic explanation.
                """.trimIndent()

                val response = model.generateContent(prompt)

                val answer = response.text
                    ?: "Sorry, I couldn't generate an answer. Please try again."

                messages.add(DoubtChatMessage(answer, false))

                val uid = FirebaseAuth.getInstance().currentUser?.uid

                if (uid != null) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .collection("academicDoubts")
                        .add(
                            hashMapOf(
                                "subject" to selectedSubject,
                                "question" to question,
                                "answer" to answer,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                }

            } catch (e: Exception) {

                messages.add(
                    DoubtChatMessage(
                        "Unable to get an answer right now.\n\n${e.localizedMessage ?: "Please check your internet connection and try again."}",
                        false
                    )
                )

            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            Text(
                text = "AI Doubt Solver",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your personal AI-powered academic tutor",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box {

                OutlinedButton(
                    onClick = {
                        subjectMenuExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Subject: $selectedSubject"
                    )
                }

                DropdownMenu(
                    expanded = subjectMenuExpanded,
                    onDismissRequest = {
                        subjectMenuExpanded = false
                    },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {

                    subjects.forEach { subject ->

                        DropdownMenuItem(
                            text = {
                                Text(subject)
                            },
                            onClick = {
                                selectedSubject = subject
                                subjectMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(messages) { message ->

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.isUser) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                    ) {

                        Card(
                            modifier = Modifier.fillMaxWidth(0.88f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (message.isUser) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {

                            Text(
                                text = message.text,
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                if (isLoading) {

                    item {

                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text("AI Tutor is thinking...")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                OutlinedTextField(
                    value = userInput,
                    onValueChange = {
                        userInput = it
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Ask your doubt...")
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        sendDoubt()
                    },
                    enabled = userInput.isNotBlank() && !isLoading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send")
                }
            }
        }
    }
}
