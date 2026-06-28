@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.omilator.core.input

import kotlin.native.concurrent.ThreadLocal

/**
 * Shared input state for iOS — touch controls write to this,
 * staticCFunction inputStateCb reads from this.
 *
 * ThreadLocal ensures the frame loop thread sees the latest values
 * written from the UI thread.
 */
@ThreadLocal
object IosInputState {
    private val buttons = IntArray(16)

    fun setButton(button: Int, pressed: Boolean) {
        if (button in buttons.indices) {
            buttons[button] = if (pressed) 1 else 0
        }
    }

    fun isPressed(button: Int): Int {
        return if (button in buttons.indices) buttons[button] else 0
    }

    fun reset() {
        buttons.fill(0)
    }
}
