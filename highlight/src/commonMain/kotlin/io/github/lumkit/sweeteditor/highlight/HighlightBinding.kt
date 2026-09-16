package io.github.lumkit.sweeteditor.highlight

import io.github.lumkit.sweeteditor.SweetEditorController

interface HighlightBinding {
    fun requestRefresh()
    fun close()
}
