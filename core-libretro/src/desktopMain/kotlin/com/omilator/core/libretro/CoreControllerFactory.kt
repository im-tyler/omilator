package com.omilator.core.libretro

import com.omilator.core.libretro.api.CoreController
import com.omilator.core.libretro.impl.FfmCoreController

actual fun createCoreController(systemDirectory: String): CoreController = FfmCoreController(systemDirectory)

internal actual val platformName: String = System.getProperty("os.name") ?: "Desktop"
