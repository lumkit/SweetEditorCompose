package io.github.lumkit.sweeteditor.highlight.internal

internal actual fun runOnHighlightHostThread(block: () -> Unit) {
    block()
}

private fun dateNow(): Double = js("Date.now()")

internal actual fun highlightNowMs(): Long = dateNow().toLong()
