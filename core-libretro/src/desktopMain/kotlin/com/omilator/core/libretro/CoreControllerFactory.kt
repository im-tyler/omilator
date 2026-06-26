package com.omilator.core.libretro

import com.omilator.core.libretro.api.CoreController
import com.omilator.core.libretro.impl.JniCoreController

actual class CoreControllerFactory {
    actual fun create(): CoreController = JniCoreController()
}

internal actual val platformName: String = System.getProperty("os.name") ?: "Desktop"
