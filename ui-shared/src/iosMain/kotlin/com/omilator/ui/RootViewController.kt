@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.ui

import androidx.compose.ui.window.ComposeUIViewController
import com.omilator.data.library.IosLibraryScanner
import com.omilator.data.library.LibraryRepository
import com.omilator.ui.library.LibraryViewModel
import com.omilator.ui.settings.SettingsViewModel
import platform.UIKit.UIViewController

fun RootViewController(): UIViewController {
    lateinit var vc: UIViewController
    vc = ComposeUIViewController {
        OmilatorApp(
            libraryViewModel = LibraryViewModel(
                repository = LibraryRepository(IosLibraryScanner()),
                settingsStore = null,
                settingsPath = "",
            ),
            settingsViewModel = SettingsViewModel(),
            onAddRomDirectory = {
                pickDirectory(vc) { path ->
                    if (path != null) {
                        println("[Omilator] Picked directory: $path")
                    }
                }
            },
            onPlayRom = { path ->
                println("[Omilator] Play ROM: $path")
            },
            onQuickPlay = {
                pickFile(vc) { path ->
                    if (path != null) {
                        println("[Omilator] Picked ROM: $path")
                    }
                }
            },
            isDesktop = false,
        )
    }
    return vc
}
