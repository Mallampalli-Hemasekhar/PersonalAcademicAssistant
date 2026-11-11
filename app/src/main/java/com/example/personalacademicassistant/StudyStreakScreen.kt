
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun getStudyDateKey(calendar: Calendar): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}

private fun parseStudyDate(date: String): Calendar? {
    return try {
        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
            ?: return null

        Calendar.getInstance().apply {
            time = parsedDate
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun StudyStreakScreen() {

    val userId = Firebase.auth.currentUser?.uid
    val db = Firebase.firestore

    var studyDates by remember { mutableStateOf(setOf<String>()) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }

    val todayString = getStudyDateKey(Calendar.getInstance())

    LaunchedEffect(userId) {
        if (userId == null) {
            loading = false
            message = "Please log in to track your study streak."
        } else {
            db.collection("users")
                .document(userId)
                .collection("studyStreaks")
                .document("activity")
                .get()
                .addOnSuccessListener { document ->
                    val dates = document.get("studyDates") as? List<*>

                    studyDates = dates
                        ?.filterIsInstance<String>()
                        ?.toSet() ?: emptySet()

                    loading = false
                }
                .addOnFailureListener {
                    loading = false
                    message = "Unable to load study activity."
                }
        }
    }

    val sortedDates = studyDates
        .sorted()
        .mapNotNull { parseStudyDate(it) }

    val totalDays = sortedDates.size

    var longestStreak = 0
    var temporaryStreak = 0
    var previousDate: Calendar? = null

    sortedDates.forEach { date ->

        val expectedDate = previousDate?.clone() as? Calendar

        if (expectedDate != null) {
            expectedDate.add(Calendar.DAY_OF_YEAR, 1)
        }

        temporaryStreak = if (
            expectedDate != null &&
            getStudyDateKey(expectedDate) == getStudyDateKey(date)
        ) {
            temporaryStreak + 1
        } else {
            1
        }

        longestStreak = maxOf(longestStreak, temporaryStreak)
        previousDate = date
    }

    var currentStreak = 0

    val checkDate = Calendar.getInstance()

    if (getStudyDateKey(checkDate) !in studyDates) {
        checkDate.add(Calendar.DAY_OF_YEAR, -1)
    }

    while (getStudyDateKey(checkDate) in studyDates) {
        currentStreak++
        checkDate.add(Calendar.DAY_OF_YEAR, -1)
    }

    fun markToday() {

        if (userId == null) return

        val updatedDates = (studyDates + todayString).sorted()

        db.collection("users")
            .document(userId)
            .collection("studyStreaks")
            .document("activity")
            .set(mapOf("studyDates" to updatedDates))
            .addOnSuccessListener {
                studyDates = updatedDates.toSet()
                message = "Today's study activity recorded successfully!"
            }
            .addOnFailureListener {
                message = "Could not save study activity. Try again."
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                text = "🔥 Study Streaks",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF263238)
            )

            Text(
                text = "Build consistency, one day at a time!",
                color = Color.Gray
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3949AB)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔥",
                        style = MaterialTheme.typography.displayLarge
                    )

                    Text(
                        text = "$currentStreak Days",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )

                    Text(
                        text = "Current Study Streak",
                        color = Color.White
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    emoji = "🏆",
                    value = "$longestStreak",
                    label = "Longest Streak",
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    emoji = "📚",
                    value = "$totalDays",
                    label = "Total Study Days",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Button(
                onClick = { markToday() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading &&
                        todayString !in studyDates &&
                        userId != null
            ) {
                Text(
                    if (todayString in studyDates) {
                        "Today's Study Completed ✓"
                    } else {
                        "Mark Today as Studied"
                    }
                )
            }
        }

        item {
            Text(
                text = "Study Activity",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Your recent 7-day activity",
                color = Color.Gray
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (6 downTo 0).forEach { offset ->

                    val date = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -offset)
                    }

                    val dateString = getStudyDateKey(date)
                    val studied = dateString in studyDates

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = SimpleDateFormat(
                                "EEE",
                                Locale.US
                            ).format(date.time),
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelSmall
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (studied) "🔥" else "○",
                            color = if (studied) {
                                Color(0xFFFF9800)
                            } else {
                                Color.Gray
                            }
                        )

                        Text(
                            text = SimpleDateFormat(
                                "d",
                                Locale.US
                            ).format(date.time),
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Achievements",
                style = MaterialTheme.typography.titleLarge
            )

            AchievementCard(
                emoji = "🌱",
                title = "First Step",
                description = "Complete your first study day",
                unlocked = totalDays >= 1
            )

            AchievementCard(
                emoji = "🔥",
                title = "3-Day Streak",
                description = "Study for 3 consecutive days",
                unlocked = longestStreak >= 3
            )

            AchievementCard(
                emoji = "⭐",
                title = "7-Day Streak",
                description = "Study for 7 consecutive days",
                unlocked = longestStreak >= 7
            )

            AchievementCard(
                emoji = "🏆",
                title = "Study Champion",
                description = "Complete 30 study days",
                unlocked = totalDays >= 30
            )
        }

        item {
            if (loading) {
                Text("Loading your study activity...")
            }

            if (message.isNotBlank()) {
                Text(
                    text = message,
                    color = Color(0xFF3949AB)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = label,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AchievementCard(
    emoji: String,
    title: String,
    description: String,
    unlocked: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) {
                Color(0xFFE8F5E9)
            } else {
                Color.White
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium
            )

            Column(
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = description,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = if (unlocked) "Unlocked ✓" else "Locked",
                    color = if (unlocked) {
                        Color(0xFF2E7D32)
                    } else {
                        Color.Gray
                    }
                )
            }
        }
    }
}
