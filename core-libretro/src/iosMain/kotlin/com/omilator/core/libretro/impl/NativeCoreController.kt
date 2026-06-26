package com.omilator.core.libretro.impl

import com.omilator.core.libretro.api.AudioSink
import com.omilator.core.libretro.api.AvInfo
import com.omilator.core.libretro.api.CoreController
import com.omilator.core.libretro.api.CoreNotLoadedException
import com.omilator.core.libretro.api.Geometry
import com.omilator.core.libretro.api.InputSource
import com.omilator.core.libretro.api.SystemInfo
import com.omilator.core.libretro.api.Timing
import com.omilator.core.libretro.api.VideoSink

internal class NativeCoreController : CoreController {
    private var loaded: Boolean = false
    private var videoSink: VideoSink? = null
    private var audioSink: AudioSink? = null
    private var inputSource: InputSource? = null

    override val isLoaded: Boolean get() = loaded
    override val memorySize: UInt get() = 0u

    override suspend fun loadCore(path: String): SystemInfo {
        loaded = true
        return SystemInfo(
            libraryName = "stub",
            libraryVersion = "0.0",
            validExtensions = emptyList(),
            needFullpath = false,
            blockExtract = false,
        )
    }

    override suspend fun loadGame(romPath: String): AvInfo {
        check(loaded) { throw CoreNotLoadedException("Core not loaded") }
        return AvInfo(
            geometry = Geometry(
                baseWidth = 240u,
                baseHeight = 160u,
                maxWidth = 240u,
                maxHeight = 160u,
                aspectRatio = 1.5f,
            ),
            timing = Timing(fps = 60f, sampleRate = 32768.0),
        )
    }

    override fun runFrame() {
        check(loaded) { throw CoreNotLoadedException("Core not loaded") }
    }

    override fun reset() { /* stub */ }
    override fun unloadGame() { /* stub */ }
    override fun unloadCore() { loaded = false }

    override fun attach(video: VideoSink, audio: AudioSink, input: InputSource) {
        videoSink = video
        audioSink = audio
        inputSource = input
    }

    override fun detach() {
        videoSink = null
        audioSink = null
        inputSource = null
    }

    override fun saveState(path: String): Boolean = false
    override fun loadState(path: String): Boolean = false
    override fun readMemory(region: UInt, offset: UInt, size: UInt): ByteArray = ByteArray(size.toInt())
    override fun writeMemory(region: UInt, offset: UInt, data: ByteArray) { /* stub */ }
}
