package com.omilator.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.omilator.data.library.JvmLibraryScanner
import com.omilator.data.library.LibraryRepository
import com.omilator.data.settings.SettingsStore
import com.omilator.data.settings.defaultConfigDir
import com.omilator.ui.OmilatorApp
import com.omilator.ui.OmilatorTheme
import com.omilator.ui.library.LibraryViewModel
import com.omilator.ui.player.PlayerScreen
import com.omilator.ui.settings.SettingsViewModel
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFrame

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1200.dp, 760.dp),
        position = WindowPosition(Alignment.Center),
    )

    var playing by remember { mutableStateOf<String?>(null) }

    val configDir = remember { defaultConfigDir() }
    val settingsPath = remember { File(configDir, "settings.json").absolutePath }
    val libraryViewModel = remember {
        LibraryViewModel(
            repository = LibraryRepository(JvmLibraryScanner()),
            settingsStore = SettingsStore(
                readText = { path -> File(path).takeIf { it.exists() }?.readText() },
                writeText = { path, content -> File(path).writeText(content) },
            ),
            settingsPath = settingsPath,
        )
    }
    val settingsViewModel = remember { SettingsViewModel() }

    val onAddRomDirectory: () -> Unit = {
        pickRomDirectory()?.let { dir ->
            libraryViewModel.addDirectory(dir)
            settingsViewModel.addDirectory(dir)
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Omilator",
        state = windowState,
    ) {
        val romPath = playing
        if (romPath != null) {
            OmilatorTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp && event.key == Key.Escape) {
                                playing = null
                                true
                            } else false
                        },
                ) {
                    PlayerScreen(
                        gameId = romPath,
                        onClose = { playing = null },
                    )
                }
            }
        } else {
            OmilatorApp(
                libraryViewModel = libraryViewModel,
                settingsViewModel = settingsViewModel,
                onAddRomDirectory = onAddRomDirectory,
                onPlayRom = { path -> playing = path },
                onQuickPlay = {
                    pickRomFile()?.let { path -> playing = path }
                },
            )
        }
    }
}

/**

 * Native macOS file picker via AWT FileDialog. Far more reliable than
 * JFileChooser on macOS — actually appears in front and respects system theme.
 */
private fun pickRomDirectory(): String? {
    val frame = JFrame().apply { isUndecorated = true; isVisible = true; extendedState = Frame.ICONIFIED }
    return try {
        val dialog = FileDialog(frame, "Select ROM directory", FileDialog.LOAD).apply {
            isMultipleMode = false
            setFilenameFilter { _, name -> File(name).isDirectory }
            isVisible = true
        }
        dialog.directory?.let { dir ->
            val selected = if (dialog.file != null) File(dir, dialog.file) else File(dir)
            if (selected.isDirectory) selected.absolutePath else null
        }
    } finally {
        frame.dispose()
    }
}

private fun pickRomFile(): String? {
    val frame = JFrame().apply { isUndecorated = true; isVisible = true; extendedState = Frame.ICONIFIED }
    return try {
        val dialog = FileDialog(frame, "Select ROM file", FileDialog.LOAD).apply {
            isMultipleMode = false
            val extensions = arrayOf(".gba", ".gb", ".gbc", ".sgb", ".nes", ".nez", ".sfc", ".smc", ".md", ".bin", ".smd", ".gen")
            setFilenameFilter { _, name -> extensions.any { name.lowercase().endsWith(it) } }
            setVisible(true)
        }
        if (dialog.file != null && dialog.directory != null) {
            File(dialog.directory, dialog.file).absolutePath
        } else null
    } finally {
        frame.dispose()
    }
}

private fun exitApplication() {
    kotlin.system.exitProcess(0)
}
