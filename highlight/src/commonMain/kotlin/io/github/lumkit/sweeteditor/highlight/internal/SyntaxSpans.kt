package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.StyleSpan
import io.github.lumkit.sweeteditor.highlight.runtime.SlHighlightSlice

internal fun mapSyntaxSpans(
    slice: SlHighlightSlice,
    mapping: PositionMapping,
): Map<Int, List<StyleSpan>> {
    val result = LinkedHashMap<Int, List<StyleSpan>>()
    slice.lines.forEachIndexed { offset, spans ->
        val line = slice.startLine + offset
        val mapped = spans.mapNotNull { span ->
            if (span.styleId == 0) return@mapNotNull null
            val start = mapping.toUtf16(line, span.column)
            val end = mapping.toUtf16(line, span.column + span.length)
            val length = end - start
            if (length <= 0) {
                null
            } else {
                StyleSpan(column = start, length = length, styleId = span.styleId)
            }
        }.sortedBy { it.column }
        result[line] = mapped
    }
    return result
}
