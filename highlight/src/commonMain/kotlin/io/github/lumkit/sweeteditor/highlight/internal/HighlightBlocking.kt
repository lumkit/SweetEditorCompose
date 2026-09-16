package io.github.lumkit.sweeteditor.highlight.internal

internal expect fun runHighlightBlocking(block: suspend () -> Unit)
