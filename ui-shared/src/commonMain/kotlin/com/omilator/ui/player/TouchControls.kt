package com.omilator.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import com.omilator.core.libretro.api.JoypadButton
import kotlin.math.hypot

data class TouchButton(
    val id: Int,
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val label: String,
)

@Composable
fun TouchControls(
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Track which pointers are on which buttons
    val activeButtons = remember { mutableStateMapOf<Int, Int>() } // pointerId → buttonId

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        for (change in event.changes) {
                            val pid = change.id.value.toInt()
                            val x = change.position.x
                            val y = change.position.y
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val buttons = layoutButtons(w, h)

                            if (change.changedToDown()) {
                                val btn = hitTest(x, y, buttons)
                                if (btn != null) {
                                    activeButtons[pid] = btn
                                    onPress(btn)
                                }
                            } else if (change.changedToUp()) {
                                val btn = activeButtons.remove(pid)
                                if (btn != null) {
                                    onRelease(btn)
                                }
                            }
                        }
                    }
                }
            }
    ) {
        drawGamepad(size.width, size.height)
    }
}

private fun layoutButtons(w: Float, h: Float): List<TouchButton> {
    val s = w * 0.07f // standard radius
    return listOf(
        // D-pad directions (cross shape)
        TouchButton(JoypadButton.DPAD_UP, w * 0.15f, h * 0.70f, s, "U"),
        TouchButton(JoypadButton.DPAD_DOWN, w * 0.15f, h * 0.86f, s, "D"),
        TouchButton(JoypadButton.DPAD_LEFT, w * 0.07f, h * 0.78f, s, "L"),
        TouchButton(JoypadButton.DPAD_RIGHT, w * 0.23f, h * 0.78f, s, "R"),
        // Face buttons
        TouchButton(JoypadButton.A, w * 0.83f, h * 0.75f, s, "A"),
        TouchButton(JoypadButton.B, w * 0.72f, h * 0.83f, s, "B"),
        // Center
        TouchButton(JoypadButton.START, w * 0.58f, h * 0.92f, s * 0.7f, ">"),
        TouchButton(JoypadButton.SELECT, w * 0.42f, h * 0.92f, s * 0.7f, "<"),
    )
}

private fun hitTest(x: Float, y: Float, buttons: List<TouchButton>): Int? {
    for (btn in buttons) {
        if (hypot(x - btn.cx, y - btn.cy) < btn.radius) {
            return btn.id
        }
    }
    return null
}

private fun PointerInputChange.changedToDown(): Boolean =
    !this.previousPressed && this.pressed

private fun PointerInputChange.changedToUp(): Boolean =
    this.previousPressed && !this.pressed

private fun DrawScope.drawGamepad(w: Float, h: Float) {
    val fillAlpha = 0.20f
    val borderAlpha = 0.45f
    val strokeWidth = 2f
    val s = w * 0.07f

    // D-pad cross — 4 rounded rectangles forming a plus shape
    val dcx = w * 0.15f
    val dcy = h * 0.78f
    val padW = s * 1.6f
    val padH = s * 1.0f

    // Up
    drawRoundRect(
        color = Color.White.copy(alpha = fillAlpha),
        topLeft = Offset(dcx - padH / 2, dcy - padW - s * 0.2f),
        size = Size(padH, padW),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = borderAlpha),
        topLeft = Offset(dcx - padH / 2, dcy - padW - s * 0.2f),
        size = Size(padH, padW),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
        style = Stroke(strokeWidth),
    )

    // Down
    drawRoundRect(
        color = Color.White.copy(alpha = fillAlpha),
        topLeft = Offset(dcx - padH / 2, dcy + s * 0.2f),
        size = Size(padH, padW),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = borderAlpha),
        topLeft = Offset(dcx - padH / 2, dcy + s * 0.2f),
        size = Size(padH, padW),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
        style = Stroke(strokeWidth),
    )

    // Left
    drawRoundRect(
        color = Color.White.copy(alpha = fillAlpha),
        topLeft = Offset(dcx - padW - s * 0.2f, dcy - padH / 2),
        size = Size(padW, padH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = borderAlpha),
        topLeft = Offset(dcx - padW - s * 0.2f, dcy - padH / 2),
        size = Size(padW, padH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
        style = Stroke(strokeWidth),
    )

    // Right
    drawRoundRect(
        color = Color.White.copy(alpha = fillAlpha),
        topLeft = Offset(dcx + s * 0.2f, dcy - padH / 2),
        size = Size(padW, padH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = borderAlpha),
        topLeft = Offset(dcx + s * 0.2f, dcy - padH / 2),
        size = Size(padW, padH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
        style = Stroke(strokeWidth),
    )

    // D-pad center
    drawRoundRect(
        color = Color.White.copy(alpha = fillAlpha * 1.5f),
        topLeft = Offset(dcx - padH / 2, dcy - padH / 2),
        size = Size(padH, padH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
    )

    // A button (large circle)
    drawCircle(Color.White.copy(alpha = fillAlpha), s, Offset(w * 0.83f, h * 0.75f))
    drawCircle(Color.White.copy(alpha = borderAlpha), s, Offset(w * 0.83f, h * 0.75f), style = Stroke(strokeWidth))

    // B button
    drawCircle(Color.White.copy(alpha = fillAlpha), s, Offset(w * 0.72f, h * 0.83f))
    drawCircle(Color.White.copy(alpha = borderAlpha), s, Offset(w * 0.72f, h * 0.83f), style = Stroke(strokeWidth))

    // Start
    drawCircle(Color.White.copy(alpha = fillAlpha), s * 0.7f, Offset(w * 0.58f, h * 0.92f))
    // Select
    drawCircle(Color.White.copy(alpha = fillAlpha), s * 0.7f, Offset(w * 0.42f, h * 0.92f))
}
