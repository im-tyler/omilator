@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.window.ComposeUIViewController

/**
 * Entry point for the iOS app. Called from Swift's ContentView.
 * Returns a UIViewController hosting the Compose UI.
 */
fun RootViewController() = ComposeUIViewController {
    MaterialTheme {
        Text("Omilator on iOS")
    }
}
