@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

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
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Phase 4 v1: stub. The cinterop integration is verified (Phase 0) and the
 * C bindings are generated from libretro.h. A full implementation needs:
 *
 *   1. dlopen() the bundled libretro core (.dylib compiled for iosArm64)
 *   2. dlsym() retro_* symbols, reinterpret<>() to CFunction types
 *   3. staticCFunction trampolines for the 6 callbacks (environment,
 *      video_refresh, audio_sample, audio_sample_batch, input_poll,
 *      input_state) — these must be top-level, with a registry to
 *      dispatch back to the active NativeCoreController instance
 *   4. memScope/nativeHeap for retro_system_info + retro_game_info +
 *      retro_system_av_info structs
 *   5. An Xcode project (iosApp/) that hosts Compose and links the
 *      Kotlin/Native framework
 *
 * Runtime testing requires a real iOS device + signed libretro core.
 * The desktop FFM bridge in FfmCoreController.kt is the reference impl.
 */
internal class NativeCoreController : CoreController {

    private var loaded: Boolean = false
    private var videoSink: VideoSink? = null
    private var audioSink: AudioSink? = null
    private var inputSource: InputSource? = null

    override val isLoaded: Boolean get() = loaded
    override val memorySize: UInt = 0u

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
            timing = Timing(fps = 60f, sampleRate = 65536.0),
        )
    }

    override fun runFrame() {}
    override fun reset() {}
    override fun unloadGame() {}
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
    override fun writeMemory(region: UInt, offset: UInt, data: ByteArray) {}
}
