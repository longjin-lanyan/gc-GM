package com.genshin.gm.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val GenshinPurple = Color(0xFF667EEA)
val GenshinPurpleDark = Color(0xFF764BA2)
val GenshinGold = Color(0xFFE6AC54)
val GenshinBackground = Color(0xFF1A1B2E)
val GenshinSurface = Color(0xFF242540)
val GenshinCard = Color(0xFF2D2E4A)

private val DarkColorScheme = darkColorScheme(
    primary = GenshinPurple,
    secondary = GenshinPurpleDark,
    tertiary = GenshinGold,
    background = GenshinBackground,
    surface = GenshinSurface,
    surfaceVariant = GenshinCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0C0),
)

@Composable
fun GenshinGMTheme(content: @Composable () -> Unit) {
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (isSystemInDarkTheme()) dynamicDarkColorScheme(context)
        else DarkColorScheme
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
