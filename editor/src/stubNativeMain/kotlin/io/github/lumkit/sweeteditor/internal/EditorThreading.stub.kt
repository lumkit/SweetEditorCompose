package io.github.lumkit.sweeteditor.internal

internal actual fun runOnEditorThread(block: () -> Unit) {
    block()
}

internal actual fun editorNowMs(): Long = 0L
