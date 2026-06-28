@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.omilator.core.libretro.api.Framebuffer
import com.omilator.core.libretro.api.PixelFormat
import com.omilator.core.libretro.createCoreController
import com.omilator.core.audio.IosAudioOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

@Composable
fun IosPlayerScreen(romPath: String, corePath: String) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var argbPixels by remember { mutableStateOf<IntArray?>(null) }
    var frameW by remember { mutableStateOf(0) }
    var frameH by remember { mutableStateOf(0) }

    val scope = remember { CoroutineScope(Dispatchers.Default) }
    val controller = remember { createCoreController("") }
    val audioOutput = remember { IosAudioOutput() }
    val buttonStates = remember { mutableMapOf<Int, Boolean>() }

    remember {
        scope.launch {
            try {
                controller.loadCore(corePath)
                val avInfo = controller.loadGame(romPath)
                frameW = avInfo.geometry.baseWidth.toInt()
                frameH = avInfo.geometry.baseHeight.toInt()
                audioOutput.configure(avInfo.timing.sampleRate, channels = 2)
                val latestFrame = arrayOfNulls<Framebuffer>(1)
                controller.attach(
                    video = { fb -> latestFrame[0] = fb },
                    audio = { samples -> audioOutput.write(samples) },
                    input = { _, _, _, id -> if (buttonStates[id] == true) 1 else 0 },
                )
                isLoading = false
                val intervalMs = (1000.0 / avInfo.timing.fps).toLong()
                while (true) {
                    controller.runFrame()
                    latestFrame[0]?.let { fb ->
                        (fb.data as? ByteArray)?.let { bytes ->
                            convertToArgb(bytes, fb.width.toInt(), fb.height.toInt(), fb.pitch.toInt(), fb.format)?.let { converted ->
                                argbPixels = converted
                                frameW = fb.width.toInt()
                                frameH = fb.height.toInt()
                            }
                        }
                    }
                    delay(intervalMs)
                }
            } catch (e: Exception) {
                error = e.message ?: e::class.simpleName
                isLoading = false
            } finally {
                audioOutput.release()
            }
        }
        true
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D12))) {
        when {
            error != null -> ErrorView(error!!, corePath, romPath)
            isLoading -> LoadingView()
            else -> {
                // === SCREEN (top 55%) ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(8.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                    ) {
                        argbPixels?.let { pixels ->
                            if (frameW > 0 && frameH > 0) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val scale = minOf(size.width / frameW, size.height / frameH)
                                    val drawW = (frameW * scale).toInt()
                                    val drawH = (frameH * scale).toInt()
                                    val dx = ((size.width - drawW) / 2f).toInt()
                                    val dy = ((size.height - drawH) / 2f).toInt()
                                    drawImage(
                                        image = createImageBitmap(pixels, frameW, frameH),
                                        srcOffset = IntOffset.Zero,
                                        srcSize = IntSize(frameW, frameH),
                                        dstOffset = IntOffset(dx, dy),
                                        dstSize = IntSize(drawW, drawH),
                                    )
                                }
                            }
                        }
                    }
                }

                // === CONTROLLER (bottom 45%) ===
                Controller3D(
                    buttonStates = buttonStates,
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                )
            }
        }
    }
}

@Composable
private fun Controller3D(
    buttonStates: MutableMap<Int, Boolean>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A24))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        for (change in event.changes) {
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            if (change.pressed && !change.previousPressed) {
                                hitTest(change.position.x, change.position.y, w, h)?.let { btn ->
                                    buttonStates[btn] = true
                                }
                            } else if (!change.pressed && change.previousPressed) {
                                hitTest(change.previousPosition.x, change.previousPosition.y, w, h)?.let { btn ->
                                    buttonStates[btn] = false
                                }
                            }
                        }
                    }
                }
            }
    ) {
        drawController3D(size.width, size.height)
    }
}

// === HIT TESTING ===

