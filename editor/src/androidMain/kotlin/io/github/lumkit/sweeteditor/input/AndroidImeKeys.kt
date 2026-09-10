package io.github.lumkit.sweeteditor.input

import android.view.KeyEvent
import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import io.github.lumkit.sweeteditor.core.protocol.KeyModifier

internal data class MappedAndroidKey(
    val keyCode: Int,
    val text: ByteArray?,
    val modifiers: Int,
)

internal fun mapAndroidImeKeyEvent(event: KeyEvent): MappedAndroidKey? {
    if (event.action != KeyEvent.ACTION_DOWN) return null
    var nativeKeyCode = mapAndroidKeyCode(event.keyCode)
    val modifiers = androidKeyModifiers(event)
    if (nativeKeyCode == KeyCode.NONE && (event.isCtrlPressed || event.isMetaPressed || event.isAltPressed)) {
        val unicode = event.getUnicodeChar(0)
        if (unicode in 'a'.code..'z'.code) {
            nativeKeyCode = unicode - 32
        }
    }
    if (nativeKeyCode != KeyCode.NONE) {
        return MappedAndroidKey(nativeKeyCode, null, modifiers)
    }
    if (!event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed) {
        val unicode = event.unicodeChar
        if (unicode > 0 && !Character.isISOControl(unicode)) {
            return MappedAndroidKey(
                KeyCode.NONE,
                String(Character.toChars(unicode)).encodeToByteArray(),
                KeyModifier.NONE,
            )
        }
    }
    return null
}

private fun androidKeyModifiers(event: KeyEvent): Int {
    var modifiers = KeyModifier.NONE
    if (event.isShiftPressed) modifiers = modifiers or KeyModifier.SHIFT
    if (event.isCtrlPressed) modifiers = modifiers or KeyModifier.CTRL
    if (event.isAltPressed) modifiers = modifiers or KeyModifier.ALT
    if (event.isMetaPressed) modifiers = modifiers or KeyModifier.META
    return modifiers
}

private fun mapAndroidKeyCode(androidKeyCode: Int): Int = when (androidKeyCode) {
    KeyEvent.KEYCODE_DEL -> KeyCode.BACKSPACE
    KeyEvent.KEYCODE_TAB -> KeyCode.TAB
    KeyEvent.KEYCODE_ENTER -> KeyCode.ENTER
    KeyEvent.KEYCODE_ESCAPE -> KeyCode.ESCAPE
    KeyEvent.KEYCODE_FORWARD_DEL -> KeyCode.DELETE_KEY
    KeyEvent.KEYCODE_DPAD_LEFT -> KeyCode.LEFT
    KeyEvent.KEYCODE_DPAD_UP -> KeyCode.UP
    KeyEvent.KEYCODE_DPAD_RIGHT -> KeyCode.RIGHT
    KeyEvent.KEYCODE_DPAD_DOWN -> KeyCode.DOWN
    KeyEvent.KEYCODE_MOVE_HOME -> KeyCode.HOME
    KeyEvent.KEYCODE_MOVE_END -> KeyCode.END
    KeyEvent.KEYCODE_PAGE_UP -> KeyCode.PAGE_UP
    KeyEvent.KEYCODE_PAGE_DOWN -> KeyCode.PAGE_DOWN
    KeyEvent.KEYCODE_SPACE -> KeyCode.SPACE
    else -> KeyCode.NONE
}
