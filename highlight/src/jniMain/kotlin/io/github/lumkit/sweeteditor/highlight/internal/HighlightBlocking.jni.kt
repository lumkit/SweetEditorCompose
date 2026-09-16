package io.github.lumkit.sweeteditor.highlight.internal

import kotlinx.coroutines.runBlocking

internal actual fun runHighlightBlocking(block: suspend () -> Unit) {
    runBlocking { block() }
}
