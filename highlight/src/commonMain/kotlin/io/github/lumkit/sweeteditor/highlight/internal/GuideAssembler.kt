package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.IndentGuide
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.highlight.runtime.SlIndentGuide
import kotlin.math.max
import kotlin.math.min

internal object GuideAssembler {
    private const val CONTINUES_BEFORE = 1
    private const val CONTINUES_AFTER = 1 shl 1

    fun assemble(
        guides: List<SlIndentGuide>,
        mapping: PositionMapping,
        visibleStartLine: Int,
        visibleEndLine: Int,
    ): List<IndentGuide> {
        if (visibleEndLine < visibleStartLine) return emptyList()
        return guides.mapNotNull { guide ->
            var startLine = guide.startLine
            var endLine = guide.endLine
            if (guide.flags and CONTINUES_BEFORE != 0) {
                startLine = max(visibleStartLine, startLine)
            }
            if (guide.flags and CONTINUES_AFTER != 0) {
                endLine = min(visibleEndLine, endLine)
            }
            if (endLine < startLine) return@mapNotNull null
            if (endLine < visibleStartLine || startLine > visibleEndLine) return@mapNotNull null
            val column = mapping.toUtf16(startLine, guide.column)
            IndentGuide(
                start = TextPosition(startLine, column),
                end = TextPosition(endLine, column),
            )
        }
    }
}
