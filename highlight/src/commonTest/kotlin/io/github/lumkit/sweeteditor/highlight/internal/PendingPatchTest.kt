package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.TextChange
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

class PendingPatchTest {
    @Test
    fun asciiReplaceMapsUtf16ColumnsToSameSweetLineColumns() {
        val mirror = TextMirror()
        mirror.setText("foo")
        val mapping = PositionMapping(mirror)
        val patch = mirror.encodePatch(
            TextChange(
                range = TextRange(TextPosition(0, 0), TextPosition(0, 3)),
                newText = "bar",
            ),
            mapping,
        )
        assertEquals(0, patch.slStartLine)
        assertEquals(0, patch.slStartColumn)
        assertEquals(0, patch.slEndLine)
        assertEquals(3, patch.slEndColumn)
        assertEquals("bar", patch.newText)
    }

    @Test
    fun emojiRangeUsesCodePointColumnsBeforeApply() {
        val mirror = TextMirror()
        mirror.setText("a😀b")
        val mapping = PositionMapping(mirror)
        val patch = mirror.encodePatch(
            TextChange(
                range = TextRange(TextPosition(0, 1), TextPosition(0, 3)),
                newText = "x",
            ),
            mapping,
        )
        assertEquals(1, patch.slStartColumn)
        assertEquals(2, patch.slEndColumn)
    }
}
