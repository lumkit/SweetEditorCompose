package io.github.lumkit.sweeteditor.highlight.internal

internal expect class MappingLock() {
    fun <T> withLock(block: () -> T): T
}
