package com.omilator.core.libretro

import com.omilator.core.libretro.api.CoreController
import com.omilator.core.libretro.impl.NativeCoreController

actual fun createCoreController(systemDirectory: String): CoreController = NativeCoreController()

internal actual val platformName: String = "iOS"
