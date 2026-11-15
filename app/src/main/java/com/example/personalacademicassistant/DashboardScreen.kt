
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DashboardBackground = Color(0xFFF5F6FC)
private val DashboardPrimary = Color(0xFF5365D9)
private val DashboardDark = Color(0xFF20294D)
private val DashboardMuted = Color(0xFF858CA3)
private val DashboardGreen = Color(0xFF12A889)
private val DashboardOrange = Color(0xFFE39A38)
private val DashboardRed = Color(0xFFE05A5A)

private data class DashboardClass(
    val id: String,
    val subject: String,
    val startTime: String,
    val endTime: String,
    val classroom: String
)

private data class DashboardTask(
    val id: String,
    val title: String,
    val subject: String,
    val dueDate: String,
    val priority: String
)

private fun parseClassTime(time: String): Int? {
    val formats = listOf("hh:mm a", "h:mm a", "HH:mm")

    for (pattern in formats) {
        try {
            val format = SimpleDateFormat(pattern, Locale.ENGLISH)
            format.isLenient = false
            val date = format.parse(time.trim()) ?: continue

            val calendar = Calendar.getInstance().apply {
                this.time = date
            }

            return calendar.get(Calendar.HOUR_OF_DAY) * 60 +
                    calendar.get(Calendar.MINUTE)
        } catch (_: Exception) {
            // Try the next supported time format.
        }
    }

    return null
}

private fun parseAssignmentDate(date: String): Date? {
    val formats = listOf(
        "dd MMM yyyy",
        "dd-MM-yyyy",
        "dd/MM/yyyy",
        "yyyy-MM-dd"
    )

    for (pattern in formats) {
        try {
            val format = SimpleDateFormat(pattern, Locale.ENGLISH)
            format.isLenient = false
            val parsed = format.parse(date.trim())

            if (parsed != null) return parsed
        } catch (_: Exception) {
            // Try the next supported date format.
        }
    }

    return null
}

