package io.github.lumkit.sweeteditor

import kotlin.reflect.KClass
import io.github.lumkit.sweeteditor.core.protocol.EditorActionResult
import io.github.lumkit.sweeteditor.core.protocol.TextChange as CoreTextChange
import io.github.lumkit.sweeteditor.core.protocol.TextPosition as CoreTextPosition
import io.github.lumkit.sweeteditor.core.protocol.TextRange as CoreTextRange

enum class EditorActionSource(val value: Int) {
    NONE(0),
    SETUP(1),
    PROGRAMMATIC(2),
    KEYBOARD(3),
    IME(4),
    GESTURE(5),
    ANIMATION(6),
    DECORATION(7),
    FOLDING(8),
    SEARCH(9),
    LINKED_EDITING(10),
    DIFF(11),
    ;

    companion object {
        fun fromValue(value: Int): EditorActionSource =
            entries.find { it.value == value } ?: NONE
    }
}

enum class TextChangeKind(val value: Int) {
    NONE(0),
    INSERTION(1),
    REPLACEMENT(2),
    DELETION(3),
    MOVE(4),
    UNDO(5),
    REDO(6),
    MIXED(7),
    ;

    companion object {
        fun fromValue(value: Int): TextChangeKind =
            entries.find { it.value == value } ?: NONE
    }
}

data class TextPosition(
    val line: Int,
    val column: Int,
)

data class TextRange(
    val start: TextPosition,
    val end: TextPosition,
)

data class TextChange(
    val range: TextRange,
    val newText: String,
)

sealed interface EditorEvent

data class TextChangedEvent(
    val changes: List<TextChange>,
    val kind: TextChangeKind,
    val source: EditorActionSource,
) : EditorEvent

data class CursorChangedEvent(
    val cursorPosition: TextPosition,
) : EditorEvent

data class SelectionChangedEvent(
    val hasSelection: Boolean,
    val selection: TextRange?,
    val cursorPosition: TextPosition,
) : EditorEvent

data class ScrollChangedEvent(
    val scrollX: Float,
    val scrollY: Float,
) : EditorEvent

data class ScaleChangedEvent(
    val scale: Float,
) : EditorEvent

class EditorEventBus {
    private val listeners = mutableMapOf<KClass<out EditorEvent>, MutableList<(EditorEvent) -> Unit>>()

    fun <T : EditorEvent> subscribe(type: KClass<T>, listener: (T) -> Unit): () -> Unit {
        val box: (EditorEvent) -> Unit = { event ->
            if (type.isInstance(event)) {
                @Suppress("UNCHECKED_CAST")
                listener(event as T)
            }
        }
        listeners.getOrPut(type) { mutableListOf() }.add(box)
        return { listeners[type]?.remove(box) }
    }

    fun publish(event: EditorEvent) {
        listeners[event::class].orEmpty().toList().forEach { notify -> notify(event) }
    }

    fun clear() {
        listeners.clear()
    }
}

inline fun <reified T : EditorEvent> EditorEventBus.subscribe(noinline listener: (T) -> Unit): () -> Unit =
    subscribe(T::class, listener)

internal fun collectStateEvents(result: EditorActionResult): List<EditorEvent> {
    val events = ArrayList<EditorEvent>(5)
    if (result.textChanges.isNotEmpty()) {
        events += TextChangedEvent(
            changes = result.textChanges.map { it.toPublic() },
            kind = TextChangeKind.fromValue(result.textChangeKind.value),
            source = EditorActionSource.fromValue(result.source.value),
        )
    }
    if (result.cursorChanged) {
        events += CursorChangedEvent(result.cursorAfter.toPublic())
    }
    if (result.selectionChanged) {
        events += SelectionChangedEvent(
            hasSelection = result.hasSelectionAfter,
            selection = result.selectionAfter.toPublic().takeIf { result.hasSelectionAfter },
            cursorPosition = result.cursorAfter.toPublic(),
        )
    }
    if (result.scrollChanged) {
        events += ScrollChangedEvent(result.scrollXAfter, result.scrollYAfter)
    }
    if (result.scaleChanged) {
        events += ScaleChangedEvent(result.scaleAfter)
    }
    return events
}

private fun CoreTextPosition.toPublic(): TextPosition = TextPosition(line, column)

private fun CoreTextRange.toPublic(): TextRange = TextRange(start.toPublic(), end.toPublic())

private fun CoreTextChange.toPublic(): TextChange = TextChange(range.toPublic(), newText)
