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
    val repoName = repoNameFor(game.system) ?: return null
    val romFileName = game.filePath.substringAfterLast('/').substringBeforeLast('.')
    
    // Try multiple name variations (same logic as desktop CoverArtService)
    val variations = linkedSetOf(romFileName)
    variations.add(romFileName.replace(Regex("\\s*\\([^)]*\\)\\s*"), "").trim())
    variations.add(romFileName.substringBefore(" (").trim())

    for (variation in variations) {
        if (variation.isBlank()) continue
        val encoded = variation.encodeURLPath() + ".png"
        val url = "https://raw.githubusercontent.com/libretro-thumbnails/$repoName/master/Named_Boxarts/$encoded"
        
        val nsUrl = NSURL(string = url)
        val data = NSData.dataWithContentsOfURL(nsUrl) ?: continue
        if (data.length.toInt() < 1000) continue // too small = error page
        
        val bytes = data.bytes?.reinterpret<ByteVar>()?.readBytes(data.length.toInt()) ?: continue
        
        return try {
            val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(bytes)
            skiaImage.toComposeImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    return null
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
