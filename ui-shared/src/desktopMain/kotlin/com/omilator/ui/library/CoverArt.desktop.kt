package com.omilator.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.omilator.data.library.CoverArtService
import com.omilator.data.library.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO

@Composable
actual fun rememberCoverArt(game: Game): ImageBitmap? {
    val coverState = produceState<ImageBitmap?>(null, game.id) {
        val cacheDir = File(System.getProperty("user.home"), "Library/Application Support/Omilator/covers")
        val service = CoverArtService(cacheDir)
        value = withContext(Dispatchers.IO) {
            runCatching {
                service.resolveCover(game)?.let { file ->
                    ImageIO.read(file)?.toComposeImageBitmap()
                }
            }.getOrNull()
        }
    }
    return coverState.value
}
