
package com.example.personalacademicassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalacademicassistant.ui.theme.PersonalAcademicAssistantTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = getSharedPreferences(
            "academic_assistant_settings",
            MODE_PRIVATE
        )

        setContent {

            var themeMode by remember {
                mutableStateOf(
                    preferences.getString("theme_mode", "System")
                        ?: "System"
                )
            }

            val darkTheme = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            PersonalAcademicAssistantTheme(
                darkTheme = darkTheme
            ) {

                var showDashboard by remember {
                    mutableStateOf(false)
                }

                var showLogin by remember {
                    mutableStateOf(false)
                }

                var showRegister by remember {
                    mutableStateOf(false)
                }

                when {

                    showDashboard -> {

                        BottomNavScreen(
                            themeMode = themeMode,

                            onThemeModeChange = { newMode ->

                                themeMode = newMode

                                preferences.edit()
                                    .putString("theme_mode", newMode)
                                    .apply()
                            },

                            onLogout = {

                                // Stop academic notifications first.
                                SmartNotificationManager.cancel(
                                    applicationContext
                                )

                                // Sign out the current Firebase user.
                                FirebaseAuth.getInstance().signOut()

                                showDashboard = false
                                showRegister = false
                                showLogin = true
                            }
                        )
                    }

                    showRegister -> {

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
                    }

                    showLogin -> {

                        LoginScreen(

                            onLoginClick = {

                                // Get the authenticated Firebase user.
                                val userId = FirebaseAuth
                                    .getInstance()
                                    .currentUser
                                    ?.uid

                                // Start notifications for this student.
                                if (!userId.isNullOrBlank()) {
                                    SmartNotificationManager.start(
                                        applicationContext,
                                        userId
                                    )
                                }

                                showLogin = false
                                showDashboard = true
                            },

                            onRegisterClick = {
                                showLogin = false
                                showRegister = true
                            }
                        )
                    }

                    else -> {

                        WelcomeScreen(

                            onGetStartedClick = {
                                showRegister = true
                            },

                            onLoginClick = {
                                showLogin = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onGetStartedClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "YOUR LEARNING JOURNEY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(25.dp))

        // Student illustration area

        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .size(225.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.10f))
            )

            Box(
                modifier = Modifier
                    .size(175.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧑‍🎓",
                    fontSize = 105.sp
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(55.dp),
                shape = RoundedCornerShape(18.dp),
                color = colors.surface,
                shadowElevation = 5.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "📚", fontSize = 27.sp)
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(52.dp),
                shape = RoundedCornerShape(17.dp),
                color = colors.surface,
                shadowElevation = 5.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "💡", fontSize = 25.sp)
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(55.dp),
                shape = RoundedCornerShape(18.dp),
                color = colors.surface,
                shadowElevation = 5.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "📅", fontSize = 26.sp)
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(55.dp),
                shape = RoundedCornerShape(18.dp),
                color = colors.surface,
                shadowElevation = 5.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "🏆", fontSize = 26.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Personal",
            fontSize = 31.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Academic Assistant",
            fontSize = 29.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Your smart companion for a smarter academic life.",
            fontSize = 15.sp,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp,
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        Spacer(modifier = Modifier.height(25.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WelcomeFeature("📚", "Learn")
            WelcomeFeature("📅", "Plan")
            WelcomeFeature("🎯", "Achieve")
        }

        Spacer(modifier = Modifier.height(35.dp))

        Button(
            onClick = onGetStartedClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary
            )
        ) {
            Text(
                text = "Get Started",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(13.dp))

        OutlinedButton(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(17.dp)
        ) {
            Text(
                text = "Login",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Plan. Learn. Achieve. Succeed.",
            fontSize = 13.sp,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun WelcomeFeature(
    emoji: String,
    title: String
) {
    val colors = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji,
            fontSize = 27.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.onSurfaceVariant
        )
    }
}
