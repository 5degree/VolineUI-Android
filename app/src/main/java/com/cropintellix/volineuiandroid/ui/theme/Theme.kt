package com.cropintellix.volineuiandroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// App colors matching themes.xml
val PrimaryColor = Color(0xFFE91E63)      // Pink
val PrimaryDarkColor = Color(0xFFC2185B)   // Darker pink
val SecondaryColor = Color(0xFF03DAC6)     // Teal

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE4EC),
    onPrimaryContainer = Color(0xFF880E4F),
    secondary = SecondaryColor,
    onSecondary = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    error = Color(0xFFB00020),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF880E4F),
    onPrimaryContainer = Color(0xFFFCE4EC),
    secondary = SecondaryColor,
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

/**
 * App theme wrapper for all Compose screens.
 * Uses primary color #E91E63 matching the XML themes.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
