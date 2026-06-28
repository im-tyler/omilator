@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.omilator.data.library.IosLibraryScanner
import com.omilator.data.library.LibraryRepository
import com.omilator.data.library.cleanRomTitle
import com.omilator.ui.library.LibraryViewModel
import com.omilator.ui.settings.SettingsViewModel
import platform.UIKit.UIViewController
import platform.Foundation.NSHomeDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun RootViewController(): UIViewController {
    lateinit var vc: UIViewController

    // Hoist ViewModels OUTSIDE the composable so they survive player ↔ library switches
    val libraryViewModel = LibraryViewModel(
        repository = LibraryRepository(IosLibraryScanner()),
        settingsStore = null,
        settingsPath = "",
    )
    val settingsViewModel = SettingsViewModel()

    // Trigger initial scan
    CoroutineScope(Dispatchers.Default).launch {
        libraryViewModel.rescan(listOf("Documents"))
    }

    vc = ComposeUIViewController {
        var playingRom by remember { mutableStateOf<String?>(null) }
        var playingCore by remember { mutableStateOf<String?>(null) }

        val romPath = playingRom
        val corePath = playingCore

        if (romPath != null && corePath != null) {
            IosPlayerScreen(romPath = romPath, corePath = corePath, onExit = {
                playingRom = null
                playingCore = null
            })
        } else {
            OmilatorApp(
                libraryViewModel = libraryViewModel,
                settingsViewModel = settingsViewModel,
                onAddRomDirectory = {
                    pickDirectory(vc) { path ->
                        if (path != null) println("[Omilator] Picked directory: $path")
                    }
                },
                onPlayRom = { path ->
                    val ext = path.substringAfterLast('.', "")
                    val coreName = when (ext.lowercase()) {
                        "gba", "gb", "gbc", "sgb" -> "mgba_libretro"
                        "nes", "nez" -> "mesen_libretro"
                        "sfc", "smc" -> "snes9x_libretro"
                        else -> "mgba_libretro"
                    }
                    val home = NSHomeDirectory()
                    val found = listOfNotNull(
                        if (exists("$home/Documents/cores/$coreName.dylib")) "$home/Documents/cores/$coreName.dylib" else null,
                    ).firstOrNull()
                    if (found != null) {
                        playingCore = found
                        playingRom = path
                    }
                },
                onQuickPlay = {
                    pickFile(vc) { path ->
                        if (path != null) {
                            val ext = path.substringAfterLast('.', "")
                            val coreName = when (ext.lowercase()) {
                                "gba", "gb", "gbc", "sgb" -> "mgba_libretro"
                                "nes", "nez" -> "mesen_libretro"
                                else -> "mgba_libretro"
                            }
                            val home = NSHomeDirectory()
                            val found = listOfNotNull(
                                if (exists("$home/Documents/cores/$coreName.dylib")) "$home/Documents/cores/$coreName.dylib" else null,
                            ).firstOrNull()
                            if (found != null) {
                                playingCore = found
                                playingRom = path
                            }
                        }
                    }
                },
                isDesktop = false,
                singleScreen = true,
            )
        }
    }
    return vc
}

private fun exists(path: String): Boolean {
    return try {
        platform.Foundation.NSFileManager.defaultManager.fileExistsAtPath(path)
    } catch (_: Exception) {
        false
    }
}
