package com.omilator.data.settings

import kotlinx.serialization.Serializable

@Serializable
data class GameSettings(
    val gameId: String,
    val coreOverride: String? = null,
    val scaleMode: ScaleMode = ScaleMode.ASPECT,
    val filter: Filter = Filter.NONE,
    val audioEnabled: Boolean = true,
    val audioVolume: Float = 1.0f,
    val inputMapping: Map<Int, Int> = emptyMap(),
    val fastForwardSpeed: Float = 2.0f,
)

@Serializable
enum class ScaleMode {
    STRETCH,
    ASPECT,
    INTEGER,
}

@Serializable
enum class Filter {
    NONE,
    SMOOTH,
    CRT,
}
