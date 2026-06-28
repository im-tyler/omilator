@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omilator.core.audio.IosAudioOutput
import com.omilator.core.libretro.api.Framebuffer
import com.omilator.core.libretro.api.PixelFormat
import com.omilator.core.libretro.createCoreController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IosPlayerScreen(romPath: String, corePath: String, onExit: () -> Unit = {}) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var argbPixels by remember { mutableStateOf<IntArray?>(null) }
    var frameW by remember { mutableStateOf(0) }
    var frameH by remember { mutableStateOf(0) }

    val scope = remember { CoroutineScope(Dispatchers.Default) }
    val controller = remember { createCoreController("") }
    val audioOutput = remember { IosAudioOutput() }
    val buttonStates = remember { mutableStateMapOf<Int, Boolean>() }

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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D12))) {
        when {
            error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(error!!, color = Color(0xFFFF6B6B))
                }
                ExitButton(onExit)
            }
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Loading...", color = Color.White)
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // === SCREEN AREA ===
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        argbPixels?.let { pixels ->
                            if (frameW > 0 && frameH > 0) {
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                ) {
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

                    // === CONTROLLER AREA ===
                    ControllerBar(buttonStates)
                }

                // Exit button floating on top-left
                ExitButton(onExit)
            }
        }
    }
}

@Composable
private fun ExitButton(onExit: () -> Unit) {
    IconButton(
        onClick = onExit,
        modifier = Modifier
            .padding(8.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f)),
        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
    ) {
        Icon(Icons.Rounded.Close, contentDescription = "Exit", modifier = Modifier.size(20.dp))
    }
}

// === CONTROLLER ===

@Composable
private fun ControllerBar(buttonStates: MutableMap<Int, Boolean>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFF1A1A24))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // === D-PAD ===
        DpadSection(buttonStates)

        // === START / SELECT ===
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallButton("SELECT", Color(0xFF48484A), buttonStates, 2)
            SmallButton("START", Color(0xFF48484A), buttonStates, 3)
        }

        // === A / B BUTTONS ===
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ActionButton("A", Color(0xFF5856D6), buttonStates, 8)
            ActionButton("B", Color(0xFFFF2D55), buttonStates, 0)
        }
    }
}

@Composable
private fun DpadSection(buttonStates: MutableMap<Int, Boolean>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        DpadButton("\u25B2", buttonStates, 4) // UP
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            DpadButton("\u25C0", buttonStates, 6) // LEFT
            Box(modifier = Modifier.size(44.dp).background(Color(0xFF2A2A32)))
            DpadButton("\u25B6", buttonStates, 7) // RIGHT
        }
        DpadButton("\u25BC", buttonStates, 5) // DOWN
    }
}

@Composable
private fun ActionButton(
    label: String,
    color: Color,
    buttonStates: MutableMap<Int, Boolean>,
    buttonId: Int,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale = if (pressed) 0.92f else 1f

    Box(
        modifier = Modifier
            .size(64.dp * scale)
            .shadow(
                elevation = if (pressed) 2.dp else 8.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.5f),
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        color.copy(brightness = 1.3f),
                        color,
                        color.copy(brightness = 0.7f),
                    ),
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        buttonStates[buttonId] = true
                        tryAwaitRelease()
                        pressed = false
                        buttonStates[buttonId] = false
                    }
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DpadButton(
    arrow: String,
    buttonStates: MutableMap<Int, Boolean>,
    buttonId: Int,
) {
    var pressed by remember { mutableStateOf(false) }
    val color = if (pressed) Color(0xFF5A5A6A) else Color(0xFF3A3A44)
    val scale = if (pressed) 0.92f else 1f

    Box(
        modifier = Modifier
            .size((44.dp * scale))
            .shadow(
                elevation = if (pressed) 1.dp else 4.dp,
                shape = RoundedCornerShape(6.dp),
            )
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        color.copy(brightness = 1.2f),
                        color,
                        color.copy(brightness = 0.8f),
                    ),
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        buttonStates[buttonId] = true
                        tryAwaitRelease()
                        pressed = false
                        buttonStates[buttonId] = false
                    }
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(arrow, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
    }
}

@Composable
private fun SmallButton(
    label: String,
    color: Color,
    buttonStates: MutableMap<Int, Boolean>,
    buttonId: Int,
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 28.dp)
            .shadow(
                elevation = if (pressed) 1.dp else 3.dp,
                shape = RoundedCornerShape(14.dp),
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        color.copy(brightness = 1.2f),
                        color,
                    ),
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        buttonStates[buttonId] = true
                        tryAwaitRelease()
                        pressed = false
                        buttonStates[buttonId] = false
                    }
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

// === HELPERS ===

private fun Color.copy(brightness: Float): Color {
    return Color(
        red = (red * brightness).coerceIn(0f, 1f),
        green = (green * brightness).coerceIn(0f, 1f),
        blue = (blue * brightness).coerceIn(0f, 1f),
        alpha = alpha,
    )
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
                    val val5 = packed and 0x1F
                    (0xFF shl 24) or ((r5 shl 3 or (r5 shr 2)) shl 16) or ((g6 shl 2 or (g6 shr 4)) shl 8) or (val5 shl 3 or (val5 shr 2))
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
