package com.omilator.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SlateDark = Color(0xFF0F1115)
private val SlateDarkElevated = Color(0xFF1A1D24)
private val SlateLight = Color(0xFFF7F8FA)
private val SlateLightElevated = Color(0xFFFFFFFF)

private val Accent = Color(0xFF6F8FFF)
private val AccentDim = Color(0xFF3F5BBF)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentDim,
    onPrimaryContainer = Color.White,
    background = SlateDark,
    onBackground = Color(0xFFE6E8EE),
    surface = SlateDarkElevated,
    onSurface = Color(0xFFE6E8EE),
    surfaceVariant = Color(0xFF22262F),
    onSurfaceVariant = Color(0xFFB7BCC7),
    outline = Color(0xFF3A3F4B),
)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF10257A),
    background = SlateLight,
    onBackground = Color(0xFF111418),
    surface = SlateLightElevated,
    onSurface = Color(0xFF111418),
    surfaceVariant = Color(0xFFEAECF1),
    onSurfaceVariant = Color(0xFF454953),
    outline = Color(0xFFC7CBD3),
)

@Composable
fun OmilatorTheme(
    forceDark: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val isDark = forceDark ?: isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        typography = OmilatorTypography,
        content = content,
    )
}
