package io.github.lumkit.sweeteditor.internal

internal expect fun runOnEditorThread(block: () -> Unit)

internal expect fun editorNowMs(): Long
