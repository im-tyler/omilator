package com.omilator.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 2026 Apple-style color system:
// - Near-black backgrounds (not pure black) with subtle cool tint
// - Elevated surfaces with very subtle warmth
// - Single accent color (SF Blue / Omilator Blue) used sparingly
// - High-contrast text but never pure white/black (avoids harshness)
// - Generous use of translucency in practice (use alpha-modulated colors)

private val Accent = Color(0xFF6F8FFF)
private val AccentContainer = Color(0xFF1E2A52)

// Dark theme — refined near-black with cool tint
private val DarkBackground = Color(0xFF0B0B0F)
private val DarkSurface = Color(0xFF131318)
private val DarkSurfaceVariant = Color(0xFF1B1B22)
private val DarkSurfaceElevated = Color(0xFF22222B)
private val DarkOnBackground = Color(0xFFF2F2F7)
private val DarkOnSurface = Color(0xFFE8E8EE)
private val DarkOnSurfaceVariant = Color(0xFF8E8E96)
private val DarkOutline = Color(0xFF2C2C36)
private val DarkOutlineVariant = Color(0xFF1F1F28)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentContainer,
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFF9FA8C7),
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = DarkSurfaceElevated,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = Color(0xFFFF453A),
    onError = Color.White,
)

// Light theme — Apple HIG light
private val LightBackground = Color(0xFFF5F5F7)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFEBEBF0)
private val LightOnBackground = Color(0xFF1C1C1E)
private val LightOnSurface = Color(0xFF1C1C1E)
private val LightOnSurfaceVariant = Color(0xFF6C6C70)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF10257A),
    secondary = Color(0xFF4A5485),
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = Color(0xFFD1D1D6),
    outlineVariant = Color(0xFFE5E5EA),
    error = Color(0xFFFF3B30),
    onError = Color.White,
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
