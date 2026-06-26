package com.omilator.core.libretro.jvm

import com.omilator.core.libretro.api.PixelFormat
import com.omilator.core.libretro.jvm.LibretroLayouts.cString
import com.omilator.core.libretro.jvm.LibretroLayouts.gameGeometry
import com.omilator.core.libretro.jvm.LibretroLayouts.systemAvInfo
import com.omilator.core.libretro.jvm.LibretroLayouts.systemInfo
import com.omilator.core.libretro.jvm.LibretroLayouts.systemTiming
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.VarHandle

internal class LibretroFfm(
    private val arena: Arena,
    private val systemDirectory: String,
) {
    private val linker = Linker.nativeLinker()

    var onVideo: ((MemorySegment, Int, Int, Long) -> Unit)? = null
    var onAudioBatch: ((MemorySegment, Long) -> Long)? = null
    var onInputState: ((Int, Int, Int, Int) -> Short)? = null

    var pixelFormat: Int = PixelFormatC.XRGB8888
        private set

    private var apiVersion: MethodHandle? = null
    private var getSystemInfo: MethodHandle? = null
    private var getSystemAvInfo: MethodHandle? = null
    private var setEnvironment: MethodHandle? = null
    private var setVideoRefresh: MethodHandle? = null
    private var setAudioSample: MethodHandle? = null
    private var setAudioSampleBatch: MethodHandle? = null
    private var setInputPoll: MethodHandle? = null
    private var setInputState: MethodHandle? = null
    private var initHandle: MethodHandle? = null
    private var deinitHandle: MethodHandle? = null
    private var loadGameHandle: MethodHandle? = null
    private var unloadGameHandle: MethodHandle? = null
    private var runHandle: MethodHandle? = null
    private var resetHandle: MethodHandle? = null
    private var serializeSizeHandle: MethodHandle? = null
    private var serializeHandle: MethodHandle? = null
    private var unserializeHandle: MethodHandle? = null

    private val systemDirSeg = arena.allocateUtf8String(systemDirectory)
    private val logCbStruct by lazy { allocateLogCallbackStruct() }

    fun loadCore(path: String) {
        val sym = SymbolLookup.libraryLookup(path, arena)

        apiVersion = sym.down("retro_api_version", FunctionDescriptor.of(ValueLayout.JAVA_INT))
        getSystemInfo = sym.down(
            "retro_get_system_info",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS.withTargetLayout(systemInfo)),
        )
        getSystemAvInfo = sym.down(
            "retro_get_system_av_info",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS.withTargetLayout(systemAvInfo)),
        )
        setEnvironment = sym.down("retro_set_environment", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS))
        setVideoRefresh = sym.down("retro_set_video_refresh", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS))
        setAudioSample = sym.down("retro_set_audio_sample", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS))
        setAudioSampleBatch = sym.down("retro_set_audio_sample_batch", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS))
        setInputPoll = sym.down("retro_set_input_poll", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS))
        setInputState = sym.down("retro_set_input_state", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS))
        initHandle = sym.down("retro_init", FunctionDescriptor.ofVoid())
        deinitHandle = sym.down("retro_deinit", FunctionDescriptor.ofVoid())
        loadGameHandle = sym.down(
            "retro_load_game",
            FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS.withTargetLayout(LibretroLayouts.gameInfo)),
        )
        unloadGameHandle = sym.down("retro_unload_game", FunctionDescriptor.ofVoid())
        runHandle = sym.down("retro_run", FunctionDescriptor.ofVoid())
        resetHandle = sym.down("retro_reset", FunctionDescriptor.ofVoid())
        serializeSizeHandle = sym.down("retro_serialize_size", FunctionDescriptor.of(ValueLayout.JAVA_LONG))
        serializeHandle = sym.down(
            "retro_serialize",
            FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
        )
        unserializeHandle = sym.down(
            "retro_unserialize",
            FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
        )
    }

    fun installEnvironmentCallback() {
        val env = upcallBound(
            "onEnvironment",
            MethodType.methodType(Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType, MemorySegment::class.java),
            FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_INT, ValueLayout.ADDRESS),
        )
        setEnvironment!!.invoke(env)
    }

    fun installMediaCallbacks() {
        val video = upcallBound(
            "onVideo",
            MethodType.methodType(Void::class.javaPrimitiveType, MemorySegment::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Long::class.javaPrimitiveType),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG),
        )
        setVideoRefresh!!.invoke(video)

        val audioBatch = upcallBound(
            "onAudioBatch",
            MethodType.methodType(Long::class.javaPrimitiveType, MemorySegment::class.java, Long::class.javaPrimitiveType),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
        )
        setAudioSampleBatch!!.invoke(audioBatch)

        val audioSample = upcallBound(
            "onAudioSample",
            MethodType.methodType(Void::class.javaPrimitiveType, Short::class.javaPrimitiveType, Short::class.javaPrimitiveType),
            FunctionDescriptor.ofVoid(ValueLayout.JAVA_SHORT, ValueLayout.JAVA_SHORT),
        )
        setAudioSample!!.invoke(audioSample)

        val inputPoll = upcallBound(
            "onInputPoll",
            MethodType.methodType(Void::class.javaPrimitiveType),
            FunctionDescriptor.ofVoid(),
        )
        setInputPoll!!.invoke(inputPoll)

        val inputState = upcallBound(
            "onInputState",
            MethodType.methodType(Short::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType),
            FunctionDescriptor.of(ValueLayout.JAVA_SHORT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT),
        )
        setInputState!!.invoke(inputState)
    }

    @Deprecated("Use installEnvironmentCallback + installMediaCallbacks in the canonical order", ReplaceWith("installEnvironmentCallback()"))
    fun installCallbacks() {
        installEnvironmentCallback()
        installMediaCallbacks()
    }

    fun callInit() { initHandle!!.invoke() }
    fun callDeinit() { deinitHandle!!.invoke() }
    fun callRun() { runHandle!!.invoke() }
    fun callReset() { resetHandle!!.invoke() }
    fun callUnloadGame() { unloadGameHandle!!.invoke() }

    fun callSerializeSize(): Long = serializeSizeHandle!!.invoke() as Long

    fun callSerialize(): ByteArray {
        val size = callSerializeSize()
        require(size > 0L) { "Core returned zero serialize size" }
        val buf = arena.allocate(size)
        val ok = serializeHandle!!.invoke(buf, size) as Boolean
        check(ok) { "retro_serialize returned false" }
        val bytes = ByteArray(size.toInt())
        MemorySegment.copy(buf, ValueLayout.JAVA_BYTE, 0, bytes, 0, size.toInt())
        return bytes
    }

    fun callUnserialize(bytes: ByteArray): Boolean {
        val size = bytes.size.toLong()
        val buf = arena.allocate(size)
        MemorySegment.copy(bytes, 0, buf, ValueLayout.JAVA_BYTE, 0, bytes.size)
        return unserializeHandle!!.invoke(buf, size) as Boolean
    }

    fun callApiVersion(): Int = apiVersion!!.invoke() as Int

    fun callSystemInfo(): Triple<String, String, String> {
        val seg = arena.allocate(systemInfo)
        getSystemInfo!!.invoke(seg)
        val name = readCString(seg, systemInfo.varHandle(path("library_name")))
        val version = readCString(seg, systemInfo.varHandle(path("library_version")))
        val ext = readCString(seg, systemInfo.varHandle(path("valid_extensions")))
        return Triple(name, version, ext)
    }

    fun callSystemAvInfo(): AvInfo {
        val seg = arena.allocate(systemAvInfo)
        getSystemAvInfo!!.invoke(seg)
        return AvInfo(
            baseWidth = systemAvInfo.varHandle(*ppath("geometry", "base_width")).get(seg) as Int,
            baseHeight = systemAvInfo.varHandle(*ppath("geometry", "base_height")).get(seg) as Int,
            maxWidth = systemAvInfo.varHandle(*ppath("geometry", "max_width")).get(seg) as Int,
            maxHeight = systemAvInfo.varHandle(*ppath("geometry", "max_height")).get(seg) as Int,
            aspectRatio = systemAvInfo.varHandle(*ppath("geometry", "aspect_ratio")).get(seg) as Float,
            fps = systemAvInfo.varHandle(*ppath("timing", "fps")).get(seg) as Double,
            sampleRate = systemAvInfo.varHandle(*ppath("timing", "sample_rate")).get(seg) as Double,
        )
    }

    fun callLoadGame(romPath: String): Boolean {
        val pathSeg = arena.allocateUtf8String(romPath)
        val info = arena.allocate(LibretroLayouts.gameInfo)
        info.set(ValueLayout.ADDRESS, 0, pathSeg)
        info.set(
            ValueLayout.JAVA_LONG,
            LibretroLayouts.gameInfo.byteOffset(path("size")),
            0L,
        )
        val result = loadGameHandle!!.invoke(info)
        return result as Boolean
    }

    @Suppress("unused")
    fun onEnvironment(cmd: Int, data: MemorySegment): Boolean {
        val handled = when (cmd) {
            RetroEnv.SET_PIXEL_FORMAT -> {
                pixelFormat = data.reinterpret(4L).get(ValueLayout.JAVA_INT, 0)
                true
            }
            RetroEnv.GET_SYSTEM_DIRECTORY,
            RetroEnv.GET_SAVE_DIRECTORY -> {
                data.reinterpret(8L).set(ValueLayout.ADDRESS, 0, systemDirSeg)
                true
            }
            RetroEnv.GET_LIBRETRO_PATH,
            RetroEnv.GET_API_VERSION,
            RetroEnv.GET_INPUT_BITMASKS,
            RetroEnv.GET_AUDIO_VIDEO_ENABLE -> true
            RetroEnv.GET_LOG_INTERFACE -> {
                // Provide a retro_log_callback struct (just a function pointer).
                // PPSSPP and others crash if they store null then call it.
                data.reinterpret(8L).set(ValueLayout.ADDRESS, 0, logCbStruct)
                true
            }
            RetroEnv.GET_RUMBLE_INTERFACE,
            RetroEnv.GET_CAMERA_DRIVER,
            RetroEnv.GET_TARGET_REFRESH_RATE,
            RetroEnv.SET_HW_RENDER,
            RetroEnv.GET_PREFERRED_HW_RENDER -> {
                val ctxType = if (cmd == RetroEnv.SET_HW_RENDER && data.address() != 0L) {
                    runCatching { data.reinterpret(4L).get(ValueLayout.JAVA_INT, 0) }.getOrDefault(-1)
                } else -1
                if (cmd == RetroEnv.SET_HW_RENDER) {
                    val typeName = when (ctxType) {
                        1 -> "OPENGL"; 2 -> "OPENGLES"; 3 -> "OPENGL_CORE"
                        6 -> "VULKAN"; 12 -> "METAL"; else -> "?"
                    }
                    println("[Omilator] core requested HW_RENDER ctx=$typeName — declining")
                }
                false
            }
            else -> false
        }
        if (!handled && cmd !in silentEnvCmds) {
            println("[Omilator] env cmd $cmd unhandled (declining)")
        }
        return handled
    }

    private fun allocateLogCallbackStruct(): MemorySegment {
        // retro_log_callback { retro_log_printf_t log; }
        // Provide a stub that swallows varargs safely (C calling convention
        // allows extra args in registers/stack that we just don't read).
        val stub = upcallBound(
            "onLog",
            MethodType.methodType(Void::class.javaPrimitiveType, Int::class.javaPrimitiveType, MemorySegment::class.java),
            FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS),
        )
        val struct = arena.allocate(8L)
        struct.set(ValueLayout.ADDRESS, 0, stub)
        return struct
    }

    @Suppress("unused")
    fun onLog(level: Int, fmt: MemorySegment) {
        if (fmt.address() == 0L) return
        val msg = fmt.reinterpret(8192L).getUtf8String(0)
        // Strip trailing newline if present
        print("[core/log] $msg" + if (!msg.endsWith("\n")) "\n" else "")
    }

    private val silentEnvCmds = setOf(
        RetroEnv.GET_VARIABLE,
        RetroEnv.SET_VARIABLES,
        RetroEnv.SET_INPUT_DESCRIPTORS,
        RetroEnv.SET_CONTROLLER_INFO,
        RetroEnv.GET_VARIABLE_UPDATE,
        RetroEnv.GET_CORE_OPTIONS_VERSION,
        RetroEnv.SET_CORE_OPTIONS,
        RetroEnv.GET_CORE_OPTIONS_UPDATE,
        RetroEnv.SET_FRAME_TIME_CALLBACK,
        RetroEnv.SET_AUDIO_CALLBACK,
        RetroEnv.GET_INPUT_INTERFACE,
    )

    @Suppress("unused")
    fun onVideo(data: MemorySegment, width: Int, height: Int, pitch: Long) {
        if (data.address() == 0L || width <= 0 || height <= 0) {
            onVideo?.invoke(data, width, height, pitch)
            return
        }
        val size = height.toLong() * pitch
        val sized = data.reinterpret(size)
        onVideo?.invoke(sized, width, height, pitch)
    }

    @Suppress("unused")
    fun onAudioBatch(data: MemorySegment, frames: Long): Long {
        if (data.address() == 0L || frames <= 0L) return frames
        val bytes = frames * 2L * 2L // stereo 16-bit
        val sized = data.reinterpret(bytes)
        return onAudioBatch?.invoke(sized, frames) ?: frames
    }

    @Suppress("unused")
    fun onAudioSample(left: Short, right: Short) {
        // Phase 1: ignore single-sample audio (cores almost always use batch).
    }

    @Suppress("unused")
    fun onInputPoll() {
        // Phase 1: no-op. Input is queried on demand via onInputState.
    }

    @Suppress("unused")
    fun onInputState(port: Int, device: Int, index: Int, id: Int): Short {
        return onInputState?.invoke(port, device, index, id) ?: 0
    }

    private fun upcallBound(methodName: String, mt: MethodType, fd: FunctionDescriptor): MemorySegment {
        val mh = MethodHandles.lookup().findVirtual(LibretroFfm::class.java, methodName, mt).bindTo(this)
        return linker.upcallStub(mh, fd, arena)
    }

    private fun readCString(structSeg: MemorySegment, handle: VarHandle): String {
        val ptr = handle.get(structSeg) as MemorySegment
        if (ptr.address() == 0L) return ""
        return ptr.reinterpret(8192L).getUtf8String(0)
    }

    private fun SymbolLookup.down(name: String, fd: FunctionDescriptor): MethodHandle {
        val addr = find(name).orElseThrow { UnsatisfiedLinkError("$name not found in core") }
        return linker.downcallHandle(addr, fd)
    }

    private fun path(name: String) = MemoryLayout.PathElement.groupElement(name)
    private fun ppath(vararg names: String): Array<MemoryLayout.PathElement> =
        names.map { MemoryLayout.PathElement.groupElement(it) }.toTypedArray()
}

internal data class AvInfo(
    val baseWidth: Int,
    val baseHeight: Int,
    val maxWidth: Int,
    val maxHeight: Int,
    val aspectRatio: Float,
    val fps: Double,
    val sampleRate: Double,
)
