package io.github.lumkit.sweeteditor.highlight.runtime

internal data class SlLineRange(
    val startLine: Int,
    val lineCount: Int,
)

internal data class SlTokenSpan(
    val line: Int,
    val column: Int,
    val length: Int,
    val styleId: Int,
)

internal data class SlHighlightSlice(
    val startLine: Int,
    val totalLineCount: Int,
    val lines: List<List<SlTokenSpan>>,
)

internal data class SlIndentGuide(
    val column: Int,
    val startLine: Int,
    val endLine: Int,
    val flags: Int,
)

internal data class SlBracketToken(
    val line: Int,
    val column: Int,
    val length: Int,
    val depth: Int,
    val isOpen: Boolean,
    val matched: Boolean,
    val matchState: Int = 0,
    val partnerLine: Int,
    val partnerColumn: Int,
    val partnerLength: Int,
)

internal data class SlBracketSlice(
    val startLine: Int,
    val totalLineCount: Int,
    val lines: List<List<SlBracketToken>>,
)
