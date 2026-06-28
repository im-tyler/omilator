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

        var playingRom by remember { mutableStateOf<String?>(null) }
        var playingCore by remember { mutableStateOf<String?>(null) }

        val romPath = playingRom
        val corePath = playingCore

        if (romPath != null && corePath != null) {
            IosPlayerScreen(romPath = romPath, corePath = corePath)
        } else {
            OmilatorApp(
                libraryViewModel = libraryViewModel,
                settingsViewModel = SettingsViewModel(),
                onAddRomDirectory = {
                    pickDirectory(vc) { path ->
                        if (path != null) println("[Omilator] Picked directory: $path")
                    }
                },
                onPlayRom = { path ->
                    // Resolve core from ROM extension
                    val ext = path.substringAfterLast('.', "")
                    val coreName = when (ext.lowercase()) {
                        "gba", "gb", "gbc", "sgb" -> "mgba_libretro"
                        "nes", "nez" -> "mesen_libretro"
                        "sfc", "smc" -> "snes9x_libretro"
                        else -> "mgba_libretro"
                    }
                    // Try multiple core locations
                    val home = NSHomeDirectory()
                    val candidates = listOf(
                        "$home/Documents/cores/$coreName.dylib",
                        "$home/Documents/$coreName.dylib",
                    )
                    val found = candidates.firstOrNull { exists(it) }
                    if (found != null) {
                        playingCore = found
                        playingRom = path
                    } else {
                        println("[Omilator] Core not found: $coreName (looked in Documents/cores/)")
                    }
                },
                onQuickPlay = {
                    pickFile(vc) { path ->
                        if (path != null) {
                            // Trigger same play logic
                            val ext = path.substringAfterLast('.', "")
                            val coreName = when (ext.lowercase()) {
                                "gba", "gb", "gbc", "sgb" -> "mgba_libretro"
                                "nes", "nez" -> "mesen_libretro"
                                "sfc", "smc" -> "snes9x_libretro"
                                else -> "mgba_libretro"
                            }
                            val home = NSHomeDirectory()
                            val candidates = listOf(
                                "$home/Documents/cores/$coreName.dylib",
                                "$home/Documents/$coreName.dylib",
                            )
                            val found = candidates.firstOrNull { exists(it) }
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