@Composable
fun DashboardScreen() {

    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val userId = auth.currentUser?.uid

    var studentName by remember(userId) {
        mutableStateOf(
            auth.currentUser?.displayName
                ?: auth.currentUser?.email?.substringBefore("@")
                ?: "Student"
        )
    }

    var classes by remember { mutableStateOf<List<DashboardClass>>(emptyList()) }
    var tasks by remember { mutableStateOf<List<DashboardTask>>(emptyList()) }
    var attendancePercentage by remember { mutableIntStateOf(0) }

    var todayGoalCount by remember { mutableIntStateOf(0) }
    var todayCompletedGoals by remember { mutableIntStateOf(0) }
    var todayTargetHours by remember { mutableDoubleStateOf(0.0) }
    var todayCompletedHours by remember { mutableDoubleStateOf(0.0) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    var currentTime by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            currentTime = System.currentTimeMillis()
        }
    }

    val calendar = Calendar.getInstance().apply {
        timeInMillis = currentTime
    }

    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinutes = hour * 60 + calendar.get(Calendar.MINUTE)

    val greeting = when (hour) {
        in 5..11 -> "Good Morning!"
        in 12..16 -> "Good Afternoon!"
        in 17..20 -> "Good Evening!"
        else -> "Good Night!"
    }

    val greetingEmoji = when (hour) {
        in 5..11 -> "☀️"
        in 12..16 -> "🌤️"
        in 17..20 -> "🌆"
        else -> "🌙"
    }

    val todayName = SimpleDateFormat(
        "EEEE",
        Locale.ENGLISH
    ).format(Date(currentTime))

    val todayDateKey = SimpleDateFormat(
        "dd-MM-yyyy",
        Locale.ENGLISH
    ).format(Date(currentTime))

    val currentDate = SimpleDateFormat(
        "EEEE, dd MMMM yyyy",
        Locale.getDefault()
    ).format(Date(currentTime))

    val nextClass = classes
        .filter { entry ->
            val start = parseClassTime(entry.startTime)
            start != null && start >= currentMinutes
        }
        .minByOrNull { entry ->
            parseClassTime(entry.startTime) ?: Int.MAX_VALUE
        }

    val nearestAssignment = tasks
        .filter { task ->
            parseAssignmentDate(task.dueDate) != null
        }
        .minByOrNull { task ->
            parseAssignmentDate(task.dueDate)?.time ?: Long.MAX_VALUE
        }

    DisposableEffect(userId, todayName, todayDateKey) {

        if (userId == null) {
            errorMessage = "Please log in to view your dashboard."
            isLoading = false

            onDispose { }
        } else {

            isLoading = true
            errorMessage = ""

            var profileLoaded = false
            var timetableLoaded = false
            var assignmentsLoaded = false
            var attendanceLoaded = false
            var dailyGoalsLoaded = false

            fun updateLoading() {
                isLoading = !(
                        profileLoaded &&
                                timetableLoaded &&
                                assignmentsLoaded &&
                                attendanceLoaded &&
                                dailyGoalsLoaded
                        )
            }

            val listeners = mutableListOf<ListenerRegistration>()

            listeners.add(
                firestore.collection("users")
                    .document(userId)
                    .addSnapshotListener { snapshot, error ->

                        if (error != null) {
                            errorMessage =
                                error.message ?: "Profile loading failed"
                        } else {
                            studentName = snapshot?.getString("name")
                                ?: snapshot?.getString("fullName")
                                        ?: snapshot?.getString("username")
                                        ?: studentName
                        }

                        profileLoaded = true
                        updateLoading()
                    }
            )

            listeners.add(
                firestore.collection("users")
                    .document(userId)
                    .collection("timetable")
                    .addSnapshotListener { snapshot, error ->

                        if (error != null) {
                            errorMessage =
                                error.message ?: "Timetable loading failed"
                        } else {

                            val today = todayName.lowercase(Locale.ENGLISH)

                            classes = snapshot?.documents
                                ?.filter { document ->
                                    document.getString("day")
                                        ?.trim()
                                        ?.lowercase(Locale.ENGLISH) == today
                                }
                                ?.map { document ->
                                    DashboardClass(
                                        id = document.id,
                                        subject = document.getString("subject")
                                            ?: "Untitled class",
                                        startTime = document.getString("startTime")
                                            ?: "",
                                        endTime = document.getString("endTime")
                                            ?: "",
                                        classroom = document.getString("classroom")
                                            ?: ""
                                    )
                                }
                                ?.sortedBy {
                                    parseClassTime(it.startTime) ?: Int.MAX_VALUE
                                }
                                ?: emptyList()
                        }

                        timetableLoaded = true
                        updateLoading()
                    }
            )

            listeners.add(
                firestore.collection("users")
                    .document(userId)
                    .collection("assignments")
                    .addSnapshotListener { snapshot, error ->

                        if (error != null) {
                            errorMessage =
                                error.message ?: "Assignments loading failed"
                        } else {

                            tasks = snapshot?.documents
                                ?.filter {
                                    it.getBoolean("completed") != true
                                }
                                ?.map { document ->
                                    DashboardTask(
                                        id = document.id,
                                        title = document.getString("title")
                                            ?: "Untitled task",
                                        subject = document.getString("subject")
                                            ?: "",
                                        dueDate = document.getString("dueDate")
                                            ?: "No due date",
                                        priority = document.getString("priority")
                                            ?: "Normal"
                                    )
                                }
                                ?: emptyList()
                        }

                        assignmentsLoaded = true
                        updateLoading()
                    }
            )

            listeners.add(
                firestore.collection("users")
                    .document(userId)
                    .collection("attendance")
                    .addSnapshotListener { snapshot, error ->

                        if (error != null) {
                            errorMessage =
                                error.message ?: "Attendance loading failed"
                        } else {

                            val records = snapshot?.documents ?: emptyList()

                            val total = records.sumOf {
                                it.getLong("totalClasses") ?: 0L
                            }

                            val attended = records.sumOf {
                                it.getLong("attendedClasses") ?: 0L
                            }

                            attendancePercentage = if (total > 0) {
                                ((attended.toDouble() / total) * 100)
                                    .toInt()
                                    .coerceIn(0, 100)
                            } else {
                                0
                            }
                        }

                        attendanceLoaded = true
                        updateLoading()
                    }
            )

            listeners.add(
                firestore.collection("users")
                    .document(userId)
                    .collection("dailyGoals")
                    .addSnapshotListener { snapshot, error ->

                        if (error != null) {
                            errorMessage =
                                error.message ?: "Daily goals loading failed"

                            todayGoalCount = 0
                            todayCompletedGoals = 0
                            todayTargetHours = 0.0
                            todayCompletedHours = 0.0

                        } else {

                            val todayGoals = snapshot?.documents
                                ?.filter {
                                    it.getString("date")?.trim() == todayDateKey
                                }
                                ?: emptyList()

                            todayGoalCount = todayGoals.size

                            todayCompletedGoals = todayGoals.count {
                                it.getBoolean("completed") == true
                            }

                            todayTargetHours = todayGoals.sumOf {
                                it.getDouble("targetHours")
                                    ?: (it.getLong("targetHours")?.toDouble() ?: 0.0)
                            }

                            todayCompletedHours = todayGoals.sumOf {
                                it.getDouble("completedHours")
                                    ?: (it.getLong("completedHours")?.toDouble() ?: 0.0)
                            }
                        }

                        dailyGoalsLoaded = true
                        updateLoading()
                    }
            )

            onDispose {
                listeners.forEach { it.remove() }
            }
        }
    }

    val remainingHours = (todayTargetHours - todayCompletedHours)
        .coerceAtLeast(0.0)

    val studyProgress = if (todayTargetHours > 0.0) {
        (todayCompletedHours / todayTargetHours)
            .toFloat()
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardBackground)
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {

        item {
            DashboardWelcomeCard(
                greeting = greeting,
                emoji = greetingEmoji,
                studentName = studentName,
                currentDate = currentDate
            )
        }

        item {
            DashboardSectionTitle(
                title = "Your Overview",
                subtitle = "A quick look at your academic day"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardStatCard(
                    title = "Today's Classes",
                    value = classes.size.toString(),
                    emoji = "📚",
                    color = DashboardPrimary,
                    modifier = Modifier.weight(1f)
                )

                DashboardStatCard(
                    title = "Pending Tasks",
                    value = tasks.size.toString(),
                    emoji = "✅",
                    color = DashboardGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            DashboardAttendanceCard(attendancePercentage)
        }

        item {
            DashboardSectionTitle(
                title = "Today's Study Progress",
                subtitle = "Keep moving towards your daily goals"
            )
        }

        item {
            if (isLoading) {
                DashboardLoadingCard()
            } else if (todayGoalCount == 0) {
                DashboardEmptyCard(
                    emoji = "🎯",
                    title = "No study goals for today",
                    description = "Add goals in Daily Goals to track your study progress here."
                )
            } else {
                DashboardStudyProgressCard(
                    targetHours = todayTargetHours,
                    completedHours = todayCompletedHours,
                    remainingHours = remainingHours,
                    completedGoals = todayCompletedGoals,
                    totalGoals = todayGoalCount,
                    progress = studyProgress
                )
            }
        }

        item {
            DashboardSectionTitle(
                title = "Next Upcoming Class",
                subtitle = "Your next class for today"
            )
        }

        item {
            if (isLoading) {
                DashboardLoadingCard()
            } else if (nextClass == null) {
                DashboardEmptyCard(
                    emoji = "📚",
                    title = "No upcoming classes",
                    description = "You have no remaining classes scheduled for today."
                )
            } else {
                DashboardNextClassCard(nextClass!!)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Today's Schedule",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardDark
                )

                DashboardCountChip(
                    text = "${classes.size} classes",
                    color = DashboardPrimary
                )
            }
        }

        if (isLoading) {
            item { DashboardLoadingCard() }
        } else if (classes.isEmpty()) {
            item {
                DashboardEmptyCard(
                    emoji = "📅",
                    title = "No classes scheduled",
                    description = "Your $todayName timetable is empty. Add a class to see it here."
                )
            }
        } else {
            items(classes, key = { it.id }) { entry ->
                DashboardClassCard(entry)
            }
        }

        item {
            DashboardSectionTitle(
                title = "Nearest Assignment Deadline",
                subtitle = "Stay ahead of your submissions"
            )
        }

        item {
            if (isLoading) {
                DashboardLoadingCard()
            } else if (nearestAssignment == null) {
                DashboardEmptyCard(
                    emoji = "🎉",
                    title = "No assignment deadlines found",
                    description = "Add a pending assignment with a valid due date to see it here."
                )
            } else {
                DashboardDeadlineCard(nearestAssignment!!)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Upcoming Tasks",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardDark
                )

                DashboardCountChip(
                    text = "${tasks.size} pending",
                    color = DashboardGreen
                )
            }
        }

        if (!isLoading && tasks.isEmpty()) {
            item {
                DashboardEmptyCard(
                    emoji = "🎉",
                    title = "You're all caught up!",
                    description = "Your pending assignments will appear here."
                )
            }
        } else if (!isLoading) {
            items(tasks, key = { it.id }) { task ->
                DashboardTaskCard(task)
            }
        }

        if (errorMessage.isNotBlank()) {
            item {
                Text(
                    text = errorMessage,
                    color = DashboardRed,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun DashboardWelcomeCard(
    greeting: String,
    emoji: String,
    studentName: String,
    currentDate: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF293B91),
                            Color(0xFF5365D9),
                            Color(0xFF8998F3)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "PERSONAL ACADEMIC ASSISTANT",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(19.dp))

                        Text(
                            "$greeting $emoji",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(7.dp))

                        Text(
                            "Welcome back,",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.82f)
                        )

                        Text(
                            studentName,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎓", fontSize = 35.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(21.dp))

                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 13.dp,
                            vertical = 9.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📅", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            currentDate,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSectionTitle(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = DashboardDark
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            subtitle,
            fontSize = 12.sp,
            color = DashboardMuted
        )
    }
}

