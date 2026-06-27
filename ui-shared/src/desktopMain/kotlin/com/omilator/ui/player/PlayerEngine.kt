package com.omilator.ui.player

import com.omilator.core.audio.AudioOutput
import com.omilator.core.input.GamepadPoller
import com.omilator.core.input.InputState
import com.omilator.core.libretro.api.CoreController
import com.omilator.core.libretro.api.Framebuffer
import com.omilator.core.libretro.api.Geometry
import com.omilator.core.libretro.api.InputDevice
import com.omilator.core.libretro.api.InputSource
import com.omilator.core.libretro.api.JoypadButton
import com.omilator.core.libretro.api.PixelFormat
import com.omilator.core.libretro.api.VideoSink
import com.omilator.core.libretro.createCoreController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicReference

class PlayerEngine(
    private val corePath: String,
    private val romPath: String,
    private val audioOutput: AudioOutput,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val controller: CoreController = createCoreController(systemDirectory = defaultSystemDir())
    private val converter = FrameConverter()
    private val inputState = InputStateHolder()
    private val gamepadPoller = GamepadPoller()
    private val latestFrame = AtomicReference<Framebuffer?>(null)

    private var frameLoop: Job? = null
    private var _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Speed multiplier for fast-forward (Tab) / slow-motion (Shift+Tab). */
    private var speedMultiplier: Float = 1.0f

    suspend fun start() {
        _state.value = _state.value.copy(isLoading = true)
        try {
            controller.loadCore(corePath)
            val avInfo = controller.loadGame(romPath)
            controller.attach(
                video = VideoSink { fb -> latestFrame.set(fb) },
                audio = AudioSinkAdapter(audioOutput),
                input = InputSourceAdapter(inputState),
            )
            audioOutput.configure(avInfo.timing.sampleRate, channels = 2)
            gamepadPoller.init()
            loadPersistedOptions()  // Apply saved core options for this ROM
            _state.value = _state.value.copy(isLoading = false, geometry = avInfo.geometry, fps = avInfo.timing.fps)
            frameLoop = scope.launch { runLoop(avInfo.timing.fps) }
        } catch (t: Throwable) {
            _state.value = _state.value.copy(isLoading = false, error = "${t::class.simpleName}: ${t.message}")
        }
    }

    private fun loadPersistedOptions() {
        val file = optionsFile()
        if (!file.exists()) return
        try {
            file.readLines().forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) controller.setOptionValue(parts[0], parts[1])
            }
        } catch (_: Throwable) {}
    }

    private fun persistOptions() {
        try {
            val selections = controller.getOptionSelections()
            if (selections.isEmpty()) return
            val text = selections.entries.joinToString("\n") { "${it.key}=${it.value}" }
            optionsFile().writeText(text)
        } catch (_: Throwable) {}
    }

    private fun optionsFile(): java.io.File {
        val dir = java.io.File(System.getProperty("user.home"),
            "Library/Application Support/Omilator/options").apply { mkdirs() }
        return java.io.File(dir, "${java.io.File(romPath).nameWithoutExtension}.json")
    }

    fun stop() {
        frameLoop?.cancel()
        frameLoop = null
        scope.launch {
            runCatching { controller.unloadGame() }
            runCatching { controller.unloadCore() }
        }
        scope.cancel()
    }

    fun pressButton(button: Int) {
        inputState.press(button)
    }

    fun releaseButton(button: Int) {
        inputState.release(button)
    }

    fun saveState(path: String): Boolean {
        val ok = controller.saveState(path)
        // Also save a thumbnail of the current frame
        if (ok) {
            val frame = latestFrame.get()
            if (frame != null) {
                try {
                    val img = converter.convert(frame)
                    val thumbPath = path.replace(".state", ".png")
                    javax.imageio.ImageIO.write(img, "PNG", java.io.File(thumbPath))
                } catch (_: Throwable) {}
            }
        }
        return ok
    }

    fun loadState(path: String): Boolean = controller.loadState(path)

    fun renderFrameIfAvailable(): BufferedImage? {
        val fb = latestFrame.getAndSet(null) ?: return null
        return converter.convert(fb)
    }

    private suspend fun runLoop(targetFps: Float) {
        val baseIntervalNanos = (1_000_000_000.0 / targetFps).toLong()
        var nextDeadline = System.nanoTime()
        while (scope.isActive) {
            // Poll gamepad before each frame
            gamepadPoller.poll(
                setButton = { btn, pressed ->
                    if (pressed) inputState.press(btn) else inputState.release(btn)
                },
                setAnalog = { _, _ -> },
            )

            controller.runFrame()
            tickRewind()

            // Run-ahead: speculative extra frame for reduced input latency.
            // Display shows 1 frame ahead; state rolls back to real frame.
            if (runAhead > 0) {
                try {
                    val savedState = controller.saveStateToMemory()
                    controller.runFrame()
                    controller.loadStateFromMemory(savedState)
                } catch (_: Throwable) {}
            }

            // Adjust deadline by speed multiplier (fast forward / slow motion)
            val interval = (baseIntervalNanos / speedMultiplier).toLong()
            nextDeadline += interval
            val now = System.nanoTime()
            val wait = nextDeadline - now
            if (wait > 0) delay(wait / 1_000_000)
            else nextDeadline = now
        }
    }

    fun setSpeedMultiplier(multiplier: Float) {
        speedMultiplier = multiplier.coerceIn(0.1f, 10f)
    }

    fun getSpeedMultiplier(): Float = speedMultiplier

    // ---- Rewind system ----
    private val rewindBuffer = java.util.ArrayDeque<ByteArray>()
    private var rewindFrameCounter = 0

    fun tickRewind() {
        rewindFrameCounter++
        if (rewindFrameCounter >= 10) {
            rewindFrameCounter = 0
            try {
                val state = controller.saveStateToMemory()
                rewindBuffer.addLast(state)
                while (rewindBuffer.size > 300) rewindBuffer.removeFirst()
            } catch (_: Throwable) {}
        }
    }

    fun rewindStep(): Boolean {
        val state = rewindBuffer.pollLast() ?: return false
        return controller.loadStateFromMemory(state)
    }

    // ---- Cheats ----
    fun applyCheat(code: String) {
        controller.cheatReset()
        controller.cheatSet(0, true, code.trim())
    }

    // ---- Core options ----
    fun getCoreOptions() = controller.getCoreOptions()
    fun setOptionValue(key: String, value: String) {
        controller.setOptionValue(key, value)
        persistOptions()
    }

    // ---- Run-ahead (latency reduction) ----
    private var runAhead = 0
    fun toggleRunAhead() { runAhead = if (runAhead > 0) 0 else 1 }
    fun isRunAhead(): Boolean = runAhead > 0

    // ---- Audio volume ----
    private var volume: Float = 1.0f
    fun setVolume(v: Float) { volume = v.coerceIn(0f, 2f) }
    fun getVolume(): Float = volume
}

data class PlayerState(
    val isLoading: Boolean = true,
    val geometry: Geometry? = null,
    val fps: Float = 60f,
    val error: String? = null,
)

private fun defaultSystemDir(): String {
    val home = System.getProperty("user.home")
    val dir = java.io.File(home, "Library/Application Support/Omilator")
    if (!dir.exists()) dir.mkdirs()
    return dir.absolutePath
}

private class InputStateHolder {
    private val buttons = IntArray(16)

    fun press(button: Int) { if (button in buttons.indices) buttons[button] = 1 }
    fun release(button: Int) { if (button in buttons.indices) buttons[button] = 0 }
    fun get(button: Int): Int = buttons.getOrElse(button) { 0 }
}

private class InputSourceAdapter(private val holder: InputStateHolder) : InputSource {
    override fun poll(port: Int, device: InputDevice, index: Int, id: Int): Int {
        if (port != 0 || device != InputDevice.JOYPAD) return 0
        return holder.get(id)
    }
}

private class AudioSinkAdapter(private val output: AudioOutput) : com.omilator.core.libretro.api.AudioSink {
    override fun onSamples(samples: ShortArray) {
        output.write(samples)
    }
}

