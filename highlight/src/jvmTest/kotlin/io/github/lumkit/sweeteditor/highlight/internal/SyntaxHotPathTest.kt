package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.DecorationContext
import io.github.lumkit.sweeteditor.EditorActionSource
import io.github.lumkit.sweeteditor.TextChange
import io.github.lumkit.sweeteditor.TextChangeKind
import io.github.lumkit.sweeteditor.TextChangedEvent
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import io.github.lumkit.sweeteditor.VisibleLineRange
import io.github.lumkit.sweeteditor.highlight.HighlightDocumentDescriptor
import io.github.lumkit.sweeteditor.highlight.runtime.NativeBridge
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SyntaxHotPathTest {
    @Test
    fun intDeclarationHighlightsKeywordThenIncrementalInsert() = runBlocking {
        assertTrue(NativeBridge.isAvailable)
        val json = checkNotNull(SyntaxCatalog().loadJson("c"))
        val native = CountingHighlightNative()
        val session = HighlightSession(
            native,
            bindingId = "syntax",
            descriptor = HighlightDocumentDescriptor(fileName = "sample.c"),
        )
        try {
            session.compileSyntaxJson(json)
            session.rebuildOnLoad("int x;")
            val context = DecorationContext(
                visibleLineRange = VisibleLineRange(0, 0),
                totalLineCount = 1,
                textChanges = emptyList(),
            )
            val first = session.buildDecorationResult(context)
            val keywordSpans = first.syntaxSpans.orEmpty().values.flatten()
                .filter { it.styleId == HighlightStyleIds.KEYWORD_ID }
            assertTrue(keywordSpans.isNotEmpty(), "expected keyword span in $first")
            assertTrue(native.lineRangeCalls >= 1)
            assertTrue(native.incrementalCalls == 0)

            session.onTextChanged(
                TextChangedEvent(
                    changes = listOf(
                        TextChange(
                            range = TextRange(TextPosition(0, 3), TextPosition(0, 3)),
                            newText = " ",
                        ),
                    ),
                    kind = TextChangeKind.INSERTION,
                    source = EditorActionSource.PROGRAMMATIC,
                ),
            )
            session.buildDecorationResult(context)
            assertTrue(native.incrementalCalls >= 1)
            assertTrue(native.lineRangeCalls == 1)
        } finally {
            session.close()
        }
    }
}
