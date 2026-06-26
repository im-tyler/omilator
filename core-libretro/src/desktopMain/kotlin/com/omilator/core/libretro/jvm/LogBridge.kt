package com.omilator.core.libretro.jvm

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/**
 * Loads the C-side log/rumble bridge from libomilator_log.dylib.
 *
 * Why this exists: JDK 21's FFM API cannot reliably upcall into Kotlin
 * for variadic C functions on macOS arm64 — the generated stub triggers
 * BUS_ADRALN crashes when libretro cores call log(level, fmt, ...) with
 * variadic args. PPSSPP, Dolphin, and others hit this during retro_init.
 *
 * The .dylib exposes plain C functions with stable ABI. We resolve them
 * here via raw dlopen/dlsym (FFM SymbolLookup returns wrong addresses
 * for cross-module symbol resolution on macOS) and pass the raw
 * addresses into libretro callback structs — no FFM upcall on the hot
 * path.
 */
internal object LogBridge {

    // --- FFM bindings for dlopen/dlsym (must initialize first) ---

    private const val RTLD_LAZY = 1
    private const val RTLD_NOW = 2
    private const val RTLD_LOCAL = 4
    private const val RTLD_GLOBAL = 0x100

    private val linker = Linker.nativeLinker()
    private val systemLookup = linker.defaultLookup()

    private val dlopenHandle: MemorySegment? = run {
        val candidates = listOf(
            java.io.File("cores/libomilator_log.dylib"),
            java.io.File("../cores/libomilator_log.dylib"),
            java.io.File(System.getProperty("user.home") + "/Library/Application Support/Omilator/cores/libomilator_log.dylib"),
        )
        val path = candidates.firstOrNull { it.exists() }?.absolutePath ?: run {
            println("[LogBridge] libomilator_log.dylib not found in any candidate location")
            return@run null
        }
        println("[LogBridge] dlopen($path)")
        val handle = dlopen(path!!, RTLD_NOW or RTLD_LOCAL)
        if (handle == null) {
            println("[LogBridge] dlopen FAILED")
        } else {
            println("[LogBridge] dlopen handle: ${handle.address()}")
            val logAddr = dlsym(handle, "omilator_log_callback")
            println("[LogBridge] omilator_log_callback resolved to: ${logAddr?.address()}")
        }
        handle
    }

    val isAvailable: Boolean get() = dlopenHandle != null

    /** Returns the raw address of omilator_log_callback, or null if unavailable. */
    fun logCallbackAddress(): MemorySegment? {
        val handle = dlopenHandle ?: return null
        return dlsym(handle, "omilator_log_callback")
    }

    /** Returns the raw address of omilator_rumble_callback, or null if unavailable. */
    fun rumbleCallbackAddress(): MemorySegment? {
        val handle = dlopenHandle ?: return null
        return dlsym(handle, "omilator_rumble_callback")
    }

    // --- dlopen / dlsym bindings ---

    private fun dlopen(path: String, flags: Int): MemorySegment? {
        val mh = linker.downcallHandle(
            systemLookup.find("dlopen").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
        )
        val pathSeg = Arena.ofAuto().allocateUtf8String(path)
        val result = mh.invoke(pathSeg, flags) as MemorySegment
        return result.takeIf { it.address() != 0L }
    }

    private fun dlsym(handle: MemorySegment, name: String): MemorySegment? {
        val mh = linker.downcallHandle(
            systemLookup.find("dlsym").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
        )
        val nameSeg = Arena.ofAuto().allocateUtf8String(name)
        val result = mh.invoke(handle, nameSeg) as MemorySegment
        return result.takeIf { it.address() != 0L }
    }
}

