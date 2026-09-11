package io.github.lumkit.sweeteditor.input

import androidx.compose.runtime.Composable

internal interface EditorClipboard {
    fun getText(): String?
    fun setText(text: String): Boolean
}

@Composable
internal expect fun rememberEditorClipboard(): EditorClipboard
