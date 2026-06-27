package com.omilator.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.omilator.data.library.Game

/**
 * Platform-specific cover art loading. Returns an ImageBitmap if cover
 * art is available for [game], null otherwise. Implemented per-platform:
 *
 *   desktopMain: CoverArtService (libretro-thumbnails CDN + local files + cache)
 *   androidMain: null for now (can add content:// based loader later)
 *   iosMain:     null for now (can add UIImage-based loader later)
 */
@Composable
expect fun rememberCoverArt(game: Game): ImageBitmap?
