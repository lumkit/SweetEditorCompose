package io.github.lumkit.sweeteditor.highlight.internal

internal actual class SerialAnalyzeQueue actual constructor(
    private val currentGeneration: () -> Int,
) {
    private val pending = ArrayDeque<() -> Unit>()
    private var scheduled = false
    private var closed = false

    actual fun submit(generation: Int, block: () -> Unit) {
        if (closed) return
        if (generation != currentGeneration()) return
        pending.addLast {
            if (generation != currentGeneration()) return@addLast
            block()
        }
        if (!scheduled) {
            scheduled = true
            enqueueMicrotask(::drain)
        }
    }

    private fun drain() {
        scheduled = false
        if (closed) {
            pending.clear()
            return
        }
        val task = pending.removeFirstOrNull() ?: return
        task()
        if (pending.isNotEmpty() && !closed) {
            scheduled = true
            enqueueMicrotask(::drain)
        }
    }

    actual fun close() {
        closed = true
        pending.clear()
    }
}

private fun enqueueMicrotask(block: () -> Unit) {
    enqueueMicrotaskJs(block)
}

@Suppress("UNUSED_PARAMETER")
private fun enqueueMicrotaskJs(block: () -> Unit): Unit =
    js("Promise.resolve().then(block)")
