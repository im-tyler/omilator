package com.omilator.ui.player

import com.omilator.core.libretro.api.JoypadButton
import java.awt.event.KeyEvent

internal object KeyboardMapping {
    val map: Map<Int, Int> = mapOf(
        KeyEvent.VK_UP to JoypadButton.DPAD_UP,
        KeyEvent.VK_DOWN to JoypadButton.DPAD_DOWN,
        KeyEvent.VK_LEFT to JoypadButton.DPAD_LEFT,
        KeyEvent.VK_RIGHT to JoypadButton.DPAD_RIGHT,
        KeyEvent.VK_Z to JoypadButton.B,
        KeyEvent.VK_X to JoypadButton.A,
        KeyEvent.VK_A to JoypadButton.Y,
        KeyEvent.VK_S to JoypadButton.X,
        KeyEvent.VK_Q to JoypadButton.L,
        KeyEvent.VK_W to JoypadButton.R,
        KeyEvent.VK_ENTER to JoypadButton.START,
        KeyEvent.VK_SHIFT to JoypadButton.SELECT,
    )

    fun buttonFor(keyCode: Int): Int? = map[keyCode]
}
