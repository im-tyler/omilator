package com.omilator.core.libretro.api

interface VideoSink {
    fun onFrame(framebuffer: Framebuffer)
}

interface AudioSink {
    fun onSamples(samples: ShortArray)
}

interface InputSource {
    fun poll(port: Int, device: InputDevice, index: Int, id: Int): Int
}

data class Framebuffer(
    val data: Any,
    val width: UInt,
    val height: UInt,
    val pitch: UInt,
    val format: PixelFormat,
)
