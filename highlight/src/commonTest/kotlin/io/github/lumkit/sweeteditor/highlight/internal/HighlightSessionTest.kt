package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.EditorActionSource
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.TextChange
import io.github.lumkit.sweeteditor.TextChangeKind
import io.github.lumkit.sweeteditor.TextChangedEvent
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HighlightSessionTest {
    @Test
    fun twoTextChangesDrainAsFifoPatches() {
        val native = RecordingHighlightNative()
        val session = HighlightSession(native, bindingId = "test")
        try {
            session.rebuildOnLoad("foo")
            session.onTextChanged(textEvent(replace("foo", "bar")))
            session.onTextChanged(textEvent(replace("bar", "baz")))
            assertEquals(2, session.pendingPatches().size)
            session.drainPatches(visibleStartLine = 0, visibleLineCount = 1)
            assertEquals(
                listOf(
                    RecordingHighlightNative.IncrementalCall(0, 0, 0, 3, "bar"),
                    RecordingHighlightNative.IncrementalCall(0, 0, 0, 3, "baz"),
                ),
                native.incrementals,
            )
            assertTrue(session.pendingPatches().isEmpty())
            assertTrue(session.generation >= 2)
        } finally {
            session.close()
        }
    }

    @Test
    fun bindSubscribesControllerTextChanges() {
        val native = RecordingHighlightNative()
        val controller = SweetEditorController("foo")
        val session = HighlightSession(native, bindingId = "bind")
        try {
            session.bind(controller)
            session.rebuildOnLoad("foo")
            controller.events.publish(textEvent(replace("foo", "bar")))
            controller.events.publish(textEvent(replace("bar", "x")))
            session.drainPatches(0, 1)
            assertEquals(listOf("bar", "x"), native.incrementals.map { it.newText })
        } finally {
            session.close()
        }
    }
}

private fun textEvent(change: TextChange) = TextChangedEvent(
    changes = listOf(change),
    kind = TextChangeKind.REPLACEMENT,
    source = EditorActionSource.PROGRAMMATIC,
)

private fun replace(from: String, to: String) = TextChange(
    range = TextRange(TextPosition(0, 0), TextPosition(0, from.length)),
    newText = to,
)

internal class RecordingHighlightNative : HighlightNativeOps {
    data class IncrementalCall(
        val startLine: Int,
        val startColumn: Int,
        val endLine: Int,
        val endColumn: Int,
        val newText: String,
    )

    val incrementals = mutableListOf<IncrementalCall>()
    private var nextHandle = 1L

    override val isAvailable: Boolean = true

    override fun createEngine(tabSize: Int): Long = nextHandle++

    override fun freeEngine(engine: Long) {}

    override fun createDocument(uri: String, text: String): Long = nextHandle++

    override fun freeDocument(document: Long) {}

    override fun loadDocument(engine: Long, document: Long): Long = nextHandle++

    override fun removeDocument(engine: Long, uri: String) {}

    override fun freeDocumentAnalyzer(analyzer: Long) {}

    override fun registerStyleName(engine: Long, name: String, styleId: Int) {}

    override fun compileJson(engine: Long, json: String) {}

    override fun analyzeLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? = null

    override fun getHighlightSlice(analyzer: Long, startLine: Int, lineCount: Int): IntArray? = null

    override fun analyzeIncrementalInLineRange(
        analyzer: Long,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        newText: String,
        visibleStartLine: Int,
        visibleLineCount: Int,
    ): IntArray? {
        incrementals += IncrementalCall(startLine, startColumn, endLine, endColumn, newText)
        return intArrayOf()
    }

    override fun analyzeIndentGuidesInLineRange(
        analyzer: Long,
        startLine: Int,
        lineCount: Int,
    ): IntArray? = null
}
