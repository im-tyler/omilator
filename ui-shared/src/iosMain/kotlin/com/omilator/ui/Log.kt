package com.omilator.ui

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLog

/**
 * Routes Kotlin log lines through NSLog so they appear in `xcrun simctl
 * log show` output. Plain `println()` on iOS goes to stdout which is
 * inconsistently captured by the unified logging system.
 *
 * Builds the string in Kotlin to avoid NSLog format-string pitfalls when
 * the message itself contains '%'.
 */
@OptIn(ExperimentalForeignApi::class)
fun logI(tag: String, message: String) {
    // Escape any literal '%' so NSLog doesn't interpret it as a format spec.
    val safe = message.replace("%", "%%")
    NSLog("[$tag] $safe")
}

