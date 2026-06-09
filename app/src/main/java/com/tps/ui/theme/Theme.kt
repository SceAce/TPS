package com.tps.ui.theme

/**
 * 文件说明：主题与通用组件定义，负责统一界面视觉风格与可复用组件。
 */

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF6A1A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEFE5),
    onPrimaryContainer = Color(0xFF8A2A00),
    secondary = Color(0xFF168765),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5F4EE),
    onSecondaryContainer = Color(0xFF063E2E),
    tertiary = Color(0xFFF5A400),
    background = Color(0xFFF5F7F6),
    onBackground = Color(0xFF1F2522),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F3F1),
    onSurface = Color(0xFF1F2522),
    onSurfaceVariant = Color(0xFF6E7973),
    outline = Color(0xFFDCE5E0),
    error = Color(0xFFD94832),
)

@Composable
fun TpsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
