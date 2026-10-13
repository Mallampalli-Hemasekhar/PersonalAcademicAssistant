package com.example.personalacademicassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalacademicassistant.ui.theme.PersonalAcademicAssistantTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PersonalAcademicAssistantTheme {

                var showDashboard by remember {
                    mutableStateOf(false)
                }

                var showLogin by remember {
                    mutableStateOf(false)
                }

                var showRegister by remember {
                    mutableStateOf(false)
                }

                // SCREEN NAVIGATION
                if (showDashboard) {

                    BottomNavScreen()

                } else if (showRegister) {

                    RegisterScreen(
                        onRegisterClick = {
                            showRegister = false
                            showLogin = true
                        },
                        onLoginClick = {
                            showRegister = false
                            showLogin = true
                        }
                    )

                } else if (showLogin) {

                    LoginScreen(
                        onLoginClick = {
                            showLogin = false
                            showDashboard = true
                        },
                        onRegisterClick = {
                            showLogin = false
                            showRegister = true
                        }
                    )

                } else {

                    WelcomeScreen(
                        onGetStartedClick = {
                            showLogin = true
                        }
                    )
                }
            }
        }
    }
}

/*
=========================================================
WELCOME SCREEN
=========================================================
*/

@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FB))
            .padding(30.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center
    ) {

        // APP LOGO
        Column(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Color(0xFF3F51B5)),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "🎓",
                fontSize = 50.sp
            )
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        // APP NAME
        Text(
            text = "Personal Academic",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF172554),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Assistant",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3F51B5),
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        // DESCRIPTION
        Text(
            text = "Your smart academic companion\nfor managing your student life.",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(
            modifier = Modifier.height(45.dp)
        )

        // GET STARTED BUTTON
        Button(
            onClick = {
                onGetStartedClick()
            },

            modifier = Modifier.size(
                width = 220.dp,
                height = 55.dp
            ),

            shape = RoundedCornerShape(15.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3F51B5)
            )
        ) {

            Text(
                text = "Get Started",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}