private fun hitTest(x: Float, y: Float, w: Float, h: Float): Int? {
    val s = w * 0.05f
    // D-pad
    val dcx = w * 0.24f
    val dcy = h * 0.42f
    if (x > dcx - s && x < dcx + s && y > dcy - s * 3 && y < dcy - s * 0.3f) return 4 // UP
    if (x > dcx - s && x < dcx + s && y > dcy + s * 0.3f && y < dcy + s * 3) return 5 // DOWN
    if (x > dcx - s * 3 && x < dcx - s * 0.3f && y > dcy - s && y < dcy + s) return 6 // LEFT
    if (x > dcx + s * 0.3f && x < dcx + s * 3 && y > dcy - s && y < dcy + s) return 7 // RIGHT
    // A / B
    if (hypot(x - w * 0.80f, y - h * 0.30f) < s * 1.6f) return 8 // A
    if (hypot(x - w * 0.66f, y - h * 0.52f) < s * 1.6f) return 0 // B
    // Start / Select
    if (hypot(x - w * 0.58f, y - h * 0.88f) < s * 1.0f) return 3 // START
    if (hypot(x - w * 0.42f, y - h * 0.88f) < s * 1.0f) return 2 // SELECT
    return null
}

// === 3D DRAWING ===

private fun DrawScope.drawController3D(w: Float, h: Float) {
    val s = w * 0.05f

    // === D-PAD ===
    val dcx = w * 0.24f
    val dcy = h * 0.42f
    drawDpad3D(dcx, dcy, s)

    // === A BUTTON (blue dome) ===
    drawButton3D(w * 0.80f, h * 0.30f, s * 1.5f, Color(0xFF2D6BD3), Color(0xFF5B9BF5))

    // === B BUTTON (red dome) ===
    drawButton3D(w * 0.66f, h * 0.52f, s * 1.5f, Color(0xFFC42B3C), Color(0xFFE85060))

    // === START / SELECT (dark pills) ===
    drawPill3D(w * 0.58f, h * 0.88f, s * 0.9f)
    drawPill3D(w * 0.42f, h * 0.88f, s * 0.9f)
}

private fun DrawScope.drawDpad3D(cx: Float, cy: Float, s: Float) {
    val dark = Color(0xFF2A2A32)
    val mid = Color(0xFF353540)
    val edge = Color(0xFF1E1E26)
    val armW = s * 1.4f
    val armH = s * 0.9f
    val cr = 8f

    val arms = listOf(
        Triple(cx - armH / 2, cy - armW - s * 0.1f, false),
        Triple(cx - armH / 2, cy + s * 0.1f, false),
        Triple(cx - armW - s * 0.1f, cy - armH / 2, true),
        Triple(cx + s * 0.1f, cy - armH / 2, true),
    )

    // Drop shadow
    for ((x, y, horizontal) in arms) {
        val aw = if (horizontal) armW else armH
        val ah = if (horizontal) armH else armW
        drawRoundRect(
            Color.Black.copy(alpha = 0.30f),
            Offset(x + 2f, y + 3f),
            Size(aw, ah),
            androidx.compose.ui.geometry.CornerRadius(cr, cr),
        )
    }

    // Uniform gradient arms (darker at edges, lighter at center - equal all around)
    for ((x, y, horizontal) in arms) {
        val aw = if (horizontal) armW else armH
        val ah = if (horizontal) armH else armW
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(mid, dark, edge),
                center = Offset(x + aw / 2, y + ah / 2),
                radius = maxOf(aw, ah) * 0.7f,
            ),
            topLeft = Offset(x, y),
            size = Size(aw, ah),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr, cr),
        )
    }

    // Center hub
    drawCircle(edge, armH * 0.6f, Offset(cx, cy))
    drawCircle(
        Brush.radialGradient(
            colors = listOf(mid, dark, edge),
            center = Offset(cx, cy),
            radius = armH * 0.5f,
        ),
        armH * 0.5f,
        Offset(cx, cy),
    )
}

