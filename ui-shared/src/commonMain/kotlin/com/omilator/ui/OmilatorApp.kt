package com.omilator.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.Modifier
import com.omilator.data.library.LibraryRepository
import com.omilator.data.library.LibraryScanner
import com.omilator.ui.adaptive.WindowSizeClass
import com.omilator.ui.adaptive.currentWindowSizeClass
import com.omilator.ui.library.LibraryScreen
import com.omilator.ui.library.LibraryViewModel
import com.omilator.ui.settings.SettingsScreen

enum class OmilatorDestination { LIBRARY, SETTINGS }

@Composable
fun OmilatorApp(
    libraryScanner: LibraryScanner,
    onPlayRom: (String) -> Unit = {},
    onPickRomFile: () -> String? = { null },
) {
    OmilatorTheme {
        val windowClass = currentWindowSizeClass()
        var destination by rememberSaveable { mutableStateOf(OmilatorDestination.LIBRARY) }

        val viewModel = remember {
            LibraryViewModel(LibraryRepository(libraryScanner))
        }

        if (windowClass == WindowSizeClass.EXPANDED) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    NavigationRailItem(
                        selected = destination == OmilatorDestination.LIBRARY,
                        onClick = { destination = OmilatorDestination.LIBRARY },
                        icon = { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = "Library") },
                        label = { Text("Library") },
                    )
                    NavigationRailItem(
                        selected = destination == OmilatorDestination.SETTINGS,
                        onClick = { destination = OmilatorDestination.SETTINGS },
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                    )
                }
                when (destination) {
                    OmilatorDestination.LIBRARY -> LibraryScreen(
                        viewModel = viewModel,
                        onAddDirectory = { onPickRomFile()?.let(onPlayRom) },
                        onOpenGame = onPlayRom,
                    )
                    OmilatorDestination.SETTINGS -> SettingsScreen()
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = destination == OmilatorDestination.LIBRARY,
                            onClick = { destination = OmilatorDestination.LIBRARY },
                            icon = { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = "Library") },
                            label = { Text("Library") },
                        )
                        NavigationBarItem(
                            selected = destination == OmilatorDestination.SETTINGS,
                            onClick = { destination = OmilatorDestination.SETTINGS },
                            icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                        )
                    }
                },
            ) { padding ->
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    when (destination) {
                        OmilatorDestination.LIBRARY -> LibraryScreen(
                            viewModel = viewModel,
                            onAddDirectory = { onPickRomFile()?.let(onPlayRom) },
                            onOpenGame = onPlayRom,
                        )
                        OmilatorDestination.SETTINGS -> SettingsScreen()
                    }
                }
            }
        }
    }
}
