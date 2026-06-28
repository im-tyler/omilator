@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.core.audio

import kotlinx.cinterop.*
import platform.AVFAudio.*

class IosAudioOutput : AudioOutput {
    override var sampleRate: Double = 0.0
        private set
    override var channels: Int = 2
        private set

    private var engine: AVAudioEngine? = null
    private var playerNode: AVAudioPlayerNode? = null
    private var started = false

    override fun configure(sampleRate: Double, channels: Int) {
        this.sampleRate = sampleRate
        this.channels = channels
        stop()

        val eng = AVAudioEngine()
        val node = AVAudioPlayerNode()
        eng.attachNode(node)
        val hwFormat = eng.outputNode.outputFormatForBus(0u)
        eng.connect(node, eng.mainMixerNode, hwFormat)
        engine = eng
        playerNode = node
        eng.prepare()
        node.play()
        eng.startAndReturnError(null)
        started = true
        println("[Audio] Started: ${sampleRate}Hz → ${hwFormat.sampleRate}Hz")
    }

    override fun write(samples: ShortArray): Int {
        if (!started || samples.isEmpty()) return samples.size
        // Simplified: just buffer the samples. Full AVAudioPCMBuffer
        // scheduling requires ObjC bridging that's complex in Kotlin/Native.
        // Audio plays when we can create proper PCM buffers.
        // For now, samples are consumed (prevents buffer overflow) but silent.
        return samples.size
    }

    override fun flush() {
        playerNode?.stop()
        playerNode?.play()
    }

    override fun release() {
        stop()
    }

    private fun stop() {
        if (started) {
            playerNode?.stop()
            engine?.stop()
            started = false
        }
        engine = null
        playerNode = null
    }
}
