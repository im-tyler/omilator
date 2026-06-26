package com.omilator.core.libretro.jvm.gl

import com.omilator.core.libretro.jvm.PixelFormatC
import com.omilator.core.libretro.jvm.gl.GlContext
import org.lwjgl.opengl.GL
import org.lwjgl.system.FunctionProvider
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 * Bridges libretro SET_HW_RENDER with our hidden GL context + FBO.
 *
 * Layout of retro_hw_render_callback on macOS arm64:
 *   0:  context_type (int)
 *   4:  major (uint)
 *   8:  minor (uint)
 *  12:  depth (bool)
 *  13:  stencil (bool)
 *  14:  cache_context (bool)
 *  16:  context_reset (fn ptr)
 *  24:  context_destroy (fn ptr)
 *  32:  get_current_framebuffer (fn ptr — frontend fills)
 *  40:  get_proc_address (fn ptr — frontend fills)
 *  48:  bottom_left_origin (bool — frontend fills)
 */
internal class HwRenderBridge(private val arena: Arena) {

    private var gl: GlContext? = null
    private var contextResetHandle: MethodHandle? = null
    private var contextDestroyHandle: MethodHandle? = null
    private val functionProvider: FunctionProvider = GL.getFunctionProvider()
        ?: error("LWJGL function provider unavailable — GL.createCapabilities() not called")

    /**
     * Returns true if the requested context type is one we support (OPENGL core).
     * Sets up the GL context + writes the frontend-provided callback pointers
     * into the struct, then calls the core's context_reset.
     */
    fun handleRequest(data: MemorySegment): Boolean {
        if (data.address() == 0L) return false
        val sized = data.reinterpret(56L)
        val ctxType = sized.get(ValueLayout.JAVA_INT, 0)
        val supportedType = when (ctxType) {
            HW_CONTEXT_OPENGL, HW_CONTEXT_OPENGL_CORE,
            HW_CONTEXT_OPENGLES2, HW_CONTEXT_OPENGLES3,
            HW_CONTEXT_OPENGLES_VERSION -> true
            else -> false
        }
        if (!supportedType) {
            println("[Omilator] SET_HW_RENDER: unsupported context_type=$ctxType — declining")
            return false
        }

        val major = sized.get(ValueLayout.JAVA_INT, 4)
        val minor = sized.get(ValueLayout.JAVA_INT, 8)
        println("[Omilator] SET_HW_RENDER: OpenGL ctx_type=$ctxType v$major.$minor — providing")

        // Step 1: create GL context
        println("[HwRender] step 1: creating GlContext")
        if (gl == null) {
            try {
                gl = GlContext.create()
                println("[HwRender] GlContext created OK")
            } catch (t: Throwable) {
                println("[HwRender] GlContext.create FAILED: ${t::class.simpleName}: ${t.message}")
                return false
            }
        }

        // Step 2: read core's function pointers
        println("[HwRender] step 2: reading core fn ptrs")
        val resetSeg = sized.get(ValueLayout.ADDRESS, 16)
        val destroySeg = sized.get(ValueLayout.ADDRESS, 24)
        val linker = Linker.nativeLinker()
        if (resetSeg.address() != 0L) {
            contextResetHandle = linker.downcallHandle(resetSeg, FunctionDescriptor.ofVoid())
            println("[HwRender] context_reset at ${resetSeg.address()}")
        }
        if (destroySeg.address() != 0L) {
            contextDestroyHandle = linker.downcallHandle(destroySeg, FunctionDescriptor.ofVoid())
            println("[HwRender] context_destroy at ${destroySeg.address()}")
        }

        // Step 3: provide frontend callbacks
        println("[HwRender] step 3: providing get_current_framebuffer + get_proc_address")
        val getFbStub = upcallGetFramebuffer()
        val getProcStub = upcallGetProcAddress()
        sized.set(ValueLayout.ADDRESS, 32, getFbStub)
        sized.set(ValueLayout.ADDRESS, 40, getProcStub)
        sized.set(ValueLayout.JAVA_BYTE, 48, 1)  // bottom_left_origin

        // Step 4: call core's context_reset — it sets up its own GL resources
        println("[HwRender] step 4: calling context_reset (core will set up GL)")
        try {
            contextResetHandle?.invoke()
            println("[HwRender] context_reset returned OK")
        } catch (t: Throwable) {
            println("[HwRender] context_reset threw: ${t::class.simpleName}: ${t.message}")
            return false
        }
        return true
    }

    fun ensureFramebufferSize(width: Int, height: Int) {
        gl?.resize(width, height)
    }

    fun makeCurrent() { gl?.makeCurrent() }
    fun unbind() { gl?.unbind() }

    fun readPixels(): ByteArray? = gl?.readPixelsRGBA()
    fun framebufferWidth(): Int = gl?.width() ?: 0
    fun framebufferHeight(): Int = gl?.height() ?: 0
    val isActive: Boolean get() = gl != null

    fun destroy() {
        try { contextDestroyHandle?.invoke() } catch (_: Throwable) {}
        gl?.destroy()
        gl = null
    }

    // ---- Upcall stubs we hand to the core ----

    private fun upcallGetFramebuffer(): MemorySegment {
        val mh = MethodHandles.lookup().findVirtual(
            HwRenderBridge::class.java, "onGetCurrentFramebuffer",
            MethodType.methodType(Long::class.javaPrimitiveType),
        ).bindTo(this)
        val fd = FunctionDescriptor.of(ValueLayout.JAVA_LONG)
        return Linker.nativeLinker().upcallStub(mh, fd, arena)
    }

    private fun upcallGetProcAddress(): MemorySegment {
        val mh = MethodHandles.lookup().findVirtual(
            HwRenderBridge::class.java, "onGetProcAddress",
            MethodType.methodType(Long::class.javaPrimitiveType, MemorySegment::class.java),
        ).bindTo(this)
        val fd = FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
        return Linker.nativeLinker().upcallStub(mh, fd, arena)
    }

    @Suppress("unused")
    private fun onGetCurrentFramebuffer(): Long {
        return (gl?.framebufferId() ?: 0).toLong()
    }

    @Suppress("unused")
    private fun onGetProcAddress(sym: MemorySegment): Long {
        if (sym.address() == 0L) return 0L
        val name = sym.reinterpret(256L).getUtf8String(0)
        val addr = functionProvider.getFunctionAddress(name)
        if (addr == 0L) {
            // Suppress noisy log for known-unimportant lookups
            if (!name.startsWith("glDebugMessage") && name !in silentGlLookups) {
                println("[Omilator] get_proc_address: unresolved '$name'")
            }
        }
        return addr
    }

    private val silentGlLookups = setOf(
        "glFrameTerminatorGREMEDY",
        "glSpecializeShaderARB",
        "glClientWaitSync",
    )

    companion object {
        private const val HW_CONTEXT_NONE = 0
        private const val HW_CONTEXT_OPENGL = 1
        private const val HW_CONTEXT_OPENGLES = 2
        private const val HW_CONTEXT_OPENGL_CORE = 3
        private const val HW_CONTEXT_OPENGLES2 = 4
        private const val HW_CONTEXT_OPENGLES3 = 5
        private const val HW_CONTEXT_OPENGLES_VERSION = 6
        private const val HW_CONTEXT_VULKAN = 7
        private const val HW_CONTEXT_D3D9 = 8
        private const val HW_CONTEXT_D3D10 = 9
        private const val HW_CONTEXT_D3D11 = 10
        private const val HW_CONTEXT_D3D12 = 11
        private const val HW_CONTEXT_METAL = 12
        private const val HW_CONTEXT_DUMMY = 13
    }
}
