package io.github.lumkit.sweeteditor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class SweetEditorController(
    initialText: String = "",
) {
    var text by mutableStateOf(initialText)
}
