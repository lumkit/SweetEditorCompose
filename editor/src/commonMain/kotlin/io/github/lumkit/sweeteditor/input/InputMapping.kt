package io.github.lumkit.sweeteditor.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import io.github.lumkit.sweeteditor.core.protocol.CoreProtocol
import io.github.lumkit.sweeteditor.core.protocol.EventType
import io.github.lumkit.sweeteditor.core.protocol.GestureEvent
import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import io.github.lumkit.sweeteditor.core.protocol.KeyModifier
import io.github.lumkit.sweeteditor.core.protocol.PointF
import kotlin.math.abs

internal fun pointerModifiers(event: PointerEvent): Int {
    var modifiers = 0
    val keyboard = event.keyboardModifiers
    if (keyboard.isShiftPressed) modifiers = modifiers or KeyModifier.SHIFT
    if (keyboard.isCtrlPressed) modifiers = modifiers or KeyModifier.CTRL
    if (keyboard.isAltPressed) modifiers = modifiers or KeyModifier.ALT
    if (keyboard.isMetaPressed) modifiers = modifiers or KeyModifier.META
    return modifiers
}

internal fun encodeGesture(
    type: EventType,
    points: List<PointF>,
    modifiers: Int = 0,
    wheelDeltaX: Float = 0f,
    wheelDeltaY: Float = 0f,
    directScale: Float = 1f,
): ByteArray = CoreProtocol.encodeGestureEvent(
    GestureEvent(
        type = type,
        points = points,
        modifiers = modifiers,
        wheelDeltaX = wheelDeltaX,
        wheelDeltaY = wheelDeltaY,
        directScale = directScale,
    ),
)

internal fun mapPointerEventType(event: PointerEvent, changeType: PointerType): EventType? {
    val isMouse = changeType == PointerType.Mouse || changeType == PointerType.Stylus
    return when (event.type) {
        PointerEventType.Press -> if (isMouse) EventType.MOUSE_DOWN else EventType.TOUCH_DOWN
        PointerEventType.Move -> if (isMouse) EventType.MOUSE_MOVE else EventType.TOUCH_MOVE
        PointerEventType.Release -> if (isMouse) EventType.MOUSE_UP else EventType.TOUCH_UP
        PointerEventType.Exit -> EventType.MOUSE_MOVE
        else -> null
    }
}

internal data class MappedKey(
    val keyCode: Int,
    val text: ByteArray?,
    val modifiers: Int,
)

internal fun mapKeyEvent(event: KeyEvent): MappedKey? {
    if (event.type != KeyEventType.KeyDown) return null
    var modifiers = 0
    if (event.isShiftPressed) modifiers = modifiers or KeyModifier.SHIFT
    if (event.isCtrlPressed) modifiers = modifiers or KeyModifier.CTRL
    if (event.isAltPressed) modifiers = modifiers or KeyModifier.ALT
    if (event.isMetaPressed) modifiers = modifiers or KeyModifier.META
    val keyCode = mapKeyCode(event.key)
    val text = characterBytes(event)
    if (keyCode == KeyCode.NONE && text == null) return null
    return MappedKey(keyCode, text, modifiers)
}

private fun mapKeyCode(key: Key): Int = when (key) {
    Key.Backspace -> KeyCode.BACKSPACE
    Key.Delete -> KeyCode.DELETE_KEY
    Key.Enter, Key.NumPadEnter -> KeyCode.ENTER
    Key.Tab -> KeyCode.TAB
    Key.Escape -> KeyCode.ESCAPE
    Key.DirectionLeft -> KeyCode.LEFT
    Key.DirectionRight -> KeyCode.RIGHT
    Key.DirectionUp -> KeyCode.UP
    Key.DirectionDown -> KeyCode.DOWN
    Key.MoveHome -> KeyCode.HOME
    Key.MoveEnd -> KeyCode.END
    Key.PageUp -> KeyCode.PAGE_UP
    Key.PageDown -> KeyCode.PAGE_DOWN
    Key.A -> KeyCode.A
    Key.C -> KeyCode.C
    Key.D -> KeyCode.D
    Key.K -> KeyCode.K
    Key.V -> KeyCode.V
    Key.X -> KeyCode.X
    Key.Y -> KeyCode.Y
    Key.Z -> KeyCode.Z
    Key.Spacebar -> KeyCode.SPACE
    else -> KeyCode.NONE
}

private fun characterBytes(event: KeyEvent): ByteArray? {
    val codePoint = event.utf16CodePoint
    return if (codePoint in 32..0xFFFF) {
        codePoint.toChar().toString().encodeToByteArray()
    } else {
        null
    }
}

/**
 * Map Compose/Skiko wheel deltas to Core `MOUSE_WHEEL`.
 *
 * Core documents positive `wheel_delta_y` as up. Compose Desktop `scrollDelta.y`
 * is positive when the wheel moves toward the user (same as `Scrollable`).
 * Swing uses `-wheelRotation * 40`; Skiko often reports tick units near ±1,
 * so small magnitudes are scaled to that notch size.
 */
internal fun coreWheelDelta(scrollDelta: Offset): Pair<Float, Float> {
    val scaledX = scaleWheelAxis(scrollDelta.x)
    val scaledY = scaleWheelAxis(scrollDelta.y)
    return -scaledX to -scaledY
}

private fun scaleWheelAxis(delta: Float): Float {
    if (delta == 0f) return 0f
    val magnitude = abs(delta)
    val pixels = if (magnitude < 8f) delta * 40f else delta
    return pixels
}
