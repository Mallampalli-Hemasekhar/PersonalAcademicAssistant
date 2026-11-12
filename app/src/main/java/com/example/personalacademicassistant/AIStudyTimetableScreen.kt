
package com.example.personalacademicassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import org.json.JSONArray

private data class StudySession(
    val day: String,
    val time: String,
    val subject: String,
    val task: String
)

@Composable
fun AIStudyTimetableScreen() {

    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var subjects by remember { mutableStateOf("") }
    var hoursPerDay by remember { mutableStateOf("3") }
    var examDetails by remember { mutableStateOf("") }

    var sessions by remember {
        mutableStateOf<List<StudySession>>(emptyList())
    }

    var loading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    val model = remember {
        Firebase.ai(
            backend = GenerativeBackend.googleAI(),
            useLimitedUseAppCheckTokens = true
        ).generativeModel("gemini-3.5-flash")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "AI Study Timetable Generator",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Generate a personalized weekly study plan using Gemini AI.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = subjects,
            onValueChange = { subjects = it },
            label = { Text("Subjects") },
            placeholder = {
                Text("Data Structures, DBMS, Operating Systems")
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = hoursPerDay,
            onValueChange = {
                hoursPerDay = it.filter { character ->
                    character.isDigit()
                }
            },
            label = { Text("Study Hours Per Day") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = examDetails,
            onValueChange = { examDetails = it },
            label = { Text("Exam Dates and Priorities (Optional)") },
            placeholder = {
                Text("Example: GATE exam in February")
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {

                val hours = hoursPerDay.toIntOrNull()

                when {
                    subjects.isBlank() -> {
                        errorMessage = "Please enter your subjects."
                    }

                    hours == null || hours !in 1..12 -> {
                        errorMessage =
                            "Enter study hours between 1 and 12."
                    }

                    else -> {

                        loading = true
                        errorMessage = ""
                        successMessage = ""
                        sessions = emptyList()

                        scope.launch {

                            try {

                                val prompt = """
                                    Create a realistic weekly study
                                    timetable for a college student.

                                    Subjects: $subjects

                                    Available study hours per day: $hours

                                    Exam dates and priorities: $examDetails

                                    Create a timetable for all seven days:
                                    Monday, Tuesday, Wednesday, Thursday,
                                    Friday, Saturday and Sunday.

                                    Requirements:
                                    1. Distribute subjects fairly.
                                    2. Include revision sessions.
                                    3. Include short breaks.
                                    4. Do not exceed available study hours.
                                    5. Use realistic time slots.
                                    6. Avoid overlapping sessions.

                                    Return ONLY a valid JSON array.
                                    Do not include markdown or code fences.

                                    Each object must contain:
                                    {
                                      "day": "Monday",
                                      "time": "7:00 AM - 8:00 AM",
                                      "subject": "Data Structures",
                                      "task": "Study arrays and linked lists"
                                    }
                                """.trimIndent()

                                val response =
                                    model.generateContent(prompt)

                                val responseText = response.text
                                    ?: throw Exception(
                                        "Gemini returned an empty response."
                                    )

                                val cleanJson = responseText
                                    .replace("```json", "")
                                    .replace("```", "")
                                    .trim()

                                val jsonArray = JSONArray(cleanJson)

                                val generatedSessions =
                                    mutableListOf<StudySession>()

                                for (i in 0 until jsonArray.length()) {

                                    val item =
                                        jsonArray.getJSONObject(i)

                                    generatedSessions.add(
                                        StudySession(
                                            day = item.optString("day"),
                                            time = item.optString("time"),
                                            subject = item.optString("subject"),
                                            task = item.optString("task")
                                        )
                                    )
                                }

                                if (generatedSessions.isEmpty()) {
                                    throw Exception(
                                        "No study sessions were generated."
                                    )
                                }

                                sessions = generatedSessions

                                successMessage =
                                    "Your weekly timetable is ready!"

                            } catch (e: Exception) {

                                errorMessage =
                                    e.message ?: "Unable to generate timetable."

                            } finally {

                                loading = false
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        ) {

            if (loading) {

                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("Generating Timetable...")

            } else {

                Text("✨ Generate AI Timetable")
            }
        }

        if (errorMessage.isNotEmpty()) {

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (successMessage.isNotEmpty()) {

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = successMessage,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sessions.isNotEmpty()) {

            Text(
                text = "Your Weekly Study Plan",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(sessions) { session ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {

                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {

                            Text(
                                text = session.day,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = session.time,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = session.subject,
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text(
                                text = session.task,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {

                    if (userId == null) {

                        errorMessage = "Please log in first."

                    } else {

                        saving = true
                        errorMessage = ""
                        successMessage = ""

                        val sessionData = sessions.map { session ->

                            hashMapOf<String, Any>(
                                "day" to session.day,
                                "time" to session.time,
                                "subject" to session.subject,
                                "task" to session.task
                            )
                        }

                        val timetableData = hashMapOf<String, Any>(
                            "subjects" to subjects,
                            "hoursPerDay" to
                                    (hoursPerDay.toIntOrNull() ?: 3),
                            "examDetails" to examDetails,
                            "sessions" to sessionData,
                            "createdAt" to System.currentTimeMillis()
                        )

                        db.collection("users")
                            .document(userId)
                            .collection("aiStudyTimetables")
                            .add(timetableData)
                            .addOnSuccessListener {

                                saving = false

                                successMessage =
                                    "Timetable saved successfully!"
                            }
                            .addOnFailureListener { exception ->

                                saving = false

                                errorMessage =
                                    exception.message
                                        ?: "Unable to save timetable."
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving
            ) {

                if (saving) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Saving...")

                } else {

                    Text("Save Timetable to Firestore")
                }
            }
        }
    }
}
