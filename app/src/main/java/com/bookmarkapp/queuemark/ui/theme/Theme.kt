package com.bookmarkapp.queuemark.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SlateTeal,
    onPrimary = Color.White,
    secondary = OliveGray,
    onSecondary = Color.White,
    tertiary = Amber,
    onTertiary = Color.White,
    background = OffWhite,
    onBackground = DarkCharcoal,
    surface = SurfaceLight,
    onSurface = DarkCharcoal,
)

private val DarkColorScheme = darkColorScheme(
    primary = MutedSage,
    onPrimary = DarkCharcoal,
    secondary = SageGray,
    onSecondary = DarkCharcoal,
    tertiary = AmberLight,
    onTertiary = DarkCharcoal,
    background = DarkCharcoal,
    onBackground = OffWhite,
    surface = SurfaceDark,
    onSurface = OffWhite,
)

@Composable
fun QueuemarkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dynamic color is intentionally disabled so the Queuemark brand palette
    // is not replaced by wallpaper-derived colors on Android 12+.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
