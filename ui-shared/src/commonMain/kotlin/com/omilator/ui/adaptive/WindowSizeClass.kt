package com.omilator.ui.adaptive

import androidx.compose.runtime.Composable

enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }

@Composable
expect fun currentWindowSizeClass(): WindowSizeClass
