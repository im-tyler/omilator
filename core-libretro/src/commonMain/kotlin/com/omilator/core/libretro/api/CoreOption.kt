package com.omilator.core.libretro.api

import kotlinx.serialization.Serializable

@Serializable
data class CoreOptionValue(
    val value: String,
    val label: String,
)

@Serializable
data class CoreOption(
    val key: String,
    val description: String,
    val info: String? = null,
    val default: String,
    val values: List<CoreOptionValue>,
)

/** User-selected values for a core's options, keyed by option key. */
typealias CoreOptionSelections = Map<String, String>
