package com.ennogrensemann.reminder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val GoogleBlue = Color(0xFF1A73E8)
private val GoogleBlueDark = Color(0xFF8AB4F8)
private val GoogleSurfaceLight = Color(0xFFF8F9FA)
private val GoogleSurfaceDark = Color(0xFF121316)

private val LightColorScheme = lightColorScheme(
    primary = GoogleBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E3FC),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF188038),
    secondaryContainer = Color(0xFFCEEAD6),
    background = GoogleSurfaceLight,
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F3F4)
)

private val DarkColorScheme = darkColorScheme(
    primary = GoogleBlueDark,
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = Color(0xFF81C995),
    secondaryContainer = Color(0xFF0D652D),
    background = GoogleSurfaceDark,
    surface = Color(0xFF1E1F22),
    surfaceVariant = Color(0xFF282A2E)
)

@Composable
fun GeoTasksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
