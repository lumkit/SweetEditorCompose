package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorLanguageJniTest {
    @Test
    fun autoClosingPairsIsHandled() {
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(
                editor.setAutoClosingPairs(intArrayOf('('.code), intArrayOf(')'.code))?.handled == true,
            )
            assertTrue(editor.insertText("(")?.handled == true)
            assertEquals("()", document.utf8Text())
        } finally {
            editor.close()
            document.close()
        }
    }

    @Test
    fun insertSpacesUsesTabSize() {
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(editor.setTabSize(2)?.handled == true)
            assertTrue(editor.setInsertSpaces(true)?.handled == true)
            assertTrue(editor.handleKeyEvent(KeyCode.TAB, null, 0)?.handled == true)
            assertEquals("  ", document.utf8Text())
        } finally {
            editor.close()
            document.close()
        }
    }

    @Test
    fun handleKeyNoneInsertsProviderText() {
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
            val document = Document.fromUtf8("")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(
                editor.handleKeyEvent(KeyCode.NONE, "\n    ".encodeToByteArray(), 0)?.handled == true,
            )
            assertEquals("\n    ", document.utf8Text())
        } finally {
            editor.close()
            document.close()
        }
    }

    @Test
    fun setBracketPairsIsHandled() {
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("()")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(
                editor.setBracketPairs(intArrayOf('('.code, '{'.code), intArrayOf(')'.code, '}'.code))
                    ?.handled == true,
            )
        } finally {
            editor.close()
            document.close()
        }
    }
}
