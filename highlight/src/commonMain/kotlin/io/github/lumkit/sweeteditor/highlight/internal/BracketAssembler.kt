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

    fun assembleGuides(
        tokens: List<SlBracketToken>,
        mapping: PositionMapping,
        visibleStartLine: Int,
        visibleEndLine: Int,
    ): List<BracketGuide> {
        if (visibleEndLine < visibleStartLine) return emptyList()
        val opens = tokens.filter { it.isOpen && it.matched && it.partnerLine >= 0 }
        return opens.mapNotNull { open ->
            val intersects =
                open.line in visibleStartLine..visibleEndLine ||
                    open.partnerLine in visibleStartLine..visibleEndLine
            if (!intersects) return@mapNotNull null
            val parent = TextPosition(open.line, mapping.toUtf16(open.line, open.column))
            val end = TextPosition(
                open.partnerLine,
                mapping.toUtf16(open.partnerLine, open.partnerColumn),
            )
            val children = opens.mapNotNull { child ->
                if (child === open || child.depth != open.depth + 1) return@mapNotNull null
                if (!strictlyBetween(child.line, child.column, open.line, open.column, open.partnerLine, open.partnerColumn)) {
                    return@mapNotNull null
                }
                TextPosition(child.line, mapping.toUtf16(child.line, child.column))
            }
            BracketGuide(parent = parent, end = end, children = children)
        }
    }

    fun rainbowSpans(
        tokens: List<SlBracketToken>,
        mapping: PositionMapping,
    ): Map<Int, List<StyleSpan>> {
        val byLine = LinkedHashMap<Int, MutableList<StyleSpan>>()
        tokens.forEach { token ->
            if (!token.matched || token.length <= 0) return@forEach
            val start = mapping.toUtf16(token.line, token.column)
            val end = mapping.toUtf16(token.line, token.column + token.length)
            val length = end - start
            if (length <= 0) return@forEach
            val styleId = HighlightStyleIds.RAINBOW_0 + (token.depth % 6)
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

    private fun strictlyBetween(
        line: Int,
        column: Int,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): Boolean = positionLess(startLine, startColumn, line, column) &&
        positionLess(line, column, endLine, endColumn)

    private fun positionLess(lineA: Int, columnA: Int, lineB: Int, columnB: Int): Boolean =
        lineA < lineB || (lineA == lineB && columnA < columnB)
}
