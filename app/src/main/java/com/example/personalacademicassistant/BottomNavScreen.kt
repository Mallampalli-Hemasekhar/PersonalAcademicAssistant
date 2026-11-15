
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun BottomNavScreen(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Home") }
    var selectedFeature by remember { mutableStateOf("Profile") }

    val navItems = listOf(
        "Home" to Icons.Default.Home,
        "Schedule" to Icons.Default.CalendarMonth,
        "Tasks" to Icons.Default.Assignment,
        "Progress" to Icons.Default.BarChart,
        "Profile" to Icons.Default.Person
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                navItems.forEach { (item, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == item,
                        onClick = {
                            selectedTab = item
                            selectedFeature = "Profile"
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = item
                            )
                        },
                        label = {
                            Text(
                                text = item,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == item)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                "Profile" -> ProfileScreen(
                    selectedFeature = selectedFeature,
                    onFeatureSelected = {
                        selectedFeature = it
                    },
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    onLogout = onLogout
                )
            }
        }
    }
}

// --------------------------------------------------
// REUSABLE MODERN SELECTION BUTTON
// --------------------------------------------------

@Composable
fun ModernSelectionButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
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
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "My Schedule",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Manage your classes and examinations",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ModernSelectionButton(
                    title = "Timetable",
                    icon = Icons.Default.CalendarMonth,
                    selected = selectedSchedule == "Timetable",
                    onClick = { selectedSchedule = "Timetable" }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                ModernSelectionButton(
                    title = "Exams",
                    icon = Icons.Default.School,
                    selected = selectedSchedule == "Exams",
                    onClick = { selectedSchedule = "Exams" }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "My Progress",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Monitor your academic performance",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ModernSelectionButton(
                    title = "Attendance",
                    icon = Icons.Default.EventAvailable,
                    selected = selectedProgress == "Attendance",
                    onClick = { selectedProgress = "Attendance" }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                ModernSelectionButton(
                    title = "Analytics",
                    icon = Icons.Default.Insights,
                    selected = selectedProgress == "Performance",
                    onClick = { selectedProgress = "Performance" }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
    onFeatureSelected: (String) -> Unit,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    when (selectedFeature) {

        "Notes" -> FeatureContainer(Color(0xFF3F51B5), {
            onFeatureSelected("Profile")
        }) { NotesScreen() }

        "GPA" -> FeatureContainer(Color(0xFF00897B), {
            onFeatureSelected("Profile")
        }) { GPAScreen() }

        "StudyPlanner" -> FeatureContainer(Color(0xFF5E35B1), {
            onFeatureSelected("Profile")
        }) { StudyPlannerScreen() }

        "DailyGoals" -> FeatureContainer(Color(0xFF039BE5), {
            onFeatureSelected("Profile")
        }) { DailyGoalsScreen() }

        "Pomodoro" -> FeatureContainer(Color(0xFFE53935), {
            onFeatureSelected("Profile")
        }) { PomodoroScreen() }

        "Reminders" -> FeatureContainer(Color(0xFFFB8C00), {
            onFeatureSelected("Profile")
        }) { ReminderScreen() }

        "AIAssistant" -> FeatureContainer(Color(0xFF3949AB), {
            onFeatureSelected("Profile")
        }) { AIAssistantScreen() }

        "AIDoubtSolver" -> FeatureContainer(Color(0xFF5E35B1), {
            onFeatureSelected("Profile")
        }) { AIDoubtSolverScreen() }

        "AICodeExplainer" -> FeatureContainer(Color(0xFF1565C0), {
            onFeatureSelected("Profile")
        }) { AICodeExplainerScreen() }

        "AIAssignmentHelper" -> FeatureContainer(Color(0xFF00897B), {
            onFeatureSelected("Profile")
        }) { AIAssignmentHelperScreen() }

        "AIMockInterview" -> FeatureContainer(Color(0xFF3949AB), {
            onFeatureSelected("Profile")
        }) { AIMockInterviewScreen() }

        "QuizGenerator" -> FeatureContainer(Color(0xFF7B1FA2), {
            onFeatureSelected("Profile")
        }) { QuizGeneratorScreen() }

        "Flashcards" -> FeatureContainer(Color(0xFF00897B), {
            onFeatureSelected("Profile")
        }) { FlashcardScreen() }

        "PDFSummarizer" -> FeatureContainer(Color(0xFF1565C0), {
            onFeatureSelected("Profile")
        }) { PDFSummarizerScreen() }

        "StudyStreaks" -> FeatureContainer(Color(0xFFFF7043), {
            onFeatureSelected("Profile")
        }) { StudyStreakScreen() }

        "AcademicResources" -> FeatureContainer(Color(0xFF00897B), {
            onFeatureSelected("Profile")
        }) { AcademicResourceScreen() }

        "PreviousQuestionPapers" -> FeatureContainer(Color(0xFF3949AB), {
            onFeatureSelected("Profile")
        }) { PreviousQuestionPaperScreen() }

        "SemesterProgress" -> FeatureContainer(Color(0xFF00897B), {
            onFeatureSelected("Profile")
        }) { SemesterProgressScreen() }

        "AIStudyTimetable" -> FeatureContainer(Color(0xFF5E35B1), {
            onFeatureSelected("Profile")
        }) { AIStudyTimetableScreen() }

        "StudyBuddy" -> FeatureContainer(Color(0xFF00897B), {
            onFeatureSelected("Profile")
        }) { StudyBuddyScreen() }

        else -> ProfileHomeContent(
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            onFeatureSelected = onFeatureSelected,
            onLogout = onLogout
        )
    }
}

// --------------------------------------------------
// PROFILE HOME CONTENT
// --------------------------------------------------

@Composable
fun ProfileHomeContent(
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    onFeatureSelected: (String) -> Unit,
    onLogout: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val studentName = user?.displayName
        ?: user?.email?.substringBefore("@")
        ?: "Student"
    val studentEmail = user?.email ?: "No email available"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            text = "My Profile",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Manage your academic account and settings",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Student",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = studentName,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = studentEmail,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = "Student Account",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        ProfileSectionTitle(
            title = "Appearance",
            subtitle = "Customize how your app looks"
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(15.dp)) {
                ProfileThemeButton(
                    title = "☀️ Light Mode",
                    description = "Use the light appearance",
                    selected = themeMode == "Light",
                    onClick = { onThemeModeChange("Light") }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 5.dp)
                )

                ProfileThemeButton(
                    title = "🌙 Dark Mode",
                    description = "Use the dark appearance",
                    selected = themeMode == "Dark",
                    onClick = { onThemeModeChange("Dark") }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 5.dp)
                )

                ProfileThemeButton(
                    title = "📱 System Default",
                    description = "Follow your device settings",
                    selected = themeMode == "System",
                    onClick = { onThemeModeChange("System") }
                )
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        ProfileSectionTitle(
            title = "Academic Tools",
            subtitle = "Manage your everyday academic activities"
        )

        Spacer(modifier = Modifier.height(10.dp))

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

        Spacer(modifier = Modifier.height(20.dp))

        ProfileSectionTitle(
            title = "AI Study Tools",
            subtitle = "Learn smarter with your AI assistant"
        )

        Spacer(modifier = Modifier.height(10.dp))

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
        ProfileButton("🤖 AI Study Timetable Generator", Color(0xFF5E35B1)) {
            onFeatureSelected("AIStudyTimetable")
        }

        Spacer(modifier = Modifier.height(20.dp))

        ProfileSectionTitle(
            title = "Study Management",
            subtitle = "Track your learning and academic progress"
        )

        Spacer(modifier = Modifier.height(10.dp))

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
        ProfileButton("👥 Study Buddy & Collaboration", Color(0xFF00897B)) {
            onFeatureSelected("StudyBuddy")
        }

        Spacer(modifier = Modifier.height(25.dp))

        ProfileSectionTitle(
            title = "About",
            subtitle = "Your personal academic companion"
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "🎓 Personal Academic Assistant",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Organize your studies, manage your schedule, track your progress, and learn smarter with AI-powered academic tools.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Version 1.0",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        ProfileSectionTitle(
            title = "Account",
            subtitle = "Manage your signed-in account"
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Sign out of your account",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "You can log in again anytime using your email and password.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(15.dp))

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Logout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// --------------------------------------------------
// PROFILE SECTION TITLE
// --------------------------------------------------

@Composable
fun ProfileSectionTitle(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// --------------------------------------------------
// PROFILE THEME BUTTON
// --------------------------------------------------

@Composable
fun ProfileThemeButton(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = 8.dp,
            vertical = 10.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
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
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        ),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 14.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
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
            .background(MaterialTheme.colorScheme.background)
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
