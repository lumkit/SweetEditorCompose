package io.github.lumkit.sweeteditor.highlight.internal

internal actual fun runOnHighlightHostThread(block: () -> Unit) {
    block()
}

internal actual fun highlightNowMs(): Long = (js("Date.now()") as Number).toLong()
