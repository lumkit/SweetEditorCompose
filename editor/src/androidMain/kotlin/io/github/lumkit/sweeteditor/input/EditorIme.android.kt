package io.github.lumkit.sweeteditor.input

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.establishTextInputSession
import io.github.lumkit.sweeteditor.session.RememberedEditorSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal actual fun Modifier.editorIme(session: RememberedEditorSession): Modifier =
    this.then(EditorImeElement(session))

private data class EditorImeElement(
    val session: RememberedEditorSession,
) : ModifierNodeElement<EditorImeNode>() {
    override fun create(): EditorImeNode = EditorImeNode(session)

    override fun update(node: EditorImeNode) {
        node.session = session
    }
}

private class EditorImeNode(
    var session: RememberedEditorSession,
) : Modifier.Node(), PlatformTextInputModifierNode, FocusEventModifierNode {
    private var focused = false
    private var inputJob: Job? = null

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
        stopInput()
        super.onDetach()
    }

    private fun startInput() {
        inputJob?.cancel()
        inputJob = coroutineScope.launch {
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
    }

    private fun stopInput() {
        inputJob?.cancel()
        inputJob = null
    }
}
