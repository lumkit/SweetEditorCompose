package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.TextChange

internal data class PendingPatch(
    val slStartLine: Int,
    val slStartColumn: Int,
    val slEndLine: Int,
    val slEndColumn: Int,
    val newText: String,
)

internal fun TextMirror.encodePatch(
    change: TextChange,
    mapping: PositionMapping,
): PendingPatch {
    val start = change.range.start
    val end = change.range.end
    return PendingPatch(
        slStartLine = start.line,
        slStartColumn = mapping.toCodePoint(start.line, start.column),
        slEndLine = end.line,
        slEndColumn = mapping.toCodePoint(end.line, end.column),
        newText = change.newText,
    )
}
