@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.ui

import androidx.compose.ui.window.ComposeUIViewController
import com.omilator.data.library.IosLibraryScanner
import com.omilator.data.library.LibraryRepository
import com.omilator.ui.library.LibraryViewModel
import com.omilator.ui.settings.SettingsViewModel

/**
 * Entry point for the iOS app. Called from Swift's ContentView.
 * Returns a UIViewController hosting the Compose UI.
 */
fun RootViewController() = ComposeUIViewController { 
    OmilatorApp(
        libraryViewModel = LibraryViewModel(
            repository = LibraryRepository(IosLibraryScanner()),
            settingsStore = null,
            settingsPath = "",
        ),
        settingsViewModel = SettingsViewModel(),
        onAddRomDirectory = {},
        onPlayRom = {},
    )
}
