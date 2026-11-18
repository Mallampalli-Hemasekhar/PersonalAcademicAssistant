
package com.example.personalacademicassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isResetLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 25.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(15.dp))

        // Back-to-study illustration

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(205.dp),
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .size(175.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.10f))
            )

            Box(
                modifier = Modifier
                    .size(135.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧑‍🎓",
                    fontSize = 85.sp
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 35.dp)
                    .size(48.dp),
                shape = RoundedCornerShape(15.dp),
                color = colors.surface,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("📚", fontSize = 23.sp)
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 30.dp)
                    .size(45.dp),
                shape = RoundedCornerShape(15.dp),
                color = colors.surface,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("💡", fontSize = 22.sp)
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 48.dp)
                    .size(43.dp),
                shape = RoundedCornerShape(14.dp),
                color = colors.surface,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("💻", fontSize = 21.sp)
                }
            }
        }

        Text(
            text = "Welcome Back!",
            fontSize = 29.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Let's learn and grow together.",
            fontSize = 15.sp,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Login form

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Login to your account",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter your email") },
                    leadingIcon = {
                        Text("✉️", fontSize = 19.sp)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    )
                )

                Spacer(modifier = Modifier.height(15.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password") },
                    leadingIcon = {
                        Text("🔒", fontSize = 19.sp)
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                passwordVisible = !passwordVisible
                            }
                        ) {
                            Text(
                                text = if (passwordVisible) "Hide" else "Show",
                                color = colors.primary,
                                fontSize = 12.sp
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    shape = RoundedCornerShape(15.dp)
                )

                // Forgot password

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            when {
                                email.isBlank() -> {
                                    errorMessage =
                                        "Enter your email to reset your password."
                                    successMessage = ""
                                }

                                else -> {
                                    errorMessage = ""
                                    successMessage = ""
                                    isResetLoading = true

                                    auth.sendPasswordResetEmail(email.trim())
                                        .addOnCompleteListener { task ->
                                            isResetLoading = false

                                            if (task.isSuccessful) {
                                                successMessage =
                                                    "Password reset link sent. Check your email."
                                            } else {
                                                errorMessage =
                                                    task.exception?.localizedMessage
                                                        ?: "Unable to send reset email."
                                            }
                                        }
                                }
                            }
                        },
                        enabled = !isResetLoading && !isLoading
                    ) {
                        Text(
                            text = if (isResetLoading) {
                                "Sending..."
                            } else {
                                "Forgot Password?"
                            },
                            color = colors.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = colors.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                if (successMessage.isNotEmpty()) {
                    Text(
                        text = successMessage,
                        color = colors.primary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Login button

                Button(
                    onClick = {
                        when {
                            email.isBlank() -> {
                                errorMessage = "Please enter your email."
                            }

                            password.isBlank() -> {
                                errorMessage = "Please enter your password."
                            }

                            else -> {
                                errorMessage = ""
                                successMessage = ""
                                isLoading = true

                                auth.signInWithEmailAndPassword(
                                    email.trim(),
                                    password
                                ).addOnCompleteListener { task ->
                                    isLoading = false

                                    if (task.isSuccessful) {
                                        onLoginClick()
                                    } else {
                                        errorMessage =
                                            task.exception?.localizedMessage
                                                ?: "Login failed. Please try again."
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(15.dp),
                    enabled = !isLoading && !isResetLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(23.dp),
                            color = colors.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Login",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Don't have an account?",
                color = colors.onSurfaceVariant,
                fontSize = 14.sp
            )

            Text(
                text = "  Sign Up",
                color = colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    onRegisterClick()
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Your academic success starts here ✨",
            color = colors.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}
