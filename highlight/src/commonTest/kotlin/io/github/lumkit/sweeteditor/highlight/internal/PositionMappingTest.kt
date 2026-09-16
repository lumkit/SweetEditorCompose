package io.github.lumkit.sweeteditor.highlight.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class PositionMappingTest {
    @Test
    fun emojiCodePointColumnTwoMapsToUtf16ColumnThree() {
        val mirror = TextMirror()
        mirror.setText("a😀b")
        val mapping = PositionMapping(mirror)
        assertEquals(3, mapping.toUtf16(0, 2))
        assertEquals(2, mapping.toCodePoint(0, 3))
        assertEquals(0, mapping.toUtf16(0, 0))
        assertEquals(1, mapping.toUtf16(0, 1))
        assertEquals(4, mapping.toUtf16(0, 3))
        assertEquals(1, mapping.toCodePoint(0, 1))
        assertEquals(1, mapping.toCodePoint(0, 2))
    }
}
