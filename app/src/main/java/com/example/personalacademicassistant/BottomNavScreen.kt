package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
        "Home",
        "Schedule",
        "Tasks",
        "Progress",
        "Profile"
    )

    val icons = listOf(
        "🏠",
        "📅",
        "✅",
        "📊",
        "👤"
    )

    Scaffold(

        bottomBar = {

            NavigationBar(
                containerColor = Color.White
            ) {

                items.forEachIndexed { index, item ->

                    NavigationBarItem(

                        selected = selectedItem == index,

                        onClick = {
                            selectedItem = index
                        },

                        icon = {

                            Text(
                                text = icons[index],
                                fontSize = 20.sp
                            )
                        },

                        label = {

                            Text(
                                text = item
                            )
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

                0 -> {
                    DashboardScreen()
                }

                1 -> {
                    ScheduleScreen()
                }

                2 -> {
                    TasksScreen()
                }

                3 -> {
                    ProgressScreen()
                }

                4 -> {
                    ProfileScreen()
                }
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

                onClick = {
                    showTimetable = true
                },

                modifier = Modifier.weight(1f),

                colors = ButtonDefaults.buttonColors(

                    containerColor =
                        if (showTimetable) {
                            Color(0xFF3F51B5)
                        } else {
                            Color.LightGray
                        }
                )

            ) {

                Text(
                    text = "📅 Timetable",
                    fontSize = 14.sp
                )
            }


            Button(

                onClick = {
                    showTimetable = false
                },

                modifier = Modifier.weight(1f),

                colors = ButtonDefaults.buttonColors(

                    containerColor =
                        if (!showTimetable) {
                            Color(0xFF3F51B5)
                        } else {
                            Color.LightGray
                        }
                )

            ) {

                Text(
                    text = "📝 Exams",
                    fontSize = 14.sp
                )
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

    AttendanceScreen()
}


/* ---------------- PROFILE SCREEN ---------------- */

@Composable
fun ProfileScreen() {

    SimpleScreen(

        icon = "👤",

        title = "Profile",

        description =
            "Manage your student profile and academic information."
    )
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

        Text(

            text = icon,

            fontSize = 55.sp
        )


        Text(

            text = title,

            fontSize = 28.sp,

            fontWeight = FontWeight.Bold,

            color = Color(0xFF172554)
        )


        Text(

            text = description,

            fontSize = 16.sp,

            color = Color.Gray
        )
    }
}