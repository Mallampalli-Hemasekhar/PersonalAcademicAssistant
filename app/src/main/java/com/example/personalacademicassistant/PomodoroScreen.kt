
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PomodoroScreen() {

    val focusTime = 25 * 60
    val shortBreakTime = 5 * 60
    val longBreakTime = 15 * 60

    var selectedMode by remember { mutableStateOf("Focus") }
    var remainingSeconds by remember { mutableIntStateOf(focusTime) }
    var isRunning by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }

    var completedFocusSessions by remember { mutableIntStateOf(0) }
    var completedBreakSessions by remember { mutableIntStateOf(0) }

    val totalTime = when (selectedMode) {
        "Short Break" -> shortBreakTime
        "Long Break" -> longBreakTime
        else -> focusTime
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    val progress = remainingSeconds.toFloat() / totalTime.toFloat()

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000)

            if (remainingSeconds == 1) {
                remainingSeconds = 0
                isRunning = false
                completed = true

                if (selectedMode == "Focus") {
                    completedFocusSessions++
                } else {
                    completedBreakSessions++
                }
            } else {
                remainingSeconds--
            }
        }
    }

    fun changeMode(mode: String) {
        selectedMode = mode
        isRunning = false
        completed = false

        remainingSeconds = when (mode) {
            "Short Break" -> shortBreakTime
            "Long Break" -> longBreakTime
            else -> focusTime
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // Header

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Pomodoro Timer",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Focus better. Study smarter. Achieve more.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Session Statistics

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎯",
                        fontSize = 25.sp
                    )

                    Text(
                        text = "$completedFocusSessions",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "Focus Sessions",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "☕",
                        fontSize = 25.sp
                    )

                    Text(
                        text = "$completedBreakSessions",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Text(
                        text = "Break Sessions",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Timer Card

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = surfaceColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                Text(
                    text = when (selectedMode) {
                        "Focus" -> "TIME TO FOCUS"
                        "Short Break" -> "TAKE A SHORT BREAK"
                        else -> "TAKE A LONG BREAK"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
                )

                // Mode Selection

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    listOf(
                        "Focus",
                        "Short Break",
                        "Long Break"
                    ).forEach { mode ->

                        val isSelected = selectedMode == mode

                        Button(
                            onClick = {
                                changeMode(mode)
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                horizontal = 2.dp,
                                vertical = 10.dp
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) {
                                    primaryColor
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        ) {
                            Text(
                                text = when (mode) {
                                    "Short Break" -> "Short"
                                    "Long Break" -> "Long"
                                    else -> "Focus"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Circular Timer

                Box(
                    modifier = Modifier.size(250.dp),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 12.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxSize(),
                        color = if (selectedMode == "Focus") {
                            primaryColor
                        } else {
                            secondaryColor
                        },
                        strokeWidth = 12.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {

                        Text(
                            text = selectedMode,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = String.format(
                                "%02d:%02d",
                                minutes,
                                seconds
                            ),
                            fontSize = 45.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (isRunning) {
                                "Keep going!"
                            } else if (completed) {
                                "Completed!"
                            } else {
                                "Ready when you are"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Timer Controls

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Button(
                        onClick = {
                            isRunning = !isRunning
                            completed = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        enabled = remainingSeconds > 0,
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text(
                            text = if (isRunning) "Pause" else "Start",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            isRunning = false
                            completed = false
                            remainingSeconds = totalTime
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text(
                            text = "Reset",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (completed) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎉 Session Completed!",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Text(
                                text = if (selectedMode == "Focus") {
                                    "Excellent work! You completed a focus session."
                                } else {
                                    "Your break is complete. Ready for the next session?"
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Study Tips

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "💡 Study Tips",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = "🎯 Focus on one task at a time.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "📵 Avoid distractions during focus sessions.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "☕ Take breaks to refresh your mind.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "💧 Drink water and stay hydrated.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Session Summary

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Today's Progress",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Completed focus sessions: $completedFocusSessions"
                )

                Text(
                    text = "Completed break sessions: $completedBreakSessions"
                )

                Text(
                    text = "Total focused time: ${
                        completedFocusSessions * 25
                    } minutes",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
