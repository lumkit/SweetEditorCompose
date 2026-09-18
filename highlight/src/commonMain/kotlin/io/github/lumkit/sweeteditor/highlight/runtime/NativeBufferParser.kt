package io.github.lumkit.sweeteditor.highlight.runtime

internal object NativeBufferParser {
    private const val SPAN_STRIDE = 3
    private const val BRACKET_STRIDE = 8
    private const val FLAG_HAS_START_INDEX = 1
    private const val FLAG_INLINE_STYLE = 1 shl 1

    fun parseHighlightSlice(buffer: IntArray?): SlHighlightSlice {
        val empty = SlHighlightSlice(startLine = 0, totalLineCount = 0, lines = emptyList())
        if (buffer == null || buffer.size < 5) {
            return empty
        }
        val flags = buffer[0]
        if (flags and (FLAG_HAS_START_INDEX or FLAG_INLINE_STYLE) != 0) {
            return empty
        }
        if (buffer[1] != SPAN_STRIDE) {
            return empty
        }
        val startLine = buffer[2]
        val totalLineCount = buffer[3]
        val lineCount = buffer[4]
        if (lineCount < 0) {
            return empty
        }
        var index = 5
        val lines = ArrayList<List<SlTokenSpan>>(lineCount)
        repeat(lineCount) { offset ->
            if (index >= buffer.size) {
                return empty
            }
            val spanCount = buffer[index++]
            if (spanCount < 0 || remaining(buffer, index) < spanCount * SPAN_STRIDE) {
                return empty
            }
            val line = startLine + offset
            val spans = ArrayList<SlTokenSpan>(spanCount)
            repeat(spanCount) {
                spans += SlTokenSpan(
                    line = line,
                    column = buffer[index++],
                    length = buffer[index++],
                    styleId = buffer[index++],
                )
            }
            lines += spans
        }
        return SlHighlightSlice(startLine, totalLineCount, lines)
    }

    fun parseIndentGuides(buffer: IntArray?): List<SlIndentGuide> {
        if (buffer == null || buffer.size < 3) {
            return emptyList()
        }
        val lineStateCount = buffer[1]
        val guideCount = buffer[2]
        if (lineStateCount < 0 || guideCount < 0) {
            return emptyList()
        }
        var index = 3
        val guides = ArrayList<SlIndentGuide>(guideCount)
        repeat(guideCount) {
            if (remaining(buffer, index) < 5) {
                return emptyList()
            }
            val column = buffer[index++]
            val startLine = buffer[index++]
            val endLine = buffer[index++]
            val flags = buffer[index++]
            val branchCount = buffer[index++]
            if (branchCount < 0 || remaining(buffer, index) < branchCount * 2) {
                return emptyList()
            }
            index += branchCount * 2
            guides += SlIndentGuide(
                column = column,
                startLine = startLine,
                endLine = endLine,
                flags = flags,
            )
        }
        if (remaining(buffer, index) < lineStateCount * 4) {
            return emptyList()
        }
        return guides
    }

    fun parseBracketSlice(buffer: IntArray?): SlBracketSlice {
        val empty = SlBracketSlice(startLine = 0, totalLineCount = 0, lines = emptyList())
        if (buffer == null || buffer.size < 5) {
            return empty
        }
        if (buffer[0] and FLAG_HAS_START_INDEX != 0) {
            return empty
        }
        if (buffer[1] != BRACKET_STRIDE) {
            return empty
        }
        val startLine = buffer[2]
        val totalLineCount = buffer[3]
        val lineCount = buffer[4]
        if (lineCount < 0) {
            return empty
        }
        var index = 5
        val lines = ArrayList<List<SlBracketToken>>(lineCount)
        repeat(lineCount) { offset ->
            if (index >= buffer.size) {
                return empty
            }
            val tokenCount = buffer[index++]
            if (tokenCount < 0 || remaining(buffer, index) < tokenCount * BRACKET_STRIDE) {
                return empty
            }
            val line = startLine + offset
            val tokens = ArrayList<SlBracketToken>(tokenCount)
            repeat(tokenCount) {
                val column = buffer[index++]
                val length = buffer[index++]
                val depth = buffer[index++]
                val kind = buffer[index++]
                val matchState = buffer[index++]
                val partnerLine = buffer[index++]
                val partnerColumn = buffer[index++]
                val partnerLength = buffer[index++]
                tokens += SlBracketToken(
                    line = line,
                    column = column,
                    length = length,
                    depth = depth,
                    isOpen = kind == 0,
                    matched = matchState == 0,
                    matchState = matchState,
                    partnerLine = partnerLine,
                    partnerColumn = partnerColumn,
                    partnerLength = partnerLength,
                )
            }
            lines += tokens
        }
        return SlBracketSlice(startLine, totalLineCount, lines)
    }

    private fun remaining(buffer: IntArray, index: Int): Int = buffer.size - index
}
