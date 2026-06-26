package com.omilator.core.audio

internal class StubAudioOutput : AudioOutput {
    override var sampleRate: Double = 0.0
        private set
    override var channels: Int = 2
        private set

    override fun configure(sampleRate: Double, channels: Int) {
        this.sampleRate = sampleRate
        this.channels = channels
    }

    override fun write(samples: ShortArray): Int = samples.size
    override fun flush() { /* stub */ }
    override fun release() { /* stub */ }
}

private class StubFactory : AudioOutputFactory {
    override fun create(): AudioOutput = StubAudioOutput()
}

actual fun createAudioOutputFactory(): AudioOutputFactory = StubFactory()
