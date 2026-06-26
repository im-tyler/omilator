package com.omilator.core.libretro.jvm

import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.foreign.Arena

fun main() = runBlocking {
    val cores = listOf("ppsspp_libretro.dylib", "dolphin_libretro.dylib")
    val sysDir = File("build/omilator-sys").apply { mkdirs() }.absolutePath

    for (fileName in cores) {
        println("\n========= $fileName =========")
        val path = File("cores/$fileName")
        if (!path.exists()) { println("not found"); continue }

        val arena = Arena.ofShared()
        // Run on a thread so we can timeout
        val thread = Thread {
            try {
                val native = LibretroFfm(arena, sysDir)
                println("[probe] loadCore")
                native.loadCore(path.absolutePath)
                println("[probe] installEnvironmentCallback")
                native.installEnvironmentCallback()
                println("[probe] callInit")
                native.callInit()
                println("[probe] installMediaCallbacks")
                native.installMediaCallbacks()
                println("[probe] callApiVersion")
                val v = native.callApiVersion()
                println("[probe] api_version = $v")
                val (n, ver, ext) = native.callSystemInfo()
                println("[probe] $n $ver ext=$ext")
                native.callDeinit()
            } catch (t: Throwable) {
                println("[probe] EXCEPTION: ${t::class.simpleName}: ${t.message}")
            }
        }
        thread.isDaemon = true
        thread.start()
        thread.join(20_000)
        if (thread.isAlive) {
            println("[probe] STILL RUNNING after 20s — hangs in native code")
            // Can't actually kill native-thread hang from JVM; just move on
        }
        arena.close()
    }
    println("\n=== probe complete ===")
}
