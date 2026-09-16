package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.TextChange
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

class TextMirrorTest {
    @Test
    fun insertDeleteAndMultilineReplaceJoinWithNewlines() {
        val mirror = TextMirror()
        mirror.setText("foo")
        mirror.apply(
            TextChange(
                range = TextRange(TextPosition(0, 3), TextPosition(0, 3)),
                newText = "bar",
            ),
        )
        assertEquals("foobar", mirror.joinToString())

        mirror.apply(
            TextChange(
                range = TextRange(TextPosition(0, 3), TextPosition(0, 6)),
                newText = "",
            ),
        )
        assertEquals("foo", mirror.joinToString())

        mirror.apply(
            TextChange(
                range = TextRange(TextPosition(0, 1), TextPosition(0, 2)),
                newText = "x\ny",
            ),
        )
        assertEquals("fx\nyo", mirror.joinToString())
        assertEquals(2, mirror.lineCount)
        assertEquals("fx", mirror.line(0))
        assertEquals("yo", mirror.line(1))
    }

    @Test
    fun insertingNewlineSplitsTheLine() {
        val mirror = TextMirror()
        mirror.setText("ab")
        mirror.apply(
            TextChange(
                range = TextRange(TextPosition(0, 1), TextPosition(0, 1)),
                newText = "\n",
            ),
        )
        assertEquals("a\nb", mirror.joinToString())
        assertEquals(listOf("a", "b"), listOf(mirror.line(0), mirror.line(1)))
    }
}
