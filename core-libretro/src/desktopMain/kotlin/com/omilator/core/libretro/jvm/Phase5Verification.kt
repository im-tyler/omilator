package com.omilator.core.libretro.jvm

import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.foreign.Arena

fun main() = runBlocking {
    val corePath = File("cores/mgba_libretro.dylib").absolutePath
    val romPath = File("cores/test.gba").absolutePath
    val sysDir = File("build/omilator-sys").apply { mkdirs() }.absolutePath

    println("== Phase 5 verification: save states ==")

    val arena = Arena.ofShared()
    val native = LibretroFfm(arena, sysDir)

    println("Loading core + ROM...")
    native.loadCore(corePath)
    native.installCallbacks()
    native.callInit()
    require(native.callLoadGame(romPath)) { "retro_load_game failed" }

    println("Running 10 frames...")
    repeat(10) { native.callRun() }

    println("\n[1/3] retro_serialize_size...")
    val size = native.callSerializeSize()
    println("  State size: $size bytes")
    require(size > 0) { "Serialize size must be > 0" }

    println("\n[2/3] retro_serialize...")
    val state = native.callSerialize()
    val stateFile = File("build/phase5/test.state").apply { parentFile?.mkdirs() }
    stateFile.writeBytes(state)
    println("  Saved ${state.size} bytes to ${stateFile.absolutePath}")
    require(state.size == size.toInt()) { "Serialized size mismatch" }

    println("\n[3/3] retro_unserialize...")
    val loaded = native.callUnserialize(state)
    println("  Unserialize returned: $loaded")
    require(loaded) { "Unserialize failed" }

    println("\n== Phase 5 RESULT ==")
    println("  State size: ${state.size} bytes")
    println("  Round-trip: OK")
    val success = size > 0 && state.size == size.toInt() && loaded
    println("  ${if (success) "PASS" else "FAIL"} — Save state pipeline works")

    native.callUnloadGame()
    native.callDeinit()
    arena.close()

    if (!success) kotlin.system.exitProcess(1)
}
