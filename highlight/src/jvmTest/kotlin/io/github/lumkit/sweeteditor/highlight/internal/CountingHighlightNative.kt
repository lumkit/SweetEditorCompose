package io.github.lumkit.sweeteditor.highlight.internal

internal class CountingHighlightNative(
    private val inner: HighlightNativeOps = DefaultHighlightNative,
) : HighlightNativeOps by inner {
    var incrementalCalls: Int = 0
    var sliceCalls: Int = 0
    var lineRangeCalls: Int = 0

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
        incrementalCalls += 1
        return inner.analyzeIncrementalInLineRange(
            analyzer,
            startLine,
            startColumn,
            endLine,
            endColumn,
            newText,
            visibleStartLine,
            visibleLineCount,
        )
    }

    override fun getHighlightSlice(analyzer: Long, startLine: Int, lineCount: Int): IntArray? {
        sliceCalls += 1
        return inner.getHighlightSlice(analyzer, startLine, lineCount)
    }

    override fun analyzeLineRange(analyzer: Long, startLine: Int, lineCount: Int): IntArray? {
        lineRangeCalls += 1
        return inner.analyzeLineRange(analyzer, startLine, lineCount)
    }
}