@Composable
private fun DashboardCountChip(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(30.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(
                horizontal = 11.dp,
                vertical = 7.dp
            ),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DashboardStatCard(
    title: String,
    value: String,
    emoji: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(148.dp),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(17.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = color.copy(alpha = 0.11f)
                ) {
                    Text(
                        emoji,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 21.sp
                    )
                }

                Text(
                    "●",
                    color = color.copy(alpha = 0.35f),
                    fontSize = 12.sp
                )
            }

            Text(
                value,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )

            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = DashboardMuted
            )
        }
    }
}

@Composable
private fun DashboardAttendanceCard(percentage: Int) {
    val progress = (percentage / 100f).coerceIn(0f, 1f)
    val statusColor = if (percentage >= 75) DashboardGreen else DashboardOrange

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Overall Attendance",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "Your academic attendance",
                        fontSize = 12.sp,
                        color = DashboardMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFFEDEFFF)
                ) {
                    Text(
                        "📊",
                        modifier = Modifier.padding(11.dp),
                        fontSize = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(19.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "$percentage%",
                    fontSize = 33.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DashboardPrimary
                )

                Text(
                    if (percentage >= 75) "Good progress ✓"
                    else "Needs improvement",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(11.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(11.dp)
                    .background(
                        Color(0xFFEAECF5),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    DashboardPrimary,
                                    DashboardGreen
                                )
                            ),
                            RoundedCornerShape(20.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "Target attendance: 75%",
                fontSize = 11.sp,
                color = DashboardMuted
            )
        }
    }
}

