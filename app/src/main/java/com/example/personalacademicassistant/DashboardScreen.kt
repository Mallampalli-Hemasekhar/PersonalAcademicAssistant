
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
            // Try another supported format.
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
            // Try another supported format.
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
        in 5..11 -> "Good Morning! ☀️"
        in 12..16 -> "Good Afternoon! 🌤️"
        in 17..20 -> "Good Evening! 🌆"
        else -> "Good Night! 🌙"
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
            .background(Color(0xFFF4F6FC))
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        item {
            Spacer(modifier = Modifier.height(5.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF263C91),
                                    Color(0xFF536DE8),
                                    Color(0xFF8297F5)
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column {

                        Text(
                            "PERSONAL ACADEMIC ASSISTANT",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            greeting,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(7.dp))

                        Text(
                            "Welcome back, $studentName",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.95f)
                        )

                        Spacer(modifier = Modifier.height(15.dp))

                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(30.dp)
                        ) {
                            Text(
                                "📅  $currentDate",
                                modifier = Modifier.padding(
                                    horizontal = 13.dp,
                                    vertical = 8.dp
                                ),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Your Overview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF18234A)
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
                    color = Color(0xFF536DE8),
                    modifier = Modifier.weight(1f)
                )

                DashboardStatCard(
                    title = "Pending Tasks",
                    value = tasks.size.toString(),
                    emoji = "✅",
                    color = Color(0xFF12A889),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            DashboardAttendanceCard(attendancePercentage)
        }

        item {
            Text(
                "Today's Study Progress",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF18234A)
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
            Text(
                "Next Upcoming Class",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF18234A)
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF18234A)
                )

                Text(
                    "${classes.size} classes",
                    fontSize = 12.sp,
                    color = Color(0xFF536DE8)
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
            Text(
                "Nearest Assignment Deadline",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF18234A)
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF18234A)
                )

                Text(
                    "${tasks.size} pending",
                    fontSize = 12.sp,
                    color = Color(0xFF12A889)
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
                    color = Color.Red,
                    fontSize = 13.sp
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(15.dp))
        }
    }
}

@Composable
private fun DashboardNextClassCard(entry: DashboardClass) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(55.dp)
                    .background(
                        Color(0xFFE9EDFF),
                        RoundedCornerShape(17.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📚", fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(15.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.subject,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF18234A)
                )

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    "⏰ ${entry.startTime} - ${entry.endTime}",
                    fontSize = 13.sp,
                    color = Color(0xFF536DE8)
                )

                if (entry.classroom.isNotBlank()) {
                    Text(
                        "🏫 ${entry.classroom}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
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
        Color(0xFFE05A5A)
    } else {
        Color(0xFF12A889)
    }

    val priorityColor = when (task.priority.lowercase()) {
        "high" -> Color(0xFFE05A5A)
        "medium" -> Color(0xFFE39A38)
        "low" -> Color(0xFF12A889)
        else -> Color(0xFF536DE8)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📝", fontSize = 30.sp)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF18234A)
                    )

                    if (task.subject.isNotBlank()) {
                        Text(
                            task.subject,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Due date", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        task.dueDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF18234A)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Priority", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        task.priority,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    deadlineStatus,
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 7.dp
                    ),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
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
        shape = RoundedCornerShape(22.dp),
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
                        "Study time completed",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF18234A)
                    )

                    Text(
                        "$completedGoals of $totalGoals goals completed",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Text("🎯", fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${(progress * 100).toInt()}%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF536DE8)
                )

                Text(
                    "${"%.1f".format(completedHours)} / ${"%.1f".format(targetHours)} hrs",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF12A889)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = Color(0xFF536DE8),
                trackColor = Color(0xFFE9ECF5)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Completed", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "${"%.1f".format(completedHours)} hrs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF12A889)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Remaining", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "${"%.1f".format(remainingHours)} hrs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE39A38)
                    )
                }
            }
        }
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
        modifier = modifier.height(145.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(17.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(emoji, fontSize = 27.sp)
            Text(
                value,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(title, fontSize = 13.sp, color = Color(0xFF737B91))
        }
    }
}

@Composable
private fun DashboardAttendanceCard(percentage: Int) {
    val progress = (percentage / 100f).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
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
                        color = Color(0xFF18234A)
                    )

                    Text(
                        "Your academic attendance",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Text("📊", fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$percentage%",
                    fontSize = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF536DE8)
                )

                Text(
                    if (percentage >= 75) "Good progress ✓"
                    else "Needs improvement",
                    fontSize = 12.sp,
                    color = if (percentage >= 75) {
                        Color(0xFF12A889)
                    } else {
                        Color(0xFFE58A35)
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(
                        Color(0xFFE9ECF5),
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
                                    Color(0xFF536DE8),
                                    Color(0xFF12A889)
                                )
                            ),
                            RoundedCornerShape(20.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                "Target attendance: 75%",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun DashboardClassCard(entry: DashboardClass) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
                    .height(65.dp)
                    .background(
                        Color(0xFF536DE8),
                        RoundedCornerShape(10.dp)
                    )
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.subject,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF18234A)
                )

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    "⏰ ${entry.startTime} - ${entry.endTime}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                if (entry.classroom.isNotBlank()) {
                    Text(
                        "🏫 ${entry.classroom}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTaskCard(task: DashboardTask) {
    val priorityColor = when (task.priority.lowercase()) {
        "high" -> Color(0xFFE05A5A)
        "medium" -> Color(0xFFE39A38)
        "low" -> Color(0xFF12A889)
        else -> Color(0xFF536DE8)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                task.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF18234A)
            )

            if (task.subject.isNotBlank()) {
                Text(
                    "📚 ${task.subject}",
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "📅 ${task.dueDate}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Surface(
                    color = priorityColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        task.priority,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        color = priorityColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 36.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF18234A)
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                description,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun DashboardLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Color(0xFF536DE8))

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Loading your academic information...",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}
