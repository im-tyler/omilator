package com.omilator.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.omilator.core.audio.createAudioOutputFactory
import java.io.File

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
    val state by engine.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(gameId) { engine.start() }

    DisposableEffect(gameId) {
        onDispose {
            engine.stop()
            audioOutput.release()
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    var latestBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var fastForward by remember { mutableStateOf(false) }
    var debugFrameCount by remember { mutableStateOf(0) }
    var lastEmittedCount by remember { mutableStateOf(0) }

    // Frame pump: pull latest framebuffer from the engine on every UI frame.
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { _ ->
                val image = engine.renderFrameIfAvailable()
                if (image != null) {
                    latestBitmap = image.toComposeImageBitmap()
                }
                val emittedNow = 0
                if (emittedNow != lastEmittedCount) {
                    lastEmittedCount = emittedNow
                }
                debugFrameCount = emittedNow
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
                if (event.type != KeyEventType.KeyUp && event.type != KeyEventType.KeyDown) {
                    return@onKeyEvent false
                }
                val keyCode = event.key.nativeKeyCode
                if (event.type == KeyEventType.KeyUp && keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
                    onClose()
                    return@onKeyEvent true
                }
                val button = KeyboardMapping.buttonFor(keyCode)
                if (button != null) {
                    when (event.type) {
                        KeyEventType.KeyDown -> engine.pressButton(button)
                        KeyEventType.KeyUp -> engine.releaseButton(button)
                    }
                    return@onKeyEvent true
                }
                if (event.type == KeyEventType.KeyUp) {
                    val saveDir = File(System.getProperty("user.home"), "Library/Application Support/Omilator/saves").apply { mkdirs() }
                    val romBase = File(gameId).nameWithoutExtension
                    when (keyCode) {
                        // Fast forward toggle (Tab)
                        java.awt.event.KeyEvent.VK_TAB -> {
                            fastForward = !fastForward
                            engine.setSpeedMultiplier(if (fastForward) 3.0f else 1.0f)
                            true
                        }
                        // Save state slots: F1-F10
                        java.awt.event.KeyEvent.VK_F1 -> { engine.saveState(File(saveDir, "$romBase.slot1.state").absolutePath); true }
                        java.awt.event.KeyEvent.VK_F2 -> { engine.saveState(File(saveDir, "$romBase.slot2.state").absolutePath); true }
                        java.awt.event.KeyEvent.VK_F3 -> { engine.saveState(File(saveDir, "$romBase.slot3.state").absolutePath); true }
                        java.awt.event.KeyEvent.VK_F4 -> { engine.saveState(File(saveDir, "$romBase.slot4.state").absolutePath); true }
                        java.awt.event.KeyEvent.VK_F5 -> { engine.saveState(File(saveDir, "$romBase.slot5.state").absolutePath); true }
                        // Load state slots: Shift+F1-F5
                        java.awt.event.KeyEvent.VK_F6 -> { engine.loadState(File(saveDir, "$romBase.slot1.state").absolutePath); true }
                        java.awt.event.KeyEvent.VK_F7 -> { engine.loadState(File(saveDir, "$romBase.slot2.state").absolutePath); true }
                        java.awt.event.KeyEvent.VK_F8 -> { engine.loadState(File(saveDir, "$romBase.slot3.state").absolutePath); true }
                        java.awt.event.KeyEvent.VK_F9 -> { engine.loadState(File(saveDir, "$romBase.slot4.state").absolutePath); true }
                        java.awt.event.KeyEvent.VK_F10 -> { engine.loadState(File(saveDir, "$romBase.slot5.state").absolutePath); true }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.error != null -> ErrorOverlay(state.error!!, corePath, gameId)
            state.isLoading -> LoadingOverlay(corePath, gameId)
            latestBitmap == null -> WaitingForFramesOverlay(corePath, gameId, debugFrameCount)
            else -> EmulatedSurface(latestBitmap!!, state.geometry?.aspectRatio ?: 1.5f)
        }

        DebugOverlay(
            corePath = corePath,
            romPath = gameId,
            geometry = state.geometry,
            framesEmitted = debugFrameCount,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

@Composable
private fun EmulatedSurface(bitmap: ImageBitmap, aspectRatio: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasW = size.width
        val canvasH = size.height
        val scaleX = canvasW / bitmap.width
        val scaleY = canvasH / bitmap.height
        val scale = minOf(scaleX, scaleY)
        val drawW = bitmap.width * scale
        val drawH = bitmap.height * scale
        val dx = (canvasW - drawW) / 2f
        val dy = (canvasH - drawH) / 2f
        drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset(dx.toInt(), dy.toInt()),
            dstSize = IntSize(drawW.toInt(), drawH.toInt()),
        )
    }
}

@Composable
private fun LoadingOverlay(corePath: String, romPath: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Loading...", color = Color.White)
        Text(romPath, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
        Text(corePath, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WaitingForFramesOverlay(corePath: String, romPath: String, framesSoFar: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Waiting for first frame...", color = Color.White)
        Text("Frames emitted by core so far: $framesSoFar", color = Color.LightGray)
        Text(romPath, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ErrorOverlay(message: String, corePath: String, romPath: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Text("Error loading game", color = MaterialTheme.colorScheme.error)
        Text(message, color = Color.White, modifier = Modifier.padding(top = 8.dp))
        Text("Core: $corePath", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        Text("ROM:  $romPath", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        Text("Esc to go back", color = Color.Gray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun DebugOverlay(
    corePath: String,
    romPath: String,
    geometry: com.omilator.core.libretro.api.Geometry?,
    framesEmitted: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(8.dp),
    ) {
        Text(
            buildString {
                appendLine("core: ${File(corePath).name}")
                append("emitted: $framesEmitted")
                geometry?.let {
                    appendLine()
                    append("geom: ${it.baseWidth}x${it.baseHeight}")
                }
            },
            color = Color(0x80FFFFFF),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun resolveCorePath(romPath: String): String {
    val ext = File(romPath).extension.lowercase()
    val coreName = when (ext) {
        "gba", "gb", "gbc", "sgb" -> "mgba_libretro"
        "nes", "nez", "unf", "unif" -> "mesen_libretro"
        "sfc", "smc", "fig", "swc" -> "snes9x_libretro"
        "md", "bin", "smd", "gen" -> "genesis_plus_gx_libretro"
        "n64", "z64", "v64" -> "mupen64plus_next_libretro"
        "cue", "chd", "m3u", "pbp", "img" -> "beetle_psx_hw_libretro"
        "nds", "ids" -> "melonds_libretro"
        "cso", "prx", "elf" -> "ppsspp_libretro"
        "gcm", "gci", "ciso" -> "dolphin_libretro"
        "wbfs", "wad", "gcz" -> "dolphin_libretro"
        "3ds", "3dsx", "cci", "cxi" -> "azahar_libretro"
        "nrg", "mdf", "gz" -> "play_libretro"
        "cdi", "gdi", "gdl" -> "flycast_libretro"
        // .iso is genuinely ambiguous (PSP / GameCube / Wii / PS1 / PS2 / DC / Saturn).
        // Default to PPSSPP since PSP ISOs are the most common modern-retro .iso use.
        // User can rename to .pbp (unambiguous PS1) or .gcm (unambiguous GameCube).
        "iso" -> "ppsspp_libretro"
        else -> "mgba_libretro"
    }
    val candidates = buildList {
        val exts = listOf("dylib", "so", "dll")
        exts.forEach { add(File("cores/$coreName.$it")) }
        exts.forEach { add(File("../cores/$coreName.$it")) }
        val home = System.getProperty("user.home")
        exts.forEach { add(File("$home/Library/Application Support/Omilator/cores/$coreName.$it")) }
    }
    return candidates.firstOrNull { it.exists() }?.absolutePath
        ?: File("cores/$coreName.dylib").absolutePath
}
