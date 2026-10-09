package io.github.lumkit.sweeteditor.highlight.internal

internal class PositionMapping(
    private val mirror: TextMirror,
) {
    private val utf16OfCodePoint = HashMap<Int, IntArray?>()
    private val gate = MappingLock()

    fun invalidate(startLine: Int, endLineInclusive: Int) {
        gate.withLock {
            val last = endLineInclusive.coerceAtLeast(startLine)
            val keys = ArrayList<Int>()
            for (key in utf16OfCodePoint.keys) {
                if (key in startLine..last) keys += key
            }
            keys.forEach { utf16OfCodePoint.remove(it) }
        }
    }

    fun invalidateFrom(startLine: Int) {
        gate.withLock {
            val keys = ArrayList<Int>()
            for (key in utf16OfCodePoint.keys) {
                if (key >= startLine) keys += key
            }
            keys.forEach { utf16OfCodePoint.remove(it) }
        }
    }

    fun toUtf16(line: Int, codePointColumn: Int): Int = gate.withLock {
        val text = mirror.line(line)
        val table = tableFor(line, text)
        val column = codePointColumn.coerceAtLeast(0)
        when {
            table == null -> column.coerceIn(0, text.length)
            column >= table.size -> text.length
            else -> table[column]
        }
    }

    fun toCodePoint(line: Int, utf16Column: Int): Int = gate.withLock {
        val text = mirror.line(line)
        val utf16 = utf16Column.coerceIn(0, text.length)
        val table = tableFor(line, text)
        when {
            table == null -> utf16
            table.isEmpty() -> 0
            utf16 >= text.length -> table.size
            else -> {
                var lo = 0
                var hi = table.lastIndex
                var found = 0
                var exact: Int? = null
                while (lo <= hi && exact == null) {
                    val mid = (lo + hi) ushr 1
                    val start = table[mid]
                    when {
                        start == utf16 -> exact = mid
                        start < utf16 -> {
                            found = mid
                            lo = mid + 1
                        }
                        else -> hi = mid - 1
                    }
                }
                exact ?: found
            }
        }
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
