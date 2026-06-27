@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.core.libretro.impl

import com.omilator.core.libretro.api.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.ThreadLocal
import platform.posix.RTLD_NOW as _RTLD_NOW
import platform.posix.dlopen
import platform.posix.dlsym
import platform.posix.dlclose
import platform.posix.dlerror as _dlerror
import platform.posix.fopen
import platform.posix.fclose
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.fread

@ThreadLocal
internal var nativeControllerInstance: NativeCoreController? = null

internal val envCb = staticCFunction { cmd: Int, data: CPointer<ByteVar>? ->
    val ctrl = nativeControllerInstance
    if (ctrl != null) {
        when (cmd) {
            10 -> { // SET_PIXEL_FORMAT
                if (data != null) {
                    ctrl.pixelFormat = data.reinterpret<IntVar>()[0]
                }
                true
            }
            0, 9, 19, 31, 51, 69 -> true
            else -> false
        }
    } else false
}

internal val videoCb = staticCFunction { data: CPointer<ByteVar>?, width: Int, height: Int, pitch: Int ->
    val ctrl = nativeControllerInstance
    if (ctrl != null && data != null && width > 0 && height > 0 && pitch > 0) {
        val size = height * pitch
        val bytes = data.readBytes(size)
        val format = if (ctrl.pixelFormat == 2) PixelFormat.RGB565 else PixelFormat.XRGB8888
        ctrl.videoSink?.onFrame(Framebuffer(bytes, width.toUInt(), height.toUInt(), pitch.toUInt(), format))
    }
}

internal val audioBatchCb = staticCFunction { data: CPointer<ByteVar>?, frames: Int ->
    val ctrl = nativeControllerInstance
    if (ctrl != null && data != null && frames > 0) {
        // Interpret data as int16_t* (stereo samples)
        val shortPtr = data.reinterpret<ShortVar>()
        val count = frames * 2
        val samples = ShortArray(count) { i -> shortPtr[i] }
        ctrl.audioSink?.onSamples(samples)
    }
    frames
}

internal val audioSampleCb = staticCFunction { left: Int, right: Int ->
    nativeControllerInstance?.audioSink?.onSamples(shortArrayOf(left.toShort(), right.toShort()))
    Unit
}

// Input poll is a no-op — core handles polling via input_state callback
internal val inputPollCb: COpaquePointer? = null

internal val inputStateCb = staticCFunction { port: Int, device: Int, index: Int, id: Int ->
    val ctrl = nativeControllerInstance ?: return@staticCFunction 0.toShort()
    if (port != 0 || device != 1) return@staticCFunction 0.toShort()
    val src = ctrl.inputSource ?: return@staticCFunction 0.toShort()
    src.poll(port, InputDevice.JOYPAD, index, id).toShort()
}

internal class NativeCoreController : CoreController {
    private var handle: CPointer<*>? = null
    private var loaded = false
    internal var pixelFormat = 1
    internal var videoSink: VideoSink? = null
    internal var audioSink: AudioSink? = null
    internal var inputSource: InputSource? = null

    override val isLoaded get() = loaded
    override val memorySize: UInt = 0u

    override suspend fun loadCore(path: String): SystemInfo {
        nativeControllerInstance = this
        val h = dlopen(path, _RTLD_NOW)
        if (h == null) {
            val err = _dlerror()?.toKString() ?: "unknown"
            throw RuntimeException("dlopen failed: $path — $err")
        }
        handle = h

        // Set callbacks — cast function pointers to opaque
        val envPtr: COpaquePointer? = envCb
        val videoPtr: COpaquePointer? = videoCb
        val audioBatchPtr: COpaquePointer? = audioBatchCb
        val audioSamplePtr: COpaquePointer? = audioSampleCb
        // inputPoll is optional — core polls on-demand via inputState
        val inputStatePtr: COpaquePointer? = inputStateCb

        // Order matters: environment BEFORE init, others AFTER init
        dlsym(h, "retro_set_environment")?.reinterpret<CFunction<(COpaquePointer?) -> Unit>>()?.invoke(envPtr)

        // retro_init
        dlsym(h, "retro_init")?.reinterpret<CFunction<() -> Unit>>()?.invoke()

        // Set remaining callbacks AFTER init
        dlsym(h, "retro_set_video_refresh")?.reinterpret<CFunction<(COpaquePointer?) -> Unit>>()?.invoke(videoPtr)
        dlsym(h, "retro_set_audio_sample_batch")?.reinterpret<CFunction<(COpaquePointer?) -> Unit>>()?.invoke(audioBatchPtr)
        dlsym(h, "retro_set_audio_sample")?.reinterpret<CFunction<(COpaquePointer?) -> Unit>>()?.invoke(audioSamplePtr)
        dlsym(h, "retro_set_input_state")?.reinterpret<CFunction<(COpaquePointer?) -> Unit>>()?.invoke(inputStatePtr)

        // retro_get_system_info
        var name = "unknown"
        var version = "0.0"
        var ext = ""
        memScoped {
            val buf = allocArray<ByteVar>(48)
            dlsym(h, "retro_get_system_info")
                ?.reinterpret<CFunction<(CPointer<ByteVar>?) -> Unit>>()
                ?.invoke(buf)
            val ptrs = buf.reinterpret<CPointerVar<ByteVar>>()
            name = ptrs[0]?.toKString() ?: "unknown"
            version = ptrs[1]?.toKString() ?: "0.0"
            ext = ptrs[2]?.toKString() ?: ""
        }

        loaded = true
        return SystemInfo(name, version, ext.split("|").filter { it.isNotBlank() }, false, false)
    }

