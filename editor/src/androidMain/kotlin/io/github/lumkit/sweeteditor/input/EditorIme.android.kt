package io.github.lumkit.sweeteditor.input

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.establishTextInputSession
import io.github.lumkit.sweeteditor.session.RememberedEditorSession
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal actual fun Modifier.editorIme(session: RememberedEditorSession): Modifier =
    this.then(EditorImeElement(session))

private data class EditorImeElement(
    val session: RememberedEditorSession,
) : ModifierNodeElement<EditorImeNode>() {
    override fun create(): EditorImeNode = EditorImeNode(session)

    override fun update(node: EditorImeNode) {
        node.bindSession(session)
    }
}

private class EditorImeNode(
    session: RememberedEditorSession,
) : Modifier.Node(),
    PlatformTextInputModifierNode,
    FocusEventModifierNode,
    CompositionLocalConsumerModifierNode {
    var session: RememberedEditorSession = session
        private set
    private var focused = false
    private var inputJob: Job? = null
    private val onPressed: () -> Unit = { startInput() }

    fun bindSession(next: RememberedEditorSession) {
        if (session === next) return
        if (isAttached) {
            session.imePressHandler = null
        }
        session = next
        if (isAttached) {
            session.imePressHandler = onPressed
        }
    }

    override fun onAttach() {
        session.imePressHandler = onPressed
    }

    override fun onFocusEvent(focusState: FocusState) {
        val nowFocused = focusState.isFocused
        if (nowFocused == focused) return
        focused = nowFocused
        if (nowFocused) {
            startInput()
        } else {
            stopInput()
        }
    }

    override fun onDetach() {
        if (session.imePressHandler === onPressed) {
            session.imePressHandler = null
        }
        stopInput()
        super.onDetach()
    }

    private fun startInput() {
        if (!isAttached) return
        inputJob?.cancel()
        inputJob = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            establishTextInputSession {
                startInputMethod(
                    PlatformTextInputMethodRequest { outAttrs ->
                        val connection = ComposeEditorInputConnection(session, view)
                        connection.configureEditorInfo(outAttrs)
                        connection
                    },
                )
            }
        }
        currentValueOf(LocalSoftwareKeyboardController)?.show()
    }

    private fun stopInput() {
        inputJob?.cancel()
        inputJob = null
        currentValueOf(LocalSoftwareKeyboardController)?.hide()
    }
}
