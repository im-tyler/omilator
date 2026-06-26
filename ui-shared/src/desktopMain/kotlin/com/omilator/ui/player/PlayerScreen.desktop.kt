package com.omilator.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import java.io.File
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.omilator.core.audio.createAudioOutputFactory
import kotlinx.coroutines.launch
import java.awt.EventQueue

@Composable
fun PlayerScreen(
    gameId: String,
    onClose: () -> Unit,
) {
    val corePath = remember(gameId) { resolveCorePath(gameId) }
    val audioOutput = remember { createAudioOutputFactory().create() }
    val engine = remember(gameId) {
        PlayerEngine(
            corePath = corePath,
            romPath = gameId,
            audioOutput = audioOutput,
        )
    }
    val scope = rememberCoroutineScope()
    val state by engine.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(gameId) {
        engine.start()
    }

    DisposableEffect(gameId) {
        onDispose {
            engine.stop()
            audioOutput.release()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var latestBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { _ ->
                val image = engine.renderFrameIfAvailable()
                if (image != null) {
                    latestBitmap = image.toComposeImageBitmap()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                val keyCode = event.key.nativeKeyCode
                val button = KeyboardMapping.buttonFor(keyCode)
                if (button != null) {
                    when (event.type) {
                        KeyEventType.KeyDown -> engine.pressButton(button)
                        KeyEventType.KeyUp -> engine.releaseButton(button)
                    }
                    return@onKeyEvent true
                }
                if (event.type == KeyEventType.KeyUp) {
                    val saveDir = File(System.getProperty("user.home"), "Library/Application Support/Omilator/saves")
                    if (!saveDir.exists()) saveDir.mkdirs()
                    val romBase = File(gameId).nameWithoutExtension
                    when (keyCode) {
                        java.awt.event.KeyEvent.VK_F5 -> {
                            val path = File(saveDir, "$romBase.state").absolutePath
                            engine.saveState(path)
                            true
                        }
                        java.awt.event.KeyEvent.VK_F8 -> {
                            val path = File(saveDir, "$romBase.state").absolutePath
                            engine.loadState(path)
                            true
                        }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        latestBitmap?.let { bmp ->
            val geom = state.geometry
            val scale = if (geom != null) {
                val (cw, ch) = currentWindowPixels()
                val sx = cw.toFloat() / geom.baseWidth.toFloat()
                val sy = ch.toFloat() / geom.baseHeight.toFloat()
                minOf(sx, sy)
            } else 1f
            val drawW = (bmp.width * scale).toInt().coerceAtLeast(1)
            val drawH = (bmp.height * scale).toInt().coerceAtLeast(1)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawImage(
                    image = bmp,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(bmp.width, bmp.height),
                    dstOffset = IntOffset(
                        ((size.width - drawW) / 2).toInt(),
                        ((size.height - drawH) / 2).toInt(),
                    ),
                    dstSize = IntSize(drawW, drawH),
                )
            }
        }
    }
}

private fun resolveCorePath(romPath: String): String {
    val romFile = java.io.File(romPath)
    val ext = romFile.extension.lowercase()
    val coreName = when (ext) {
        "gba", "gb", "gbc", "sgb" -> "mgba_libretro"
        "nes", "nez" -> "mesen_libretro"
        "sfc", "smc" -> "snes9x_libretro"
        "md", "bin", "smd", "gen" -> "genesis_plus_gx_libretro"
        else -> "mgba_libretro"
    }
    val baseDir = java.io.File("cores").absoluteFile
    val candidates = listOf(
        java.io.File(baseDir, "$coreName.dylib"),
        java.io.File(baseDir, "$coreName.so"),
        java.io.File(baseDir, "$coreName.dll"),
    )
    return candidates.firstOrNull { it.exists() }?.absolutePath
        ?: java.io.File(baseDir, "$coreName.dylib").absolutePath
}

private fun currentWindowPixels(): Pair<Int, Int> {
    var size: Pair<Int, Int> = Pair(800, 600)
    EventQueue.invokeLater {
        // best-effort: we don't have direct access here
    }
    return size
}
