package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.highlight.runtime.SlBracketToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BracketAssemblerTest {
    @Test
    fun nestedOpenBecomesChildOfOuterGuide() {
        val mirror = TextMirror()
        mirror.setText("{ { } }")
        val mapping = PositionMapping(mirror)
        val outer = SlBracketToken(0, 0, 1, 0, isOpen = true, matched = true, 0, 6, 1)
        val inner = SlBracketToken(0, 2, 1, 1, isOpen = true, matched = true, 0, 4, 1)
        val guides = BracketAssembler.assembleGuides(
            tokens = listOf(outer, inner),
            mapping = mapping,
            visibleStartLine = 0,
            visibleEndLine = 0,
        )
        assertEquals(2, guides.size)
        val parent = guides.first { it.parent.column == 0 }
        assertEquals(listOf(TextPosition(0, 2)), parent.children)
        assertEquals(TextPosition(0, 6), parent.end)
    }

    @Test
    fun matchedHostIsCalledForCursorOnOpenToken() {
        val mirror = TextMirror()
        mirror.setText("{    }")
        val mapping = PositionMapping(mirror)
        val host = RecordingMatchedHost()
        val open = SlBracketToken(0, 0, 1, 0, isOpen = true, matched = true, 0, 5, 1)
        BracketAssembler.applyMatched(
            tokens = listOf(open),
            cursor = TextPosition(0, 0),
            mapping = mapping,
            host = host,
        )
        assertEquals(listOf(intArrayOf(0, 0, 0, 5).toList()), host.setCalls)
        assertEquals(0, host.clearCalls)
    }

    @Test
    fun rainbowUsesOverlayStyleIds() {
        val mirror = TextMirror()
        mirror.setText("{ }")
        val mapping = PositionMapping(mirror)
        val spans = BracketAssembler.rainbowSpans(
            tokens = listOf(
                SlBracketToken(0, 0, 1, 7, isOpen = true, matched = true, 0, 2, 1),
                SlBracketToken(0, 2, 1, 7, isOpen = false, matched = true, 0, 0, 1),
            ),
            mapping = mapping,
        )
        assertEquals(HighlightStyleIds.RAINBOW_0 + 1, spans[0]?.first()?.styleId)
        assertTrue(spans[0]?.all { it.styleId == HighlightStyleIds.RAINBOW_1 } == true)
    }
}

internal class RecordingMatchedHost : MatchedBracketHost {
    val setCalls = mutableListOf<List<Int>>()
    var clearCalls: Int = 0

    override fun setMatchedBrackets(openLine: Int, openColumn: Int, closeLine: Int, closeColumn: Int) {
        setCalls += listOf(openLine, openColumn, closeLine, closeColumn)
    }

    override fun clearMatchedBrackets() {
        clearCalls += 1
    }
}