@Composable
private fun DashboardStudyProgressCard(
    targetHours: Double,
    completedHours: Double,
    remainingHours: Double,
    completedGoals: Int,
    totalGoals: Int,
    progress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Study time completed",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardDark
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        "$completedGoals of $totalGoals goals completed",
                        fontSize = 12.sp,
                        color = DashboardMuted
                    )
                }

                Text("🎯", fontSize = 29.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    "${(progress * 100).toInt()}%",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DashboardPrimary
                )

                Text(
                    "${"%.1f".format(completedHours)} / ${"%.1f".format(targetHours)} hrs",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = DashboardPrimary,
                trackColor = Color(0xFFEAECF5)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Completed",
                        fontSize = 11.sp,
                        color = DashboardMuted
                    )

                    Text(
                        "${"%.1f".format(completedHours)} hrs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardGreen
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Remaining",
                        fontSize = 11.sp,
                        color = DashboardMuted
                    )

                    Text(
                        "${"%.1f".format(remainingHours)} hrs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardNextClassCard(entry: DashboardClass) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(19.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFEDEFFF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("📚", fontSize = 29.sp)
                }
            }

            Spacer(modifier = Modifier.width(15.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "UP NEXT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = DashboardPrimary
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    entry.subject,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardDark
                )

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    "⏰ ${entry.startTime} - ${entry.endTime}",
                    fontSize = 12.sp,
                    color = DashboardMuted
                )

                if (entry.classroom.isNotBlank()) {
                    Text(
                        "🏫 ${entry.classroom}",
                        fontSize = 12.sp,
                        color = DashboardMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardClassCard(entry: DashboardClass) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(67.dp)
                    .background(
                        DashboardPrimary,
                        RoundedCornerShape(10.dp)
                    )
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.subject,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardDark
                )

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    "⏰ ${entry.startTime} - ${entry.endTime}",
                    fontSize = 12.sp,
                    color = DashboardMuted
                )

                if (entry.classroom.isNotBlank()) {
                    Text(
                        "🏫 ${entry.classroom}",
                        fontSize = 12.sp,
                        color = DashboardMuted
                    )
                }
            }

            Text("›", fontSize = 27.sp, color = DashboardPrimary)
        }
    }
}

