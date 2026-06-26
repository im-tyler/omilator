package com.omilator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.omilator.data.library.AndroidLibraryScanner
import com.omilator.data.library.LibraryRepository
import com.omilator.data.settings.SettingsStore
import com.omilator.ui.OmilatorApp
import com.omilator.ui.library.LibraryViewModel
import com.omilator.ui.settings.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configDir = filesDir.resolve("config").apply { mkdirs() }
        val settingsPath = configDir.resolve("settings.json").absolutePath
        val libraryViewModel = LibraryViewModel(
            repository = LibraryRepository(AndroidLibraryScanner()),
            settingsStore = SettingsStore(
                readText = { path -> java.io.File(path).takeIf { it.exists() }?.readText() },
                writeText = { path, content -> java.io.File(path).writeText(content) },
            ),
            settingsPath = settingsPath,
        )
        setContent {
            OmilatorApp(
                libraryViewModel = libraryViewModel,
                settingsViewModel = SettingsViewModel(),
                onAddRomDirectory = { /* TODO: Android directory picker via SAF */ },
                onPlayRom = { /* TODO: Android player screen */ },
            )
        }
    }
}
