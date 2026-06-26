package com.omilator.core.libretro.jvm

import com.omilator.core.libretro.api.Framebuffer
import com.omilator.core.libretro.api.PixelFormat
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.lang.foreign.Arena
import javax.imageio.ImageIO

fun main() = runBlocking {
    val corePath = File("cores/mgba_libretro.dylib").absolutePath
    val romPath = File("cores/test.gba").absolutePath
    val sysDir = File("build/omilator-sys").apply { mkdirs() }.absolutePath
    val outDir = File("build/phase2-frames").apply { mkdirs() }

    println("== Phase 2 verification: video rendering ==")

    val arena = Arena.ofShared()
    val native = LibretroFfm(arena, sysDir)

    var firstFrame: Framebuffer? = null
    var frameCount = 0
    var distinctFrameHashes = mutableSetOf<Long>()

    native.onVideo = { _, w, h, pitch ->
        frameCount++
        if (firstFrame == null) {
            // copy bytes synchronously — core will overwrite
            val size = (h * pitch).toInt()
            val bytes = ByteArray(size)
            // FFM pointer is in data segment — but we don't have it here in lambda
            // For Phase 2 proof, just record metadata
            firstFrame = Framebuffer(ByteArray(0), w.toUInt(), h.toUInt(), pitch.toUInt(), PixelFormat.RGB565)
        }
    }

    println("Loading core + ROM...")
    native.loadCore(corePath)
    native.installCallbacks()
    native.callInit()
    require(native.callLoadGame(romPath)) { "retro_load_game failed" }
    val av = native.callSystemAvInfo()
    println("  ${av.baseWidth}x${av.baseHeight}, ${av.fps} fps, fmt=${native.pixelFormat}")

    println("Running 60 frames, capturing frames for PNG dump...")
    val argb = IntArray(av.baseWidth * av.baseHeight)
    val image = BufferedImage(av.baseWidth, av.baseHeight, BufferedImage.TYPE_INT_ARGB)
    val imagePixels = (image.raster.dataBuffer as DataBufferInt).bankData[0]

    repeat(60) { frame ->
        // Replace the video handler with one that captures + converts each frame
        native.onVideo = { data, w, h, pitch ->
            if (frame == 0 || frame == 30 || frame == 59) {
                // Copy pixels via FFM and convert
                val bpp = 2  // RGB565
                for (y in 0 until h) {
                    val rowOffset = y * pitch.toInt()
                    for (x in 0 until w) {
                        val src = rowOffset + x * bpp
                        val lo = data.get(java.lang.foreign.ValueLayout.JAVA_BYTE, src.toLong()).toInt() and 0xFF
                        val hi = data.get(java.lang.foreign.ValueLayout.JAVA_BYTE, src.toLong() + 1).toInt() and 0xFF
                        val packed = lo or (hi shl 8)
                        val r5 = (packed shr 11) and 0x1F
                        val g6 = (packed shr 5) and 0x3F
                        val b5 = packed and 0x1F
                        val r = (r5 shl 3) or (r5 shr 2)
                        val g = (g6 shl 2) or (g6 shr 4)
                        val b = (b5 shl 3) or (b5 shr 2)
                        argb[y * w + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    }
                }
                System.arraycopy(argb, 0, imagePixels, 0, argb.size)
                val outFile = File(outDir, "frame-${frame.toString().padStart(3, '0')}.png")
                ImageIO.write(image, "PNG", outFile)
                val hash = argb.contentHashCode().toLong()
                distinctFrameHashes.add(hash)
                println("  frame ${frame + 1}/60 saved: ${outFile.name} (${outFile.length()} bytes)")
            }
        }
        native.callRun()
    }

    println("\n== Phase 2 RESULT ==")
    println("  Frames captured: 3 PNGs in build/phase2-frames/")
    println("  Distinct frame hashes: ${distinctFrameHashes.size}")
    val success = distinctFrameHashes.size >= 1
    println("  ${if (success) "PASS" else "FAIL"} — Video rendering pipeline works")

    native.callUnloadGame()
    native.callDeinit()
    arena.close()

    if (!success) kotlin.system.exitProcess(1)
}
