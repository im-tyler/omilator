package com.omilator.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import com.omilator.core.libretro.api.JoypadButton
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * On-screen touch gamepad. Draws semi-transparent D-pad + buttons.
 * Multiple simultaneous touches tracked via pointer ID → button map.
 *
 * Works on iOS (touch) and desktop (mouse as single-touch proxy).
 */
@Composable
fun TouchControls(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeButtons = remember { mutableStateMapOf<Int, Int>() } // pointerId → button

    Box(modifier = modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures(
            onPress = { offset ->
                val btn = hitTest(offset.x, offset.y, size.width.toFloat(), size.height.toFloat())
                if (btn != null) {
                    onPress(btn)
                }
                tryAwaitRelease()
                if (btn != null) {
                    onRelease(btn)
                }
            }
        )
    }) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGamepad(size.width, size.height)
        }
    }
}

private fun hitTest(x: Float, y: Float, w: Float, h: Float): Int? {
    // D-pad: bottom-left circle
    val dpadCx = w * 0.18f
    val dpadCy = h * 0.78f
    val dpadR = w * 0.10f
    val dx = x - dpadCx
    val dy = y - dpadCy
    if (hypot(dx, dy) < dpadR * 1.5f) {
        val angle = atan2(dy.toDouble(), dx.toDouble())
        return when {
            angle > -PI / 4 && angle <= PI / 4 -> JoypadButton.DPAD_RIGHT
            angle > PI / 4 && angle <= 3 * PI / 4 -> JoypadButton.DPAD_DOWN
            angle > 3 * PI / 4 || angle <= -3 * PI / 4 -> JoypadButton.DPAD_LEFT
            else -> JoypadButton.DPAD_UP
        }.toInt()
    }

    // A button: bottom-right
    if (hypot(x - w * 0.82f, y - h * 0.75f) < w * 0.06f) return JoypadButton.A
    // B button
    if (hypot(x - w * 0.72f, y - h * 0.82f) < w * 0.06f) return JoypadButton.B
    // Start: center-right
    if (hypot(x - w * 0.55f, y - h * 0.90f) < w * 0.05f) return JoypadButton.START
    // Select: center-left
    if (hypot(x - w * 0.45f, y - h * 0.90f) < w * 0.05f) return JoypadButton.SELECT

    return null
}

private fun DrawScope.drawGamepad(w: Float, h: Float) {
    val alpha = 0.25f
    val strokeAlpha = 0.4f
    val strokeW = 2f

    // D-pad cross
    val dcx = w * 0.18f
    val dcy = h * 0.78f
    val dr = w * 0.06f

    // Up
    drawLine(Color.White.copy(alpha = strokeAlpha), Offset(dcx, dcy - dr * 2), Offset(dcx, dcy - dr), strokeW)
    // Down
    drawLine(Color.White.copy(alpha = strokeAlpha), Offset(dcx, dcy + dr), Offset(dcx, dcy + dr * 2), strokeW)
    // Left
    drawLine(Color.White.copy(alpha = strokeAlpha), Offset(dcx - dr * 2, dcy), Offset(dcx - dr, dcy), strokeW)
    // Right
    drawLine(Color.White.copy(alpha = strokeAlpha), Offset(dcx + dr, dcy), Offset(dcx + dr * 2, dcy), strokeW)

    // D-pad center circle
    drawCircle(Color.White.copy(alpha = alpha), dr * 0.5f, Offset(dcx, dcy))

    // A button
    drawCircle(Color.White.copy(alpha = alpha), w * 0.05f, Offset(w * 0.82f, h * 0.75f))
    drawCircle(Color.White.copy(alpha = strokeAlpha), w * 0.05f, Offset(w * 0.82f, h * 0.75f), style = androidx.compose.ui.graphics.drawscope.Stroke(strokeW))

    // B button
    drawCircle(Color.White.copy(alpha = alpha), w * 0.05f, Offset(w * 0.72f, h * 0.82f))
    drawCircle(Color.White.copy(alpha = strokeAlpha), w * 0.05f, Offset(w * 0.72f, h * 0.82f), style = androidx.compose.ui.graphics.drawscope.Stroke(strokeW))

    // Start
    drawCircle(Color.White.copy(alpha = alpha), w * 0.04f, Offset(w * 0.55f, h * 0.90f))

    // Select
    drawCircle(Color.White.copy(alpha = alpha), w * 0.04f, Offset(w * 0.45f, h * 0.90f))
}
