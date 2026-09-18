package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.BracketGuide
import io.github.lumkit.sweeteditor.StyleSpan
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.highlight.runtime.SlBracketSlice
import io.github.lumkit.sweeteditor.highlight.runtime.SlBracketToken

internal interface MatchedBracketHost {
    fun setMatchedBrackets(openLine: Int, openColumn: Int, closeLine: Int, closeColumn: Int)
    fun clearMatchedBrackets()
}

internal object BracketAssembler {
    fun flatten(slice: SlBracketSlice): List<SlBracketToken> = slice.lines.flatten()

    /**
     * Official SweetLine KMP/Flutter/OHOS demos never draw bracket *lines*.
     * They only recolor `()`, `[]`, `{}` glyphs (see [rainbowSpans]). SweetEditor's
     * [BracketGuide] tree (vertical + child horizontals) is a different widget.
     */
    @Suppress("UNUSED_PARAMETER")
    fun assembleGuides(
        tokens: List<SlBracketToken>,
        mapping: PositionMapping,
        visibleStartLine: Int,
        visibleEndLine: Int,
    ): List<BracketGuide> {
        return emptyList()
    }

    fun rainbowSpans(
        tokens: List<SlBracketToken>,
        mapping: PositionMapping,
    ): Map<Int, List<StyleSpan>> {
        val byLine = LinkedHashMap<Int, MutableList<StyleSpan>>()
        tokens.forEach { token ->
            if (token.length <= 0) return@forEach
            val start = mapping.toUtf16(token.line, token.column)
            val end = mapping.toUtf16(token.line, token.column + token.length)
            val length = end - start
            if (length <= 0) return@forEach
            val styleId = officialBracketStyleId(token.matchState, token.depth)
            byLine.getOrPut(token.line) { ArrayList() } += StyleSpan(start, length, styleId)
        }
        byLine.values.forEach { line -> line.sortBy { it.column } }
        return byLine
    }

    fun applyMatched(
        tokens: List<SlBracketToken>,
        cursor: TextPosition?,
        mapping: PositionMapping,
        host: MatchedBracketHost,
    ) {
        if (cursor == null) {
            host.clearMatchedBrackets()
            return
        }
        val codePointColumn = mapping.toCodePoint(cursor.line, cursor.column)
        val hit = tokens.firstOrNull { token ->
            token.line == cursor.line &&
                codePointColumn >= token.column &&
                codePointColumn < token.column + token.length
        }
        if (hit == null || !hit.matched || hit.partnerLine < 0) {
            host.clearMatchedBrackets()
            return
        }
        val openLine: Int
        val openColumn: Int
        val closeLine: Int
        val closeColumn: Int
        if (hit.isOpen) {
            openLine = hit.line
            openColumn = mapping.toUtf16(hit.line, hit.column)
            closeLine = hit.partnerLine
            closeColumn = mapping.toUtf16(hit.partnerLine, hit.partnerColumn)
        } else {
            openLine = hit.partnerLine
            openColumn = mapping.toUtf16(hit.partnerLine, hit.partnerColumn)
            closeLine = hit.line
            closeColumn = mapping.toUtf16(hit.line, hit.column)
        }
        host.setMatchedBrackets(openLine, openColumn, closeLine, closeColumn)
    }

    private fun officialBracketStyleId(matchState: Int, depth: Int): Int {
        if (matchState == MATCH_UNMATCHED) {
            return HighlightStyleIds.BRACKET_UNMATCHED
        }
        val palette = ((depth % 6) + 6) % 6
        return if (matchState == MATCH_UNKNOWN) {
            HighlightStyleIds.RAINBOW_UNKNOWN_0 + palette
        } else {
            HighlightStyleIds.RAINBOW_0 + palette
        }
    }

    private const val MATCH_UNMATCHED = 1
    private const val MATCH_UNKNOWN = 2
}
