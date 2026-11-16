package com.zameerahma.snakeapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F8A47),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F1C5),
    onPrimaryContainer = Color(0xFF00210D),
    secondary = Color(0xFF4F6354),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8D5),
    onSecondaryContainer = Color(0xFF0C1F13),
    tertiary = Color(0xFFB8860B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF3A2A00),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFF4FBF3),
    onBackground = Color(0xFF171D19),
    surface = Color(0xFFF4FBF3),
    onSurface = Color(0xFF171D19),
    surfaceVariant = Color(0xFFDDE5DB),
    onSurfaceVariant = Color(0xFF414942),
    surfaceContainer = Color(0xFFE8F0E7),
    surfaceContainerHigh = Color(0xFFE2EAE1),
    outline = Color(0xFF717971),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CE38B),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF0F5129),
    onPrimaryContainer = Color(0xFFB7F1C5),
    secondary = Color(0xFFB6CCB9),
    onSecondary = Color(0xFF213527),
    secondaryContainer = Color(0xFF2E4433),
    onSecondaryContainer = Color(0xFFD2E8D5),
    tertiary = Color(0xFFFFC83D),
    onTertiary = Color(0xFF3A2A00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFE08A),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF0E1511),
    onBackground = Color(0xFFDEE4DD),
    surface = Color(0xFF0E1511),
    onSurface = Color(0xFFDEE4DD),
    surfaceVariant = Color(0xFF26322A),
    onSurfaceVariant = Color(0xFFC0C9BF),
    surfaceContainer = Color(0xFF18211B),
    surfaceContainerHigh = Color(0xFF212B24),
    outline = Color(0xFF8A938A),
)

/** Colors used to draw the game board. */
@Immutable
data class BoardColors(
    val board: Color,
    val boardAlt: Color,
    val snakeHead: Color,
    val snakeTail: Color,
    val crashed: Color,
    val food: Color,
    val leaf: Color,
    val bonus: Color,
    val obstacle: Color,
    val obstacleEdge: Color,
)

private val LightBoard = BoardColors(
    board = Color(0xFFDDEFD9),
    boardAlt = Color(0xFFD2E8CD),
    snakeHead = Color(0xFF14713A),
    snakeTail = Color(0xFF5CC27A),
    crashed = Color(0xFFD32F2F),
    food = Color(0xFFE53935),
    leaf = Color(0xFF2E7D32),
    bonus = Color(0xFFF2A900),
    obstacle = Color(0xFF7D8B82),
    obstacleEdge = Color(0xFF5E6B63),
)

private val DarkBoard = BoardColors(
    board = Color(0xFF16231B),
    boardAlt = Color(0xFF1B2A21),
    snakeHead = Color(0xFF8CF29A),
    snakeTail = Color(0xFF2B9C52),
    crashed = Color(0xFFFF6B6B),
    food = Color(0xFFFF5A5F),
    leaf = Color(0xFF7CE38B),
    bonus = Color(0xFFFFC83D),
    obstacle = Color(0xFF55635A),
    obstacleEdge = Color(0xFF3E4A43),
)

val LocalBoardColors = staticCompositionLocalOf { DarkBoard }

private val SnakeTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 8.sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
fun SnakeTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    CompositionLocalProvider(LocalBoardColors provides if (darkTheme) DarkBoard else LightBoard) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = SnakeTypography,
            content = content,
        )
    }
}
