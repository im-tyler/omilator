@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.omilator.data.library.IosLibraryScanner
import com.omilator.data.library.LibraryRepository
import com.omilator.ui.library.LibraryViewModel
import com.omilator.ui.settings.SettingsViewModel
import platform.UIKit.UIViewController

fun RootViewController(): UIViewController {
    lateinit var vc: UIViewController
    vc = ComposeUIViewController {
        val libraryViewModel = LibraryViewModel(
            repository = LibraryRepository(IosLibraryScanner()),
            settingsStore = null,
            settingsPath = "",
        )

        androidx.compose.runtime.LaunchedEffect(Unit) {
            libraryViewModel.rescan(listOf("Documents"))
        }

        var showPlayerAlert by remember { mutableStateOf<String?>(null) }

        OmilatorApp(
            libraryViewModel = libraryViewModel,
            settingsViewModel = SettingsViewModel(),
            onAddRomDirectory = {
                pickDirectory(vc) { path ->
                    if (path != null) println("[Omilator] Picked directory: $path")
                }
            },
            onPlayRom = { path ->
                showPlayerAlert = path
            },
            onQuickPlay = {
                pickFile(vc) { path ->
                    if (path != null) showPlayerAlert = path
                }
            },
            isDesktop = false,
        )

        showPlayerAlert?.let { romPath ->
            AlertDialog(
                onDismissRequest = { showPlayerAlert = null },
                title = { Text("ROM Selected") },
                text = {
                    val name = romPath.substringAfterLast('/')
                    Text("Selected: $name\n\nIn-game emulation on iOS requires a libretro core compiled for iOS arm64. This is the next development step.")
                },
                confirmButton = {
                    TextButton(onClick = { showPlayerAlert = null }) { Text("OK") }
                },
            )
        }
    }
    return vc
}
