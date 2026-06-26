package com.omilator.app

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.omilator.data.library.JvmLibraryScanner
import com.omilator.ui.OmilatorApp

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1200.dp, 760.dp),
        position = WindowPosition(Alignment.Center),
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Omilator",
        state = windowState,
    ) {
        OmilatorApp(libraryScanner = JvmLibraryScanner())
    }
}

private fun exitApplication() {
    kotlin.system.exitProcess(0)
}
