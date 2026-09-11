package io.github.lumkit.sweeteditor.input

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberEditorClipboard(): EditorClipboard {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidEditorClipboard(context) }
}

private class AndroidEditorClipboard(
    private val context: Context,
) : EditorClipboard {
    private val manager: ClipboardManager?
        get() = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    override fun getText(): String? {
        val clip = manager?.primaryClip ?: return null
        if (clip.itemCount <= 0) return null
        return clip.getItemAt(0).coerceToText(context)?.toString()
    }

    override fun setText(text: String): Boolean {
        val clipboard = manager ?: return false
        clipboard.setPrimaryClip(ClipData.newPlainText("SweetEditor", text))
        return true
    }
}
