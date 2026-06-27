package com.omilator.core.input

import com.omilator.core.libretro.api.JoypadButton
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWGamepadState
import org.lwjgl.system.Configuration

class GamepadPoller {

    private var initialized = false
    private var available = false
    private val state = GLFWGamepadState.malloc()

    fun init(): Boolean {
        if (initialized) return available
        initialized = true
        try {
            Configuration.GLFW_CHECK_THREAD0.set(false)
            val ok = GLFW.glfwInit()
            if (ok) {
                for (jid in 0 until GLFW.GLFW_JOYSTICK_LAST) {
                    if (GLFW.glfwJoystickIsGamepad(jid)) {
                        println("[Gamepad] Controller detected at joystick $jid")
                    }
                }
                available = true
            }
        } catch (e: Throwable) {
            println("[Gamepad] Init error: ${e.message}")
        }
        return available
    }

    fun poll(
        setButton: (button: Int, pressed: Boolean) -> Unit,
        setAnalog: (index: Int, value: Int) -> Unit,
    ) {
        if (!available) return

        for (jid in 0 until GLFW.GLFW_JOYSTICK_LAST) {
            if (!GLFW.glfwJoystickPresent(jid)) continue
            if (!GLFW.glfwJoystickIsGamepad(jid)) continue
            if (!GLFW.glfwGetGamepadState(jid, state)) continue

            setButton(JoypadButton.A, btn(GLFW.GLFW_GAMEPAD_BUTTON_A))
            setButton(JoypadButton.B, btn(GLFW.GLFW_GAMEPAD_BUTTON_B))
            setButton(JoypadButton.X, btn(GLFW.GLFW_GAMEPAD_BUTTON_X))
            setButton(JoypadButton.Y, btn(GLFW.GLFW_GAMEPAD_BUTTON_Y))
            setButton(JoypadButton.DPAD_UP, btn(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP))
            setButton(JoypadButton.DPAD_DOWN, btn(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN))
            setButton(JoypadButton.DPAD_LEFT, btn(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT))
            setButton(JoypadButton.DPAD_RIGHT, btn(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT))
            setButton(JoypadButton.START, btn(GLFW.GLFW_GAMEPAD_BUTTON_START))
            setButton(JoypadButton.SELECT, btn(GLFW.GLFW_GAMEPAD_BUTTON_BACK))
            setButton(JoypadButton.L, btn(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER))
            setButton(JoypadButton.R, btn(GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER))
            setButton(JoypadButton.L2, axis(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER) > 0.5f)
            setButton(JoypadButton.R2, axis(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER) > 0.5f)

            setAnalog(0, (axis(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X) * 32767f).toInt())
            setAnalog(1, (-axis(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y) * 32767f).toInt())
            setAnalog(2, (axis(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X) * 32767f).toInt())
            setAnalog(3, (-axis(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y) * 32767f).toInt())

            return
        }
    }

    private fun btn(code: Int): Boolean = state.buttons(code) != 0.toByte()
    private fun axis(code: Int): Float = state.axes(code)

    fun destroy() {
        if (initialized && available) {
            try { GLFW.glfwTerminate() } catch (_: Throwable) {}
        }
        state.free()
    }
}
