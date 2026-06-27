package com.omilator.data.library

/**
 * Cleans raw ROM filenames into human-readable game titles.
 *
 * ROM naming conventions (No-Intro, GoodROM, scene) include metadata
 * in parentheses and brackets after the title:
 *
 *   "MotorStorm - Arctic Edge (USA) (En,Fr,De,Es,It,Nl,Pt,Ru)"
 *   "Pokemon - Yellow Version - Special Pikachu Edition (USA, Europe) (CGB+SGB Enhanced)"
 *   "Super Mario World (USA) [!]"
 *
 * This function strips all parenthetical and bracketed metadata,
 * returning just the game title.
 */
fun cleanRomTitle(rawName: String): String {
    // Everything before the first ( or [ is the title
    var title = rawName
        .substringBefore(" (")
        .substringBefore(" [")
        .trim()

    // Clean up: replace multiple spaces, trim dangling dashes
    title = title
        .replace(Regex("\\s+"), " ")
        .trim(' ', '-', '_')

    // If the result is empty (filename was all metadata), fall back to original
    return title.ifBlank { rawName.trim() }
}

/**
 * Detects the likely region from a ROM filename's tags.
 * Returns null if no region is detectable.
 */
fun detectRegion(rawName: String): String? {
    val lower = rawName.lowercase()
    return when {
        "(usa)" in lower || "(u)" in lower || "(us)" in lower -> "USA"
        "(europe)" in lower || "(e)" in lower || "(eur)" in lower -> "Europe"
        "(japan)" in lower || "(j)" in lower || "(jpn)" in lower -> "Japan"
        "(world)" in lower -> "World"
        "(asia)" in lower -> "Asia"
        "(korea)" in lower || "(k)" in lower -> "Korea"
        else -> null
    }
}