@Composable
private fun DashboardDeadlineCard(task: DashboardTask) {
    val dueDate = parseAssignmentDate(task.dueDate)

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    val deadlineStatus = when {
        dueDate == null -> "Date unavailable"
        dueDate.before(today) -> "Overdue"
        else -> "Upcoming deadline"
    }

    val statusColor = if (deadlineStatus == "Overdue") {
        DashboardRed
    } else {
        DashboardGreen
    }

    val priorityColor = dashboardPriorityColor(task.priority)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFFFFF2E7)
                ) {
                    Text(
                        "📝",
                        modifier = Modifier.padding(11.dp),
                        fontSize = 23.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardDark
                    )

                    if (task.subject.isNotBlank()) {
                        Text(
                            task.subject,
                            fontSize = 12.sp,
                            color = DashboardMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "DUE DATE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardMuted
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        task.dueDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardDark
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "PRIORITY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardMuted
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        task.priority,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            DashboardCountChip(
                text = deadlineStatus,
                color = statusColor
            )
        }
    }
}

@Composable
private fun DashboardTaskCard(task: DashboardTask) {
    val priorityColor = dashboardPriorityColor(task.priority)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = Color(0xFFEDEFFF)
                ) {
                    Text(
                        "📋",
                        modifier = Modifier.padding(9.dp),
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    task.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardDark
                )
            }

            if (task.subject.isNotBlank()) {
                Text(
                    "📚 ${task.subject}",
                    modifier = Modifier.padding(top = 11.dp),
                    fontSize = 12.sp,
                    color = DashboardMuted
                )
            }

            Spacer(modifier = Modifier.height(13.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "📅 ${task.dueDate}",
                    fontSize = 12.sp,
                    color = DashboardMuted
                )

                DashboardCountChip(
                    text = task.priority,
                    color = priorityColor
                )
            }
        }
    }
}

private fun dashboardPriorityColor(priority: String): Color {
    return when (priority.lowercase()) {
        "high" -> DashboardRed
        "medium" -> DashboardOrange
        "low" -> DashboardGreen
        else -> DashboardPrimary
    }
}

@Composable
private fun DashboardEmptyCard(
    emoji: String,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFF0F1FA)
            ) {
                Text(
                    emoji,
                    modifier = Modifier.padding(15.dp),
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(13.dp))

            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DashboardDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                description,
                fontSize = 12.sp,
                color = DashboardMuted
            )
        }
    }
}

@Composable
private fun DashboardLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = DashboardPrimary,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Loading your academic information...",
                fontSize = 12.sp,
                color = DashboardMuted
            )
        }
    }
}
