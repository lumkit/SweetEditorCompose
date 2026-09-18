package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.highlight.runtime.SlBracketToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BracketAssemblerTest {
    @Test
    fun assembleGuidesIsEmptyLikeOfficialDemos() {
        val mirror = TextMirror()
        mirror.setText("{\n  {\n  }\n}")
        val mapping = PositionMapping(mirror)
        val outer = token(0, 0, depth = 0, partnerLine = 3)
        val inner = token(1, 2, depth = 1, partnerLine = 2, partnerColumn = 2)
        val guides = BracketAssembler.assembleGuides(
            tokens = listOf(outer, inner),
            mapping = mapping,
            visibleStartLine = 0,
            visibleEndLine = 3,
        )
        assertTrue(guides.isEmpty())
    }

    @Test
    fun matchedHostIsCalledForCursorOnOpenToken() {
        val mirror = TextMirror()
        mirror.setText("{    }")
        val mapping = PositionMapping(mirror)
        val host = RecordingMatchedHost()
        val open = token(0, 0, depth = 0, partnerLine = 0, partnerColumn = 5)
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
    fun rainbowUsesOfficialDepthPaletteAndUnmatchedRed() {
        val mirror = TextMirror()
        mirror.setText("{ }(")
        val mapping = PositionMapping(mirror)
        val spans = BracketAssembler.rainbowSpans(
            tokens = listOf(
                token(0, 0, depth = 7, partnerLine = 0, partnerColumn = 2),
                token(0, 2, depth = 7, isOpen = false, partnerLine = 0, partnerColumn = 0),
                token(0, 3, depth = 0, matchState = 1, matched = false, partnerLine = -1, partnerColumn = -1),
            ),
            mapping = mapping,
        )
        assertEquals(HighlightStyleIds.RAINBOW_0 + 1, spans[0]?.get(0)?.styleId)
        assertEquals(HighlightStyleIds.RAINBOW_0 + 1, spans[0]?.get(1)?.styleId)
        assertEquals(HighlightStyleIds.BRACKET_UNMATCHED, spans[0]?.get(2)?.styleId)
    }
}

private fun token(
    line: Int,
    column: Int,
    depth: Int,
    isOpen: Boolean = true,
    matched: Boolean = true,
    matchState: Int = 0,
    partnerLine: Int,
    partnerColumn: Int = 0,
    partnerLength: Int = 1,
): SlBracketToken = SlBracketToken(
    line = line,
    column = column,
    length = 1,
    depth = depth,
    isOpen = isOpen,
    matched = matched,
    matchState = matchState,
    partnerLine = partnerLine,
    partnerColumn = partnerColumn,
    partnerLength = partnerLength,
)

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
