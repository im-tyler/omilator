@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.core.audio

internal class IosAudioOutput : AudioOutput {
    override var sampleRate: Double = 0.0
        private set
    override var channels: Int = 2
        private set

    override fun configure(sampleRate: Double, channels: Int) {
        this.sampleRate = sampleRate
        this.channels = channels
    }

    override fun write(samples: ShortArray): Int {
        // Audio playback via AVAudioEngine deferred — video rendering is the
        // proof point for iOS emulation. Audio comes in Phase 2.
        return samples.size
    }

    override fun flush() {}
    override fun release() {}
}

internal class IosAudioFactory {
    fun create(): AudioOutput = IosAudioOutput()
}
