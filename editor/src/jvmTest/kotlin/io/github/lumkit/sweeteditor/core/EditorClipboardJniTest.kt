package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.core.protocol.EditorBuiltinCommand
import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import io.github.lumkit.sweeteditor.core.protocol.KeyModifier
import io.github.lumkit.sweeteditor.input.JvmEditorClipboard
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorClipboardJniTest {
    @Test
    fun selectedTextAfterSelectAll() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(editor.insertText("hello")?.handled == true)
            assertEquals("", editor.getSelectedText())

            val selectAll = editor.handleKeyEvent(KeyCode.A, null, KeyModifier.CTRL)
            assertTrue(selectAll != null && selectAll.handled)
            assertEquals(EditorBuiltinCommand.SELECT_ALL.value, selectAll.command)
            assertEquals("hello", editor.getSelectedText())
        } finally {
            editor.close()
            document.close()
        }
    }

    @Test
    fun jvmSystemClipboardRoundTrip() {
        assertTrue(JvmEditorClipboard.setText("sweet-editor-clipboard"))
        assertEquals("sweet-editor-clipboard", JvmEditorClipboard.getText())
    }
}
