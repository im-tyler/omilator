@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.omilator.core.libretro.api.CoreController
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

    // Touch input state
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
                    input = { _, _, _, id ->
                        if (buttonStates[id] == true) 1 else 0
                    },
                )

                isLoading = false
                val intervalMs = (1000.0 / avInfo.timing.fps).toLong()
                while (true) {
                    controller.runFrame()
                    val fb = latestFrame[0]
                    if (fb != null) {
                        val bytes = fb.data as? ByteArray
                        if (bytes != null) {
                            val converted = convertToArgb(bytes, fb.width.toInt(), fb.height.toInt(), fb.pitch.toInt(), fb.format)
                            if (converted != null) {
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

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {
        when {
            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Button(onClick = {
                            platform.UIKit.UIPasteboard.generalPasteboard().string = "$error\nCore: $corePath\nROM: $romPath"
                        }) { Text("Copy Error") }
                        Spacer(Modifier.height(16.dp))
                        Text(error!!, color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("Loading...", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            else -> {
                // === SCREEN AREA (top ~55%) ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    val pixels = argbPixels
                    if (pixels != null && frameW > 0 && frameH > 0) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val scaleX = size.width / frameW
                            val scaleY = size.height / frameH
                            val scale = minOf(scaleX, scaleY)
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

                // === CONTROLLER AREA (bottom ~45%) ===
                HandheldController(
                    buttonStates = buttonStates,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color(0xFF16162A)),
                )
            }
        }
    }
}

@Composable
private fun HandheldController(
    buttonStates: MutableMap<Int, Boolean>,
    modifier: Modifier = Modifier,
) {
    // Layout the controller as a canvas with touch zones

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        for (change in event.changes) {
                            val x = change.position.x
                            val y = change.position.y
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()

                            if (change.pressed && !change.previousPressed) {
                                // Press down
                                val btn = hitTestController(x, y, w, h)
                                if (btn != null) {
                                    buttonStates[btn] = true
                                }
                            } else if (!change.pressed && change.previousPressed) {
                                // Release
                                val btn = hitTestController(change.previousPosition.x, change.previousPosition.y, w, h)
                                if (btn != null) {
                                    buttonStates[btn] = false
                                }
                            }
                        }
                    }
                }
            }
    ) {
        drawController(size.width, size.height)
    }
}

private fun hitTestController(x: Float, y: Float, w: Float, h: Float): Int? {
    val s = w * 0.055f

    // D-pad cross zones
    val dcx = w * 0.22f
    val dcy = h * 0.45f

    // Up
    if (x > dcx - s && x < dcx + s && y > dcy - s * 2.5f && y < dcy - s * 0.3f) return 4
    // Down
    if (x > dcx - s && x < dcx + s && y > dcy + s * 0.3f && y < dcy + s * 2.5f) return 5
    // Left
    if (x > dcx - s * 2.5f && x < dcx - s * 0.3f && y > dcy - s && y < dcy + s) return 6
    // Right
    if (x > dcx + s * 0.3f && x < dcx + s * 2.5f && y > dcy - s && y < dcy + s) return 7

    // A button
    if (hypot(x - w * 0.78f, y - h * 0.35f) < s * 1.3f) return 8
    // B button
    if (hypot(x - w * 0.65f, y - h * 0.55f) < s * 1.3f) return 0

    // Start
    if (hypot(x - w * 0.58f, y - h * 0.85f) < s * 0.9f) return 3
    // Select
    if (hypot(x - w * 0.42f, y - h * 0.85f) < s * 0.9f) return 2

    return null
}

private fun DrawScope.drawController(w: Float, h: Float) {
    val fill = Color.White.copy(alpha = 0.18f)
    val border = Color.White.copy(alpha = 0.40f)
    val sw = 2.5f
    val s = w * 0.055f

    val dcx = w * 0.22f
    val dcy = h * 0.45f

    // === D-PAD: rounded rectangle cross ===
    val padW = s * 1.8f
    val padH = s * 1.1f
    val cr = 6f

    // Up arm
    drawRoundRect(fill, Offset(dcx - padH / 2, dcy - padW - s * 0.15f), Size(padH, padW), androidx.compose.ui.geometry.CornerRadius(cr, cr))
    drawRoundRect(border, Offset(dcx - padH / 2, dcy - padW - s * 0.15f), Size(padH, padW), androidx.compose.ui.geometry.CornerRadius(cr, cr), Stroke(sw))

    // Down arm
    drawRoundRect(fill, Offset(dcx - padH / 2, dcy + s * 0.15f), Size(padH, padW), androidx.compose.ui.geometry.CornerRadius(cr, cr))
    drawRoundRect(border, Offset(dcx - padH / 2, dcy + s * 0.15f), Size(padH, padW), androidx.compose.ui.geometry.CornerRadius(cr, cr), Stroke(sw))

    // Left arm
    drawRoundRect(fill, Offset(dcx - padW - s * 0.15f, dcy - padH / 2), Size(padW, padH), androidx.compose.ui.geometry.CornerRadius(cr, cr))
    drawRoundRect(border, Offset(dcx - padW - s * 0.15f, dcy - padH / 2), Size(padW, padH), androidx.compose.ui.geometry.CornerRadius(cr, cr), Stroke(sw))

    // Right arm
    drawRoundRect(fill, Offset(dcx + s * 0.15f, dcy - padH / 2), Size(padW, padH), androidx.compose.ui.geometry.CornerRadius(cr, cr))
    drawRoundRect(border, Offset(dcx + s * 0.15f, dcy - padH / 2), Size(padW, padH), androidx.compose.ui.geometry.CornerRadius(cr, cr), Stroke(sw))

    // Center hub
    drawRoundRect(
        Color.White.copy(alpha = 0.28f),
        Offset(dcx - padH / 2, dcy - padH / 2),
        Size(padH, padH),
        androidx.compose.ui.geometry.CornerRadius(4f, 4f),
    )

    // === A / B BUTTONS (diagonal arrangement like real GB) ===
    val aCx = w * 0.78f
    val aCy = h * 0.35f
    val bCx = w * 0.65f
    val bCy = h * 0.55f

    // A
    drawCircle(Color(0xFF4A6FA5).copy(alpha = 0.6f), s * 1.2f, Offset(aCx, aCy), style = Fill)
    drawCircle(border, s * 1.2f, Offset(aCx, aCy), style = Stroke(sw))

    // B
    drawCircle(Color(0xFFB23A48).copy(alpha = 0.6f), s * 1.2f, Offset(bCx, bCy), style = Fill)
    drawCircle(border, s * 1.2f, Offset(bCx, bCy), style = Stroke(sw))

    // === START / SELECT (pill shapes, center-bottom) ===
    drawCircle(Color.White.copy(alpha = 0.15f), s * 0.8f, Offset(w * 0.58f, h * 0.85f))
    drawCircle(Color.White.copy(alpha = 0.15f), s * 0.8f, Offset(w * 0.42f, h * 0.85f))
}

// === Frame conversion helpers (unchanged) ===

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
                    val r = (r5 shl 3) or (r5 shr 2)
                    val g = (g6 shl 2) or (g6 shr 4)
                    val b = (b5 shl 3) or (b5 shr 2)
                    (0xFF shl 24) or (r shl 16) or (g shl 8) or b
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
    val image = org.jetbrains.skia.Image.makeRaster(
        org.jetbrains.skia.ImageInfo.makeN32Premul(width, height),
        bytes,
        (width * 4),
    )
    return image.toComposeImageBitmap()
}
