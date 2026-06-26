package com.omilator.core.render

import com.omilator.core.libretro.api.Framebuffer
import com.omilator.core.libretro.api.PixelFormat

internal class StubRenderSurface : RenderSurface {
    override var width: UInt = 0u
        private set
    override var height: UInt = 0u
        private set

    private var format: PixelFormat = PixelFormat.XRGB8888

    override fun configure(width: UInt, height: UInt, format: PixelFormat, maxWidth: UInt, maxHeight: UInt) {
        this.width = width
        this.height = height
        this.format = format
    }

    override fun uploadFrame(framebuffer: Framebuffer) { /* stub */ }
    override fun release() { /* stub */ }
}

private class StubFactory : RenderSurfaceFactory {
    override fun create(): RenderSurface = StubRenderSurface()
}

actual fun createRenderSurfaceFactory(): RenderSurfaceFactory = StubFactory()
