package io.github.lumkit.sweeteditor.internal

internal actual fun runOnEditorThread(block: () -> Unit) {
    block()
}

private fun dateNow(): Double = js("Date.now()")

internal actual fun editorNowMs(): Long = dateNow().toLong()
