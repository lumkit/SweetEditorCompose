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

internal actual fun Modifier.editorHostScale(session: RememberedEditorSession): Modifier = this

internal actual fun Modifier.editorIme(session: RememberedEditorSession, readOnly: Boolean): Modifier =
    this.then(EditorImeElement(session, readOnly))

private data class EditorImeElement(
    val session: RememberedEditorSession,
    val readOnly: Boolean,
) : ModifierNodeElement<EditorImeNode>() {
    override fun create(): EditorImeNode = EditorImeNode(session, readOnly)

    override fun update(node: EditorImeNode) {
        node.bindSession(session)
        node.setReadOnly(readOnly)
    }
}

private class EditorImeNode(
    session: RememberedEditorSession,
    private var readOnly: Boolean,
) : Modifier.Node(),
    PlatformTextInputModifierNode,
    FocusEventModifierNode,
    CompositionLocalConsumerModifierNode {
    var session: RememberedEditorSession = session
        private set
    private var inputJob: Job? = null
    private val onTap: () -> Unit = {
        startInput()
        currentValueOf(LocalSoftwareKeyboardController)?.show()
    }

    fun setReadOnly(value: Boolean) {
        if (readOnly == value) return
        readOnly = value
        if (value) {
            stopInput()
        }
    }

    fun bindSession(next: RememberedEditorSession) {
        if (session === next) return
        if (isAttached) {
            session.imeTapHandler = null
        }
        session = next
        if (isAttached) {
            session.imeTapHandler = onTap
        }
    }

    override fun onAttach() {
        session.imeTapHandler = onTap
    }

    // Unlike the Desktop/iOS Skiko request path, the Android IME session is bound to
    // the InputConnection returned by establishTextInputSession. Compose cancels the
    // session (and thus invokes ComposeEditorInputConnection.closeConnection ->
    // closeOwnedSession) automatically when the node loses focus, so there is no need
    // to start/stop input manually here. Input is started on tap (see onTap) and on
    // readOnly -> false is not auto-started, matching the shared lifecycle contract.
    override fun onFocusEvent(focusState: FocusState) = Unit

    override fun onDetach() {
        if (session.imeTapHandler === onTap) {
            session.imeTapHandler = null
        }
        stopInput()
        super.onDetach()
    }

    private fun startInput() {
        if (!isAttached || readOnly) return
        if (inputJob?.isActive == true) return
        inputJob = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                establishTextInputSession {
                    startInputMethod(
                        PlatformTextInputMethodRequest { outAttrs ->
                            val connection = ComposeEditorInputConnection(session, view)
                            connection.configureEditorInfo(outAttrs)
                            connection
                        },
                    )
                }
            } finally {
                if (inputJob?.isActive != true) {
                    inputJob = null
                }
            }
        }
    }

    private fun stopInput() {
        inputJob?.cancel()
        inputJob = null
        currentValueOf(LocalSoftwareKeyboardController)?.hide()
    }
}
