package com.omilator.data.library

import com.omilator.data.library.GameSystem
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder

/**
 * Fetches and caches box art for games.
 *
 * Source priority:
 *   1. Local file alongside ROM (<rom-name>.png or .jpg)
 *   2. libretro-thumbnails GitHub CDN (exact ROM name match)
 *   3. Not found → returns null (GameCard shows gradient fallback)
 *
 * Covers are cached at <cacheDir>/<system>/<rom-name>.png to avoid
 * re-fetching on subsequent library scans.
 */
class CoverArtService(private val cacheDir: File) {

    init {
        cacheDir.mkdirs()
    }

    /**
     * Returns the cached cover art file for [game], fetching if necessary.
     * Returns null if no cover is available. Call from Dispatchers.IO.
     */
    fun resolveCover(game: Game): File? {
        // 1. Check cache
        val cacheFile = File(cacheDir, "${game.system.name}/${sanitizeFileName(game.title)}.png")
        if (cacheFile.exists() && cacheFile.length() > 0) return cacheFile
        if (cacheFile.exists() && cacheFile.length() == 0L) return null  // cached miss

        // 2. Check local file alongside ROM
        val romFile = File(game.filePath)
        val localPng = File(romFile.parentFile, romFile.nameWithoutExtension + ".png")
        val localJpg = File(romFile.parentFile, romFile.nameWithoutExtension + ".jpg")
        when {
            localPng.exists() -> return cacheFrom(localPng, cacheFile)
            localJpg.exists() -> return cacheFrom(localJpg, cacheFile)
        }

        // 3. Try libretro-thumbnails GitHub CDN
        val repoName = repoNameFor(game.system) ?: return cacheMiss(cacheFile)
        val url = buildThumbnailUrl(repoName, game.title)
        return tryFetch(url, cacheFile) ?: cacheMiss(cacheFile)
    }

    private fun buildThumbnailUrl(repoName: String, romTitle: String): String {
        val encoded = URLEncoder.encode("$romTitle.png", "UTF-8").replace("+", "%20")
        return "https://raw.githubusercontent.com/libretro-thumbnails/$repoName/master/Named_Boxarts/$encoded"
    }

    private fun tryFetch(url: String, cacheFile: File): File? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.instanceFollowRedirects = true
            if (conn.responseCode == 200) {
                cacheFile.parentFile?.mkdirs()
                cacheFile.writeBytes(conn.inputStream.readBytes())
                conn.disconnect()
                cacheFile
            } else {
                conn.disconnect()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun cacheFrom(source: File, cacheFile: File): File {
        cacheFile.parentFile?.mkdirs()
        source.copyTo(cacheFile, overwrite = true)
        return cacheFile
    }

    /** Cache a "miss" (empty file) so we don't retry on every scan. */
    private fun cacheMiss(cacheFile: File): File? {
        cacheFile.parentFile?.mkdirs()
        cacheFile.writeText("")
        return null
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(100)

    /** Maps GameSystem to the libretro-thumbnails GitHub repo name. */
    private fun repoNameFor(system: GameSystem): String? = when (system) {
        GameSystem.NES -> "Nintendo_-_Nintendo_Entertainment_System"
        GameSystem.SNES -> "Nintendo_-_Super_Nintendo_Entertainment_System"
        GameSystem.GAME_BOY -> "Nintendo_-_Game_Boy"
        GameSystem.GAME_BOY_COLOR -> "Nintendo_-_Game_Boy_Color"
        GameSystem.GAME_BOY_ADVANCE -> "Nintendo_-_Game_Boy_Advance"
        GameSystem.GENESIS -> "Sega_-_Mega_Drive_-_Genesis"
        GameSystem.NINTENDO_64 -> "Nintendo_-_Nintendo_64"
        GameSystem.PLAYSTATION -> "Sony_-_PlayStation"
        GameSystem.NINTENDO_DS -> "Nintendo_-_Nintendo_DS"
        GameSystem.PSP -> "Sony_-_PlayStation_Portable"
        GameSystem.GAMECUBE -> "Nintendo_-_GameCube"
        GameSystem.WII -> "Nintendo_-_Wii"
        GameSystem.NINTENDO_3DS -> "Nintendo_-_Nintendo_3DS"
        GameSystem.PLAYSTATION_2 -> "Sony_-_PlayStation_2"
        GameSystem.DREAMCAST -> "Sega_-_Dreamcast"
        GameSystem.SATURN -> "Sega_-_Saturn"
    }
}
