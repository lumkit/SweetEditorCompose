package io.github.lumkit.sweeteditor.input

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.ModifierNodeElement
import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import io.github.lumkit.sweeteditor.core.protocol.KeyModifier
import io.github.lumkit.sweeteditor.session.RememberedEditorSession

internal actual fun Modifier.editorHostScale(session: RememberedEditorSession): Modifier = this

internal actual fun Modifier.editorIme(session: RememberedEditorSession, readOnly: Boolean): Modifier =
    this.then(WebImeElement(session, readOnly)).pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press) {
                    WebImeFocusRequester.requestFocus()
                }
            }
        }
    }

private object WebImeFocusRequester {
    var request: (() -> Unit)? = null
    fun requestFocus() {
        request?.invoke()
    }
}

private data class WebImeElement(
    val session: RememberedEditorSession,
    val readOnly: Boolean,
) : ModifierNodeElement<WebImeNode>() {
    override fun create(): WebImeNode = WebImeNode(session, readOnly)
    override fun update(node: WebImeNode) {
        node.bindSession(session)
        node.setReadOnly(readOnly)
    }
}

private class WebImeNode(
    session: RememberedEditorSession,
    private var readOnly: Boolean,
) : Modifier.Node(), FocusEventModifierNode {
    var session: RememberedEditorSession = session
        private set
    private var token: Int = 0
    private val focusTap: () -> Unit = { if (token != 0) webImeBridgeFocus(token) }

    fun setReadOnly(value: Boolean) {
        if (readOnly == value) return
        readOnly = value
        if (token != 0) webImeBridgeSetReadOnly(token, value)
    }

    fun bindSession(next: RememberedEditorSession) {
        if (session === next) return
        session = next
    }

    override fun onAttach() {
        token = webImeBridgeInstall(
            onKey = { key, ctrl, shift, alt, meta, _ ->
                val mapped = mapDomKey(key, ctrl, shift, alt, meta) ?: return@webImeBridgeInstall false
                if (mapped.pointerModifiersOnly) {
                    session.updatePointerModifiers(mapped.modifiers)
                    return@webImeBridgeInstall false
                }
                session.handleKey(mapped.keyCode, mapped.text, mapped.modifiers)
                true
            },
            onCompositionEnd = { data ->
                if (data.isNotEmpty()) session.insertText(data)
            },
            readOnly = readOnly,
        )
        session.imeTapHandler = focusTap
        WebImeFocusRequester.request = focusTap
    }

    override fun onDetach() {
        if (session.imeTapHandler === focusTap) {
            session.imeTapHandler = null
        }
        if (WebImeFocusRequester.request === focusTap) {
            WebImeFocusRequester.request = null
        }
        if (token != 0) {
            webImeBridgeDestroy(token)
            token = 0
        }
        super.onDetach()
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (token == 0) return
        if (focusState.isFocused && !readOnly) {
            focusTap()
        }
    }
}

private val DOM_MODIFIER_KEYS = setOf(
    "Shift", "Control", "Alt", "Meta",
    "CapsLock", "NumLock", "ScrollLock", "Fn",
)

private fun commandKeyCode(key: String): Int = when (key) {
    "Backspace" -> KeyCode.BACKSPACE
    "Delete" -> KeyCode.DELETE_KEY
    "Enter" -> KeyCode.ENTER
    "Tab" -> KeyCode.TAB
    "Escape" -> KeyCode.ESCAPE
    "ArrowLeft" -> KeyCode.LEFT
    "ArrowRight" -> KeyCode.RIGHT
    "ArrowUp" -> KeyCode.UP
    "ArrowDown" -> KeyCode.DOWN
    "Home" -> KeyCode.HOME
    "End" -> KeyCode.END
    "PageUp" -> KeyCode.PAGE_UP
    "PageDown" -> KeyCode.PAGE_DOWN
    else -> KeyCode.NONE
}

private fun shortcutKeyCode(key: String): Int = when (key.lowercase()) {
    "a" -> KeyCode.A
    "c" -> KeyCode.C
    "d" -> KeyCode.D
    "k" -> KeyCode.K
    "v" -> KeyCode.V
    "x" -> KeyCode.X
    "y" -> KeyCode.Y
    "z" -> KeyCode.Z
    " " -> KeyCode.SPACE
    else -> KeyCode.NONE
}

internal fun mapDomKey(
    key: String,
    ctrl: Boolean,
    shift: Boolean,
    alt: Boolean,
    meta: Boolean,
): MappedKey? {
    var modifiers = 0
    if (shift) modifiers = modifiers or KeyModifier.SHIFT
    if (ctrl) modifiers = modifiers or KeyModifier.CTRL
    if (alt) modifiers = modifiers or KeyModifier.ALT
    if (meta) modifiers = modifiers or KeyModifier.META
    if (key.length > 1 && key in DOM_MODIFIER_KEYS) {
        return MappedKey(KeyCode.NONE, null, modifiers, pointerModifiersOnly = true)
    }
    val command = commandKeyCode(key)
    if (command != KeyCode.NONE) {
        return MappedKey(command, null, modifiers)
    }
    val shortcut = (modifiers and (KeyModifier.CTRL or KeyModifier.ALT or KeyModifier.META)) != 0
    if (shortcut) {
        val sc = shortcutKeyCode(key)
        if (sc != KeyCode.NONE) {
            return MappedKey(sc, null, modifiers)
        }
        return null
    }
    if (key.length == 1) {
        val cp = key[0].code
        if (isTypedCharacter(cp)) {
            return MappedKey(KeyCode.NONE, key.encodeToByteArray(), 0)
        }
        return null
    }
    return null
}

internal expect fun webImeBridgeInstall(
    onKey: (key: String, ctrl: Boolean, shift: Boolean, alt: Boolean, meta: Boolean, isComposing: Boolean) -> Boolean,
    onCompositionEnd: (data: String) -> Unit,
    readOnly: Boolean,
): Int

internal expect fun webImeBridgeFocus(token: Int)

internal expect fun webImeBridgeSetReadOnly(token: Int, readOnly: Boolean)

internal expect fun webImeBridgeDestroy(token: Int)
