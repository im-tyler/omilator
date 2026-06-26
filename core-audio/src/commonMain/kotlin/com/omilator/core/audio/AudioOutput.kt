package com.omilator.core.audio

interface AudioOutput {
    val sampleRate: Double
    val channels: Int

    fun configure(sampleRate: Double, channels: Int)
    fun write(samples: ShortArray): Int
    fun flush()
    fun release()
}

interface AudioOutputFactory {
    fun create(): AudioOutput
}

expect fun createAudioOutputFactory(): AudioOutputFactory
