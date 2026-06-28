package com.omilator.ui.player

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Cross-platform ARGB IntArray → ImageBitmap conversion. Each target
 * implements this with its preferred image API:
 *   - iOS: org.jetbrains.skia.Image.makeRaster (Skia is the Compose backend)
 *   - Android: android.graphics.Bitmap.createBitmap + asImageBitmap()
 *     (Android uses its own rendering pipeline; Skia isn't directly
 *     accessible from Kotlin source even though it's the underlying impl)
 *   - Desktop: same Skia path as iOS
 *
 * Input is ARGB8888 (0xAARRGGBB) row-major, [width]*[height] pixels.
 */
expect fun createImageBitmapFromArgb(argb: IntArray, width: Int, height: Int): ImageBitmap
