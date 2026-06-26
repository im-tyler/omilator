package com.omilator.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun currentWindowSizeClass(): WindowSizeClass {
    return remember { WindowSizeClass.COMPACT }
}
