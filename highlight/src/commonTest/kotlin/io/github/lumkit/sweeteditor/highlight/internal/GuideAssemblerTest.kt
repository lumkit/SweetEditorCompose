package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.IndentGuide
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.highlight.runtime.SlIndentGuide
import kotlin.test.Test
import kotlin.test.assertEquals

class GuideAssemblerTest {
    @Test
    fun flagsClipGuideToVisibleRange() {
        val mirror = TextMirror()
        mirror.setText((0..10).joinToString("\n") { "        " })
        val mapping = PositionMapping(mirror)
        val guides = listOf(
            SlIndentGuide(column = 4, startLine = 0, endLine = 10, flags = 0b11),
        )
        val assembled = GuideAssembler.assemble(
            guides = guides,
            mapping = mapping,
            visibleStartLine = 2,
            visibleEndLine = 5,
        )
        assertEquals(
            listOf(
                IndentGuide(
                    start = TextPosition(2, 4),
                    end = TextPosition(5, 4),
                ),
            ),
            assembled,
        )
    }

    @Test
    fun continuesBeforeOnlyClampsStart() {
        val mirror = TextMirror()
        mirror.setText((0..6).joinToString("\n") { "        " })
        val mapping = PositionMapping(mirror)
        val assembled = GuideAssembler.assemble(
            guides = listOf(SlIndentGuide(column = 4, startLine = 0, endLine = 6, flags = 0b01)),
            mapping = mapping,
            visibleStartLine = 2,
            visibleEndLine = 4,
        )
        assertEquals(TextPosition(2, 4), assembled.single().start)
        assertEquals(TextPosition(6, 4), assembled.single().end)
    }
}
