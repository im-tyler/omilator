@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.omilator.core.libretro.api.CoreController
import com.omilator.core.libretro.api.Framebuffer
import com.omilator.core.libretro.api.PixelFormat
import com.omilator.core.libretro.createCoreController
import com.omilator.core.audio.AudioOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.posix.clock
import platform.posix.CLOCKS_PER_SEC

@Composable
fun IosPlayerScreen(romPath: String, corePath: String) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var argbPixels by remember { mutableStateOf<IntArray?>(null) }
    var frameW by remember { mutableStateOf(0) }
    var frameH by remember { mutableStateOf(0) }

    val scope = remember { CoroutineScope(Dispatchers.Default) }
    val controller = remember { createCoreController("") }

    remember {
        scope.launch {
            try {
                controller.loadCore(corePath)
                val avInfo = controller.loadGame(romPath)
                frameW = avInfo.geometry.baseWidth.toInt()
                frameH = avInfo.geometry.baseHeight.toInt()

                val latestFrame = arrayOfNulls<Framebuffer>(1)

                controller.attach(
                    video = { fb -> latestFrame[0] = fb },
                    audio = { _ -> },
                    input = { _, _, _, _ -> 0 },
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
            }
        }
        true
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text("Error", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Core: $corePath", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Text("ROM: $romPath", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        platform.UIKit.UIPasteboard.generalPasteboard().string = "${error}\nCore: $corePath\nROM: $romPath"
                    }) { Text("Copy Error") }
                }
            }
            isLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Loading...", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                }
            }
            argbPixels != null && frameW > 0 && frameH > 0 -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pixels = argbPixels!!
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
            else -> Text("Waiting for frames...", color = Color.White)
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
    // Convert IntArray to ByteArray in ARGB byte order for Skia
    val bytes = ByteArray(argb.size * 4)
    for (i in argb.indices) {
        val v = argb[i]
        bytes[i * 4] = (v and 0xFF).toByte()         // B
        bytes[i * 4 + 1] = ((v shr 8) and 0xFF).toByte()  // G
        bytes[i * 4 + 2] = ((v shr 16) and 0xFF).toByte() // R
        bytes[i * 4 + 3] = ((v shr 24) and 0xFF).toByte() // A
    }
    val image = org.jetbrains.skia.Image.makeRaster(
        org.jetbrains.skia.ImageInfo.makeN32Premul(width, height),
        bytes,
        (width * 4),
    )
    return image.toComposeImageBitmap()
}
