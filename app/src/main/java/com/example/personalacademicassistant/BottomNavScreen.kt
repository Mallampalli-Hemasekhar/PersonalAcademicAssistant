
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavScreen() {

    var selectedItem by remember {
        mutableIntStateOf(0)
    }

    val items = listOf(
        "Home", "Schedule", "Tasks", "Progress", "Profile"
    )

    val icons = listOf(
        "🏠", "📅", "✅", "📊", "👤"
    )

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        icon = {
                            Text(icons[index], fontSize = 20.sp)
                        },
                        label = {
                            Text(item)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F7FB)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            when (selectedItem) {
                0 -> DashboardScreen()
                1 -> ScheduleScreen()
                2 -> TasksScreen()
                3 -> ProgressScreen()
                4 -> ProfileScreen()
            }
        }
    }
}

/* ---------------- SCHEDULE SCREEN ---------------- */

@Composable
fun ScheduleScreen() {

    var showTimetable by remember {
        mutableStateOf(true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Button(
                onClick = { showTimetable = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showTimetable) {
                        Color(0xFF3F51B5)
                    } else {
                        Color.LightGray
                    }
                )
            ) {
                Text("📅 Timetable", fontSize = 14.sp)
            }

            Button(
                onClick = { showTimetable = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showTimetable) {
                        Color(0xFF3F51B5)
                    } else {
                        Color.LightGray
                    }
                )
            ) {
                Text("📝 Exams", fontSize = 14.sp)
            }
        }

        if (showTimetable) {
            TimetableScreen()
        } else {
            ExamScreen()
        }
    }
}

/* ---------------- TASKS SCREEN ---------------- */

@Composable
fun TasksScreen() {
    AssignmentScreen()
}

/* ---------------- PROGRESS SCREEN ---------------- */

@Composable
fun ProgressScreen() {

    var showAnalytics by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Button(
                onClick = { showAnalytics = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showAnalytics) {
                        Color(0xFF3F51B5)
                    } else {
                        Color.LightGray
                    }
                )
            ) {
                Text("📊 Attendance")
            }

            Button(
                onClick = { showAnalytics = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showAnalytics) {
                        Color(0xFF00897B)
                    } else {
                        Color.LightGray
                    }
                )
            ) {
                Text("📈 Analytics")
            }
        }

        if (showAnalytics) {
            PerformanceAnalyticsScreen()
        } else {
            AttendanceScreen()
        }
    }
}

/* ---------------- PROFILE SCREEN ---------------- */

@Composable
fun ProfileScreen() {

    var selectedFeature by remember {
        mutableStateOf("Profile")
    }

    when (selectedFeature) {

        "Notes" -> {

            FeatureContainer(
                buttonColor = Color(0xFF3F51B5),
                onBack = { selectedFeature = "Profile" }
            ) {
                NotesScreen()
            }
        }

        "GPA" -> {

            FeatureContainer(
                buttonColor = Color(0xFF00897B),
                onBack = { selectedFeature = "Profile" }
            ) {
                GPAScreen()
            }
        }

        "StudyPlanner" -> {

            FeatureContainer(
                buttonColor = Color(0xFFFF9800),
                onBack = { selectedFeature = "Profile" }
            ) {
                StudyPlannerScreen()
            }
        }

        "DailyGoals" -> {

            FeatureContainer(
                buttonColor = Color(0xFF7B1FA2),
                onBack = { selectedFeature = "Profile" }
            ) {
                DailyGoalsScreen()
            }
        }

        // NEW: POMODORO STUDY TIMER

        "Pomodoro" -> {

            FeatureContainer(
                buttonColor = Color(0xFFE64A19),
                onBack = { selectedFeature = "Profile" }
            ) {
                PomodoroScreen()
            }
        }

        else -> {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F7FB))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "👤",
                    fontSize = 55.sp
                )

                Text(
                    text = "Profile",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172554)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Manage your student profile and academic information.",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = { selectedFeature = "Notes" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3F51B5)
                    )
                ) {
                    Text("📒 Academic Notes", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(15.dp))

                Button(
                    onClick = { selectedFeature = "GPA" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00897B)
                    )
                ) {
                    Text("🎓 GPA / CGPA Calculator", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(15.dp))

                Button(
                    onClick = { selectedFeature = "StudyPlanner" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Text("📚 Smart Study Planner", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(15.dp))

                Button(
                    onClick = { selectedFeature = "DailyGoals" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7B1FA2)
                    )
                ) {
                    Text("🎯 Daily Goals", fontSize = 16.sp)
                }

                // NEW: POMODORO BUTTON

                Spacer(modifier = Modifier.height(15.dp))

                Button(
                    onClick = { selectedFeature = "Pomodoro" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE64A19)
                    )
                ) {
                    Text("🍅 Pomodoro Study Timer", fontSize = 16.sp)
                }
            }
        }
    }
}

/* ---------------- REUSABLE FEATURE CONTAINER ---------------- */

@Composable
fun FeatureContainer(
    buttonColor: Color,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
    ) {

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 12.dp
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor
            )
        ) {
            Text("← Back to Profile")
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

/* ---------------- COMMON SCREEN ---------------- */

@Composable
fun SimpleScreen(
    icon: String,
    title: String,
    description: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(icon, fontSize = 55.sp)

        Text(
            title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Text(
            description,
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}
