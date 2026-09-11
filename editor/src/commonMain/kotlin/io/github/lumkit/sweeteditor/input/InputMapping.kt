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

internal data class MappedPointerGesture(
    val type: EventType,
    val points: List<PointF>,
)

internal fun mapPointerGesture(
    eventType: PointerEventType,
    isMouse: Boolean,
    pressedPoints: List<PointF>,
    fallbackPoint: PointF,
    previousPressedCount: Int,
): MappedPointerGesture? {
    return when (eventType) {
        PointerEventType.Press -> when {
            isMouse -> MappedPointerGesture(EventType.MOUSE_DOWN, listOf(fallbackPoint))
            previousPressedCount == 0 -> MappedPointerGesture(EventType.TOUCH_DOWN, pressedPoints.ifEmpty { listOf(fallbackPoint) })
            else -> MappedPointerGesture(EventType.TOUCH_POINTER_DOWN, pressedPoints.ifEmpty { listOf(fallbackPoint) })
        }
        PointerEventType.Move -> when {
            isMouse -> MappedPointerGesture(EventType.MOUSE_MOVE, listOf(fallbackPoint))
            pressedPoints.isEmpty() -> null
            else -> MappedPointerGesture(EventType.TOUCH_MOVE, pressedPoints)
        }
        PointerEventType.Release -> when {
            isMouse -> MappedPointerGesture(EventType.MOUSE_UP, listOf(fallbackPoint))
            pressedPoints.isEmpty() -> MappedPointerGesture(EventType.TOUCH_UP, listOf(fallbackPoint))
            else -> MappedPointerGesture(EventType.TOUCH_POINTER_UP, pressedPoints)
        }
        PointerEventType.Exit -> if (isMouse && previousPressedCount == 0) {
            MappedPointerGesture(EventType.MOUSE_MOVE, listOf(PointF(-1f, -1f)))
        } else {
            null
        }
        else -> null
    }
}

internal fun magnificationToDirectScale(magnification: Double): Float? {
    val factor = (1.0 + magnification).toFloat()
    return factor.takeIf { it.isFinite() && it > 0f && it != 1f }
}

internal fun wheelModifiersForCore(modifiers: Int): Int {
    return if ((modifiers and (KeyModifier.CTRL or KeyModifier.META)) != 0) {
        modifiers or KeyModifier.CTRL
    } else {
        modifiers
    }
}

internal data class MappedKey(
    val keyCode: Int,
    val text: ByteArray?,
    val modifiers: Int,
    val pointerModifiersOnly: Boolean = false,
)

internal fun mapKeyEvent(event: KeyEvent): MappedKey? {
    val modifiers = keyModifiers(event)
    if (isModifierKey(event.key)) {
        if (event.type != KeyEventType.KeyDown && event.type != KeyEventType.KeyUp) return null
        return MappedKey(KeyCode.NONE, null, modifiers, pointerModifiersOnly = true)
    }
    if (event.type != KeyEventType.KeyDown) return null
    val command = mapCommandKeyCode(event.key)
    if (command != KeyCode.NONE) {
        return MappedKey(command, null, modifiers)
    }
    val shortcut = (modifiers and (KeyModifier.CTRL or KeyModifier.ALT or KeyModifier.META)) != 0
    if (shortcut) {
        val shortcutCode = mapShortcutKeyCode(event.key)
        if (shortcutCode != KeyCode.NONE) {
            return MappedKey(shortcutCode, null, modifiers)
        }
    }
    if (event.key == Key.Spacebar) {
        return MappedKey(KeyCode.NONE, " ".encodeToByteArray(), 0)
    }
    val text = typedCharacterBytes(event.utf16CodePoint) ?: return null
    return MappedKey(KeyCode.NONE, text, 0)
}

internal fun keyModifiers(event: KeyEvent): Int {
    var modifiers = 0
    if (event.isShiftPressed) modifiers = modifiers or KeyModifier.SHIFT
    if (event.isCtrlPressed) modifiers = modifiers or KeyModifier.CTRL
    if (event.isAltPressed) modifiers = modifiers or KeyModifier.ALT
    if (event.isMetaPressed) modifiers = modifiers or KeyModifier.META
    return modifiers
}

internal fun isModifierKey(key: Key): Boolean = when (key) {
    Key.ShiftLeft, Key.ShiftRight,
    Key.CtrlLeft, Key.CtrlRight,
    Key.AltLeft, Key.AltRight,
    Key.MetaLeft, Key.MetaRight,
    Key.CapsLock, Key.NumLock, Key.ScrollLock,
    Key.Function,
    -> true
    else -> false
}

private fun mapCommandKeyCode(key: Key): Int = when (key) {
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
    else -> KeyCode.NONE
}

private fun mapShortcutKeyCode(key: Key): Int = when (key) {
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

internal fun typedCharacterBytes(codePoint: Int): ByteArray? {
    if (!isTypedCharacter(codePoint)) return null
    return codePoint.toChar().toString().encodeToByteArray()
}

internal fun isTypedCharacter(codePoint: Int): Boolean {
    if (codePoint < 32 || codePoint == 127) return false
    if (codePoint in 0x80..0x9F) return false
    if (codePoint in 0xE000..0xF8FF) return false
    if (codePoint >= 0xFFE0) return false
    return codePoint <= 0x10FFFF
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
