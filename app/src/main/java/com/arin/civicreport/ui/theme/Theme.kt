package com.arin.civicreport.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val md_theme_light = lightColorScheme(
    primary = Color(0xFF206039), secondaryContainer = Color(0xFFD7F0D8),
    tertiary = Color(0xFF6750A4), background = Color(0xFFFBFDF7), surface = Color(0xFFFBFDF7),
    onPrimary = Color.White, onSecondaryContainer = Color(0xFF14291A),
    onTertiary = Color.White, onError = Color(0xFFB3261E),
    outline = Color(0xFF79747E), surfaceVariant = Color(0xFFE7E0EC)
)

private val md_theme_dark = darkColorScheme(
    primary = Color(0xFFA8D7A8), secondaryContainer = Color(0xFF2A4231),
    tertiary = Color(0xFFD0BCFF), background = Color(0xFF1A1C18), surface = Color(0xFF1A1C18),
    onPrimary = Color(0xFF123E23), onSecondaryContainer = Color(0xFFD7F0D8),
    onTertiary = Color(0xFF381E72), onError = Color(0xFFF2D0C5),
    outline = Color(0xFF938F99), surfaceVariant = Color(0xFF3B444E)
)

private val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    )
)

@Composable
fun CivicReportTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> md_theme_dark
        else -> md_theme_light
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
