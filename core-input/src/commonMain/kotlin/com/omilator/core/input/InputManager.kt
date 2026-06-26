package com.omilator.core.input

import com.omilator.core.libretro.api.InputDevice
import com.omilator.core.libretro.api.InputSource
import com.omilator.core.libretro.api.JoypadButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InputState {
    private val buttons = IntArray(16)
    private val analogs = IntArray(4)

    fun setButton(button: Int, pressed: Boolean) {
        buttons[button] = if (pressed) 1 else 0
    }

    fun setAnalog(index: Int, value: Int) {
        analogs[index] = value
    }

    fun button(button: Int): Int = buttons.getOrElse(button) { 0 }
    fun analog(index: Int): Int = analogs.getOrElse(index) { 0 }

    fun reset() {
        buttons.fill(0)
        analogs.fill(0)
    }
}

class InputManager : InputSource {
    private val _state = MutableStateFlow(InputState())
    val state: StateFlow<InputState> = _state

    fun update(transform: InputState.() -> Unit) {
        _state.value.apply(transform)
    }

    fun press(button: Int) = update { setButton(button, true) }
    fun release(button: Int) = update { setButton(button, false) }

    override fun poll(port: Int, device: InputDevice, index: Int, id: Int): Int {
        if (port != 0) return 0
        return when (device) {
            InputDevice.JOYPAD -> _state.value.button(id)
            InputDevice.ANALOG -> _state.value.analog(id)
            else -> 0
        }
    }
}
