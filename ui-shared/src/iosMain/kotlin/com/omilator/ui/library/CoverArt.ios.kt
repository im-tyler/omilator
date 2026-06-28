@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.omilator.data.library.Game
import com.omilator.data.library.GameSystem
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.*
import kotlin.native.concurrent.freeze

@Composable
actual fun rememberCoverArt(game: Game): ImageBitmap? {
    val coverState = produceState<ImageBitmap?>(null, game.id) {
        value = withContext(Dispatchers.IO) {
            fetchCoverArt(game)
        }
    }
    return coverState.value
}

private fun fetchCoverArt(game: Game): ImageBitmap? {
    // 1. Try libretro-thumbnails (has non-Nintendo games)
    val repoName = repoNameFor(game.system) ?: return null
    val romFileName = game.filePath.substringAfterLast('/').substringBeforeLast('.')

    val variations = linkedSetOf(romFileName)
    variations.add(romFileName.replace(Regex("\\s*\\([^)]*\\)\\s*"), "").trim())
    variations.add(romFileName.substringBefore(" (").trim())

    for (variation in variations) {
        if (variation.isBlank()) continue
        val encoded = variation.encodeURLPath() + ".png"
        val url = "https://raw.githubusercontent.com/libretro-thumbnails/$repoName/master/Named_Boxarts/$encoded"
        val result = fetchImage(url)
        if (result != null) return result
    }

    // 2. Fallback: try ScreenScraper CDN (has ALL games including Nintendo)
    val ssSystemId = screenScraperSystemId(game.system) ?: return null
    val cleanName = game.title.replace(" ", "+").encodeURLPath()
    // ScreenScraper image CDN: direct box art URL pattern
    for (region in listOf("us", "wor", "jp", "eu")) {
        val ssUrl = "https://screenscraper.fr/image.php?gameid=0&media=box-3D&region=$region&systemeid=$ssSystemId&gamename=$cleanName"
        val result = fetchImage(ssUrl)
        if (result != null) return result
    }

    // 3. Try generic cover art repos that include Nintendo
    for (variation in variations) {
        if (variation.isBlank()) continue
        val encoded = variation.encodeURLPath()
        val url = "https://raw.githubusercontent.com/libretro-thumbnails/$repoName/Recalbox/User/_data/boxart/$encoded.png"
        val result = fetchImage(url)
        if (result != null) return result
    }

    return null
}

private fun fetchImage(urlString: String): ImageBitmap? {
    return try {
        val nsUrl = NSURL(string = urlString)
        val data = NSData.dataWithContentsOfURL(nsUrl) ?: return null
        if (data.length.toInt() < 1000) return null
        val bytes = data.bytes?.reinterpret<ByteVar>()?.readBytes(data.length.toInt()) ?: return null
        org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private fun screenScraperSystemId(system: GameSystem): Int? = when (system) {
    GameSystem.NES -> 3
    GameSystem.SNES -> 4
    GameSystem.GAME_BOY -> 9
    GameSystem.GAME_BOY_COLOR -> 10
    GameSystem.GAME_BOY_ADVANCE -> 12
    GameSystem.GENESIS -> 1
    GameSystem.NINTENDO_64 -> 14
    GameSystem.PLAYSTATION -> 57
    GameSystem.NINTENDO_DS -> 15
    GameSystem.PSP -> 61
    GameSystem.GAMECUBE -> 13
    GameSystem.WII -> 16
    GameSystem.NINTENDO_3DS -> 17
    GameSystem.PLAYSTATION_2 -> 58
    GameSystem.DREAMCAST -> 23
    GameSystem.SATURN -> 32
    else -> null
}

private fun String.encodeURLPath(): String {
    return this.map { c ->
        when {
            c.isLetterOrDigit() || c == '-' || c == '_' || c == '.' -> c.toString()
            c == ' ' -> "%20"
            else -> {
                val hex = c.code.toString(16).padStart(2, '0')
                "%$hex"
            }
        }
    }.joinToString("")
}

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
