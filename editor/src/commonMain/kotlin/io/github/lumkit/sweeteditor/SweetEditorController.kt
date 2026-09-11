package io.github.lumkit.sweeteditor

import androidx.compose.runtime.Stable
import io.github.lumkit.sweeteditor.session.RememberedEditorSession

@Stable
class SweetEditorController(
    internal val initialText: String = "",
) {
    private var session: RememberedEditorSession? = null
    private val readyCallbacks = mutableListOf<() -> Unit>()
    val events = EditorEventBus()

    val isReady: Boolean get() = session?.isReady == true

    fun whenReady(block: () -> Unit) {
        val current = session
        if (current != null && current.isReady) {
            block()
        } else {
            readyCallbacks += block
        }
    }

    fun insertText(text: String) {
        session?.insertText(text)
    }

    fun undo() {
        session?.undo()
    }

    fun redo() {
        session?.redo()
    }

    fun backspace() {
        session?.backspace()
    }

    fun moveLineUp() {
        session?.moveLineUp()
    }

    fun moveLineDown() {
        session?.moveLineDown()
    }

    fun copyLineUp() {
        session?.copyLineUp()
    }

    fun copyLineDown() {
        session?.copyLineDown()
    }

    fun deleteLine() {
        session?.deleteLine()
    }

    fun insertLineAbove() {
        session?.insertLineAbove()
    }

    fun insertLineBelow() {
        session?.insertLineBelow()
    }

    fun onTextChanged(listener: (TextChangedEvent) -> Unit): () -> Unit =
        events.subscribe(listener)

    fun onCursorChanged(listener: (CursorChangedEvent) -> Unit): () -> Unit =
        events.subscribe(listener)

    fun onSelectionChanged(listener: (SelectionChangedEvent) -> Unit): () -> Unit =
        events.subscribe(listener)

    fun onScrollChanged(listener: (ScrollChangedEvent) -> Unit): () -> Unit =
        events.subscribe(listener)

    fun onScaleChanged(listener: (ScaleChangedEvent) -> Unit): () -> Unit =
        events.subscribe(listener)

    fun dispose() {
        readyCallbacks.clear()
        events.clear()
    }

    internal fun attach(next: RememberedEditorSession) {
        check(session == null || session === next) {
            "SweetEditorController is already attached to another SweetEditor"
        }
        session = next
        val pending = readyCallbacks.toList()
        readyCallbacks.clear()
        pending.forEach { it() }
    }

    internal fun detach(current: RememberedEditorSession) {
        if (session === current) {
            session = null
        }
    }
}
