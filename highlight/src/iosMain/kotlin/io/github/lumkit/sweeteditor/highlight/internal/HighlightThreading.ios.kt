package io.github.lumkit.sweeteditor.highlight.internal

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSThread
import platform.Foundation.timeIntervalSince1970
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
internal actual fun runOnHighlightHostThread(block: () -> Unit) {
    if (NSThread.isMainThread) {
        block()
    } else {
        dispatch_async(dispatch_get_main_queue()) { block() }
    }
}

internal actual fun highlightNowMs(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
