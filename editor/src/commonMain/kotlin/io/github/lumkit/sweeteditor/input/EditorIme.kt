package io.github.lumkit.sweeteditor.input

import androidx.compose.ui.Modifier
import io.github.lumkit.sweeteditor.core.protocol.EditorActionResult
import io.github.lumkit.sweeteditor.session.RememberedEditorSession

internal interface EditorImeAdapter {
    fun onEditorActionResult(result: EditorActionResult)
    fun closeOwnedSession()
}

internal expect fun Modifier.editorIme(session: RememberedEditorSession): Modifier
