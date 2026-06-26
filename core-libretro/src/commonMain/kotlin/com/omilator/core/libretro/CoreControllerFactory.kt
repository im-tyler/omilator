package com.omilator.core.libretro

import com.omilator.core.libretro.api.CoreController

expect class CoreControllerFactory {
    fun create(): CoreController
}

internal expect val platformName: String
