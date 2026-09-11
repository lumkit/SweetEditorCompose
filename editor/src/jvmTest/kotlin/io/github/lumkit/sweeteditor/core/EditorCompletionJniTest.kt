package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.EditorTextEdit
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import io.github.lumkit.sweeteditor.encodeApplyTextEdits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorCompletionJniTest {
    @Test
    fun wordRangeReplaceAndApplyTextEdits() {
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("fun pr")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            editor.buildRenderModel()

            assertTrue(editor.insertText("i")?.handled == true)
            val word = editor.getWordRangeAtCursor()
            assertEquals(0, word.start.line)
            assertTrue(word.end.column > word.start.column)

            assertTrue(
                editor.replaceText(
                    word.start.line,
                    word.start.column,
                    word.end.line,
                    word.end.column,
                    "println",
                )?.handled == true,
            )
            assertTrue(document.utf8Text().contains("println"), document.utf8Text())

            val cursor = editor.getCursorPosition()
            assertEquals(0, cursor.line)
            assertTrue(
                editor.applyTextEdits(
                    encodeApplyTextEdits(
                        listOf(
                            EditorTextEdit(
                                range = TextRange(cursor, cursor),
                                newText = "()",
                            ),
                        ),
                    ),
                )?.handled == true,
            )
            assertTrue(document.utf8Text().contains("println()"), document.utf8Text())
        } finally {
            editor.close()
            document.close()
        }
    }
}
