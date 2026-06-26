package com.omilator.core.libretro.api

fun interface VideoSink {
    fun onFrame(framebuffer: Framebuffer)
}

fun interface AudioSink {
    fun onSamples(samples: ShortArray)
}

fun interface InputSource {
    fun poll(port: Int, device: InputDevice, index: Int, id: Int): Int
}

data class Framebuffer(
    val data: Any,
    val width: UInt,
    val height: UInt,
    val pitch: UInt,
    val format: PixelFormat,
)
