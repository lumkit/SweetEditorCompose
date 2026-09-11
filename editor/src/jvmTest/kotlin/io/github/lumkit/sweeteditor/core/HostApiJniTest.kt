package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HostApiJniTest {
    @Test
    fun selectionNavigationAndEditsRoundTrip() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("hello world")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertEquals(1, document.lineCount())
            assertTrue(editor.setCursorPosition(0, 5)?.handled == true)
            assertEquals(TextPosition(0, 5), editor.getCursorPosition())
            assertTrue(editor.setSelection(TextPosition(0, 0), TextPosition(0, 5))?.handled == true)
            assertEquals("hello", editor.getSelectedText())
            assertTrue(editor.replaceText(0, 0, 0, 5, "hi")?.handled == true)
            assertTrue(editor.canUndo())
            assertTrue(editor.deleteText(0, 0, 0, 2)?.handled == true)
            assertTrue(editor.gotoPosition(0, 0)?.handled == true)
            assertTrue(editor.scrollToLine(0, 1)?.handled == true)
            assertTrue(editor.setScroll(0f, 0f)?.handled == true)
            assertTrue(editor.selectAll()?.handled == true)
        } finally {
            editor.close()
            document.close()
        }
    }
}
