
package com.example.personalacademicassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

    val totalTime = when (selectedMode) {
        "Short Break" -> shortBreakTime
        "Long Break" -> longBreakTime
        else -> focusTime
    }

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000)
            if (remainingSeconds == 1) {
                remainingSeconds = 0
                isRunning = false
                completed = true
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

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val progress = remainingSeconds.toFloat() / totalTime

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Text(
            "Pomodoro Study Timer",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Stay focused and make every study session count!",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Focus", "Short Break", "Long Break").forEach { mode ->
                Button(
                    onClick = { changeMode(mode) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = 10.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMode == mode)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (selectedMode == mode)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        mode,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    selectedMode,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    String.format("%02d:%02d", minutes, seconds),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    isRunning = !isRunning
                    completed = false
                },
                enabled = remainingSeconds > 0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isRunning) "Pause" else "Start")
            }

            OutlinedButton(
                onClick = {
                    isRunning = false
                    completed = false
                    remainingSeconds = totalTime
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reset")
            }
        }

        if (completed) {
            Text(
                "Session completed! Great work!",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Study Tips",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text("• Avoid distractions during focus time.")
                Text("• Take breaks between study sessions.")
                Text("• Drink water and relax during breaks.")
            }
        }
    }
}
