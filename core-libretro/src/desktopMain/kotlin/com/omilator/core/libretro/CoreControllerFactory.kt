package com.omilator.core.libretro

import com.omilator.core.libretro.api.CoreController
import com.omilator.core.libretro.impl.FfmCoreController
import java.nio.file.Files
import java.nio.file.Path

actual class CoreControllerFactory {
    actual fun create(): CoreController {
        val home = System.getProperty("user.home")
        val dir = Path.of(home, "Library", "Application Support", "Omilator")
        Files.createDirectories(dir)
        return FfmCoreController(systemDirectory = dir.toString())
    }
}

internal actual val platformName: String = System.getProperty("os.name") ?: "Desktop"
