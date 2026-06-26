package com.omilator.core.libretro.impl

import com.omilator.core.libretro.api.AudioSink
import com.omilator.core.libretro.api.AvInfo
import com.omilator.core.libretro.api.CoreController
import com.omilator.core.libretro.api.CoreNotLoadedException
import com.omilator.core.libretro.api.Framebuffer
import com.omilator.core.libretro.api.Geometry
import com.omilator.core.libretro.api.InputSource
import com.omilator.core.libretro.api.PixelFormat
import com.omilator.core.libretro.api.SystemInfo
import com.omilator.core.libretro.api.Timing
import com.omilator.core.libretro.api.VideoSink
import com.omilator.core.libretro.jvm.AvInfo as FfmAvInfo
import com.omilator.core.libretro.jvm.LibretroFfm
import com.omilator.core.libretro.jvm.PixelFormatC
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

internal class FfmCoreController(
    private val systemDirectory: String,
) : CoreController {

    private val arena = Arena.ofShared()
    private var native: LibretroFfm? = null
    private var systemInfoCache: SystemInfo? = null
    private var avInfoCache: AvInfo? = null

    private var videoSink: VideoSink? = null
    private var audioSink: AudioSink? = null
    private var inputSource: InputSource? = null

    override val isLoaded: Boolean
        get() = native != null && avInfoCache != null

    override val memorySize: UInt = 0u

    override suspend fun loadCore(path: String): SystemInfo {
        val n = LibretroFfm(arena, systemDirectory).apply {
            loadCore(path)
            installEnvironmentCallback()
            callInit()
            installMediaCallbacks()
            onVideo = ::dispatchVideo
            onAudioBatch = ::dispatchAudioBatch
            onInputState = ::dispatchInputState
        }
        native = n
        val (name, version, ext) = n.callSystemInfo()
        val info = SystemInfo(
            libraryName = name,
            libraryVersion = version,
            validExtensions = ext.split("|").filter { it.isNotBlank() },
            needFullpath = false,
            blockExtract = false,
        )
        systemInfoCache = info
        return info
    }

    override suspend fun loadGame(romPath: String): AvInfo {
        val n = native ?: throw CoreNotLoadedException("Core not loaded")
        require(n.callLoadGame(romPath)) { "retro_load_game returned false for $romPath" }
        val av = n.callSystemAvInfo()
        val info = AvInfo(
            geometry = Geometry(
                baseWidth = av.baseWidth.toUInt(),
                baseHeight = av.baseHeight.toUInt(),
                maxWidth = av.maxWidth.toUInt(),
                maxHeight = av.maxHeight.toUInt(),
                aspectRatio = av.aspectRatio,
            ),
            timing = Timing(
                fps = av.fps.toFloat(),
                sampleRate = av.sampleRate,
            ),
        )
        avInfoCache = info
        return info
    }

    override fun runFrame() {
        native?.callRun()
    }

    override fun reset() { native?.callReset() }
    override fun unloadGame() {
        native?.callUnloadGame()
        avInfoCache = null
    }

    override fun unloadCore() {
        native?.callDeinit()
        native = null
        systemInfoCache = null
        avInfoCache = null
    }

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

    override fun saveState(path: String): Boolean {
        val n = native ?: return false
        return try {
            val bytes = n.callSerialize()
            java.io.File(path).writeBytes(bytes)
            true
        } catch (t: Throwable) {
            false
        }
    }

    override fun loadState(path: String): Boolean {
        val n = native ?: return false
        return try {
            val file = java.io.File(path)
            if (!file.exists()) return false
            val bytes = file.readBytes()
            n.callUnserialize(bytes)
        } catch (t: Throwable) {
            false
        }
    }

    override fun readMemory(region: UInt, offset: UInt, size: UInt): ByteArray = ByteArray(size.toInt())
    override fun writeMemory(region: UInt, offset: UInt, data: ByteArray) {}

    private fun dispatchVideo(data: MemorySegment, width: Int, height: Int, pitch: Long) {
        val format = when (native?.pixelFormat ?: PixelFormatC.XRGB8888) {
            PixelFormatC.XRGB8888 -> PixelFormat.XRGB8888
            PixelFormatC.RGB565 -> PixelFormat.RGB565
            else -> PixelFormat.XRGB8888
        }
        val size = (height.toLong() * pitch).coerceAtLeast(0L).toInt()
        val bytes = if (size > 0 && data.address() != 0L) {
            val copy = ByteArray(size)
            MemorySegment.copy(data, ValueLayout.JAVA_BYTE, 0, copy, 0, size)
            copy
        } else ByteArray(0)
        videoSink?.onFrame(
            Framebuffer(
                data = bytes,
                width = width.toUInt(),
                height = height.toUInt(),
                pitch = pitch.toUInt(),
                format = format,
            ),
        )
    }

    private fun dispatchAudioBatch(data: MemorySegment, frames: Long): Long {
        if (audioSink == null || data.address() == 0L) return frames
        val sampleCount = (frames * 2).toInt()
        val samples = ShortArray(sampleCount)
        MemorySegment.copy(data, ValueLayout.JAVA_SHORT, 0, samples, 0, sampleCount)
        audioSink!!.onSamples(samples)
        return frames
    }

    private fun dispatchInputState(port: Int, device: Int, index: Int, id: Int): Short {
        val source = inputSource ?: return 0
        return source.poll(port, device.toEnum(), index, id).toShort()
    }

    private fun Int.toEnum() = when (this) {
        1 -> com.omilator.core.libretro.api.InputDevice.JOYPAD
        2 -> com.omilator.core.libretro.api.InputDevice.MOUSE
        3 -> com.omilator.core.libretro.api.InputDevice.KEYBOARD
        4 -> com.omilator.core.libretro.api.InputDevice.LIGHTGUN
        5 -> com.omilator.core.libretro.api.InputDevice.ANALOG
        6 -> com.omilator.core.libretro.api.InputDevice.POINTER
        else -> com.omilator.core.libretro.api.InputDevice.NONE
    }
}
