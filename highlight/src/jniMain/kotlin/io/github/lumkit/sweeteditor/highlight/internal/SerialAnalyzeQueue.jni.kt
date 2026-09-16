package io.github.lumkit.sweeteditor.highlight.internal

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal actual class SerialAnalyzeQueue actual constructor(
    private val currentGeneration: () -> Int,
) {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "sweetline-analyze").apply { isDaemon = true }
    }

    actual fun submit(generation: Int, block: () -> Unit) {
        if (generation != currentGeneration()) return
        executor.execute {
            if (generation != currentGeneration()) return@execute
            block()
        }
    }

    actual fun close() {
        executor.shutdownNow()
        executor.awaitTermination(1, TimeUnit.SECONDS)
    }
}
