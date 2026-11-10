
package com.example.personalacademicassistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                text = "Hello! I'm your AI Academic Assistant. 🎓\n\nAsk me about programming, subjects, exams, study plans or any academic topic!",
                isUser = false
            )
        )
    }

    var userInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Firebase AI Logic Gemini model
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
                        text = "Sorry, something went wrong while contacting Gemini.\n\n" +
                                "Error: ${e.localizedMessage ?: "Unknown error"}",
                        isUser = false
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
                .padding(12.dp)
        ) {

            Text(
                text = "🎓 AI Academic Assistant",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Powered by Gemini AI",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.padding(6.dp))

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
                                    MaterialTheme.colorScheme.secondaryContainer
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .width(22.dp),
                                strokeWidth = 2.dp
                            )

                            Text("Gemini is thinking...")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.padding(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Ask your academic question...")
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { sendMessage() },
                    enabled = userInput.isNotBlank() && !isLoading
                ) {

                    Text("Send")
                }
            }
        }
    }
}
