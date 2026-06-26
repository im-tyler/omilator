package com.omilator.core.libretro

import com.omilator.core.libretro.api.CoreController
import com.omilator.core.libretro.impl.NativeCoreController

actual class CoreControllerFactory {
    actual fun create(): CoreController = NativeCoreController()
}

internal actual val platformName: String = "iOS"
