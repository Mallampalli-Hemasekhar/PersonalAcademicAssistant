
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun BottomNavScreen() {

    var selectedTab by remember { mutableStateOf("Home") }
    var selectedFeature by remember { mutableStateOf("Profile") }

    val navItems = listOf(
        "Home",
        "Schedule",
        "Tasks",
        "Progress",
        "Profile"
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item,
                        onClick = {
                            selectedTab = item
                            selectedFeature = "Profile"
                        },
                        icon = {
                            Text(
                                text = when (item) {
                                    "Home" -> "🏠"
                                    "Schedule" -> "📅"
                                    "Tasks" -> "📝"
                                    "Progress" -> "📊"
                                    else -> "👤"
                                },
                                fontSize = 20.sp
                            )
                        },
                        label = {
                            Text(item)
                        }
                    )
                }
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when (selectedTab) {

                "Home" -> DashboardScreen()

                "Schedule" -> ScheduleScreen()

                "Tasks" -> TasksScreen()

                "Progress" -> ProgressScreen()

                "Profile" -> {
                    ProfileScreen(
                        selectedFeature = selectedFeature,
                        onFeatureSelected = {
                            selectedFeature = it
                        }
                    )
                }
            }
        }
    }
}

// --------------------------------------------------
// SCHEDULE SCREEN
// --------------------------------------------------

@Composable
fun ScheduleScreen() {

    var selectedSchedule by remember {
        mutableStateOf("Timetable")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .padding(16.dp)
    ) {

        Text(
            text = "My Schedule",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                selectedSchedule = "Timetable"
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedSchedule == "Timetable")
                    Color(0xFF3F51B5)
                else
                    Color.Gray
            )
        ) {
            Text("📅 Timetable")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                selectedSchedule = "Exams"
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedSchedule == "Exams")
                    Color(0xFF3F51B5)
                else
                    Color.Gray
            )
        ) {
            Text("📝 Exams")
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (selectedSchedule) {
            "Timetable" -> TimetableScreen()
            "Exams" -> ExamScreen()
        }
    }
}

// --------------------------------------------------
// TASKS SCREEN
// --------------------------------------------------

@Composable
fun TasksScreen() {
    AssignmentScreen()
}

// --------------------------------------------------
// PROGRESS SCREEN
// --------------------------------------------------

@Composable
fun ProgressScreen() {

    var selectedProgress by remember {
        mutableStateOf("Attendance")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .padding(16.dp)
    ) {

        Text(
            text = "My Progress",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                selectedProgress = "Attendance"
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedProgress == "Attendance")
                    Color(0xFF3F51B5)
                else
                    Color.Gray
            )
        ) {
            Text("📊 Attendance")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                selectedProgress = "Performance"
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedProgress == "Performance")
                    Color(0xFF3F51B5)
                else
                    Color.Gray
            )
        ) {
            Text("📈 Performance Analytics")
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (selectedProgress) {
            "Attendance" -> AttendanceScreen()
            "Performance" -> PerformanceAnalyticsScreen()
        }
    }
}

// --------------------------------------------------
// PROFILE SCREEN AND FEATURE NAVIGATION
// --------------------------------------------------

