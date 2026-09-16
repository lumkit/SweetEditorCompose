package io.github.lumkit.sweeteditor.highlight.internal

import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create

@OptIn(ExperimentalForeignApi::class)
internal actual class SerialAnalyzeQueue actual constructor(
    private val currentGeneration: () -> Int,
) {
    private val queue = dispatch_queue_create("sweetline-analyze", null)

    actual fun submit(generation: Int, block: () -> Unit) {
        if (generation != currentGeneration()) return
        dispatch_async(queue) {
            if (generation != currentGeneration()) return@dispatch_async
            block()
        }
    }

    actual fun close() {
    }
}
