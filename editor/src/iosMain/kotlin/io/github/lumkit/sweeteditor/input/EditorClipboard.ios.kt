package io.github.lumkit.sweeteditor.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIPasteboard

@Composable
internal actual fun rememberEditorClipboard(): EditorClipboard = remember { IosEditorClipboard }

private object IosEditorClipboard : EditorClipboard {
    override fun getText(): String? = UIPasteboard.generalPasteboard.string

    override fun setText(text: String): Boolean {
        UIPasteboard.generalPasteboard.string = text
        return true
    }
}
