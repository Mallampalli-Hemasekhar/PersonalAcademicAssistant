
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    var selectedSubject by remember {
        mutableStateOf("General")
    }

    var subjectMenuExpanded by remember {
        mutableStateOf(false)
    }

    val messages = remember {
        mutableStateListOf(
            DoubtChatMessage(
                "Hello! 👋 I'm your AI Academic Tutor.\n\nSelect a subject and ask me any academic question. I'll explain concepts step by step using simple language and examples.",
                false
            )
        )
    }

    var userInput by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

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

        messages.add(
            DoubtChatMessage(question, true)
        )

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

                messages.add(
                    DoubtChatMessage(answer, false)
                )

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
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // Header

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🤖",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "AI Doubt Solver",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Your personal AI-powered academic tutor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(7.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        MaterialTheme.colorScheme.tertiary,
                                        CircleShape
                                    )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "AI Tutor Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subject selector

            Text(
                text = "Choose your subject",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box {

                OutlinedButton(
                    onClick = {
                        subjectMenuExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 13.dp
                    )
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "📚",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = selectedSubject,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "▼",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                DropdownMenu(
                    expanded = subjectMenuExpanded,
                    onDismissRequest = {
                        subjectMenuExpanded = false
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {

                    subjects.forEach { subject ->

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = subject,
                                    fontWeight = if (selectedSubject == subject) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            },
                            onClick = {
                                selectedSubject = subject
                                subjectMenuExpanded = false
                            },
                            trailingIcon = {
                                if (selectedSubject == subject) {
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

            Spacer(modifier = Modifier.height(14.dp))

            // Chat heading

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Your Conversation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {

                    Text(
                        text = "${messages.count { it.isUser }} Questions",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chat messages

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    vertical = 8.dp
                )
            ) {

                items(messages) { message ->

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.isUser) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        },
                        verticalAlignment = Alignment.Bottom
                    ) {

                        if (!message.isUser) {

                            Surface(
                                modifier = Modifier.size(30.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {

                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🤖")
                                }
                            }

                            Spacer(modifier = Modifier.width(7.dp))
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(0.84f),
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (message.isUser) 18.dp else 4.dp,
                                bottomEnd = if (message.isUser) 4.dp else 18.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (message.isUser) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 1.dp
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {

                                Text(
                                    text = if (message.isUser) {
                                        "You"
                                    } else {
                                        "AI Tutor"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (message.isUser) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    }
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (isLoading) {

                    item {

                        Row(
                            modifier = Modifier.padding(
                                start = 6.dp,
                                top = 4.dp,
                                bottom = 8.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "AI Tutor is thinking...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Question input

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {

                Column(
                    modifier = Modifier.padding(10.dp)
                ) {

                    OutlinedTextField(
                        value = userInput,
                        onValueChange = {
                            userInput = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Ask your academic doubt...")
                        },
                        enabled = !isLoading,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "Ask anything about your studies",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                sendDoubt()
                            },
                            enabled = userInput.isNotBlank() && !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(
                                horizontal = 20.dp,
                                vertical = 11.dp
                            )
                        ) {

                            Text(
                                text = if (isLoading) "Wait..." else "Send ➤",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
