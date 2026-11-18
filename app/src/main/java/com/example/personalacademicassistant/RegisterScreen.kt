
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.google.firebase.auth.UserProfileChangeRequest

@Composable
fun RegisterScreen(
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 25.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(8.dp))

        // Student illustration

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(125.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.11f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧑‍🎓",
                    fontSize = 78.sp
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 65.dp)
                    .size(43.dp),
                shape = RoundedCornerShape(14.dp),
                color = colors.surface,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("📚", fontSize = 22.sp)
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 65.dp)
                    .size(43.dp),
                shape = RoundedCornerShape(14.dp),
                color = colors.surface,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✨", fontSize = 22.sp)
                }
            }
        }

        Text(
            text = "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground
        )

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = "Start your journey towards academic success.",
            fontSize = 14.sp,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(19.dp)
            ) {

                Text(
                    text = "Your Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Full Name") },
                    placeholder = { Text("Enter your full name") },
                    leadingIcon = {
                        Text("👤", fontSize = 19.sp)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter your email") },
                    leadingIcon = {
                        Text("✉️", fontSize = 19.sp)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    placeholder = { Text("Create a password") },
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
                                if (passwordVisible) "Hide" else "Show",
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
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm Password") },
                    placeholder = { Text("Re-enter your password") },
                    leadingIcon = {
                        Text("🔐", fontSize = 19.sp)
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                confirmPasswordVisible =
                                    !confirmPasswordVisible
                            }
                        ) {
                            Text(
                                if (confirmPasswordVisible) "Hide" else "Show",
                                color = colors.primary,
                                fontSize = 12.sp
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = colors.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        when {
                            name.isBlank() -> {
                                errorMessage = "Please enter your name."
                            }

                            email.isBlank() -> {
                                errorMessage = "Please enter your email."
                            }

                            !android.util.Patterns.EMAIL_ADDRESS
                                .matcher(email.trim()).matches() -> {
                                errorMessage = "Please enter a valid email."
                            }

                            password.isBlank() -> {
                                errorMessage = "Please enter a password."
                            }

                            password.length < 6 -> {
                                errorMessage =
                                    "Password must be at least 6 characters."
                            }

                            confirmPassword.isBlank() -> {
                                errorMessage = "Please confirm your password."
                            }

                            password != confirmPassword -> {
                                errorMessage = "Passwords do not match."
                            }

                            else -> {
                                errorMessage = ""
                                isLoading = true

                                auth.createUserWithEmailAndPassword(
                                    email.trim(),
                                    password
                                ).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val profileUpdate =
                                            UserProfileChangeRequest.Builder()
                                                .setDisplayName(name.trim())
                                                .build()

                                        auth.currentUser
                                            ?.updateProfile(profileUpdate)
                                            ?.addOnCompleteListener {
                                                isLoading = false
                                                onRegisterClick()
                                            } ?: run {
                                            isLoading = false
                                            onRegisterClick()
                                        }
                                    } else {
                                        isLoading = false
                                        errorMessage =
                                            task.exception?.localizedMessage
                                                ?: "Registration failed."
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(15.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = colors.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Create Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Already have an account?",
                color = colors.onSurfaceVariant,
                fontSize = 14.sp
            )

            TextButton(onClick = onLoginClick) {
                Text(
                    text = "Login",
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Learn. Grow. Achieve. ✨",
            fontSize = 13.sp,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))
    }
}
