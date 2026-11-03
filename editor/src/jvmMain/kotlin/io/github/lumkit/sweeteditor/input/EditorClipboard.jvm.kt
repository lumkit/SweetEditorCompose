package io.github.lumkit.sweeteditor.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

@Composable
internal actual fun rememberEditorClipboard(): EditorClipboard = remember { JvmEditorClipboard }

internal object JvmEditorClipboard : EditorClipboard {
    @Volatile
    private var fallbackText: String? = null

    override fun getText(): String? = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
    }.getOrElse { fallbackText }

    override fun setText(text: String): Boolean {
        fallbackText = text
        return runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            true
        }.getOrDefault(true)
    }
}
