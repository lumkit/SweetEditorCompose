package io.github.lumkit.sweeteditor.highlight.internal

internal class PositionMapping(
    private val mirror: TextMirror,
) {
    private val utf16OfCodePoint = HashMap<Int, IntArray?>()

    fun invalidate(startLine: Int, endLineInclusive: Int) {
        val last = endLineInclusive.coerceAtLeast(startLine)
        val keys = utf16OfCodePoint.keys.filter { it in startLine..last }
        keys.forEach { utf16OfCodePoint.remove(it) }
    }

    fun invalidateFrom(startLine: Int) {
        val keys = utf16OfCodePoint.keys.filter { it >= startLine }
        keys.forEach { utf16OfCodePoint.remove(it) }
    }

    fun toUtf16(line: Int, codePointColumn: Int): Int {
        val text = mirror.line(line)
        val table = tableFor(line, text)
        val column = codePointColumn.coerceAtLeast(0)
        if (table == null) {
            return column.coerceIn(0, text.length)
        }
        if (column >= table.size) return text.length
        return table[column]
    }

    fun toCodePoint(line: Int, utf16Column: Int): Int {
        val text = mirror.line(line)
        val utf16 = utf16Column.coerceIn(0, text.length)
        val table = tableFor(line, text)
        if (table == null) {
            return utf16
        }
        if (table.isEmpty()) return 0
        if (utf16 >= text.length) return table.size
        var lo = 0
        var hi = table.lastIndex
        var found = 0
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val start = table[mid]
            when {
                start == utf16 -> return mid
                start < utf16 -> {
                    found = mid
                    lo = mid + 1
                }
                else -> hi = mid - 1
            }
        }
        return found
    }

    private fun tableFor(line: Int, text: String): IntArray? {
        if (utf16OfCodePoint.containsKey(line)) {
            return utf16OfCodePoint[line]
        }
        val table = if (text.all { it < 0x80.toChar() }) {
            null
        } else {
            buildUtf16Table(text)
        }
        utf16OfCodePoint[line] = table
        return table
    }
}

private fun buildUtf16Table(text: String): IntArray {
    val offsets = ArrayList<Int>(text.length)
    var index = 0
    while (index < text.length) {
        offsets += index
        val ch = text[index]
        index += if (ch.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
            2
        } else {
            1
        }
    }
    return offsets.toIntArray()
}
