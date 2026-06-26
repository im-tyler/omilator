package com.omilator.core.render

import com.omilator.core.libretro.api.Framebuffer
import com.omilator.core.libretro.api.PixelFormat

interface RenderSurface {
    val width: UInt
    val height: UInt

    fun configure(width: UInt, height: UInt, format: PixelFormat, maxWidth: UInt, maxHeight: UInt)
    fun uploadFrame(framebuffer: Framebuffer)
    fun release()
}

interface RenderSurfaceFactory {
    fun create(): RenderSurface
}

expect fun createRenderSurfaceFactory(): RenderSurfaceFactory
