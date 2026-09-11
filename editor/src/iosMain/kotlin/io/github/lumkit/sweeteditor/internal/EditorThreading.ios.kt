package io.github.lumkit.sweeteditor.internal

import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.Foundation.NSDate
import platform.Foundation.NSThread
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
internal actual fun runOnEditorThread(block: () -> Unit) {
    if (NSThread.isMainThread) {
        block()
    } else {
        dispatch_async(dispatch_get_main_queue()) { block() }
    }
}

internal actual fun editorNowMs(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
