package com.taskbar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Тёмная цветовая схема Material 3 в палитре Taskbar
private val TaskbarColorScheme = darkColorScheme(
    primary = TaskbarHeader,
    onPrimary = TaskbarWhite,
    secondary = TaskbarOrange,
    onSecondary = TaskbarWhite,
    background = TaskbarBackground,
    onBackground = TaskbarWhite,
    surface = TaskbarSurface,
    onSurface = TaskbarWhite,
    surfaceVariant = TaskbarSurface,
    onSurfaceVariant = TaskbarSecondary,
)

/**
 * Тема приложения. Приложение всегда работает в тёмном оформлении
 * (согласно дизайну Taskbar), системная тема не переключает палитру.
 */
@Composable
fun TaskbarTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TaskbarColorScheme,
        typography = TaskbarTypography,
        content = content
    )
}
