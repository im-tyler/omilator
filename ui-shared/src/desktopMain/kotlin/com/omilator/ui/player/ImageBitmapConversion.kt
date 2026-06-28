package com.omilator.ui.player

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

actual fun createImageBitmapFromArgb(argb: IntArray, width: Int, height: Int): ImageBitmap {
    val bytes = ByteArray(argb.size * 4)
    for (i in argb.indices) {
        val v = argb[i]
        bytes[i * 4] = (v and 0xFF).toByte()
        bytes[i * 4 + 1] = ((v shr 8) and 0xFF).toByte()
        bytes[i * 4 + 2] = ((v shr 16) and 0xFF).toByte()
        bytes[i * 4 + 3] = ((v shr 24) and 0xFF).toByte()
    }
    return Image.makeRaster(
        ImageInfo.makeN32Premul(width, height),
        bytes,
        width * 4,
    ).toComposeImageBitmap()
}