@Composable
fun ProfileScreen(
    selectedFeature: String,
    onFeatureSelected: (String) -> Unit
) {

    when (selectedFeature) {

        "Notes" -> {
            FeatureContainer(
                buttonColor = Color(0xFF3F51B5),
                onBack = { onFeatureSelected("Profile") }
            ) {
                NotesScreen()
            }
        }

        "GPA" -> {
            FeatureContainer(
                buttonColor = Color(0xFF00897B),
                onBack = { onFeatureSelected("Profile") }
            ) {
                GPAScreen()
            }
        }

        "StudyPlanner" -> {
            FeatureContainer(
                buttonColor = Color(0xFF5E35B1),
                onBack = { onFeatureSelected("Profile") }
            ) {
                StudyPlannerScreen()
            }
        }

        "DailyGoals" -> {
            FeatureContainer(
                buttonColor = Color(0xFF039BE5),
                onBack = { onFeatureSelected("Profile") }
            ) {
                DailyGoalsScreen()
            }
        }

        "Pomodoro" -> {
            FeatureContainer(
                buttonColor = Color(0xFFE53935),
                onBack = { onFeatureSelected("Profile") }
            ) {
                PomodoroScreen()
            }
        }

        "Reminders" -> {
            FeatureContainer(
                buttonColor = Color(0xFFFB8C00),
                onBack = { onFeatureSelected("Profile") }
            ) {
                ReminderScreen()
            }
        }

        "AIAssistant" -> {
            FeatureContainer(
                buttonColor = Color(0xFF3949AB),
                onBack = { onFeatureSelected("Profile") }
            ) {
                AIAssistantScreen()
            }
        }

        "QuizGenerator" -> {
            FeatureContainer(
                buttonColor = Color(0xFF7B1FA2),
                onBack = { onFeatureSelected("Profile") }
            ) {
                QuizGeneratorScreen()
            }
        }

        // FEATURE 19: AI FLASHCARD GENERATOR
        "Flashcards" -> {
            FeatureContainer(
                buttonColor = Color(0xFF00897B),
                onBack = { onFeatureSelected("Profile") }
            ) {
                FlashcardScreen()
            }
        }

        // FEATURE 20: AI PDF STUDY SUMMARIZER
        "PDFSummarizer" -> {
            FeatureContainer(
                buttonColor = Color(0xFF1565C0),
                onBack = { onFeatureSelected("Profile") }
            ) {
                PDFSummarizerScreen()
            }
        }

        // FEATURE 21: STUDY STREAKS AND ACHIEVEMENTS
        "StudyStreaks" -> {
            FeatureContainer(
                buttonColor = Color(0xFFFF7043),
                onBack = { onFeatureSelected("Profile") }
            ) {
                StudyStreakScreen()
            }
        }

        // FEATURE 22: ACADEMIC RESOURCE MANAGER
        "AcademicResources" -> {
            FeatureContainer(
                buttonColor = Color(0xFF00897B),
                onBack = { onFeatureSelected("Profile") }
            ) {
                AcademicResourceScreen()
            }
        }

        // FEATURE 23: PREVIOUS QUESTION PAPER MANAGER
        "PreviousQuestionPapers" -> {
            FeatureContainer(
                buttonColor = Color(0xFF3949AB),
                onBack = { onFeatureSelected("Profile") }
            ) {
                PreviousQuestionPaperScreen()
            }
        }

        // FEATURE 24: SEMESTER PROGRESS TRACKER
        "SemesterProgress" -> {
            FeatureContainer(
                buttonColor = Color(0xFF00897B),
                onBack = { onFeatureSelected("Profile") }
            ) {
                SemesterProgressScreen()
            }
        }

        // FEATURE 25: AI STUDY TIMETABLE GENERATOR
        "AIStudyTimetable" -> {
            FeatureContainer(
                buttonColor = Color(0xFF5E35B1),
                onBack = { onFeatureSelected("Profile") }
            ) {
                AIStudyTimetableScreen()
            }
        }

        // FEATURE 26: STUDY BUDDY AND COLLABORATION
        "StudyBuddy" -> {
            FeatureContainer(
                buttonColor = Color(0xFF00897B),
                onBack = { onFeatureSelected("Profile") }
            ) {
                StudyBuddyScreen()
            }
        }

        else -> {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F7FB))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "My Profile",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172554)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Personal Academic Assistant",
                    fontSize = 15.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(25.dp))

                ProfileButton(
                    text = "📚 Academic Notes",
                    color = Color(0xFF3F51B5)
                ) {
                    onFeatureSelected("Notes")
                }

                ProfileButton(
                    text = "🎓 GPA / CGPA Calculator",
                    color = Color(0xFF00897B)
                ) {
                    onFeatureSelected("GPA")
                }

                ProfileButton(
                    text = "📅 Smart Study Planner",
                    color = Color(0xFF5E35B1)
                ) {
                    onFeatureSelected("StudyPlanner")
                }

                ProfileButton(
                    text = "🎯 Daily Goals",
                    color = Color(0xFF039BE5)
                ) {
                    onFeatureSelected("DailyGoals")
                }

                ProfileButton(
                    text = "⏱️ Pomodoro Study Timer",
                    color = Color(0xFFE53935)
                ) {
                    onFeatureSelected("Pomodoro")
                }

                ProfileButton(
                    text = "🔔 Notifications & Reminders",
                    color = Color(0xFFFB8C00)
                ) {
                    onFeatureSelected("Reminders")
                }

                ProfileButton(
                    text = "🤖 AI Academic Assistant",
                    color = Color(0xFF3949AB)
                ) {
                    onFeatureSelected("AIAssistant")
                }

                ProfileButton(
                    text = "📝 AI Quiz Generator",
                    color = Color(0xFF7B1FA2)
                ) {
                    onFeatureSelected("QuizGenerator")
                }

                ProfileButton(
                    text = "📚 AI Flashcard Generator",
                    color = Color(0xFF00897B)
                ) {
                    onFeatureSelected("Flashcards")
                }

                ProfileButton(
                    text = "📄 AI PDF Study Summarizer",
                    color = Color(0xFF1565C0)
                ) {
                    onFeatureSelected("PDFSummarizer")
                }

                ProfileButton(
                    text = "🔥 Study Streaks & Achievements",
                    color = Color(0xFFFF7043)
                ) {
                    onFeatureSelected("StudyStreaks")
                }

                ProfileButton(
                    text = "📚 Academic Resource Manager",
                    color = Color(0xFF00897B)
                ) {
                    onFeatureSelected("AcademicResources")
                }

                ProfileButton(
                    text = "📄 Previous Question Paper Manager",
                    color = Color(0xFF3949AB)
                ) {
                    onFeatureSelected("PreviousQuestionPapers")
                }

                // FEATURE 24 BUTTON
                ProfileButton(
                    text = "📊 Semester Progress Tracker",
                    color = Color(0xFF00897B)
                ) {
                    onFeatureSelected("SemesterProgress")
                }

                // FEATURE 25 BUTTON
                ProfileButton(
                    text = "🤖 AI Study Timetable Generator",
                    color = Color(0xFF5E35B1)
                ) {
                    onFeatureSelected("AIStudyTimetable")
                }

                // FEATURE 26 BUTTON
                ProfileButton(
                    text = "👥 Study Buddy & Collaboration",
                    color = Color(0xFF00897B)
                ) {
                    onFeatureSelected("StudyBuddy")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// --------------------------------------------------
// REUSABLE PROFILE BUTTON
// --------------------------------------------------

@Composable
fun ProfileButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp
        )
    }
}

// --------------------------------------------------
// REUSABLE FEATURE CONTAINER
// --------------------------------------------------

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
            modifier = Modifier.padding(12.dp),
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
