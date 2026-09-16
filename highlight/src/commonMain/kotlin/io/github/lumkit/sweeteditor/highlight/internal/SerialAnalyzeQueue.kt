package io.github.lumkit.sweeteditor.highlight.internal

internal expect class SerialAnalyzeQueue(currentGeneration: () -> Int) {
    fun submit(generation: Int, block: () -> Unit)

    fun close()
}
