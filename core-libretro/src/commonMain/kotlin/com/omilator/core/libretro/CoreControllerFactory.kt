package com.omilator.core.libretro

import com.omilator.core.libretro.api.CoreController

expect fun createCoreController(systemDirectory: String): CoreController

internal expect val platformName: String
