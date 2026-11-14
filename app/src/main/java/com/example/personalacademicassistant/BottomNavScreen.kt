
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
            FeatureContainer(Color(0xFF3F51B5), {
                onFeatureSelected("Profile")
            }) {
                NotesScreen()
            }
        }

        "GPA" -> {
            FeatureContainer(Color(0xFF00897B), {
                onFeatureSelected("Profile")
            }) {
                GPAScreen()
            }
        }

        "StudyPlanner" -> {
            FeatureContainer(Color(0xFF5E35B1), {
                onFeatureSelected("Profile")
            }) {
                StudyPlannerScreen()
            }
        }

        "DailyGoals" -> {
            FeatureContainer(Color(0xFF039BE5), {
                onFeatureSelected("Profile")
            }) {
                DailyGoalsScreen()
            }
        }

        "Pomodoro" -> {
            FeatureContainer(Color(0xFFE53935), {
                onFeatureSelected("Profile")
            }) {
                PomodoroScreen()
            }
        }

        "Reminders" -> {
            FeatureContainer(Color(0xFFFB8C00), {
                onFeatureSelected("Profile")
            }) {
                ReminderScreen()
            }
        }

        "AIAssistant" -> {
            FeatureContainer(Color(0xFF3949AB), {
                onFeatureSelected("Profile")
            }) {
                AIAssistantScreen()
            }
        }

        "AIDoubtSolver" -> {
            FeatureContainer(Color(0xFF5E35B1), {
                onFeatureSelected("Profile")
            }) {
                AIDoubtSolverScreen()
            }
        }

        "AICodeExplainer" -> {
            FeatureContainer(Color(0xFF1565C0), {
                onFeatureSelected("Profile")
            }) {
                AICodeExplainerScreen()
            }
        }

        "AIAssignmentHelper" -> {
            FeatureContainer(Color(0xFF00897B), {
                onFeatureSelected("Profile")
            }) {
                AIAssignmentHelperScreen()
            }
        }

        // FEATURE 30: AI MOCK INTERVIEW

        "AIMockInterview" -> {
            FeatureContainer(Color(0xFF3949AB), {
                onFeatureSelected("Profile")
            }) {
                AIMockInterviewScreen()
            }
        }

        "QuizGenerator" -> {
            FeatureContainer(Color(0xFF7B1FA2), {
                onFeatureSelected("Profile")
            }) {
                QuizGeneratorScreen()
            }
        }

        "Flashcards" -> {
            FeatureContainer(Color(0xFF00897B), {
                onFeatureSelected("Profile")
            }) {
                FlashcardScreen()
            }
        }

        "PDFSummarizer" -> {
            FeatureContainer(Color(0xFF1565C0), {
                onFeatureSelected("Profile")
            }) {
                PDFSummarizerScreen()
            }
        }

        "StudyStreaks" -> {
            FeatureContainer(Color(0xFFFF7043), {
                onFeatureSelected("Profile")
            }) {
                StudyStreakScreen()
            }
        }

        "AcademicResources" -> {
            FeatureContainer(Color(0xFF00897B), {
                onFeatureSelected("Profile")
            }) {
                AcademicResourceScreen()
            }
        }

        "PreviousQuestionPapers" -> {
            FeatureContainer(Color(0xFF3949AB), {
                onFeatureSelected("Profile")
            }) {
                PreviousQuestionPaperScreen()
            }
        }

        "SemesterProgress" -> {
            FeatureContainer(Color(0xFF00897B), {
                onFeatureSelected("Profile")
            }) {
                SemesterProgressScreen()
            }
        }

        "AIStudyTimetable" -> {
            FeatureContainer(Color(0xFF5E35B1), {
                onFeatureSelected("Profile")
            }) {
                AIStudyTimetableScreen()
            }
        }

        "StudyBuddy" -> {
            FeatureContainer(Color(0xFF00897B), {
                onFeatureSelected("Profile")
            }) {
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

                ProfileButton("📚 Academic Notes", Color(0xFF3F51B5)) {
                    onFeatureSelected("Notes")
                }

                ProfileButton("🎓 GPA / CGPA Calculator", Color(0xFF00897B)) {
                    onFeatureSelected("GPA")
                }

                ProfileButton("📅 Smart Study Planner", Color(0xFF5E35B1)) {
                    onFeatureSelected("StudyPlanner")
                }

                ProfileButton("🎯 Daily Goals", Color(0xFF039BE5)) {
                    onFeatureSelected("DailyGoals")
                }

                ProfileButton("⏱️ Pomodoro Study Timer", Color(0xFFE53935)) {
                    onFeatureSelected("Pomodoro")
                }

                ProfileButton("🔔 Notifications & Reminders", Color(0xFFFB8C00)) {
                    onFeatureSelected("Reminders")
                }

                ProfileButton("🤖 AI Academic Assistant", Color(0xFF3949AB)) {
                    onFeatureSelected("AIAssistant")
                }

                ProfileButton("💡 AI Academic Doubt Solver", Color(0xFF5E35B1)) {
                    onFeatureSelected("AIDoubtSolver")
                }

                ProfileButton("💻 AI Code Explainer", Color(0xFF1565C0)) {
                    onFeatureSelected("AICodeExplainer")
                }

                ProfileButton("📝 AI Assignment Helper", Color(0xFF00897B)) {
                    onFeatureSelected("AIAssignmentHelper")
                }

                ProfileButton("🎤 AI Mock Interview", Color(0xFF3949AB)) {
                    onFeatureSelected("AIMockInterview")
                }

                ProfileButton("📝 AI Quiz Generator", Color(0xFF7B1FA2)) {
                    onFeatureSelected("QuizGenerator")
                }

                ProfileButton("📚 AI Flashcard Generator", Color(0xFF00897B)) {
                    onFeatureSelected("Flashcards")
                }

                ProfileButton("📄 AI PDF Study Summarizer", Color(0xFF1565C0)) {
                    onFeatureSelected("PDFSummarizer")
                }

                ProfileButton("🔥 Study Streaks & Achievements", Color(0xFFFF7043)) {
                    onFeatureSelected("StudyStreaks")
                }

                ProfileButton("📚 Academic Resource Manager", Color(0xFF00897B)) {
                    onFeatureSelected("AcademicResources")
                }

                ProfileButton("📄 Previous Question Paper Manager", Color(0xFF3949AB)) {
                    onFeatureSelected("PreviousQuestionPapers")
                }

                ProfileButton("📊 Semester Progress Tracker", Color(0xFF00897B)) {
                    onFeatureSelected("SemesterProgress")
                }

                ProfileButton("🤖 AI Study Timetable Generator", Color(0xFF5E35B1)) {
                    onFeatureSelected("AIStudyTimetable")
                }

                ProfileButton("👥 Study Buddy & Collaboration", Color(0xFF00897B)) {
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
