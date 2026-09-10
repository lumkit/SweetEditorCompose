package io.github.lumkit.sweeteditor.input

import androidx.compose.ui.Modifier
import io.github.lumkit.sweeteditor.session.RememberedEditorSession

internal actual fun Modifier.editorIme(session: RememberedEditorSession): Modifier = this
