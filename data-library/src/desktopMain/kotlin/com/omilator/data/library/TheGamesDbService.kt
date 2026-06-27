package com.omilator.data.library

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Queries TheGamesDB API for cover art. Requires a free API key
 * from https://wiki.thegamesdb.net/wiki/Get_API_Key
 *
 * If no key is configured, this service is inactive and CoverArtService
 * falls back to libretro-thumbnails.
 */
class TheGamesDbService(private val apiKey: String?) {

    private val baseUrl = "https://api.thegamesdb.net/v1"
    private val cdnBase = "https://cdn.thegamesdb.net/images/original"

    val isActive: Boolean get() = apiKey != null && apiKey.isNotBlank()

    /**
     * Searches for a game by name and returns the box art URL, or null.
     * Call from Dispatchers.IO.
     */
    fun fetchCoverUrl(gameTitle: String, system: GameSystem): String? {
        if (!isActive) return null
        val platformId = platformIdFor(system) ?: return null
        val query = URLEncoder.encode(gameTitle, "UTF-8")

        return try {
            val url = "$baseUrl/Games/ByGameName?apikey=$apiKey&name=$query&filter[platform]=$platformId"
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            // Parse JSON for box art filename
            // Response structure: { data: { games: [...], base_url: { original: "..." } } }
            val gameId = extractFirstInt(body, """"id"\s*:\s*(\d+)""") ?: return null
            val baseUrlMatch = Regex(""""original"\s*:\s*"(.*?)"""").find(body)?.groupValues?.get(1)
            val artBase = baseUrlMatch ?: cdnBase

            // Query box art for this game
            val artUrl = "$baseUrl/Games/BoxArt?apikey=$apiKey&gamesid=$gameId"
            val artConn = URL(artUrl).openConnection() as HttpURLConnection
            artConn.connectTimeout = 5000
            artConn.readTimeout = 5000
            if (artConn.responseCode != 200) return null
            val artBody = artConn.inputStream.bufferedReader().readText()
            artConn.disconnect()

            // Find the first box art filename
            val filename = Regex(""""filename"\s*:\s*"(.*?)"""").find(artBody)?.groupValues?.get(1)
            filename?.let { "$artBase/$it" }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractFirstInt(body: String, pattern: String): Int? {
        return Regex(pattern).find(body)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun platformIdFor(system: GameSystem): Int? = when (system) {
        GameSystem.NES -> 7
        GameSystem.SNES -> 6
        GameSystem.GAME_BOY -> 4
        GameSystem.GAME_BOY_COLOR -> 41
        GameSystem.GAME_BOY_ADVANCE -> 5
        GameSystem.GENESIS -> 18
        GameSystem.NINTENDO_64 -> 2
        GameSystem.PLAYSTATION -> 10
        GameSystem.NINTENDO_DS -> 8
        GameSystem.PSP -> 13
        GameSystem.GAMECUBE -> 13
        GameSystem.WII -> 9
        GameSystem.NINTENDO_3DS -> 4912
        GameSystem.PLAYSTATION_2 -> 11
        GameSystem.DREAMCAST -> 23
        GameSystem.SATURN -> 16
    }
}
