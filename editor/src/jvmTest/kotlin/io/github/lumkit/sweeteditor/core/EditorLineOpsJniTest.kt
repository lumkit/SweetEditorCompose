package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.AutoIndentMode
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorLineOpsJniTest {
    @Test
    fun indentSettingsAndLineCommandsChangeDocument() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("alpha\nbeta")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(editor.setAutoIndentMode(AutoIndentMode.KEEP_INDENT.value)?.handled == true)
            assertTrue(editor.setBackspaceUnindent(true)?.handled == true)
            assertTrue(editor.setInsertSpaces(true)?.handled == true)
            assertTrue(editor.setTabSize(4)?.handled == true)

            assertTrue(editor.moveLineDown()?.handled == true)
            assertEquals("beta\nalpha", document.utf8Text())

            assertTrue(editor.copyLineDown()?.handled == true)
            assertEquals("beta\nalpha\nalpha", document.utf8Text())

            assertTrue(editor.deleteLine()?.handled == true)
            assertEquals("beta\nalpha", document.utf8Text())

            assertTrue(editor.insertLineAbove()?.handled == true)
            assertTrue(document.utf8Text().startsWith("\n") || document.utf8Text().contains("\n\n"))
        } finally {
            editor.close()
            document.close()
        }
    }
}
