package com.omilator.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

/**
 * Android audio output via AudioTrack (MODE_STREAM).
 *
 * Equivalent of IosAudioOutput (AVAudioEngine + AVAudioPCMBuffer) but
 * using Android's standard audio API. The libretro core delivers S16LE
 * stereo samples via write(); we push them straight into AudioTrack which
 * handles sample-rate conversion to the device HW rate internally.
 *
 * Threading: AudioTrack.write blocks when the internal buffer is full,
 * which gives us natural backpressure — no need for the MAX_PENDING cap
 * we use on iOS.
 */
internal class AndroidAudioOutput : AudioOutput {
    override var sampleRate: Double = 0.0
        private set
    override var channels: Int = 2
        private set

    private var track: AudioTrack? = null

    override fun configure(sampleRate: Double, channels: Int) {
        this.sampleRate = sampleRate
        this.channels = channels
        stop()

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate.toInt(),
            if (channels == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(sampleRate.toInt() / 4) // floor of ~250ms

        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate.toInt())
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(if (channels == 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        t.play()
        track = t
    }

    override fun write(samples: ShortArray): Int {
        val t = track ?: return samples.size
        if (samples.isEmpty()) return 0
        // AudioTrack.write(short[]) blocks until the buffer has room —
        // natural backpressure, never throws, returns # samples written.
        return t.write(samples, 0, samples.size) / 1
    }

    override fun flush() {
        track?.pause()
        track?.flush()
        track?.play()
    }

    override fun release() {
        stop()
    }

    private fun stop() {
        track?.let {
            it.pause()
            it.flush()
            it.release()
        }
        track = null
    }
}

private class AndroidFactory : AudioOutputFactory {
    override fun create(): AudioOutput = AndroidAudioOutput()
}

/** Wired into commonMain via expect fun createAudioOutputFactory(). */
actual fun createAudioOutputFactory(): AudioOutputFactory = AndroidFactory()
