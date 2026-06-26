package com.omilator.core.libretro.jvm

import com.omilator.core.libretro.api.AudioSink
import com.omilator.core.libretro.api.Framebuffer
import com.omilator.core.libretro.api.InputDevice
import com.omilator.core.libretro.api.InputSource
import com.omilator.core.libretro.api.VideoSink
import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.foreign.Arena

fun main() = runBlocking {
    val corePath = File("cores/mgba_libretro.dylib").absolutePath
    val romPath = File("cores/test.gba").absolutePath
    val sysDir = File("build/omilator-sys").apply { mkdirs() }.absolutePath

    println("== Phase 1 verification ==")
    println("Core: $corePath")
    println("ROM:  $romPath")

    val arena = Arena.ofShared()
    val native = LibretroFfm(arena, sysDir)

    var framesReceived = 0
    var lastFrameInfo: String = "(none)"
    var audioBatchesReceived = 0
    var totalSamples = 0L

    native.onVideo = { _, w, h, pitch ->
        framesReceived++
        lastFrameInfo = "${w}x${h} pitch=${pitch}"
    }
    native.onAudioBatch = { data, frames ->
        audioBatchesReceived++
        totalSamples += frames * 2
        frames
    }

    println("\n[1/5] Loading core...")
    native.loadCore(corePath)
    val apiVersion = native.callApiVersion()
    println("  retro_api_version = $apiVersion (expected 1)")

    val (libName, libVersion, extensions) = native.callSystemInfo()
    println("  library_name      = $libName")
    println("  library_version   = $libVersion")
    println("  valid_extensions  = $extensions")

    println("\n[2/5] Installing callbacks...")
    native.installCallbacks()

    println("\n[3/5] retro_init...")
    native.callInit()

    println("\n[4/5] retro_load_game...")
    val loaded = native.callLoadGame(romPath)
    println("  retro_load_game returned: $loaded")
    require(loaded) { "retro_load_game failed" }

    val avInfo = native.callSystemAvInfo()
    println("  geometry: ${avInfo.baseWidth}x${avInfo.baseHeight} (max ${avInfo.maxWidth}x${avInfo.maxHeight})")
    println("  aspect:   ${avInfo.aspectRatio}")
    println("  fps:      ${avInfo.fps}")
    println("  sample_rate: ${avInfo.sampleRate} Hz")
    println("  pixel format: ${native.pixelFormat} (1=XRGB8888, 2=RGB565)")

    println("\n[5/5] Running 60 frames...")
    repeat(60) { frame ->
        native.callRun()
        if (frame == 0 || frame == 59) {
            println("  frame ${frame + 1}/60  — last video: $lastFrameInfo")
        }
    }

    println("\n== Phase 1 RESULT ==")
    println("  API version:    $apiVersion        (must be 1)")
    println("  Library:        $libName $libVersion")
    println("  Frames rendered: $framesReceived   (must be ~60)")
    println("  Audio batches:  $audioBatchesReceived  (~$totalSamples samples)")
    println("  AV info OK:     ${avInfo.baseWidth > 0 && avInfo.baseHeight > 0 && avInfo.fps > 0}")

    val success = apiVersion == 1 && libName.contains("mGBA", ignoreCase = true) && framesReceived > 0
    println("\n  ${if (success) "PASS" else "FAIL"} — FFM libretro bridge is ${if (success) "WORKING" else "BROKEN"}")

    native.callUnloadGame()
    native.callDeinit()
    arena.close()

    if (!success) kotlin.system.exitProcess(1)
}
