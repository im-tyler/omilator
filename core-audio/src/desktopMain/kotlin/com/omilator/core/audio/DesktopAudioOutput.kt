package com.omilator.core.audio

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

internal class DesktopAudioOutput : AudioOutput {
    private var line: SourceDataLine? = null
    override var sampleRate: Double = 0.0
        private set
    override var channels: Int = 2
        private set

    override fun configure(sampleRate: Double, channels: Int) {
        this.sampleRate = sampleRate
        this.channels = channels
        val format = AudioFormat(
            sampleRate.toFloat(),
            16,
            channels,
            true,
            false,
        )
        val info = DataLine.Info(SourceDataLine::class.java, format)
        line?.close()
        val newLine = AudioSystem.getLine(info) as SourceDataLine
        newLine.open(format, (sampleRate * channels * 0.1).toInt() * 2)
        newLine.start()
        line = newLine
    }

    override fun write(samples: ShortArray): Int {
        val l = line ?: return 0
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val v = samples[i].toInt()
            bytes[i * 2] = (v and 0xFF).toByte()
            bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return l.write(bytes, 0, bytes.size) / 2
    }

    override fun flush() {
        line?.flush()
    }

    override fun release() {
        line?.let { it.drain(); it.close() }
        line = null
    }
}

private class DesktopFactory : AudioOutputFactory {
    override fun create(): AudioOutput = DesktopAudioOutput()
}

actual fun createAudioOutputFactory(): AudioOutputFactory = DesktopFactory()
