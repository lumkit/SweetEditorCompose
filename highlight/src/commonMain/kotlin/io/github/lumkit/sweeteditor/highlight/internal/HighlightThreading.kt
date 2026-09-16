package io.github.lumkit.sweeteditor.highlight.internal

internal expect fun runOnHighlightHostThread(block: () -> Unit)

internal expect fun highlightNowMs(): Long
