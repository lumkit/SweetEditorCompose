package io.github.lumkit.sweeteditor.internal

import android.os.Handler
import android.os.Looper

private val mainHandler = Handler(Looper.getMainLooper())

internal actual fun runOnEditorThread(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        block()
    } else {
        mainHandler.post(block)
    }
}

internal actual fun editorNowMs(): Long = System.currentTimeMillis()
