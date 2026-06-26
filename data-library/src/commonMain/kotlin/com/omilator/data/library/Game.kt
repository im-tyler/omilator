package com.omilator.data.library

import kotlinx.serialization.Serializable

@Serializable
enum class GameSystem(
    val displayName: String,
    val manufacturer: String,
    val releaseYear: Int,
    val extensions: List<String>,
    val preferredCore: String,
) {
    NES("Nintendo Entertainment System", "Nintendo", 1985, listOf("nes", "nez", "unf", "unif"), "mesen"),
    SNES("Super Nintendo Entertainment System", "Nintendo", 1990, listOf("sfc", "smc", "fig", "swc"), "bsnes"),
    GAME_BOY("Game Boy", "Nintendo", 1989, listOf("gb"), "sameboy"),
    GAME_BOY_COLOR("Game Boy Color", "Nintendo", 1998, listOf("gbc"), "sameboy"),
    GAME_BOY_ADVANCE("Game Boy Advance", "Nintendo", 2001, listOf("gba"), "mgba"),
    GENESIS("Sega Genesis / Mega Drive", "Sega", 1988, listOf("md", "bin", "smd", "gen"), "genesis_plus_gx"),
    NINTENDO_64("Nintendo 64", "Nintendo", 1996, listOf("n64", "z64", "v64"), "mupen64plus_next"),
    PLAYSTATION("PlayStation", "Sony", 1994, listOf("cue", "chd", "m3u"), "beetle_psx_hw");

    companion object {
        fun detectByExtension(extension: String): GameSystem? =
            entries.firstOrNull { sys -> sys.extensions.any { it.equals(extension, ignoreCase = true) } }
    }
}

@Serializable
data class Game(
    val id: String,
    val title: String,
    val system: GameSystem,
    val filePath: String,
    val fileSizeBytes: Long,
    val coverArtUrl: String? = null,
    val lastPlayedEpochMillis: Long? = null,
)
