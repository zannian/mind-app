package com.lyulf.mind.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val scheme = lightColorScheme(
    primary = MindPrimary,
    onPrimary = MindSurface,
    background = MindBg,
    onBackground = MindInk,
    surface = MindSurface,
    onSurface = MindInk,
    secondary = MindPending,
)

@Composable
fun MindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = MindTypography,
        content = content,
    )
}
