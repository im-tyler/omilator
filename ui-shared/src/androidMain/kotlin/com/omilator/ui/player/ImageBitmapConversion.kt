package com.omilator.ui.player

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun createImageBitmapFromArgb(argb: IntArray, width: Int, height: Int): ImageBitmap {
    // createBitmap(int[], width, height, Config.ARGB_8888) — Android's
    // int[] bitmaps are 0xAARRGGBB by default which matches our input.
    val bitmap = Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)
    return bitmap.asImageBitmap()
}
