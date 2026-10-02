package com.pixelbot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6C5CE7),
    primaryContainer = Color(0xFF5A4BD9),
    secondary = Color(0xFF00CEC9),
    secondaryContainer = Color(0xFF00B5B0),
    tertiary = Color(0xFFFFD93D),
    tertiaryContainer = Color(0xFFE6C200),
    background = Color(0xFF1A1A2E),
    surface = Color(0xFF16213E),
    surfaceVariant = Color(0xFF1F2A48),
    error = Color(0xFFFF6B6B),
    onPrimary = Color.White,
    onSecondary = Color(0xFF1A1A2E),
    onTertiary = Color(0xFF1A1A2E),
    onBackground = Color(0xFFEEEEEE),
    onSurface = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFFB0B0C8),
    onError = Color.White,
    outline = Color(0xFF3D3D5C),
    outlineVariant = Color(0xFF2D2D4A),
    shadow = Color.Black,
    scrim = Color.Black,
    inverseSurface = Color(0xFFEEEEEE),
    inverseOnSurface = Color(0xFF1A1A2E),
    inversePrimary = Color(0xFF5A4BD9)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5A4BD9),
    primaryContainer = Color(0xFFE0DEFF),
    secondary = Color(0xFF009E9A),
    secondaryContainer = Color(0xFFCCF4F2),
    tertiary = Color(0xFFB88600),
    tertiaryContainer = Color(0xFFFFF3CC),
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE0E0EC),
    error = Color(0xFFC00000),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E),
    onSurfaceVariant = Color(0xFF44445C),
    onError = Color.White,
    outline = Color(0xFF74748C),
    outlineVariant = Color(0xFFC4C4DC),
    shadow = Color.Black,
    scrim = Color.Black,
    inverseSurface = Color(0xFF1A1A2E),
    inverseOnSurface = Color(0xFFEEEEEE),
    inversePrimary = Color(0xFFB8AFFF)
)

@Composable
fun PixelBotTheme(
    darkTheme: Boolean = false, // isSystemInDarkTheme()
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)