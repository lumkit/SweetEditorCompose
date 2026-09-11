package io.github.lumkit.sweeteditor.input

import androidx.compose.runtime.Composable

@Composable
internal actual fun rememberEditorClipboard(): EditorClipboard = NoopEditorClipboard

private object NoopEditorClipboard : EditorClipboard {
    override fun getText(): String? = null
    override fun setText(text: String): Boolean = false
}
