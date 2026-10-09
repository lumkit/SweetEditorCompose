package io.github.lumkit.sweeteditor.highlight.internal

import platform.Foundation.NSLock

internal actual class MappingLock actual constructor() {
    private val lock = NSLock()

    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}
