package io.github.lumkit.sweeteditor.highlight.internal

internal actual class MappingLock actual constructor() {
    actual fun <T> withLock(block: () -> T): T = block()
}
