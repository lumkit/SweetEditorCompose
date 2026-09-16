package io.github.lumkit.sweeteditor.highlight.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeBufferParserTest {
    @Test
    fun highlightSliceParsesOneSpan() {
        val buffer = intArrayOf(0, 3, 0, 1, 1, 1, 0, 1, 1)
        val slice = NativeBufferParser.parseHighlightSlice(buffer)
        assertEquals(0, slice.startLine)
        assertEquals(1, slice.totalLineCount)
        assertEquals(1, slice.lines.size)
        assertEquals(
            listOf(SlTokenSpan(line = 0, column = 0, length = 1, styleId = 1)),
            slice.lines[0],
        )
    }

    @Test
    fun highlightSliceRejectsInlineStyle() {
        val buffer = intArrayOf(2, 3, 0, 1, 1, 1, 0, 1, 1)
        val slice = NativeBufferParser.parseHighlightSlice(buffer)
        assertTrue(slice.lines.isEmpty())
    }

    @Test
    fun highlightSliceRejectsStartIndexAndTruncation() {
        assertTrue(NativeBufferParser.parseHighlightSlice(intArrayOf(1, 4, 0, 1, 1)).lines.isEmpty())
        assertTrue(NativeBufferParser.parseHighlightSlice(intArrayOf(0, 3, 0, 1, 1, 1, 0, 1)).lines.isEmpty())
        assertTrue(NativeBufferParser.parseHighlightSlice(null).lines.isEmpty())
    }

    @Test
    fun indentGuidesSkipBranches() {
        val buffer = intArrayOf(
            0, 1, 1,
            4, 0, 2, 1, 1, 1, 8,
            0, 2, 4, 1,
        )
        val guides = NativeBufferParser.parseIndentGuides(buffer)
        assertEquals(listOf(SlIndentGuide(column = 4, startLine = 0, endLine = 2, flags = 1)), guides)
    }

    @Test
    fun bracketSliceParsesOpenToken() {
        val buffer = intArrayOf(
            0, 8, 0, 1, 1,
            1,
            0, 1, 0, 0, 0, 0, 5, 1,
        )
        val slice = NativeBufferParser.parseBracketSlice(buffer)
        assertEquals(1, slice.lines.size)
        assertEquals(
            SlBracketToken(
                line = 0,
                column = 0,
                length = 1,
                depth = 0,
                isOpen = true,
                matched = true,
                partnerLine = 0,
                partnerColumn = 5,
                partnerLength = 1,
            ),
            slice.lines[0][0],
        )
    }
}
