package com.omilator.ui.adaptive

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.FrameWindowScope
import java.awt.Toolkit

@Composable
actual fun currentWindowSizeClass(): WindowSizeClass {
    return remember {
        val screenWidth = Toolkit.getDefaultToolkit().screenSize.width
        when {
            screenWidth < 700 -> WindowSizeClass.COMPACT
            screenWidth < 1100 -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }
    }
}