    // Persist game info struct — must survive beyond memScoped block
    private var gameInfoNative: NativePlacement? = null

    override suspend fun loadGame(romPath: String): AvInfo {
        check(loaded) { throw CoreNotLoadedException("Core not loaded") }

        // Allocate retro_game_info on nativeHeap, ZERO-INITIALIZED.
        // struct retro_game_info { char* path; void* data; size_t size; char* meta; } = 32 bytes
        val info = nativeHeap.allocArray<ByteVar>(32)
        val zeroBytes = ByteArray(32)
        zeroBytes.copyInto(info.readBytes(32))

        // mGBA can't fopen() inside the iOS Simulator sandbox — load ROM
        // into memory and pass data+size instead of relying on path.
        val romBytes = readRomToBytes(romPath)
        val dataSize = romBytes.size

        if (romBytes.isNotEmpty()) {
            // Copy ROM data to nativeHeap — write each byte, not readBytes!
            val dataBuf = nativeHeap.allocArray<ByteVar>(dataSize)
            for (i in 0 until dataSize) {
                dataBuf[i] = romBytes[i]
            }

            // Set path (for metadata/display)
            val pathBytes = romPath.encodeToByteArray() + 0.toByte()
            val pathBuf = nativeHeap.allocArray<ByteVar>(pathBytes.size)
            pathBytes.copyInto(pathBuf.readBytes(pathBytes.size))

            // retro_game_info layout (arm64):
            // offset 0: path (char*), offset 8: data (void*),
            // offset 16: size (Long), offset 24: meta (char*)
            info.reinterpret<CPointerVar<ByteVar>>()[0] = pathBuf
            info.reinterpret<CPointerVar<ByteVar>>()[1] = dataBuf
            info.reinterpret<LongVar>()[2] = dataSize.toLong()
        }

        val ok = dlsym(handle, "retro_load_game")
            ?.reinterpret<CFunction<(CPointer<ByteVar>?) -> Boolean>>()
            ?.invoke(info) ?: false
        require(ok) { "retro_load_game returned false" }

        return AvInfo(
            Geometry(240u, 160u, 240u, 160u, 1.5f),
            Timing(60f, 32768.0),
        )
    }

    override fun runFrame() {
        // CRITICAL: set thread-local on THIS thread (runFrame runs on a
        // different coroutine thread than loadCore). Without this, callbacks
        // find nativeControllerInstance = null → EXC_BAD_ACCESS.
        nativeControllerInstance = this
        try {
            dlsym(handle, "retro_run")?.reinterpret<CFunction<() -> Unit>>()?.invoke()
        } catch (e: Throwable) {
            println("[NativeCoreController] retro_run threw: ${e.message}")
        }
    }

    override fun reset() { dlsym(handle, "retro_reset")?.reinterpret<CFunction<() -> Unit>>()?.invoke() }
    override fun unloadGame() {
        dlsym(handle, "retro_unload_game")?.reinterpret<CFunction<() -> Unit>>()?.invoke()
        gameInfoNative = null
    }
    override fun unloadCore() {
        dlsym(handle, "retro_deinit")?.reinterpret<CFunction<() -> Unit>>()?.invoke()
        handle?.let { dlclose(it) }
        handle = null; loaded = false; nativeControllerInstance = null
    }

    override fun attach(video: VideoSink, audio: AudioSink, input: InputSource) {
        videoSink = video; audioSink = audio; inputSource = input
    }
    override fun detach() { videoSink = null; audioSink = null; inputSource = null }
    override fun saveState(path: String) = false
    override fun loadState(path: String) = false
    override fun readMemory(region: UInt, offset: UInt, size: UInt) = ByteArray(size.toInt())
    override fun writeMemory(region: UInt, offset: UInt, data: ByteArray) {}
    override fun saveStateToMemory() = ByteArray(0)
    override fun loadStateFromMemory(bytes: ByteArray) = false
    override fun cheatReset() {}
    override fun cheatSet(index: Int, enabled: Boolean, code: String) {}

    internal fun handleEnv(cmd: Int, data: COpaquePointer?): Boolean {
        return when (cmd) {
            10 -> { // SET_PIXEL_FORMAT
                data?.reinterpret<IntVar>()?.let { pixelFormat = it.pointed.value }
                true
            }
            0, 9, 19, 31, 51, 69 -> true
            else -> false
        }
    }
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun readRomToBytes(path: String): ByteArray = memScoped {
    // Use POSIX fopen/fread — avoids Foundation API quirks
    val fp = fopen(path, "rb")
        ?: throw RuntimeException("Cannot open ROM: $path")
    try {
        fseek(fp, 0, 2) // SEEK_END
        val size = ftell(fp).toInt()
        fseek(fp, 0, 0) // SEEK_SET
        if (size <= 0) throw RuntimeException("ROM empty: $path")
        val buf = allocArray<ByteVar>(size)
        fread(buf, 1.toULong(), size.toULong(), fp)
        buf.readBytes(size)
    } finally {
        fclose(fp)
    }
}
