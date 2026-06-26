package com.omilator.core.libretro.jvm

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.lang.foreign.Arena

data class CoreSpec(
    val name: String,
    val fileName: String,
    val expectedExtensions: String,
)

fun main() = runBlocking {
    val cores = listOf(
        CoreSpec("mGBA",              "mgba_libretro.dylib",             "gba|gb|gbc|sgb"),
        CoreSpec("Mesen",             "mesen_libretro.dylib",            "nes|fds|unf|unif"),
        CoreSpec("Snes9x",            "snes9x_libretro.dylib",           "smc|sfc|swc|fig"),
        CoreSpec("Genesis Plus GX",   "genesis_plus_gx_libretro.dylib",  "mdx|md|smd|bin|gen"),
        CoreSpec("Mupen64Plus-Next",  "mupen64plus_next_libretro.dylib", "n64|z64|v64|ndd"),
        CoreSpec("Beetle PSX HW",     "beetle_psx_hw_libretro.dylib",    "exe|cue|toc|ccd|m3u|pbp|chd|bin|img|iso"),
        CoreSpec("PCSX ReARMed",      "pcsx_rearmed_libretro.dylib",     "bin|cue|img|mdf|pbp|toc|cbn|m3u|chd|iso|exe"),
        CoreSpec("melonDS",           "melonds_libretro.dylib",          "nds|ids|dsi"),
        CoreSpec("Azahar (3DS)",      "azahar_libretro.dylib",           "3ds|3dsx|cci|cxi|app|elf"),
        CoreSpec("Play! (PS2)",       "play_libretro.dylib",             "iso|bin|elf|nrg|mdf|gz"),
        CoreSpec("flycast (DC)",      "flycast_libretro.dylib",          "gdi|cdi|chd|cue|gdl|m3u"),
        CoreSpec("mednafen_saturn",   "mednafen_saturn_libretro.dylib",  "cue|ccd|chd|toc|m3u|iso|bin"),
        CoreSpec("Dolphin (GC/Wii)",  "dolphin_libretro.dylib",          "gcm|iso|gcz|wbfs|wad|ciso|rvz|wia"),
        CoreSpec("PPSSPP (PSP)",      "ppsspp_libretro.dylib",           "elf|iso|cso|prx|pbp|chd"),
    )

    val sysDir = File("build/omilator-sys").apply { mkdirs() }.absolutePath
    val coresDir = File("cores")
    println("== Core loader test ==")
    println("Cores dir: ${coresDir.absolutePath}\n")

    data class Result(val spec: CoreSpec, val ok: Boolean, val libraryName: String, val libraryVersion: String, val extensions: String, val error: String?)

    val results = cores.map { spec ->
        val path = File(coresDir, spec.fileName)
        if (!path.exists()) {
            return@map Result(spec, false, "", "", "", "file not found")
        }
        print("[test] ${spec.name.padEnd(20)} ... "); System.out.flush()
        val outcome = withTimeoutOrNull(15_000) {
            val arena = Arena.ofShared()
            try {
                val native = LibretroFfm(arena, sysDir)
                native.loadCore(path.absolutePath)
                native.installEnvironmentCallback()
                native.callInit()
                native.installMediaCallbacks()
                val apiVersion = native.callApiVersion()
                val (libName, libVersion, ext) = native.callSystemInfo()
                native.callDeinit()
                Triple(apiVersion, Triple(libName, libVersion, ext), null as String?)
            } catch (t: Throwable) {
                Triple(0, Triple("", "", ""), "${t::class.simpleName}: ${t.message}")
            } finally {
                arena.close()
            }
        }
        if (outcome == null) {
            println("HANG (>15s) — likely needs HW render context")
            Result(spec, false, "", "", "", "init timeout")
        } else {
            val (apiVersion, info, err) = outcome
            val (libName, libVersion, ext) = info
            if (err != null) {
                println("FAIL: $err")
                Result(spec, false, libName, libVersion, ext, err)
            } else if (apiVersion != 1) {
                println("FAIL: api version $apiVersion")
                Result(spec, false, libName, libVersion, ext, "api version $apiVersion")
            } else {
                println("OK")
                Result(spec, true, libName, libVersion, ext, null)
            }
        }
    }

    println()
    println(String.format("%-22s %-8s %-25s %-18s %s", "CORE", "STATUS", "LIBRARY", "VERSION", "EXTENSIONS"))
    println("-".repeat(110))
    results.forEach { r ->
        val status = if (r.ok) "OK" else "FAIL"
        println(String.format("%-22s %-8s %-25s %-18s %s", r.spec.name, status, r.libraryName, r.libraryVersion, r.extensions))
        if (!r.ok) println("    reason: ${r.error}")
    }
    println()

    val passed = results.count { it.ok }
    val total = results.size
    println("== RESULT: $passed/$total cores loaded successfully ==")
    // Exit 0 even if some fail, so we can see the full table — failures are informational
}