private fun DrawScope.drawButton3D(cx: Float, cy: Float, r: Float, baseColor: Color, lightColor: Color) {
    // Drop shadow
    drawCircle(Color.Black.copy(alpha = 0.30f), r, Offset(cx + 2f, cy + 3f))

    // Darker outer rim
    drawCircle(baseColor.copy(alpha = 0.3f), r * 1.08f, Offset(cx, cy))

    // Uniform radial gradient — equally 3D, no directional light
    drawCircle(
        Brush.radialGradient(
            colors = listOf(lightColor, baseColor, Color.Black.copy(alpha = 0.3f)),
            center = Offset(cx, cy),
            radius = r * 1.1f,
        ),
        r,
        Offset(cx, cy),
    )

    // Uniform inner shadow ring for depth (evenly darker at edges)
    drawCircle(
        Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.20f)),
            center = Offset(cx, cy),
            radius = r,
        ),
        r,
        Offset(cx, cy),
    )
}

private fun DrawScope.drawPill3D(cx: Float, cy: Float, r: Float) {
    val dark = Color(0xFF2A2A32)
    val mid = Color(0xFF3A3A46)
    val edge = Color(0xFF1E1E26)

    // Shadow
    drawCircle(Color.Black.copy(alpha = 0.30f), r, Offset(cx + 1f, cy + 2f))

    // Uniform gradient
    drawCircle(
        Brush.radialGradient(
            colors = listOf(mid, dark, edge),
            center = Offset(cx, cy),
            radius = r,
        ),
        r,
        Offset(cx, cy),
    )
}

// === HELPERS ===

@Composable
private fun ErrorView(error: String, corePath: String, romPath: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Button(onClick = {
                platform.UIKit.UIPasteboard.generalPasteboard().string = "$error\nCore: $corePath\nROM: $romPath"
            }) { Text("Copy Error") }
            Spacer(Modifier.height(16.dp))
            Text(error, color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text("Loading...", color = Color.White, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private fun convertToArgb(bytes: ByteArray, width: Int, height: Int, pitch: Int, format: PixelFormat): IntArray? {
    if (width <= 0 || height <= 0) return null
    val argb = IntArray(width * height)
    val bpp = if (format == PixelFormat.RGB565) 2 else 4
    for (y in 0 until height) {
        val rowOffset = y * pitch
        for (x in 0 until width) {
            val src = rowOffset + x * bpp
            if (src + bpp > bytes.size) continue
            argb[y * width + x] = when (format) {
                PixelFormat.RGB565 -> {
                    val lo = bytes[src].toInt() and 0xFF
                    val hi = bytes[src + 1].toInt() and 0xFF
                    val packed = lo or (hi shl 8)
                    val r5 = (packed shr 11) and 0x1F
                    val g6 = (packed shr 5) and 0x3F
                    val b5 = packed and 0x1F
                    (0xFF shl 24) or ((r5 shl 3 or (r5 shr 2)) shl 16) or ((g6 shl 2 or (g6 shr 4)) shl 8) or (b5 shl 3 or (b5 shr 2))
                }
                PixelFormat.XRGB8888 -> {
                    val b = bytes[src].toInt() and 0xFF
                    val g = bytes[src + 1].toInt() and 0xFF
                    val r = bytes[src + 2].toInt() and 0xFF
                    (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }
    }
    return argb
}

private fun createImageBitmap(argb: IntArray, width: Int, height: Int): ImageBitmap {
    val bytes = ByteArray(argb.size * 4)
    for (i in argb.indices) {
        val v = argb[i]
        bytes[i * 4] = (v and 0xFF).toByte()
        bytes[i * 4 + 1] = ((v shr 8) and 0xFF).toByte()
        bytes[i * 4 + 2] = ((v shr 16) and 0xFF).toByte()
        bytes[i * 4 + 3] = ((v shr 24) and 0xFF).toByte()
    }
    return org.jetbrains.skia.Image.makeRaster(
        org.jetbrains.skia.ImageInfo.makeN32Premul(width, height),
        bytes,
        width * 4,
    ).toComposeImageBitmap()
}
