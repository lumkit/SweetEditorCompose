package io.github.lumkit.sweeteditor.highlight.internal

import android.os.Handler
import android.os.Looper

private val mainHandler = Handler(Looper.getMainLooper())

internal actual fun runOnHighlightHostThread(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        block()
    } else {
        mainHandler.post(block)
    }
}

internal actual fun highlightNowMs(): Long = System.currentTimeMillis()
