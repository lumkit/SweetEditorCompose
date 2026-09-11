package io.github.lumkit.sweeteditor.internal

import java.awt.EventQueue

internal actual fun runOnEditorThread(block: () -> Unit) {
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

internal actual fun editorNowMs(): Long = System.currentTimeMillis()
