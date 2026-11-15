package com.example.personalacademicassistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB9A5FF),
    onPrimary = Color(0xFF24105C),

    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003731),

    tertiary = Color(0xFFFFB6C1),
    onTertiary = Color(0xFF5C1028),

    background = Color(0xFF0D1020),
    onBackground = Color(0xFFE8E7F0),

    surface = Color(0xFF171B2E),
    onSurface = Color(0xFFE8E7F0),

    surfaceVariant = Color(0xFF292D42),
    onSurfaceVariant = Color(0xFFC4C4D4),

    outline = Color(0xFF777B91)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650C8),
    onPrimary = Color.White,

    secondary = Color(0xFF008F83),
    onSecondary = Color.White,

    tertiary = Color(0xFFB94F78),
    onTertiary = Color.White,

    background = Color(0xFFF7F7FC),
    onBackground = Color(0xFF19172A),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF19172A),

    surfaceVariant = Color(0xFFECEAF5),
    onSurfaceVariant = Color(0xFF49465D),

    outline = Color(0xFF858197)
)

@Composable
fun PersonalAcademicAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
