package io.github.lumkit.sweeteditor.highlight.internal

import java.awt.EventQueue

internal actual fun runOnHighlightHostThread(block: () -> Unit) {
    try {
        if (EventQueue.isDispatchThread() || java.awt.GraphicsEnvironment.isHeadless()) {
            block()
        } else {
            EventQueue.invokeAndWait(block)
        }
    } catch (_: Throwable) {
        block()
    }
}

internal actual fun highlightNowMs(): Long = System.currentTimeMillis()
