
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val todayString = getStudyDateKey(Calendar.getInstance())

    LaunchedEffect(userId) {
        loading = true

        if (userId == null) {
            loading = false
            message = "Please log in to track your study streak."
            isError = true
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
                    message = ""
                }
                .addOnFailureListener {
                    loading = false
                    message = "Unable to load study activity. Please try again."
                    isError = true
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

    val todayCompleted = todayString in studyDates

    fun markToday() {

        if (userId == null || todayCompleted || saving) return

        saving = true
        message = ""

        val updatedDates = (studyDates + todayString).sorted()

        db.collection("users")
            .document(userId)
            .collection("studyStreaks")
            .document("activity")
            .set(mapOf("studyDates" to updatedDates))
            .addOnSuccessListener {
                studyDates = updatedDates.toSet()
                saving = false
                message = "Today's study activity recorded successfully!"
                isError = false
            }
            .addOnFailureListener {
                saving = false
                message = "Could not save study activity. Please try again."
                isError = true
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        item {
            Column {
                Text(
                    text = "Study Streaks 🔥",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "Build consistency, one day at a time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(25.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Surface(
                        modifier = Modifier.size(76.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🔥",
                                fontSize = 38.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "$currentStreak Days",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "Current Study Streak",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (currentStreak > 0) {
                            "Amazing! Keep your momentum going."
                        } else {
                            "Start a new streak today. You've got this!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {

                    Text(
                        text = "Today's Goal 🎯",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (todayCompleted) {
                            "You've recorded your study activity today."
                        } else {
                            "Have you studied today? Record your progress."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { markToday() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading &&
                                !saving &&
                                !todayCompleted &&
                                userId != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(19.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                        }

                        Text(
                            text = when {
                                saving -> "Saving..."
                                todayCompleted -> "Today's Study Completed ✓"
                                else -> "Mark Today as Studied"
                            },
                            modifier = Modifier.padding(vertical = 5.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Column {
                Text(
                    text = "Weekly Activity 📅",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "Your study activity over the last 7 days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    (6 downTo 0).forEach { offset ->

                        val date = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -offset)
                        }

                        val dateString = getStudyDateKey(date)
                        val studied = dateString in studyDates

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = SimpleDateFormat(
                                    "EEE",
                                    Locale.US
                                ).format(date.time),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = if (studied) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (studied) "✓" else "·",
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (studied) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = SimpleDateFormat(
                                    "d",
                                    Locale.US
                                ).format(date.time),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            Column {
                Text(
                    text = "Achievements 🏆",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "Celebrate every milestone in your journey.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            AchievementCard(
                emoji = "🌱",
                title = "First Step",
                description = "Complete your first study day",
                unlocked = totalDays >= 1
            )
        }

        item {
            AchievementCard(
                emoji = "🔥",
                title = "3-Day Streak",
                description = "Study for 3 consecutive days",
                unlocked = longestStreak >= 3
            )
        }

        item {
            AchievementCard(
                emoji = "⭐",
                title = "7-Day Streak",
                description = "Study for 7 consecutive days",
                unlocked = longestStreak >= 7
            )
        }

        item {
            AchievementCard(
                emoji = "🏆",
                title = "Study Champion",
                description = "Complete 30 study days",
                unlocked = totalDays >= 30
            )
        }

        item {
            if (loading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(modifier = Modifier.size(12.dp))

                    Text(
                        text = "Loading your study activity...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (message.isNotBlank()) {
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = emoji,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 26.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = if (unlocked) "Unlocked ✓" else "Not unlocked yet",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (unlocked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (unlocked) {
                Text(
                    text = "🏅",
                    fontSize = 22.sp
                )
            }
        }
    }
}
