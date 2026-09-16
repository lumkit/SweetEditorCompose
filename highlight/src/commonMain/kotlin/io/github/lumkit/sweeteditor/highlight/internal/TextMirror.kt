package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.TextChange

internal class TextMirror {
    private val lines: MutableList<String> = mutableListOf("")

    val lineCount: Int
        get() = lines.size

    fun line(index: Int): String {
        if (index < 0 || index >= lines.size) return ""
        return lines[index]
    }

    fun joinToString(separator: String = "\n"): String = lines.joinToString(separator)

    fun setText(text: String) {
        lines.clear()
        lines.addAll(splitEditorLines(text))
    }

    fun apply(change: TextChange) {
        val startLine = change.range.start.line.coerceIn(0, lines.lastIndex)
        val endLine = change.range.end.line.coerceIn(0, lines.lastIndex)
        val startColumn = change.range.start.column.coerceIn(0, lines[startLine].length)
        val endColumn = change.range.end.column.coerceIn(0, lines[endLine].length)
        val prefix = lines[startLine].substring(0, startColumn)
        val suffix = lines[endLine].substring(endColumn)
        val inserted = splitEditorLines(change.newText).toMutableList()
        inserted[0] = prefix + inserted[0]
        inserted[inserted.lastIndex] = inserted.last() + suffix
        val removeCount = endLine - startLine + 1
        repeat(removeCount) { lines.removeAt(startLine) }
        lines.addAll(startLine, inserted)
    }
}

internal fun splitEditorLines(text: String): List<String> {
    val result = ArrayList<String>()
    var start = 0
    var index = 0
    while (index < text.length) {
        val ch = text[index]
        when {
            ch == '\r' && index + 1 < text.length && text[index + 1] == '\n' -> {
                result += text.substring(start, index)
                index += 2
                start = index
            }
            ch == '\n' || ch == '\r' -> {
                result += text.substring(start, index)
                index += 1
                start = index
            }
            else -> index += 1
        }
    }
    result += text.substring(start)
    return result
}
