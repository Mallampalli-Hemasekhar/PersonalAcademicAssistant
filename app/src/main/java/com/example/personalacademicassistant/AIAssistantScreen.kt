
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.launch

private data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

@Composable
fun AIAssistantScreen() {

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "Hello! I'm your AI Academic Assistant. 🎓\n\nI can help you understand concepts, solve programming doubts, prepare for exams and create study plans.\n\nWhat would you like to learn today?",
                isUser = false
            )
        )
    }

    var userInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val model = remember {
        Firebase.ai(
            backend = GenerativeBackend.googleAI(),
            useLimitedUseAppCheckTokens = true
        ).generativeModel("gemini-3.5-flash")
    }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(
                if (isLoading) messages.size else messages.size - 1
            )
        }
    }

    fun sendMessage() {

        val question = userInput.trim()

        if (question.isEmpty() || isLoading) return

        val previousConversation = messages.joinToString("\n") {
            if (it.isUser) {
                "Student: ${it.text}"
            } else {
                "Assistant: ${it.text}"
            }
        }

        messages.add(ChatMessage(question, true))

        userInput = ""
        isLoading = true

        keyboardController?.hide()
        focusManager.clearFocus()

        coroutineScope.launch {

            try {

                val prompt = """
                    You are a friendly and knowledgeable AI Academic Assistant
                    inside a Personal Digital Academic Assistant Android app.

                    Your responsibilities:

                    1. Explain academic concepts in simple language.
                    2. Help students understand programming and coding.
                    3. Help prepare for examinations.
                    4. Create useful study plans.
                    5. Explain mathematics and technical subjects.
                    6. Give clear, beginner-friendly answers.

                    Keep answers organized and easy to understand.
                    Use headings, numbered steps and examples when useful.

                    Previous conversation:
                    $previousConversation

                    Student: $question

                    Assistant:
                """.trimIndent()

                val response = model.generateContent(prompt)

                messages.add(
                    ChatMessage(
                        text = response.text
                            ?: "Sorry, I couldn't generate a response.",
                        isUser = false
                    )
                )

            } catch (e: Exception) {

                messages.add(
                    ChatMessage(
                        text = "Sorry, something went wrong while contacting Gemini.\n\nError: ${e.localizedMessage ?: "Unknown error"}",
                        isUser = false
                    )
                )

            } finally {
                isLoading = false
            }
        }
    }

    val suggestions = listOf(
        "Explain Kotlin basics",
        "What is machine learning?",
        "Help me prepare for exams",
        "Explain data structures"
    )

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
        ) {

            // Header

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎓",
                        fontSize = 25.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "AI Academic Assistant",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Powered by Gemini AI",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "AI",
                        modifier = Modifier.padding(
                            horizontal = 13.dp,
                            vertical = 7.dp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Chat Messages

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎓", fontSize = 16.sp)
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
                                    MaterialTheme.colorScheme.primary
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
                                        "Academic Assistant"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (message.isUser) {
                                        MaterialTheme.colorScheme.onPrimary.copy(
                                            alpha = 0.8f
                                        )
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )

                                Spacer(modifier = Modifier.height(5.dp))

                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (message.isUser) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }

                if (isLoading) {

                    item {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎓", fontSize = 16.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {

                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = "Gemini is thinking...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Suggested Questions

            if (messages.size == 1 && !isLoading) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {

                    Text(
                        text = "Try asking",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    suggestions.forEach { suggestion ->

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 7.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                userInput = suggestion
                            }
                        ) {
                            Text(
                                text = "✨  $suggestion",
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Message Input

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 5.dp
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    OutlinedTextField(
                        value = userInput,
                        onValueChange = {
                            userInput = it
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Ask your academic question...")
                        },
                        shape = RoundedCornerShape(18.dp),
                        maxLines = 4,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                sendMessage()
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            sendMessage()
                        },
                        enabled = userInput.isNotBlank() && !isLoading,
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        contentPadding = PaddingValues(horizontal = 15.dp)
                    ) {
                        Text(
                            text = "Send",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
